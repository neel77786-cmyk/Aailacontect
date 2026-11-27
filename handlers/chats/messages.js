import { v4 as uuidv4 } from "uuid";
import { getFirebaseAdmin, getFirestore } from "../../lib/firebase-admin.js";
import { requireApprovedMember } from "../../lib/auth-middleware.js";

/**
 * /api/chats/messages
 *
 * GET  ?threadId=... -> Fetch messages for a thread and mark unread count as 0 for caller
 * POST               -> Send a text/photo/video/voice message and trigger FCM push
 */
async function messagesHandler(req, res) {
  const { uid, familyId, name } = req.user;
  const firestore = getFirestore();
  const familyRef = firestore.collection("families").doc(familyId);

  if (req.method === "GET") {
    const threadId = req.query?.threadId?.toString().trim();
    if (!threadId) {
      return res.status(400).json({ success: false, error: "threadId is required." });
    }

    try {
      const threadRef = familyRef.collection("threads").doc(threadId);
      const [snap] = await Promise.all([
        threadRef.collection("messages").orderBy("timestamp", "asc").limit(100).get(),
        threadRef.set({ unreadBy: { [uid]: 0 } }, { merge: true }).catch(() => {}),
      ]);

      const messages = snap.docs.map((doc) => {
        const d = doc.data();
        return {
          id: doc.id,
          threadId,
          senderId: d.senderId || "",
          senderName: d.senderName || "Family Member",
          isOutgoing: d.senderId === uid,
          body: d.body || "",
          mediaType: d.mediaType || "TEXT",
          mediaUrl: d.mediaUrl || null,
          timestamp: d.timestamp || null,
          status: d.status || "DELIVERED",
        };
      });

      return res.status(200).json({ success: true, threadId, messages });
    } catch (err) {
      console.error("Error fetching messages:", err);
      return res.status(500).json({ success: false, error: "Failed to fetch messages." });
    }
  }

  if (req.method === "POST") {
    const { threadId, body, mediaType = "TEXT", mediaUrl = null } = req.body || {};
    if (!threadId || typeof body !== "string" || body.trim().length === 0) {
      return res.status(400).json({
        success: false,
        error: "threadId and non-empty message body are required.",
      });
    }

    try {
      const cleanBody = body.trim();
      const msgId = uuidv4();
      const now = new Date().toISOString();

      const msgData = {
        id: msgId,
        threadId,
        senderId: uid,
        senderName: name,
        body: cleanBody,
        mediaType: mediaType.toUpperCase(),
        mediaUrl,
        timestamp: now,
        status: "DELIVERED",
      };

      const threadRef = familyRef.collection("threads").doc(threadId);

      // Fetch approved members and thread doc in parallel
      const [membersSnap, threadDoc] = await Promise.all([
        familyRef.collection("members").where("status", "==", "APPROVED").get(),
        threadRef.get(),
      ]);

      const existingUnread = threadDoc.exists ? threadDoc.data().unreadBy || {} : {};
      const updatedUnread = { ...existingUnread, [uid]: 0 };
      const tokenSet = new Set();

      for (const mDoc of membersSnap.docs) {
        if (mDoc.id !== uid) {
          updatedUnread[mDoc.id] = Number(updatedUnread[mDoc.id] || 0) + 1;
          const mData = mDoc.data();
          if (mData.fcmToken) tokenSet.add(mData.fcmToken);
          if (Array.isArray(mData.fcmTokens)) {
            mData.fcmTokens.forEach((t) => {
              if (typeof t === "string" && t.trim().length > 0) tokenSet.add(t.trim());
            });
          }
        }
      }

      // Write message and update thread preview in parallel
      await Promise.all([
        threadRef.collection("messages").doc(msgId).set(msgData),
        threadRef.set(
          {
            lastMessagePreview: `${name}: ${cleanBody}`,
            updatedAt: now,
            unreadBy: updatedUnread,
          },
          { merge: true }
        ),
      ]);

      // Best-effort FCM push notification
      const tokens = Array.from(tokenSet);
      if (tokens.length > 0) {
        await getFirebaseAdmin()
          .messaging()
          .sendEachForMulticast({
            tokens,
            notification: {
              title: `${name} · AAILA`,
              body: cleanBody.substring(0, 120),
            },
            data: {
              type: "message",
              threadId,
              title: `${name} · AAILA`,
              body: cleanBody.substring(0, 120),
            },
          })
          .catch(() => {});
      }

      return res.status(201).json({
        success: true,
        message: {
          ...msgData,
          isOutgoing: true,
        },
      });
    } catch (err) {
      console.error("Error sending message:", err);
      return res.status(500).json({ success: false, error: "Failed to send message." });
    }
  }

  res.setHeader("Allow", "GET, POST");
  return res.status(405).json({ success: false, error: "Method not allowed." });
}

export default requireApprovedMember(messagesHandler);
