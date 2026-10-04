package com.lpavs.caliinda.feature.calendar.presentation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.core.data.calendar.CalendarPermissionManager
import com.lpavs.caliinda.core.ui.util.BackgroundShapeContext
import com.lpavs.caliinda.core.ui.util.BackgroundShapes
import com.lpavs.caliinda.feature.calendar.presentation.components.bars.BottomBar
import com.lpavs.caliinda.feature.calendar.presentation.components.bars.CalendarAppBar
import com.lpavs.caliinda.feature.calendar.presentation.components.dialogs.CalendarDatePickerDialog
import com.lpavs.caliinda.feature.calendar.presentation.components.dialogs.CreateBottomSheet
import com.lpavs.caliinda.feature.calendar.presentation.components.dialogs.CustomEventDetailsDialog
import com.lpavs.caliinda.feature.calendar.presentation.components.dialogs.EditBottomSheet
import com.lpavs.caliinda.feature.calendar.presentation.components.dialogs.EventManagementDialogs
import com.lpavs.caliinda.feature.calendar.presentation.components.page.CalendarEffectHandler
import com.lpavs.caliinda.feature.calendar.presentation.components.page.CalendarPagerScreen
import com.lpavs.caliinda.feature.calendar.presentation.components.page.ManagementScreen
import com.lpavs.caliinda.feature.event_management.ui.create.CreateEventScreen
import com.lpavs.caliinda.feature.event_management.ui.edit.EditEventScreen
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
    onNavigateToSettings: () -> Unit,
) {
  val calendarState by calendarViewModel.state.collectAsStateWithLifecycle()
  val introductionState by calendarViewModel.introState.collectAsStateWithLifecycle()
  val eventManagementState by eventManagementViewModel.uiState.collectAsStateWithLifecycle()
  val timeZone = settignsViewModel.timeZone.collectAsStateWithLifecycle()
  val themeMode by settignsViewModel.themeMode.collectAsStateWithLifecycle()
  val userTimeZoneId = ZoneId.of(timeZone.value)

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
  var openSettingsForAccess by remember { mutableStateOf(false) }
  val permissionLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
          results ->
        calendarViewModel.onCalendarPermissionChanged()
        val activity = context as? Activity
        // Отказали "навсегда" — системный диалог больше не появится, остаются только настройки.
        openSettingsForAccess =
            activity != null &&
                results.values.any { granted -> !granted } &&
                CalendarPermissionManager.PERMISSIONS.none {
                  activity.shouldShowRequestPermissionRationale(it)
                }
      }
  val requestCalendarAccess: () -> Unit = {
    if (openSettingsForAccess) {
      context.startActivity(
          Intent(
              Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
              Uri.fromParts("package", context.packageName, null)))
    } else {
      permissionLauncher.launch(CalendarPermissionManager.PERMISSIONS)
    }
  }
  var autoRequestedAccess by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    if (!calendarState.hasCalendarPermission && !autoRequestedAccess) {
      autoRequestedAccess = true
      requestCalendarAccess()
    }
  }
  val eventToEdit = eventManagementState.eventBeingEdited
  val mode = eventManagementState.selectedUpdateMode
  val currentCalendarScreenMode = calendarState.currentMode
  CalendarEffectHandler(
      calendarViewModel = calendarViewModel,
      eventManagementViewModel = eventManagementViewModel,
      pagerState = pagerState,
      snackbarHostState = snackbarHostState,
      initialPageIndex = initialPageIndex,
      today = today)

  var showDatePicker by remember { mutableStateOf(false) }
  val datePickerState =
      rememberDatePickerState(
          initialSelectedDateMillis =
              currentVisibleDate.atStartOfDay(userTimeZoneId).toInstant().toEpochMilli(),
      )

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
  var showCreateEventSheet by remember { mutableStateOf(false) }
  var selectedDateForSheet by remember { mutableStateOf<LocalDate>(today) }
  var createAsProject by remember { mutableStateOf(false) }
  val CreateEventAction = {
    // С экрана проектов (страница 0) сразу предлагаем промежуток дней, начиная с сегодня.
    createAsProject = horizontalPagerState.currentPage == 0
    selectedDateForSheet = if (createAsProject) today else currentVisibleDate
    showCreateEventSheet = true
  }
  var showEditEventSheet by remember { mutableStateOf(false) }
  val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

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
                }
              }
            },
            onTitleClick = {
              datePickerState.selectableDates

              showDatePicker = true
            },
            onTitleHold = {
              haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
              calendarViewModel.syncCalendars()
            },
            date = currentVisibleDate,
            hasCalendarAccess = calendarState.hasCalendarPermission,
            currentCalendarScreenMode = currentCalendarScreenMode)
      },
  ) { paddingValues ->
    Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      BackgroundShapes(context = BackgroundShapeContext.Main, themeMode = themeMode)
      AnimatedContent(
          targetState = currentCalendarScreenMode,
          label = "ChangeInScenery",
          transitionSpec = { fadeIn() togetherWith fadeOut() }) { mode ->
            when (mode) {
              AppMode.CALENDAR -> {
                CalendarPagerScreen(
                    calendarViewModel = calendarViewModel,
                    eventManagementViewModel = eventManagementViewModel,
                    calendarPagerState = horizontalPagerState,
                    dailyViewPagerState = pagerState,
                    weekViewPagerState = weekViewPagerState,
                    hasCalendarAccess = calendarState.hasCalendarPermission,
                    onGrantAccessClick = requestCalendarAccess,
                    createEventAction = CreateEventAction,
                    initialPageIndex = initialPageIndex,
                    today = today,
                    introductionState = introductionState)
              }
              AppMode.MANAGEMENT -> {
                ManagementScreen()
              }
            }
          }

      if (calendarState.hasCalendarPermission) {
        BottomBar(
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset),
            onCreateEventClick = CreateEventAction)
      }
    }
  }

  CalendarDatePickerDialog(
      show = showDatePicker,
      state = datePickerState,
      onDismiss = { showDatePicker = false },
      onConfirm = { millis ->
        showDatePicker = false

        val selectedDate = Instant.ofEpochMilli(millis).atZone(userTimeZoneId).toLocalDate()

        if (selectedDate != currentVisibleDate) {
          calendarViewModel.onVisibleDateChanged(selectedDate)

          val daysDiff = ChronoUnit.DAYS.between(today, selectedDate)
          val targetPage =
              (initialPageIndex.toLong() + daysDiff)
                  .coerceIn(0L, Int.MAX_VALUE.toLong() - 1)
                  .toInt()

          scope.launch { pagerState.scrollToPage(targetPage.toInt()) }
        }
      })
  CreateBottomSheet(
      show = showCreateEventSheet,
      sheetState = sheetState,
      onDismiss = { showCreateEventSheet = false },
  ) {
    CreateEventScreen(
        userTimeZone = timeZone.value,
        initialDate = selectedDateForSheet,
        initialProject = createAsProject,
        onDismiss = {
          scope
              .launch { sheetState.hide() }
              .invokeOnCompletion {
                if (!sheetState.isVisible) {
                  showCreateEventSheet = false
                }
              }
        },
    )
  }

  EditBottomSheet(
      show = showEditEventSheet,
      sheetState = editSheetState,
      eventToEdit = eventToEdit,
      mode = mode,
      onDismiss = {
        showEditEventSheet = false
        eventManagementViewModel.cancelEditEvent()
      }) {
        EditEventScreen(
            viewModel = eventManagementViewModel,
            userTimeZone = timeZone.value,
            eventToEdit = eventToEdit!!,
            selectedUpdateMode = mode!!,
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

  if (calendarState.showEventDetailedView && calendarState.eventForDetailedView != null) {
    CustomEventDetailsDialog(
        event = calendarState.eventForDetailedView!!,
        onDismissRequest = { calendarViewModel.cancelEventDetails() },
        userTimeZone = timeZone.value,
        eventManagementViewModel = eventManagementViewModel,
        themeMode = themeMode)
  }
  EventManagementDialogs(state = eventManagementState, viewModel = eventManagementViewModel)

  LaunchedEffect(sheetState.isVisible) {
    if (!sheetState.isVisible && showCreateEventSheet) {
      showCreateEventSheet = false
    }
  }
}
