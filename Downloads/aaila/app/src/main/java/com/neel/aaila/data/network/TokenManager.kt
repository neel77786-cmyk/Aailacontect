package com.neel.aaila.data.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the user's JWT session token and identity metadata in encrypted storage.
 * Falls back to standard private SharedPreferences only if Android Keystore is unavailable
 * (e.g., in Robolectric unit tests).
 */
class TokenManager(context: Context) {

    private val prefs: SharedPreferences = createPreferences(context.applicationContext)

    private val _sessionExpiredEvent = MutableStateFlow(false)
    val sessionExpiredEvent: StateFlow<Boolean> = _sessionExpiredEvent.asStateFlow()

    private fun createPreferences(appContext: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Throwable) {
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun saveSession(
        token: String,
        userId: String,
        familyId: String,
        role: String
    ) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_FAMILY_ID, familyId)
            .putString(KEY_ROLE, role)
            .apply()
        _sessionExpiredEvent.value = false
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getFamilyId(): String? = prefs.getString(KEY_FAMILY_ID, null)

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun notifyUnauthorized() {
        clearSession()
        _sessionExpiredEvent.value = true
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "aaila_secure_session"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FAMILY_ID = "family_id"
        private const val KEY_ROLE = "user_role"
    }
}
