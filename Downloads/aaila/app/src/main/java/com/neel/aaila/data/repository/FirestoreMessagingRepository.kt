package com.neel.aaila.data.repository

import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.neel.aaila.data.model.BackendActionResult
import com.neel.aaila.data.model.ChatMessage
import com.neel.aaila.data.model.ChatThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class FirestoreMessagingRepository(
    private val fallbackRepository: Phase1FoundationRepository,
    private val familyAuthRepository: FamilyAuthRepository
) : MessagingRepository {

    private val _threads = MutableStateFlow<List<ChatThread>>(fallbackRepository.threads.value)
    override val threads: StateFlow<List<ChatThread>> = _threads.asStateFlow()

    private val _messagesByThread = MutableStateFlow<Map<String, List<ChatMessage>>>(
        fallbackRepository.messagesByThread.value
    )
    override val messagesByThread: StateFlow<Map<String, List<ChatMessage>>> = _messagesByThread.asStateFlow()

    private val threadListeners = mutableMapOf<String, ListenerRegistration>()
    private var mainThreadsListener: ListenerRegistration? = null
    private var currentFamilyId: String? = null

    init {
        listenToThreads()
    }

    private fun isFirebaseConfigured(): Boolean {
        return try {
            FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()
        } catch (_: Throwable) {
            false
        }
    }

    fun listenToThreads() {
        val familyId = familyAuthRepository.activeFamilySpace.value.familyId
        if (familyId == currentFamilyId && mainThreadsListener != null) return
        currentFamilyId = familyId

        if (!isFirebaseConfigured()) {
            _threads.value = fallbackRepository.threads.value
            _messagesByThread.value = fallbackRepository.messagesByThread.value
            return
        }

        mainThreadsListener?.remove()
        try {
            val firestore = FirebaseFirestore.getInstance()
            mainThreadsListener = firestore.collection("families")
                .document(familyId)
                .collection("threads")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

                    val threadList = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val title = doc.getString("title") ?: "Family Chat"
                        val subtitle = doc.getString("subtitle") ?: ""
                        val memberId = doc.getString("memberId")
                        val isGroup = doc.getBoolean("isGroup") ?: false
                        val initials = doc.getString("initials") ?: title.take(2).uppercase(Locale.US)
                        val lastMessagePreview = doc.getString("lastMessagePreview") ?: ""

                        ChatThread(
                            id = id,
                            title = title,
                            subtitle = subtitle,
                            memberId = memberId,
                            isGroup = isGroup,
                            initials = initials,
                            lastMessagePreview = lastMessagePreview,
                            timestampLabel = "Just now",
                            unreadCount = 0
                        )
                    }

                    if (threadList.isNotEmpty()) {
                        _threads.value = threadList
                        threadList.forEach { thread -> listenToMessagesForThread(familyId, thread.id) }
                    }
                }
        } catch (_: Exception) {
            _threads.value = fallbackRepository.threads.value
        }
    }

    private fun listenToMessagesForThread(familyId: String, threadId: String) {
        if (threadListeners.containsKey(threadId)) return

        try {
            val firestore = FirebaseFirestore.getInstance()
            val listener = firestore.collection("families")
                .document(familyId)
                .collection("threads")
                .document(threadId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(100)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

                    val msgList = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val senderName = doc.getString("senderName") ?: "Family Member"
                        val isOutgoing = doc.getBoolean("isOutgoing") ?: false
                        val body = doc.getString("body") ?: ""

                        ChatMessage(
                            id = id,
                            threadId = threadId,
                            senderName = senderName,
                            isOutgoing = isOutgoing,
                            body = body,
                            timestampLabel = "Synced",
                            isLocalPreviewOnly = false
                        )
                    }

                    _messagesByThread.value = _messagesByThread.value + (threadId to msgList)
                }
            threadListeners[threadId] = listener
        } catch (_: Exception) {
            // fallback stays
        }
    }

    override fun messagesForThread(threadId: String): List<ChatMessage> {
        return _messagesByThread.value[threadId]
            ?: fallbackRepository.messagesForThread(threadId)
    }

    override fun sendPreviewMessage(
        threadId: String,
        text: String,
        senderName: String
    ): BackendActionResult<ChatMessage> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return BackendActionResult.ValidationError("Message cannot be empty.")
        }

        val familyId = familyAuthRepository.activeFamilySpace.value.familyId

        if (!isFirebaseConfigured()) {
            return fallbackRepository.sendPreviewMessage(threadId, text, senderName)
        }

        return try {
            val firestore = FirebaseFirestore.getInstance()
            val msgId = UUID.randomUUID().toString()

            val msgData = mapOf(
                "id" to msgId,
                "threadId" to threadId,
                "senderName" to senderName,
                "isOutgoing" to true,
                "body" to trimmed,
                "timestamp" to Timestamp.now()
            )

            val msgRef = firestore.collection("families")
                .document(familyId)
                .collection("threads")
                .document(threadId)
                .collection("messages")
                .document(msgId)

            Tasks.await(msgRef.set(msgData), 10, TimeUnit.SECONDS)

            // Update thread last message preview
            val threadRef = firestore.collection("families")
                .document(familyId)
                .collection("threads")
                .document(threadId)

            Tasks.await(
                threadRef.update(
                    mapOf(
                        "lastMessagePreview" to "$senderName: $trimmed",
                        "updatedAt" to Timestamp.now()
                    )
                ),
                10,
                TimeUnit.SECONDS
            )

            val chatMsg = ChatMessage(
                id = msgId,
                threadId = threadId,
                senderName = senderName,
                isOutgoing = true,
                body = trimmed,
                timestampLabel = "Sent",
                isLocalPreviewOnly = false
            )

            BackendActionResult.Success(chatMsg)
        } catch (e: Exception) {
            fallbackRepository.sendPreviewMessage(threadId, text, senderName)
        }
    }

    fun uploadMediaAndSendMessage(
        threadId: String,
        mediaUri: Uri,
        type: String,
        senderName: String
    ): BackendActionResult<Unit> {
        val familyId = familyAuthRepository.activeFamilySpace.value.familyId
        if (!isFirebaseConfigured()) return BackendActionResult.Success(Unit)

        return try {
            val storage = FirebaseStorage.getInstance()
            val fileId = UUID.randomUUID().toString()
            val path = "families/$familyId/chats/$threadId/$fileId"
            val ref = storage.reference.child(path)

            val uploadTask = ref.putFile(mediaUri)
            Tasks.await(uploadTask, 30, TimeUnit.SECONDS)

            val downloadUrlTask = ref.downloadUrl
            val downloadUrl = Tasks.await(downloadUrlTask, 10, TimeUnit.SECONDS).toString()

            val bodyText = when (type.uppercase(Locale.US)) {
                "IMAGE" -> "📷 Photo attachment"
                "VIDEO" -> "🎥 Video attachment"
                "AUDIO" -> "🎙️ Voice message"
                else -> "Attachment"
            }

            sendPreviewMessage(threadId, "$bodyText: $downloadUrl", senderName)
            BackendActionResult.Success(Unit)
        } catch (e: Exception) {
            BackendActionResult.ValidationError(e.localizedMessage ?: "Media upload failed.")
        }
    }
}
