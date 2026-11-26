import bcrypt from "bcryptjs";
import { v4 as uuidv4 } from "uuid";
import { getFirestore } from "../../lib/firebase-admin.js";
import { signToken } from "../../lib/auth-middleware.js";
import {
  validateFamilyId,
  validateName,
  validatePassword,
  collectErrors,
} from "../../lib/validators.js";

/**
 * POST /api/family/join
 *
 * Allows an invited member to register with Family ID + Invite Code + Name + Password.
 * - Verifies invite code exists, is active, not expired, and under maxUses
 * - Prevents duplicate member names in the same family
 * - Hashes password with bcryptjs
 * - Sets status to PENDING if requireAdminApproval is true, else APPROVED
 */
export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  try {
    const {
      familyId,
      inviteCode,
      name,
      relationship = "Family Member",
      role = "MEMBER",
      password,
    } = req.body || {};

    const errors = collectErrors([
      validateFamilyId(familyId),
      validateName(name, "Your name"),
      validatePassword(password),
      !inviteCode || typeof inviteCode !== "string" || inviteCode.trim().length < 4
        ? "A valid invitation code is required."
        : null,
    ]);

    if (errors) {
      return res.status(400).json({ success: false, errors });
    }

    const cleanFamilyId = familyId.trim().toUpperCase();
    const cleanCode = inviteCode.trim().toUpperCase();
    const cleanName = name.trim();
    const firestore = getFirestore();

    const familyRef = firestore.collection("families").doc(cleanFamilyId);
    const familyDoc = await familyRef.get();

    if (!familyDoc.exists) {
      return res.status(404).json({
        success: false,
        error: `Family Space "${cleanFamilyId}" not found.`,
      });
    }

    const familyData = familyDoc.data();

    // Verify invite code
    const inviteRef = familyRef.collection("invites").doc(cleanCode);
    const inviteDoc = await inviteRef.get();

    if (!inviteDoc.exists) {
      return res.status(400).json({
        success: false,
        error: "Invalid invitation code.",
      });
    }

    const inviteData = inviteDoc.data();
    if (!inviteData.active) {
      return res.status(400).json({
        success: false,
        error: "This invitation code has been revoked.",
      });
    }

    if (inviteData.expiresAt && new Date(inviteData.expiresAt).getTime() < Date.now()) {
      return res.status(400).json({
        success: false,
        error: "This invitation code has expired. Ask your Family Admin for a new one.",
      });
    }

    if ((inviteData.usedCount || 0) >= (inviteData.maxUses || 1)) {
      return res.status(400).json({
        success: false,
        error: "This invitation code has reached its maximum usage limit.",
      });
    }

    // Prevent duplicate member name in the same family
    const existingMembersSnap = await familyRef.collection("members").get();
    const duplicate = existingMembersSnap.docs.some((doc) => {
      const existingName = doc.data().name || "";
      return existingName.trim().toLowerCase() === cleanName.toLowerCase();
    });

    if (duplicate) {
      return res.status(409).json({
        success: false,
        error: `A member named "${cleanName}" already exists in ${cleanFamilyId}. Use a distinct name or log in instead.`,
      });
    }

    const allowedRoles = ["PARENT", "SIBLING", "GRANDPARENT", "MEMBER"];
    const resolvedRole = allowedRoles.includes(role.toUpperCase()) ? role.toUpperCase() : "MEMBER";

    const memberUid = `mem_${uuidv4().replace(/-/g, "").substring(0, 20)}`;
    const memberId = `${cleanName.replace(/[^A-Za-z0-9]/g, "").toUpperCase().substring(0, 6)}-${Math.floor(100 + Math.random() * 900)}`;
    const passwordHash = await bcrypt.hash(password, 10);
    const needsApproval = familyData.requireAdminApproval ?? true;
    const status = needsApproval ? "PENDING" : "APPROVED";
    const now = new Date().toISOString();

    const memberData = {
      uid: memberUid,
      memberId,
      name: cleanName,
      relationship: relationship.trim() || "Family Member",
      role: resolvedRole,
      status,
      onlineStatus: status === "APPROVED" ? "AVAILABLE" : "OFFLINE",
      statusNote: status === "APPROVED" ? "Joined family space" : "Waiting for admin approval",
      initials: cleanName.substring(0, 2).toUpperCase(),
      avatarColorIndex: cleanName.split("").reduce((acc, ch) => acc + ch.charCodeAt(0), 0) % 6,
      passwordHash,
      lastSeen: now,
      createdAt: now,
      joinedViaCode: cleanCode,
    };

    const batch = firestore.batch();
    batch.set(familyRef.collection("members").doc(memberUid), memberData);
    batch.update(inviteRef, { usedCount: (inviteData.usedCount || 0) + 1 });
    await batch.commit();

    const token = status === "APPROVED" ? signToken(memberUid, cleanFamilyId, resolvedRole) : null;

    return res.status(201).json({
      success: true,
      userId: memberUid,
      memberId,
      familyId: cleanFamilyId,
      familyName: familyData.familyName || "Family Space",
      status,
      requiresApproval: needsApproval,
      token,
      message: needsApproval
        ? "Join request sent! Ask your Family Admin to approve your request, then log in."
        : "Welcome to the family!",
    });
  } catch (err) {
    console.error("Join family error:", err);
    return res.status(500).json({
      success: false,
      error: "Failed to process join request.",
    });
  }
}
