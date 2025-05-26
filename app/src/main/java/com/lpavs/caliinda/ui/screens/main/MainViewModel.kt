package com.lpavs.caliinda.ui.screens.main

import android.content.Intent
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.data.repo.SettingsRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import android.content.Context
import androidx.lifecycle.ViewModel
import com.lpavs.caliinda.data.ai.AiInteractionManager
import com.lpavs.caliinda.data.auth.AuthManager
import com.lpavs.caliinda.data.auth.AuthState
import com.lpavs.caliinda.data.calendar.CalendarDataManager
import com.lpavs.caliinda.di.ITimeTicker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import com.lpavs.caliinda.data.ai.model.AiVisualizerState
import com.lpavs.caliinda.data.calendar.ApiDeleteEventMode
import com.lpavs.caliinda.data.calendar.CreateEventResult
import com.lpavs.caliinda.data.calendar.DeleteEventResult
import com.lpavs.caliinda.data.calendar.EventNetworkState
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.ui.RecurringDeleteChoice
import com.google.android.gms.tasks.Task
import com.lpavs.caliinda.data.calendar.ClientEventUpdateMode
import com.lpavs.caliinda.data.calendar.UpdateEventResult
import com.lpavs.caliinda.data.local.UpdateEventApiRequest
import kotlinx.coroutines.flow.distinctUntilChanged

