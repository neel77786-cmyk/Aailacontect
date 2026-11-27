import bcrypt from "bcryptjs";
import { getAuth, getFirestore } from "../../lib/firebase-admin.js";
import { signToken } from "../../lib/auth-middleware.js";
import { validateFamilyId, validatePassword, collectErrors } from "../../lib/validators.js";

/**
 * POST /api/auth/login
 *
 * Supports:
 * 1. Personal User ID + Password login: { userId: "USR-XXXXXX", password }
 * 2. Legacy/Family ID + Password login: { familyId: "FAM-XXXXXX", password, memberIdentifier? }
 */
export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const { userId, familyId, password, memberIdentifier } = req.body || {};
    const identifier = (userId || familyId || "").trim().toUpperCase();

    const errors = collectErrors([
      validateFamilyId(identifier),
      validatePassword(password),
    ]);

    if (errors) {
      return res.status(400).json({ success: false, errors });
    }

    const firestore = getFirestore();

    // Case 1: Login with Personal User ID (USR-XXXXXX)
    if (identifier.startsWith("USR-")) {
      const userDoc = await firestore.collection("users").doc(identifier).get();
      if (!userDoc.exists) {
        return res.status(401).json({
          success: false,
          error: "Invalid User ID or password.",
        });
      }

      const userData = userDoc.data();
      const matches = await bcrypt.compare(password, userData.passwordHash || "");
      if (!matches) {
        return res.status(401).json({
          success: false,
          error: "Invalid User ID or password.",
        });
      }

      const targetFamilyId = userData.ownFamilyId;
      const uid = userData.uid;
      const familyDoc = await firestore.collection("families").doc(targetFamilyId).get();
      const familyData = familyDoc.exists ? familyDoc.data() : {};

      const memberRef = firestore
        .collection("families")
        .doc(targetFamilyId)
        .collection("members")
        .doc(uid);
      const memberDoc = await memberRef.get();
      const memberData = memberDoc.exists ? memberDoc.data() : {};
      const role = memberData.role || "ADMIN";

      if (memberDoc.exists) {
        await memberRef.update({
          lastSeen: new Date().toISOString(),
          onlineStatus: "AVAILABLE",
        });
      }

      const token = signToken(uid, targetFamilyId, role);

      return res.status(200).json({
        success: true,
        userId: identifier,
        authUid: uid,
        familyId: targetFamilyId,
        familyJoinId: targetFamilyId,
        familyName: familyData.familyName || "Family Space",
        adminName: familyData.adminName || userData.name || "Admin",
        memberName: userData.name || "Member",
        role,
        token,
        expiresIn: process.env.JWT_EXPIRY || "24h",
      });
    }

    // Case 2: Login with Family ID (existing behavior preserved)
    const cleanFamilyId = identifier;
    const familyDoc = await firestore.collection("families").doc(cleanFamilyId).get();

    if (!familyDoc.exists) {
      return res.status(404).json({
        success: false,
        error: `Family Space "${cleanFamilyId}" not found.`,
      });
    }

    const familyData = familyDoc.data();
    const cleanMemberId = memberIdentifier?.trim();

    // Case 2A: Individual member login inside a family
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
          (d.userCode && d.userCode.toLowerCase() === cleanMemberId.toLowerCase()) ||
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
          error: "This account uses User ID or Admin login. Leave Member Name empty to sign in.",
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
        userId: memberData.userCode || uid,
        authUid: uid,
        familyId: cleanFamilyId,
        familyJoinId: cleanFamilyId,
        familyName: familyData.familyName || "Family Space",
        adminName: familyData.adminName || "Admin",
        memberName: memberData.name || "Member",
        role,
        token,
        expiresIn: process.env.JWT_EXPIRY || "24h",
      });
    }

    // Case 2B: Admin login via Firebase Auth
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
        return res.status(401).json({
          success: false,
          error: "Invalid Family ID or password.",
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
    if ((memberData.status || "PENDING") !== "APPROVED") {
      return res.status(403).json({
        success: false,
        error: `Your access is currently "${memberData.status}". Contact your family admin.`,
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
      userId: memberData.userCode || familyData.adminUserCode || firebaseUid,
      authUid: firebaseUid,
      familyId: cleanFamilyId,
      familyJoinId: cleanFamilyId,
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
