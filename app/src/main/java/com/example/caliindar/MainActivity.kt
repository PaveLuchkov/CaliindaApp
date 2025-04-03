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
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material3.* // Используем Material 3
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Mic
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerInputChange

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

@Composable
fun RecordButton(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // Scope для запуска корутин (Toast, ViewModel)

    // Лаунчер для запроса разрешения
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        viewModel.updatePermissionStatus(isGranted) // Обновляем статус в ViewModel
        if (!isGranted) {
            Toast.makeText(context, "Разрешение на запись отклонено.", Toast.LENGTH_LONG).show()
            Log.w("RecordButton", "Permission denied by user.")
        } else {
            Log.i("RecordButton", "Permission granted by user.")
        }
    }

    // Цвета и состояние enabled
    val fabBackgroundColor = if (uiState.isRecording) {
        MaterialTheme.colorScheme.error // Красный во время записи
    } else {
        MaterialTheme.colorScheme.primary // Стандартный цвет
    }
    val fabContentColor = contentColorFor(fabBackgroundColor)
    // Кнопка активна, если пользователь вошел, не идет загрузка
    val isEnabled = uiState.isSignedIn && !uiState.isLoading

    FloatingActionButton(
        onClick = {
            Log.d("RecordButton", "FAB onClick triggered (should be ignored)")
        },
        containerColor = fabBackgroundColor,
        contentColor = fabContentColor,
        modifier = modifier
            .padding(bottom = 12.dp)
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput

                awaitPointerEventScope {
                    while (true) {
                        // Ожидаем ПЕРВОГО нажатия пальцем вниз
                        val down: PointerInputChange = awaitFirstDown(requireUnconsumed = false)
                        Log.d("RecordButton", "Pointer DOWN detected (awaitFirstDown).")

                        // Проверяем разрешение СРАЗУ после нажатия
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        // Обновляем статус разрешения в ViewModel в любом случае
                        viewModel.updatePermissionStatus(hasPermission)

                        if (hasPermission) {
                            Log.d("RecordButton", "Permission OK. Starting recording...")
                            // Поглощаем событие Down, чтобы оно не вызвало onClick или другие жесты
                            down.consume()

                            // Пытаемся начать запись (ViewModel сделает доп. проверки)
                            // Запускаем в отдельной корутине, чтобы не блокировать UI поток
                            scope.launch { viewModel.startRecording() }

                            try {
                                // Ожидаем отпускания пальца или отмены жеста
                                val upOrCancel: PointerInputChange? = waitForUpOrCancellation()

                                if (upOrCancel != null) {
                                    Log.d("RecordButton", "Pointer UP/CANCEL detected (waitForUpOrCancellation). Consuming event.")
                                    upOrCancel.consume()
                                } else {
                                    Log.w("RecordButton", "Gesture CANCELLED (waitForUpOrCancellation returned null).")
                                }
                            } finally {
                                Log.i("RecordButton", "Stopping recording (finally block)...")
                                scope.launch { viewModel.stopRecordingAndSend() }
                            }
                        } else {
                            Log.d("RecordButton", "Permission needed. Launching request...")
                            down.consume()

                            // Запускаем лаунчер разрешений
                            scope.launch {
                                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                Toast.makeText(context, "Нужно разрешение на запись звука", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = if (uiState.isRecording) "Идет запись (Отпустите для остановки)" else "Начать запись (Нажмите и удерживайте)"
        )
    }
}


// --- Главный Composable экрана ---
@Composable
fun MainScreen(viewModel: MainViewModel) {
    // collectAsStateWithLifecycle безопасен для жизненного цикла
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
            val uiState by viewModel.uiState.collectAsState() // Получаем состояние
            RecordButton(uiState = uiState, viewModel = viewModel)

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
                isRecording = true
            )
        )
    }
}