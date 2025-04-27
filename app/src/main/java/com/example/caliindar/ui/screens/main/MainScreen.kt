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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.Alignment
import com.example.caliindar.ui.screens.main.components.calendarui.DayEventsPage
import java.time.LocalDate

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
    val today = remember { LocalDate.now() }
    val initialPageIndex = remember { Int.MAX_VALUE / 2 }
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { Int.MAX_VALUE } // Передаем лямбду, возвращающую "бесконечное" количество страниц
    )
    val currentVisibleDate by viewModel.currentVisibleDate.collectAsStateWithLifecycle()
    val rangeNetworkState by viewModel.rangeNetworkState.collectAsStateWithLifecycle()


    // --- НОВОЕ: Эффект для синхронизации Pager -> ViewModel ---
    LaunchedEffect(pagerState.settledPage) { // Реагируем, когда страница "устаканилась"
        val settledDate = today.plusDays((pagerState.settledPage - initialPageIndex).toLong())
        Log.d("MainScreen", "Pager settled on page ${pagerState.settledPage}, date: $settledDate")
        viewModel.onVisibleDateChanged(settledDate)
    }

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

    val isSignedIn = uiState.isSignedIn // Получаем статус входа
    LaunchedEffect(isSignedIn) { // Ключ = isSignedIn
        if (isSignedIn) { // Выполняем ТОЛЬКО если пользователь вошел
            Log.d("MainScreen", "User signed in, triggering initial load.")
            viewModel.ensureDateRangeLoadedAround(viewModel.currentVisibleDate.value)
            // или viewModel.ensureDateRangeLoadedAround(today) - выберите, что логичнее
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
                isLoading = uiState.isLoading || rangeNetworkState is MainViewModel.EventNetworkState.Loading,
                isBusy = uiState.isLoading || rangeNetworkState is MainViewModel.EventNetworkState.Loading,
                isListening = uiState.isListening,
                onNavigateToSettings = onNavigateToSettings,
                onGoToTodayClick = {
                    scope.launch {
                        // Анимированная прокрутка к странице "сегодня"
                        pagerState.animateScrollToPage(initialPageIndex)
                        // Опционально: можно сразу обновить видимую дату в ViewModel,
                        // чтобы заголовок обновился мгновенно, не дожидаясь settledPage
                        if (viewModel.currentVisibleDate.value != today) {
                            viewModel.onVisibleDateChanged(today)
                        }
                    }
                }
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
        ) {
            // --- Слой 1: Фон (самый нижний) ---
            BackgroundShapes(
                colorScheme = MaterialTheme.colorScheme,
                modifier = Modifier.fillMaxSize()
            )

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // Добавим key, чтобы помочь Pager различать страницы при изменениях
                key = { index -> today.plusDays((index - initialPageIndex).toLong()).toEpochDay() }
            ) { pageIndex ->
                // Рассчитываем дату для текущей страницы
                val pageDate = remember(pageIndex) {
                    today.plusDays((pageIndex - initialPageIndex).toLong())
                }

                // --- НОВОЕ: Компонент для одной страницы/дня ---
                DayEventsPage(
                    date = pageDate,
                    viewModel = viewModel,
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

        } // End основной Box
    } // End Scaffold
} // End MainScreen