package com.neel.aaila.data.repository

import android.content.Context
import com.neel.aaila.data.network.AailaApiService
import com.neel.aaila.data.network.LiveKitTokenRequest
import com.neel.aaila.data.network.TokenManager
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class GroupParticipant(
    val id: String,
    val name: String,
    val initials: String,
    val isAudioMuted: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val isSpeaking: Boolean = false
)

data class GroupCallState(
    val roomId: String = "",
    val roomName: String = "",
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isMicMuted: Boolean = false,
    val isCameraEnabled: Boolean = true,
    val activeSpeakerId: String? = null,
    val participants: List<GroupParticipant> = emptyList(),
    val errorMessage: String? = null
)

class LiveKitGroupCallManager(
    private val context: Context,
    private val familyAuthRepository: FamilyAuthRepository,
    private val familyDirectoryRepository: FamilyDirectoryRepository,
    private val apiService: AailaApiService,
    private val tokenManager: TokenManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _groupCallState = MutableStateFlow(GroupCallState())
    val groupCallState: StateFlow<GroupCallState> = _groupCallState.asStateFlow()

    private var activeRoom: Room? = null

    fun joinGroupRoom(roomName: String): Boolean {
        val currentSpace = familyAuthRepository.activeFamilySpace.value
        val adminName = currentSpace.adminName
        val members = familyDirectoryRepository.members.value.take(5)
        val resolvedRoomName = roomName.ifEmpty { "Family Circle Call" }
        val roomId = roomName.ifEmpty { "room_${UUID.randomUUID().toString().take(8)}" }

        val initialParticipants = members.mapIndexed { index, m ->
            GroupParticipant(
                id = m.id,
                name = m.name,
                initials = m.initials,
                isAudioMuted = index % 3 == 0,
                isVideoEnabled = true,
                isSpeaking = index == 0
            )
        }.ifEmpty {
            listOf(
                GroupParticipant(
                    id = tokenManager.getUserId() ?: "user_me",
                    name = adminName,
                    initials = adminName.take(2).uppercase(),
                    isSpeaking = true
                )
            )
        }

        // If in UI Preview mode (no backend session), show local preview room without network call
        if (currentSpace.isPreviewSession || !tokenManager.isLoggedIn()) {
            _groupCallState.value = GroupCallState(
                roomId = roomId,
                roomName = resolvedRoomName,
                isConnected = true,
                isConnecting = false,
                isMicMuted = false,
                isCameraEnabled = true,
                activeSpeakerId = initialParticipants.firstOrNull()?.id,
                participants = initialParticipants
            )
            return true
        }

        // Authenticated mode: fetch LiveKit token from Vercel backend and connect
        _groupCallState.value = GroupCallState(
            roomId = roomId,
            roomName = resolvedRoomName,
            isConnected = false,
            isConnecting = true,
            participants = initialParticipants
        )

        scope.launch {
            try {
                val tokenResponse = withContext(Dispatchers.IO) {
                    apiService.getLiveKitToken(
                        LiveKitTokenRequest(
                            roomName = roomId,
                            participantName = adminName
                        )
                    )
                }

                val body = tokenResponse.body()
                if (tokenResponse.isSuccessful && body != null && body.success &&
                    !body.token.isNullOrBlank() && !body.url.isNullOrBlank()
                ) {
                    val room = LiveKit.create(context.applicationContext)
                    room.connect(url = body.url, token = body.token)
                    activeRoom = room

                    _groupCallState.value = GroupCallState(
                        roomId = body.room ?: roomId,
                        roomName = resolvedRoomName,
                        isConnected = true,
                        isConnecting = false,
                        isMicMuted = false,
                        isCameraEnabled = true,
                        activeSpeakerId = initialParticipants.firstOrNull()?.id,
                        participants = initialParticipants,
                        errorMessage = null
                    )
                } else {
                    _groupCallState.value = _groupCallState.value.copy(
                        isConnected = false,
                        isConnecting = false,
                        errorMessage = body?.error ?: "Failed to obtain LiveKit call token."
                    )
                }
            } catch (e: Exception) {
                _groupCallState.value = _groupCallState.value.copy(
                    isConnected = false,
                    isConnecting = false,
                    errorMessage = e.localizedMessage ?: "LiveKit connection failed."
                )
            }
        }

        return true
    }

    fun toggleMic() {
        val current = _groupCallState.value
        val newMuted = !current.isMicMuted
        scope.launch {
            try {
                activeRoom?.localParticipant?.setMicrophoneEnabled(!newMuted)
            } catch (_: Exception) {
            }
        }
        _groupCallState.value = current.copy(isMicMuted = newMuted)
    }

    fun toggleCamera() {
        val current = _groupCallState.value
        val newCameraEnabled = !current.isCameraEnabled
        scope.launch {
            try {
                activeRoom?.localParticipant?.setCameraEnabled(newCameraEnabled)
            } catch (_: Exception) {
            }
        }
        _groupCallState.value = current.copy(isCameraEnabled = newCameraEnabled)
    }

    fun leaveRoom() {
        try {
            activeRoom?.disconnect()
            activeRoom?.release()
        } catch (_: Exception) {
        }
        activeRoom = null
        _groupCallState.value = GroupCallState(isConnected = false)
    }
}
