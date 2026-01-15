package com.lpavs.caliinda.feature.calendar.presentation

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.auth.AuthViewModel
import com.lpavs.caliinda.core.ui.util.BackgroundShapeContext
import com.lpavs.caliinda.core.ui.util.BackgroundShapes
import com.lpavs.caliinda.feature.agent.presentation.indicators.AiVisualizer
import com.lpavs.caliinda.feature.agent.presentation.vm.AgentViewModel
import com.lpavs.caliinda.feature.calendar.presentation.components.bars.BottomBar
import com.lpavs.caliinda.feature.calendar.presentation.components.bars.CalendarAppBar
import com.lpavs.caliinda.feature.calendar.presentation.components.dialogs.EventManagementDialogs
import com.lpavs.caliinda.feature.calendar.presentation.components.page.CalendarEffectHandler
import com.lpavs.caliinda.feature.calendar.presentation.components.page.DayEventsPage
import com.lpavs.caliinda.feature.calendar.presentation.components.page.ProjectEventsPage
import com.lpavs.caliinda.feature.event_management.ui.create.CreateEventScreen
import com.lpavs.caliinda.feature.event_management.ui.edit.EditEventScreen
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringEventEditOptionsDialog
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import com.lpavs.caliinda.feature.settings.vm.SettingsViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel,
    eventManagementViewModel: EventManagementViewModel,
    settignsViewModel: SettingsViewModel,
    agentViewModel: AgentViewModel,
    authViewModel: AuthViewModel,
    onNavigateToSettings: () -> Unit,
) {
  val calendarState by calendarViewModel.state.collectAsStateWithLifecycle()
  val agentState by agentViewModel.agentState.collectAsStateWithLifecycle()
  val recState by agentViewModel.recState.collectAsStateWithLifecycle()
  val eventManagementState by eventManagementViewModel.uiState.collectAsStateWithLifecycle()
  val authState by authViewModel.authState.collectAsStateWithLifecycle()
  val timeZone = calendarViewModel.timeZone.collectAsStateWithLifecycle()
  val themeMode by settignsViewModel.themeMode.collectAsStateWithLifecycle()
  val userTimeZoneId = remember { ZoneId.of(timeZone.value) }
  val agentResponse by agentViewModel.agentResponse.collectAsStateWithLifecycle()
  val suggestions = agentResponse?.suggestions ?: emptyList()

  var textFieldState by remember { mutableStateOf(TextFieldValue("")) }
  val isTextInputVisible by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }
  val haptic = LocalHapticFeedback.current

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val today = remember { LocalDate.now() }
  val initialPageIndex = remember { Int.MAX_VALUE / 2 }
  val pagerState = rememberPagerState(initialPage = initialPageIndex, pageCount = { Int.MAX_VALUE })
  val initialHorizontalPageIndex = remember { 1 }
  val horizontalPagerState =
      rememberPagerState(initialPage = initialHorizontalPageIndex, pageCount = { 2 })
  val initialWeekViewPageIndex = remember { 1 }
  val weekViewPagerState =
      rememberPagerState(initialPage = initialWeekViewPageIndex, pageCount = { 3 })
  val currentVisibleDate by calendarViewModel.currentVisibleDate.collectAsStateWithLifecycle()
  val activity = context as? Activity
  val authorizationLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
              result.data?.let { authViewModel.handleAuthorizationResult(it) }
            } else {
              Log.w("MainScreen", "Authorization flow was cancelled by user.")
              authViewModel.signOut()
            }
          }
  val isOverallLoading = calendarState.isLoading || eventManagementState.isLoading

    CalendarEffectHandler(
        calendarViewModel = calendarViewModel,
        eventManagementViewModel = eventManagementViewModel,
        agentViewModel = agentViewModel,
        authViewModel = authViewModel,
        pagerState = pagerState,
        snackbarHostState = snackbarHostState,
        initialPageIndex = initialPageIndex,
        today = today,
        authorizationLauncher = authorizationLauncher
    )

  var showDatePicker by remember { mutableStateOf(false) }
  val datePickerState =
      rememberDatePickerState(
          initialSelectedDateMillis =
              currentVisibleDate.atStartOfDay(userTimeZoneId).toInstant().toEpochMilli(),
      )

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
  var showCreateEventSheet by remember { mutableStateOf(false) }
  var selectedDateForSheet by remember { mutableStateOf<LocalDate>(today) }
  val CreateEventAction = {
    selectedDateForSheet = currentVisibleDate
    showCreateEventSheet = true
  }
  var showEditEventSheet by remember { mutableStateOf(false) }
  val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

  if (eventManagementState.showRecurringEditOptionsDialog &&
      eventManagementState.eventBeingEdited != null) {
    RecurringEventEditOptionsDialog(
        eventName = eventManagementState.eventBeingEdited!!.summary,
        onDismiss = { eventManagementViewModel.cancelEditEvent() },
        onOptionSelected = { choice ->
          eventManagementViewModel.onRecurringEditOptionSelected(choice)
        })
  }

  LaunchedEffect(eventManagementState.showEditEventDialog, eventManagementState.eventBeingEdited) {
    if (eventManagementState.showEditEventDialog && eventManagementState.eventBeingEdited != null) {
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
            onTitleHold = {
              haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
              calendarViewModel.refreshCurrentVisibleDate()
            },
            date = currentVisibleDate,
            isSignedIn = !calendarState.signInRequired)
      },
  ) { paddingValues ->
    Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      BackgroundShapes(context = BackgroundShapeContext.Main, themeMode = themeMode)
      HorizontalPager(
          state = horizontalPagerState,
          modifier = Modifier.fillMaxSize(),
          userScrollEnabled = !calendarState.signInRequired,
      ) { page ->
        when (page) {
          0 ->
              VerticalPager(
                  state = weekViewPagerState,
                  modifier = Modifier.fillMaxSize(),
                  flingBehavior =
                      PagerDefaults.flingBehavior(
                          state = weekViewPagerState, snapPositionalThreshold = 0.05f),
                  userScrollEnabled = false,
                  beyondViewportPageCount = 1) { pageIndex ->
                    ProjectEventsPage(
                        isLoading = isOverallLoading,
                        viewModel = calendarViewModel,
                        eventManagementViewModel = eventManagementViewModel,
                        createEventClick = CreateEventAction)
                  }
          1 ->
              VerticalPager(
                  state = pagerState,
                  modifier = Modifier.fillMaxSize(),
                  key = { index ->
                    today.plusDays((index - initialPageIndex).toLong()).toEpochDay()
                  },
                  flingBehavior =
                      PagerDefaults.flingBehavior(
                          state = pagerState, snapPositionalThreshold = 0.05f),
                  userScrollEnabled = !calendarState.signInRequired,
                  beyondViewportPageCount = 1) { pageIndex ->
                    val pageDate =
                        remember(pageIndex) {
                          today.plusDays((pageIndex - initialPageIndex).toLong())
                        }

                    DayEventsPage(
                        isLoading = isOverallLoading,
                        isSignIn = calendarState.signInRequired,
                        date = pageDate,
                        viewModel = calendarViewModel,
                        eventManagementViewModel = eventManagementViewModel,
                        onSignInClick = {
                          if (activity != null) {
                            authViewModel.signIn(activity)
                          } else {
                            Log.e("MainScreen", "Activity is null, cannot start sign-in flow.")
                          }
                        },
                        agentViewModel = agentViewModel,
                        createEventClick = CreateEventAction)
                  }
        }
      }
      AiVisualizer(aiState = agentState, modifier = Modifier.fillMaxSize())
      if (!calendarState.signInRequired) {
        BottomBar(
            calendarState = calendarState,
            textFieldValue = textFieldState,
            onTextChanged = { textFieldState = it },
            onSendClick = { messageText ->
              agentViewModel.sendTextMessage(messageText)
              textFieldState = TextFieldValue("")
            },
            onRecordStart = { agentViewModel.startListening() },
            onRecordStopAndSend = { agentViewModel.stopListening() },
            onUpdatePermissionResult = { granted ->
              agentViewModel.updatePermissionStatus(granted)
            },
            isTextInputVisible = isTextInputVisible,
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset),
            onCreateEventClick = CreateEventAction,
            recordState = recState,
            authState = authState,
            suggestions = suggestions)
      }
    }
  }

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
                      Instant.ofEpochMilli(selectedMillis).atZone(userTimeZoneId).toLocalDate()

                  if (selectedDate != currentVisibleDate) {
                    Log.d("DatePicker", "Date selected: $selectedDate. Updating ViewModel.")
                    calendarViewModel.onVisibleDateChanged(selectedDate)

                    val daysDifference = ChronoUnit.DAYS.between(today, selectedDate)
                    val targetPageIndex =
                        (initialPageIndex + daysDifference)
                            .coerceIn(0L, Int.MAX_VALUE.toLong() - 1L)
                            .toInt()

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
              enabled = datePickerState.selectedDateMillis != null) {
                Text("OK")
              }
        },
        dismissButton = {
          TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
        }) {
          DatePicker(state = datePickerState)
        }
  }
  if (showCreateEventSheet) {
    ModalBottomSheet(
        onDismissRequest = { showCreateEventSheet = false },
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.navigationBars }) {
          CreateEventScreen(
              userTimeZone = timeZone.value,
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
    val eventToEdit = eventManagementState.eventBeingEdited
    val mode = eventManagementState.selectedUpdateMode

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
  EventManagementDialogs(state = eventManagementState, viewModel = eventManagementViewModel)

  LaunchedEffect(sheetState.isVisible) {
    if (!sheetState.isVisible && showCreateEventSheet) {
      showCreateEventSheet = false
    }
  }
}
