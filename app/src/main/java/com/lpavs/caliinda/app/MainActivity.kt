package com.lpavs.caliinda.app

// Android Framework Imports

// Android Resources

// AndroidX Imports
// AndroidX Compose

// Accompanist

// Google APIs and Services

// Kotlin Coroutines

// Other Libraries

// Project-Specific Imports
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.ui.Modifier
import com.lpavs.caliinda.core.ui.theme.CaliindaTheme
import com.lpavs.caliinda.feature.settings.vm.SettingsViewModel
import com.lpavs.caliinda.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  private val settingsViewModel: SettingsViewModel by viewModels()
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    setContent {
      val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
      CaliindaTheme(
        themeMode = themeMode
      ) {
        AppNavHost(
            modifier = Modifier.background(colorScheme.background),
        )
      }
    }
  }
}
