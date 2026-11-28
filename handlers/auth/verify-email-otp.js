import { verifyStoredEmailOtp } from "../../lib/email-otp.js";

/**
 * POST /api/auth/verify-email-otp
 * Body: { email, otp, purpose: "register" | "reset" }
 */
export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const { email, otp, purpose = "register" } = req.body || {};
    const cleanPurpose = purpose === "reset" ? "reset" : "register";
    const otpToken = await verifyStoredEmailOtp(email, otp, cleanPurpose);

    return res.status(200).json({
      success: true,
      otpToken,
      message: "Email verified successfully!",
    });
  } catch (err) {
    return res.status(err.statusCode || 500).json({
      success: false,
      error: err.message || "OTP verification failed.",
    });
  }
}
