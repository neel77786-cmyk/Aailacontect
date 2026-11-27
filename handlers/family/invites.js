import crypto from "node:crypto";
import { getFirestore } from "../../lib/firebase-admin.js";
import { requireApprovedMember } from "../../lib/auth-middleware.js";

/**
 * /api/family/invites
 *
 * GET    - List active invite codes and pending join requests (Admin)
 * POST   - Generate a new secure invite code with expiry and usage limit (Admin)
 * DELETE - Revoke an invite code by ?code=... (Admin)
 */
async function invitesHandler(req, res) {
  const { uid, familyId, role } = req.user;
  const firestore = getFirestore();

  if (req.method === "GET") {
    try {
      const invitesSnap = await firestore
        .collection("families")
        .doc(familyId)
        .collection("invites")
        .get();

      const invites = invitesSnap.docs.map((doc) => doc.data());

      const membersSnap = await firestore
        .collection("families")
        .doc(familyId)
        .collection("members")
        .where("status", "==", "PENDING")
        .get();

      const pendingRequests = membersSnap.docs.map((doc) => {
        const d = doc.data();
        return {
          id: doc.id,
          name: d.name || "Member",
          relationship: d.relationship || "Family",
          role: d.role || "MEMBER",
          initials: d.initials || "??",
          createdAt: d.createdAt || null,
        };
      });

      return res.status(200).json({
        success: true,
        invites,
        pendingRequests,
      });
    } catch (err) {
      console.error("Error fetching invites:", err);
      return res.status(500).json({ success: false, error: "Failed to load invitations." });
    }
  }

  if (req.method === "POST") {
    if (role !== "ADMIN") {
      return res.status(403).json({
        success: false,
        error: "Only the Family Admin can generate invitation codes.",
      });
    }

    const { expiresInHours = 24 } = req.body || {};
    const parsedHours = Math.min(Math.max(Number(expiresInHours) || 24, 1), 168);

    try {
      const code = crypto.randomBytes(4).toString("hex").toUpperCase();
      const now = Date.now();
      const expiresAt = new Date(now + parsedHours * 3600 * 1000).toISOString();

      const inviteData = {
        code,
        familyId,
        createdBy: uid,
        createdAt: new Date(now).toISOString(),
        expiresAt,
        maxUses: 0,
        usedCount: 0,
        active: true,
      };

      await firestore
        .collection("families")
        .doc(familyId)
        .collection("invites")
        .doc(code)
        .set(inviteData);

      const baseUrl = process.env.PUBLIC_BACKEND_URL || "https://ailacontect.vercel.app";
      const inviteLink = `${baseUrl}/join?familyId=${encodeURIComponent(familyId)}&code=${encodeURIComponent(code)}`;

      return res.status(201).json({
        success: true,
        invite: {
          ...inviteData,
          inviteLink,
        },
      });
    } catch (err) {
      console.error("Error creating invite:", err);
      return res.status(500).json({ success: false, error: "Failed to generate invite." });
    }
  }

  if (req.method === "DELETE") {
    if (role !== "ADMIN") {
      return res.status(403).json({
        success: false,
        error: "Only the Family Admin can revoke invitations.",
      });
    }

    const code = req.query?.code?.toString().trim().toUpperCase();
    if (!code) {
      return res.status(400).json({ success: false, error: "Invite code is required." });
    }

    try {
      await firestore
        .collection("families")
        .doc(familyId)
        .collection("invites")
        .doc(code)
        .delete();

      return res.status(200).json({ success: true, message: "Invitation revoked." });
    } catch (err) {
      console.error("Error revoking invite:", err);
      return res.status(500).json({ success: false, error: "Failed to revoke invitation." });
    }
  }

  res.setHeader("Allow", "GET, POST, DELETE");
  return res.status(405).json({ success: false, error: "Method not allowed." });
}

export default requireApprovedMember(invitesHandler);
