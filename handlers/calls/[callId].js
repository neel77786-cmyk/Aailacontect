import { getFirebaseAdmin, getFirestore } from "../../lib/firebase-admin.js";
import { requireApprovedMember } from "../../lib/auth-middleware.js";

/**
 * GET / PATCH /api/calls/[callId]
 *
 * Uses a Firestore transaction to safely handle simultaneous Accept / Reject / Cancel
 * across multiple receiver devices, and sends a high-priority `call_cancel` FCM
 * to stop ringing on all other receiver devices.
 */
async function callByIdHandler(req, res) {
  const { familyId } = req.user;
  const callId = req.query?.callId?.toString().trim();

  if (!callId) {
    return res.status(400).json({ success: false, error: "callId is required." });
  }

  const firestore = getFirestore();
  const familyRef = firestore.collection("families").doc(familyId);
  const callRef = familyRef.collection("calls").doc(callId);

  if (req.method === "GET") {
    const doc = await callRef.get();
    if (!doc.exists) {
      return res.status(404).json({ success: false, error: "Call not found." });
    }
    return res.status(200).json({ success: true, call: { id: doc.id, ...doc.data() } });
  }

  if (req.method === "PATCH") {
    const { status, durationSeconds } = req.body || {};
    const allowedStatuses = ["OFFERING", "CONNECTED", "REJECTED", "MISSED", "COMPLETED", "CANCELLED"];
    const cleanStatus = status?.toString().toUpperCase();

    if (!cleanStatus || !allowedStatuses.includes(cleanStatus)) {
      return res.status(400).json({
        success: false,
        error: `status must be one of: ${allowedStatuses.join(", ")}`,
      });
    }

    try {
      const result = await firestore.runTransaction(async (tx) => {
        const doc = await tx.get(callRef);
        if (!doc.exists) {
          return { notFound: true };
        }

        const currentData = doc.data();
        const currentStatus = currentData.status;

        // Prevent conflicting transitions across multiple devices
        if (currentStatus === "CONNECTED" && (cleanStatus === "REJECTED" || cleanStatus === "MISSED" || cleanStatus === "CANCELLED")) {
          return { conflict: `Call already ${currentStatus}.` };
        }
        if ((currentStatus === "REJECTED" || currentStatus === "MISSED" || currentStatus === "CANCELLED" || currentStatus === "COMPLETED") && cleanStatus === "CONNECTED") {
          return { conflict: `Call already ended (${currentStatus}).` };
        }

        const updates = { status: cleanStatus };
        if (["COMPLETED", "REJECTED", "MISSED", "CANCELLED"].includes(cleanStatus)) {
          updates.endTime = new Date().toISOString();
          if (durationSeconds !== undefined) {
            updates.durationSeconds = Math.max(0, Number(durationSeconds) || 0);
          }
        }

        tx.update(callRef, updates);
        return { callData: currentData, updates };
      });

      if (result.notFound) {
        return res.status(404).json({ success: false, error: "Call not found." });
      }
      if (result.conflict) {
        return res.status(409).json({ success: false, error: result.conflict });
      }

      // Notify all receiver devices to stop ringing when call is accepted, rejected, missed, or cancelled
      const receiverId = result.callData?.receiverId;
      if (receiverId && ["CONNECTED", "REJECTED", "MISSED", "CANCELLED", "COMPLETED"].includes(cleanStatus)) {
        const receiverDoc = await familyRef.collection("members").doc(receiverId).get();
        if (receiverDoc.exists) {
          const rData = receiverDoc.data();
          const tokenSet = new Set();
          if (rData.fcmToken) tokenSet.add(rData.fcmToken);
          if (Array.isArray(rData.fcmTokens)) {
            rData.fcmTokens.forEach((t) => {
              if (typeof t === "string" && t.trim().length > 0) tokenSet.add(t.trim());
            });
          }
          const tokens = Array.from(tokenSet);
          if (tokens.length > 0) {
            getFirebaseAdmin()
              .messaging()
              .sendEachForMulticast({
                tokens,
                android: { priority: "high", ttl: 15000 },
                data: {
                  type: "call_cancel",
                  callId,
                  reason: cleanStatus,
                },
              })
              .catch(() => {});
          }
        }
      }

      return res.status(200).json({ success: true, callId, ...result.updates });
    } catch (err) {
      console.error("Error updating call:", err);
      return res.status(500).json({ success: false, error: "Failed to update call." });
    }
  }

  res.setHeader("Allow", "GET, PATCH");
  return res.status(405).json({ success: false, error: "Method not allowed." });
}

export default requireApprovedMember(callByIdHandler);
