import { getFirestore } from "../../lib/firebase-admin.js";
import { requireAuth } from "../../lib/auth-middleware.js";

/**
 * GET /api/auth/me
 *
 * Returns the authenticated user's profile and family info.
 * Requires a valid Bearer token.
 */
async function meHandler(req, res) {
  if (req.method !== "GET") {
    res.setHeader("Allow", "GET");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const { uid, familyId, role } = req.user;
    const firestore = getFirestore();

    // Fetch member document
    const memberDoc = await firestore
      .collection("families")
      .doc(familyId)
      .collection("members")
      .doc(uid)
      .get();

    if (!memberDoc.exists) {
      return res.status(404).json({
        success: false,
        error: "User profile not found.",
      });
    }

    const memberData = memberDoc.data();

    // Fetch family summary
    const familyDoc = await firestore
      .collection("families")
      .doc(familyId)
      .get();

    const familyData = familyDoc.exists ? familyDoc.data() : {};

    // Fetch all families this user owns or has joined
    let families = [
      {
        familyId,
        familyName: familyData.familyName || "Family Space",
        role,
        status: memberData.status || "APPROVED",
      },
    ];
    const userCode = memberData.userCode;
    if (userCode && userCode.startsWith("USR-")) {
      const userDoc = await firestore.collection("users").doc(userCode).get();
      const famIds = userDoc.exists && Array.isArray(userDoc.data().families) ? userDoc.data().families : [];
      if (famIds.length > 0) {
        const resolved = await Promise.all(
          famIds.map(async (fId) => {
            const fRef = firestore.collection("families").doc(fId);
            const [fD, mD] = await Promise.all([fRef.get(), fRef.collection("members").doc(uid).get()]);
            if (!fD.exists || !mD.exists) return null;
            return {
              familyId: fId,
              familyName: fD.data().familyName || "Family Space",
              role: mD.data().role || "MEMBER",
              status: mD.data().status || "PENDING",
            };
          })
        );
        const valid = resolved.filter(Boolean);
        if (valid.length > 0) families = valid;
      }
    }

    return res.status(200).json({
      success: true,
      user: {
        userId: memberData.userCode || uid,
        authUid: uid,
        familyId,
        familyJoinId: familyId,
        role,
        name: memberData.name || "Family Member",
        relationship: memberData.relationship || "Member",
        initials: memberData.initials || "??",
        avatarColorIndex: memberData.avatarColorIndex || 0,
        onlineStatus: memberData.onlineStatus || "OFFLINE",
        statusNote: memberData.statusNote || "",
        status: memberData.status || "PENDING",
      },
      family: {
        familyId,
        familyJoinId: familyId,
        familyName: familyData.familyName || "Family Space",
        adminName: familyData.adminName || "Admin",
        requireAdminApproval: familyData.requireAdminApproval ?? true,
        privateDirectoryOnly: familyData.privateDirectoryOnly ?? true,
        memberUnit: Number(familyData.memberUnit || 0),
      },
      families,
    });
  } catch (err) {
    console.error("Error fetching user profile:", err);
    return res.status(500).json({
      success: false,
      error: "Internal server error.",
    });
  }
}

export default function handler(req, res) {
  requireAuth(req, res, () => meHandler(req, res));
}
