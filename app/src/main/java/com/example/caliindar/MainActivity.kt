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

// Kotlin Coroutines

// Other Libraries
import com.example.caliindar.navigation.AppNavHost

// Project-Specific Imports
import com.example.caliindar.ui.theme.CaliindarTheme
import com.example.caliindar.ui.screens.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

// Math (you might want to put these inside a utility class/file)

enum class AiVisualizerState {
    IDLE, RECORDING, THINKING, ASKING, RESULT, ERROR // Добавил состояние ERROR
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Лаунчер остается здесь, так как он нужен для SettingsScreen через AppNavHost
    private lateinit var googleSignInLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CaliindarTheme {
                // Получаем ViewModel здесь, если она нужна для обработки результата лаунчера
                // Либо передаем обработку результата через лямбду в NavHost -> Screen,
                // где ViewModel будет получен через hiltViewModel()
                // Оставим получение ViewModel внутри Composable, где он нужен
                val viewModel: MainViewModel = hiltViewModel()

                googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = { result ->
                        // Логика обработки результата остается здесь, используем полученный viewModel
                        if (result.resultCode == RESULT_OK) {
                            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                            viewModel.handleSignInResult(task)
                        } else {
                            Log.w("MainActivity", "Google Sign-In failed or cancelled in Activity. ResultCode: ${result.resultCode}")
                            viewModel.handleSignInResult(
                                Tasks.forException(
                                    ApiException(
                                        Status(
                                            result.resultCode,
                                            "Google Sign-In flow was cancelled or failed (Activity Result code: ${result.resultCode})."
                                        )
                                    )
                                )
                            )
                        }
                    }
                )

                // Передаем лямбду для запуска Sign-In в NavHost
                // ViewModel будет получен внутри экранов через hiltViewModel()
                AppNavHost(
                    onSignInClick = { // Тип этой лямбды () -> Unit
                        val signInIntent = viewModel.getSignInIntent() // Используем захваченный viewModel
                        if (signInIntent != null) {
                            googleSignInLauncher.launch(signInIntent) // Используем захваченный launcher
                        } else {
                            Log.e("MainActivity", "Failed to get sign-in intent for launcher.")
                        }
                    }
                )
            }
        }
    }
}