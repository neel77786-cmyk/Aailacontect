package com.neel.aaila.data.repository

import com.neel.aaila.data.model.BackendActionResult
import com.neel.aaila.data.model.FamilySpaceSummary
import com.neel.aaila.data.network.AailaApiService
import com.neel.aaila.data.network.ApiErrorResponse
import com.neel.aaila.data.network.LoginRequest
import com.neel.aaila.data.network.RegisterRequest
import com.neel.aaila.data.network.TokenManager
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.Locale

/**
 * Auth repository backed by the AAILA Vercel REST API.
 * Stores JWT sessions securely via [TokenManager] and never embeds secrets in the app.
 */
class BackendFamilyAuthRepository(
    private val apiService: AailaApiService,
    private val tokenManager: TokenManager,
    private val moshi: Moshi,
    private val fallbackRepository: Phase1FoundationRepository
) : FamilyAuthRepository {

    private val _activeFamilySpace = MutableStateFlow(
        FamilySpaceSummary(
            familyId = tokenManager.getFamilyId() ?: "AAILA-HOME-01",
            familyName = "The Family Circle",
            adminName = "Neel",
            requireAdminApproval = true,
            privateDirectoryOnly = true,
            isPreviewSession = !tokenManager.isLoggedIn()
        )
    )
    override val activeFamilySpace: StateFlow<FamilySpaceSummary> = _activeFamilySpace.asStateFlow()

    override fun signInWithFamilyId(
        familyId: String,
        password: String
    ): BackendActionResult<FamilySpaceSummary> = runBlocking {
        signInAsync(familyId, password)
    }

    override suspend fun signInAsync(
        familyId: String,
        password: String
    ): BackendActionResult<FamilySpaceSummary> = withContext(Dispatchers.IO) {
        val cleanFamilyId = familyId.trim().uppercase(Locale.US)
        try {
            val response = apiService.login(
                LoginRequest(
                    familyId = cleanFamilyId,
                    password = password
                )
            )
            val body = response.body()
            if (response.isSuccessful && body != null && body.success && !body.token.isNullOrBlank()) {
                val userId = body.userId.orEmpty()
                val resolvedFamilyId = body.familyId ?: cleanFamilyId
                val role = body.role ?: "MEMBER"
                tokenManager.saveSession(
                    token = body.token,
                    userId = userId,
                    familyId = resolvedFamilyId,
                    role = role
                )
                val summary = FamilySpaceSummary(
                    familyId = resolvedFamilyId,
                    familyName = body.familyName ?: "Family Space",
                    adminName = body.adminName ?: "Admin",
                    requireAdminApproval = true,
                    privateDirectoryOnly = true,
                    isPreviewSession = false
                )
                _activeFamilySpace.value = summary
                BackendActionResult.Success(summary)
            } else {
                val msg = extractErrorMessage(response, "Sign-in failed. Please check your credentials.")
                BackendActionResult.ValidationError(msg)
            }
        } catch (e: Exception) {
            BackendActionResult.ValidationError(
                e.localizedMessage ?: "Unable to reach AAILA server. Check your internet connection."
            )
        }
    }

    override fun createFamilySpaceAsAdmin(
        familyName: String,
        adminName: String,
        familyId: String,
        password: String,
        requireAdminApproval: Boolean,
        privateDirectoryOnly: Boolean
    ): BackendActionResult<FamilySpaceSummary> = runBlocking {
        createFamilySpaceAsync(
            familyName,
            adminName,
            familyId,
            password,
            requireAdminApproval,
            privateDirectoryOnly
        )
    }

    override suspend fun createFamilySpaceAsync(
        familyName: String,
        adminName: String,
        familyId: String,
        password: String,
        requireAdminApproval: Boolean,
        privateDirectoryOnly: Boolean
    ): BackendActionResult<FamilySpaceSummary> = withContext(Dispatchers.IO) {
        val cleanFamilyId = familyId.trim().uppercase(Locale.US)
        try {
            val response = apiService.register(
                RegisterRequest(
                    familyName = familyName.trim(),
                    adminName = adminName.trim(),
                    familyId = cleanFamilyId,
                    password = password,
                    requireAdminApproval = requireAdminApproval,
                    privateDirectoryOnly = privateDirectoryOnly
                )
            )
            val body = response.body()
            if (response.isSuccessful && body != null && body.success && !body.token.isNullOrBlank()) {
                val userId = body.userId.orEmpty()
                val resolvedFamilyId = body.familyId ?: cleanFamilyId
                tokenManager.saveSession(
                    token = body.token,
                    userId = userId,
                    familyId = resolvedFamilyId,
                    role = body.role ?: "ADMIN"
                )
                val summary = FamilySpaceSummary(
                    familyId = resolvedFamilyId,
                    familyName = body.familyName ?: familyName.trim(),
                    adminName = body.adminName ?: adminName.trim(),
                    requireAdminApproval = requireAdminApproval,
                    privateDirectoryOnly = privateDirectoryOnly,
                    isPreviewSession = false
                )
                _activeFamilySpace.value = summary
                BackendActionResult.Success(summary)
            } else {
                val msg = extractErrorMessage(response, "Failed to create Family Space.")
                BackendActionResult.ValidationError(msg)
            }
        } catch (e: Exception) {
            BackendActionResult.ValidationError(
                e.localizedMessage ?: "Unable to reach AAILA server. Check your internet connection."
            )
        }
    }

    override fun signOut() {
        tokenManager.clearSession()
        _activeFamilySpace.value = fallbackRepository.activeFamilySpace.value
    }

    override fun enterUiPreview(customSpace: FamilySpaceSummary?) {
        if (customSpace != null) {
            _activeFamilySpace.value = customSpace
        } else {
            fallbackRepository.enterUiPreview(null)
        }
    }

    private fun <T> extractErrorMessage(response: Response<T>, fallback: String): String {
        val rawError = response.errorBody()?.string()
        if (!rawError.isNullOrBlank()) {
            try {
                val adapter = moshi.adapter(ApiErrorResponse::class.java)
                val parsed = adapter.fromJson(rawError)
                val singleError = parsed?.error
                if (!singleError.isNullOrBlank()) return singleError
                val errorList = parsed?.errors
                if (!errorList.isNullOrEmpty()) return errorList.joinToString("\n")
            } catch (_: Exception) {
                // Fall through to HTTP status message
            }
        }
        return when (response.code()) {
            401 -> "Invalid Family ID or password (HTTP 401)."
            409 -> "This Family ID is already registered."
            else -> fallback
        }
    }
}
