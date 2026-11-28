import jwt from "jsonwebtoken";
import { getFirestore } from "../../lib/firebase-admin.js";
import { signToken } from "../../lib/auth-middleware.js";

/**
 * POST /api/auth/refresh
 *
 * Refreshes an expired (or active) validly-signed JWT without requiring the user
 * to re-enter credentials, as long as their membership still exists in Firestore.
 */
export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({
      success: false,
      error: "Authentication required. Provide a Bearer token.",
    });
  }

  const token = authHeader.split(" ")[1];
  const secret = process.env.JWT_SECRET?.trim();
  if (!token || !secret) {
    return res.status(401).json({
      success: false,
      error: "Invalid authentication token.",
    });
  }

  let decoded;
  try {
    decoded = jwt.verify(token, secret, { ignoreExpiration: true });
  } catch (_err) {
    return res.status(401).json({
      success: false,
      error: "Invalid authentication token.",
    });
  }

  const { uid, familyId } = decoded || {};
  if (!uid || !familyId) {
    return res.status(401).json({
      success: false,
      error: "Malformed authentication token.",
    });
  }

  try {
    const firestore = getFirestore();
    const memberRef = firestore
      .collection("families")
      .doc(familyId)
      .collection("members")
      .doc(uid);
    const memberDoc = await memberRef.get();

    if (!memberDoc.exists) {
      return res.status(403).json({
        success: false,
        error: "Membership record not found. You may have been removed by an Admin.",
      });
    }

    const memberData = memberDoc.data();
    const role = memberData.role || "MEMBER";
    const freshToken = signToken(uid, familyId, role);

    await memberRef
      .update({
        onlineStatus: "AVAILABLE",
        lastSeen: new Date().toISOString(),
      })
      .catch(() => {});

    return res.status(200).json({
      success: true,
      userId: memberData.userCode || uid,
      authUid: uid,
      familyId,
      role,
      memberName: memberData.name || "Member",
      token: freshToken,
    });
  } catch (err) {
    console.error("Token refresh error:", err);
    return res.status(500).json({
      success: false,
      error: "Failed to refresh authentication token.",
    });
  }
}
