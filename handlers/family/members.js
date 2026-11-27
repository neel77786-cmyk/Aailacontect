import { getFirestore } from "../../lib/firebase-admin.js";
import { requireApprovedMember } from "../../lib/auth-middleware.js";
import { validateName } from "../../lib/validators.js";
import { v4 as uuidv4 } from "uuid";

const heartbeatCache = new Map();
const HEARTBEAT_TTL_MS = 45_000;

/**
 * /api/family/members
 *
 * GET  - List approved (and for Admin, pending) members of the authenticated user's family
 * POST - Admin: add a new family member
 */
async function membersHandler(req, res) {
  const { uid, familyId, role } = req.user;
  const firestore = getFirestore();

  if (req.method === "GET") {
    try {
      const membersSnapshot = await firestore
        .collection("families")
        .doc(familyId)
        .collection("members")
        .get();

      const nowMs = Date.now();
      const nowIso = new Date(nowMs).toISOString();

      // Throttle heartbeat writes to once every 45s to conserve Firestore write quota
      const hbKey = `${familyId}:${uid}`;
      const lastHb = heartbeatCache.get(hbKey) || 0;
      if (nowMs - lastHb >= HEARTBEAT_TTL_MS) {
        heartbeatCache.set(hbKey, nowMs);
        firestore
          .collection("families")
          .doc(familyId)
          .collection("members")
          .doc(uid)
          .update({ onlineStatus: "AVAILABLE", lastSeen: nowIso })
          .catch(() => {});
      }

      const allMembers = membersSnapshot.docs.map((doc) => {
        const data = doc.data();
        const isCaller = doc.id === uid;
        const lastSeenMs = isCaller ? nowMs : (data.lastSeen ? new Date(data.lastSeen).getTime() : 0);
        const isRecentlyActive = nowMs - lastSeenMs < 120_000;
        const computedOnline = isCaller
          ? "AVAILABLE"
          : data.onlineStatus === "AVAILABLE" && !isRecentlyActive
            ? "OFFLINE"
            : (data.onlineStatus || "OFFLINE");

        return {
          id: doc.id,
          memberId: data.memberId || doc.id,
          name: data.name || "Family Member",
          relationship: data.relationship || "Member",
          role: data.role || "MEMBER",
          status: data.status || "PENDING",
          onlineStatus: computedOnline,
          statusNote: data.statusNote || "",
          initials: data.initials || "??",
          avatarColorIndex: data.avatarColorIndex || 0,
          photoUrl: data.photoUrl || null,
          lastSeen: data.lastSeen || null,
        };
      });

      // Only APPROVED members are visible in the directory; PENDING are returned separately for Admin
      const approvedMembers = allMembers.filter((m) => m.status === "APPROVED");
      const pendingMembers = role === "ADMIN" ? allMembers.filter((m) => m.status === "PENDING") : [];

      let invites = [];
      if (role === "ADMIN") {
        const invitesSnap = await firestore
          .collection("families")
          .doc(familyId)
          .collection("invites")
          .get();
        invites = invitesSnap.docs.map((d) => d.data());
      }

      return res.status(200).json({
        success: true,
        familyId,
        members: approvedMembers,
        pendingMembers,
        invites,
        count: approvedMembers.length,
      });
    } catch (err) {
      console.error("Error listing members:", err);
      return res.status(500).json({
        success: false,
        error: "Failed to fetch family members.",
      });
    }
  }

  if (req.method === "POST") {
    if (role !== "ADMIN") {
      return res.status(403).json({
        success: false,
        error: "Only family admins can add members.",
      });
    }

    const { name, relationship, memberRole = "MEMBER" } = req.body || {};

    const nameError = validateName(name, "Member name");
    if (nameError) {
      return res.status(400).json({ success: false, error: nameError });
    }

    const relError = validateName(relationship, "Relationship");
    if (relError) {
      return res.status(400).json({ success: false, error: relError });
    }

    const allowedRoles = ["ADMIN", "PARENT", "SIBLING", "GRANDPARENT", "MEMBER"];
    if (!allowedRoles.includes(memberRole.toUpperCase())) {
      return res.status(400).json({
        success: false,
        error: `Invalid role. Must be one of: ${allowedRoles.join(", ")}`,
      });
    }

    try {
      const memberUid = uuidv4();
      const now = new Date().toISOString();
      const initials = name.trim().substring(0, 2).toUpperCase();

      const memberData = {
        uid: memberUid,
        name: name.trim(),
        relationship: relationship.trim(),
        role: memberRole.toUpperCase(),
        status: "APPROVED",
        onlineStatus: "OFFLINE",
        statusNote: "Joined family space",
        initials,
        avatarColorIndex: name.trim().split("").reduce((acc, ch) => acc + ch.charCodeAt(0), 0) % 6,
        lastSeen: now,
        createdAt: now,
        addedBy: uid,
      };

      await firestore
        .collection("families")
        .doc(familyId)
        .collection("members")
        .doc(memberUid)
        .set(memberData);

      return res.status(201).json({
        success: true,
        member: {
          id: memberUid,
          ...memberData,
        },
      });
    } catch (err) {
      console.error("Error adding member:", err);
      return res.status(500).json({
        success: false,
        error: "Failed to add family member.",
      });
    }
  }

  res.setHeader("Allow", "GET, POST");
  return res.status(405).json({ success: false, error: "Method not allowed." });
}

export default requireApprovedMember(membersHandler);
