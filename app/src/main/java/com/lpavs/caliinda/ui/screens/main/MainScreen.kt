package com.lpavs.caliinda.ui.screens.main

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.ui.common.BackgroundShapes
import com.lpavs.caliinda.ui.screens.main.components.*
import com.lpavs.caliinda.ui.screens.main.components.AI.AiVisualizer
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.navigation.NavHostController
import com.lpavs.caliinda.data.calendar.EventNetworkState
import com.lpavs.caliinda.ui.common.BackgroundShapeContext
import com.lpavs.caliinda.ui.screens.main.components.calendarui.DayEventsPage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.CreateEventScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    navController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var textFieldState by remember { mutableStateOf(TextFieldValue("")) }
    val snackbarHostState = remember { SnackbarHostState() }
    var isTextInputVisible by remember { mutableStateOf(false) }
    val aiState by viewModel.aiState.collectAsState()
    val aiMessage by viewModel.aiMessage.collectAsState()
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

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        // Инициализируем текущей видимой датой из ViewModel
        initialSelectedDateMillis = currentVisibleDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli(),
    )
    val isBusy = uiState.isLoading || rangeNetworkState is EventNetworkState.Loading
    val isListening = uiState.isListening

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false // Чтобы лист либо полностью открыт, либо закрыт
    )
    var showCreateEventSheet by remember { mutableStateOf(false) }
    var selectedDateForSheet by remember { mutableStateOf<LocalDate>(today) }

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

    LaunchedEffect(uiState.deleteOperationError) {
        uiState.deleteOperationError?.let { error ->
            snackbarHostState.showSnackbar("Ошибка удаления: $error")
            viewModel.clearDeleteError() // Сбрасываем ошибку после показа
        }
    }
    // TODO: Добавь обработку rangeNetworkState.Error, если нужно показывать снекбар и для этого

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        viewModel.updatePermissionStatus(hasPermission)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CalendarAppBar( // Передаем только нужные данные
                onNavigateToSettings = onNavigateToSettings,
                onGoToTodayClick = {
                    scope.launch {
                        if (pagerState.currentPage != initialPageIndex) {
                            // 1. Обновляем ViewModel *до* скролла
                            viewModel.onVisibleDateChanged(today)
                            // 2. Анимируем скролл
                            pagerState.animateScrollToPage(initialPageIndex)
                        } else {
                            // Если уже на сегодня, просто обновляем данные
                            viewModel.refreshCurrentVisibleDate()
                        }
                    }
                },
                onTitleClick = {
                    // Обновляем состояние DatePicker текущей датой перед показом
                    datePickerState.selectableDates

                    showDatePicker = true // Показываем диалог
                },
                date = currentVisibleDate
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // --- Слой 1: Фон (самый нижний) ---
            BackgroundShapes(BackgroundShapeContext.Main)

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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isBusy && !isListening) {
                    LoadingIndicator()
                }
            }
            // --- Слой 3: AI Visualizer (рисуется поверх фона и списка) ---
            AiVisualizer(
                aiState = aiState,
                aiMessage = aiMessage,
                modifier = Modifier.fillMaxSize(), // Занимает всю область Box, но рисует где нужно
                onResultShownTimeout = { viewModel.resetAiStateAfterResult() },
                onAskingShownTimeout = { viewModel.resetAiStateAfterAsking() }
            )
            BottomBar(
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
                viewModel,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset),
                onCreateEventClick = {
                    selectedDateForSheet = currentVisibleDate // Use current visible date from Pager
                    showCreateEventSheet = true
                }
            )
        } // End основной Box
    } // End Scaffold

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false // Скрываем диалог
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val selectedDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.systemDefault()) // Используй корректную ZoneId
                                .toLocalDate()

                            // Проверяем, изменилась ли дата
                            if (selectedDate != currentVisibleDate) {
                                // 1. Сообщаем ViewModel о новой дате *до* скролла
                                Log.d(
                                    "DatePicker",
                                    "Date selected: $selectedDate. Updating ViewModel."
                                )
                                viewModel.onVisibleDateChanged(selectedDate)

                                // 2. Рассчитываем целевую страницу
                                val daysDifference = ChronoUnit.DAYS.between(today, selectedDate)
                                val targetPageIndex = (initialPageIndex + daysDifference)
                                    // Ограничиваем индекс на всякий случай
                                    .coerceIn(0L, Int.MAX_VALUE.toLong() - 1L).toInt()

                                // 3. Запускаем скролл к странице (используем scrollToPage для мгновенного перехода)
                                scope.launch {
                                    Log.d(
                                        "DatePicker",
                                        "Scrolling Pager to page index: $targetPageIndex"
                                    )
                                    pagerState.scrollToPage(targetPageIndex)
                                }
                            } else {
                                Log.d(
                                    "DatePicker",
                                    "Selected date $selectedDate is the same as current $currentVisibleDate. No action."
                                )
                            }
                        } else {
                            Log.w("DatePicker", "Confirm clicked but selectedDateMillis is null.")
                        }
                    },
                    // Кнопка активна, только если дата выбрана
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("OK") // Используем Text из M3
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена") // Используем Text из M3
                }
            }
        ) {
            // Сам DatePicker
            DatePicker(state = datePickerState)
        }
    }
    if (showCreateEventSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateEventSheet = false },
            sheetState = sheetState,
            contentWindowInsets = { WindowInsets.navigationBars } // Recommended for system nav bar
        ) {
            // Use a Box to layer the scrollable content and the fixed button row
            CreateEventScreen(
                viewModel = viewModel,
                initialDate = selectedDateForSheet, // Pass the selected date
                onDismiss = { // Lambda for your sheet content to call when it wants to close
                    scope.launch {
                        sheetState.hide() // Programmatically hide the sheet
                    }.invokeOnCompletion {
                        // This ensures state is updated even if hide() is interrupted
                        if (!sheetState.isVisible) {
                            showCreateEventSheet = false
                        }
                    }
                },
                currentSheetValue = sheetState.currentValue
            )
        }
    }
    LaunchedEffect(sheetState.isVisible) {
        if (!sheetState.isVisible && showCreateEventSheet) {
            showCreateEventSheet = false
        }
    }
}
