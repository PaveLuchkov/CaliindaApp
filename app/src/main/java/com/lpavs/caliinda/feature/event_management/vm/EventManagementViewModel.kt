package com.lpavs.caliinda.feature.event_management.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.core.data.remote.EventDeleteMode
import com.lpavs.caliinda.core.data.remote.EventUpdateMode
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.data.remote.dto.EventRequest
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringDeleteChoice
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
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

@HiltViewModel
class EventManagementViewModel
@Inject
constructor(
    settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(EventManagementUiState())
  val uiState: StateFlow<EventManagementUiState> = _uiState.asStateFlow()

  private val _eventFlow = MutableSharedFlow<EventManagementUiEvent>()
  val eventFlow: SharedFlow<EventManagementUiEvent> = _eventFlow.asSharedFlow()

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
  fun confirmEventUpdate(updatedEventData: EventRequest, modeFromUi: EventUpdateMode) {
    val originalEvent = _uiState.value.eventBeingEdited ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, operationError = null) }
      val result =
          calendarRepository.updateEvent(
              eventId = originalEvent.id, updateData = updatedEventData, mode = modeFromUi)
      _uiState.update { it.copy(isLoading = false) }
      if (result.isSuccess) {
        _eventFlow.emit(EventManagementUiEvent.ShowMessage("Событие успешно обновлено")) // TODO R
        _eventFlow.emit(EventManagementUiEvent.OperationSuccess)
      } else {
        val errorMessage = result.exceptionOrNull()?.message ?: "Ошибка обновления"
        _eventFlow.emit(EventManagementUiEvent.ShowMessage(errorMessage))
      }
    }
  }

  fun createEvent(request: EventRequest) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, operationError = null) }

      val result = calendarRepository.createEvent(request)

      _uiState.update { it.copy(isLoading = false) }

      if (result.isSuccess) {
        _eventFlow.emit(EventManagementUiEvent.ShowMessage("Событие успешно создано"))
        _eventFlow.emit(EventManagementUiEvent.OperationSuccess)
      } else {
        val errorMessage = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
        _eventFlow.emit(EventManagementUiEvent.ShowMessage(errorMessage))
      }
    }
  }

  /**
   * Вызывается из UI, когда пользователь инициирует удаление события. Устанавливает ID события и
   * показывает диалог подтверждения.
   */
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

    viewModelScope.launch {
      _uiState.update {
        it.copy(
            isLoading = true,
            showDeleteConfirmationDialog = false,
            eventPendingDeletion = null,
            operationError = null)
      }

      val result = calendarRepository.deleteEvent(eventToDelete.id, EventDeleteMode.DEFAULT)

      if (result.isSuccess) {
        _eventFlow.emit(EventManagementUiEvent.ShowMessage("События успешно удалены")) // TODO
        _eventFlow.emit(EventManagementUiEvent.OperationSuccess)
      } else {
        val errorMessage = result.exceptionOrNull()?.message ?: "Неизвестная ошибка" // TODO
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
          if (result.isSuccess) {
            _eventFlow.emit(EventManagementUiEvent.ShowMessage("Событие успешно удалено")) // TODO
            _eventFlow.emit(EventManagementUiEvent.OperationSuccess)
          } else {
            val errorMessage = result.exceptionOrNull()?.message ?: "Неизвестная ошибка" // TODO
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
          if (result.isSuccess) {
            _eventFlow.emit(EventManagementUiEvent.ShowMessage("События успешно удалены")) // TODO
            _eventFlow.emit(EventManagementUiEvent.OperationSuccess)
          } else {
            val errorMessage = result.exceptionOrNull()?.message ?: "Неизвестная ошибка" // TODO
            _eventFlow.emit(EventManagementUiEvent.ShowMessage(errorMessage))
          }
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
  private fun handleThisAndFollowingDelete(eventInstance: EventDto) {
    val originalRRule = eventInstance.recurrenceRule
    if (originalRRule.isNullOrBlank()) {
      Log.e(
          TAG,
          "Cannot perform 'this and following' delete: Event ${eventInstance.id} has no recurrence rule.")
      _uiState.update {
        it.copy(operationError = "Не удалось обновить серию: отсутствует правило повторения.")
      }
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
            _uiState.update {
              it.copy(operationError = "Ошибка в дате события. Невозможно выполнить операцию.")
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

    val updateRequest = EventRequest(recurrence = listOf(newRRuleString))

    Log.d(
        TAG, "Updating master event $masterEventId to stop recurrence. New RRULE: $newRRuleString")

    viewModelScope.launch {
      val result =
          calendarRepository.updateEvent(
              eventId = masterEventId,
              updateData = updateRequest,
              mode = EventUpdateMode.ALL_IN_SERIES)
      if (result.isSuccess) {
        _eventFlow.emit(EventManagementUiEvent.ShowMessage("События успешно удалены")) // TODO
        _eventFlow.emit(EventManagementUiEvent.OperationSuccess)
      } else {
        val errorMessage = result.exceptionOrNull()?.message ?: "Неизвестная ошибка" // TODO
        _eventFlow.emit(EventManagementUiEvent.ShowMessage(errorMessage))
      }
    }
  }

  /** Вызывается из UI, когда пользователь инициирует редактирование события. */
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

  /** Вызывается из диалога выбора режима редактирования для повторяющихся событий. */
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

  /** Вызывается для отмены процесса редактирования (закрытия диалогов). */
  fun cancelEditEvent() {
    _uiState.update {
      it.copy(
          eventBeingEdited = null,
          showRecurringEditOptionsDialog = false,
          showEditEventDialog = false)
    }
    Log.d(TAG, "Event editing cancelled.")
  }

  /**
   * Вызывается из UI, когда пользователь хочет посмотреть детали события. Устанавливает событие для
   * просмотра и флаг для отображения UI.
   */
  fun requestEventDetails(event: EventDto) {
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
    val operationError: String? = null,
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
  data class ShowMessage(val message: String) : EventManagementUiEvent()

  object OperationSuccess : EventManagementUiEvent()
}
