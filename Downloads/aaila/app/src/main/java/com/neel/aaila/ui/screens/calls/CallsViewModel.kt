package com.neel.aaila.ui.screens.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neel.aaila.data.model.CallLogEntry
import com.neel.aaila.data.model.FamilyMember
import com.neel.aaila.data.repository.Phase1FoundationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CallsUiState(
    val quickContacts: List<FamilyMember> = emptyList(),
    val callHistory: List<CallLogEntry> = emptyList()
)

class CallsViewModel(
    private val repository: Phase1FoundationRepository
) : ViewModel() {

    val uiState: StateFlow<CallsUiState> = combine(
        repository.members,
        repository.callHistory
    ) { members, history ->
        CallsUiState(
            quickContacts = members.filter { it.id != "member_neel" },
            callHistory = history
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CallsUiState(
            quickContacts = repository.members.value.filter { it.id != "member_neel" },
            callHistory = repository.callHistory.value
        )
    )

    fun findMember(memberId: String): FamilyMember {
        return repository.findMemberById(memberId)
            ?: repository.members.value.first()
    }

    class Factory(private val repository: Phase1FoundationRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CallsViewModel(repository) as T
        }
    }
}
