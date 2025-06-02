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

    // 1. Получаем ViewModel с помощью делегата, привязанного к Activity
    private val mainViewModel: MainViewModel by viewModels()

    // 2. Регистрация ActivityResultLauncher для Google Sign-In
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Инициализация лаунчера ДО setContent (или в классе)
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // Результат приходит сюда
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Передаем результат в ViewModel для обработки
                mainViewModel.handleSignInResult(task)
            } catch (e: ApiException) {
                // Обработка ошибки на уровне ViewModel
                mainViewModel.handleSignInResult(task) // Передаем даже если ошибка, ViewModel разберется
                Log.w("MainActivity", "Google sign in failed in launcher", e)
                // Можно показать Snackbar или Toast с общей ошибкой, если нужно
            } catch (e: Exception) {
                mainViewModel.handleSignInResult(task) // Передаем на всякий случай
                Log.e("MainActivity", "Error handling sign in result in launcher", e)
            }
        }

        setContent {
            CaliindaTheme {
                // 3. Передаем Activity-scoped ViewModel и лямбду в NavHost
                AppNavHost(
                    // --- УБЕДИСЬ, что AppNavHost принимает MainViewModel ---
                    viewModel = mainViewModel,
                    onSignInClick = {
                        // Получаем Intent от ViewModel
                        val signInIntent = mainViewModel.getSignInIntent()
                        // Запускаем процесс входа через лаунчер
                        // .let не обязателен, т.к. getSignInIntent должен всегда возвращать Intent
                        googleSignInLauncher.launch(signInIntent)
                    },
                    modifier = Modifier.background(colorScheme.background),
                )
            }
        }
    }
}