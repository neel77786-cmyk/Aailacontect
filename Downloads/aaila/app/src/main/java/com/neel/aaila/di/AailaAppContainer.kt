package com.neel.aaila.di

import android.content.Context
import com.neel.aaila.data.network.AailaApiService
import com.neel.aaila.data.network.NetworkModule
import com.neel.aaila.data.network.TokenManager
import com.neel.aaila.data.preferences.ThemePreferencesRepository
import com.neel.aaila.data.repository.BackendFamilyAuthRepository
import com.neel.aaila.data.repository.CallingRepository
import com.neel.aaila.data.repository.FamilyAuthRepository
import com.neel.aaila.data.repository.FamilyDirectoryRepository
import com.neel.aaila.data.repository.FirestoreFamilyDirectoryRepository
import com.neel.aaila.data.repository.FirestoreMessagingRepository
import com.neel.aaila.data.repository.LiveKitGroupCallManager
import com.neel.aaila.data.repository.MessagingRepository
import com.neel.aaila.data.repository.Phase1FoundationRepository
import com.neel.aaila.data.repository.WebRtcCallingRepository
import com.neel.aaila.data.services.AudioRecorderManager

/**
 * Dependency Container for AAILA.
 * Wires the Vercel backend Retrofit client, secure JWT TokenManager, and repositories.
 */
class AailaAppContainer(context: Context) {
    private val appContext = context.applicationContext

    val themePreferencesRepository: ThemePreferencesRepository =
        ThemePreferencesRepository(appContext)

    val tokenManager: TokenManager = TokenManager(appContext)

    private val moshi = NetworkModule.createMoshi()
    private val okHttpClient = NetworkModule.createOkHttpClient(tokenManager)
    val apiService: AailaApiService = NetworkModule.createApiService(okHttpClient, moshi)

    val foundationRepository: Phase1FoundationRepository = Phase1FoundationRepository()

    val familyAuthRepository: FamilyAuthRepository =
        BackendFamilyAuthRepository(apiService, tokenManager, moshi, foundationRepository)

    val familyDirectoryRepository: FamilyDirectoryRepository =
        FirestoreFamilyDirectoryRepository(foundationRepository, familyAuthRepository)

    val messagingRepository: MessagingRepository =
        FirestoreMessagingRepository(foundationRepository, familyAuthRepository)

    val callingRepository: CallingRepository =
        WebRtcCallingRepository(appContext, foundationRepository, familyAuthRepository)

    val liveKitGroupCallManager: LiveKitGroupCallManager =
        LiveKitGroupCallManager(
            context = appContext,
            familyAuthRepository = familyAuthRepository,
            familyDirectoryRepository = familyDirectoryRepository,
            apiService = apiService,
            tokenManager = tokenManager
        )

    val audioRecorderManager: AudioRecorderManager =
        AudioRecorderManager(appContext)
}
