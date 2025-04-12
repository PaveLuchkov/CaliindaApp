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
import androidx.compose.runtime.Composable // Для @Composable AppNavHost


// Math (you might want to put these inside a utility class/file)

enum class AiVisualizerState {
    IDLE, RECORDING, THINKING, ASKING, RESULT, ERROR // Добавил состояние ERROR
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 1. Получаем ViewModel с помощью делегата, привязанного к Activity
    private val mainViewModel: MainViewModel by viewModels()

    // 2. Лаунчер регистрируем как поле Activity
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Используем mainViewModel, который теперь является свойством Activity
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        mainViewModel.handleSignInResult(task) // Обновляем ЕДИНСТВЕННЫЙ экземпляр

        // Более детальная обработка ошибок, если нужно
        if (result.resultCode != RESULT_OK) {
            Log.w("MainActivity", "Google Sign-In failed or cancelled. ResultCode: ${result.resultCode}")
            // Можно передать фиктивный Task с ошибкой, если handleSignInResult это ожидает
            // или вызвать отдельный метод viewModel.handleSignInError(result.resultCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CaliindarTheme {
                // 3. Передаем Activity-scoped ViewModel и лямбду в NavHost
                AppNavHost(
                    viewModel = mainViewModel, // <--- Передаем экземпляр из Activity
                    onSignInClick = { // Лямбда захватывает свойство Activity 'googleSignInLauncher' и 'mainViewModel'
                        val signInIntent = mainViewModel.getSignInIntent()
                        // Добавим проверку на null для надежности
                        signInIntent.let {
                            googleSignInLauncher.launch(it)
                        }
                    }
                )
            }
        }
    }
}