package com.neel.aaila

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neel.aaila.data.model.ThemeMode
import com.neel.aaila.di.AailaAppContainer
import com.neel.aaila.ui.navigation.AailaNavHost
import com.neel.aaila.ui.theme.AailaTheme

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AailaAppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appContainer = AailaAppContainer(applicationContext)

        setContent {
            val themeMode by appContainer.themePreferencesRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            AailaTheme(themeMode = themeMode) {
                AailaNavHost(container = appContainer)
            }
        }
    }
}
