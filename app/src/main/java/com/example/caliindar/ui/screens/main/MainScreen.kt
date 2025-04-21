package com.example.caliindar.ui.screens.main

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caliindar.ui.common.BackgroundShapes
import com.example.caliindar.ui.screens.main.components.*
import com.example.caliindar.ui.screens.main.components.AI.AiVisualizer
import com.example.caliindar.ui.screens.main.components.calendarui.EventsList
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
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
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle() // Собираем состояние событий
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEventsState.collectAsStateWithLifecycle()
    val eventNetworkState by viewModel.eventNetworkState.collectAsStateWithLifecycle()

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

        // SwipeRefresh
    val pullRefreshState = rememberPullRefreshState(
        refreshing = eventNetworkState is MainViewModel.EventNetworkState.Loading,
        onRefresh = { viewModel.refreshEvents() }
    )


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
                isBusy = uiState.isLoading || eventNetworkState is MainViewModel.EventNetworkState.Loading,
                isListening = uiState.isListening,
                onNavigateToSettings = onNavigateToSettings
                // TODO: Добавить кнопку/иконку для вызова DatePicker -> viewModel.setSelectedDate()
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
                onRecordStart = { viewModel.startListening() }, // Передаем лямбды для записи
                onRecordStopAndSend = { viewModel.stopListening() },
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
                .pullRefresh(pullRefreshState) // Применяем SwipeRefresh
        ) {
            // --- Слой 1: Фон (самый нижний) ---
            BackgroundShapes(
                colorScheme = MaterialTheme.colorScheme,
                modifier = Modifier.fillMaxSize()
            )

            // --- Слой 2: Контент (Заголовок + Список событий) ---
            Column(modifier = Modifier.fillMaxSize()) {
                // Заголовок
                Text(
                    text = "События на ${selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")))}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, // Сделаем жирнее
                    color = MaterialTheme.colorScheme.onSurface, // Используем основной цвет текста на фоне
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp) // Немного больше отступ
                )

                // Список Событий (Занимает оставшееся место в Column)
                EventsList(
                    events = calendarEvents,
                    timeFormatter = viewModel::formatEventListTime,
                    // Применяем Modifier для веса и фона (если нужен контраст)
                    modifier = Modifier
                        .weight(1f) // Занимает все доступное пространство в Column
                        .fillMaxWidth()
                    // .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)) // Очень легкий фон для списка, если нужно
                )
            } // End Column

            // --- Слой 3: AI Visualizer (рисуется поверх фона и списка) ---
            AiVisualizer(
                aiState = aiState,
                aiMessage = aiMessage,
                modifier = Modifier.fillMaxSize(), // Занимает всю область Box, но рисует где нужно
                onResultShownTimeout = { viewModel.resetAiStateAfterResult() },
                onAskingShownTimeout = { viewModel.resetAiStateAfterAsking() }
            )

            // --- Слой 4: Индикатор SwipeRefresh (поверх всего остального) ---
            PullRefreshIndicator(
                refreshing = eventNetworkState is MainViewModel.EventNetworkState.Loading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter), // Выравниваем по верху Box
            )

        } // End основной Box
    } // End Scaffold
} // End MainScreen