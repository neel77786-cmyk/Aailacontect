import { requireApprovedMember } from "../../lib/auth-middleware.js";
import { generateLiveKitToken } from "../../lib/livekit.js";

/**
 * POST /api/livekit/token
 *
 * Generates a LiveKit access token ONLY for verified, APPROVED members of the family.
 */
async function tokenHandler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const { uid, familyId, name } = req.user;
    const { roomName, participantName } = req.body || {};

    if (!roomName || typeof roomName !== "string" || roomName.trim().length === 0) {
      return res.status(400).json({
        success: false,
        error: "roomName is required.",
      });
    }

    const displayName = participantName?.trim() || name || `User-${uid.substring(0, 6)}`;

    // Scope room name to the family for strict isolation
    const scopedRoomName = `${familyId}_${roomName.trim()}`;
    const participantIdentity = `${familyId}_${uid}`;

    const result = await generateLiveKitToken(
      participantIdentity,
      displayName,
      scopedRoomName
    );

    console.log(`[LIVEKIT] token family=${familyId} uid=${uid} room=${scopedRoomName}`);

    return res.status(200).json({
      success: true,
      token: result.token,
      url: result.url,
      room: scopedRoomName,
    });
  } catch (err) {
    console.error("Error generating LiveKit token:", err);

    if (err.message?.includes("Missing LiveKit")) {
      return res.status(503).json({
        success: false,
        error: "LiveKit is not configured on this server.",
      });
    }

    return res.status(500).json({
      success: false,
      error: "Failed to generate LiveKit token.",
    });
  }
}

export default requireApprovedMember(tokenHandler);
