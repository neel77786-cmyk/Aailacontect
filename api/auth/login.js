import bcrypt from "bcryptjs";
import { getAuth, getFirestore } from "../../lib/firebase-admin.js";
import { signToken } from "../../lib/auth-middleware.js";
import { validateFamilyId, validatePassword, collectErrors } from "../../lib/validators.js";

/**
 * POST /api/auth/login
 *
 * Supports:
 * 1. Family Admin login: { familyId, password } (verifies via Firebase Auth REST API)
 * 2. Family Member login: { familyId, memberIdentifier, password } (verifies via bcryptjs hash in Firestore)
 */
export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const { familyId, password, memberIdentifier } = req.body || {};

    const errors = collectErrors([
      validateFamilyId(familyId),
      validatePassword(password),
    ]);

    if (errors) {
      return res.status(400).json({ success: false, errors });
    }

    const cleanFamilyId = familyId.trim().toUpperCase();
    const firestore = getFirestore();

    const familyDoc = await firestore
      .collection("families")
      .doc(cleanFamilyId)
      .get();

    if (!familyDoc.exists) {
      return res.status(404).json({
        success: false,
        error: `Family Space "${cleanFamilyId}" not found.`,
      });
    }

    const familyData = familyDoc.data();
    const cleanMemberId = memberIdentifier?.trim();

    // Case A: Member login with memberIdentifier (name or memberId)
    if (cleanMemberId && cleanMemberId.toUpperCase() !== "ADMIN") {
      const membersSnap = await firestore
        .collection("families")
        .doc(cleanFamilyId)
        .collection("members")
        .get();

      const matchedDoc = membersSnap.docs.find((doc) => {
        const d = doc.data();
        return (
          doc.id.toLowerCase() === cleanMemberId.toLowerCase() ||
          (d.memberId && d.memberId.toLowerCase() === cleanMemberId.toLowerCase()) ||
          (d.name && d.name.toLowerCase() === cleanMemberId.toLowerCase())
        );
      });

      if (!matchedDoc) {
        return res.status(401).json({
          success: false,
          error: "Member not found in this Family Space.",
        });
      }

      const memberData = matchedDoc.data();
      if (!memberData.passwordHash) {
        return res.status(401).json({
          success: false,
          error: "This member account uses Admin login. Leave Member Name empty to sign in as Admin.",
        });
      }

      const passwordMatches = await bcrypt.compare(password, memberData.passwordHash);
      if (!passwordMatches) {
        return res.status(401).json({
          success: false,
          error: "Invalid Family ID, member name, or password.",
        });
      }

      const memberStatus = memberData.status || "PENDING";
      if (memberStatus !== "APPROVED") {
        return res.status(403).json({
          success: false,
          error:
            memberStatus === "PENDING"
              ? "Your join request is waiting for Family Admin approval."
              : `Your access is currently "${memberStatus}". Contact your family admin.`,
        });
      }

      const uid = matchedDoc.id;
      const role = memberData.role || "MEMBER";

      await matchedDoc.ref.update({
        lastSeen: new Date().toISOString(),
        onlineStatus: "AVAILABLE",
      });

      const token = signToken(uid, cleanFamilyId, role);

      return res.status(200).json({
        success: true,
        userId: uid,
        familyId: cleanFamilyId,
        familyName: familyData.familyName || "Family Space",
        adminName: familyData.adminName || "Admin",
        memberName: memberData.name || "Member",
        role,
        token,
        expiresIn: process.env.JWT_EXPIRY || "24h",
      });
    }

    // Case B: Admin login (existing behavior, 100% backward compatible)
    const syntheticEmail = `admin.${cleanFamilyId.toLowerCase()}@aaila.family`;
    const auth = getAuth();
    const firebaseApiKey = process.env.FIREBASE_WEB_API_KEY?.trim();

    let firebaseUid;

    if (firebaseApiKey) {
      const authResponse = await fetch(
        `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${firebaseApiKey}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            email: syntheticEmail,
            password,
            returnSecureToken: true,
          }),
        }
      );

      if (!authResponse.ok) {
        const errorData = await authResponse.json().catch(() => ({}));
        const errorMessage = errorData?.error?.message || "AUTH_FAILED";

        if (
          errorMessage === "EMAIL_NOT_FOUND" ||
          errorMessage === "INVALID_PASSWORD" ||
          errorMessage === "INVALID_LOGIN_CREDENTIALS"
        ) {
          return res.status(401).json({
            success: false,
            error: "Invalid Family ID or password.",
          });
        }

        if (errorMessage === "USER_DISABLED") {
          return res.status(403).json({
            success: false,
            error: "This account has been disabled. Contact your family admin.",
          });
        }

        return res.status(401).json({
          success: false,
          error: "Authentication failed. Please check your credentials.",
        });
      }

      const authData = await authResponse.json();
      firebaseUid = authData.localId;
    } else {
      try {
        const userRecord = await auth.getUserByEmail(syntheticEmail);
        firebaseUid = userRecord.uid;
      } catch (lookupErr) {
        if (lookupErr.code === "auth/user-not-found") {
          return res.status(401).json({
            success: false,
            error: "Invalid Family ID or password.",
          });
        }
        throw lookupErr;
      }
    }

    const memberDoc = await firestore
      .collection("families")
      .doc(cleanFamilyId)
      .collection("members")
      .doc(firebaseUid)
      .get();

    if (!memberDoc.exists) {
      return res.status(403).json({
        success: false,
        error: `You are not a member of Family Space "${cleanFamilyId}".`,
      });
    }

    const memberData = memberDoc.data();
    const memberStatus = memberData.status || "PENDING";

    if (memberStatus !== "APPROVED") {
      return res.status(403).json({
        success: false,
        error: `Your access is currently "${memberStatus}". Contact your family admin.`,
      });
    }

    const role = memberData.role || "ADMIN";

    await memberDoc.ref.update({
      lastSeen: new Date().toISOString(),
      onlineStatus: "AVAILABLE",
    });

    const token = signToken(firebaseUid, cleanFamilyId, role);

    return res.status(200).json({
      success: true,
      userId: firebaseUid,
      familyId: cleanFamilyId,
      familyName: familyData.familyName || "Family Space",
      adminName: familyData.adminName || "Admin",
      memberName: memberData.name || familyData.adminName || "Admin",
      role,
      token,
      expiresIn: process.env.JWT_EXPIRY || "24h",
    });
  } catch (err) {
    console.error("Login error:", err);
    return res.status(500).json({
      success: false,
      error: "Internal server error during login.",
    });
  }
}
