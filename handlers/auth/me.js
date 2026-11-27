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
      },
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
