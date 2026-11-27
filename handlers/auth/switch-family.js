import { getFirestore } from "../../lib/firebase-admin.js";
import { requireAuth, signToken } from "../../lib/auth-middleware.js";

/**
 * POST /api/auth/switch-family
 *
 * Allows a user who owns their own family and/or joined another family
 * to switch their active family space without losing ownership.
 */
async function switchFamilyHandler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  const { uid } = req.user;
  const targetFamilyId = req.body?.familyId?.toString().trim().toUpperCase();
  if (!targetFamilyId) {
    return res.status(400).json({ success: false, error: "familyId is required." });
  }

  try {
    const firestore = getFirestore();
    const famRef = firestore.collection("families").doc(targetFamilyId);
    const [famDoc, memDoc] = await Promise.all([
      famRef.get(),
      famRef.collection("members").doc(uid).get(),
    ]);

    if (!famDoc.exists || !memDoc.exists) {
      return res.status(404).json({ success: false, error: "Membership in that family not found." });
    }

    const memData = memDoc.data();
    if (memData.status !== "APPROVED") {
      return res.status(403).json({
        success: false,
        error: `Your membership in ${targetFamilyId} is ${memData.status}.`,
      });
    }

    const famData = famDoc.data();
    const role = memData.role || "MEMBER";
    await famRef.collection("members").doc(uid).update({
      onlineStatus: "AVAILABLE",
      lastSeen: new Date().toISOString(),
    });
    const token = signToken(uid, targetFamilyId, role);

    return res.status(200).json({
      success: true,
      userId: memData.userCode || uid,
      authUid: uid,
      familyId: targetFamilyId,
      familyJoinId: targetFamilyId,
      familyName: famData.familyName || "Family Space",
      adminName: famData.adminName || "Admin",
      memberName: memData.name || "Member",
      role,
      token,
    });
  } catch (err) {
    console.error("Switch family error:", err);
    return res.status(500).json({ success: false, error: "Failed to switch family." });
  }
}

export default function handler(req, res) {
  requireAuth(req, res, () => switchFamilyHandler(req, res));
}
