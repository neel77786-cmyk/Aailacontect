import bcrypt from "bcryptjs";
import { getAuth, getFirestore } from "../../lib/firebase-admin.js";
import { requireApprovedMember } from "../../lib/auth-middleware.js";
import { validatePassword } from "../../lib/validators.js";

/**
 * POST /api/auth/password   -> Change current user's password
 * DELETE /api/auth/password -> Delete current user's account
 */
async function passwordHandler(req, res) {
  const { uid, familyId, role } = req.user;
  const firestore = getFirestore();
  const memberRef = firestore
    .collection("families")
    .doc(familyId)
    .collection("members")
    .doc(uid);

  if (req.method === "POST") {
    const { newPassword } = req.body || {};
    const pwdErr = validatePassword(newPassword);
    if (pwdErr) {
      return res.status(400).json({ success: false, error: pwdErr });
    }

    try {
      const memberDoc = await memberRef.get();
      if (!memberDoc.exists) {
        return res.status(404).json({ success: false, error: "Member not found." });
      }

      const memberData = memberDoc.data();
      if (memberData.passwordHash) {
        const hash = await bcrypt.hash(newPassword, 10);
        await memberRef.update({ passwordHash: hash, updatedAt: new Date().toISOString() });
      } else {
        // Admin Firebase Auth user
        await getAuth().updateUser(uid, { password: newPassword });
      }

      return res.status(200).json({
        success: true,
        message: "Password updated successfully.",
      });
    } catch (err) {
      console.error("Password change error:", err);
      return res.status(500).json({
        success: false,
        error: "Failed to update password.",
      });
    }
  }

  if (req.method === "DELETE") {
    try {
      const memberDoc = await memberRef.get();
      if (!memberDoc.exists) {
        return res.status(404).json({ success: false, error: "Account not found." });
      }
      const memberData = memberDoc.data();

      await memberRef.delete();

      if (!memberData.passwordHash) {
        // Admin Firebase Auth user
        await getAuth().deleteUser(uid).catch(() => {});
      }

      return res.status(200).json({
        success: true,
        message: "Account deleted successfully.",
      });
    } catch (err) {
      console.error("Delete account error:", err);
      return res.status(500).json({
        success: false,
        error: "Failed to delete account.",
      });
    }
  }

  res.setHeader("Allow", "POST, DELETE");
  return res.status(405).json({ success: false, error: "Method not allowed." });
}

export default requireApprovedMember(passwordHandler);
