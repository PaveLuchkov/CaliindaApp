package com.example.caliindar

// Android Framework Imports
import android.content.Intent
import android.os.Bundle
import android.util.Log

// Android Resources

// AndroidX Imports
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
// AndroidX Compose

// Accompanist

// Google APIs and Services
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Tasks

// Other Libraries
import com.example.caliindar.navigation.AppNavHost

// Project-Specific Imports
import com.example.caliindar.ui.theme.CaliindarTheme
import com.example.caliindar.ui.screens.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

// Math (you might want to put these inside a utility class/file)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Лаунчер остается здесь, так как он нужен для SettingsScreen через AppNavHost
    private lateinit var googleSignInLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CaliindarTheme {
                val viewModel: MainViewModel = hiltViewModel()
                AppNavHost(
                    onSignInClick = { // Тип этой лямбды () -> Unit
                        viewModel.startSignInProcess()
                    }
                )
            }
        }
    }
}