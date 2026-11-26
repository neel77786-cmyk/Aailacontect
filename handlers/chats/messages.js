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
      const snap = await threadRef
        .collection("messages")
        .orderBy("timestamp", "asc")
        .limit(100)
        .get();

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

      // Clear unread count for current user
      await threadRef.set({ unreadBy: { [uid]: 0 } }, { merge: true }).catch(() => {});

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
      await threadRef.collection("messages").doc(msgId).set(msgData);

      // Increment unread counts for other approved members
      const membersSnap = await familyRef
        .collection("members")
        .where("status", "==", "APPROVED")
        .get();

      const threadDoc = await threadRef.get();
      const existingUnread = threadDoc.exists ? threadDoc.data().unreadBy || {} : {};
      const updatedUnread = { ...existingUnread, [uid]: 0 };
      const tokens = [];

      for (const mDoc of membersSnap.docs) {
        if (mDoc.id !== uid) {
          updatedUnread[mDoc.id] = Number(updatedUnread[mDoc.id] || 0) + 1;
          const fcm = mDoc.data().fcmToken;
          if (fcm) tokens.push(fcm);
        }
      }

      await threadRef.set(
        {
          lastMessagePreview: `${name}: ${cleanBody}`,
          updatedAt: now,
          unreadBy: updatedUnread,
        },
        { merge: true }
      );

      // Best-effort FCM push notification
      if (tokens.length > 0) {
        getFirebaseAdmin()
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
