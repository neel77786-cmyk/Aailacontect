import { getFirestore } from "../../lib/firebase-admin.js";
import { requireAuth } from "../../lib/auth-middleware.js";
import { v4 as uuidv4 } from "uuid";

/**
 * /api/calls/history
 *
 * GET - Fetch call history for the authenticated user's family
 */
async function historyHandler(req, res) {
  if (req.method !== "GET") {
    res.setHeader("Allow", "GET");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const { familyId } = req.user;
    const firestore = getFirestore();

    const callsSnapshot = await firestore
      .collection("families")
      .doc(familyId)
      .collection("calls")
      .orderBy("createdAt", "desc")
      .limit(50)
      .get();

    const calls = callsSnapshot.docs.map((doc) => {
      const data = doc.data();
      return {
        id: doc.id,
        callId: data.callId || doc.id,
        targetMemberId: data.targetMemberId || "",
        callerName: data.callerName || "Unknown",
        callType: data.callType || "VOICE",
        status: data.status || "ENDED",
        direction: data.direction || "OUTGOING",
        createdAt: data.createdAt || null,
        durationSeconds: data.durationSeconds || 0,
      };
    });

    return res.status(200).json({
      success: true,
      familyId,
      calls,
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

export default function handler(req, res) {
  requireAuth(req, res, () => historyHandler(req, res));
}
