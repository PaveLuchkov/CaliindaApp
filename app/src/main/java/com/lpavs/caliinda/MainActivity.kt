package com.lpavs.caliinda

// Android Framework Imports

// Android Resources

// AndroidX Imports
// AndroidX Compose

// Accompanist

// Google APIs and Services

// Kotlin Coroutines

// Other Libraries

// Project-Specific Imports
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.ui.Modifier
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.lpavs.caliinda.navigation.AppNavHost
import com.lpavs.caliinda.ui.screens.main.MainViewModel
import com.lpavs.caliinda.ui.theme.CaliindaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private val mainViewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    setContent {
      CaliindaTheme {
        AppNavHost(
            viewModel = mainViewModel,
            modifier = Modifier.background(colorScheme.background),
        )
      }
    }
  }
}
