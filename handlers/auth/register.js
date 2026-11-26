import { getAuth, getFirestore } from "../../lib/firebase-admin.js";
import { signToken } from "../../lib/auth-middleware.js";
import {
  validateFamilyId,
  validatePassword,
  validateName,
  collectErrors,
} from "../../lib/validators.js";

/**
 * POST /api/auth/register
 *
 * Creates a new Family Space with an admin account.
 * - Creates a Firebase Auth user with a synthetic email
 * - Creates Firestore documents for the family and admin member
 * - Returns a signed JWT session token
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
    } = req.body || {};

    // Validate inputs
    const errors = collectErrors([
      validateName(familyName, "Family name"),
      validateName(adminName, "Admin name"),
      validateFamilyId(familyId),
      validatePassword(password),
    ]);

    if (errors) {
      return res.status(400).json({ success: false, errors });
    }

    const cleanFamilyId = familyId.trim().toUpperCase();
    const syntheticEmail = `admin.${cleanFamilyId.toLowerCase()}@aaila.family`;

    const auth = getAuth();
    const firestore = getFirestore();

    // Check if family already exists
    const familyDoc = await firestore
      .collection("families")
      .doc(cleanFamilyId)
      .get();

    if (familyDoc.exists) {
      return res.status(409).json({
        success: false,
        error: `Family Space "${cleanFamilyId}" already exists. Use login instead.`,
      });
    }

    // Create Firebase Auth user
    let userRecord;
    try {
      userRecord = await auth.createUser({
        email: syntheticEmail,
        password: password,
        displayName: adminName.trim(),
      });
    } catch (authErr) {
      if (authErr.code === "auth/email-already-exists") {
        return res.status(409).json({
          success: false,
          error: "An admin account for this Family ID already exists.",
        });
      }
      throw authErr;
    }

    const uid = userRecord.uid;
    const now = new Date().toISOString();

    // Create family document
    const familyData = {
      familyId: cleanFamilyId,
      familyName: familyName.trim(),
      adminName: adminName.trim(),
      adminUid: uid,
      requireAdminApproval,
      privateDirectoryOnly,
      createdAt: now,
      updatedAt: now,
    };

    // Create admin member document
    const memberData = {
      uid,
      memberId: "ADMIN",
      name: adminName.trim(),
      relationship: "Family Admin",
      role: "ADMIN",
      status: "APPROVED",
      onlineStatus: "AVAILABLE",
      statusNote: "Family Organizer",
      initials: adminName.trim().substring(0, 2).toUpperCase(),
      avatarColorIndex: 0,
      lastSeen: now,
      createdAt: now,
    };

    // Write family, admin member, and default Family Circle group thread in a batch
    const batch = firestore.batch();
    batch.set(firestore.collection("families").doc(cleanFamilyId), familyData);
    batch.set(
      firestore
        .collection("families")
        .doc(cleanFamilyId)
        .collection("members")
        .doc(uid),
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
        title: `${familyName.trim()} Circle`,
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

    // Sign session JWT
    const token = signToken(uid, cleanFamilyId, "ADMIN");

    return res.status(201).json({
      success: true,
      userId: uid,
      familyId: cleanFamilyId,
      familyName: familyName.trim(),
      adminName: adminName.trim(),
      role: "ADMIN",
      token,
      expiresIn: process.env.JWT_EXPIRY || "24h",
    });
  } catch (err) {
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
