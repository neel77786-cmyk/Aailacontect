package com.neel.aaila.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neel.aaila.data.model.ChatThread
import com.neel.aaila.data.model.FamilyMember
import com.neel.aaila.data.model.FamilySpaceSummary
import com.neel.aaila.data.repository.Phase1FoundationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val familySpace: FamilySpaceSummary,
    val members: List<FamilyMember>,
    val recentThreads: List<ChatThread>
)

class DashboardViewModel(
    repository: Phase1FoundationRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.activeFamilySpace,
        repository.members,
        repository.threads
    ) { space, members, threads ->
        DashboardUiState(
            familySpace = space,
            members = members,
            recentThreads = threads.take(2)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = DashboardUiState(
            familySpace = repository.activeFamilySpace.value,
            members = repository.members.value,
            recentThreads = repository.threads.value.take(2)
        )
    )

    class Factory(private val repository: Phase1FoundationRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
