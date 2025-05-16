package com.example.caliindar.ui.screens.main

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.caliindar.data.local.EventDao
import com.example.caliindar.data.mapper.EventMapper
import com.example.caliindar.data.repo.SettingsRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import org.json.JSONException
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject
import android.content.Context
import android.text.TextUtils.replace
import androidx.lifecycle.ViewModel
import com.example.caliindar.BuildConfig.BACKEND_BASE_URL
import com.example.caliindar.data.ai.AiInteractionManager
import com.example.caliindar.data.auth.AuthManager
import com.example.caliindar.data.auth.AuthState
import com.example.caliindar.data.calendar.CalendarDataManager
import com.example.caliindar.data.local.DateTimeUtils
import com.example.caliindar.di.ITimeTicker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import com.example.caliindar.di.BackendUrl // Импорт квалификатора
import com.example.caliindar.di.WebClientId // Импорт квалификатора
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import java.time.LocalTime
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit
import com.example.caliindar.data.ai.model.AiVisualizerState
import com.example.caliindar.data.calendar.ApiDeleteEventMode
import com.example.caliindar.data.calendar.CreateEventResult
import com.example.caliindar.data.calendar.DeleteEventResult
import com.example.caliindar.data.calendar.EventNetworkState
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.ui.RecurringDeleteChoice
import com.google.android.gms.tasks.Task
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

                // Потребляем результат (сбрасываем в Idle в DataManager),
                // когда он Success или Error, чтобы UI не реагировал на него повторно
                if (result is DeleteEventResult.Success || result is DeleteEventResult.Error) {
                    calendarDataManager.consumeDeleteEventResult()
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
        deleteEventState: DeleteEventResult = calendarDataManager.deleteEventResult.value
    ): Boolean {
        val calendarLoading = networkState is EventNetworkState.Loading
        val creatingEvent = createEventState is CreateEventResult.Loading
        val deletingEvent = deleteEventState is DeleteEventResult.Loading
        val aiThinking = aiState == AiVisualizerState.THINKING
        // Комбинируем все источники загрузки
        return authLoading || calendarLoading || creatingEvent || aiThinking || deletingEvent
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
    fun clearRangeNetworkError() = calendarDataManager.clearNetworkError()

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
            it.copy(
                eventPendingDeletion = event,
                showDeleteConfirmationDialog = event.recurringEventId == null && event.originalStartTime == null, // Показываем простой диалог, если нет признаков повторения
                showRecurringDeleteOptionsDialog = event.recurringEventId != null || event.originalStartTime != null, // Показываем диалог с опциями, если есть признаки повторения
                deleteOperationError = null
            )
        }
        Log.d(TAG, "Requested delete for event ID: ${event.id}, recurringId: ${event.recurringEventId}, originalStart: ${event.originalStartTime}")
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


