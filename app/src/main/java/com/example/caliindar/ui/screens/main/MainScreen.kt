package com.example.caliindar.ui.screens.main

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caliindar.ui.common.BackgroundShapes // <-- Импорт
import com.example.caliindar.ui.screens.main.components.* // <-- Импорт компонентов
import com.example.caliindar.ui.screens.main.components.AI.AiVisualizer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var textFieldState by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    var isTextInputVisible by remember { mutableStateOf(false) }
    val aiState by viewModel.aiState.collectAsState()
    val aiMessage by viewModel.aiMessage.collectAsState()
    val isAiRotating by viewModel.isAiRotating.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- УБРАЛИ дублирующийся googleSignInLauncher отсюда ---

    // --- Side Effects (Snackbar) ---
    LaunchedEffect(uiState.showGeneralError) {
        uiState.showGeneralError?.let { error ->
            snackbarHostState.showSnackbar("Ошибка: $error")
            viewModel.clearGeneralError()
        }
    }
    LaunchedEffect(uiState.showAuthError) {
        uiState.showAuthError?.let { error ->
            snackbarHostState.showSnackbar(error) // Ошибки аутентификации часто уже содержат пояснение
            viewModel.clearAuthError()
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.d("MainScreen", "Permission result: $isGranted")
        viewModel.updatePermissionStatus(isGranted)
        if (!isGranted) {
            // Handle permission denial (e.g., show message)
            scope.launch { /* Show snackbar or message */ }
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        viewModel.updatePermissionStatus(hasPermission)
    }


    var isFabPressed by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CalendarAppBar( // Передаем только нужные данные
                isLoading = uiState.isLoading,
                isRecording = uiState.isRecording,
                onNavigateToSettings = onNavigateToSettings
            )
        },
        bottomBar = {
            ChatInputBar(
                uiState = uiState, // Передаем весь uiState, т.к. Bar зависит от многих полей
                textFieldValue = textFieldState,
                onTextChanged = { textFieldState = it },
                onSendClick = {
                    viewModel.sendTextMessage(textFieldState.text)
                    textFieldState = TextFieldValue("") // Очищаем поле после отправки
                },
                onRecordStart = { viewModel.startRecording() }, // Передаем лямбды для записи
                onRecordStopAndSend = { viewModel.stopRecordingAndSend() },
                onUpdatePermissionResult = { granted -> viewModel.updatePermissionStatus(granted) }, // Передаем лямбду для обновления разрешений
                isTextInputVisible = isTextInputVisible,
                onToggleTextInput = { isTextInputVisible = !isTextInputVisible }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Используем BackgroundShapes из папки common
            BackgroundShapes(MaterialTheme.colorScheme) // Передаем текущую цветовую схему

            AiVisualizer(
                aiState = aiState,
                aiMessage = aiMessage,
                modifier = Modifier.fillMaxSize(), // Visualizer's container fills the Box
                uriHandler = uriHandler,
                onResultShownTimeout = { viewModel.resetAiStateAfterResult() }
            )


        }
    }
}