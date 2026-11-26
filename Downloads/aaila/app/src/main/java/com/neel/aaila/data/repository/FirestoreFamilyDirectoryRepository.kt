package com.neel.aaila.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.neel.aaila.data.model.BackendActionResult
import com.neel.aaila.data.model.FamilyMember
import com.neel.aaila.data.model.MemberRole
import com.neel.aaila.data.model.OnlineStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class FirestoreFamilyDirectoryRepository(
    private val fallbackRepository: Phase1FoundationRepository,
    private val familyAuthRepository: FamilyAuthRepository
) : FamilyDirectoryRepository {

    private val _members = MutableStateFlow<List<FamilyMember>>(fallbackRepository.members.value)
    override val members: StateFlow<List<FamilyMember>> = _members.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null
    private var currentFamilyId: String? = null

    init {
        listenToCurrentFamily()
    }

    private fun isFirebaseConfigured(): Boolean {
        return try {
            FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()
        } catch (_: Throwable) {
            false
        }
    }

    fun listenToCurrentFamily() {
        val familyId = familyAuthRepository.activeFamilySpace.value.familyId
        if (familyId == currentFamilyId && listenerRegistration != null) return
        currentFamilyId = familyId

        if (!isFirebaseConfigured()) {
            _members.value = fallbackRepository.members.value
            return
        }

        listenerRegistration?.remove()
        try {
            val firestore = FirebaseFirestore.getInstance()
            listenerRegistration = firestore.collection("families")
                .document(familyId)
                .collection("members")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

                    val memberList = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val name = doc.getString("name") ?: "Family Member"
                        val relationship = doc.getString("relationship") ?: "Member"
                        val roleStr = doc.getString("role") ?: MemberRole.MEMBER.name
                        val role = try { MemberRole.valueOf(roleStr) } catch (_: Exception) { MemberRole.MEMBER }
                        val statusStr = doc.getString("onlineStatus") ?: OnlineStatus.OFFLINE.name
                        val status = try { OnlineStatus.valueOf(statusStr) } catch (_: Exception) { OnlineStatus.OFFLINE }
                        val statusNote = doc.getString("statusNote") ?: ""
                        val initials = doc.getString("initials") ?: name.take(2).uppercase(Locale.US)
                        val avatarIndex = (doc.getLong("avatarColorIndex") ?: 0L).toInt()

                        FamilyMember(
                            id = id,
                            name = name,
                            relationship = relationship,
                            role = role,
                            status = status,
                            statusNote = statusNote,
                            initials = initials,
                            avatarColorIndex = avatarIndex,
                            lastSeenLabel = "Online"
                        )
                    }
                    if (memberList.isNotEmpty()) {
                        _members.value = memberList
                    }
                }
        } catch (_: Exception) {
            _members.value = fallbackRepository.members.value
        }
    }

    override fun findMemberById(memberId: String): FamilyMember? {
        return _members.value.firstOrNull { it.id == memberId }
            ?: fallbackRepository.findMemberById(memberId)
    }

    // Admin Operations
    fun adminCreateMember(
        name: String,
        relationship: String,
        role: MemberRole,
        tempPassword: String
    ): BackendActionResult<Unit> {
        val familyId = familyAuthRepository.activeFamilySpace.value.familyId
        if (!isFirebaseConfigured()) {
            return BackendActionResult.Success(Unit)
        }

        return try {
            val firestore = FirebaseFirestore.getInstance()
            val memberUid = UUID.randomUUID().toString()
            val initials = name.trim().take(2).uppercase(Locale.US)

            val memberData = mapOf(
                "uid" to memberUid,
                "name" to name.trim(),
                "relationship" to relationship.trim(),
                "role" to role.name,
                "status" to "APPROVED",
                "onlineStatus" to OnlineStatus.AVAILABLE.name,
                "statusNote" to "Joined family space",
                "initials" to initials,
                "avatarColorIndex" to (name.hashCode() % 6 + 6) % 6,
                "lastSeen" to Timestamp.now(),
                "createdAt" to Timestamp.now()
            )

            Tasks.await(
                firestore.collection("families")
                    .document(familyId)
                    .collection("members")
                    .document(memberUid)
                    .set(memberData),
                10,
                TimeUnit.SECONDS
            )

            BackendActionResult.Success(Unit)
        } catch (e: Exception) {
            BackendActionResult.ValidationError(e.localizedMessage ?: "Failed to create member.")
        }
    }

    fun adminUpdateMemberStatus(memberUid: String, newStatus: String): BackendActionResult<Unit> {
        val familyId = familyAuthRepository.activeFamilySpace.value.familyId
        if (!isFirebaseConfigured()) return BackendActionResult.Success(Unit)

        return try {
            val firestore = FirebaseFirestore.getInstance()
            Tasks.await(
                firestore.collection("families")
                    .document(familyId)
                    .collection("members")
                    .document(memberUid)
                    .update("status", newStatus),
                10,
                TimeUnit.SECONDS
            )
            BackendActionResult.Success(Unit)
        } catch (e: Exception) {
            BackendActionResult.ValidationError(e.localizedMessage ?: "Failed to update member status.")
        }
    }

    fun adminRemoveMember(memberUid: String): BackendActionResult<Unit> {
        val familyId = familyAuthRepository.activeFamilySpace.value.familyId
        if (!isFirebaseConfigured()) return BackendActionResult.Success(Unit)

        return try {
            val firestore = FirebaseFirestore.getInstance()
            Tasks.await(
                firestore.collection("families")
                    .document(familyId)
                    .collection("members")
                    .document(memberUid)
                    .delete(),
                10,
                TimeUnit.SECONDS
            )
            BackendActionResult.Success(Unit)
        } catch (e: Exception) {
            BackendActionResult.ValidationError(e.localizedMessage ?: "Failed to remove member.")
        }
    }
}
