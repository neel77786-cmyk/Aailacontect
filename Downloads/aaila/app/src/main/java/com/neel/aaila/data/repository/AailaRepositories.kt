package com.neel.aaila.data.repository

import com.neel.aaila.data.model.BackendActionResult
import com.neel.aaila.data.model.BackendServiceStatus
import com.neel.aaila.data.model.CallDirection
import com.neel.aaila.data.model.CallLogEntry
import com.neel.aaila.data.model.CallType
import com.neel.aaila.data.model.ChatMessage
import com.neel.aaila.data.model.ChatThread
import com.neel.aaila.data.model.FamilyMember
import com.neel.aaila.data.model.FamilySpaceSummary
import com.neel.aaila.data.model.MemberRole
import com.neel.aaila.data.model.OnlineStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Phase 1 Repository Interfaces.
 * Phase 2 will provide Firebase Auth, Cloud Firestore, FCM, and WebRTC Signaling implementations.
 */
interface FamilyAuthRepository {
    val activeFamilySpace: StateFlow<FamilySpaceSummary>
    fun signInWithFamilyId(familyId: String, password: String): BackendActionResult<FamilySpaceSummary>
    fun createFamilySpaceAsAdmin(
        familyName: String,
        adminName: String,
        familyId: String,
        password: String,
        requireAdminApproval: Boolean,
        privateDirectoryOnly: Boolean
    ): BackendActionResult<FamilySpaceSummary>
    suspend fun signInAsync(familyId: String, password: String): BackendActionResult<FamilySpaceSummary> =
        signInWithFamilyId(familyId, password)
    suspend fun createFamilySpaceAsync(
        familyName: String,
        adminName: String,
        familyId: String,
        password: String,
        requireAdminApproval: Boolean,
        privateDirectoryOnly: Boolean
    ): BackendActionResult<FamilySpaceSummary> =
        createFamilySpaceAsAdmin(familyName, adminName, familyId, password, requireAdminApproval, privateDirectoryOnly)
    fun signOut() {}
    fun enterUiPreview(customSpace: FamilySpaceSummary? = null)
}

interface FamilyDirectoryRepository {
    val members: StateFlow<List<FamilyMember>>
    fun findMemberById(memberId: String): FamilyMember?
}

interface CallingRepository {
    val callHistory: StateFlow<List<CallLogEntry>>
    fun initiateCall(memberId: String, callType: CallType): BackendActionResult<Unit>
}

interface MessagingRepository {
    val threads: StateFlow<List<ChatThread>>
    val messagesByThread: StateFlow<Map<String, List<ChatMessage>>>
    fun messagesForThread(threadId: String): List<ChatMessage>
    fun sendPreviewMessage(threadId: String, text: String, senderName: String): BackendActionResult<ChatMessage>
}

/**
 * Phase 1 Foundation Data & Unconfigured Backend Provider.
 *
 * IMPORTANT: No fake backend networking is performed here. Every server-dependent action
 * explicitly returns [BackendActionResult.BackendNotConfigured] so the UI informs the user
 * accurately that Firebase / Signaling configuration belongs to Phase 2.
 */
