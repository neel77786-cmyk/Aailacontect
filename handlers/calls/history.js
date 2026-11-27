import { getFirestore } from "../../lib/firebase-admin.js";
import { requireApprovedMember } from "../../lib/auth-middleware.js";

/**
 * GET /api/calls/history
 *
 * Returns real call history for the authenticated user's family
 * plus any active incoming call (OFFERING within last 60 seconds).
 */
async function historyHandler(req, res) {
  if (req.method !== "GET") {
    res.setHeader("Allow", "GET");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const { uid, familyId } = req.user;
    const firestore = getFirestore();

    const callsSnapshot = await firestore
      .collection("families")
      .doc(familyId)
      .collection("calls")
      .orderBy("createdAt", "desc")
      .limit(15)
      .get();

    const nowMs = Date.now();
    let activeIncomingCall = null;

    const calls = callsSnapshot.docs.map((doc) => {
      const data = doc.data();
      const isOutgoing = data.callerId === uid;
      const peerId = isOutgoing ? data.receiverId : data.callerId;
      const peerName = isOutgoing
        ? data.receiverName || "Family Member"
        : data.callerName || "Family Member";

      const createdMs = data.createdAt ? new Date(data.createdAt).getTime() : 0;
      if (
        !activeIncomingCall &&
        data.receiverId === uid &&
        data.status === "OFFERING" &&
        nowMs - createdMs < 60_000
      ) {
        activeIncomingCall = {
          callId: doc.id,
          callerId: data.callerId,
          callerName: data.callerName || "Family Member",
          callType: data.callType || "VOICE",
          roomName: data.roomName || `call_${doc.id}`,
          status: data.status,
          startTime: data.startTime,
        };
      }

      let direction = isOutgoing ? "OUTGOING" : "INCOMING";
      if (!isOutgoing && (data.status === "MISSED" || data.status === "REJECTED")) {
        direction = "MISSED";
      }

      return {
        id: doc.id,
        callId: data.callId || doc.id,
        callerId: data.callerId || "",
        receiverId: data.receiverId || "",
        peerId: peerId || "",
        peerName,
        initials: peerName.substring(0, 2).toUpperCase(),
        callType: data.callType || "VOICE",
        status: data.status || "COMPLETED",
        direction,
        roomName: data.roomName || "",
        startTime: data.startTime || data.createdAt || null,
        endTime: data.endTime || null,
        durationSeconds: Number(data.durationSeconds || 0),
      };
    });

    if (activeIncomingCall) {
      console.log(
        `[CALL] activeIncomingCall family=${familyId} receiver=${uid} callId=${activeIncomingCall.callId}`
      );
    }

    return res.status(200).json({
      success: true,
      familyId,
      calls,
      activeIncomingCall,
      count: calls.length,
    });
  } catch (err) {
    console.error("Error fetching call history:", err);
    return res.status(500).json({
      success: false,
      error: "Failed to fetch call history.",
    });
  }
}

export default requireApprovedMember(historyHandler);
