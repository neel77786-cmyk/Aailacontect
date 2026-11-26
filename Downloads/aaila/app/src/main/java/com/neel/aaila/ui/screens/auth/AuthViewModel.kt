package com.neel.aaila.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neel.aaila.data.model.BackendActionResult
import com.neel.aaila.data.model.FamilySpaceSummary
import com.neel.aaila.data.repository.FamilyAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class LoginUiState(
    val familyId: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val familyIdError: String? = null,
    val passwordError: String? = null,
    val backendNotice: String? = null
)

data class AdminSetupUiState(
    val familyName: String = "",
    val adminName: String = "",
    val familyId: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val requireAdminApproval: Boolean = true,
    val privateDirectoryOnly: Boolean = true,
    val familyNameError: String? = null,
    val adminNameError: String? = null,
    val familyIdError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val backendNotice: String? = null,
    val readyForPreview: Boolean = false
)

class AuthViewModel(
    private val authRepository: FamilyAuthRepository
) : ViewModel() {

    private val familyIdPattern = Regex("^[A-Za-z0-9-]{4,24}$")

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _adminSetupState = MutableStateFlow(AdminSetupUiState())
    val adminSetupState: StateFlow<AdminSetupUiState> = _adminSetupState.asStateFlow()

    fun onLoginFamilyIdChanged(value: String) {
        _loginState.update {
            it.copy(
                familyId = value.uppercase().take(24),
                familyIdError = null,
                backendNotice = null
            )
        }
    }

    fun onLoginPasswordChanged(value: String) {
        _loginState.update {
            it.copy(
                password = value,
                passwordError = null,
                backendNotice = null
            )
        }
    }

    fun toggleLoginPasswordVisibility() {
        _loginState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun submitLogin(onSuccess: () -> Unit = {}): Boolean {
        val state = _loginState.value
        if (state.isLoading) return false

        val trimmedId = state.familyId.trim()
        val idError = when {
            trimmedId.isEmpty() -> "Please enter your Family ID."
            !familyIdPattern.matches(trimmedId) ->
                "Family ID must be 4–24 characters using letters, numbers, or hyphens."
            else -> null
        }
        val pwdError = when {
            state.password.isEmpty() -> "Please enter your family password."
            state.password.length < 6 -> "Password must be at least 6 characters."
            else -> null
        }

        if (idError != null || pwdError != null) {
            _loginState.update {
                it.copy(familyIdError = idError, passwordError = pwdError)
            }
            return false
        }

        _loginState.update { it.copy(isLoading = true, backendNotice = null) }
        viewModelScope.launch {
            when (val result = authRepository.signInAsync(trimmedId, state.password)) {
                is BackendActionResult.Success -> {
                    _loginState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is BackendActionResult.BackendNotConfigured -> {
                    _loginState.update {
                        it.copy(
                            isLoading = false,
                            backendNotice = "${result.featureName} requires ${result.requiredBackend}. " +
                                "Use \"Enter UI Preview\" below to explore the Phase 1 interface."
                        )
                    }
                }
                is BackendActionResult.ValidationError -> {
                    _loginState.update { it.copy(isLoading = false, backendNotice = result.message) }
                }
            }
        }
        return false
    }

    fun enterLoginPreview(onSuccess: () -> Unit) {
        val state = _loginState.value
        val trimmedId = state.familyId.trim()
        if (trimmedId.isNotEmpty() && familyIdPattern.matches(trimmedId)) {
            authRepository.enterUiPreview(
                FamilySpaceSummary(
                    familyId = trimmedId,
                    familyName = "Family Space ($trimmedId)",
                    adminName = "Neel",
                    requireAdminApproval = true,
                    privateDirectoryOnly = true,
                    isPreviewSession = true
                )
            )
        } else {
            authRepository.enterUiPreview(null)
        }
        onSuccess()
    }

    fun onFamilyNameChanged(value: String) {
        _adminSetupState.update {
            it.copy(familyName = value, familyNameError = null, backendNotice = null)
        }
    }

    fun onAdminNameChanged(value: String) {
        _adminSetupState.update {
            it.copy(adminName = value, adminNameError = null, backendNotice = null)
        }
    }

    fun onSetupFamilyIdChanged(value: String) {
        _adminSetupState.update {
            it.copy(
                familyId = value.uppercase().take(24),
                familyIdError = null,
                backendNotice = null
            )
        }
    }

    fun generateSuggestedFamilyId() {
        val cleanName = _adminSetupState.value.familyName
            .uppercase()
            .replace(Regex("[^A-Z0-9]"), "")
            .take(6)
            .ifEmpty { "AAILA" }
        val suffix = Random.nextInt(100, 999)
        _adminSetupState.update {
            it.copy(familyId = "$cleanName-$suffix", familyIdError = null)
        }
    }

    fun onSetupPasswordChanged(value: String) {
        _adminSetupState.update {
            it.copy(password = value, passwordError = null, backendNotice = null)
        }
    }

    fun onSetupConfirmPasswordChanged(value: String) {
        _adminSetupState.update {
            it.copy(confirmPassword = value, confirmPasswordError = null, backendNotice = null)
        }
    }

    fun toggleSetupPasswordVisibility() {
        _adminSetupState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun onRequireAdminApprovalChanged(enabled: Boolean) {
        _adminSetupState.update { it.copy(requireAdminApproval = enabled) }
    }

    fun onPrivateDirectoryChanged(enabled: Boolean) {
        _adminSetupState.update { it.copy(privateDirectoryOnly = enabled) }
    }

    fun validateAndSubmitAdminSetup(onSuccess: () -> Unit = {}): Boolean {
        val s = _adminSetupState.value
        if (s.isLoading) return false

        val familyNameErr = if (s.familyName.trim().length < 2) "Please enter a family name (at least 2 characters)." else null
        val adminNameErr = if (s.adminName.trim().length < 2) "Please enter your display name." else null
        val idTrimmed = s.familyId.trim()
        val idErr = when {
            idTrimmed.isEmpty() -> "Please enter or generate a Family ID."
            !familyIdPattern.matches(idTrimmed) ->
                "Family ID must be 4–24 characters using letters, numbers, or hyphens."
            else -> null
        }
        val pwdErr = when {
            s.password.isEmpty() -> "Please create a family password."
            s.password.length < 6 -> "Password must be at least 6 characters."
            else -> null
        }
        val confirmErr = if (s.confirmPassword != s.password) "Passwords do not match." else null

        if (listOf(familyNameErr, adminNameErr, idErr, pwdErr, confirmErr).any { it != null }) {
            _adminSetupState.update {
                it.copy(
                    familyNameError = familyNameErr,
                    adminNameError = adminNameErr,
                    familyIdError = idErr,
                    passwordError = pwdErr,
                    confirmPasswordError = confirmErr
                )
            }
            return false
        }

        _adminSetupState.update { it.copy(isLoading = true, backendNotice = null) }
        viewModelScope.launch {
            val result = authRepository.createFamilySpaceAsync(
                familyName = s.familyName,
                adminName = s.adminName,
                familyId = idTrimmed,
                password = s.password,
                requireAdminApproval = s.requireAdminApproval,
                privateDirectoryOnly = s.privateDirectoryOnly
            )

            when (result) {
                is BackendActionResult.Success -> {
                    _adminSetupState.update { it.copy(isLoading = false, readyForPreview = true) }
                    onSuccess()
                }
                is BackendActionResult.BackendNotConfigured -> {
                    _adminSetupState.update {
                        it.copy(
                            isLoading = false,
                            readyForPreview = true,
                            backendNotice = "${result.featureName} is saved locally for UI Preview only. " +
                                "Persisting to ${result.requiredBackend} will be added in Phase 2."
                        )
                    }
                }
                is BackendActionResult.ValidationError -> {
                    _adminSetupState.update { it.copy(isLoading = false, backendNotice = result.message) }
                }
            }
        }
        return false
    }

    class Factory(private val authRepository: FamilyAuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
}
