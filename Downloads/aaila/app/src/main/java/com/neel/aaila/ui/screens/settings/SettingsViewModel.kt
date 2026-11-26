package com.neel.aaila.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neel.aaila.data.model.BackendServiceStatus
import com.neel.aaila.data.model.FamilySpaceSummary
import com.neel.aaila.data.model.ThemeMode
import com.neel.aaila.data.preferences.ThemePreferencesRepository
import com.neel.aaila.data.repository.Phase1FoundationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val familySpace: FamilySpaceSummary,
    val backendServices: List<BackendServiceStatus>
)

class SettingsViewModel(
    private val themePreferences: ThemePreferencesRepository,
    repository: Phase1FoundationRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        themePreferences.themeMode,
        repository.activeFamilySpace
    ) { mode, space ->
        SettingsUiState(
            themeMode = mode,
            familySpace = space,
            backendServices = repository.backendServices
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SettingsUiState(
            themeMode = ThemeMode.SYSTEM,
            familySpace = repository.activeFamilySpace.value,
            backendServices = repository.backendServices
        )
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    class Factory(
        private val themePreferences: ThemePreferencesRepository,
        private val repository: Phase1FoundationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(themePreferences, repository) as T
        }
    }
}
