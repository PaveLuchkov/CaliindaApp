package com.lpavs.caliinda.feature.event_management.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDeleteMode
import com.lpavs.caliinda.core.data.calendar.model.EventDraft
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.calendar.model.EventUpdateMode
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.core.data.utils.UiText
import com.lpavs.caliinda.core.ui.util.IDateTimeUtils
import com.lpavs.caliinda.feature.calendar.presentation.components.IFunMessages
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringDeleteChoice
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimeState
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.RecurrenceEndType
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.RecurrenceOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class EventManagementViewModel
@Inject
constructor(
    settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
    private val funMessages: IFunMessages,
    private val dateTimeUtils: IDateTimeUtils
) : ViewModel() {
  private val _uiState = MutableStateFlow(EventManagementUiState())
  val uiState: StateFlow<EventManagementUiState> = _uiState.asStateFlow()

  private val _eventFlow = MutableSharedFlow<EventManagementUiEvent>()
  val eventFlow: SharedFlow<EventManagementUiEvent> = _eventFlow.asSharedFlow()

  val timeZone: StateFlow<String> =
      settingsRepository.timeZoneFlow.stateIn(
          viewModelScope, SharingStarted.WhileSubscribed(5000), ZoneId.systemDefault().id)
  private val untilFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

  // --- Создание ---

  fun createEvent(
      summary: String,
      description: String,
      location: String,
      dateTimeState: EventDateTimeState
  ) {
    viewModelScope.launch {
      if (!validateInput(summary, dateTimeState)) {
        _eventFlow.emit(
            EventManagementUiEvent.ShowMessage(UiText.from(R.string.error_check_input_data)))
        return@launch
      }
      val draft =
          buildDraft(summary, description, location, dateTimeState, buildRecurrenceRule(dateTimeState))
      runOperation(
          operation = { calendarRepository.createEvent(draft) },
          successMessage = { funMessages.getEventCreatedMessage(draft.summary) },
          errorMessage = { funMessages.getCreateErrorMessage() })
    }
  }

  // --- Редактирование ---

  fun updateEvent(
      summary: String,
      description: String,
      location: String,
      dateTimeState: EventDateTimeState,
      updateMode: EventUpdateMode
  ) {
    viewModelScope.launch {
      val originalEvent = uiState.value.eventBeingEdited
      if (originalEvent == null) {
        Log.e(TAG, "updateEvent called but originalEvent is null")
        return@launch
      }
      if (!validateInput(summary, dateTimeState)) {
        _eventFlow.emit(
            EventManagementUiEvent.ShowMessage(UiText.from(R.string.error_check_input_data)))
        return@launch
      }

      // Форма умеет не все части RRULE (INTERVAL, BYMONTHDAY...). Если повторение не трогали —
      // сохраняем исходное правило как есть, чтобы не потерять их.
      val originalState = parseEventToState(originalEvent)
      val formRule = buildRecurrenceRule(dateTimeState)
      val recurrenceRule =
          if (formRule == buildRecurrenceRule(originalState)) originalEvent.recurrenceRule
          else formRule

      val draft = buildDraft(summary, description, location, dateTimeState, recurrenceRule)
      val unchangedDraft =
          buildDraft(
              originalEvent.summary,
              originalEvent.description.orEmpty(),
              originalEvent.location.orEmpty(),
              originalState,
              originalEvent.recurrenceRule)
      if (draft == unchangedDraft) {
        _eventFlow.emit(
            EventManagementUiEvent.ShowMessage(UiText.from(R.string.no_changes_to_save)))
        _eventFlow.emit(EventManagementUiEvent.OperationSuccess)
        return@launch
      }

      runOperation(
          operation = { calendarRepository.updateEvent(originalEvent, draft, updateMode) },
          successMessage = { funMessages.getEventUpdatedMessage(originalEvent.summary) },
          errorMessage = { funMessages.getUpdateErrorMessage() })
    }
  }

  // --- Удаление ---

  fun confirmDeleteEvent() {
    val eventToDelete = _uiState.value.eventPendingDeletion ?: return
    _uiState.update {
      it.copy(showDeleteConfirmationDialog = false, eventPendingDeletion = null)
    }
    viewModelScope.launch {
      runOperation(
          operation = { calendarRepository.deleteEvent(eventToDelete, EventDeleteMode.DEFAULT) },
          successMessage = { funMessages.getEventDeletedMessage(eventToDelete.summary) },
          errorMessage = { funMessages.getDeleteErrorMessage() })
    }
  }

  fun confirmRecurringDelete(choice: RecurringDeleteChoice) {
    val eventToDelete = _uiState.value.eventPendingDeletion ?: return
    _uiState.update {
      it.copy(
          showDeleteConfirmationDialog = false,
          showRecurringDeleteOptionsDialog = false,
          eventPendingDeletion = null)
    }
    val mode =
        when (choice) {
          RecurringDeleteChoice.SINGLE_INSTANCE -> EventDeleteMode.INSTANCE_ONLY
          RecurringDeleteChoice.THIS_AND_FOLLOWING -> EventDeleteMode.THIS_AND_FOLLOWING
          RecurringDeleteChoice.ALL_IN_SERIES -> EventDeleteMode.ALL_IN_SERIES
        }
    viewModelScope.launch {
      runOperation(
          operation = { calendarRepository.deleteEvent(eventToDelete, mode) },
          successMessage = {
            if (mode == EventDeleteMode.INSTANCE_ONLY)
                funMessages.getEventDeletedMessage(eventToDelete.summary)
            else funMessages.getSeriesDeletedMessage()
          },
          errorMessage = { funMessages.getDeleteErrorMessage() })
    }
  }

  private suspend fun runOperation(
      operation: suspend () -> Result<Unit>,
      successMessage: () -> UiText,
      errorMessage: () -> UiText
  ) {
    _uiState.update { it.copy(isLoading = true) }
    val result = operation()
    _uiState.update { it.copy(isLoading = false) }
    if (result.isSuccess) {
      _eventFlow.emit(EventManagementUiEvent.ShowMessage(successMessage()))
      _eventFlow.emit(EventManagementUiEvent.OperationSuccess)
    } else {
      _eventFlow.emit(EventManagementUiEvent.ShowMessage(errorMessage()))
    }
  }

  // --- Форма <-> модель ---

  private fun buildDraft(
      summary: String,
      description: String,
      location: String,
      state: EventDateTimeState,
      recurrenceRule: String?
  ) =
      EventDraft(
          summary = summary.trim(),
          description = description.trim().takeIf { it.isNotEmpty() },
          location = location.trim().takeIf { it.isNotEmpty() },
          isAllDay = state.isAllDay,
          startDate = state.startDate,
          startTime = if (state.isAllDay) null else state.startTime,
          endDate = state.endDate,
          endTime = if (state.isAllDay) null else state.endTime,
          timeZoneId = timeZone.value,
          recurrenceRule = recurrenceRule)

  private fun validateInput(summary: String, state: EventDateTimeState): Boolean {
    if (summary.isBlank()) return false
    if (state.endDate.isBefore(state.startDate)) return false
    if (state.isAllDay) return true
    val startTime = state.startTime ?: return false
    val endTime = state.endTime ?: return false
    return LocalDateTime.of(state.endDate, endTime).isAfter(LocalDateTime.of(state.startDate, startTime))
  }

  private fun buildRecurrenceRule(state: EventDateTimeState): String? {
    val baseRule = state.recurrenceRule?.takeIf { it.isNotBlank() } ?: return null

    val ruleParts = mutableListOf(baseRule) // Начинаем с FREQ=...

    if (baseRule == RecurrenceOption.Weekly.rruleValue && state.selectedWeekdays.isNotEmpty()) {
      val bydayString =
          state.selectedWeekdays.sorted().joinToString(",") { day ->
            when (day) {
              DayOfWeek.MONDAY -> "MO"
              DayOfWeek.TUESDAY -> "TU"
              DayOfWeek.WEDNESDAY -> "WE"
              DayOfWeek.THURSDAY -> "TH"
              DayOfWeek.FRIDAY -> "FR"
              DayOfWeek.SATURDAY -> "SA"
              DayOfWeek.SUNDAY -> "SU"
            }
          }
      ruleParts.add("BYDAY=$bydayString")
    }

    when (state.recurrenceEndType) {
      RecurrenceEndType.DATE -> {
        state.recurrenceEndDate?.let { endDate ->
          val until =
              if (state.isAllDay) {
                // Для all-day серий UNTIL должен быть датой (RFC 5545).
                endDate.format(DateTimeFormatter.BASIC_ISO_DATE)
              } else {
                untilFormatter.format(
                    endDate
                        .atTime(23, 59, 59)
                        .atZone(ZoneId.of(timeZone.value))
                        .withZoneSameInstant(ZoneOffset.UTC))
              }
          ruleParts.add("UNTIL=$until")
        }
      }
      RecurrenceEndType.COUNT -> {
        state.recurrenceCount?.let { count -> ruleParts.add("COUNT=$count") }
      }
      RecurrenceEndType.NEVER -> {}
    }

    return ruleParts.joinToString(";")
  }

  fun parseEventToState(event: EventDto): EventDateTimeState {
    val userTimeZoneId = timeZone.value
    val isAllDay = event.isAllDay

    var parsedStartDate = LocalDate.now()
    var parsedEndDate = LocalDate.now()
    var parsedStartTime: LocalTime? = null
    var parsedEndTime: LocalTime? = null

    try {
      if (isAllDay) {
        // startTime/endTime — локальная полночь; конец эксклюзивный.
        parsedStartDate = LocalDate.parse(event.startTime?.take(10))
        val rawEndDate = LocalDate.parse(event.endTime?.take(10))
        parsedEndDate =
            if (rawEndDate.isAfter(parsedStartDate)) rawEndDate.minusDays(1) else rawEndDate
      } else {
        val zone = ZoneId.of(userTimeZoneId)
        dateTimeUtils.parseToInstant(event.startTime, userTimeZoneId)?.atZone(zone)?.let {
          parsedStartDate = it.toLocalDate()
          parsedStartTime = it.toLocalTime().withNano(0)
        }
        dateTimeUtils.parseToInstant(event.endTime, userTimeZoneId)?.atZone(zone)?.let {
          parsedEndDate = it.toLocalDate()
          parsedEndTime = it.toLocalTime().withNano(0)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to parse event ${event.id}: start=${event.startTime}, end=${event.endTime}", e)
    }

    var recurrenceOption: RecurrenceOption? = null
    var selectedWeekdays: Set<DayOfWeek> = emptySet()
    var recurrenceEndType = RecurrenceEndType.NEVER
    var recurrenceEndDate: LocalDate? = null
    var recurrenceCount: Int? = null

    event.recurrenceRule?.removePrefix("RRULE:")?.split(';')?.forEach { rulePart ->
      val parts = rulePart.split('=')
      if (parts.size != 2) return@forEach
      val (key, value) = parts
      when (key.uppercase()) {
        "FREQ" -> recurrenceOption = RecurrenceOption.ALL_OPTIONS.find { it.rruleValue == "FREQ=$value" }
        "BYDAY" ->
            selectedWeekdays =
                value
                    .split(',')
                    .mapNotNull { dayStr ->
                      when (dayStr.takeLast(2)) {
                        "MO" -> DayOfWeek.MONDAY
                        "TU" -> DayOfWeek.TUESDAY
                        "WE" -> DayOfWeek.WEDNESDAY
                        "TH" -> DayOfWeek.THURSDAY
                        "FR" -> DayOfWeek.FRIDAY
                        "SA" -> DayOfWeek.SATURDAY
                        "SU" -> DayOfWeek.SUNDAY
                        else -> null
                      }
                    }
                    .toSet()
        "UNTIL" -> {
          recurrenceEndDate = parseUntil(value, userTimeZoneId)
          if (recurrenceEndDate != null) recurrenceEndType = RecurrenceEndType.DATE
        }
        "COUNT" -> {
          recurrenceCount = value.toIntOrNull()
          if (recurrenceCount != null) recurrenceEndType = RecurrenceEndType.COUNT
        }
      }
    }

    return EventDateTimeState(
        startDate = parsedStartDate,
        startTime = parsedStartTime,
        endDate = parsedEndDate,
        endTime = parsedEndTime,
        isAllDay = isAllDay,
        isRecurring = event.recurrenceRule != null,
        recurrenceRule = recurrenceOption?.rruleValue,
        selectedWeekdays = selectedWeekdays,
        recurrenceEndType = recurrenceEndType,
        recurrenceEndDate = recurrenceEndDate,
        recurrenceCount = recurrenceCount)
  }

  private fun parseUntil(value: String, userTimeZoneId: String): LocalDate? =
      try {
        if (value.length == 8) {
          LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)
        } else {
          ZonedDateTime.parse(value, untilFormatter.withZone(ZoneOffset.UTC))
              .withZoneSameInstant(ZoneId.of(userTimeZoneId))
              .toLocalDate()
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error parsing UNTIL value: $value", e)
        null
      }

  // --- Состояние диалогов ---

  fun requestDeleteConfirmation(event: EventDto) {
    val isRecurring = event.recurringEventId != null
    _uiState.update {
      it.copy(
          eventPendingDeletion = event,
          showDeleteConfirmationDialog = !isRecurring,
          showRecurringDeleteOptionsDialog = isRecurring,
      )
    }
  }

  fun cancelDelete() {
    _uiState.update {
      it.copy(
          eventPendingDeletion = null,
          showDeleteConfirmationDialog = false,
          showRecurringDeleteOptionsDialog = false)
    }
  }

  fun requestEditEvent(event: EventDto) {
    val isRecurring = event.recurringEventId != null
    _uiState.update {
      it.copy(
          eventBeingEdited = event,
          showRecurringEditOptionsDialog = isRecurring,
          showEditEventDialog = !isRecurring,
          selectedUpdateMode = if (!isRecurring) EventUpdateMode.ALL_IN_SERIES else it.selectedUpdateMode,
      )
    }
  }

  fun onRecurringEditOptionSelected(choice: EventUpdateMode) {
    if (_uiState.value.eventBeingEdited == null) {
      Log.e(TAG, "onRecurringEditOptionSelected called but eventBeingEdited is null.")
      cancelEditEvent()
      return
    }
    _uiState.update {
      it.copy(
          showRecurringEditOptionsDialog = false,
          showEditEventDialog = true,
          selectedUpdateMode = choice)
    }
  }

  fun cancelEditEvent() {
    _uiState.update {
      it.copy(
          eventBeingEdited = null,
          showRecurringEditOptionsDialog = false,
          showEditEventDialog = false)
    }
  }

  fun requestEventDetails(event: EventDto) {
    _uiState.update { it.copy(eventForDetailedView = event, showEventDetailedView = true) }
  }

  fun cancelEventDetails() {
    _uiState.update { it.copy(eventForDetailedView = null, showEventDetailedView = false) }
  }

  companion object {
    private const val TAG = "EventManagementViewModel"
  }
}

data class EventManagementUiState(
    val isLoading: Boolean = false,
    val eventPendingDeletion: EventDto? = null,
    val showDeleteConfirmationDialog: Boolean = false,
    val showRecurringDeleteOptionsDialog: Boolean = false,
    val eventBeingEdited: EventDto? = null,
    val showRecurringEditOptionsDialog: Boolean = false,
    val showEditEventDialog: Boolean = false,
    val selectedUpdateMode: EventUpdateMode? = null,
    val eventForDetailedView: EventDto? = null,
    val showEventDetailedView: Boolean = false,
)

sealed class EventManagementUiEvent {
  data class ShowMessage(val message: UiText) : EventManagementUiEvent()

  object OperationSuccess : EventManagementUiEvent()
}
