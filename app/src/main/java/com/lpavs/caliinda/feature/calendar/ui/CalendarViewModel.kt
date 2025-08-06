package com.lpavs.caliinda.feature.calendar.ui


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.core.common.EventNetworkState
import com.lpavs.caliinda.core.data.auth.AuthManager
import com.lpavs.caliinda.core.data.di.ITimeTicker
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.core.ui.util.IDateTimeUtils
import com.lpavs.caliinda.feature.calendar.data.EventUiModelMapper
import com.lpavs.caliinda.feature.calendar.ui.components.events.DayPageUiState
import com.lpavs.caliinda.feature.calendar.ui.components.events.EventUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import java.time.ZoneId

@HiltViewModel
class CalendarViewModel
@Inject
constructor(
    private val authManager: AuthManager,
    private val calendarRepository: CalendarRepository,
    private val timeTicker: ITimeTicker,
    settingsRepository: SettingsRepository,
    private val dateTimeUtils: IDateTimeUtils,
    private val eventUiModelMapper: EventUiModelMapper,

) : ViewModel() {

  // --- ОСНОВНОЕ СОСТОЯНИЕ UI ---
  private val _uiState = MutableStateFlow(CalendarState())
  val state: StateFlow<CalendarState> = _uiState.asStateFlow()

  private var initialAuthCheckCompletedAndProcessed = false

  // --- ДЕЛЕГИРОВАННЫЕ И ПРОИЗВОДНЫЕ СОСТОЯНИЯ ДЛЯ UI ---
  val currentTime: StateFlow<Instant> = timeTicker.currentTime

  val timeZone: StateFlow<String> =
    settingsRepository.timeZoneFlow.stateIn(
      viewModelScope, SharingStarted.WhileSubscribed(5000), ZoneId.systemDefault().id
    )

  // Состояния Календаря
  private val _currentVisibleDate = MutableStateFlow(LocalDate.now())
  val currentVisibleDate: StateFlow<LocalDate> = _currentVisibleDate.asStateFlow()
  val rangeNetworkState: StateFlow<EventNetworkState> = calendarRepository.rangeNetworkState

  private val _eventFlow = MutableSharedFlow<CalendarUiEvent>()
  val eventFlow: SharedFlow<CalendarUiEvent> = _eventFlow.asSharedFlow()

  init {
    observeAuthState()
    observeCalendarNetworkState()
  }


  private fun observeAuthState() {
    viewModelScope.launch {
      authManager.authState.collect { authState ->
        val previousUiState = _uiState.value
        _uiState.update { currentState ->
          currentState.copy(
              isSignedIn = authState.isSignedIn,
              isLoading = calculateIsLoading(authLoading = authState.isLoading),)
        }

        authState.authError?.let { error ->
          _eventFlow.emit(CalendarUiEvent.ShowMessage(error))
          authManager.clearAuthError()
        }
        if (!initialAuthCheckCompletedAndProcessed && !authState.isLoading) {
          initialAuthCheckCompletedAndProcessed = true
          Log.d(TAG, "Initial auth check completed and processed.")
          if (!authState.isSignedIn && authState.authError == null) {
            Log.d(TAG, "Initial auth check: Showing sign-in required dialog.")
            _uiState.update { it.copy(showSignInRequiredDialog = true) }
          } else {
            _uiState.update { it.copy(showSignInRequiredDialog = false) }
          }
        }
        if (authState.isSignedIn && _uiState.value.showSignInRequiredDialog) {
          _uiState.update { it.copy(showSignInRequiredDialog = false) }
        }
        if (authState.isSignedIn && !previousUiState.isSignedIn) {
          _uiState.update { it.copy(showSignInRequiredDialog = false) }
        }
        if (authState.isSignedIn && !previousUiState.isSignedIn) {
          Log.d(TAG, "Auth observer: User signed in. Triggering calendar refresh")
          calendarRepository.setCurrentVisibleDate(currentVisibleDate.value, forceRefresh = true)
        }
      }
    }
  }

  private fun observeCalendarNetworkState() {
    viewModelScope.launch {
      calendarRepository.rangeNetworkState.collect { network ->
        _uiState.update { it.copy(isLoading = calculateIsLoading(networkState = network)) }

        if (network is EventNetworkState.Error) {
          if (authManager.authState.value.authError == null) {
            _eventFlow.emit(CalendarUiEvent.ShowMessage(network.message))
          }
        }
      }
    }
  }




  // --- ПРИВАТНЫЙ ХЕЛПЕР ДЛЯ РАСЧЕТА ОБЩЕГО isLoading ---
  /** Рассчитывает общее состояние загрузки, комбинируя состояния менеджеров */
  private fun calculateIsLoading(
      authLoading: Boolean =
          authManager.authState.value.isLoading, // Берем текущие значения по умолчанию
      networkState: EventNetworkState = calendarRepository.rangeNetworkState.value,
  ): Boolean {
    val calendarLoading = networkState is EventNetworkState.Loading

    return authLoading || calendarLoading
  }



  fun getEventsUiModelsForDate(date: LocalDate): Flow<List<EventUiModel>> {
    val timeZoneIdFlow: Flow<ZoneId> = timeZone.map { zoneIdString -> ZoneId.of(zoneIdString) }

    return calendarRepository.getEventsFlowForDate(date)
      .combine(timeTicker.currentTime) { events, now -> Pair(events, now) }
      .combine(timeZoneIdFlow) { (events, now), zoneId ->
        eventUiModelMapper.mapToUiModels(
          events = events,
          currentTime = now,
          timeZoneId = zoneId.toString(),
          date = date
        )
      }
      .flowOn(Dispatchers.Default)
      .distinctUntilChanged()
  }

  fun getDayPageUiState(date: LocalDate): Flow<DayPageUiState> {
    val timeZoneIdFlow: Flow<ZoneId> = timeZone.map { zoneIdString -> ZoneId.of(zoneIdString) }
    val rangeNetworkStateFlow = calendarRepository.rangeNetworkState

    return calendarRepository.getEventsFlowForDate(date)
      .combine(currentTime) { events, now -> events to now }
      .combine(timeZoneIdFlow) { (events, now), zoneId -> Triple(events, now, zoneId) }
      .combine(rangeNetworkStateFlow) { (events, now, zoneId), networkState ->

        // !!! ВСЯ ЛОГИКА ИЗ DayEventsPage ПЕРЕЕЗЖАЕТ СЮДА !!!

        val isToday = date == LocalDate.now()
        val zoneId = zoneId.toString()

        // 1. Разделяем события
        val (allDayDtos, timedDtos) = events.partition { it.isAllDay }

        // 2. Сортируем события со временем
        val sortedTimedDtos = timedDtos.sortedBy { event ->
          dateTimeUtils.parseToInstant(event.startTime, zoneId) ?: Instant.MAX
        }

        // 3. Вычисляем nextStartTime (промежуточный шаг)
        val nextStartTime: Instant? = if (!isToday) {
          null
        } else {
          sortedTimedDtos.firstNotNullOfOrNull { event ->
            val start = dateTimeUtils.parseToInstant(event.startTime, zoneId)
            if (start != null && start.isAfter(now)) start else null
          }
        }

        // 4. ОПРЕДЕЛЯЕМ ЦЕЛЕВОЙ ИНДЕКС ДЛЯ ПРОКРУТКИ
        val scrollIndex = if (!isToday || sortedTimedDtos.isEmpty()) {
          -1
        } else {
          val currentEventIndex = sortedTimedDtos.indexOfFirst { event ->
            val start = dateTimeUtils.parseToInstant(event.startTime, zoneId)
            val end = dateTimeUtils.parseToInstant(event.endTime, zoneId)
            start != null && end != null && !now.isBefore(start) && now.isBefore(end)
          }
          if (currentEventIndex != -1) {
            currentEventIndex
          } else if (nextStartTime != null) {
            sortedTimedDtos.indexOfFirst { event ->
              val start = dateTimeUtils.parseToInstant(event.startTime, zoneId)
              start != null && start == nextStartTime
            }
          } else {
            -1
          }
        }

        val timedUiModels = eventUiModelMapper.mapToUiModels(
          events = sortedTimedDtos,
          currentTime = now,
          timeZoneId = zoneId.toString(),
          date = date
        )

        // 6. Возвращаем полностью готовый стейт для страницы
        DayPageUiState(
          isLoading = networkState is EventNetworkState.Loading,
          allDayEvents = allDayDtos,
          timedEvents = timedUiModels,
          targetScrollIndex = scrollIndex
        )
      }
      .flowOn(Dispatchers.Default) // Вся эта работа - в фоновом потоке
      .distinctUntilChanged()
  }

  // --- ДЕЙСТВИЯ АУТЕНТИФИКАЦИИ ---

  fun onSignInRequiredDialogDismissed() {
    _uiState.update { it.copy(showSignInRequiredDialog = false) }
    Log.d(TAG, "Sign-in dismissed")
  }

  // --- ДЕЙСТВИЯ КАЛЕНДАРЯ ---
  fun onVisibleDateChanged(newDate: LocalDate) {
    if (newDate == _currentVisibleDate.value) return
    _currentVisibleDate.value = newDate
    viewModelScope.launch { calendarRepository.setCurrentVisibleDate(newDate) }
  }

  fun getEventsFlowForDate(date: LocalDate): Flow<List<EventDto>> =
      calendarRepository.getEventsFlowForDate(date)

  fun refreshCurrentVisibleDate() {
    viewModelScope.launch { calendarRepository.refreshDate(currentVisibleDate.value) }
  }

  // --- COMPANION ---
  companion object {
    private const val TAG = "CalendarViewModel" // Используем один TAG
  }
}

sealed class CalendarUiEvent {
  data class ShowMessage(val message: String) : CalendarUiEvent()
}
