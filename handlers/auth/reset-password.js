import bcrypt from "bcryptjs";
import { getAuth, getFirestore } from "../../lib/firebase-admin.js";
import { validatePassword } from "../../lib/validators.js";

function normalizePhone(raw) {
  if (typeof raw !== "string") return "";
  const cleaned = raw.replace(/[\s\-()]/g, "").trim();
  if (!cleaned) return "";
  if (cleaned.startsWith("+")) return cleaned;
  if (/^\d{10}$/.test(cleaned)) return `+91${cleaned}`;
  return `+${cleaned}`;
}

/**
 * POST /api/auth/reset-password
 *
 * Verifies a Firebase Phone Auth ID token (issued after SMS OTP verification),
 * finds the matching user by phoneNumber, and resets their password cleanly.
 */
export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const { phoneNumber, firebaseIdToken, newPassword } = req.body || {};

    const pwdErr = validatePassword(newPassword);
    if (pwdErr) {
      return res.status(400).json({ success: false, error: pwdErr });
    }

    if (!firebaseIdToken || typeof firebaseIdToken !== "string") {
      return res.status(400).json({
        success: false,
        error: "OTP verification is required before resetting password.",
      });
    }

    const auth = getAuth();
    let decoded;
    try {
      decoded = await auth.verifyIdToken(firebaseIdToken);
    } catch (_err) {
      return res.status(401).json({
        success: false,
        error: "OTP verification expired or invalid. Please verify OTP again.",
      });
    }

    const verifiedPhone = normalizePhone(decoded.phone_number || "");
    const requestedPhone = normalizePhone(phoneNumber || "");
    if (!verifiedPhone || (requestedPhone && verifiedPhone !== requestedPhone)) {
      return res.status(403).json({
        success: false,
        error: "Verified phone number does not match the entered phone number.",
      });
    }

    const firestore = getFirestore();
    const usersSnap = await firestore
      .collection("users")
      .where("phoneNumber", "==", verifiedPhone)
      .limit(1)
      .get();

    if (usersSnap.empty) {
      return res.status(404).json({
        success: false,
        error: `No AAILA account found with phone number ${verifiedPhone}.`,
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
      await auth.updateUser(uid, { password: newPassword }).catch(() => {});
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
