import admin from "firebase-admin";
import { v4 as uuidv4 } from "uuid";
import { getFirebaseAdmin, getFirestore } from "../../lib/firebase-admin.js";
import { requireApprovedMember } from "../../lib/auth-middleware.js";
import { generateLiveKitToken } from "../../lib/livekit.js";

/**
 * POST /api/calls/initiate
 *
 * Creates a real call record + LiveKit room + returns LiveKit token immediately (1 roundtrip!)
 * and notifies the receiver via FCM.
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
    const safeCallerName =
      typeof name === "string" && name.trim().length > 0 ? name.trim() : "Family Member";
    const callId = uuidv4();
    const roomName = `call_${callId.replace(/-/g, "").substring(0, 12)}`;
    const now = new Date().toISOString();

    const callData = {
      callId,
      familyId,
      callerId: uid,
      callerName: safeCallerName,
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

    // Send high-priority data-only FCM push to ALL registered receiver devices
    const tokenSet = new Set();
    if (receiver.fcmToken) tokenSet.add(receiver.fcmToken);
    if (Array.isArray(receiver.fcmTokens)) {
      receiver.fcmTokens.forEach((t) => {
        if (typeof t === "string" && t.trim().length > 0) tokenSet.add(t.trim());
      });
    }
    const tokens = Array.from(tokenSet);

    let fcmResult = { successCount: 0, failureCount: 0 };
    if (tokens.length > 0) {
      try {
        const resp = await getFirebaseAdmin()
          .messaging()
          .sendEachForMulticast({
            tokens,
            android: {
              priority: "high",
              ttl: 30000,
            },
            data: {
              type: "call",
              callId: String(callId),
              familyId: String(familyId),
              callerId: String(uid),
              callerName: safeCallerName,
              callType: String(cleanType),
              roomName: String(roomName),
              title: `Incoming ${cleanType === "VIDEO" ? "Video" : "Voice"} Call`,
              body: `Incoming ${cleanType === "VIDEO" ? "Video" : "Voice"} Call from ${safeCallerName}`,
            },
          });
        fcmResult = { successCount: resp.successCount, failureCount: resp.failureCount };
        if (resp.failureCount > 0) {
          const invalidTokens = [];
          const errs = [];
          resp.responses.forEach((r, idx) => {
            if (!r.success) {
              const code = r.error?.code || r.error?.message || "unknown";
              if (errs.length < 3) errs.push(code);
              if (
                code === "messaging/registration-token-not-registered" ||
                code === "messaging/invalid-registration-token"
              ) {
                invalidTokens.push(tokens[idx]);
              }
            }
          });
          console.error(`[CALL] FCM multicast partial failure callId=${callId}:`, errs);
          if (invalidTokens.length > 0) {
            const memberRef = firestore
              .collection("families")
              .doc(familyId)
              .collection("members")
              .doc(targetMemberId);
            const cleanup = {
              fcmTokens: admin.firestore.FieldValue.arrayRemove(...invalidTokens),
            };
            if (invalidTokens.includes(receiver.fcmToken)) {
              cleanup.fcmToken = null;
            }
            await memberRef.update(cleanup).catch(() => {});
          }
        }
      } catch (fcmErr) {
        console.error(`[CALL] FCM multicast error callId=${callId}:`, fcmErr?.message || fcmErr);
      }
    }

    // Generate LiveKit token immediately to eliminate a 2nd network roundtrip
    let livekit = null;
    try {
      const scopedRoomName = `${familyId}_${roomName}`;
      const participantIdentity = `${familyId}_${uid}`;
      livekit = await generateLiveKitToken(participantIdentity, safeCallerName, scopedRoomName);
    } catch (lkErr) {
      console.error("[CALL] LiveKit token generation failed during initiate:", lkErr?.message || lkErr);
      // Fallback: client can still request /api/livekit/token if needed
    }

    console.log(
      `[CALL] initiate family=${familyId} caller=${uid} receiver=${targetMemberId} type=${cleanType} callId=${callId} fcmTokens=${tokens.length} fcmSent=${fcmResult.successCount}/${tokens.length} livekit=${Boolean(livekit?.token)}`
    );

    return res.status(201).json({
      success: true,
      callId,
      roomName,
      status: "OFFERING",
      livekitToken: livekit?.token || null,
      livekitUrl: livekit?.url || null,
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
