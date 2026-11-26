import { v4 as uuidv4 } from "uuid";
import { getFirebaseAdmin, getFirestore } from "../../lib/firebase-admin.js";
import { requireApprovedMember } from "../../lib/auth-middleware.js";

/**
 * POST /api/calls/initiate
 *
 * Creates a real call record + LiveKit room name and notifies the receiver via FCM.
 */
async function initiateHandler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const { uid, familyId, name } = req.user;
    const { targetMemberId, callType = "VOICE" } = req.body || {};

    if (!targetMemberId) {
      return res.status(400).json({
        success: false,
        error: "targetMemberId is required.",
      });
    }

    const allowedTypes = ["VOICE", "VIDEO"];
    const cleanType = callType.toUpperCase();
    if (!allowedTypes.includes(cleanType)) {
      return res.status(400).json({
        success: false,
        error: "callType must be VOICE or VIDEO.",
      });
    }

    const firestore = getFirestore();
    const memberDoc = await firestore
      .collection("families")
      .doc(familyId)
      .collection("members")
      .doc(targetMemberId)
      .get();

    if (!memberDoc.exists || memberDoc.data().status !== "APPROVED") {
      return res.status(404).json({
        success: false,
        error: "Approved target family member not found.",
      });
    }

    const receiver = memberDoc.data();
    const callId = uuidv4();
    const roomName = `call_${callId.replace(/-/g, "").substring(0, 12)}`;
    const now = new Date().toISOString();

    const callData = {
      callId,
      familyId,
      callerId: uid,
      callerName: name,
      receiverId: targetMemberId,
      receiverName: receiver.name || "Family Member",
      receiverInitials: receiver.initials || "FM",
      callType: cleanType,
      roomName,
      status: "OFFERING",
      startTime: now,
      endTime: null,
      durationSeconds: 0,
      createdAt: now,
    };

    await firestore
      .collection("families")
      .doc(familyId)
      .collection("calls")
      .doc(callId)
      .set(callData);

    // Send FCM incoming call push if receiver has a registered FCM token
    if (receiver.fcmToken) {
      getFirebaseAdmin()
        .messaging()
        .send({
          token: receiver.fcmToken,
          notification: {
            title: `Incoming ${cleanType === "VIDEO" ? "Video" : "Voice"} Call`,
            body: `${name} is calling you on AAILA`,
          },
          data: {
            type: "call",
            callId,
            callerId: uid,
            callerName: name,
            callType: cleanType,
            roomName,
          },
        })
        .catch(() => {});
    }

    return res.status(201).json({
      success: true,
      callId,
      roomName,
      status: "OFFERING",
    });
  } catch (err) {
    console.error("Error initiating call:", err);
    return res.status(500).json({
      success: false,
      error: "Failed to initiate call.",
    });
  }
}

export default requireApprovedMember(initiateHandler);
