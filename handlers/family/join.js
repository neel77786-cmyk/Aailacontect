import admin from "firebase-admin";
import bcrypt from "bcryptjs";
import { v4 as uuidv4 } from "uuid";
import { getFirestore } from "../../lib/firebase-admin.js";
import { requireAuth, signToken } from "../../lib/auth-middleware.js";
import {
  validateFamilyId,
  validateName,
  validatePassword,
  collectErrors,
} from "../../lib/validators.js";

/**
 * POST /api/family/join
 *
 * Supports TWO flows:
 * 1. Authenticated user joining another family with ONLY `familyJoinId` (no new account needed!)
 *    - Preserves ownership of their own Family Space
 *    - Sends a join request (status PENDING if requireAdminApproval, else APPROVED)
 * 2. Legacy invite-code registration flow (preserved for backward compatibility)
 */
export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ success: false, error: "Method not allowed." });
  }

  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith("Bearer ")) {
    return requireAuth(req, res, () => handleAuthenticatedJoin(req, res));
  }

  return handleLegacyInviteJoin(req, res);
}

async function handleAuthenticatedJoin(req, res) {
  try {
    const { uid, familyId: currentFamilyId } = req.user;
    const rawTarget = req.body?.familyJoinId || req.body?.familyId;
    const err = validateFamilyId(rawTarget);
    if (err) {
      return res.status(400).json({ success: false, error: err });
    }

    const targetFamilyId = rawTarget.trim().toUpperCase();
    if (targetFamilyId === currentFamilyId) {
      return res.status(400).json({
        success: false,
        error: "You are already a member of this Family Space.",
      });
    }

    const firestore = getFirestore();
    const targetFamilyRef = firestore.collection("families").doc(targetFamilyId);
    const targetFamilyDoc = await targetFamilyRef.get();

    if (!targetFamilyDoc.exists) {
      return res.status(404).json({
        success: false,
        error: `Family Join ID "${targetFamilyId}" does not exist.`,
      });
    }

    const targetFamilyData = targetFamilyDoc.data();

    // Check if already a member or pending in target family
    const existingMemberDoc = await targetFamilyRef.collection("members").doc(uid).get();
    if (existingMemberDoc.exists) {
      const existingStatus = existingMemberDoc.data().status;
      return res.status(409).json({
        success: false,
        error:
          existingStatus === "PENDING"
            ? `Your join request to "${targetFamilyData.familyName}" is already pending admin approval.`
            : `You are already a member of "${targetFamilyData.familyName}".`,
      });
    }

    // Look up caller's profile from current family or /users
    const currentMemberDoc = await firestore
      .collection("families")
      .doc(currentFamilyId)
      .collection("members")
      .doc(uid)
      .get();

    const callerData = currentMemberDoc.exists ? currentMemberDoc.data() : {};
    const callerName = callerData.name || "Family Member";
    const userCode = callerData.userCode || uid;

    const needsApproval = targetFamilyData.requireAdminApproval ?? true;
    const status = needsApproval ? "PENDING" : "APPROVED";
    const now = new Date().toISOString();

    await targetFamilyRef.collection("members").doc(uid).set({
      uid,
      userCode,
      memberId: userCode,
      name: callerName,
      relationship: "Family Member",
      role: "MEMBER",
      status,
      onlineStatus: status === "APPROVED" ? "AVAILABLE" : "OFFLINE",
      statusNote: status === "APPROVED" ? "Joined family space" : "Waiting for admin approval",
      initials: callerName.substring(0, 2).toUpperCase(),
      avatarColorIndex: callerName.split("").reduce((acc, ch) => acc + ch.charCodeAt(0), 0) % 6,
      lastSeen: now,
      createdAt: now,
    });

    if (userCode.startsWith("USR-")) {
      await firestore
        .collection("users")
        .doc(userCode)
        .set(
          { families: admin.firestore.FieldValue.arrayUnion(targetFamilyId) },
          { merge: true }
        )
        .catch(() => {});
    }

    return res.status(201).json({
      success: true,
      userId: userCode,
      familyId: targetFamilyId,
      familyName: targetFamilyData.familyName || "Family Space",
      status,
      requiresApproval: needsApproval,
      message: needsApproval
        ? `Join request sent to ${targetFamilyData.familyName || targetFamilyId}! Waiting for admin approval.`
        : `You joined ${targetFamilyData.familyName || targetFamilyId}!`,
    });
  } catch (err) {
    console.error("Authenticated join error:", err);
    return res.status(500).json({
      success: false,
      error: "Failed to send join request.",
    });
  }
}

async function handleLegacyInviteJoin(req, res) {
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
    const inviteRef = familyRef.collection("invites").doc(cleanCode);
    const inviteDoc = await inviteRef.get();
    if (!inviteDoc.exists || !inviteDoc.data().active) {
      return res.status(400).json({ success: false, error: "Invalid or revoked invitation code." });
    }

    const inviteData = inviteDoc.data();
    if (inviteData.expiresAt && new Date(inviteData.expiresAt).getTime() < Date.now()) {
      return res.status(400).json({ success: false, error: "Invitation code expired." });
    }

    const memberUid = `mem_${uuidv4().replace(/-/g, "").substring(0, 20)}`;
    const passwordHash = await bcrypt.hash(password, 10);
    const needsApproval = familyData.requireAdminApproval ?? true;
    const status = needsApproval ? "PENDING" : "APPROVED";
    const now = new Date().toISOString();

    const memberData = {
      uid: memberUid,
      memberId: memberUid,
      name: cleanName,
      relationship: relationship.trim() || "Family Member",
      role: "MEMBER",
      status,
      onlineStatus: status === "APPROVED" ? "AVAILABLE" : "OFFLINE",
      statusNote: status === "APPROVED" ? "Joined family space" : "Waiting for admin approval",
      initials: cleanName.substring(0, 2).toUpperCase(),
      avatarColorIndex: cleanName.split("").reduce((acc, ch) => acc + ch.charCodeAt(0), 0) % 6,
      passwordHash,
      lastSeen: now,
      createdAt: now,
    };

    const batch = firestore.batch();
    batch.set(familyRef.collection("members").doc(memberUid), memberData);
    batch.update(inviteRef, { usedCount: (inviteData.usedCount || 0) + 1 });
    await batch.commit();

    const token = status === "APPROVED" ? signToken(memberUid, cleanFamilyId, "MEMBER") : null;
    return res.status(201).json({
      success: true,
      userId: memberUid,
      familyId: cleanFamilyId,
      familyName: familyData.familyName || "Family Space",
      status,
      requiresApproval: needsApproval,
      token,
      message: needsApproval
        ? "Join request sent! Ask your Family Admin to approve your request."
        : "Welcome to the family!",
    });
  } catch (err) {
    console.error("Join family error:", err);
    return res.status(500).json({ success: false, error: "Failed to process join request." });
  }
}
