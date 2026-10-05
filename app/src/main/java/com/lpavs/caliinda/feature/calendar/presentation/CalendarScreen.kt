package com.lpavs.caliinda.feature.calendar.presentation

import java.time.YearMonth
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.lpavs.caliinda.core.ui.util.fromPickerMillis
import com.lpavs.caliinda.core.ui.util.toPickerMillis
import com.lpavs.caliinda.feature.calendar.presentation.components.bars.BottomBar
import com.lpavs.caliinda.feature.calendar.presentation.components.bars.CalendarAppBar
import com.lpavs.caliinda.feature.calendar.presentation.components.dialogs.CalendarDatePickerDialog
import com.lpavs.caliinda.feature.calendar.presentation.components.dialogs.CustomEventDetailsDialog
import com.lpavs.caliinda.feature.calendar.presentation.components.dialogs.EventFormSheet
import com.lpavs.caliinda.feature.calendar.presentation.components.dialogs.EventManagementDialogs
import com.lpavs.caliinda.feature.calendar.presentation.components.page.CalendarEffectHandler
import com.lpavs.caliinda.feature.calendar.presentation.components.page.CalendarPagerScreen
import com.lpavs.caliinda.feature.event_management.ui.create.CreateEventScreen
import com.lpavs.caliinda.feature.event_management.ui.edit.EditEventScreen
import com.lpavs.caliinda.feature.event_management.vm.EventDialog
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import com.lpavs.caliinda.feature.settings.vm.SettingsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
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
  val timeZone by calendarViewModel.timeZone.collectAsStateWithLifecycle()
  val themeMode by settignsViewModel.themeMode.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  val haptic = LocalHapticFeedback.current

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val today by calendarViewModel.today.collectAsStateWithLifecycle()
  // Дата, от которой отсчитываются страницы пейджера дней. Не двигается в полночь, иначе
  // открытая страница молча сменила бы дату; переживает и пересоздание процесса вместе с пейджером.
  val anchorDate = rememberSaveable { today }
  val initialPageIndex = remember { Int.MAX_VALUE / 2 }
  val pagerState = rememberPagerState(initialPage = initialPageIndex, pageCount = { Int.MAX_VALUE })
  val initialHorizontalPageIndex = remember { 1 }
  val horizontalPagerState =
      rememberPagerState(initialPage = initialHorizontalPageIndex, pageCount = { 2 })
  val anchorMonth = rememberSaveable { YearMonth.from(today) }
  val monthPagerState =
      rememberPagerState(initialPage = initialPageIndex, pageCount = { Int.MAX_VALUE })
  val pageForMonth: (YearMonth) -> Int = { month ->
    (initialPageIndex.toLong() + ChronoUnit.MONTHS.between(anchorMonth, month))
        .coerceIn(0L, Int.MAX_VALUE.toLong() - 1)
        .toInt()
  }
  val visibleMonth =
      anchorMonth.plusMonths((monthPagerState.currentPage - initialPageIndex).toLong())
  val pageForDate: (LocalDate) -> Int = { date ->
    (initialPageIndex.toLong() + ChronoUnit.DAYS.between(anchorDate, date))
        .coerceIn(0L, Int.MAX_VALUE.toLong() - 1)
        .toInt()
  }
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
  CalendarEffectHandler(
      calendarViewModel = calendarViewModel,
      eventManagementViewModel = eventManagementViewModel,
      pagerState = pagerState,
      snackbarHostState = snackbarHostState,
      initialPageIndex = initialPageIndex,
      anchorDate = anchorDate)

  var showDatePicker by remember { mutableStateOf(false) }
  val datePickerState =
      rememberDatePickerState(
          initialSelectedDateMillis =
              currentVisibleDate.toPickerMillis(),
      )

  val createEventAction = {
    // С экрана проектов (страница 0) сразу предлагаем промежуток дней, начиная с сегодня.
    val asProject = horizontalPagerState.currentPage == 0
    val date =
        when {
          !asProject -> currentVisibleDate
          visibleMonth == YearMonth.from(today) -> today
          else -> visibleMonth.atDay(1)
        }
    eventManagementViewModel.openCreate(date, asProject)
  }
  Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        CalendarAppBar(
            onNavigateToSettings = onNavigateToSettings,
            onGoToTodayClick = {
              scope.launch {
                if (horizontalPagerState.currentPage == 0) {
                  monthPagerState.animateScrollToPage(pageForMonth(YearMonth.from(today)))
                  return@launch
                }
                val todayPage = pageForDate(today)
                if (pagerState.currentPage != todayPage) {
                  calendarViewModel.onVisibleDateChanged(today)
                  pagerState.animateScrollToPage(todayPage)
                }
              }
            },
            onTitleClick = {
              // Состояние пикера живёт дольше диалога — открываем его на текущей видимой дате.
              val visibleMillis = currentVisibleDate.toPickerMillis()
              datePickerState.selectedDateMillis = visibleMillis
              datePickerState.displayedMonthMillis = visibleMillis
              showDatePicker = true
            },
            onTitleHold = {
              haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
              calendarViewModel.syncCalendars()
            },
            date = currentVisibleDate,
            today = today,
            month = if (horizontalPagerState.currentPage == 0) visibleMonth else null,
            hasCalendarAccess = calendarState.hasCalendarPermission)
      },
  ) { paddingValues ->
    Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      BackgroundShapes(context = BackgroundShapeContext.Main, themeMode = themeMode)
      CalendarPagerScreen(
          calendarViewModel = calendarViewModel,
          eventManagementViewModel = eventManagementViewModel,
          calendarPagerState = horizontalPagerState,
          dailyViewPagerState = pagerState,
          monthPagerState = monthPagerState,
          anchorMonth = anchorMonth,
          hasCalendarAccess = calendarState.hasCalendarPermission,
          onGrantAccessClick = requestCalendarAccess,
          createEventAction = createEventAction,
          initialPageIndex = initialPageIndex,
          anchorDate = anchorDate,
          introductionState = introductionState)

      if (calendarState.hasCalendarPermission) {
        BottomBar(
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset),
            onCreateEventClick = createEventAction)
      }
    }
  }

  CalendarDatePickerDialog(
      show = showDatePicker,
      state = datePickerState,
      onDismiss = { showDatePicker = false },
      onConfirm = { millis ->
        showDatePicker = false

        val selectedDate = millis.fromPickerMillis()

        if (selectedDate != currentVisibleDate) {
          calendarViewModel.onVisibleDateChanged(selectedDate)

          scope.launch { pagerState.scrollToPage(pageForDate(selectedDate)) }
        }
        // Выбор даты с экрана проектов — переходим к этому дню.
        if (horizontalPagerState.currentPage == 0) {
          scope.launch { horizontalPagerState.animateScrollToPage(1) }
        }
      })
  EventFormSheet(
      dialog = eventManagementState.dialog, onDismiss = eventManagementViewModel::dismissDialog) {
          dialog,
          sheetSettled ->
        when (dialog) {
          is EventDialog.Creating ->
              CreateEventScreen(
                  viewModel = eventManagementViewModel,
                  userTimeZone = timeZone,
                  initialDate = dialog.date,
                  initialProject = dialog.asProject,
                  sheetSettled = sheetSettled)
          is EventDialog.Editing ->
              EditEventScreen(
                  viewModel = eventManagementViewModel,
                  eventToEdit = dialog.event,
                  selectedUpdateMode = dialog.mode)
          else -> {}
        }
      }

  calendarState.eventDetails?.let { details ->
    CustomEventDetailsDialog(
        event = details,
        onDismissRequest = { calendarViewModel.cancelEventDetails() },
        userTimeZone = timeZone,
        eventManagementViewModel = eventManagementViewModel,
        themeMode = themeMode)
  }
  EventManagementDialogs(
      dialog = eventManagementState.dialog,
      onEditModeSelected = eventManagementViewModel::onRecurringEditOptionSelected,
      onDeleteModeSelected = eventManagementViewModel::confirmRecurringDelete,
      onDismiss = eventManagementViewModel::dismissDialog)
}
