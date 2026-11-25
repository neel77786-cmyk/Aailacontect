import { getFirestore } from "../../../lib/firebase-admin.js";
import { requireAuth } from "../../../lib/auth-middleware.js";

/**
 * /api/family/members/[uid]
 *
 * DELETE - Admin: remove a member
 * PATCH  - Admin: update member status/role
 */
async function memberByUidHandler(req, res) {
  const { uid: callerUid, familyId, role: callerRole } = req.user;
  const { uid: targetUid } = req.query || {};

  if (!targetUid) {
    return res.status(400).json({ success: false, error: "Member UID is required." });
  }

  // Only admins can modify other members
  if (callerRole !== "ADMIN") {
    return res.status(403).json({
      success: false,
      error: "Only family admins can manage members.",
    });
  }

  const firestore = getFirestore();
  const memberRef = firestore
    .collection("families")
    .doc(familyId)
    .collection("members")
    .doc(targetUid);

  if (req.method === "DELETE") {
    // Don't allow admin to delete themselves
    if (targetUid === callerUid) {
      return res.status(400).json({
        success: false,
        error: "Admin cannot remove themselves from the family.",
      });
    }

    try {
      const memberDoc = await memberRef.get();
      if (!memberDoc.exists) {
        return res.status(404).json({
          success: false,
          error: "Member not found.",
        });
      }

      await memberRef.delete();

      return res.status(200).json({
        success: true,
        message: `Member ${targetUid} removed from family.`,
      });
    } catch (err) {
      console.error("Error deleting member:", err);
      return res.status(500).json({
        success: false,
        error: "Failed to remove member.",
      });
    }
  }

  if (req.method === "PATCH") {
    const { status, memberRole, onlineStatus, statusNote } = req.body || {};

    const updates = {};
    if (status) updates.status = status;
    if (memberRole) updates.role = memberRole;
    if (onlineStatus) updates.onlineStatus = onlineStatus;
    if (statusNote !== undefined) updates.statusNote = statusNote;

    if (Object.keys(updates).length === 0) {
      return res.status(400).json({
        success: false,
        error: "No fields to update. Provide status, memberRole, onlineStatus, or statusNote.",
      });
    }

    updates.updatedAt = new Date().toISOString();

    try {
      const memberDoc = await memberRef.get();
      if (!memberDoc.exists) {
        return res.status(404).json({
          success: false,
          error: "Member not found.",
        });
      }

      await memberRef.update(updates);

      return res.status(200).json({
        success: true,
        message: "Member updated.",
        updates,
      });
    } catch (err) {
      console.error("Error updating member:", err);
      return res.status(500).json({
        success: false,
        error: "Failed to update member.",
      });
    }
  }

  res.setHeader("Allow", "DELETE, PATCH");
  return res.status(405).json({ success: false, error: "Method not allowed." });
}

export default function handler(req, res) {
  requireAuth(req, res, () => memberByUidHandler(req, res));
}
