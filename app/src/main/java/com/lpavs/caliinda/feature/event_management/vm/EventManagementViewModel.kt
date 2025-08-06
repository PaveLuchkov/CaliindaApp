package com.lpavs.caliinda.feature.event_management.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.remote.EventDeleteMode
import com.lpavs.caliinda.core.data.remote.EventUpdateMode
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.data.remote.dto.EventRequest
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.core.data.utils.UiText
import com.lpavs.caliinda.core.ui.util.IDateTimeUtils
import com.lpavs.caliinda.feature.calendar.ui.components.IFunMessages
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
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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

  fun confirmEventUpdate(updatedEventData: EventRequest, modeFromUi: EventUpdateMode) {
    val originalEvent = _uiState.value.eventBeingEdited ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, operationError = null) }
      val result =
          calendarRepository.updateEvent(
              eventId = originalEvent.id, updateData = updatedEventData, mode = modeFromUi)
      _uiState.update { it.copy(isLoading = false) }
      if (result.isSuccess) {
        val message = funMessages.getEventUpdatedMessage(originalEvent.summary)
        _eventFlow.emit(EventManagementUiEvent.ShowMessage(message))
        _eventFlow.emit(EventManagementUiEvent.OperationSuccess)
      } else {
        val message =
            result.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
                ?: funMessages.getUpdateErrorMessage()
        _eventFlow.emit(EventManagementUiEvent.ShowMessage(message))
      }
    }
  }

  fun createEvent(
      summary: String,
      description: String,
      location: String,
      dateTimeState: EventDateTimeState
  ) {
    viewModelScope.launch {
      // 1. Валидация
      if (!validateInput(summary, dateTimeState)) {
        _eventFlow.emit(
            EventManagementUiEvent.ShowMessage(UiText.from(R.string.error_check_input_data)))
        return@launch
      }

      _uiState.update { it.copy(isLoading = true, operationError = null) }

      // 2. Форматирование строк времени
      val (startStr, endStr) = formatEventTimesForSaving(dateTimeState)
      if (startStr == null || endStr == null) {
        _uiState.update { it.copy(isLoading = false) }
        _eventFlow.emit(
            EventManagementUiEvent.ShowMessage(
                UiText.from(R.string.error_failed_to_format_datetime)))
        Log.e(
            TAG,
            "Failed to format strings based on state: $dateTimeState and TimeZone: ${timeZone.value}")
        return@launch
      }

      // 3. Построение RRULE
      val finalRecurrenceRule = buildRecurrenceRule(dateTimeState)
      Log.d(TAG, "Final RRULE to send: $finalRecurrenceRule")

      // 4. Создание объекта запроса
      val request =
          EventRequest(
              summary = summary.trim(),
              startTime = startStr,
              endTime = endStr,
              isAllDay = dateTimeState.isAllDay,
              timeZoneId = if (dateTimeState.isAllDay) null else timeZone.value,
              description = description.trim().takeIf { it.isNotEmpty() },
              location = location.trim().takeIf { it.isNotEmpty() },
              recurrence = finalRecurrenceRule?.let { listOf("RRULE:$it") })

      // 5. Отправка в репозиторий
      val result = calendarRepository.createEvent(request)
      _uiState.update { it.copy(isLoading = false) }

      // 6. Обработка результата
      if (result.isSuccess) {
        val message = funMessages.getEventCreatedMessage(request.summary)
        _eventFlow.emit(EventManagementUiEvent.ShowMessage(message))
        _eventFlow.emit(EventManagementUiEvent.OperationSuccess)
      } else {
        val message =
            result.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
                ?: funMessages.getCreateErrorMessage()
        _eventFlow.emit(EventManagementUiEvent.ShowMessage(message))
      }
    }
  }

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

      _uiState.update { it.copy(isLoading = true) }

      val (startStr, endStr) = formatEventTimesForSaving(dateTimeState)
      if (startStr == null || endStr == null) {
        _uiState.update { it.copy(isLoading = false) }
        _eventFlow.emit(
            EventManagementUiEvent.ShowMessage(
                UiText.from(R.string.error_failed_to_format_datetime)))
        return@launch
      }

      val finalRecurrenceRule = buildRecurrenceRule(dateTimeState)

      val updateRequest =
          buildUpdateEventApiRequest(
              originalEvent = originalEvent,
              currentSummary = summary.trim(),
              currentDescription = description.trim(),
              currentLocation = location.trim(),
              currentDateTimeState = dateTimeState,
              formattedStartStr = startStr,
              formattedEndStr = endStr,
              finalRRuleStringFromUi = finalRecurrenceRule,
              selectedUpdateMode = updateMode)

      if (updateRequest == null) {
        _uiState.update { it.copy(isLoading = false) }
        _eventFlow.emit(
            EventManagementUiEvent.ShowMessage(UiText.from(R.string.no_changes_to_save)))
        _eventFlow.emit(EventManagementUiEvent.OperationSuccess) // Закрываем экран
        return@launch
      }

      confirmEventUpdate(updateRequest, updateMode)
    }
  }

  private fun validateInput(summary: String, state: EventDateTimeState): Boolean {
    if (summary.isBlank()) {
      // Можно добавить специальное событие для подсветки поля, если нужно
      return false
    }
    if (!state.isAllDay && (state.startTime == null || state.endTime == null)) {
      return false
    }
    val (start, end) = formatEventTimesForSaving(state)
    return start != null && end != null
  }

  private fun formatEventTimesForSaving(state: EventDateTimeState): Pair<String?, String?> {
    return if (state.isAllDay) {
      val formatter = DateTimeFormatter.ISO_LOCAL_DATE
      val startDateStr =
          try {
            state.startDate.format(formatter)
          } catch (_: Exception) {
            null
          }
      // Для all-day событий, конечная дата должна быть на день позже и не включаться
      val effectiveEndDate = state.endDate.plusDays(1)
      val endDateStr =
          try {
            effectiveEndDate.format(formatter)
          } catch (_: Exception) {
            null
          }
      Pair(startDateStr, endDateStr)
    } else {
      val timeZoneId = timeZone.value
      if (timeZoneId.isBlank()) {
        Log.e(TAG, "Cannot format timed event without TimeZone ID!")
        return Pair(null, null)
      }
      // Используем ISO_OFFSET_DATE_TIME для бэкенда
      val startTimeIso =
          dateTimeUtils.formatDateTimeToIsoWithOffset(
              state.startDate, state.startTime, false, timeZoneId)
      val endTimeIso =
          dateTimeUtils.formatDateTimeToIsoWithOffset(
              state.endDate, state.endTime, false, timeZoneId)
      Pair(startTimeIso, endTimeIso)
    }
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
          val endDateTimeUtc =
              endDate
                  .atTime(23, 59, 59)
                  .atZone(ZoneId.of(timeZone.value))
                  .withZoneSameInstant(ZoneOffset.UTC)
          val untilString = untilFormatter.format(endDateTimeUtc)
          ruleParts.add("UNTIL=$untilString")
        }
      }
      RecurrenceEndType.COUNT -> {
        state.recurrenceCount?.let { count -> ruleParts.add("COUNT=$count") }
      }
      RecurrenceEndType.NEVER -> {
        /* Ничего не добавляем */
      }
    }

    return ruleParts.joinToString(";")
  }

  fun parseEventToState(event: EventDto): EventDateTimeState {
    val userTimeZoneId = timeZone.value
    val isAllDay = event.isAllDay

    var parsedStartDate: LocalDate = LocalDate.now()
    var parsedStartTime: LocalTime? = null
    var parsedEndDate: LocalDate = LocalDate.now()
    var parsedEndTime: LocalTime? = null

    try {
      if (isAllDay) {
        parsedStartDate = LocalDate.parse(event.startTime, DateTimeFormatter.ISO_LOCAL_DATE)
        parsedEndDate =
            LocalDate.parse(event.endTime, DateTimeFormatter.ISO_LOCAL_DATE).minusDays(1)
      } else {
        val startInstant = dateTimeUtils.parseToInstant(event.startTime, userTimeZoneId)
        val endInstant = dateTimeUtils.parseToInstant(event.endTime, userTimeZoneId)

        if (startInstant != null) {
          val startZonedDateTime = startInstant.atZone(ZoneId.of(userTimeZoneId))
          parsedStartDate = startZonedDateTime.toLocalDate()
          parsedStartTime = startZonedDateTime.toLocalTime().withNano(0)
        }
        if (endInstant != null) {
          val endZonedDateTime = endInstant.atZone(ZoneId.of(userTimeZoneId))
          parsedEndDate = endZonedDateTime.toLocalDate()
          parsedEndTime = endZonedDateTime.toLocalTime().withNano(0)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing event date/time for editing: ${e.message}")
      val now = ZonedDateTime.now(ZoneId.of(userTimeZoneId))
      parsedStartDate = now.toLocalDate()
      parsedStartTime = if (!isAllDay) now.toLocalTime().plusHours(1).withMinute(0) else null
      parsedEndDate = parsedStartDate
      parsedEndTime = if (!isAllDay) parsedStartTime?.plusHours(1) else null
    }

    // ... (остальная логика парсинга RRULE остается без изменений) ...
    var recurrenceOption: RecurrenceOption? = null
    var selectedWeekdays: Set<DayOfWeek> = emptySet()
    var recurrenceEndType = RecurrenceEndType.NEVER
    var recurrenceEndDate: LocalDate? = null
    var recurrenceCount: Int? = null
    var isRecurring = false

    event.recurrenceRule?.let { rruleString ->
      isRecurring = true
      val rules = rruleString.split(';')
      rules.forEach { rulePart ->
        val parts = rulePart.split('=')
        if (parts.size == 2) {
          val key = parts[0]
          val value = parts[1]
          when (key) {
            "FREQ" -> {
              recurrenceOption =
                  RecurrenceOption.ALL_OPTIONS.find { it.rruleValue == "FREQ=$value" }
            }
            "BYDAY" -> {
              selectedWeekdays =
                  value
                      .split(',')
                      .mapNotNull { dayStr ->
                        when (dayStr) {
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
            }
            "UNTIL" -> {
              try {
                val zonedDateTime =
                    ZonedDateTime.parse(value, untilFormatter.withZone(ZoneOffset.UTC))
                recurrenceEndDate =
                    zonedDateTime.withZoneSameInstant(ZoneId.of(userTimeZoneId)).toLocalDate()
                recurrenceEndType = RecurrenceEndType.DATE
              } catch (e: Exception) {
                Log.e(TAG, "Error parsing UNTIL value: $value - ${e.message}")
              }
            }
            "COUNT" -> {
              recurrenceCount = value.toIntOrNull()
              if (recurrenceCount != null) recurrenceEndType = RecurrenceEndType.COUNT
            }
          }
        }
      }
    }

    return EventDateTimeState(
        startDate = parsedStartDate,
        startTime = parsedStartTime,
        endDate = parsedEndDate,
        endTime = parsedEndTime,
        isAllDay = isAllDay,
        isRecurring = isRecurring,
        recurrenceRule = recurrenceOption?.rruleValue,
        selectedWeekdays = selectedWeekdays,
        recurrenceEndType = recurrenceEndType,
        recurrenceEndDate = recurrenceEndDate,
        recurrenceCount = recurrenceCount)
  }

  private fun buildUpdateEventApiRequest(
      originalEvent: EventDto,
      currentSummary: String,
      currentDescription: String,
      currentLocation: String,
      currentDateTimeState: EventDateTimeState,
      formattedStartStr: String,
      formattedEndStr: String,
      finalRRuleStringFromUi: String?,
      selectedUpdateMode: EventUpdateMode
  ): EventRequest? {
    var hasChanges = false

    val summaryUpdate =
        currentSummary.takeIf { it != originalEvent.summary }?.also { hasChanges = true }
    val descriptionUpdate =
        currentDescription
            .takeIf { it != (originalEvent.description ?: "") }
            ?.also { hasChanges = true }
    val locationUpdate =
        currentLocation.takeIf { it != (originalEvent.location ?: "") }?.also { hasChanges = true }

    var startTimeUpdate: String? = null
    var endTimeUpdate: String? = null
    var isAllDayUpdate: Boolean? = null
    var timeZoneIdUpdate: String? = null

    val originalRRuleString = originalEvent.recurrenceRule?.takeIf { it.isNotBlank() }
    val currentRRuleString = finalRRuleStringFromUi?.takeIf { it.isNotBlank() }
    val recurrenceRuleChanged = currentRRuleString != originalRRuleString

    val isOnlyRecurrenceChangeForAllEvents =
        recurrenceRuleChanged &&
            selectedUpdateMode == EventUpdateMode.ALL_IN_SERIES &&
            currentSummary == originalEvent.summary &&
            currentDescription == (originalEvent.description ?: "") &&
            currentLocation == (originalEvent.location ?: "") &&
            currentDateTimeState.isAllDay == originalEvent.isAllDay

    if (!isOnlyRecurrenceChangeForAllEvents) {
      if (currentDateTimeState.isAllDay != originalEvent.isAllDay) {
        isAllDayUpdate = currentDateTimeState.isAllDay
        hasChanges = true
      }

      val originalStartTimeState = parseEventToState(originalEvent)
      val (originalStartStr, originalEndStr) = formatEventTimesForSaving(originalStartTimeState)

      if (formattedStartStr != originalStartStr) {
        startTimeUpdate = formattedStartStr
        hasChanges = true
      }
      if (formattedEndStr != originalEndStr) {
        endTimeUpdate = formattedEndStr
        hasChanges = true
      }

      if (!currentDateTimeState.isAllDay) {
        if (timeZone.value.isNotBlank()) {
          if (isAllDayUpdate == false ||
              (isAllDayUpdate == null && (startTimeUpdate != null || endTimeUpdate != null))) {
            timeZoneIdUpdate = timeZone.value
            // Считаем изменение таймзоны изменением только если она действительно поменялась, а не
            // просто добавляется
            // hasChanges = true
          }
        }
      }
    }

    var recurrenceForApiRequest: List<String>? = null
    if (recurrenceRuleChanged) {
      hasChanges = true
      recurrenceForApiRequest =
          if (currentRRuleString != null) {
            listOf("RRULE:$currentRRuleString")
          } else {
            emptyList() // Для удаления правила
          }
    }

    if (selectedUpdateMode == EventUpdateMode.SINGLE_INSTANCE && recurrenceForApiRequest != null) {
      recurrenceForApiRequest = null // Нельзя менять правило для одного экземпляра
    }

    if (!hasChanges) {
      Log.d(TAG, "No actual changes to save after considering all fields.")
      return null
    }

    return EventRequest(
        summary = summaryUpdate,
        description = descriptionUpdate,
        location = locationUpdate,
        startTime = startTimeUpdate,
        endTime = endTimeUpdate,
        isAllDay = isAllDayUpdate,
        timeZoneId = timeZoneIdUpdate,
        recurrence = recurrenceForApiRequest)
  }

  fun requestDeleteConfirmation(event: EventDto) {
    _uiState.update {
      val isActuallyRecurring = event.recurringEventId != null || event.originalStartTime != null
      Log.d(
          TAG,
          "requestDeleteConfirmation for event: ${event.id}, summary: '${event.summary}', isAllDay: ${event.isAllDay}, recurringId: ${event.recurringEventId}, originalStart: ${event.originalStartTime}, calculatedIsRecurring: $isActuallyRecurring")
      it.copy(
          eventPendingDeletion = event,
          showDeleteConfirmationDialog = !isActuallyRecurring,
          showRecurringDeleteOptionsDialog = isActuallyRecurring,
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

  fun confirmDeleteEvent() {
    val eventToDelete = _uiState.value.eventPendingDeletion ?: return

    viewModelScope.launch {
      _uiState.update {
        it.copy(
            isLoading = true,
            showDeleteConfirmationDialog = false,
            eventPendingDeletion = null,
            operationError = null)
      }

      val result = calendarRepository.deleteEvent(eventToDelete.id, EventDeleteMode.DEFAULT)
      _uiState.update { it.copy(isLoading = false) }
      if (result.isSuccess) {
        val message = funMessages.getEventDeletedMessage(eventToDelete.summary)
        _eventFlow.emit(EventManagementUiEvent.ShowMessage(message))
        _eventFlow.emit(EventManagementUiEvent.OperationSuccess)
      } else {
        val errorMessage: UiText =
            result.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
                ?: run { funMessages.getDeleteErrorMessage() }
        _eventFlow.emit(EventManagementUiEvent.ShowMessage(errorMessage))
      }
    }
  }

  fun confirmRecurringDelete(choice: RecurringDeleteChoice) {
    val eventToDelete = _uiState.value.eventPendingDeletion ?: return
    _uiState.update {
      it.copy(
          isLoading = true,
          showDeleteConfirmationDialog = false,
          eventPendingDeletion = null,
          operationError = null)
    }

    when (choice) {
      RecurringDeleteChoice.SINGLE_INSTANCE -> {
        viewModelScope.launch {
          val result =
              calendarRepository.deleteEvent(eventToDelete.id, EventDeleteMode.INSTANCE_ONLY)
          _uiState.update { it.copy(isLoading = false) }

          if (result.isSuccess) {
            val message = funMessages.getSeriesDeletedMessage()
            _eventFlow.emit(EventManagementUiEvent.ShowMessage(message))
            _eventFlow.emit(EventManagementUiEvent.OperationSuccess)
          } else {
            val errorMessage: UiText =
                result.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
                    ?: run { funMessages.getGenericErrorMessage() }
            _eventFlow.emit(EventManagementUiEvent.ShowMessage(errorMessage))
          }
        }
      }

      RecurringDeleteChoice.THIS_AND_FOLLOWING -> {
        handleThisAndFollowingDelete(eventToDelete)
      }

      RecurringDeleteChoice.ALL_IN_SERIES -> {
        val idForBackendCall = eventToDelete.recurringEventId ?: eventToDelete.id
        viewModelScope.launch {
          val result = calendarRepository.deleteEvent(idForBackendCall, EventDeleteMode.DEFAULT)
          _uiState.update { it.copy(isLoading = false) }

          if (result.isSuccess) {
            val message = funMessages.getSeriesDeletedMessage()
            _eventFlow.emit(EventManagementUiEvent.ShowMessage(message))
            _eventFlow.emit(EventManagementUiEvent.OperationSuccess)
          } else {
            val errorMessage: UiText =
                result.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
                    ?: run { funMessages.getGenericErrorMessage() }
            _eventFlow.emit(EventManagementUiEvent.ShowMessage(errorMessage))
          }
        }
      }
    }
  }

  private fun handleThisAndFollowingDelete(eventInstance: EventDto) {
    val originalRRule = eventInstance.recurrenceRule
    if (originalRRule.isNullOrBlank()) {
      Log.e(
          TAG,
          "Cannot perform 'this and following' delete: Event ${eventInstance.id} has no recurrence rule.")
      _uiState.update { it.copy(operationError = funMessages.getGenericErrorMessage()) }
      return
    }

    val masterEventId = eventInstance.recurringEventId ?: eventInstance.id

    val instanceStartDate: LocalDate =
        try {
          OffsetDateTime.parse(eventInstance.startTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
              .toLocalDate()
        } catch (_: DateTimeParseException) {
          try {
            LocalDate.parse(eventInstance.startTime, DateTimeFormatter.ISO_LOCAL_DATE)
          } catch (e2: DateTimeParseException) {
            Log.e(
                TAG,
                "Failed to parse event start time in any known format: ${eventInstance.startTime}",
                e2)
            _uiState.update { it.copy(operationError = funMessages.getGenericErrorMessage()) }
            return
          }
        }

    val newUntilDate = instanceStartDate.minusDays(1)

    val untilString =
        newUntilDate
            .atTime(23, 59, 59)
            .atZone(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))

    val ruleParts =
        originalRRule.split(';').filterNot {
          it.startsWith("UNTIL=", ignoreCase = true) || it.startsWith("COUNT=", ignoreCase = true)
        }
    val newRRuleString = "RRULE:" + ruleParts.joinToString(";") + ";UNTIL=$untilString"

    val updateRequest = EventRequest(recurrence = listOf(newRRuleString))

    Log.d(
        TAG, "Updating master event $masterEventId to stop recurrence. New RRULE: $newRRuleString")

    viewModelScope.launch {
      val result =
          calendarRepository.updateEvent(
              eventId = masterEventId,
              updateData = updateRequest,
              mode = EventUpdateMode.ALL_IN_SERIES)
      _uiState.update { it.copy(isLoading = false) }
      if (result.isSuccess) {
        val message = funMessages.getSeriesDeletedMessage()
        _eventFlow.emit(EventManagementUiEvent.ShowMessage(message))
        _eventFlow.emit(EventManagementUiEvent.OperationSuccess)
      } else {
        val errorMessage: UiText =
            result.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
                ?: run { funMessages.getGenericErrorMessage() }
        _eventFlow.emit(EventManagementUiEvent.ShowMessage(errorMessage))
      }
    }
  }

  fun requestEditEvent(event: EventDto) {
    val isAlreadyRecurring =
        event.recurringEventId != null ||
            event.originalStartTime != null ||
            !event.recurrenceRule.isNullOrEmpty()

    _uiState.update {
      it.copy(
          eventBeingEdited = event,
          showRecurringEditOptionsDialog = isAlreadyRecurring,
          showEditEventDialog = !isAlreadyRecurring,
          selectedUpdateMode =
              if (!isAlreadyRecurring) {
                EventUpdateMode.ALL_IN_SERIES
              } else {
                it.selectedUpdateMode
              },
      )
    }
    Log.d(
        TAG,
        "Requested edit for event ID: ${event.id}, isAlreadyRecurring: $isAlreadyRecurring, initial selectedUpdateMode for form: ${_uiState.value.selectedUpdateMode}")
  }

  fun onRecurringEditOptionSelected(choice: EventUpdateMode) {
    val currentEvent = _uiState.value.eventBeingEdited
    if (currentEvent == null) {
      Log.e(TAG, "onRecurringEditOptionSelected called but eventBeingEdited is null.")
      cancelEditEvent()
      return
    }

    Log.d(
        TAG,
        "Recurring edit mode selected: $choice for event: ${currentEvent.id}. Current RRULE in event: ${currentEvent.recurrenceRule}")

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
    Log.d(TAG, "Event editing cancelled.")
  }

  fun requestEventDetails(event: EventDto) {
    _uiState.update { currentState ->
      currentState.copy(eventForDetailedView = event, showEventDetailedView = true)
    }
    Log.d(TAG, "Requested event details for event ID: ${event.id}")
  }

  fun cancelEventDetails() {
    _uiState.update { currentState ->
      currentState.copy(eventForDetailedView = null, showEventDetailedView = false)
    }
    Log.d(TAG, "Cancelled event details view.")
  }

  companion object {
    private const val TAG = "EventManagementViewModel"
  }
}

data class EventManagementUiState(
    val operationError: UiText? = null,
    val isLoading: Boolean = false,
    val eventToDeleteId: String? = null,
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
