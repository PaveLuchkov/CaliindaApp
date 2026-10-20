package com.lpavs.caliinda.feature.calendar.presentation

import com.lpavs.caliinda.feature.event_management.ui.shared.sections.repeatDate
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
import com.lpavs.caliinda.core.ui.components.AppDatePickerDialog
import com.lpavs.caliinda.feature.calendar.presentation.components.dialogs.CustomEventDetailsDialog
import com.lpavs.caliinda.feature.calendar.presentation.components.dialogs.EventFormSheet
import com.lpavs.caliinda.core.data.calendar.model.EventUpdateMode
import com.lpavs.caliinda.feature.calendar.presentation.components.dialogs.EventManagementDialogs
import com.lpavs.caliinda.feature.calendar.presentation.components.page.CalendarEffectHandler
import com.lpavs.caliinda.feature.calendar.presentation.components.page.CalendarPagerScreen
import com.lpavs.caliinda.feature.event_management.ui.create.CreateEventScreen
import com.lpavs.caliinda.app.LaunchRequests
import com.lpavs.caliinda.feature.event_management.ui.edit.EditEventScreen
import com.lpavs.caliinda.feature.event_management.vm.EventDialog
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import com.lpavs.caliinda.feature.event_management.EventActions
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel,
    eventManagementViewModel: EventManagementViewModel,
    onNavigateToSettings: () -> Unit,
) {
  val calendarState by calendarViewModel.state.collectAsStateWithLifecycle()
  val introductionState by calendarViewModel.introState.collectAsStateWithLifecycle()
  val eventManagementState by eventManagementViewModel.uiState.collectAsStateWithLifecycle()
  val timeZone by calendarViewModel.timeZone.collectAsStateWithLifecycle()
  val themeMode by calendarViewModel.themeMode.collectAsStateWithLifecycle()
  val reminderDefaults by eventManagementViewModel.reminderDefaults.collectAsStateWithLifecycle()
  val writableCalendars by eventManagementViewModel.writableCalendars.collectAsStateWithLifecycle()
  val shownCalendars by calendarViewModel.shownCalendars.collectAsStateWithLifecycle()
  val defaultCalendarId by eventManagementViewModel.defaultCalendarId.collectAsStateWithLifecycle()

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

  val eventActions =
      remember(eventManagementViewModel, calendarViewModel) {
        EventActions(
            onDelete = eventManagementViewModel::requestDelete,
            onEdit = eventManagementViewModel::requestEditEvent,
            onDetails = calendarViewModel::requestEventDetails)
      }
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
  // Нажали «+» на рабочем столе: форма нового события на сегодня. Без доступа к календарю
  // (или пока он ещё не проверен на холодном старте) просьба ждёт — откроем, как только дадут.
  val createRequested by LaunchRequests.createEvent.collectAsStateWithLifecycle()
  LaunchedEffect(createRequested, calendarState.hasCalendarPermission) {
    if (createRequested && calendarState.hasCalendarPermission) {
      LaunchRequests.consumeCreateEvent()
      eventManagementViewModel.openCreate(today, false)
    }
  }
  // Перейти на день: из выбора даты и из подробностей события (тап по дате начала/конца).
  val goToDate: (LocalDate) -> Unit = { date ->
    if (date != currentVisibleDate) {
      calendarViewModel.onVisibleDateChanged(date)
      scope.launch { pagerState.scrollToPage(pageForDate(date)) }
    }
    // С экрана проектов — переходим к самому дню.
    if (horizontalPagerState.currentPage == 0) {
      scope.launch { horizontalPagerState.animateScrollToPage(1) }
    }
  }

  // Тап по событию на виджете: его день и подробности — тоже после доступа к календарю.
  val openRequest by LaunchRequests.openEvent.collectAsStateWithLifecycle()
  LaunchedEffect(openRequest, calendarState.hasCalendarPermission) {
    val request = openRequest
    if (request != null && calendarState.hasCalendarPermission) {
      LaunchRequests.consumeOpenEvent()
      goToDate(request.date)
      calendarViewModel.openEventFromWidget(request.date, request.eventId, request.instanceBegin)
    }
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
            onTitleClick = { showDatePicker = true },
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
          dayPageState = calendarViewModel::getDayPageUiState,
          monthPageState = calendarViewModel::getMonthPageUiState,
          actions = eventActions,
          onIntroNext = calendarViewModel::onIntroNext,
          onIntroFinish = calendarViewModel::finishIntro,
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

  if (showDatePicker) {
    AppDatePickerDialog(
      initialDate = currentVisibleDate,
      onDismiss = { showDatePicker = false },
      onDateSelected = goToDate)
  }
  EventFormSheet(
      dialog = eventManagementState.dialog, onDismiss = eventManagementViewModel::dismissDialog) {
          dialog,
          sheetSettled ->
        when (dialog) {
          is EventDialog.Creating ->
              CreateEventScreen(
                  userTimeZone = timeZone,
                  isLoading = eventManagementState.isLoading,
                  onSave = eventManagementViewModel::createEvent,
                  reminderDefaults = reminderDefaults,
                  calendars = writableCalendars,
                  defaultCalendarId = defaultCalendarId,
                  initialDate = dialog.date,
                  initialProject = dialog.asProject,
                  template = dialog.template,
                  templateReminders = eventManagementState.editingReminders,
                  sheetSettled = sheetSettled)
          is EventDialog.Editing ->
              EditEventScreen(
                  eventToEdit = dialog.event,
                  userTimeZone = timeZone,
                  isLoading = eventManagementState.isLoading,
                  onSave = { summary, description, location, dateTime, reminder ->
                    eventManagementViewModel.updateEvent(
                        summary, description, location, dateTime, dialog.mode, reminder)
                  },
                  reminderDefaults = reminderDefaults,
                  initialReminders = eventManagementState.editingReminders,
                  showRecurrence = dialog.mode != EventUpdateMode.SINGLE_INSTANCE)
          else -> {}
        }
      }

  calendarState.eventDetails?.let { details ->
    CustomEventDetailsDialog(
        event = details,
        // Календарь называем, только когда их несколько: при одном это лишняя строка.
        calendar =
            shownCalendars
                .takeIf { it.size > 1 }
                ?.firstOrNull { it.id == details.originalEvent.calendarId },
        onDismissRequest = { calendarViewModel.cancelEventDetails() },
        userTimeZone = timeZone,
        onEdit = eventManagementViewModel::requestEditEvent,
        onDelete = eventManagementViewModel::requestDelete,
        onRepeat = { event ->
          calendarViewModel.cancelEventDetails()
          eventManagementViewModel.openRepeat(event, event.repeatDate(today, timeZone))
        },
        onOpenDate = { date ->
          calendarViewModel.cancelEventDetails()
          goToDate(date)
        })
  }
  EventManagementDialogs(
      dialog = eventManagementState.dialog,
      onEditModeSelected = eventManagementViewModel::onRecurringEditOptionSelected,
      onDeleteModeSelected = eventManagementViewModel::confirmRecurringDelete,
      onDismiss = eventManagementViewModel::dismissDialog)
}
