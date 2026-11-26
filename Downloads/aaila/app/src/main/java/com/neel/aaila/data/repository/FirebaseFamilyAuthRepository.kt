package com.neel.aaila.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neel.aaila.data.model.BackendActionResult
import com.neel.aaila.data.model.FamilySpaceSummary
import com.neel.aaila.data.model.MemberRole
import com.neel.aaila.data.model.OnlineStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.TimeUnit

class FirebaseFamilyAuthRepository(
    private val fallbackRepository: Phase1FoundationRepository
) : FamilyAuthRepository {

    private val _activeFamilySpace = MutableStateFlow(
        FamilySpaceSummary(
            familyId = "AAILA-HOME-01",
            familyName = "The Family Circle",
            adminName = "Neel",
            requireAdminApproval = true,
            privateDirectoryOnly = true,
            isPreviewSession = true
        )
    )
    override val activeFamilySpace: StateFlow<FamilySpaceSummary> = _activeFamilySpace.asStateFlow()

    private fun isFirebaseConfigured(): Boolean {
        return try {
            FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()
        } catch (_: Throwable) {
            false
        }
    }

    override fun signInWithFamilyId(
        familyId: String,
        password: String
    ): BackendActionResult<FamilySpaceSummary> {
        if (!isFirebaseConfigured()) {
            return fallbackRepository.signInWithFamilyId(familyId, password)
        }

        return try {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val cleanFamilyId = familyId.trim().uppercase(Locale.US)
            val syntheticEmail = "admin.$cleanFamilyId@aaila.family"

            val authTask = auth.signInWithEmailAndPassword(syntheticEmail, password)
            val authResult = Tasks.await(authTask, 10, TimeUnit.SECONDS)

            val uid = authResult.user?.uid
                ?: return BackendActionResult.ValidationError("Authentication failed: invalid credentials.")

            val memberTask = firestore.collection("families")
                .document(cleanFamilyId)
                .collection("members")
                .document(uid)
                .get()
            val memberDoc = Tasks.await(memberTask, 10, TimeUnit.SECONDS)

            if (!memberDoc.exists()) {
                auth.signOut()
                return BackendActionResult.ValidationError("Family member record not found in $cleanFamilyId.")
            }

            val status = memberDoc.getString("status") ?: "PENDING"
            if (status != "APPROVED") {
                auth.signOut()
                return BackendActionResult.ValidationError(
                    "Your access to family space $cleanFamilyId is currently $status. Please contact your family admin."
                )
            }

            val familyTask = firestore.collection("families").document(cleanFamilyId).get()
            val familyDoc = Tasks.await(familyTask, 10, TimeUnit.SECONDS)

            val familyName = familyDoc.getString("familyName") ?: "Family Space"
            val adminName = familyDoc.getString("adminName") ?: "Admin"

            val spaceSummary = FamilySpaceSummary(
                familyId = cleanFamilyId,
                familyName = familyName,
                adminName = adminName,
                requireAdminApproval = familyDoc.getBoolean("requireAdminApproval") ?: true,
                privateDirectoryOnly = familyDoc.getBoolean("privateDirectoryOnly") ?: true,
                isPreviewSession = false
            )

            _activeFamilySpace.value = spaceSummary
            BackendActionResult.Success(spaceSummary)
        } catch (e: Exception) {
            BackendActionResult.ValidationError(e.localizedMessage ?: "Sign-in failed. Please check credentials.")
        }
    }

    override fun createFamilySpaceAsAdmin(
        familyName: String,
        adminName: String,
        familyId: String,
        password: String,
        requireAdminApproval: Boolean,
        privateDirectoryOnly: Boolean
    ): BackendActionResult<FamilySpaceSummary> {
        if (!isFirebaseConfigured()) {
            return fallbackRepository.createFamilySpaceAsAdmin(
                familyName, adminName, familyId, password, requireAdminApproval, privateDirectoryOnly
            )
        }

        return try {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val cleanFamilyId = familyId.trim().uppercase(Locale.US)
            val syntheticEmail = "admin.$cleanFamilyId@aaila.family"

            val authTask = auth.createUserWithEmailAndPassword(syntheticEmail, password)
            val authResult = Tasks.await(authTask, 10, TimeUnit.SECONDS)

            val uid = authResult.user?.uid
                ?: return BackendActionResult.ValidationError("Admin account creation failed.")

            val familyData = mapOf(
                "familyId" to cleanFamilyId,
                "familyName" to familyName.trim(),
                "adminName" to adminName.trim(),
                "adminUid" to uid,
                "requireAdminApproval" to requireAdminApproval,
                "privateDirectoryOnly" to privateDirectoryOnly,
                "createdAt" to Timestamp.now()
            )

            val memberData = mapOf(
                "uid" to uid,
                "memberId" to "ADMIN",
                "name" to adminName.trim(),
                "relationship" to "Family Admin",
                "role" to MemberRole.ADMIN.name,
                "status" to "APPROVED",
                "onlineStatus" to OnlineStatus.AVAILABLE.name,
                "statusNote" to "Family Organizer",
                "initials" to adminName.trim().take(2).uppercase(Locale.US),
                "avatarColorIndex" to 0,
                "lastSeen" to Timestamp.now()
            )

            Tasks.await(firestore.collection("families").document(cleanFamilyId).set(familyData), 10, TimeUnit.SECONDS)
            Tasks.await(
                firestore.collection("families").document(cleanFamilyId)
                    .collection("members").document(uid).set(memberData),
                10,
                TimeUnit.SECONDS
            )

            val summary = FamilySpaceSummary(
                familyId = cleanFamilyId,
                familyName = familyName.trim(),
                adminName = adminName.trim(),
                requireAdminApproval = requireAdminApproval,
                privateDirectoryOnly = privateDirectoryOnly,
                isPreviewSession = false
            )

            _activeFamilySpace.value = summary
            BackendActionResult.Success(summary)
        } catch (e: Exception) {
            BackendActionResult.ValidationError(e.localizedMessage ?: "Failed to create Family Space.")
        }
    }

    override fun enterUiPreview(customSpace: FamilySpaceSummary?) {
        if (customSpace != null) {
            _activeFamilySpace.value = customSpace
        } else {
            fallbackRepository.enterUiPreview(null)
        }
    }
}
