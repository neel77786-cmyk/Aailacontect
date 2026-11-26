package com.neel.aaila.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.neel.aaila.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.aailaPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aaila_settings"
)

class ThemePreferencesRepository(private val context: Context) {

    private val themeModeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.aailaPreferencesDataStore.data.map { prefs ->
        val stored = prefs[themeModeKey]
        ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.aailaPreferencesDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }
}