@HiltViewModel
class MainViewModel @Inject constructor(
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

    // --- ДЕЛЕГИРОВАННЫЕ И ПРОИЗВОДНЫЕ СОСТОЯНИЯ ДЛЯ UI ---
    val currentTime: StateFlow<Instant> = timeTicker.currentTime

    // Состояния Аутентификации
    val authState: StateFlow<AuthState> = authManager.authState

    // Состояния Календаря
    val currentVisibleDate: StateFlow<LocalDate> = calendarDataManager.currentVisibleDate
    val loadedDateRange: StateFlow<ClosedRange<LocalDate>?> = calendarDataManager.loadedDateRange
    val rangeNetworkState: StateFlow<EventNetworkState> = calendarDataManager.rangeNetworkState
    val createEventResult: StateFlow<CreateEventResult> = calendarDataManager.createEventResult
    val deleteEventResult: StateFlow<DeleteEventResult> = calendarDataManager.deleteEventResult

    // Состояния AI
    val aiState: StateFlow<AiVisualizerState> = aiInteractionManager.aiState
    val aiMessage: StateFlow<String?> = aiInteractionManager.aiMessage // Сообщение от AI (Asking/Result)
    val isAiRotating: StateFlow<Boolean> = aiInteractionManager.aiState.map {
        it == AiVisualizerState.LISTENING || it == AiVisualizerState.THINKING
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Состояния Настроек
    val timeZone: StateFlow<String> = settingsRepository.timeZoneFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ZoneId.systemDefault().id)

    val botTemperState: StateFlow<String> = settingsRepository.botTemperFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "") // Пустая строка или осмысленный дефолт

    init {
        // --- НАБЛЮДЕНИЕ ЗА СОСТОЯНИЯМИ МЕНЕДЖЕРОВ ДЛЯ ОБНОВЛЕНИЯ ГЛАВНОГО UI СОСТОЯНИЯ ---
        observeAuthState()
        observeAiState() // Наблюдаем и за AI для обновления isLoading и message
        observeCalendarNetworkState()
        observeCreateEventResult()
        observeDeleteEventResult()
        observeUpdateEventResult()
    }

    // --- НАБЛЮДАТЕЛИ (вынесены из init для чистоты) ---
    private fun observeAuthState() {
        viewModelScope.launch {
            authManager.authState.map { it.isSignedIn }.distinctUntilChanged().collect { isSignedIn ->
                val auth = authManager.authState.value
                val previousState = _uiState.value
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        isSignedIn = auth.isSignedIn,
                        userEmail = auth.userEmail,
                        displayName = auth.displayName,
                        showAuthError = auth.authError,
                        isLoading = calculateIsLoading(authLoading = auth.isLoading), // Обновляем только этот источник
                        message = if (auth.isSignedIn != previousState.isSignedIn || auth.authError != null) null else currentUiState.message
                    )
                }
                if (isSignedIn) {
                    Log.d(TAG, "Auth observer: User is signed in. Triggering calendar check.")
                    // Инициируем проверку календаря при подтверждении входа
                    calendarDataManager.setCurrentVisibleDate(calendarDataManager.currentVisibleDate.value, forceRefresh = true)
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
                        // Обновляем сообщение, если есть сообщение от AI или стандартное для Listening/Thinking
                        message = aiInteractionManager.aiMessage.value ?: when (ai) {
                            AiVisualizerState.LISTENING -> "Слушаю..."
                            AiVisualizerState.THINKING -> "Обработка..."
                            else -> currentUiState.message // Иначе оставляем текущее
                        }
                    )
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
                        isLoading = calculateIsLoading(networkState = network), // Обновляем только этот источник
                        // Показываем ошибку сети, только если нет ошибки аутентификации
                        showGeneralError = if (network is EventNetworkState.Error && currentUiState.showAuthError == null) network.message else currentUiState.showGeneralError,
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
                    val nextMessage = when (result) {
                        is CreateEventResult.Success -> "Событие успешно создано"
                        is CreateEventResult.Error -> result.message
                        is CreateEventResult.Idle -> {
                            val prevMsg = currentUiState.message
                            if (prevMsg == "Событие успешно создано"  || prevMsg?.contains("Ошибка") == true ) null else prevMsg
                        }
                        is CreateEventResult.Loading -> currentUiState.message
                    }
                    currentUiState.copy(
                        isLoading = calculateIsLoading(createEventState = result), // Обновляем только этот источник
                        message = nextMessage
                    )
                }
            }
        }
    }

    private fun observeDeleteEventResult() {
        viewModelScope.launch {
            calendarDataManager.deleteEventResult.collect { result ->
                _uiState.update { currentState ->
                    val updatedState = when (result) {
                        is DeleteEventResult.Success -> {
                            Log.i(TAG, "Event deletion successful (observed in VM).")
                            // Можно добавить сообщение об успехе в message, если нужно
                            // Сбрасываем ID и ошибку
                            currentState.copy(
                                eventToDeleteId = null, // Убираем ID, т.к. операция завершена
                                deleteOperationError = null,
                                // message = "Событие удалено" // Опционально
                            )
                        }
                        is DeleteEventResult.Error -> {
                            Log.e(TAG, "Event deletion failed (observed in VM): ${result.message}")
                            // Показываем специфическую ошибку удаления
                            currentState.copy(
                                eventToDeleteId = null, // Убираем ID, т.к. операция завершена
                                deleteOperationError = result.message,
                                showDeleteConfirmationDialog = false // Скрываем диалог, если он был открыт и произошла ошибка
                            )
                        }
                        is DeleteEventResult.Loading -> {
                            // Состояние загрузки обрабатывается в calculateIsLoading
                            // Сбрасываем ошибку на время загрузки
                            currentState.copy(deleteOperationError = null)
                        }
                        is DeleteEventResult.Idle -> {
                            // Если состояние стало Idle, значит результат обработан
                            // Сбрасываем ошибку, если она была
                            if (currentState.deleteOperationError != null) {
                                currentState.copy(deleteOperationError = null)
                            } else {
                                currentState // Не меняем, если не нужно
                            }
                        }
                    }
                    // Обновляем isLoading независимо от результата, так как он зависит от всех операций
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
                    val updatedState = when (result) {
                        is UpdateEventResult.Success -> {
                            Log.i(
                                TAG,
                                "Event update successful (observed in VM). Event ID: ${result.updatedEventId}"
                            )
                            currentState.copy(
                                eventBeingEdited = null, // Сбрасываем редактируемое событие
                                showEditEventDialog = false, // Закрываем диалог редактирования
                                editOperationError = null,
                                message = "Событие успешно обновлено" // Опциональное сообщение
                            )
                        }

                        is UpdateEventResult.Error -> {
                            Log.e(TAG, "Event update failed (observed in VM): ${result.message}")
                            currentState.copy(
                                // eventBeingEdited оставляем, чтобы пользователь мог исправить ошибку в форме
                                // showEditEventDialog тоже оставляем открытым
                                editOperationError = result.message
                            )
                        }

                        is UpdateEventResult.Loading -> {
                            currentState.copy(editOperationError = null) // Сбрасываем ошибку на время загрузки
                        }

                        is UpdateEventResult.Idle -> {
                            // Если состояние стало Idle, значит результат обработан (или не было операции)
                            // Если была ошибка и она показана, и теперь Idle, то можно ее сбросить,
                            // если это не делается явно через clearEditError().
                            // Либо просто не меняем message и error.
                            if (currentState.editOperationError != null || currentState.message == "Событие успешно обновлено") {
                                currentState.copy(
                                    editOperationError = null,
                                    message = null
                                ) // Пример сброса
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
        authLoading: Boolean = authManager.authState.value.isLoading, // Берем текущие значения по умолчанию
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
        // Комбинируем все источники загрузки
        return authLoading || calendarLoading || creatingEvent || aiThinking || deletingEvent || updatingEvent
    }

    // --- ДЕЙСТВИЯ АУТЕНТИФИКАЦИИ ---
    fun getSignInIntent(): Intent = authManager.getSignInIntent()
    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) = authManager.handleSignInResult(completedTask)
    fun signOut() = authManager.signOut()
    fun clearAuthError() = authManager.clearAuthError()

    // --- ДЕЙСТВИЯ КАЛЕНДАРЯ ---
    fun onVisibleDateChanged(newDate: LocalDate) = calendarDataManager.setCurrentVisibleDate(newDate)
    fun getEventsFlowForDate(date: LocalDate): Flow<List<CalendarEvent>> = calendarDataManager.getEventsFlowForDate(date)
    fun createEvent(
        summary: String, startTimeString: String, endTimeString: String,
        isAllDay: Boolean, description: String?, timeZoneId: String?, location: String?, recurrenceRule: String?
    ) {
        viewModelScope.launch {
            calendarDataManager.createEvent(summary, startTimeString, endTimeString, isAllDay, timeZoneId = timeZoneId, description, location, recurrenceRule = recurrenceRule)
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
        if (!_uiState.value.isSignedIn) { Log.w(TAG, "Cannot send message: Not signed in."); return }
        // Проверка isLoading происходит внутри calculateIsLoading, UI обновится сам
        // if (_uiState.value.isLoading && aiState.value != AiVisualizerState.LISTENING) { Log.w(TAG, "Cannot send message: App is busy."); return }
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
     * Вызывается из UI, когда пользователь инициирует удаление события.
     * Устанавливает ID события и показывает диалог подтверждения.
     */
    fun requestDeleteConfirmation(event: CalendarEvent) {
        _uiState.update {
            // --- ПРОВЕРЬ ЭТУ ЛОГИКУ ВНИМАТЕЛЬНО ---
            val isActuallyRecurring = event.recurringEventId != null || event.originalStartTime != null
            // ---------------------------------------
            Log.d(TAG, "requestDeleteConfirmation for event: ${event.id}, summary: '${event.summary}', isAllDay: ${event.isAllDay}, recurringId: ${event.recurringEventId}, originalStart: ${event.originalStartTime}, calculatedIsRecurring: $isActuallyRecurring")

            it.copy(
                eventPendingDeletion = event,
                showDeleteConfirmationDialog = !isActuallyRecurring, // Показываем простой диалог, если НЕ повторяющееся
                showRecurringDeleteOptionsDialog = isActuallyRecurring, // Показываем диалог с опциями, если ПОВТОРЯЮЩЕЕСЯ
                deleteOperationError = null
            )
        }
    }


    /**
     * Вызывается из UI, когда пользователь отменяет удаление в диалоге.
     */
    fun cancelDelete() {
        _uiState.update {
            it.copy(
                eventPendingDeletion = null,
                showDeleteConfirmationDialog = false,
                showRecurringDeleteOptionsDialog = false
            )
        }
    }

    /**
     * Вызывается из UI, когда пользователь подтверждает удаление в диалоге.
     * Запускает процесс удаления через DataManager.
     */
    fun confirmDeleteEvent() {
        val eventToDelete = _uiState.value.eventPendingDeletion ?: return
        _uiState.update { it.copy(showDeleteConfirmationDialog = false, eventPendingDeletion = null) }
        viewModelScope.launch {
            // Для одиночного события или если мы решаем удалить экземпляр как обычное событие (что сделает его исключением)
            // или если это мастер и мы хотим удалить всю серию по умолчанию.
            calendarDataManager.deleteEvent(eventToDelete.id, ApiDeleteEventMode.DEFAULT)
        }
    }

    fun confirmRecurringDelete(choice: RecurringDeleteChoice) {
        val eventToDelete = _uiState.value.eventPendingDeletion ?: return
        _uiState.update { it.copy(showRecurringDeleteOptionsDialog = false, eventPendingDeletion = null) }

        val mode = when (choice) {
            RecurringDeleteChoice.SINGLE_INSTANCE -> ApiDeleteEventMode.INSTANCE_ONLY
            RecurringDeleteChoice.ALL_IN_SERIES -> ApiDeleteEventMode.DEFAULT // Бэкенд обработает это как удаление всей серии
        }

        // Для INSTANCE_ONLY, ID должен быть ID экземпляра.
        // Для ALL_IN_SERIES, Google API обычно сам разбирается, если передать ID экземпляра,
        // но безопаснее передать ID мастер-события, если он известен (eventToDelete.recurringEventId).
        // Однако, если eventToDelete.id УЖЕ является ID мастер-события (т.е. recurringEventId == null, но событие по своей природе повторяющееся),
        // то eventToDelete.id и есть то, что нужно для удаления всей серии.

        val idForBackendCall: String
        if (mode == ApiDeleteEventMode.INSTANCE_ONLY) {
            idForBackendCall = eventToDelete.id // Должен быть ID экземпляра
        } else { // ALL_IN_SERIES (через DEFAULT на бэке)
            // Если у нас есть recurringEventId, это ID мастер-события.
            // Если нет, то eventToDelete.id - это либо одиночное, либо уже ID мастер-события.
            idForBackendCall = eventToDelete.recurringEventId ?: eventToDelete.id
        }

        Log.d(TAG, "Confirming recurring delete. Event ID for backend: $idForBackendCall, Mode: $mode")
        viewModelScope.launch {
            calendarDataManager.deleteEvent(idForBackendCall, mode)
        }
    }

    /**
     * Вызывается из UI для сброса флага ошибки удаления после ее показа (например, в Snackbar).
     */
    fun clearDeleteError() {
        _uiState.update { it.copy(deleteOperationError = null) }
    }

    fun clearGeneralError() {
        // Сбрасываем только generalError, не трогая authError
        _uiState.update { it.copy(showGeneralError = null) }
        // Возможно, стоит сбросить и ошибку сети календаря?
        calendarDataManager.clearNetworkError() // Опционально
    }

    /**
     * Вызывается из UI, когда пользователь инициирует редактирование события.
     */
    fun requestEditEvent(event: CalendarEvent) {
        val isRecurring = event.recurringEventId != null || event.originalStartTime != null
        _uiState.update {
            it.copy(
                eventBeingEdited = event, // Сохраняем оригинал для предзаполнения формы и для update API
                showRecurringEditOptionsDialog = isRecurring, // Если повторяющееся, сначала показываем выбор режима
                showEditEventDialog = !isRecurring, // Если одиночное, сразу показываем форму редактирования
                editOperationError = null, // Сбрасываем предыдущую ошибку
                // Сбрасываем состояние формы редактирования, если оно хранится в UiState
                // editFormState = EditEventFormState(summary = event.summary, ...)
            )
        }
        Log.d(TAG, "Requested edit for event ID: ${event.id}, isRecurring: $isRecurring")
    }

    /**
     * Вызывается из диалога выбора режима редактирования для повторяющихся событий.
     */
    fun onRecurringEditOptionSelected(choice: ClientEventUpdateMode) { // Используем ClientEventUpdateMode из DataManager
        // Теперь, когда режим выбран, показываем основную форму/диалог редактирования
        _uiState.update {
            it.copy(
                showRecurringEditOptionsDialog = false, // Скрываем диалог выбора режима
                showEditEventDialog = true // Показываем диалог/экран редактирования
                // eventBeingEdited уже должен быть установлен
            )
        }
        Log.d(TAG, "Recurring edit mode selected: $choice for event: ${_uiState.value.eventBeingEdited?.id}")
    }

    /**
     * Вызывается для отмены процесса редактирования (закрытия диалогов).
     */
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
     * @param updatedEventData Данные, введенные пользователем в форме.
     * @param mode Режим обновления (особенно важен для повторяющихся, выбирается заранее).
     *             Если событие одиночное, mode обычно SINGLE_INSTANCE или можно передать специальное значение,
     *             которое бэкенд поймет как "не повторяющееся".
     *             Но так как update_mode на бэке обязательный, всегда передаем режим.
     */
    fun confirmEventUpdate(
        updatedEventData: UpdateEventApiRequest, // Модель с опциональными полями для API
        // Режим должен быть определен до вызова этой функции (например, сохранен в UiState или передан)
        // Для одиночных событий можно по умолчанию использовать SINGLE_INSTANCE,
        // т.к. для них это не имеет особого значения, API применит изменения к этому ID.
        // Для повторяющихся - режим уже должен быть выбран пользователем.
        modeFromUi: ClientEventUpdateMode // Режим, выбранный пользователем (или дефолтный для одиночных)
    ) {
        val originalEvent = _uiState.value.eventBeingEdited
        if (originalEvent == null) {
            Log.e(TAG, "confirmEventUpdate called but eventBeingEdited is null.")
            _uiState.update { it.copy(editOperationError = "Ошибка: нет данных для редактирования.") }
            return
        }

        Log.d(TAG, "Confirming update for event ID: ${originalEvent.id}, mode: $modeFromUi, data: $updatedEventData")
        // Можно здесь же закрыть диалог редактирования оптимистично, или ждать ответа.
        // _uiState.update { it.copy(showEditEventDialog = false) }

        viewModelScope.launch {
            calendarDataManager.updateEvent(
                eventId = originalEvent.id, // ID оригинального события/экземпляра
                updateData = updatedEventData,
                mode = modeFromUi
            )
            // Результат (успех/ошибка) будет обработан в observeUpdateEventResult
        }
    }

    /**
     * Сбрасывает ошибку операции редактирования.
     */
    fun clearEditError() {
        _uiState.update { it.copy(editOperationError = null) }
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


