package com.lpavs.caliinda.feature.event_management.vm

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.data.calendar.ApiDeleteEventMode
import com.lpavs.caliinda.data.calendar.CalendarDataManager
import com.lpavs.caliinda.data.calendar.ClientEventUpdateMode
import com.lpavs.caliinda.data.calendar.CreateEventResult
import com.lpavs.caliinda.data.calendar.DeleteEventResult
import com.lpavs.caliinda.data.calendar.UpdateEventResult
import com.lpavs.caliinda.data.local.UpdateEventApiRequest
import com.lpavs.caliinda.feature.calendar.data.model.CalendarEvent
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringDeleteChoice
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

@HiltViewModel
class EventManagementViewModel
@Inject
constructor(
    settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
    private val calendarDataManager: CalendarDataManager,
) : ViewModel() {
  private val _uiState = MutableStateFlow(EventManagementUiState())
  val state: StateFlow<EventManagementUiState> = _uiState.asStateFlow()

  init {
    observeCreateEventResult()
    observeDeleteEventResult()
    observeUpdateEventResult()
  }

  private fun calculateIsLoading(
      createEventState: CreateEventResult = calendarDataManager.createEventResult.value,
      deleteEventState: DeleteEventResult = calendarDataManager.deleteEventResult.value,
      updateEventState: UpdateEventResult = calendarDataManager.updateEventResult.value
  ): Boolean {
    val creatingEvent = createEventState is CreateEventResult.Loading
    val deletingEvent = deleteEventState is DeleteEventResult.Loading
    val updatingEvent = updateEventState is UpdateEventResult.Loading

    return creatingEvent || deletingEvent || updatingEvent
  }

  private val eventCreatedMessage: String = context.getString(R.string.event_created)

  private fun observeCreateEventResult() {
    viewModelScope.launch {
      calendarDataManager.createEventResult.collect { result ->
        _uiState.update { currentUiState ->
          val nextMessage =
              when (result) {
                is CreateEventResult.Success -> "Событие успешно создано"
                is CreateEventResult.Error -> result.message
                is CreateEventResult.Idle -> {
                  val prevMsg = currentUiState.message
                  if (prevMsg == eventCreatedMessage ||
                      prevMsg?.contains(context.getString(R.string.error)) == true) {
                    null
                  } else {
                    prevMsg
                  }
                }
                is CreateEventResult.Loading -> currentUiState.message
              }
          currentUiState.copy(
              isLoading = calculateIsLoading(createEventState = result), message = nextMessage)
        }
      }
    }
  }

  private fun observeDeleteEventResult() {
    viewModelScope.launch {
      calendarDataManager.deleteEventResult.collect { result ->
        _uiState.update { currentState ->
          val updatedState =
              when (result) {
                is DeleteEventResult.Success -> {
                  Log.i(TAG, "Event deletion successful (observed in VM).")
                  currentState.copy(
                      eventToDeleteId = null,
                      deleteOperationError = null,
                  )
                }
                is DeleteEventResult.Error -> {
                  Log.e(TAG, "Event deletion failed (observed in VM): ${result.message}")
                  currentState.copy(
                      eventToDeleteId = null,
                      deleteOperationError = result.message,
                      showDeleteConfirmationDialog = false)
                }
                is DeleteEventResult.Loading -> {
                  currentState.copy(deleteOperationError = null)
                }
                is DeleteEventResult.Idle -> {
                  if (currentState.deleteOperationError != null) {
                    currentState.copy(deleteOperationError = null)
                  } else {
                    currentState
                  }
                }
              }
          updatedState.copy(isLoading = calculateIsLoading())
        }
        if (result is DeleteEventResult.Success || result is DeleteEventResult.Error) {
          calendarDataManager.consumeDeleteEventResult()
        }
      }
    }
  }

  private fun observeUpdateEventResult() {
    viewModelScope.launch {
      calendarDataManager.updateEventResult.collect { result ->
        _uiState.update { currentState ->
          val updatedState =
              when (result) {
                is UpdateEventResult.Success -> {
                  Log.i(
                      TAG,
                      "Event update successful (observed in VM). Event ID: ${result.updatedEventId}")
                  currentState.copy(
                      eventBeingEdited = null,
                      showEditEventDialog = false,
                      editOperationError = null,
                      message = "Событие успешно обновлено")
                }

                is UpdateEventResult.Error -> {
                  Log.e(TAG, "Event update failed (observed in VM): ${result.message}")
                  currentState.copy(editOperationError = result.message)
                }

                is UpdateEventResult.Loading -> {
                  currentState.copy(editOperationError = null)
                }

                is UpdateEventResult.Idle -> {
                  if (currentState.editOperationError != null ||
                      currentState.message == "Событие успешно обновлено") {
                    currentState.copy(editOperationError = null, message = null)
                  } else {
                    currentState
                  }
                }
              }
          updatedState.copy(isLoading = calculateIsLoading())
        }

        if (result is UpdateEventResult.Success || result is UpdateEventResult.Error) {
          calendarDataManager.consumeUpdateEventResult()
        }
      }
    }
  }

  val createEventResult: StateFlow<CreateEventResult> = calendarDataManager.createEventResult
  val updateEventResult: StateFlow<UpdateEventResult> = calendarDataManager.updateEventResult

  val timeZone: StateFlow<String> =
      settingsRepository.timeZoneFlow.stateIn(
          viewModelScope, SharingStarted.WhileSubscribed(5000), ZoneId.systemDefault().id)

  /**
   * Вызывается из UI (формы/диалога редактирования) для сохранения изменений.
   *
   * @param updatedEventData Данные, введенные пользователем в форме.
   * @param modeFromUi Режим обновления (особенно важен для повторяющихся, выбирается заранее). Если
   *   событие одиночное, mode обычно SINGLE_INSTANCE или можно передать специальное значение,
   *   которое бэкенд поймет как "не повторяющееся". Но так как update_mode на бэке обязательный,
   *   всегда передаем режим.
   */
  fun confirmEventUpdate(
      updatedEventData: UpdateEventApiRequest,
      modeFromUi: ClientEventUpdateMode
  ) {
    val originalEvent = _uiState.value.eventBeingEdited
    if (originalEvent == null) {
      Log.e(TAG, "confirmEventUpdate called but eventBeingEdited is null.")
      _uiState.update { it.copy(editOperationError = "Ошибка: нет данных для редактирования.") }
      return
    }

    Log.d(
        TAG,
        "Confirming update for event ID: ${originalEvent.id}, mode: $modeFromUi, data: $updatedEventData")

    viewModelScope.launch {
      calendarDataManager.updateEvent(
          eventId = originalEvent.id, // ID оригинального события/экземпляра
          updateData = updatedEventData,
          mode = modeFromUi)
    }
  }

  fun consumeUpdateEventResult() {
    calendarDataManager.consumeUpdateEventResult()
  }

  fun createEvent(
      summary: String,
      startTimeString: String,
      endTimeString: String,
      isAllDay: Boolean,
      description: String?,
      timeZoneId: String?,
      location: String?,
      recurrenceRule: String?
  ) {
    viewModelScope.launch {
      calendarDataManager.createEvent(
          summary,
          startTimeString,
          endTimeString,
          isAllDay,
          timeZoneId = timeZoneId,
          description,
          location,
          recurrenceRule = recurrenceRule)
    }
  }

  fun consumeCreateEventResult() = calendarDataManager.consumeCreateEventResult()

  /**
   * Вызывается из UI, когда пользователь инициирует удаление события. Устанавливает ID события и
   * показывает диалог подтверждения.
   */
  fun requestDeleteConfirmation(event: CalendarEvent) {
    _uiState.update {
      val isActuallyRecurring = event.recurringEventId != null || event.originalStartTime != null
      Log.d(
          TAG,
          "requestDeleteConfirmation for event: ${event.id}, summary: '${event.summary}', isAllDay: ${event.isAllDay}, recurringId: ${event.recurringEventId}, originalStart: ${event.originalStartTime}, calculatedIsRecurring: $isActuallyRecurring")

      it.copy(
          eventPendingDeletion = event,
          showDeleteConfirmationDialog =
              !isActuallyRecurring, // Показываем простой диалог, если НЕ повторяющееся
          showRecurringDeleteOptionsDialog = isActuallyRecurring,
          deleteOperationError = null)
    }
  }

  /** Вызывается из UI, когда пользователь отменяет удаление в диалоге. */
  fun cancelDelete() {
    _uiState.update {
      it.copy(
          eventPendingDeletion = null,
          showDeleteConfirmationDialog = false,
          showRecurringDeleteOptionsDialog = false)
    }
  }

  /**
   * Вызывается из UI, когда пользователь подтверждает удаление в диалоге. Запускает процесс
   * удаления через DataManager.
   */
  fun confirmDeleteEvent() {
    val eventToDelete = _uiState.value.eventPendingDeletion ?: return
    _uiState.update { it.copy(showDeleteConfirmationDialog = false, eventPendingDeletion = null) }
    viewModelScope.launch {
      calendarDataManager.deleteEvent(eventToDelete.id, ApiDeleteEventMode.DEFAULT)
    }
  }

  fun confirmRecurringDelete(choice: RecurringDeleteChoice) {
    val eventToDelete = _uiState.value.eventPendingDeletion ?: return
    _uiState.update {
      it.copy(showRecurringDeleteOptionsDialog = false, eventPendingDeletion = null)
    }

    when (choice) {
      RecurringDeleteChoice.SINGLE_INSTANCE -> {
        viewModelScope.launch {
          calendarDataManager.deleteEvent(eventToDelete.id, ApiDeleteEventMode.INSTANCE_ONLY)
        }
      }

      RecurringDeleteChoice.THIS_AND_FOLLOWING -> {
        handleThisAndFollowingDelete(eventToDelete)
      }

      RecurringDeleteChoice.ALL_IN_SERIES -> {
        val idForBackendCall = eventToDelete.recurringEventId ?: eventToDelete.id
        viewModelScope.launch {
          calendarDataManager.deleteEvent(idForBackendCall, ApiDeleteEventMode.DEFAULT)
        }
      }
    }
  }

  /**
   * Обрабатывает удаление текущего и последующих событий. Это делается путем обновления
   * мастер-события: в его правило повторения (RRULE) добавляется дата окончания (UNTIL),
   * установленная на день до удаляемого экземпляра.
   *
   * @param eventInstance Экземпляр события, с которого начинается удаление.
   */
  private fun handleThisAndFollowingDelete(eventInstance: CalendarEvent) {
    val originalRRule = eventInstance.recurrenceRule
    if (originalRRule.isNullOrBlank()) {
      Log.e(
          TAG,
          "Cannot perform 'this and following' delete: Event ${eventInstance.id} has no recurrence rule.")
      _uiState.update {
        it.copy(showGeneralError = "Не удалось обновить серию: отсутствует правило повторения.")
      }
      return
    }

    val masterEventId = eventInstance.recurringEventId ?: eventInstance.id

    val instanceStartDate: LocalDate =
        try {
          OffsetDateTime.parse(eventInstance.startTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
              .toLocalDate()
        } catch (e: DateTimeParseException) {
          try {
            LocalDate.parse(eventInstance.startTime, DateTimeFormatter.ISO_LOCAL_DATE)
          } catch (e2: DateTimeParseException) {
            Log.e(
                TAG,
                "Failed to parse event start time in any known format: ${eventInstance.startTime}",
                e2)
            _uiState.update {
              it.copy(showGeneralError = "Ошибка в дате события. Невозможно выполнить операцию.")
            }
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

    val updateRequest = UpdateEventApiRequest(recurrence = listOf(newRRuleString))

    Log.d(
        TAG, "Updating master event $masterEventId to stop recurrence. New RRULE: $newRRuleString")

    viewModelScope.launch {
      calendarDataManager.updateEvent(
          eventId = masterEventId,
          updateData = updateRequest,
          mode = ClientEventUpdateMode.ALL_IN_SERIES)
    }
  }

  /** Вызывается из UI для сброса флага ошибки удаления после ее показа (например, в Snackbar). */
  fun clearDeleteError() {
    _uiState.update { it.copy(deleteOperationError = null) }
  }

  fun clearGeneralError() {
    _uiState.update { it.copy(showGeneralError = null) }
    calendarDataManager.clearNetworkError() // Опционально
  }

  /** Вызывается из UI, когда пользователь инициирует редактирование события. */
  fun requestEditEvent(event: CalendarEvent) {
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
                ClientEventUpdateMode.ALL_IN_SERIES
              } else {
                it.selectedUpdateMode
              },
          editOperationError = null)
    }
    Log.d(
        TAG,
        "Requested edit for event ID: ${event.id}, isAlreadyRecurring: $isAlreadyRecurring, initial selectedUpdateMode for form: ${_uiState.value.selectedUpdateMode}")
  }

  /** Вызывается из диалога выбора режима редактирования для повторяющихся событий. */
  fun onRecurringEditOptionSelected(choice: ClientEventUpdateMode) {
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

  /** Вызывается для отмены процесса редактирования (закрытия диалогов). */
  fun cancelEditEvent() {
    _uiState.update {
      it.copy(
          eventBeingEdited = null,
          showRecurringEditOptionsDialog = false,
          showEditEventDialog = false,
          editOperationError = null)
    }
    Log.d(TAG, "Event editing cancelled.")
  }

  /**
   * Вызывается из UI, когда пользователь хочет посмотреть детали события. Устанавливает событие для
   * просмотра и флаг для отображения UI.
   */
  fun requestEventDetails(event: CalendarEvent) {
    _uiState.update { currentState ->
      currentState.copy(eventForDetailedView = event, showEventDetailedView = true)
    }
    Log.d(TAG, "Requested event details for event ID: ${event.id}")
  }

  /**
   * Вызывается из UI, когда пользователь закрывает детальный просмотр события. Сбрасывает событие и
   * флаг.
   */
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
    val showGeneralError: String? = null,
    val message: String? = "Log in required",
    val isLoading: Boolean = false,
    val eventToDeleteId: String? = null,
    val eventPendingDeletion: CalendarEvent? = null,
    val showDeleteConfirmationDialog: Boolean = false,
    val showRecurringDeleteOptionsDialog: Boolean = false,
    val deleteOperationError: String? = null,
    val eventBeingEdited: CalendarEvent? = null,
    val showRecurringEditOptionsDialog: Boolean = false,
    val showEditEventDialog: Boolean = false,
    val selectedUpdateMode: ClientEventUpdateMode? = null,
    val editOperationError: String? = null,
    val eventForDetailedView: CalendarEvent? = null,
    val showEventDetailedView: Boolean = false,
)
