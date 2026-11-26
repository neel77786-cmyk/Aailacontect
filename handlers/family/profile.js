import admin from "firebase-admin";
import { getFirestore } from "../../lib/firebase-admin.js";
import { requireApprovedMember } from "../../lib/auth-middleware.js";

/**
 * PATCH /api/family/profile
 *
 * Updates the current user's profile/presence/FCM token,
 * OR (if Admin and familySettings provided) updates family privacy settings.
 */
async function profileHandler(req, res) {
  if (req.method !== "PATCH") {
    res.setHeader("Allow", "PATCH");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  const { uid, familyId, role } = req.user;
  const {
    name,
    relationship,
    statusNote,
    onlineStatus,
    photoUrl,
    fcmToken,
    requireAdminApproval,
    privateDirectoryOnly,
  } = req.body || {};

  const firestore = getFirestore();
  const familyRef = firestore.collection("families").doc(familyId);

  try {
    const memberUpdates = {};
    if (typeof name === "string" && name.trim().length >= 2) {
      memberUpdates.name = name.trim();
      memberUpdates.initials = name.trim().substring(0, 2).toUpperCase();
    }
    if (typeof relationship === "string" && relationship.trim().isNotEmpty !== false) {
      memberUpdates.relationship = relationship.trim();
    }
    if (typeof statusNote === "string") {
      memberUpdates.statusNote = statusNote.trim();
    }
    if (typeof onlineStatus === "string") {
      memberUpdates.onlineStatus = onlineStatus.trim().toUpperCase();
    }
    if (typeof photoUrl === "string") {
      memberUpdates.photoUrl = photoUrl.trim();
    }
    if (typeof fcmToken === "string" && fcmToken.trim().length > 0) {
      const cleanToken = fcmToken.trim();
      memberUpdates.fcmToken = cleanToken;
      memberUpdates.fcmTokens = admin.firestore.FieldValue.arrayUnion(cleanToken);
    }

    if (Object.keys(memberUpdates).length > 0) {
      memberUpdates.lastSeen = new Date().toISOString();
      await familyRef.collection("members").doc(uid).update(memberUpdates);
    }

    // Admin-only family settings update
    if (requireAdminApproval !== undefined || privateDirectoryOnly !== undefined) {
      if (role !== "ADMIN") {
        return res.status(403).json({
          success: false,
          error: "Only the Family Admin can change family privacy settings.",
        });
      }
      const familyUpdates = { updatedAt: new Date().toISOString() };
      if (typeof requireAdminApproval === "boolean") {
        familyUpdates.requireAdminApproval = requireAdminApproval;
      }
      if (typeof privateDirectoryOnly === "boolean") {
        familyUpdates.privateDirectoryOnly = privateDirectoryOnly;
      }
      await familyRef.update(familyUpdates);
    }

    return res.status(200).json({
      success: true,
      message: "Profile updated.",
    });
  } catch (err) {
    console.error("Profile update error:", err);
    return res.status(500).json({
      success: false,
      error: "Failed to update profile.",
    });
  }
}

export default requireApprovedMember(profileHandler);
