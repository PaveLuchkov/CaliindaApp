package com.lpavs.caliinda.feature.event_management.vm

import com.lpavs.caliinda.feature.widget.WidgetRefresher
import kotlinx.coroutines.delay
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import com.lpavs.caliinda.core.data.repository.PendingDeletions
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
import com.lpavs.caliinda.feature.calendar.presentation.components.IFunMessages
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringDeleteChoice
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimeState
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.recurrenceRuleAfterEdit
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.toDateTimeState
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
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class EventManagementViewModel
@Inject
constructor(
    settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
    private val funMessages: IFunMessages,
    private val pendingDeletions: PendingDeletions,
    private val widgetRefresher: WidgetRefresher,
) : ViewModel() {
  private val _uiState = MutableStateFlow(EventManagementUiState())
  val uiState: StateFlow<EventManagementUiState> = _uiState.asStateFlow()

  private val _eventFlow = MutableSharedFlow<EventManagementUiEvent>()
  val eventFlow: SharedFlow<EventManagementUiEvent> = _eventFlow.asSharedFlow()

  // Eagerly: пояс читается через .value при сохранении, а подписчиков в UI у него нет — с
  // WhileSubscribed здесь навсегда оставался бы системный пояс вместо выбранного в настройках.
  val timeZone: StateFlow<ZoneId> =
      settingsRepository.zoneFlow.stateIn(
          viewModelScope, SharingStarted.Eagerly, ZoneId.systemDefault())

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
      val zone = timeZone.value
      val draft =
          buildDraft(
              summary, description, location, dateTimeState, dateTimeState.toRrule(zone)?.format())
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

      val zone = timeZone.value
      val originalState = originalEvent.toDateTimeState(zone)
      val recurrenceRule = recurrenceRuleAfterEdit(originalEvent, dateTimeState, zone)

      val draft = buildDraft(summary, description, location, dateTimeState, recurrenceRule)
      val unchangedDraft =
          buildDraft(
              if (originalEvent.isUntitled) "" else originalEvent.summary,
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

  /** Удалённые, но ещё не стёртые из календаря события: id экземпляра → (событие, таймер). */
  private val undoableDeletes = mutableMapOf<String, Pair<EventDto, Job>>()

  /**
   * Обычное событие удаляем без диалога: сразу прячем и даём отменить в снекбаре. Стираем из
   * календаря, когда снекбар закрылся, или по таймеру — если снекбар так и не показали.
   */
  private fun deleteWithUndo(event: EventDto) {
    if (event.id in undoableDeletes) return
    pendingDeletions.add(event.id)
    val timer =
        viewModelScope.launch {
          delay(UNDO_FALLBACK_MS)
          commitDelete(event.id)
        }
    undoableDeletes[event.id] = event to timer
    viewModelScope.launch {
      _eventFlow.emit(
          EventManagementUiEvent.ShowUndoDelete(
              event.id, UiText.from(R.string.event_deleted, event.summary)))
    }
  }

  fun undoDelete(id: String) {
    val (_, timer) = undoableDeletes.remove(id) ?: return
    timer.cancel()
    pendingDeletions.remove(id)
  }

  fun commitDelete(id: String) {
    val (event, timer) = undoableDeletes.remove(id) ?: return
    timer.cancel()
    viewModelScope.launch { deleteFromCalendar(event) }
  }

  private suspend fun deleteFromCalendar(event: EventDto) {
    val result = calendarRepository.deleteEvent(event, EventDeleteMode.DEFAULT)
    // Снимаем скрытие после записи: провайдер уже без события, и карточка не мигнёт обратно.
    pendingDeletions.remove(event.id)
    if (result.isSuccess) widgetRefresher.refresh()
    if (result.isFailure) {
      _eventFlow.emit(EventManagementUiEvent.ShowMessage(funMessages.getDeleteErrorMessage()))
    }
  }

  override fun onCleared() {
    // Экран ушёл, пока снекбар висел: отмены уже не будет — удаляем, а не теряем действие.
    val leftovers = undoableDeletes.values.map { it.first }
    undoableDeletes.clear()
    if (leftovers.isNotEmpty()) {
      // Своя область: viewModelScope к этому моменту уже отменён.
      CoroutineScope(SupervisorJob()).launch {
        leftovers.forEach { deleteFromCalendar(it) }
      }
    }
  }

  fun confirmRecurringDelete(choice: RecurringDeleteChoice) {
    val eventToDelete = _uiState.value.eventPendingDeletion ?: return
    _uiState.update {
      it.copy(
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
      widgetRefresher.refresh()
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
          zone = timeZone.value,
          recurrenceRule = recurrenceRule)

  private fun validateInput(summary: String, state: EventDateTimeState): Boolean =
      summary.isNotBlank() && state.validationError == null

  fun parseEventToState(event: EventDto): EventDateTimeState = event.toDateTimeState(timeZone.value)

  // --- Состояние диалогов ---

  fun requestDelete(event: EventDto) {
    if (event.recurringEventId == null) {
      deleteWithUndo(event)
      return
    }
    // У повторяющегося нужно выбрать, что именно удалять — тут без диалога не обойтись.
    _uiState.update {
      it.copy(eventPendingDeletion = event, showRecurringDeleteOptionsDialog = true)
    }
  }

  fun cancelDelete() {
    _uiState.update {
      it.copy(eventPendingDeletion = null, showRecurringDeleteOptionsDialog = false)
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

  companion object {
    private const val TAG = "EventManagementViewModel"
    /** Запасной таймер окна отмены; обычно удаление запускает закрытие снекбара раньше. */
    private const val UNDO_FALLBACK_MS = 15_000L
  }
}

data class EventManagementUiState(
    val isLoading: Boolean = false,
    val eventPendingDeletion: EventDto? = null,
    val showRecurringDeleteOptionsDialog: Boolean = false,
    val eventBeingEdited: EventDto? = null,
    val showRecurringEditOptionsDialog: Boolean = false,
    val showEditEventDialog: Boolean = false,
    val selectedUpdateMode: EventUpdateMode? = null,
)

sealed class EventManagementUiEvent {
  data class ShowMessage(val message: UiText) : EventManagementUiEvent()

  data class ShowUndoDelete(val eventId: String, val message: UiText) : EventManagementUiEvent()

  object OperationSuccess : EventManagementUiEvent()
}
