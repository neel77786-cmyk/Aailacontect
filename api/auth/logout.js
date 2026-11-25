import { requireAuth } from "../../lib/auth-middleware.js";

/**
 * POST /api/auth/logout
 *
 * Client-side token invalidation acknowledgement.
 * The JWT is stateless, so the client simply discards the token.
 * This endpoint exists so the Android app has a clean logout contract.
 */
async function logoutHandler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  // In a stateless JWT system, the server cannot revoke the token.
  // The client is responsible for deleting the stored token.
  // A future enhancement could add token blocklisting via Redis.
  return res.status(200).json({
    success: true,
    message: "Logged out successfully. Discard your token.",
  });
}

export default function handler(req, res) {
  requireAuth(req, res, () => logoutHandler(req, res));
}
