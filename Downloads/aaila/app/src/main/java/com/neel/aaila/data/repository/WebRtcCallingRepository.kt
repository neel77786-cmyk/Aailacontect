package com.neel.aaila.data.repository

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.neel.aaila.data.model.BackendActionResult
import com.neel.aaila.data.model.CallDirection
import com.neel.aaila.data.model.CallLogEntry
import com.neel.aaila.data.model.CallType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

data class CallState(
    val callId: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val callType: CallType = CallType.VOICE,
    val direction: CallDirection = CallDirection.OUTGOING,
    val status: String = "IDLE", // IDLE, DIALING, RINGING, CONNECTED, ENDED, REJECTED
    val isMuted: Boolean = false,
    val isSpeaker: Boolean = true,
    val isFrontCamera: Boolean = true,
    val durationSeconds: Int = 0
)

class WebRtcCallingRepository(
    private val context: Context,
    private val fallbackRepository: Phase1FoundationRepository,
    private val familyAuthRepository: FamilyAuthRepository
) : CallingRepository {

    private val _callHistory = MutableStateFlow<List<CallLogEntry>>(fallbackRepository.callHistory.value)
    override val callHistory: StateFlow<List<CallLogEntry>> = _callHistory.asStateFlow()

    private val _currentCallState = MutableStateFlow(CallState())
    val currentCallState: StateFlow<CallState> = _currentCallState.asStateFlow()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var callListener: ListenerRegistration? = null

    init {
        initializeWebRtc()
    }

    private fun initializeWebRtc() {
        try {
            val options = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(options)

            peerConnectionFactory = PeerConnectionFactory.builder()
                .createPeerConnectionFactory()
        } catch (_: Exception) {
            // Graceful fallback
        }
    }

    private fun isFirebaseConfigured(): Boolean {
        return try {
            FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()
        } catch (_: Throwable) {
            false
        }
    }

    override fun initiateCall(memberId: String, callType: CallType): BackendActionResult<Unit> {
        val familyId = familyAuthRepository.activeFamilySpace.value.familyId
        val member = fallbackRepository.findMemberById(memberId)
        val memberName = member?.name ?: "Family Member"

        val callId = UUID.randomUUID().toString()
        _currentCallState.value = CallState(
            callId = callId,
            memberId = memberId,
            memberName = memberName,
            callType = callType,
            direction = CallDirection.OUTGOING,
            status = "DIALING"
        )

        if (!isFirebaseConfigured()) {
            return BackendActionResult.Success(Unit)
        }

        return try {
            val firestore = FirebaseFirestore.getInstance()

            val callData = mapOf(
                "callId" to callId,
                "familyId" to familyId,
                "targetMemberId" to memberId,
                "callerName" to familyAuthRepository.activeFamilySpace.value.adminName,
                "callType" to callType.name,
                "status" to "OFFERING",
                "createdAt" to Timestamp.now()
            )

            Tasks.await(
                firestore.collection("families")
                    .document(familyId)
                    .collection("calls")
                    .document(callId)
                    .set(callData),
                10,
                TimeUnit.SECONDS
            )

            // Listen for answer/rejection
            callListener = firestore.collection("families")
                .document(familyId)
                .collection("calls")
                .document(callId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val status = snapshot.getString("status") ?: "OFFERING"
                    _currentCallState.value = _currentCallState.value.copy(status = status)
                }

            startAudioTrack()
            BackendActionResult.Success(Unit)
        } catch (e: Exception) {
            BackendActionResult.ValidationError(e.localizedMessage ?: "Failed to initiate call.")
        }
    }

    private fun startAudioTrack() {
        try {
            peerConnectionFactory?.let { factory ->
                val constraints = MediaConstraints()
                audioSource = factory.createAudioSource(constraints)
                localAudioTrack = factory.createAudioTrack("ARDAMSa0", audioSource)
                localAudioTrack?.setEnabled(true)
            }
        } catch (_: Exception) {}
    }

    fun acceptIncomingCall(callId: String) {
        _currentCallState.value = _currentCallState.value.copy(status = "CONNECTED")
        val familyId = familyAuthRepository.activeFamilySpace.value.familyId
        if (!isFirebaseConfigured()) return

        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("families")
                .document(familyId)
                .collection("calls")
                .document(callId)
                .update("status", "CONNECTED")
        } catch (_: Exception) {}
    }

    fun rejectIncomingCall(callId: String) {
        _currentCallState.value = _currentCallState.value.copy(status = "REJECTED")
        val familyId = familyAuthRepository.activeFamilySpace.value.familyId
        if (!isFirebaseConfigured()) return

        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("families")
                .document(familyId)
                .collection("calls")
                .document(callId)
                .update("status", "REJECTED")
        } catch (_: Exception) {}
    }

    fun toggleMute() {
        val newMuted = !_currentCallState.value.isMuted
        localAudioTrack?.setEnabled(!newMuted)
        _currentCallState.value = _currentCallState.value.copy(isMuted = newMuted)
    }

    fun toggleSpeaker() {
        _currentCallState.value = _currentCallState.value.copy(
            isSpeaker = !_currentCallState.value.isSpeaker
        )
    }

    fun toggleCamera() {
        _currentCallState.value = _currentCallState.value.copy(
            isFrontCamera = !_currentCallState.value.isFrontCamera
        )
    }

    fun endCall() {
        callListener?.remove()
        callListener = null

        val state = _currentCallState.value
        val familyId = familyAuthRepository.activeFamilySpace.value.familyId

        if (state.callId.isNotEmpty() && isFirebaseConfigured()) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("families")
                    .document(familyId)
                    .collection("calls")
                    .document(state.callId)
                    .update("status", "ENDED")
            } catch (_: Exception) {}
        }

        // Add to call history log
        if (state.memberId.isNotEmpty()) {
            val log = CallLogEntry(
                id = UUID.randomUUID().toString(),
                memberId = state.memberId,
                memberName = state.memberName,
                relationship = "Family Member",
                initials = state.memberName.take(2).uppercase(),
                callType = state.callType,
                direction = state.direction,
                timestampLabel = "Just now",
                durationLabel = "${state.durationSeconds / 60}m ${state.durationSeconds % 60}s"
            )
            _callHistory.value = listOf(log) + _callHistory.value
        }

        try {
            localAudioTrack?.dispose()
            audioSource?.dispose()
            peerConnection?.close()
        } catch (_: Exception) {}

        localAudioTrack = null
        audioSource = null
        peerConnection = null
        _currentCallState.value = CallState(status = "ENDED")
    }
}
