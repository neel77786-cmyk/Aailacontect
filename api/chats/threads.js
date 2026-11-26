import { getFirestore } from "../../lib/firebase-admin.js";
import { requireApprovedMember } from "../../lib/auth-middleware.js";

/**
 * /api/chats/threads
 *
 * GET  - List all chat threads for the family (auto-creates Family Circle if missing)
 * POST - Get or create a 1-to-1 private chat thread with another approved family member
 */
async function threadsHandler(req, res) {
  const { uid, familyId } = req.user;
  const firestore = getFirestore();
  const familyRef = firestore.collection("families").doc(familyId);
  const threadsRef = familyRef.collection("threads");

  if (req.method === "GET") {
    try {
      const snap = await threadsRef.orderBy("updatedAt", "desc").get();
      let threads = snap.docs
        .map((doc) => ({ id: doc.id, ...doc.data() }))
        .filter((t) => t.isGroup || !Array.isArray(t.participants) || t.participants.includes(uid));

      // Ensure the default Family Circle group thread exists for older families
      if (!threads.some((t) => t.id === "thread_family_circle")) {
        const familyDoc = await familyRef.get();
        const familyName = familyDoc.exists ? familyDoc.data().familyName || "Family" : "Family";
        const now = new Date().toISOString();
        const defaultThread = {
          id: "thread_family_circle",
          title: `${familyName} Circle`,
          subtitle: "Private Family Group",
          memberId: null,
          isGroup: true,
          initials: "FC",
          lastMessagePreview: "Start chatting with your family!",
          updatedAt: now,
          createdAt: now,
        };
        await threadsRef.doc("thread_family_circle").set(defaultThread);
        threads = [defaultThread, ...threads];
      }

      const formatted = threads.map((t) => ({
        id: t.id,
        title: t.title || "Family Chat",
        subtitle: t.subtitle || (t.isGroup ? "Private Family Group" : "Direct Message"),
        memberId: t.isGroup ? null : (Array.isArray(t.participants) ? t.participants.find((p) => p !== uid) : t.memberId),
        isGroup: Boolean(t.isGroup),
        initials: t.initials || (t.title ? t.title.substring(0, 2).toUpperCase() : "FC"),
        lastMessagePreview: t.lastMessagePreview || "",
        updatedAt: t.updatedAt || null,
        unreadCount: Number(t.unreadBy?.[uid] || 0),
      }));

      return res.status(200).json({ success: true, threads: formatted });
    } catch (err) {
      console.error("Error loading threads:", err);
      return res.status(500).json({ success: false, error: "Failed to load chat threads." });
    }
  }

  if (req.method === "POST") {
    const { targetMemberId } = req.body || {};
    if (!targetMemberId) {
      return res.status(400).json({ success: false, error: "targetMemberId is required." });
    }

    try {
      const targetDoc = await familyRef.collection("members").doc(targetMemberId).get();
      if (!targetDoc.exists || targetDoc.data().status !== "APPROVED") {
        return res.status(404).json({ success: false, error: "Approved member not found." });
      }
      const target = targetDoc.data();
      const sorted = [uid, targetMemberId].sort();
      const threadId = `dm_${sorted[0]}_${sorted[1]}`;

      const existing = await threadsRef.doc(threadId).get();
      if (existing.exists) {
        return res.status(200).json({ success: true, thread: { id: threadId, ...existing.data() } });
      }

      const now = new Date().toISOString();
      const threadData = {
        id: threadId,
        title: target.name || "Family Member",
        subtitle: target.relationship || "Private Chat",
        memberId: targetMemberId,
        participants: sorted,
        isGroup: false,
        initials: target.initials || "FM",
        lastMessagePreview: "Private conversation started",
        updatedAt: now,
        createdAt: now,
      };
      await threadsRef.doc(threadId).set(threadData);

      return res.status(201).json({ success: true, thread: threadData });
    } catch (err) {
      console.error("Error creating thread:", err);
      return res.status(500).json({ success: false, error: "Failed to create chat thread." });
    }
  }

  res.setHeader("Allow", "GET, POST");
  return res.status(405).json({ success: false, error: "Method not allowed." });
}

export default requireApprovedMember(threadsHandler);
