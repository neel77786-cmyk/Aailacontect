package com.neel.aaila.ui.screens.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neel.aaila.data.model.BackendActionResult
import com.neel.aaila.data.model.ChatMessage
import com.neel.aaila.data.model.ChatThread
import com.neel.aaila.data.repository.Phase1FoundationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ChatsUiState(
    val threads: List<ChatThread> = emptyList(),
    val messagesByThread: Map<String, List<ChatMessage>> = emptyMap(),
    val composerNotice: String? = null
)

class ChatsViewModel(
    private val repository: Phase1FoundationRepository
) : ViewModel() {

    private val composerNotice = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ChatsUiState> = combine(
        repository.threads,
        repository.messagesByThread,
        composerNotice
    ) { threads, messagesMap, notice ->
        ChatsUiState(
            threads = threads,
            messagesByThread = messagesMap,
            composerNotice = notice
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ChatsUiState(
            threads = repository.threads.value,
            messagesByThread = repository.messagesByThread.value
        )
    )

    fun threadById(threadId: String): ChatThread {
        return repository.threads.value.firstOrNull { it.id == threadId }
            ?: repository.threads.value.first()
    }

    fun sendPreviewMessage(threadId: String, text: String) {
        val adminName = repository.activeFamilySpace.value.adminName
        when (val result = repository.sendPreviewMessage(threadId, text, adminName)) {
            is BackendActionResult.BackendNotConfigured -> {
                composerNotice.value =
                    "${result.featureName} is not active yet — requires ${result.requiredBackend}. Added to local UI preview only."
            }
            is BackendActionResult.ValidationError -> {
                composerNotice.value = result.message
            }
            is BackendActionResult.Success -> {
                composerNotice.value = null
            }
        }
    }

    fun clearNotice() {
        composerNotice.value = null
    }

    class Factory(private val repository: Phase1FoundationRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatsViewModel(repository) as T
        }
    }
}
