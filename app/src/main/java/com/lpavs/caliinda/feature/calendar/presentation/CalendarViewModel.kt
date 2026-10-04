package com.lpavs.caliinda.feature.calendar.presentation

import com.lpavs.caliinda.core.data.repository.PendingDeletions
import com.lpavs.caliinda.core.data.utils.UiText
import com.lpavs.caliinda.R
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.core.data.calendar.CalendarPermissionManager
import com.lpavs.caliinda.core.data.di.ICalendarStateHolder
import com.lpavs.caliinda.core.data.di.ITimeTicker
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.feature.calendar.data.EventUiDetailsModelMapper
import com.lpavs.caliinda.feature.calendar.data.EventUiModelMapper
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroState
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroStep
import com.lpavs.caliinda.feature.calendar.presentation.components.page.DayPageUiState
import com.lpavs.caliinda.feature.calendar.presentation.components.page.EventsPageUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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

  val timeZone: StateFlow<String> =
      settingsRepository.timeZoneFlow.stateIn(
          viewModelScope, SharingStarted.WhileSubscribed(5000), ZoneId.systemDefault().id)

  /** Сегодняшняя дата в поясе из настроек; сменяется в полночь (с точностью до тика таймера). */
  val today: StateFlow<LocalDate> =
      combine(currentTime, timeZone) { now, zone -> now.atZone(parseZone(zone)).toLocalDate() }
          .distinctUntilChanged()
          .stateIn(viewModelScope, SharingStarted.Eagerly, LocalDate.now(parseZone(timeZone.value)))

  // Состояния Календаря
  val currentVisibleDate: StateFlow<LocalDate> = calendarStateHolder.currentVisibleDate

  private val _eventFlow = MutableSharedFlow<CalendarUiEvent>()
  val eventFlow: SharedFlow<CalendarUiEvent> = _eventFlow.asSharedFlow()

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
    val timeZoneIdFlow: Flow<ZoneId> = timeZone.map { zoneIdString -> ZoneId.of(zoneIdString) }

    return calendarRepository
        .getEventsFlowForDate(date)
        .withoutPendingDeletions()
        .combine(currentTime) { events, now -> events to now }
        .combine(timeZoneIdFlow) { (events, now), zoneId ->
          val isToday = date == now.atZone(zoneId).toLocalDate()
          val zoneId = zoneId.toString()

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
                  timeZoneId = zoneId.toString(),
                  date = date,
                  project = false)

          DayPageUiState(
              isLoading = false,
              allDayEvents = allDayDtos,
              timedEvents = timedUiModels,
              targetScrollIndex = scrollIndex)
        }
        .flowOn(Dispatchers.Default) // Вся эта работа - в фоновом потоке
        .distinctUntilChanged()
  }

  fun getProjectsPageUiState(): Flow<EventsPageUiState> {
    // Окно проектов отсчитывается от сегодня — после полуночи перезапрашиваем.
    return today
        .flatMapLatest { date ->
          calendarRepository.getEventsFlowForProjects(date).withoutPendingDeletions().map { events ->
            date to events
          }
        }
        .combine(currentTime) { dated, now -> dated to now }
        .combine(timeZone) { (dated, now), zoneId ->
          val (date, events) = dated
          val projectUIModels =
              eventUiModelMapper.mapToUiModels(
                  events = events,
                  currentTime = now,
                  timeZoneId = zoneId,
                  date = date,
                  project = true)

          EventsPageUiState(isLoading = false, events = projectUIModels)
        }
        .flowOn(Dispatchers.Default) // Вся эта работа - в фоновом потоке
        .distinctUntilChanged()
  }

  // --- ДЕЙСТВИЯ КАЛЕНДАРЯ ---
  fun onVisibleDateChanged(newDate: LocalDate) {
    calendarStateHolder.setCurrentVisibleDate(newDate)
  }

  fun requestEventDetails(event: EventDto) {
    _uiState.update { currentState ->
      val eventForDetails =
          eventUiDetailsModelMapper.mapToUiModels(event, timeZone.value, currentTime.value)
      currentState.copy(eventForDetailedView = eventForDetails, showEventDetailedView = true)
    }
    Log.d(TAG, "Requested event details for event ID: ${event.id}")
  }

  fun cancelEventDetails() {
    _uiState.update { currentState ->
      currentState.copy(eventForDetailedView = null, showEventDetailedView = false)
    }
    Log.d(TAG, "Cancelled event details view.")
  }

  /** Просит систему синхронизировать аккаунты; UI обновится сам через ContentObserver. */
  fun syncCalendars() {
    viewModelScope.launch {
      calendarRepository.requestSync()
      _eventFlow.emit(CalendarUiEvent.ShowMessage(UiText.from(R.string.syncing_calendars)))
    }
  }

  /** Прячет события, удалённые с возможностью отмены, до их настоящего удаления. */
  private fun Flow<List<EventDto>>.withoutPendingDeletions(): Flow<List<EventDto>> =
      combine(pendingDeletions.ids) { events, hidden ->
        if (hidden.isEmpty()) events else events.filterNot { it.id in hidden }
      }

  private fun parseZone(zone: String): ZoneId =
      runCatching { ZoneId.of(zone) }.getOrDefault(ZoneId.systemDefault())

  // --- COMPANION ---
  companion object {
    private const val TAG = "CalendarViewModel" // Используем один TAG
  }
}

sealed class CalendarUiEvent {
  data class ShowMessage(val message: UiText) : CalendarUiEvent()
}
