import { getFirestore } from "../../lib/firebase-admin.js";
import { requireAuth } from "../../lib/auth-middleware.js";
import { v4 as uuidv4 } from "uuid";

/**
 * POST /api/calls/initiate
 *
 * Creates a new call document in Firestore for signaling.
 */
async function initiateHandler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const { uid, familyId } = req.user;
    const { targetMemberId, callType = "VOICE" } = req.body || {};

    if (!targetMemberId) {
      return res.status(400).json({
        success: false,
        error: "targetMemberId is required.",
      });
    }

    const allowedTypes = ["VOICE", "VIDEO"];
    if (!allowedTypes.includes(callType.toUpperCase())) {
      return res.status(400).json({
        success: false,
        error: "callType must be VOICE or VIDEO.",
      });
    }

    const firestore = getFirestore();

    // Verify target member exists
    const memberDoc = await firestore
      .collection("families")
      .doc(familyId)
      .collection("members")
      .doc(targetMemberId)
      .get();

    if (!memberDoc.exists) {
      return res.status(404).json({
        success: false,
        error: "Target family member not found.",
      });
    }

    const callId = uuidv4();
    const now = new Date().toISOString();

    const callData = {
      callId,
      familyId,
      callerUid: uid,
      targetMemberId,
      callType: callType.toUpperCase(),
      status: "OFFERING",
      direction: "OUTGOING",
      createdAt: now,
    };

    await firestore
      .collection("families")
      .doc(familyId)
      .collection("calls")
      .doc(callId)
      .set(callData);

    return res.status(201).json({
      success: true,
      callId,
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

export default function handler(req, res) {
  requireAuth(req, res, () => initiateHandler(req, res));
}
