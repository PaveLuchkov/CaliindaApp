package com.lpavs.caliinda.feature.calendar.presentation

import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import java.util.Locale
import java.time.format.DateTimeFormatter
import com.lpavs.caliinda.feature.calendar.presentation.components.page.rememberTopNoticeState
import com.lpavs.caliinda.feature.calendar.presentation.components.page.NoticeAutoDismiss
import com.lpavs.caliinda.feature.calendar.presentation.components.page.Notice
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
import java.time.Instant
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

  val noticeState = rememberTopNoticeState()
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
      noticeState = noticeState,
      initialPageIndex = initialPageIndex,
      anchorDate = anchorDate)

  var showDatePicker by remember { mutableStateOf(false) }

  val deletions by calendarViewModel.deletions.collectAsStateWithLifecycle()
  val celebration by eventManagementViewModel.celebration.collectAsStateWithLifecycle()
  val eventActions =
      remember(eventManagementViewModel, calendarViewModel, deletions, celebration) {
        EventActions(
            onDelete = eventManagementViewModel::requestDelete,
            onEdit = eventManagementViewModel::requestEditEvent,
            onDetails = calendarViewModel::requestEventDetails,
            deletions = deletions,
            onUndoDelete = eventManagementViewModel::undoDelete,
            celebration = celebration)
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

  // Сохранили на другой день — подсвечивать на экране нечего: плашка «Создано на чт, 22 окт →»,
  // тап — переход на день и «рождение» уже там.
  val dayFormat = remember(context) { DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()) }
  LaunchedEffect(celebration?.startedAt) {
    val saved = celebration ?: return@LaunchedEffect
    if (horizontalPagerState.currentPage == 1 && saved.date != currentVisibleDate) {
      val day = saved.date.format(dayFormat)
      noticeState.show(
          Notice(
              context.getString(
                  if (saved.created) R.string.notice_created_on else R.string.notice_moved_to, day),
              onClick = {
                goToDate(saved.date)
                eventManagementViewModel.celebrateAgain(saved)
              }))
    }
  }
  // Тап по событию на виджете или ссылка на событие из другого приложения: его день и подробности
  // (или сразу правка) — тоже после доступа к календарю.
  val openRequest by LaunchRequests.openEvent.collectAsStateWithLifecycle()
  LaunchedEffect(openRequest, calendarState.hasCalendarPermission) {
    val request = openRequest
    if (request != null && calendarState.hasCalendarPermission) {
      LaunchRequests.consumeOpenEvent()
      val date =
          request.date
              ?: calendarViewModel.dayOfEvent(request.eventId, request.instanceBegin)
              ?: return@LaunchedEffect
      goToDate(date)
      val event =
          calendarViewModel.findEvent(date, request.eventId, request.instanceBegin)
              ?: return@LaunchedEffect
      if (request.edit) eventManagementViewModel.requestEditEvent(event)
      else calendarViewModel.requestEventDetails(event)
    }
  }
  // «Добавить в календарь» из другого приложения или .ics-файл: форма с его полями.
  val insertRequest by LaunchRequests.insertEvent.collectAsStateWithLifecycle()
  LaunchedEffect(insertRequest, calendarState.hasCalendarPermission) {
    val request = insertRequest
    if (request != null && calendarState.hasCalendarPermission) {
      LaunchRequests.consumeInsertEvent()
      eventManagementViewModel.openInsert(request)
    }
  }
  // «Открыть календарь на этом времени»: просто переходим на день.
  val openTime by LaunchRequests.openTime.collectAsStateWithLifecycle()
  LaunchedEffect(openTime) {
    val millis = openTime ?: return@LaunchedEffect
    LaunchRequests.consumeOpenTime()
    goToDate(Instant.ofEpochMilli(millis).atZone(timeZone).toLocalDate())
  }
  Scaffold(
      topBar = {
        NoticeAutoDismiss(noticeState)
        CalendarAppBar(
            notice = noticeState.current,
            onNoticeClick = {
              noticeState.current?.let { notice ->
                notice.onClick?.invoke()
                noticeState.dismiss(notice)
              }
            },
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
          dialog ->
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
                  templateReminders = eventManagementState.editingReminders)
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
        // Подробности закрываем: после сохранения в ленте «рождается» карточка — за ними её
        // не было бы видно, да и показывали бы они уже старое.
        onEdit = { event ->
          calendarViewModel.cancelEventDetails()
          eventManagementViewModel.requestEditEvent(event)
        },
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
