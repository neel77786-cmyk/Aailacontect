import { getFirestore } from "../../lib/firebase-admin.js";
import { requireApprovedMember } from "../../lib/auth-middleware.js";

/**
 * GET / PATCH /api/calls/[callId]
 *
 * Updates real call state (CONNECTED, REJECTED, MISSED, COMPLETED) with endTime & durationSeconds.
 */
async function callByIdHandler(req, res) {
  const { familyId } = req.user;
  const callId = req.query?.callId?.toString().trim();

  if (!callId) {
    return res.status(400).json({ success: false, error: "callId is required." });
  }

  const firestore = getFirestore();
  const callRef = firestore
    .collection("families")
    .doc(familyId)
    .collection("calls")
    .doc(callId);

  if (req.method === "GET") {
    const doc = await callRef.get();
    if (!doc.exists) {
      return res.status(404).json({ success: false, error: "Call not found." });
    }
    return res.status(200).json({ success: true, call: { id: doc.id, ...doc.data() } });
  }

  if (req.method === "PATCH") {
    const { status, durationSeconds } = req.body || {};
    const allowedStatuses = ["OFFERING", "CONNECTED", "REJECTED", "MISSED", "COMPLETED"];
    const cleanStatus = status?.toString().toUpperCase();

    if (!cleanStatus || !allowedStatuses.includes(cleanStatus)) {
      return res.status(400).json({
        success: false,
        error: `status must be one of: ${allowedStatuses.join(", ")}`,
      });
    }

    try {
      const doc = await callRef.get();
      if (!doc.exists) {
        return res.status(404).json({ success: false, error: "Call not found." });
      }

      const updates = { status: cleanStatus };
      if (cleanStatus === "COMPLETED" || cleanStatus === "REJECTED" || cleanStatus === "MISSED") {
        updates.endTime = new Date().toISOString();
        if (durationSeconds !== undefined) {
          updates.durationSeconds = Math.max(0, Number(durationSeconds) || 0);
        }
      }

      await callRef.update(updates);
      return res.status(200).json({ success: true, callId, ...updates });
    } catch (err) {
      console.error("Error updating call:", err);
      return res.status(500).json({ success: false, error: "Failed to update call." });
    }
  }

  res.setHeader("Allow", "GET, PATCH");
  return res.status(405).json({ success: false, error: "Method not allowed." });
}

export default requireApprovedMember(callByIdHandler);
