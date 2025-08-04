package com.lpavs.caliinda.feature.event_management.vm

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.core.data.remote.EventDeleteMode
import com.lpavs.caliinda.core.data.remote.EventUpdateMode
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.data.remote.dto.EventRequest
import com.lpavs.caliinda.core.data.repository.CalendarRepository
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
    private val calendarRepository: CalendarRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(EventManagementUiState())
  val state: StateFlow<EventManagementUiState> = _uiState.asStateFlow()

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
      updatedEventData: EventRequest,
      modeFromUi: EventUpdateMode
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
      val result = calendarRepository.updateEvent(
          eventId = originalEvent.id, // ID оригинального события/экземпляра
          updateData = updatedEventData,
          mode = modeFromUi)
        if (result.isSuccess) {
            _uiState.update {
                it.copy(isLoading = false, eventUpdateSuccess = true) // Добавьте eventCreationSuccess
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    operationError = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                )
            }
        }
    }
  }



  fun createEvent(
      request: EventRequest
  ) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
      val result = calendarRepository.createEvent(request)
        if (result.isSuccess) {
            _uiState.update {
                it.copy(isLoading = false, eventCreationSuccess = true) // Добавьте eventCreationSuccess
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    operationError = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                )
            }
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

      viewModelScope.launch {
          _uiState.update {
              it.copy(
                  isLoading = true,
                  showDeleteConfirmationDialog = false,
                  eventPendingDeletion = null,
                  operationError = null
              )
          }

          val result = calendarRepository.deleteEvent(eventToDelete.id, EventDeleteMode.DEFAULT)

          if (result.isSuccess) {
              _uiState.update { it.copy(isLoading = false, userFacingMessage = "Событие удалено") }
          } else {
              _uiState.update {
                  it.copy(
                      isLoading = false,
                      operationError = result.exceptionOrNull()?.message ?: "Ошибка удаления"
                  )
              }
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
            operationError = null
        )
    }

    when (choice) {
      RecurringDeleteChoice.SINGLE_INSTANCE -> {
        viewModelScope.launch {
          val result = calendarRepository.deleteEvent(eventToDelete.id, EventDeleteMode.INSTANCE_ONLY)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, userFacingMessage = "Событие удалено") }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        operationError = result.exceptionOrNull()?.message ?: "Ошибка удаления"
                    )
                }
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
                _uiState.update { it.copy(isLoading = false, userFacingMessage = "Событие удалено") }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        operationError = result.exceptionOrNull()?.message ?: "Ошибка удаления"
                    )
                }
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
      val result = calendarRepository.updateEvent(
          eventId = masterEventId,
          updateData = updateRequest,
          mode = EventUpdateMode.ALL_IN_SERIES)
        if (result.isSuccess) {
            _uiState.update { it.copy(isLoading = false, userFacingMessage = "События удалены") }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    operationError = result.exceptionOrNull()?.message ?: "Ошибка удаления"
                )
            }
        }
    }
  }

  /** Вызывается из UI для сброса флага ошибки удаления после ее показа (например, в Snackbar). */
  fun clearDeleteError() {
    _uiState.update { it.copy(deleteOperationError = null) }
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
          editOperationError = null)
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
          showEditEventDialog = false,
          editOperationError = null)
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
    val deleteOperationError: String? = null,
    val eventBeingEdited: EventDto? = null,
    val showRecurringEditOptionsDialog: Boolean = false,
    val showEditEventDialog: Boolean = false,
    val selectedUpdateMode: EventUpdateMode? = null,
    val editOperationError: String? = null,
    val eventForDetailedView: EventDto? = null,
    val showEventDetailedView: Boolean = false,
    val eventCreationSuccess: Boolean = false,
    val eventUpdateSuccess: Boolean = false,
    val eventDeletionSuccess: Boolean = false
)

sealed class EventManagementUiEvent {
    data class ShowMessage(val message: String) : EventManagementUiEvent()
    object OperationSuccess : EventManagementUiEvent()
}