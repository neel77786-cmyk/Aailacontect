import bcrypt from "bcryptjs";
import { getAuth, getFirestore } from "../../lib/firebase-admin.js";
import { isValidEmail, normalizeEmail, verifyOtpProofToken } from "../../lib/email-otp.js";
import { validatePassword } from "../../lib/validators.js";

/**
 * POST /api/auth/reset-password
 *
 * Verifies an Email OTP proof token (`otpToken`), finds the user by email,
 * and updates their password across Firestore and Firebase Auth.
 */
export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const { email, otpToken, newPassword } = req.body || {};
    const cleanEmail = normalizeEmail(email);

    if (!isValidEmail(cleanEmail)) {
      return res.status(400).json({ success: false, error: "Please enter a valid email address." });
    }

    const pwdErr = validatePassword(newPassword);
    if (pwdErr) {
      return res.status(400).json({ success: false, error: pwdErr });
    }

    const verifiedEmail = verifyOtpProofToken(otpToken, cleanEmail, "reset");
    if (!verifiedEmail) {
      return res.status(401).json({
        success: false,
        error: "Email OTP verification expired or invalid. Please verify OTP again.",
      });
    }

    const firestore = getFirestore();
    const usersSnap = await firestore
      .collection("users")
      .where("email", "==", verifiedEmail)
      .limit(1)
      .get();

    if (usersSnap.empty) {
      return res.status(404).json({
        success: false,
        error: `No AAILA account found for ${verifiedEmail}.`,
      });
    }

    const userDoc = usersSnap.docs[0];
    const userData = userDoc.data();
    const userCode = userDoc.id;
    const uid = userData.uid;
    const ownFamilyId = userData.ownFamilyId;
    const passwordHash = await bcrypt.hash(newPassword, 10);
    const now = new Date().toISOString();

    await userDoc.ref.update({
      passwordHash,
      updatedAt: now,
    });

    if (uid && ownFamilyId) {
      await firestore
        .collection("families")
        .doc(ownFamilyId)
        .collection("members")
        .doc(uid)
        .update({ passwordHash, updatedAt: now })
        .catch(() => {});
    }

    if (uid) {
      await getAuth().updateUser(uid, { password: newPassword }).catch(() => {});
    }

    return res.status(200).json({
      success: true,
      userId: userCode,
      familyId: ownFamilyId,
      message: "Password reset successfully! You can now sign in with your new password.",
    });
  } catch (err) {
    console.error("Reset password error:", err);
    return res.status(500).json({
      success: false,
      error: "Failed to reset password.",
    });
  }
}
