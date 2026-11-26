import jwt from "jsonwebtoken";
import { getFirestore } from "./firebase-admin.js";

/**
 * Express middleware that verifies the Authorization: Bearer <token> header.
 * On success, attaches `req.user` with { uid, familyId, role }.
 */
export function requireAuth(req, res, next) {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({
      success: false,
      error: "Authentication required. Provide a Bearer token.",
    });
  }

  const token = authHeader.split(" ")[1];

  if (!token) {
    return res.status(401).json({
      success: false,
      error: "Malformed Authorization header.",
    });
  }

  const secret = process.env.JWT_SECRET?.trim();
  if (!secret) {
    console.error("JWT_SECRET environment variable is not set.");
    return res.status(500).json({
      success: false,
      error: "Server configuration error.",
    });
  }

  try {
    const decoded = jwt.verify(token, secret);
    req.user = {
      uid: decoded.uid,
      familyId: decoded.familyId,
      role: decoded.role,
    };
    next();
  } catch (err) {
    if (err.name === "TokenExpiredError") {
      return res.status(401).json({
        success: false,
        error: "Token expired. Please log in again.",
      });
    }
    return res.status(401).json({
      success: false,
      error: "Invalid authentication token.",
    });
  }
}

/**
 * Middleware that verifies the authenticated user is still an APPROVED member
 * of their family in Firestore (never trusting client-supplied roles).
 */
export function requireApprovedMember(handler) {
  return (req, res) => {
    requireAuth(req, res, async () => {
      try {
        const { uid, familyId } = req.user;
        const firestore = getFirestore();
        const memberDoc = await firestore
          .collection("families")
          .doc(familyId)
          .collection("members")
          .doc(uid)
          .get();

        if (!memberDoc.exists) {
          return res.status(403).json({
            success: false,
            error: "Membership record not found. You may have been removed from this family.",
          });
        }

        const memberData = memberDoc.data();
        if (memberData.status !== "APPROVED") {
          return res.status(403).json({
            success: false,
            error: `Your membership status is ${memberData.status}. Admin approval is required.`,
          });
        }

        // Always use authoritative role and name from Firestore
        req.user.role = memberData.role || "MEMBER";
        req.user.name = memberData.name || "Family Member";
        return handler(req, res);
      } catch (err) {
        console.error("Membership verification error:", err);
        return res.status(500).json({
          success: false,
          error: "Failed to verify family membership.",
        });
      }
    });
  };
}

/**
 * Creates a signed JWT for the given user.
 */
export function signToken(uid, familyId, role) {
  const secret = process.env.JWT_SECRET?.trim();
  const rawExpiry = process.env.JWT_EXPIRY?.trim()?.replace(/^["']|["']$/g, "");
  const expiry = rawExpiry && /^\d+[smhd]$/.test(rawExpiry) ? rawExpiry : "24h";

  if (!secret) {
    throw new Error("JWT_SECRET environment variable is not set.");
  }

  return jwt.sign({ uid, familyId, role }, secret, { expiresIn: expiry });
}
