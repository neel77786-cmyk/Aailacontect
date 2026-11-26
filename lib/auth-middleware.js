import jwt from "jsonwebtoken";

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

  const secret = process.env.JWT_SECRET;
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
