package com.neel.aaila.data.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val familyName: String,
    val adminName: String,
    val familyId: String,
    val password: String,
    val requireAdminApproval: Boolean = true,
    val privateDirectoryOnly: Boolean = true
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val familyId: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val success: Boolean = false,
    val userId: String? = null,
    val familyId: String? = null,
    val familyName: String? = null,
    val adminName: String? = null,
    val role: String? = null,
    val token: String? = null,
    val expiresIn: String? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    val userId: String,
    val familyId: String,
    val role: String,
    val name: String,
    val relationship: String,
    val initials: String,
    val avatarColorIndex: Int = 0,
    val onlineStatus: String = "AVAILABLE",
    val statusNote: String = "",
    val status: String = "APPROVED"
)

@JsonClass(generateAdapter = true)
data class FamilySummaryDto(
    val familyId: String,
    val familyName: String,
    val adminName: String,
    val requireAdminApproval: Boolean = true,
    val privateDirectoryOnly: Boolean = true
)

@JsonClass(generateAdapter = true)
data class MeResponse(
    val success: Boolean = false,
    val user: UserProfileDto? = null,
    val family: FamilySummaryDto? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class FamilyMemberDto(
    val id: String,
    val name: String,
    val relationship: String,
    val role: String,
    val status: String,
    val onlineStatus: String,
    val statusNote: String = "",
    val initials: String = "??",
    val avatarColorIndex: Int = 0,
    val lastSeen: String? = null
)

@JsonClass(generateAdapter = true)
data class MembersResponse(
    val success: Boolean = false,
    val familyId: String? = null,
    val members: List<FamilyMemberDto> = emptyList(),
    val count: Int = 0,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class AddMemberRequest(
    val name: String,
    val relationship: String,
    val memberRole: String = "MEMBER"
)

@JsonClass(generateAdapter = true)
data class LiveKitTokenRequest(
    val roomName: String,
    val participantName: String? = null
)

@JsonClass(generateAdapter = true)
data class LiveKitTokenResponse(
    val success: Boolean = false,
    val token: String? = null,
    val url: String? = null,
    val room: String? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class GenericResponse(
    val success: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiErrorResponse(
    val success: Boolean = false,
    val error: String? = null,
    val errors: List<String>? = null
)
