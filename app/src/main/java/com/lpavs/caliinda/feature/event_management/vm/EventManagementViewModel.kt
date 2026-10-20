package com.lpavs.caliinda.feature.event_management.vm

import com.lpavs.caliinda.feature.event_management.ui.shared.sections.minutes
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.ReminderDefaults
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.ReminderChoice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import com.lpavs.caliinda.feature.live.LiveEventNotifier
import com.lpavs.caliinda.feature.widget.WidgetRefresher
import kotlinx.coroutines.delay
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import com.lpavs.caliinda.core.data.repository.DeletionCountdown
import com.lpavs.caliinda.core.data.repository.PendingDeletions
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.DeviceCalendar
import com.lpavs.caliinda.core.data.calendar.model.EventDeleteMode
import com.lpavs.caliinda.core.data.calendar.model.EventDraft
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.calendar.model.EventUpdateMode
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.core.data.utils.UiText
import com.lpavs.caliinda.feature.event_management.messages.IFunMessages
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringDeleteChoice
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimeState
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.recurrenceRuleAfterEdit
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.toDateTimeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class EventManagementViewModel
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
    private val funMessages: IFunMessages,
    private val pendingDeletions: PendingDeletions,
    private val widgetRefresher: WidgetRefresher,
    private val liveEventNotifier: LiveEventNotifier,
    private val undoWindow: UndoWindow,
) : ViewModel() {
  private val _uiState = MutableStateFlow(EventManagementUiState())
  val uiState: StateFlow<EventManagementUiState> = _uiState.asStateFlow()

  // Channel, а не SharedFlow: сообщение не теряется, пока экран не слушает, и отправитель не
  // ждёт, пока получатель покажет предыдущий снекбар.
  private val _events = Channel<EventManagementUiEvent>(Channel.BUFFERED)
  val events: Flow<EventManagementUiEvent> = _events.receiveAsFlow()

  // Eagerly: пояс читается через .value при сохранении, а подписчиков в UI у него нет — с
  // WhileSubscribed здесь навсегда оставался бы системный пояс вместо выбранного в настройках.
  val timeZone: StateFlow<ZoneId> =
      settingsRepository.zoneFlow.stateIn(
          viewModelScope, SharingStarted.Eagerly, ZoneId.systemDefault())

  /** Напоминания по умолчанию — форма показывает их, пока пользователь не выбрал своё. */
  val reminderDefaults: StateFlow<ReminderDefaults> =
      combine(settingsRepository.defaultReminderFlow, settingsRepository.defaultAllDayReminderFlow) {
              timed,
              allDay ->
            ReminderDefaults(timed, allDay)
          }
          .stateIn(
              viewModelScope,
              SharingStarted.Eagerly,
              ReminderDefaults(
                  SettingsRepository.DEFAULT_REMINDER, SettingsRepository.DEFAULT_ALL_DAY_REMINDER))

  /** Куда можно сохранить новое событие — только показанные фильтром календари. */
  val writableCalendars: StateFlow<List<DeviceCalendar>> =
      calendarRepository
          .getWritableCalendars()
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  /** Календарь по умолчанию — с него форма создания начинает. */
  val defaultCalendarId: StateFlow<Long?> =
      calendarRepository
          .getDefaultCalendarId()
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  // --- Создание ---

  fun createEvent(
      summary: String,
      description: String,
      location: String,
      dateTimeState: EventDateTimeState,
      reminder: ReminderChoice = ReminderChoice.Default,
      calendarId: Long? = null,
  ) {
    viewModelScope.launch {
      if (!validateInput(summary, dateTimeState)) {
        _events.send(
            EventManagementUiEvent.ShowMessage(UiText.from(R.string.error_check_input_data)))
        return@launch
      }
      val zone = timeZone.value
      val defaults =
          ReminderDefaults(
              settingsRepository.defaultReminderFlow.first(),
              settingsRepository.defaultAllDayReminderFlow.first())
      val draft =
          buildDraft(
                  summary, description, location, dateTimeState, dateTimeState.toRrule(zone)?.format())
              .copy(
                  reminders = listOfNotNull(reminder.minutes(defaults, dateTimeState.isAllDay)),
                  calendarId = calendarId)
      runOperation(
          operation = { calendarRepository.createEvent(draft) },
          onSuccess = { id ->
            // Без названия: фраза встаёт в саму карточку, где оно и так крупно.
            celebrate(setOf(id), draft.startDate, funMessages.getEventCreatedMessage(null))
          },
          errorMessage = { funMessages.getCreateErrorMessage() })
    }
  }

  // --- Редактирование ---

  fun updateEvent(
      summary: String,
      description: String,
      location: String,
      dateTimeState: EventDateTimeState,
      updateMode: EventUpdateMode,
      reminder: ReminderChoice = ReminderChoice.Default,
  ) {
    viewModelScope.launch {
      val originalEvent = (uiState.value.dialog as? EventDialog.Editing)?.event
      if (originalEvent == null) {
        Log.e(TAG, "updateEvent called without an event being edited")
        return@launch
      }
      if (!validateInput(summary, dateTimeState)) {
        _events.send(
            EventManagementUiEvent.ShowMessage(UiText.from(R.string.error_check_input_data)))
        return@launch
      }

      val zone = timeZone.value
      val originalState = originalEvent.toDateTimeState(zone)
      val recurrenceRule = recurrenceRuleAfterEdit(originalEvent, dateTimeState, zone)

      // Напоминание трогаем, только если выбрали другое: у события их может быть несколько,
      // а форма показывает первое — без изменений остальные не теряются.
      val original = uiState.value.editingReminders
      val reminders =
          when (reminder) {
            ReminderChoice.Default -> null
            is ReminderChoice.Custom ->
                if (original != null && reminder.minutes == original.firstOrNull()) null
                else listOfNotNull(reminder.minutes)
          }
      val draft =
          buildDraft(summary, description, location, dateTimeState, recurrenceRule)
              .copy(reminders = reminders)
      val unchangedDraft =
          buildDraft(
              if (originalEvent.isUntitled) "" else originalEvent.summary,
              originalEvent.description.orEmpty(),
              originalEvent.location.orEmpty(),
              originalState,
              originalEvent.recurrenceRule)
      if (draft == unchangedDraft) {
        _events.send(
            EventManagementUiEvent.ShowMessage(UiText.from(R.string.no_changes_to_save)))
        dismissDialog()
        return@launch
      }

      runOperation(
          operation = { calendarRepository.updateEvent(originalEvent, draft, updateMode) },
          onSuccess = {
            // Правка экземпляра даёт новую запись-исключение — её узнаём по ссылке на серию.
            val ids =
                setOfNotNull(
                    originalEvent.eventId,
                    originalEvent.originalEventId,
                    originalEvent.recurringEventId?.toLongOrNull())
            celebrate(ids, draft.startDate, funMessages.getEventUpdatedMessage(null))
          },
          errorMessage = { funMessages.getUpdateErrorMessage() })
    }
  }

  // --- Удаление ---

  /** Удаления в обратном отсчёте: id экземпляра → (событие, как удалять, таймер). */
  private val undoableDeletes = mutableMapOf<String, Triple<EventDto, EventDeleteMode, Job>>()

  /**
   * Удаление с отменой: на месте карточки — призрак с отсчётом и «Отменить» (см. PendingDeletions).
   * Время вышло — карточка уходит из списка, и только тогда событие стирается из календаря.
   */
  private fun deleteWithUndo(event: EventDto, mode: EventDeleteMode = EventDeleteMode.DEFAULT) {
    if (event.id in undoableDeletes) return
    val window = undoWindow.millis()
    pendingDeletions.startCountdown(
        event.id,
        DeletionCountdown(
            // Без названия: оно и так крупно в призраке — в фразе вышло бы дважды.
            message = funMessages.getEventDeletedMessage(null),
            startedAtMillis = SystemClock.uptimeMillis(),
            durationMillis = window))
    val timer =
        viewModelScope.launch {
          delay(window)
          undoableDeletes.remove(event.id)
          pendingDeletions.hide(event.id)
          deleteFromCalendar(event, mode)
        }
    undoableDeletes[event.id] = Triple(event, mode, timer)
  }

  fun undoDelete(id: String) {
    val (_, _, timer) = undoableDeletes.remove(id) ?: return
    timer.cancel()
    pendingDeletions.cancel(id)
  }

  private suspend fun deleteFromCalendar(event: EventDto, mode: EventDeleteMode) {
    val result = calendarRepository.deleteEvent(event, mode)
    // Снимаем скрытие после записи: провайдер уже без события, и карточка не мигнёт обратно.
    pendingDeletions.release(event.id)
    if (result.isSuccess) refreshSurfaces()
    if (result.isFailure) {
      _events.send(EventManagementUiEvent.ShowMessage(funMessages.getDeleteErrorMessage()))
    }
  }

  override fun onCleared() {
    // Экран ушёл, пока шёл отсчёт: отмены уже не будет — удаляем, а не теряем действие.
    val leftovers = undoableDeletes.values.map { (event, mode, _) -> event to mode }
    undoableDeletes.clear()
    leftovers.forEach { (event, _) -> pendingDeletions.hide(event.id) }
    if (leftovers.isNotEmpty()) {
      // Своя область: viewModelScope к этому моменту уже отменён.
      CoroutineScope(SupervisorJob()).launch {
        leftovers.forEach { (event, mode) -> deleteFromCalendar(event, mode) }
      }
    }
  }

  fun confirmRecurringDelete(choice: RecurringDeleteChoice) {
    val eventToDelete = (_uiState.value.dialog as? EventDialog.ChooseDeleteMode)?.event ?: return
    dismissDialog()
    val mode =
        when (choice) {
          RecurringDeleteChoice.SINGLE_INSTANCE -> EventDeleteMode.INSTANCE_ONLY
          RecurringDeleteChoice.THIS_AND_FOLLOWING -> EventDeleteMode.THIS_AND_FOLLOWING
          RecurringDeleteChoice.ALL_IN_SERIES -> EventDeleteMode.ALL_IN_SERIES
        }
    // Один экземпляр — тот же призрак с отменой. «Это и следующие» и «вся серия» уже
    // подтверждены в диалоге — удаляем сразу.
    if (mode == EventDeleteMode.INSTANCE_ONLY) {
      deleteWithUndo(eventToDelete, mode)
      return
    }
    viewModelScope.launch {
      runOperation(
          operation = { calendarRepository.deleteEvent(eventToDelete, mode) },
          onSuccess = {
            _events.send(EventManagementUiEvent.ShowMessage(funMessages.getSeriesDeletedMessage()))
          },
          errorMessage = { funMessages.getDeleteErrorMessage() })
    }
  }

  private suspend fun <T> runOperation(
      operation: suspend () -> Result<T>,
      onSuccess: suspend (T) -> Unit,
      errorMessage: () -> UiText
  ) {
    _uiState.update { it.copy(isLoading = true) }
    val result = operation()
    _uiState.update { it.copy(isLoading = false) }
    result
        .onSuccess { value ->
          refreshSurfaces()
          dismissDialog()
          onSuccess(value)
        }
        .onFailure { _events.send(EventManagementUiEvent.ShowMessage(errorMessage())) }
  }

  // --- «Рождение» сохранённой карточки ---

  private val _celebration = MutableStateFlow<Celebration?>(null)
  /** Только что созданное или изменённое событие: его карточка «рождается» (см. Celebration). */
  val celebration: StateFlow<Celebration?> = _celebration.asStateFlow()
  private var celebrationJob: Job? = null

  /** Сохранили — карточка этого события играет «рождение» с фразой, потом всё как обычно. */
  private fun celebrate(eventIds: Set<Long>, date: LocalDate, message: UiText) {
    val celebration = Celebration(eventIds, date, message, SystemClock.uptimeMillis())
    _celebration.value = celebration
    celebrationJob?.cancel()
    celebrationJob =
        viewModelScope.launch {
          delay(Celebration.DURATION_MS)
          _celebration.update { if (it == celebration) null else it }
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

  // --- Состояние диалогов ---

  fun openCreate(date: LocalDate, asProject: Boolean) {
    // Напоминания прошлой правки или «Ещё раз» здесь лишние: иначе новое событие унаследует их
    // (у all-day из Google «накануне в 17:00» = 420 мин — в событии со временем «за 7 часов»).
    _uiState.update {
      it.copy(dialog = EventDialog.Creating(date, asProject), editingReminders = null)
    }
  }

  /**
   * «Ещё раз»: новое событие как [event], но на [date]. Напоминания подтягиваем так же, как при
   * правке, — в той же [EventUiState.editingReminders].
   */
  fun openRepeat(event: EventDto, date: LocalDate) {
    val creating = EventDialog.Creating(date, asProject = false, template = event)
    _uiState.update { it.copy(dialog = creating, editingReminders = null) }
    viewModelScope.launch {
      val reminders = runCatching { calendarRepository.remindersOf(event) }.getOrNull()
      _uiState.update { if (it.dialog == creating) it.copy(editingReminders = reminders) else it }
    }
  }

  fun requestDelete(event: EventDto) {
    if (event.recurringEventId == null) {
      deleteWithUndo(event)
      return
    }
    // У повторяющегося нужно выбрать, что именно удалять — тут без диалога не обойтись.
    _uiState.update { it.copy(dialog = EventDialog.ChooseDeleteMode(event)) }
  }

  fun requestEditEvent(event: EventDto) {
    val dialog =
        if (event.recurringEventId != null) EventDialog.ChooseEditMode(event)
        else EventDialog.Editing(event, EventUpdateMode.ALL_IN_SERIES)
    if (dialog is EventDialog.Editing) startEditing(dialog)
    else _uiState.update { it.copy(dialog = dialog) }
  }

  /** Открыть правку и подтянуть напоминания события — форма покажет текущее. */
  private fun startEditing(editing: EventDialog.Editing) {
    _uiState.update { it.copy(dialog = editing, editingReminders = null) }
    viewModelScope.launch {
      val reminders = runCatching { calendarRepository.remindersOf(editing.event) }.getOrNull()
      _uiState.update { if (it.dialog == editing) it.copy(editingReminders = reminders) else it }
    }
  }

  fun onRecurringEditOptionSelected(choice: EventUpdateMode) {
    val event = (_uiState.value.dialog as? EventDialog.ChooseEditMode)?.event ?: return
    // Перенесённый экземпляр (исключение) своего RRULE не хранит — при правке серии форма
    // должна показать правило головного события, а не «не повторяется».
    if (choice == EventUpdateMode.SINGLE_INSTANCE || event.recurrenceRule != null) {
      startEditing(EventDialog.Editing(event, choice))
      return
    }
    viewModelScope.launch {
      val withRule = event.copy(recurrenceRule = calendarRepository.seriesRule(event))
      startEditing(EventDialog.Editing(withRule, choice))
    }
  }

  /** Закрыть шторку или диалог: отмена пользователем или успешное сохранение. */
  fun dismissDialog() {
    _uiState.update { it.copy(dialog = EventDialog.None) }
  }

  /** Виджет и live-уведомление сами не узнают о правке из приложения — будим их. */
  private fun refreshSurfaces() {
    widgetRefresher.refresh()
    liveEventNotifier.refreshAsync()
  }

  /** Без названия — общая фраза вместо «Событие '' обновлено». */
  private fun EventDto.nameForMessage(): String? = summary.takeUnless { isUntitled }

  companion object {
    private const val TAG = "EventManagementViewModel"
  }
}

data class EventManagementUiState(
    val isLoading: Boolean = false,
    val dialog: EventDialog = EventDialog.None,
    /** Напоминания события в шторке правки; null — ещё не загружены. */
    val editingReminders: List<Int>? = null,
)

/** Что сейчас открыто поверх календаря. Одно значение вместо набора флагов. */
sealed interface EventDialog {
  data object None : EventDialog

  /** Шторка создания; [template] — «Ещё раз»: форма заполнена как у этого события. */
  data class Creating(
      val date: LocalDate,
      val asProject: Boolean,
      val template: EventDto? = null,
  ) : EventDialog

  /** Повторяющееся событие: выбор — один экземпляр, этот и следующие или вся серия. */
  data class ChooseEditMode(val event: EventDto) : EventDialog

  /** Шторка редактирования. */
  data class Editing(val event: EventDto, val mode: EventUpdateMode) : EventDialog

  /** Повторяющееся событие: выбор, что удалять. */
  data class ChooseDeleteMode(val event: EventDto) : EventDialog
}

sealed interface EventManagementUiEvent {
  data class ShowMessage(val message: UiText) : EventManagementUiEvent
}

/**
 * Карточка события из [eventIds] на дне [date] «рождается»: фигура оборачивается, контейнер
 * вспыхивает, вместо времени на [DURATION_MS] — [message]. [startedAt] отличает повторное
 * сохранение того же события — анимация играет заново.
 */
data class Celebration(
    val eventIds: Set<Long>,
    val date: LocalDate,
    val message: UiText,
    val startedAt: Long,
) {
  fun matches(event: EventDto): Boolean =
      event.eventId in eventIds || event.originalEventId in eventIds

  companion object {
    const val DURATION_MS = 2_500L
  }
}
