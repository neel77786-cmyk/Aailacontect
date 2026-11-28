import { getFirestore } from "../../lib/firebase-admin.js";
import { generateAndSendEmailOtp, isValidEmail, normalizeEmail } from "../../lib/email-otp.js";

/**
 * POST /api/auth/send-email-otp
 * Body: { email, purpose: "register" | "reset" }
 */
export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const { email, purpose = "register" } = req.body || {};
    const cleanEmail = normalizeEmail(email);

    if (!isValidEmail(cleanEmail)) {
      return res.status(400).json({
        success: false,
        error: "Please enter a valid email address.",
      });
    }

    const cleanPurpose = purpose === "reset" ? "reset" : "register";
    const firestore = getFirestore();

    if (cleanPurpose === "reset") {
      const existing = await firestore
        .collection("users")
        .where("email", "==", cleanEmail)
        .limit(1)
        .get();
      if (existing.empty) {
        return res.status(404).json({
          success: false,
          error: `No AAILA account found for ${cleanEmail}.`,
        });
      }
    } else {
      const existing = await firestore
        .collection("users")
        .where("email", "==", cleanEmail)
        .limit(1)
        .get();
      if (!existing.empty) {
        return res.status(409).json({
          success: false,
          error: `An account already exists with ${cleanEmail}. Please sign in or use Forgot Password.`,
        });
      }
    }

    const result = await generateAndSendEmailOtp(cleanEmail, cleanPurpose);
    return res.status(200).json({
      success: true,
      email: result.email,
      message: `6-digit OTP sent to ${result.email}.`,
    });
  } catch (err) {
    console.error("Send email OTP error:", err);
    return res.status(err.statusCode || 500).json({
      success: false,
      error: err.message || "Failed to send OTP email.",
    });
  }
}
