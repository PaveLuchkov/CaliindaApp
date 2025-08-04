package com.lpavs.caliinda.feature.calendar.ui

import android.Manifest
import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.util.BackgroundShapeContext
import com.lpavs.caliinda.core.ui.util.BackgroundShapes
import com.lpavs.caliinda.feature.agent.ui.AiVisualizer
import com.lpavs.caliinda.feature.calendar.ui.components.BottomBar
import com.lpavs.caliinda.feature.calendar.ui.components.CalendarAppBar
import com.lpavs.caliinda.feature.calendar.ui.components.DayEventsPage
import com.lpavs.caliinda.feature.event_management.ui.create.CreateEventScreen
import com.lpavs.caliinda.feature.event_management.ui.details.CustomEventDetailsDialog
import com.lpavs.caliinda.feature.event_management.ui.edit.EditEventScreen
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringEventEditOptionsDialog
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import com.lpavs.caliinda.feature.settings.ui.LogInScreenDialog
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel,
    onNavigateToSettings: () -> Unit,
    eventManagementViewModel: EventManagementViewModel
) {
  val timeZone = eventManagementViewModel.timeZone.collectAsStateWithLifecycle()
  val calendarState by calendarViewModel.state.collectAsStateWithLifecycle()
  val aiState by calendarViewModel.aiState.collectAsState()
  val eventManagementState by eventManagementViewModel.state.collectAsState()
  var textFieldState by remember { mutableStateOf(TextFieldValue("")) }
  val snackbarHostState = remember { SnackbarHostState() }
  val isTextInputVisible by remember { mutableStateOf(false) }
  val aiMessage by calendarViewModel.aiMessage.collectAsState()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val today = remember { LocalDate.now() }
  val initialPageIndex = remember { Int.MAX_VALUE / 2 }
  val pagerState = rememberPagerState(initialPage = initialPageIndex, pageCount = { Int.MAX_VALUE })
  val currentVisibleDate by calendarViewModel.currentVisibleDate.collectAsStateWithLifecycle()
  val activity = context as? Activity
  val authorizationLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
              result.data?.let { calendarViewModel.handleAuthorizationResult(it) }
            } else {
              Log.w("MainScreen", "Authorization flow was cancelled by user.")
              calendarViewModel.signOut()
            }
          }
  val isOverallLoading = calendarState.isLoading || eventManagementState.isLoading

  LaunchedEffect(calendarState.authorizationIntent) {
    calendarState.authorizationIntent?.let { pendingIntent ->
      try {
        val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent).build()
        authorizationLauncher.launch(intentSenderRequest)
        calendarViewModel.clearAuthorizationIntent()
      } catch (e: Exception) {
        Log.e("MainScreen", "Couldn't start authorization UI", e)
      }
    }
  }

  var showDatePicker by remember { mutableStateOf(false) }
  val datePickerState =
      rememberDatePickerState(
          // Инициализируем текущей видимой датой из ViewModel
          initialSelectedDateMillis =
              currentVisibleDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
      )

  // --- НОВОЕ: Настройка flingBehavior ---
  val customFlingBehavior =
      PagerDefaults.flingBehavior(
          state = pagerState,
          // ---
          snapPositionalThreshold = 0.2f,
          // ---
          snapAnimationSpec =
              spring(
                  stiffness = Spring.StiffnessLow, // По умолчанию Spring.StiffnessMediumLow
              ))

  val sheetState =
      rememberModalBottomSheetState(
          skipPartiallyExpanded = false // Чтобы лист либо полностью открыт, либо закрыт
          )
  var showCreateEventSheet by remember { mutableStateOf(false) }
  var selectedDateForSheet by remember { mutableStateOf<LocalDate>(today) }

  var showEditEventSheet by remember { mutableStateOf(false) }
  val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

  // --- НОВОЕ: Эффект для синхронизации Pager -> ViewModel ---
  LaunchedEffect(pagerState.settledPage) { // Реагируем, когда страница "устаканилась"
    val settledDate = today.plusDays((pagerState.settledPage - initialPageIndex).toLong())
    Log.d("MainScreen", "Pager settled on page ${pagerState.settledPage}, date: $settledDate")
    calendarViewModel.onVisibleDateChanged(settledDate)
  }

  // --- Side Effects (Snackbar) ---
  LaunchedEffect(calendarState.showGeneralError) {
    calendarState.showGeneralError?.let { error ->
      snackbarHostState.showSnackbar("Ошибка: $error")
      calendarViewModel.clearGeneralError()
    }
  }
  LaunchedEffect(calendarState.showAuthError) {
    calendarState.showAuthError?.let { error ->
      snackbarHostState.showSnackbar(error) // Ошибки аутентификации часто уже содержат пояснение
      calendarViewModel.clearAuthError()
    }
  }

  LaunchedEffect(calendarState.deleteOperationError) {
    calendarState.deleteOperationError?.let { error ->
      snackbarHostState.showSnackbar("Ошибка удаления: $error")
      eventManagementViewModel.clearDeleteError() // Сбрасываем ошибку после показа
    }
  }
  // TODO: Добавь обработку rangeNetworkState.Error, если нужно показывать снекбар и для этого

  LaunchedEffect(Unit) {
    val hasPermission =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    calendarViewModel.updatePermissionStatus(hasPermission)
  }

  if (calendarState.showRecurringEditOptionsDialog && calendarState.eventBeingEdited != null) {
    RecurringEventEditOptionsDialog( // Вам нужно создать этот Composable
        eventName = calendarState.eventBeingEdited!!.summary,
        onDismiss = {
          eventManagementViewModel.cancelEditEvent()
        }, // Если пользователь закрыл диалог
        onOptionSelected = { choice ->
          eventManagementViewModel.onRecurringEditOptionSelected(choice)
        })
  }

  LaunchedEffect(calendarState.showEditEventDialog, calendarState.eventBeingEdited) {
    if (calendarState.showEditEventDialog && calendarState.eventBeingEdited != null) {
      showEditEventSheet = true
    } else {
      if (showEditEventSheet) {
        scope
            .launch { editSheetState.hide() }
            .invokeOnCompletion {
              if (!editSheetState.isVisible) {
                showEditEventSheet = false
              }
            }
      }
    }
  }
  LaunchedEffect(editSheetState.isVisible) {
    if (!editSheetState.isVisible && showEditEventSheet) {
      showEditEventSheet = false
      eventManagementViewModel.cancelEditEvent()
    }
  }

  Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        CalendarAppBar(
            onNavigateToSettings = onNavigateToSettings,
            onGoToTodayClick = {
              scope.launch {
                if (pagerState.currentPage != initialPageIndex) {
                  calendarViewModel.onVisibleDateChanged(today)
                  pagerState.animateScrollToPage(initialPageIndex)
                } else {
                  calendarViewModel.refreshCurrentVisibleDate()
                }
              }
            },
            onTitleClick = {
              datePickerState.selectableDates

              showDatePicker = true
            },
            date = currentVisibleDate)
      },
  ) { paddingValues ->
    Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      BackgroundShapes(BackgroundShapeContext.Main)

      VerticalPager(
          state = pagerState,
          modifier = Modifier.fillMaxSize(),
          key = { index -> today.plusDays((index - initialPageIndex).toLong()).toEpochDay() },
          flingBehavior = customFlingBehavior) { pageIndex ->
            val pageDate =
                remember(pageIndex) { today.plusDays((pageIndex - initialPageIndex).toLong()) }

            DayEventsPage(
                isLoading = isOverallLoading,
                date = pageDate,
                viewModel = calendarViewModel,
                eventManagementViewModel = eventManagementViewModel)
          }
      AiVisualizer(
          aiState = aiState,
          aiMessage = aiMessage,
          modifier = Modifier.fillMaxSize(),
          onResultShownTimeout = { calendarViewModel.resetAiStateAfterResult() },
          onAskingShownTimeout = { calendarViewModel.resetAiStateAfterAsking() })
      BottomBar(
          uiState = calendarState, // Передаем весь uiState, т.к. Bar зависит от многих полей
          textFieldValue = textFieldState,
          onTextChanged = { textFieldState = it },
          onSendClick = {
            calendarViewModel.sendTextMessage(textFieldState.text)
            textFieldState = TextFieldValue("") // Очищаем поле после отправки
          },
          onRecordStart = { calendarViewModel.startListening() }, // Передаем лямбды для записи
          onRecordStopAndSend = { calendarViewModel.stopListening() },
          onUpdatePermissionResult = { granted ->
            calendarViewModel.updatePermissionStatus(granted)
          }, // Передаем лямбду для обновления разрешений
          isTextInputVisible = isTextInputVisible,
          modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset),
          onCreateEventClick = {
            selectedDateForSheet = currentVisibleDate
            showCreateEventSheet = true
          })
    } // End основной Box
  } // End Scaffold

  if (showDatePicker) {
    DatePickerDialog(
        onDismissRequest = { showDatePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                showDatePicker = false
                val selectedMillis = datePickerState.selectedDateMillis
                if (selectedMillis != null) {
                  val selectedDate =
                      Instant.ofEpochMilli(selectedMillis)
                          .atZone(ZoneId.systemDefault()) // Используй корректную ZoneId
                          .toLocalDate()

                  // Проверяем, изменилась ли дата
                  if (selectedDate != currentVisibleDate) {
                    // 1. Сообщаем ViewModel о новой дате *до* скролла
                    Log.d("DatePicker", "Date selected: $selectedDate. Updating ViewModel.")
                    calendarViewModel.onVisibleDateChanged(selectedDate)

                    // 2. Рассчитываем целевую страницу
                    val daysDifference = ChronoUnit.DAYS.between(today, selectedDate)
                    val targetPageIndex =
                        (initialPageIndex + daysDifference)
                            // Ограничиваем индекс на всякий случай
                            .coerceIn(0L, Int.MAX_VALUE.toLong() - 1L)
                            .toInt()

                    // 3. Запускаем скролл к странице (используем scrollToPage для мгновенного
                    // перехода)
                    scope.launch {
                      Log.d("DatePicker", "Scrolling Pager to page index: $targetPageIndex")
                      pagerState.scrollToPage(targetPageIndex)
                    }
                  } else {
                    Log.d(
                        "DatePicker",
                        "Selected date $selectedDate is the same as current $currentVisibleDate. No action.")
                  }
                } else {
                  Log.w("DatePicker", "Confirm clicked but selectedDateMillis is null.")
                }
              },
              // Кнопка активна, только если дата выбрана
              enabled = datePickerState.selectedDateMillis != null) {
                Text("OK") // Используем Text из M3
              }
        },
        dismissButton = {
          TextButton(onClick = { showDatePicker = false }) {
            Text(stringResource(R.string.cancel)) // Используем Text из M3
          }
        }) {
          // Сам DatePicker
          DatePicker(state = datePickerState)
        }
  }
  if (showCreateEventSheet) {
    ModalBottomSheet(
        onDismissRequest = { showCreateEventSheet = false },
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.navigationBars }) {
          CreateEventScreen(
              viewModel = eventManagementViewModel,
              userTimeZoneId = timeZone.value,
              initialDate = selectedDateForSheet,
              onDismiss = {
                scope
                    .launch { sheetState.hide() }
                    .invokeOnCompletion {
                      if (!sheetState.isVisible) {
                        showCreateEventSheet = false
                      }
                    }
              },
              currentSheetValue = sheetState.currentValue)
        }
  }
  if (showEditEventSheet) {
    val eventToEdit = calendarState.eventBeingEdited
    val mode = calendarState.selectedUpdateMode

    if (eventToEdit != null && mode != null) {
      ModalBottomSheet(
          onDismissRequest = {
            scope
                .launch { editSheetState.hide() }
                .invokeOnCompletion {
                  if (!editSheetState.isVisible) {
                    showEditEventSheet = false
                    eventManagementViewModel.cancelEditEvent()
                  }
                }
          },
          sheetState = editSheetState,
          contentWindowInsets = { WindowInsets.navigationBars }) {
            EditEventScreen(
                viewModel = eventManagementViewModel,
                userTimeZone = timeZone.value,
                eventToEdit = eventToEdit,
                selectedUpdateMode = mode,
                onDismiss = {
                  scope
                      .launch { editSheetState.hide() }
                      .invokeOnCompletion {
                        if (!editSheetState.isVisible) {
                          showEditEventSheet = false
                          eventManagementViewModel.cancelEditEvent()
                        }
                      }
                },
                currentSheetValue = editSheetState.currentValue)
          }
    }
  }
  if (calendarState.showEventDetailedView && calendarState.eventForDetailedView != null) {
    CustomEventDetailsDialog(
        event = calendarState.eventForDetailedView!!, // Передаем событие
        onDismissRequest = { eventManagementViewModel.cancelEventDetails() },
        viewModel = calendarViewModel,
        userTimeZoneId = timeZone.value,
        eventManagementViewModel = eventManagementViewModel)
  }
  if (calendarState.showSignInRequiredDialog) {
    LogInScreenDialog(
        onDismissRequest = { calendarViewModel.onSignInRequiredDialogDismissed() },
        onSignInClick = {
          if (activity != null) {
            calendarViewModel.signIn(activity)
          } else {
            Log.e("MainScreen", "Activity is null, cannot start sign-in flow.")
          }
        })
  }

  LaunchedEffect(sheetState.isVisible) {
    if (!sheetState.isVisible && showCreateEventSheet) {
      showCreateEventSheet = false
    }
  }
}
