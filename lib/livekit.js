import { AccessToken } from "livekit-server-sdk";

/**
 * Generates a LiveKit access token for a participant to join a room.
 *
 * @param {string} participantIdentity - Unique identity for the participant (e.g., Firebase UID)
 * @param {string} participantName - Display name in the room
 * @param {string} roomName - LiveKit room name to join
 * @returns {Promise<{token: string, url: string}>}
 */
export async function generateLiveKitToken(
  participantIdentity,
  participantName,
  roomName
) {
  const apiKey = process.env.LIVEKIT_API_KEY;
  const apiSecret = process.env.LIVEKIT_API_SECRET;
  const livekitUrl = process.env.LIVEKIT_URL;

  if (!apiKey || !apiSecret || !livekitUrl) {
    throw new Error(
      "Missing LiveKit environment variables. " +
        "Set LIVEKIT_API_KEY, LIVEKIT_API_SECRET, and LIVEKIT_URL."
    );
  }

  const token = new AccessToken(apiKey, apiSecret, {
    identity: participantIdentity,
    name: participantName,
    ttl: "1h",
  });

  token.addGrant({
    roomJoin: true,
    room: roomName,
    canPublish: true,
    canSubscribe: true,
    canPublishData: true,
  });

  const jwt = await token.toJwt();

  return {
    token: jwt,
    url: livekitUrl,
  };
}
