import { getFirestore } from "../../lib/firebase-admin.js";
import { requireApprovedMember } from "../../lib/auth-middleware.js";
import { generateLiveKitToken } from "../../lib/livekit.js";

const userFamiliesCache = new Map();
const USER_FAMILIES_TTL_MS = 60_000;

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
      .limit(10)
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
          familyId,
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

    // Also check any other approved families this user belongs to (with 60s family-list cache)
    if (!activeIncomingCall) {
      try {
        const cacheKey = `${familyId}:${uid}`;
        let cached = userFamiliesCache.get(cacheKey);
        let famIds = cached && nowMs - cached.ts < USER_FAMILIES_TTL_MS ? cached.famIds : null;

        if (!famIds) {
          famIds = [];
          const memDoc = await firestore
            .collection("families")
            .doc(familyId)
            .collection("members")
            .doc(uid)
            .get();
          const userCode = memDoc.exists ? memDoc.data()?.userCode : null;
          if (userCode && userCode.startsWith("USR-")) {
            const userDoc = await firestore.collection("users").doc(userCode).get();
            famIds =
              userDoc.exists && Array.isArray(userDoc.data().families)
                ? userDoc.data().families.filter((f) => f && f !== familyId)
                : [];
          }
          userFamiliesCache.set(cacheKey, { famIds, ts: nowMs });
        }

        for (const otherFamId of famIds) {
          if (activeIncomingCall) break;
          const otherSnap = await firestore
            .collection("families")
            .doc(otherFamId)
            .collection("calls")
            .orderBy("createdAt", "desc")
            .limit(3)
            .get();
          for (const cDoc of otherSnap.docs) {
            const cData = cDoc.data();
            const cMs = cData.createdAt ? new Date(cData.createdAt).getTime() : 0;
            if (
              cData.receiverId === uid &&
              cData.status === "OFFERING" &&
              nowMs - cMs < 60_000
            ) {
              activeIncomingCall = {
                callId: cDoc.id,
                familyId: otherFamId,
                callerId: cData.callerId,
                callerName: cData.callerName || "Family Member",
                callType: cData.callType || "VOICE",
                roomName: cData.roomName || `call_${cDoc.id}`,
                status: cData.status,
                startTime: cData.startTime,
              };
              break;
            }
          }
        }
      } catch (_otherErr) {
        // Non-fatal: active family history still returns cleanly
      }
    }

    if (activeIncomingCall) {
      try {
        const targetFam = activeIncomingCall.familyId || familyId;
        const scopedRoomName = `${targetFam}_${activeIncomingCall.roomName}`;
        const participantIdentity = `${targetFam}_${uid}`;
        const lk = await generateLiveKitToken(
          participantIdentity,
          req.user.name || "Family Member",
          scopedRoomName
        );
        activeIncomingCall.livekitToken = lk?.token || null;
        activeIncomingCall.livekitUrl = lk?.url || null;
      } catch (_lkErr) {
        // Fallback: client can still request /api/livekit/token on accept
      }
      console.log(
        `[CALL] activeIncomingCall family=${activeIncomingCall.familyId || familyId} receiver=${uid} callId=${activeIncomingCall.callId} preToken=${Boolean(activeIncomingCall.livekitToken)}`
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
