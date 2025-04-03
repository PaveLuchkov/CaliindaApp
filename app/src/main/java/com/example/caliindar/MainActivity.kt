package com.example.caliindar

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels // Для by viewModels()
import androidx.compose.foundation.layout.*
import androidx.compose.material3.* // Используем Material 3
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Рекомендуемый способ сбора StateFlow
import com.example.caliindar.ui.theme.CaliindarTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

class MainActivity : ComponentActivity() {

    // Получаем ViewModel с помощью делегата
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CaliindarTheme { // Оборачиваем в вашу тему
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

// --- Главный Composable экрана ---
@Composable
fun MainScreen(viewModel: MainViewModel) {
    // Собираем состояние из ViewModel
    // collectAsStateWithLifecycle безопасен для жизненного цикла
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current // Получаем контекст для Toast и разрешений

    // --- Лаунчеры для Activity Result ---

    // Лаунчер для Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                viewModel.handleSignInResult(task)
            } else {
                // Обработка отмены или ошибки входа
                viewModel.handleSignInResult(
                    com.google.android.gms.tasks.Tasks.forException(
                        ApiException(
                            com.google.android.gms.common.api.Status(
                                result.resultCode, // Используем resultCode как статус ошибки
                                "Google Sign-In flow was cancelled or failed."
                            )
                        )
                    )
                )
            }
        }
    )

    // Лаунчер для запроса разрешения на запись аудио
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            viewModel.setAudioPermissionGranted(isGranted)
            if (isGranted) {
                // Разрешение получено, можно попробовать начать запись снова, если нужно
                // viewModel.toggleRecording() // Или просто обновить состояние
                showToast(context, "Разрешение на запись получено")
            } else {
                showToast(context, "Разрешение на запись НЕ предоставлено")
            }
        }
    )

    // --- Обработка Side Effects (например, показ Toast) ---
    val generalError = uiState.showGeneralError
    LaunchedEffect(generalError) { // Запускается, когда generalError меняется (и не null)
        if (generalError != null) {
            showToast(context, "Ошибка: $generalError")
            viewModel.clearGeneralError() // Сбрасываем ошибку после показа
        }
    }

    val authError = uiState.showAuthError
    LaunchedEffect(authError) { // Отдельный эффект для ошибок аутентификации
        if (authError != null) {
            showToast(context, authError) // Показываем сообщение как есть
            viewModel.clearAuthError() // Сбрасываем ошибку после показа
        }
    }

    // --- UI ---
    Surface( // Используем Surface из Material 3
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Отображение статуса/сообщения
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodyLarge, // Стили текста из темы M3
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Индикатор загрузки
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
            }

            // Кнопка Записи/Стоп
            Button(
                onClick = {
                    // Проверяем разрешение ПЕРЕД попыткой записи
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) -> {
                            // Разрешение есть, вызываем ViewModel
                            viewModel.toggleRecording()
                        }
                        else -> {
                            // Разрешения нет, запрашиваем
                            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                // Кнопка доступна только если пользователь вошел и нет загрузки
                enabled = uiState.isSignedIn && !uiState.isLoading,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(text = uiState.recordButtonText)
            }

            Spacer(modifier = Modifier.height(20.dp)) // Пространство между кнопками

            // Кнопка Входа (видна, если пользователь НЕ вошел)
            if (!uiState.isSignedIn) {
                Button(
                    onClick = {
                        val signInIntent = viewModel.getSignInIntent()
                        googleSignInLauncher.launch(signInIntent)
                    },
                    // Блокируем кнопку во время загрузки
                    enabled = !uiState.isLoading,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text("Войти через Google")
                }
            }

            // Кнопка Выхода (видна, если пользователь вошел)
            if (uiState.isSignedIn) {
                Button(
                    onClick = { viewModel.signOut() },
                    // Блокируем кнопку во время загрузки
                    enabled = !uiState.isLoading
                ) {
                    Text("Выйти из аккаунта")
                }
                // Отображение email вошедшего пользователя (опционально)
                uiState.userEmail?.let { email ->
                    Text(
                        text = "Вошли как: $email",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

// Вспомогательная функция для Toast
private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

// --- Preview для Android Studio ---
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CaliindarTheme {
        // НЕ СОЗДАЕМ ЗДЕСЬ НАСТОЯЩИЙ ViewModel!
        // Вызываем Composable с дефолтным состоянием или моковыми данными.
        PreviewScreenContent() // Использует uiState по умолчанию из своей сигнатуры
    }
}

@Composable
fun PreviewScreenContent(
    uiState: MainUiState = MainUiState(), // Дефолтное состояние для превью
    onRecordClick: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
            }

            Button(
                onClick = onRecordClick,
                enabled = uiState.isSignedIn && !uiState.isLoading,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(text = uiState.recordButtonText)
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!uiState.isSignedIn) {
                Button(
                    onClick = onSignInClick,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text("Войти через Google")
                }
            }

            if (uiState.isSignedIn) {
                Button(
                    onClick = onSignOutClick,
                    enabled = !uiState.isLoading
                ) {
                    Text("Выйти из аккаунта")
                }
                uiState.userEmail?.let { email ->
                    Text(
                        text = "Вошли как: $email",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

// Пример превью с разными состояниями
@Preview(showBackground = true, name = "Signed In State")
@Composable
fun SignedInPreview() {
    CaliindarTheme {
        PreviewScreenContent(
            uiState = MainUiState(
                message = "Аккаунт: test@example.com (Авторизован)",
                isSignedIn = true,
                userEmail = "test@example.com",
                isPermissionGranted = true
            )
        )
    }
}

@Preview(showBackground = true, name = "Signed Out State")
@Composable
fun SignedOutPreview() {
    CaliindarTheme {
        PreviewScreenContent(
            uiState = MainUiState(
                message = "Требуется вход и авторизация",
                isSignedIn = false
            )
        )
    }
}

@Preview(showBackground = true, name = "Recording State")
@Composable
fun RecordingPreview() {
    CaliindarTheme {
        PreviewScreenContent(
            uiState = MainUiState(
                message = "Запись началась...",
                isSignedIn = true,
                userEmail = "test@example.com",
                isPermissionGranted = true,
                isRecording = true,
                recordButtonText = "Стоп"
            )
        )
    }
}