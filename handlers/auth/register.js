import crypto from "node:crypto";
import bcrypt from "bcryptjs";
import { getAuth, getFirestore } from "../../lib/firebase-admin.js";
import { signToken } from "../../lib/auth-middleware.js";
import {
  validateFamilyId,
  validatePassword,
  validateName,
  collectErrors,
} from "../../lib/validators.js";

const ID_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

function generateCode(prefix, length = 6) {
  const bytes = crypto.randomBytes(length);
  let out = "";
  for (let i = 0; i < length; i++) {
    out += ID_ALPHABET[bytes[i] % ID_ALPHABET.length];
  }
  return `${prefix}-${out}`;
}

/**
 * POST /api/auth/register
 *
 * Simple 3-field registration: { familyName, adminName, password }
 * (Also accepts optional legacy `familyId` if provided.)
 *
 * Automatically generates:
 * - Unique personal User ID (USR-XXXXXX)
 * - Unique Family Join ID (FAM-XXXXXX)
 * Uses a Firestore transaction to verify uniqueness before saving.
 */
export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const {
      familyName,
      adminName,
      familyId,
      password,
      requireAdminApproval = true,
      privateDirectoryOnly = true,
      memberUnit = 5,
    } = req.body || {};

    const parsedMemberUnit = Math.min(Math.max(parseInt(memberUnit, 10) || 5, 1), 1000);

    const validationList = [
      validateName(familyName, "Family name"),
      validateName(adminName, "Your name"),
      validatePassword(password),
    ];
    if (familyId && typeof familyId === "string" && familyId.trim().isNotEmpty !== false) {
      validationList.push(validateFamilyId(familyId));
    }

    const errors = collectErrors(validationList);
    if (errors) {
      return res.status(400).json({ success: false, errors });
    }

    const cleanFamilyName = familyName.trim();
    const cleanAdminName = adminName.trim();
    const auth = getAuth();
    const firestore = getFirestore();

    // Hash password securely with bcryptjs
    const passwordHash = await bcrypt.hash(password, 10);

    // Generate collision-resistant unique User ID and Family Join ID
    let userCode = generateCode("USR", 6);
    let cleanFamilyId = familyId?.trim()
      ? familyId.trim().toUpperCase()
      : generateCode("FAM", 6);

    // Reserve both IDs atomically in a Firestore transaction
    let reserved = false;
    for (let attempt = 0; attempt < 5; attempt++) {
      const userRef = firestore.collection("users").doc(userCode);
      const famRef = firestore.collection("families").doc(cleanFamilyId);

      const ok = await firestore.runTransaction(async (tx) => {
        const [uDoc, fDoc] = await Promise.all([tx.get(userRef), tx.get(famRef)]);
        if (uDoc.exists) {
          userCode = generateCode("USR", 6);
          return false;
        }
        if (fDoc.exists) {
          if (familyId?.trim()) {
            throw Object.assign(new Error(`Family Space "${cleanFamilyId}" already exists.`), {
              statusCode: 409,
            });
          }
          cleanFamilyId = generateCode("FAM", 6);
          return false;
        }
        return true;
      });

      if (ok) {
        reserved = true;
        break;
      }
    }

    if (!reserved) {
      return res.status(503).json({
        success: false,
        error: "Could not allocate unique IDs. Please try again.",
      });
    }

    const syntheticEmail = `admin.${cleanFamilyId.toLowerCase()}@aaila.family`;

    let userRecord;
    try {
      userRecord = await auth.createUser({
        email: syntheticEmail,
        password,
        displayName: cleanAdminName,
      });
    } catch (authErr) {
      if (authErr.code === "auth/email-already-exists") {
        return res.status(409).json({
          success: false,
          error: "An account for this Family ID already exists.",
        });
      }
      throw authErr;
    }

    const uid = userRecord.uid;
    const now = new Date().toISOString();

    const userAccountData = {
      uid,
      userCode,
      name: cleanAdminName,
      passwordHash,
      ownFamilyId: cleanFamilyId,
      families: [cleanFamilyId],
      createdAt: now,
      updatedAt: now,
    };

    const familyData = {
      familyId: cleanFamilyId,
      familyJoinId: cleanFamilyId,
      familyName: cleanFamilyName,
      adminName: cleanAdminName,
      adminUid: uid,
      adminUserCode: userCode,
      requireAdminApproval,
      privateDirectoryOnly,
      memberUnit: parsedMemberUnit,
      createdAt: now,
      updatedAt: now,
    };

    const memberData = {
      uid,
      userCode,
      memberId: userCode,
      name: cleanAdminName,
      relationship: "Family Admin",
      role: "ADMIN",
      status: "APPROVED",
      onlineStatus: "AVAILABLE",
      statusNote: "Family Organizer",
      initials: cleanAdminName.substring(0, 2).toUpperCase(),
      avatarColorIndex: cleanAdminName.split("").reduce((acc, ch) => acc + ch.charCodeAt(0), 0) % 6,
      lastSeen: now,
      createdAt: now,
    };

    // Write user account, family space, admin member, and default group thread atomically
    const batch = firestore.batch();
    batch.set(firestore.collection("users").doc(userCode), userAccountData);
    batch.set(firestore.collection("families").doc(cleanFamilyId), familyData);
    batch.set(
      firestore.collection("families").doc(cleanFamilyId).collection("members").doc(uid),
      memberData
    );
    batch.set(
      firestore
        .collection("families")
        .doc(cleanFamilyId)
        .collection("threads")
        .doc("thread_family_circle"),
      {
        id: "thread_family_circle",
        title: `${cleanFamilyName} Circle`,
        subtitle: "Private Family Group",
        memberId: null,
        isGroup: true,
        initials: "FC",
        lastMessagePreview: "Welcome to your private family chat!",
        updatedAt: now,
        createdAt: now,
      }
    );
    await batch.commit();

    const token = signToken(uid, cleanFamilyId, "ADMIN");

    return res.status(201).json({
      success: true,
      userId: userCode,
      authUid: uid,
      familyId: cleanFamilyId,
      familyJoinId: cleanFamilyId,
      familyName: cleanFamilyName,
      adminName: cleanAdminName,
      memberUnit: parsedMemberUnit,
      role: "ADMIN",
      token,
      expiresIn: process.env.JWT_EXPIRY || "24h",
    });
  } catch (err) {
    if (err.statusCode === 409) {
      return res.status(409).json({ success: false, error: err.message });
    }
    console.error("Registration error:", err);

    const msg = err?.message || "";
    const code = err?.code;

    if (msg.includes("Missing Firebase Admin SDK")) {
      return res.status(500).json({
        success: false,
        error: "Server config error: Firebase Admin environment variables are missing in Vercel.",
      });
    }
    if (msg.includes("JWT_SECRET")) {
      return res.status(500).json({
        success: false,
        error: "Server config error: JWT_SECRET is missing in Vercel.",
      });
    }
    if (msg.includes("PEM") || msg.includes("private key") || code === "app/invalid-credential") {
      return res.status(500).json({
        success: false,
        error: "Firebase credential error: check FIREBASE_PRIVATE_KEY and FIREBASE_CLIENT_EMAIL in Vercel.",
      });
    }
    if (code === 5 || msg.includes("NOT_FOUND") || msg.includes("database")) {
      return res.status(500).json({
        success: false,
        error: "Cloud Firestore database not found. Create Firestore in Firebase Console first.",
      });
    }

    return res.status(500).json({
      success: false,
      error: `Registration failed (${code || "server_error"}): ${msg || "Internal server error."}`,
    });
  }
}
