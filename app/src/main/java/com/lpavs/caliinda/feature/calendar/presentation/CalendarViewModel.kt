package com.lpavs.caliinda.feature.calendar.presentation

import com.lpavs.caliinda.core.data.repository.PendingDeletions
import com.lpavs.caliinda.core.data.utils.UiText
import com.lpavs.caliinda.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.core.data.calendar.CalendarPermissionManager
import com.lpavs.caliinda.core.data.di.ICalendarStateHolder
import com.lpavs.caliinda.core.data.di.ITimeTicker
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.feature.calendar.presentation.model.EventUiDetailsModelMapper
import com.lpavs.caliinda.feature.calendar.presentation.model.EventUiModelMapper
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroState
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroStep
import com.lpavs.caliinda.feature.calendar.presentation.components.page.DayPageUiState
import com.lpavs.caliinda.feature.calendar.presentation.components.page.MonthPageUiState
import com.lpavs.caliinda.feature.calendar.presentation.components.page.ProjectRibbon
import com.lpavs.caliinda.feature.calendar.presentation.model.dateRange
import java.time.YearMonth
import com.lpavs.caliinda.core.data.model.ThemeMode
import java.time.temporal.ChronoUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel
@Inject
constructor(
    private val permissionManager: CalendarPermissionManager,
    private val calendarRepository: CalendarRepository,
    timeTicker: ITimeTicker,
    private val calendarStateHolder: ICalendarStateHolder,
    private val settingsRepository: SettingsRepository,
    private val eventUiModelMapper: EventUiModelMapper,
    private val eventUiDetailsModelMapper: EventUiDetailsModelMapper,
    private val pendingDeletions: PendingDeletions,
) : ViewModel() {

  // --- ОСНОВНОЕ СОСТОЯНИЕ UI ---
  private val _uiState =
      MutableStateFlow(CalendarState(hasCalendarPermission = permissionManager.isGranted.value))
  val state: StateFlow<CalendarState> = _uiState.asStateFlow()

  private val _introState = MutableStateFlow(IntroState())
  val introState: StateFlow<IntroState> = _introState.asStateFlow()

  // --- ДЕЛЕГИРОВАННЫЕ И ПРОИЗВОДНЫЕ СОСТОЯНИЯ ДЛЯ UI ---
  val currentTime: StateFlow<Instant> = timeTicker.currentTime

  // Eagerly: .value читается вне подписок (детали события), там нужен пояс из настроек.
  val timeZone: StateFlow<ZoneId> =
      settingsRepository.zoneFlow.stateIn(
          viewModelScope, SharingStarted.Eagerly, ZoneId.systemDefault())

  /** Сегодняшняя дата в поясе из настроек; сменяется в полночь (с точностью до тика таймера). */
  val today: StateFlow<LocalDate> =
      combine(currentTime, timeZone) { now, zone -> now.atZone(zone).toLocalDate() }
          .distinctUntilChanged()
          .stateIn(viewModelScope, SharingStarted.Eagerly, LocalDate.now(timeZone.value))

  val themeMode: StateFlow<ThemeMode> =
      settingsRepository.themeModeFlow.stateIn(
          viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

  // Состояния Календаря
  val currentVisibleDate: StateFlow<LocalDate> = calendarStateHolder.currentVisibleDate

  private val _events = Channel<CalendarUiEvent>(Channel.BUFFERED)
  val events: Flow<CalendarUiEvent> = _events.receiveAsFlow()

  init {
    observeCalendarPermission()
    observeIntroduction()
  }

    private fun observeIntroduction() {
        viewModelScope.launch {
            val finished = settingsRepository.isIntroFinished()
            if (finished) {
                _introState.value = IntroState(isFinished = true)
            }
        }
    }

    fun onIntroNext() {
        val next = IntroStep.next(_introState.value.currentStep)

        if (next == null) {
            _introState.value = _introState.value.copy(isFinished = true)
            viewModelScope.launch {
                settingsRepository.saveIntroComplete()
            }
        } else {
            _introState.value = _introState.value.copy(currentStep = next)
        }
    }

  private fun observeCalendarPermission() {
    viewModelScope.launch {
      permissionManager.isGranted.collect { granted ->
        _uiState.update { it.copy(hasCalendarPermission = granted) }
      }
    }
  }

  /** Вызывается после системного диалога разрешений и при возврате в приложение. */
  fun onCalendarPermissionChanged() {
    permissionManager.refresh()
  }

  fun getDayPageUiState(date: LocalDate): Flow<DayPageUiState> {
    return combine(
            calendarRepository.getEventsFlowForDate(date).withoutPendingDeletions(),
            calendarRepository.getProjectsFlowForDate(date).withoutPendingDeletions(),
            currentTime,
            timeZone) { events, projects, now, zoneId ->
          val ribbons =
              projects.map { project ->
                val range = project.dateRange(zoneId)
                ProjectRibbon(
                    event = project,
                    dayNumber = ChronoUnit.DAYS.between(range.start, date).toInt() + 1,
                    totalDays = ChronoUnit.DAYS.between(range.start, range.endInclusive).toInt() + 1)
              }
          val isToday = date == now.atZone(zoneId).toLocalDate()

          val (allDayDtos, timedDtos) = events.partition { it.isAllDay }

          val sortedTimedDtos = timedDtos.sortedBy { it.startTime }

          // Скроллим к текущему событию, а если его нет — к ближайшему следующему.
          val scrollIndex =
              if (!isToday) {
                -1
              } else {
                sortedTimedDtos
                    .indexOfFirst { !now.isBefore(it.startTime) && now.isBefore(it.endTime) }
                    .takeIf { it != -1 } ?: sortedTimedDtos.indexOfFirst { it.startTime.isAfter(now) }
              }

          val timedUiModels =
              eventUiModelMapper.mapToUiModels(
                  events = sortedTimedDtos,
                  currentTime = now,
                  zone = zoneId,
                  date = date,
                  project = false)

          DayPageUiState(
              isLoading = false,
              allDayEvents = allDayDtos,
              timedEvents = timedUiModels,
              projects = ribbons,
              targetScrollIndex = scrollIndex)
        }
        .flowOn(Dispatchers.Default) // Вся эта работа - в фоновом потоке
        .distinctUntilChanged()
  }

  /** Страница месяца на экране проектов: карточки проектов, которые пересекают этот месяц. */
  fun getMonthPageUiState(month: YearMonth): Flow<MonthPageUiState> =
      combine(
              calendarRepository.getMonthProjectsFlow(month).withoutPendingDeletions(),
              currentTime,
              timeZone) { projects, now, zone ->
            MonthPageUiState(
                isLoading = false,
                events =
                    eventUiModelMapper.mapToUiModels(
                        events = projects,
                        currentTime = now,
                        zone = zone,
                        date = now.atZone(zone).toLocalDate(),
                        project = true))
          }
          .flowOn(Dispatchers.Default)
          .distinctUntilChanged()

  // --- ДЕЙСТВИЯ КАЛЕНДАРЯ ---
  fun onVisibleDateChanged(newDate: LocalDate) {
    calendarStateHolder.setCurrentVisibleDate(newDate)
  }

  fun requestEventDetails(event: EventDto) {
    val details = eventUiDetailsModelMapper.mapToUiModels(event, timeZone.value, currentTime.value)
    _uiState.update { it.copy(eventDetails = details) }
  }

  fun cancelEventDetails() {
    _uiState.update { it.copy(eventDetails = null) }
  }

  /** Просит систему синхронизировать аккаунты; UI обновится сам через ContentObserver. */
  fun syncCalendars() {
    viewModelScope.launch {
      calendarRepository.requestSync()
      _events.send(CalendarUiEvent.ShowMessage(UiText.from(R.string.syncing_calendars)))
    }
  }

  /** Прячет события, удалённые с возможностью отмены, до их настоящего удаления. */
  private fun Flow<List<EventDto>>.withoutPendingDeletions(): Flow<List<EventDto>> =
      combine(pendingDeletions.ids) { events, hidden ->
        if (hidden.isEmpty()) events else events.filterNot { it.id in hidden }
      }
}

sealed class CalendarUiEvent {
  data class ShowMessage(val message: UiText) : CalendarUiEvent()
}
