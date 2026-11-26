package com.neel.aaila.ui.screens.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neel.aaila.data.model.FamilyMember
import com.neel.aaila.data.model.MemberRole
import com.neel.aaila.data.repository.Phase1FoundationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class FamilyRoleFilter(val label: String) {
    ALL("All"),
    ADMINS("Admins"),
    PARENTS("Parents"),
    OTHERS("Siblings & Elders")
}

data class FamilyUiState(
    val searchQuery: String = "",
    val selectedFilter: FamilyRoleFilter = FamilyRoleFilter.ALL,
    val filteredMembers: List<FamilyMember> = emptyList(),
    val selectedMember: FamilyMember? = null
)

class FamilyViewModel(
    private val repository: Phase1FoundationRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(FamilyRoleFilter.ALL)
    private val selectedMember = MutableStateFlow<FamilyMember?>(null)

    val uiState: StateFlow<FamilyUiState> = combine(
        repository.members,
        searchQuery,
        selectedFilter,
        selectedMember
    ) { members, query, filter, inspected ->
        val trimmed = query.trim().lowercase()
        val filtered = members.filter { member ->
            val matchesRole = when (filter) {
                FamilyRoleFilter.ALL -> true
                FamilyRoleFilter.ADMINS -> member.role == MemberRole.ADMIN
                FamilyRoleFilter.PARENTS -> member.role == MemberRole.PARENT
                FamilyRoleFilter.OTHERS ->
                    member.role == MemberRole.SIBLING ||
                        member.role == MemberRole.GRANDPARENT ||
                        member.role == MemberRole.MEMBER
            }
            val matchesQuery = trimmed.isEmpty() ||
                member.name.lowercase().contains(trimmed) ||
                member.relationship.lowercase().contains(trimmed)
            matchesRole && matchesQuery
        }

        FamilyUiState(
            searchQuery = query,
            selectedFilter = filter,
            filteredMembers = filtered,
            selectedMember = inspected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = FamilyUiState(filteredMembers = repository.members.value)
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onFilterSelected(filter: FamilyRoleFilter) {
        selectedFilter.value = filter
    }

    fun onInspectMember(member: FamilyMember?) {
        selectedMember.value = member
    }

    fun threadIdForMember(memberId: String): String {
        return when (memberId) {
            "member_maa" -> "thread_maa"
            "member_meera" -> "thread_meera"
            "member_dada" -> "thread_dada"
            else -> "thread_family_circle"
        }
    }

    class Factory(private val repository: Phase1FoundationRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FamilyViewModel(repository) as T
        }
    }
}
