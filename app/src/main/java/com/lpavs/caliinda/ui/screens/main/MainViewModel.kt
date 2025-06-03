package com.lpavs.caliinda.ui.screens.main

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.lpavs.caliinda.R
import com.lpavs.caliinda.data.ai.AiInteractionManager
import com.lpavs.caliinda.data.ai.model.AiVisualizerState
import com.lpavs.caliinda.data.auth.AuthManager
import com.lpavs.caliinda.data.calendar.ApiDeleteEventMode
import com.lpavs.caliinda.data.calendar.CalendarDataManager
import com.lpavs.caliinda.data.calendar.ClientEventUpdateMode
import com.lpavs.caliinda.data.calendar.CreateEventResult
import com.lpavs.caliinda.data.calendar.DeleteEventResult
import com.lpavs.caliinda.data.calendar.EventNetworkState
import com.lpavs.caliinda.data.calendar.UpdateEventResult
import com.lpavs.caliinda.data.local.UpdateEventApiRequest
import com.lpavs.caliinda.data.repo.SettingsRepository
import com.lpavs.caliinda.di.ITimeTicker
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.ui.RecurringDeleteChoice
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel
@Inject
constructor(
    // --- ЗАВИСИМОСТИ ---
    private val authManager: AuthManager,
    private val calendarDataManager: CalendarDataManager,
    private val aiInteractionManager: AiInteractionManager,
    private val settingsRepository: SettingsRepository,
    timeTicker: ITimeTicker,
    @ApplicationContext private val context: Context // Оставляем, если нужен для чего-то еще
) : ViewModel() {

  // --- ОСНОВНОЕ СОСТОЯНИЕ UI ---
  private val _uiState = MutableStateFlow(MainUiState())
  val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

  private var initialAuthCheckCompletedAndProcessed = false

  // --- ДЕЛЕГИРОВАННЫЕ И ПРОИЗВОДНЫЕ СОСТОЯНИЯ ДЛЯ UI ---
  val currentTime: StateFlow<Instant> = timeTicker.currentTime

  // Состояния Календаря
  val currentVisibleDate: StateFlow<LocalDate> = calendarDataManager.currentVisibleDate
  val rangeNetworkState: StateFlow<EventNetworkState> = calendarDataManager.rangeNetworkState
  val createEventResult: StateFlow<CreateEventResult> = calendarDataManager.createEventResult
  val updateEventResult: StateFlow<UpdateEventResult> = calendarDataManager.updateEventResult

  // Состояния AI
  val aiState: StateFlow<AiVisualizerState> = aiInteractionManager.aiState
  val aiMessage: StateFlow<String?> =
      aiInteractionManager.aiMessage // Сообщение от AI (Asking/Result)

  // Состояния Настроек
  val timeZone: StateFlow<String> =
      settingsRepository.timeZoneFlow.stateIn(
          viewModelScope, SharingStarted.WhileSubscribed(5000), ZoneId.systemDefault().id)

  val botTemperState: StateFlow<String> =
      settingsRepository.botTemperFlow.stateIn(
          viewModelScope, SharingStarted.WhileSubscribed(5000), "")

  init {
    observeAuthState()
    observeAiState()
    observeCalendarNetworkState()
    observeCreateEventResult()
    observeDeleteEventResult()
    observeUpdateEventResult()
  }

  private val eventCreatedMessage: String =
      context.getString(R.string.event_created) // Получаем строку один раз

  // --- НАБЛЮДАТЕЛИ (вынесены из init для чистоты) ---
  private fun observeAuthState() {
    viewModelScope.launch {
      // Собираем весь AuthState, чтобы иметь доступ ко всем его полям
      authManager.authState.collect { auth ->
        val previousUiState = _uiState.value
        var shouldShowSignInDialog = previousUiState.showSignInRequiredDialog

        if (!initialAuthCheckCompletedAndProcessed && !auth.isLoading) {
          // Это первая полная (не isLoading) информация о состоянии авторизации
          initialAuthCheckCompletedAndProcessed = true
          if (!auth.isSignedIn && auth.authError == null) {
            // Silent sign-in не удался (нет явной ошибки, просто не вошел)
            // Это соответствует вашему логу "SIGN_IN_REQUIRED"
            Log.d(TAG, "Initial Auth: Not signed in, no explicit error. Triggering sign-in dialog.")
            shouldShowSignInDialog = true
          }
        }

        // Если пользователь успешно вошел (возможно, после показа диалога), скрываем диалог
        if (auth.isSignedIn && shouldShowSignInDialog) {
          shouldShowSignInDialog = false
        }

        // Если пользователь вышел (auth.isSignedIn стало false), а до этого был внутри
        // и диалог "требуется вход" не показывался (т.е. это не silent-fail),
        // то мы не должны снова показывать диалог "требуется вход".
        // Это обрабатывается тем, что shouldShowSignInDialog не меняется на true в этом случае.

        _uiState.update { currentUiState ->
          currentUiState.copy(
              isSignedIn = auth.isSignedIn,
              userEmail = auth.userEmail,
              displayName = auth.displayName, // Убедитесь, что AuthState это предоставляет
              showAuthError = auth.authError,
              isLoading =
                  calculateIsLoading(
                      authLoading = auth.isLoading), // Передаем текущий auth.isLoading
              // Сбрасываем общее сообщение, если изменился статус входа, ошибка или видимость
              // диалога
              message =
                  if (auth.isSignedIn != previousUiState.isSignedIn ||
                      auth.authError != null ||
                      shouldShowSignInDialog != previousUiState.showSignInRequiredDialog) {
                    null
                  } else {
                    currentUiState.message
                  },
              showSignInRequiredDialog = shouldShowSignInDialog)
        }

        if (auth.isSignedIn) {
          Log.d(TAG, "Auth observer: User is signed in. Triggering calendar check.")
          calendarDataManager.setCurrentVisibleDate(
              calendarDataManager.currentVisibleDate.value, forceRefresh = true)
        } else {
          // Пользователь не вошел. Если это не случай "требуется вход", то просто не вошел.
          // Данные календаря и так не загрузятся из-за проверки в CalendarDataManager.
          // Можно дополнительно очистить локальные данные календаря, если необходимо.
          // TODO calendarDataManager.clearLocalDataOnSignOut() // Пример
          if (!shouldShowSignInDialog && previousUiState.isSignedIn) {
            // Это был явный выход пользователя, а не silent fail
            Log.d(TAG, "Auth observer: User has signed out.")
          }
        }
      }
    }
  }

  private fun observeAiState() {
    viewModelScope.launch {
      aiInteractionManager.aiState.collect { ai ->
        _uiState.update { currentUiState ->
          currentUiState.copy(
              isListening = ai == AiVisualizerState.LISTENING,
              isLoading = calculateIsLoading(aiState = ai), // Обновляем только этот источник
              // Обновляем сообщение, если есть сообщение от AI или стандартное для
              // Listening/Thinking
              message =
                  aiInteractionManager.aiMessage.value
                      ?: when (ai) {
                        AiVisualizerState.LISTENING -> "Слушаю..."
                        AiVisualizerState.THINKING -> "Обработка..."
                        else -> currentUiState.message // Иначе оставляем текущее
                      })
        }
        // Реакция на результат AI (успешное создание события)
        if (ai == AiVisualizerState.RESULT /*&& какое-то условие успеха, если нужно*/) {
          Log.d(TAG, "AI observer: Interaction finished with RESULT, triggering calendar refresh.")
          refreshCurrentVisibleDate() // Обновляем календарь
        }
      }
    }
  }

  private fun observeCalendarNetworkState() {
    viewModelScope.launch {
      calendarDataManager.rangeNetworkState.collect { network ->
        _uiState.update { currentUiState ->
          currentUiState.copy(
              isLoading =
                  calculateIsLoading(networkState = network), // Обновляем только этот источник
              // Показываем ошибку сети, только если нет ошибки аутентификации
              showGeneralError =
                  if (network is EventNetworkState.Error && currentUiState.showAuthError == null)
                      network.message
                  else currentUiState.showGeneralError,
              // Сбрасываем ошибку сети, если она была показана и состояние изменилось
          )
        }
      }
    }
  }

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
                      prevMsg?.contains(context.getString(R.string.error)) ==
                          true) { // Пример для "Ошибка"
                    null
                  } else {
                    prevMsg
                  }
                }
                is CreateEventResult.Loading -> currentUiState.message
              }
          currentUiState.copy(
              isLoading =
                  calculateIsLoading(createEventState = result), // Обновляем только этот источник
              message = nextMessage)
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
                      eventBeingEdited = null, // Сбрасываем редактируемое событие
                      showEditEventDialog = false, // Закрываем диалог редактирования
                      editOperationError = null,
                      message = "Событие успешно обновлено" // Опциональное сообщение
                      )
                }

                is UpdateEventResult.Error -> {
                  Log.e(TAG, "Event update failed (observed in VM): ${result.message}")
                  currentState.copy(editOperationError = result.message)
                }

                is UpdateEventResult.Loading -> {
                  currentState.copy(
                      editOperationError = null) // Сбрасываем ошибку на время загрузки
                }

                is UpdateEventResult.Idle -> {
                  // Если состояние стало Idle, значит результат обработан (или не было операции)
                  // Если была ошибка и она показана, и теперь Idle, то можно ее сбросить,
                  // если это не делается явно через clearEditError().
                  // Либо просто не меняем message и error.
                  if (currentState.editOperationError != null ||
                      currentState.message == "Событие успешно обновлено") {
                    currentState.copy(editOperationError = null, message = null) // Пример сброса
                  } else {
                    currentState
                  }
                }
              }
          updatedState.copy(isLoading = calculateIsLoading()) // Обновляем общий isLoading
        }

        if (result is UpdateEventResult.Success || result is UpdateEventResult.Error) {
          calendarDataManager.consumeUpdateEventResult() // Сбрасываем состояние в DataManager
        }
      }
    }
  }

  // --- ПРИВАТНЫЙ ХЕЛПЕР ДЛЯ РАСЧЕТА ОБЩЕГО isLoading ---
  /** Рассчитывает общее состояние загрузки, комбинируя состояния менеджеров */
  private fun calculateIsLoading(
      authLoading: Boolean =
          authManager.authState.value.isLoading, // Берем текущие значения по умолчанию
      networkState: EventNetworkState = calendarDataManager.rangeNetworkState.value,
      aiState: AiVisualizerState = aiInteractionManager.aiState.value,
      createEventState: CreateEventResult = calendarDataManager.createEventResult.value,
      deleteEventState: DeleteEventResult = calendarDataManager.deleteEventResult.value,
      updateEventState: UpdateEventResult = calendarDataManager.updateEventResult.value
  ): Boolean {
    val calendarLoading = networkState is EventNetworkState.Loading
    val creatingEvent = createEventState is CreateEventResult.Loading
    val deletingEvent = deleteEventState is DeleteEventResult.Loading
    val updatingEvent = updateEventState is UpdateEventResult.Loading
    val aiThinking = aiState == AiVisualizerState.THINKING

    return authLoading ||
        calendarLoading ||
        creatingEvent ||
        aiThinking ||
        deletingEvent ||
        updatingEvent
  }

  // --- ДЕЙСТВИЯ АУТЕНТИФИКАЦИИ ---
  fun getSignInIntent(): Intent = authManager.getSignInIntent()

  fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) =
      authManager.handleSignInResult(completedTask)

  fun signOut() {
    // Перед выходом, если диалог "требуется вход" был активен, его нужно скрыть
    if (_uiState.value.showSignInRequiredDialog) {
      _uiState.update { it.copy(showSignInRequiredDialog = false) }
    }
    authManager.signOut()
  }

  fun clearAuthError() = authManager.clearAuthError()

  fun onSignInRequiredDialogConfirmed() {
    // Пользователь нажал "Войти" в диалоге "требуется вход"
    _uiState.update { it.copy(showSignInRequiredDialog = false) }
    // UI должен будет вызвать getSignInIntent() и запустить Activity for result.
    // ViewModel здесь просто скрывает диалог.
  }

  fun onSignInRequiredDialogDismissed() {
    // Пользователь нажал "Отмена" или закрыл диалог "требуется вход"
    _uiState.update { it.copy(showSignInRequiredDialog = false) }
    // Можно здесь дополнительно обновить uiState.message, если хотите
    // _uiState.update { it.copy(message = "Вход отменен. Функционал ограничен.") }
    Log.d(TAG, "Sign-in required dialog was dismissed by the user.")
  }

  // --- ДЕЙСТВИЯ КАЛЕНДАРЯ ---
  fun onVisibleDateChanged(newDate: LocalDate) = calendarDataManager.setCurrentVisibleDate(newDate)

  fun getEventsFlowForDate(date: LocalDate): Flow<List<CalendarEvent>> =
      calendarDataManager.getEventsFlowForDate(date)

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

  fun refreshCurrentVisibleDate() {
    viewModelScope.launch {
      calendarDataManager.refreshDate(calendarDataManager.currentVisibleDate.value)
    }
  }

  // --- ДЕЙСТВИЯ AI ---
  fun startListening() {
    if (!_uiState.value.isPermissionGranted) {
      _uiState.update { it.copy(showGeneralError = "Нет разрешения на запись аудио") }
      return
    }
    aiInteractionManager.startListening()
  }

  fun stopListening() = aiInteractionManager.stopListening()

  fun sendTextMessage(text: String) {
    if (!_uiState.value.isSignedIn) {
      Log.w(TAG, "Cannot send message: Not signed in.")
      return
    }
    // Проверка isLoading происходит внутри calculateIsLoading, UI обновится сам
    // if (_uiState.value.isLoading && aiState.value != AiVisualizerState.LISTENING) { Log.w(TAG,
    // "Cannot send message: App is busy."); return }
    aiInteractionManager.sendTextMessage(text)
  }

  fun resetAiStateAfterResult() = aiInteractionManager.resetAiState()

  fun resetAiStateAfterAsking() = aiInteractionManager.resetAiState()

  // --- ДЕЙСТВИЯ НАСТРОЕК ---
  fun updateTimeZoneSetting(zoneId: String) {
    if (ZoneId.getAvailableZoneIds().contains(zoneId)) {
      viewModelScope.launch { settingsRepository.saveTimeZone(zoneId) }
    } else {
      Log.e(TAG, "Attempted to save invalid time zone ID: $zoneId")
    }
  }

  fun updateBotTemperSetting(newTemper: String) {
    viewModelScope.launch { settingsRepository.saveBotTemper(newTemper) }
  }

  // --- ОБРАБОТКА UI СОБЫТИЙ / РАЗРЕШЕНИЙ ---
  fun updatePermissionStatus(isGranted: Boolean) {
    if (_uiState.value.isPermissionGranted != isGranted) {
      _uiState.update { it.copy(isPermissionGranted = isGranted) }
      Log.d(TAG, "Audio permission status updated to: $isGranted")
    }
  }

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
      // Для одиночного события или если мы решаем удалить экземпляр как обычное событие (что
      // сделает его исключением)
      // или если это мастер и мы хотим удалить всю серию по умолчанию.
      calendarDataManager.deleteEvent(eventToDelete.id, ApiDeleteEventMode.DEFAULT)
    }
  }

  fun confirmRecurringDelete(choice: RecurringDeleteChoice) {
    val eventToDelete = _uiState.value.eventPendingDeletion ?: return
    _uiState.update {
      it.copy(showRecurringDeleteOptionsDialog = false, eventPendingDeletion = null)
    }

    val mode =
        when (choice) {
          RecurringDeleteChoice.SINGLE_INSTANCE -> ApiDeleteEventMode.INSTANCE_ONLY
          RecurringDeleteChoice.ALL_IN_SERIES ->
              ApiDeleteEventMode.DEFAULT
        }

      val idForBackendCall: String = if (mode == ApiDeleteEventMode.INSTANCE_ONLY) {
            eventToDelete.id
        } else {
            eventToDelete.recurringEventId ?: eventToDelete.id
        }

    Log.d(TAG, "Confirming recurring delete. Event ID for backend: $idForBackendCall, Mode: $mode")
    viewModelScope.launch { calendarDataManager.deleteEvent(idForBackendCall, mode) }
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
          editOperationError = null
          // editFormState = EditEventFormState() // Сброс формы
          )
    }
    Log.d(TAG, "Event editing cancelled.")
  }

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
    // Можно здесь же закрыть диалог редактирования оптимистично, или ждать ответа.
    // _uiState.update { it.copy(showEditEventDialog = false) }

    viewModelScope.launch {
      calendarDataManager.updateEvent(
          eventId = originalEvent.id, // ID оригинального события/экземпляра
          updateData = updatedEventData,
          mode = modeFromUi)
    }
  }

  /** Сбрасывает ошибку операции редактирования. */
  fun clearEditError() {
    _uiState.update { it.copy(editOperationError = null) }
  }

  fun consumeUpdateEventResult() {
    calendarDataManager.consumeUpdateEventResult()
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

  // --- LIFECYCLE ---
  override fun onCleared() {
    super.onCleared()
    aiInteractionManager.destroy() // Вызываем очистку менеджера AI
  }

  // --- COMPANION ---
  companion object {
    private const val TAG = "MainViewModel" // Используем один TAG
  }
}
