package com.neel.aaila.data.model

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class MemberRole(val displayName: String) {
    ADMIN("Family Admin"),
    PARENT("Parent"),
    SIBLING("Sibling"),
    GRANDPARENT("Grandparent"),
    MEMBER("Member")
}

enum class OnlineStatus(val label: String) {
    AVAILABLE("Available"),
    BUSY("In a moment"),
    AWAY("Away"),
    OFFLINE("Offline")
}

enum class CallType {
    VOICE,
    VIDEO
}

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

data class FamilySpaceSummary(
    val familyId: String,
    val familyName: String,
    val adminName: String,
    val requireAdminApproval: Boolean,
    val privateDirectoryOnly: Boolean,
    val isPreviewSession: Boolean = true
)

data class FamilyMember(
    val id: String,
    val name: String,
    val relationship: String,
    val role: MemberRole,
    val status: OnlineStatus,
    val statusNote: String,
    val initials: String,
    val avatarColorIndex: Int,
    val lastSeenLabel: String
)

data class CallLogEntry(
    val id: String,
    val memberId: String,
    val memberName: String,
    val relationship: String,
    val initials: String,
    val callType: CallType,
    val direction: CallDirection,
    val timestampLabel: String,
    val durationLabel: String
)

data class ChatThread(
    val id: String,
    val title: String,
    val subtitle: String,
    val memberId: String?,
    val isGroup: Boolean,
    val initials: String,
    val lastMessagePreview: String,
    val timestampLabel: String,
    val unreadCount: Int
)

data class ChatMessage(
    val id: String,
    val threadId: String,
    val senderName: String,
    val isOutgoing: Boolean,
    val body: String,
    val timestampLabel: String,
    val isLocalPreviewOnly: Boolean = false
)

data class BackendServiceStatus(
    val id: String,
    val serviceName: String,
    val purpose: String,
    val isConfigured: Boolean,
    val phaseLabel: String
)

sealed class BackendActionResult<out T> {
    data class Success<T>(val data: T) : BackendActionResult<T>()
    data class BackendNotConfigured(
        val featureName: String,
        val requiredBackend: String
    ) : BackendActionResult<Nothing>()
    data class ValidationError(val message: String) : BackendActionResult<Nothing>()
}