class Phase1FoundationRepository :
    FamilyAuthRepository,
    FamilyDirectoryRepository,
    CallingRepository,
    MessagingRepository {

    private val _activeFamilySpace = MutableStateFlow(
        FamilySpaceSummary(
            familyId = "AAILA-HOME-01",
            familyName = "The Family Circle",
            adminName = "Neel",
            requireAdminApproval = true,
            privateDirectoryOnly = true,
            isPreviewSession = true
        )
    )
    override val activeFamilySpace: StateFlow<FamilySpaceSummary> = _activeFamilySpace.asStateFlow()

    private val _members = MutableStateFlow(samplePreviewMembers())
    override val members: StateFlow<List<FamilyMember>> = _members.asStateFlow()

    private val _callHistory = MutableStateFlow(samplePreviewCallHistory())
    override val callHistory: StateFlow<List<CallLogEntry>> = _callHistory.asStateFlow()

    private val _threads = MutableStateFlow(samplePreviewThreads())
    override val threads: StateFlow<List<ChatThread>> = _threads.asStateFlow()

    private val _messagesByThread = MutableStateFlow(samplePreviewMessages())
    override val messagesByThread: StateFlow<Map<String, List<ChatMessage>>> = _messagesByThread.asStateFlow()

    val backendServices: List<BackendServiceStatus> = listOf(
        BackendServiceStatus(
            id = "firebase_auth",
            serviceName = "Family ID Authentication",
            purpose = "Verifies Family ID & hashed family passcodes securely",
            isConfigured = false,
            phaseLabel = "Requires Firebase Auth / Cloud Functions (Phase 2)"
        ),
        BackendServiceStatus(
            id = "cloud_firestore",
            serviceName = "Cloud Firestore Sync",
            purpose = "Stores family spaces, member profiles, and encrypted chat threads",
            isConfigured = false,
            phaseLabel = "Requires Cloud Firestore (Phase 2)"
        ),
        BackendServiceStatus(
            id = "webrtc_signaling",
            serviceName = "Voice & Video Calling",
            purpose = "Real-time WebRTC peer audio/video and call signaling",
            isConfigured = false,
            phaseLabel = "Requires WebRTC & Signaling Server (Phase 2)"
        ),
        BackendServiceStatus(
            id = "fcm_push",
            serviceName = "Incoming Call & Message Alerts",
            purpose = "Wakes devices for incoming family calls and messages",
            isConfigured = false,
            phaseLabel = "Requires Firebase Cloud Messaging (Phase 2)"
        )
    )

    override fun signInWithFamilyId(
        familyId: String,
        password: String
    ): BackendActionResult<FamilySpaceSummary> {
        // TODO(Phase 2): Integrate Firebase Auth / Cloud Function verification for Family ID + Password.
        return BackendActionResult.BackendNotConfigured(
            featureName = "Family Sign-In",
            requiredBackend = "Firebase Auth & Cloud Firestore (Phase 2)"
        )
    }

    override fun createFamilySpaceAsAdmin(
        familyName: String,
        adminName: String,
        familyId: String,
        password: String,
        requireAdminApproval: Boolean,
        privateDirectoryOnly: Boolean
    ): BackendActionResult<FamilySpaceSummary> {
        // Persist preview metadata locally in memory so the user can inspect the dashboard UI.
        _activeFamilySpace.value = FamilySpaceSummary(
            familyId = familyId.trim().uppercase(),
            familyName = familyName.trim(),
            adminName = adminName.trim(),
            requireAdminApproval = requireAdminApproval,
            privateDirectoryOnly = privateDirectoryOnly,
            isPreviewSession = true
        )
        // TODO(Phase 2): Provision Family Space document in Cloud Firestore & Admin credentials in Firebase Auth.
        return BackendActionResult.BackendNotConfigured(
            featureName = "Admin Family Space Creation",
            requiredBackend = "Cloud Firestore & Firebase Auth (Phase 2)"
        )
    }

    override fun enterUiPreview(customSpace: FamilySpaceSummary?) {
        if (customSpace != null) {
            _activeFamilySpace.value = customSpace
        }
    }

    override fun findMemberById(memberId: String): FamilyMember? {
        return _members.value.firstOrNull { it.id == memberId }
    }

    override fun initiateCall(memberId: String, callType: CallType): BackendActionResult<Unit> {
        // TODO(Phase 2): Connect to WebRTC signaling server + ICE/TURN relay before starting media stream.
        return BackendActionResult.BackendNotConfigured(
            featureName = if (callType == CallType.VIDEO) "Video Calling" else "Voice Calling",
            requiredBackend = "WebRTC & Signaling Server (Phase 2)"
        )
    }

    override fun messagesForThread(threadId: String): List<ChatMessage> {
        return _messagesByThread.value[threadId].orEmpty()
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
        val previewMsg = ChatMessage(
            id = "preview_${System.currentTimeMillis()}",
            threadId = threadId,
            senderName = senderName,
            isOutgoing = true,
            body = trimmed,
            timestampLabel = "Preview · Not sent",
            isLocalPreviewOnly = true
        )
        _messagesByThread.update { current ->
            val updatedList = current[threadId].orEmpty() + previewMsg
            current + (threadId to updatedList)
        }
        // TODO(Phase 2): Write message to Cloud Firestore and trigger FCM push notification.
        return BackendActionResult.BackendNotConfigured(
            featureName = "Live Message Delivery",
            requiredBackend = "Cloud Firestore & FCM (Phase 2)"
        )
    }

    private fun samplePreviewMembers(): List<FamilyMember> = listOf(
        FamilyMember(
            id = "member_neel",
            name = "Neel",
            relationship = "Admin · You",
            role = MemberRole.ADMIN,
            status = OnlineStatus.AVAILABLE,
            statusNote = "Family Space Organizer",
            initials = "NL",
            avatarColorIndex = 0,
            lastSeenLabel = "UI Preview Profile"
        ),
        FamilyMember(
            id = "member_maa",
            name = "Asha",
            relationship = "Mom",
            role = MemberRole.PARENT,
            status = OnlineStatus.AVAILABLE,
            statusNote = "At home · Free for a quick call",
            initials = "AS",
            avatarColorIndex = 1,
            lastSeenLabel = "Sample Profile · Available"
        ),
        FamilyMember(
            id = "member_papa",
            name = "Rajesh",
            relationship = "Dad",
            role = MemberRole.PARENT,
            status = OnlineStatus.BUSY,
            statusNote = "Evening walk",
            initials = "RJ",
            avatarColorIndex = 2,
            lastSeenLabel = "Sample Profile · 15m ago"
        ),
        FamilyMember(
            id = "member_meera",
            name = "Meera",
            relationship = "Sister",
            role = MemberRole.SIBLING,
            status = OnlineStatus.AVAILABLE,
            statusNote = "Online in Family Chat",
            initials = "MR",
            avatarColorIndex = 3,
            lastSeenLabel = "Sample Profile · Available"
        ),
        FamilyMember(
            id = "member_dada",
            name = "Mohan",
            relationship = "Grandpa",
            role = MemberRole.GRANDPARENT,
            status = OnlineStatus.AWAY,
            statusNote = "Reading afternoon news",
            initials = "MH",
            avatarColorIndex = 4,
            lastSeenLabel = "Sample Profile · 1h ago"
        ),
        FamilyMember(
            id = "member_dadi",
            name = "Kamla",
            relationship = "Grandma",
            role = MemberRole.GRANDPARENT,
            status = OnlineStatus.OFFLINE,
            statusNote = "Large text friendly mode",
            initials = "KM",
            avatarColorIndex = 5,
            lastSeenLabel = "Sample Profile · Yesterday"
        )
    )

    private fun samplePreviewCallHistory(): List<CallLogEntry> = listOf(
        CallLogEntry(
            id = "call_1",
            memberId = "member_maa",
            memberName = "Asha",
            relationship = "Mom",
            initials = "AS",
            callType = CallType.VIDEO,
            direction = CallDirection.INCOMING,
            timestampLabel = "Sample · Today, 7:15 PM",
            durationLabel = "14m 20s"
        ),
        CallLogEntry(
            id = "call_2",
            memberId = "member_dada",
            memberName = "Mohan",
            relationship = "Grandpa",
            initials = "MH",
            callType = CallType.VOICE,
            direction = CallDirection.OUTGOING,
            timestampLabel = "Sample · Yesterday",
            durationLabel = "8m 05s"
        ),
        CallLogEntry(
            id = "call_3",
            memberId = "member_meera",
            memberName = "Meera",
            relationship = "Sister",
            initials = "MR",
            callType = CallType.VIDEO,
            direction = CallDirection.MISSED,
            timestampLabel = "Sample · Mon, 6:40 PM",
            durationLabel = "Missed"
        ),
        CallLogEntry(
            id = "call_4",
            memberId = "member_papa",
            memberName = "Rajesh",
            relationship = "Dad",
            initials = "RJ",
            callType = CallType.VOICE,
            direction = CallDirection.INCOMING,
            timestampLabel = "Sample · Sun, 10:12 AM",
            durationLabel = "21m 48s"
        )
    )

    private fun samplePreviewThreads(): List<ChatThread> = listOf(
        ChatThread(
            id = "thread_family_circle",
            title = "Family Circle",
            subtitle = "Private Group · 6 Members",
            memberId = null,
            isGroup = true,
            initials = "FC",
            lastMessagePreview = "Asha: Dinner is ready whenever everyone gets home!",
            timestampLabel = "Sample · 6:50 PM",
            unreadCount = 2
        ),
        ChatThread(
            id = "thread_maa",
            title = "Asha (Mom)",
            subtitle = "Parent · UI Preview",
            memberId = "member_maa",
            isGroup = false,
            initials = "AS",
            lastMessagePreview = "Let's do a quick video call after 8 PM.",
            timestampLabel = "Sample · 5:30 PM",
            unreadCount = 1
        ),
        ChatThread(
            id = "thread_meera",
            title = "Meera (Sister)",
            subtitle = "Sibling · UI Preview",
            memberId = "member_meera",
            isGroup = false,
            initials = "MR",
            lastMessagePreview = "Shared the weekend plans note.",
            timestampLabel = "Sample · Yesterday",
            unreadCount = 0
        ),
        ChatThread(
            id = "thread_dada",
            title = "Mohan (Grandpa)",
            subtitle = "Grandparent · UI Preview",
            memberId = "member_dada",
            isGroup = false,
            initials = "MH",
            lastMessagePreview = "Glad we have our own quiet family app!",
            timestampLabel = "Sample · Mon",
            unreadCount = 0
        )
    )

    private fun samplePreviewMessages(): Map<String, List<ChatMessage>> = mapOf(
        "thread_family_circle" to listOf(
            ChatMessage(
                id = "m1",
                threadId = "thread_family_circle",
                senderName = "Rajesh (Dad)",
                isOutgoing = false,
                body = "Good evening everyone! Heading home in 20 minutes.",
                timestampLabel = "6:20 PM"
            ),
            ChatMessage(
                id = "m2",
                threadId = "thread_family_circle",
                senderName = "You",
                isOutgoing = true,
                body = "Sounds great! Should we call Grandpa before dinner?",
                timestampLabel = "6:24 PM"
            ),
            ChatMessage(
                id = "m3",
                threadId = "thread_family_circle",
                senderName = "Asha (Mom)",
                isOutgoing = false,
                body = "Dinner is ready whenever everyone gets home!",
                timestampLabel = "6:50 PM"
            )
        ),
        "thread_maa" to listOf(
            ChatMessage(
                id = "m4",
                threadId = "thread_maa",
                senderName = "Asha (Mom)",
                isOutgoing = false,
                body = "Hi dear, hope your day went smoothly.",
                timestampLabel = "5:25 PM"
            ),
            ChatMessage(
                id = "m5",
                threadId = "thread_maa",
                senderName = "Asha (Mom)",
                isOutgoing = false,
                body = "Let's do a quick video call after 8 PM.",
                timestampLabel = "5:30 PM"
            )
        ),
        "thread_meera" to listOf(
            ChatMessage(
                id = "m6",
                threadId = "thread_meera",
                senderName = "You",
                isOutgoing = true,
                body = "Hey Meera, did you check Sunday lunch timings?",
                timestampLabel = "Yesterday"
            ),
            ChatMessage(
                id = "m7",
                threadId = "thread_meera",
                senderName = "Meera (Sister)",
                isOutgoing = false,
                body = "Shared the weekend plans note.",
                timestampLabel = "Yesterday"
            )
        ),
        "thread_dada" to listOf(
            ChatMessage(
                id = "m8",
                threadId = "thread_dada",
                senderName = "Mohan (Grandpa)",
                isOutgoing = false,
                body = "Glad we have our own quiet family app!",
                timestampLabel = "Mon"
            )
        )
    )
}
