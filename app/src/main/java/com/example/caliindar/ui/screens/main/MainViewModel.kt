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
import com.example.caliindar.data.calendar.CreateEventResult
import com.example.caliindar.data.calendar.EventNetworkState
import com.google.android.gms.tasks.Task

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
    }

    // --- НАБЛЮДАТЕЛИ (вынесены из init для чистоты) ---
    private fun observeAuthState() {
        viewModelScope.launch {
            authManager.authState.collect { auth ->
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
                if (auth.isSignedIn) {
                    Log.d(TAG, "Auth observer: User is signed in. Triggering calendar check.")
                    // Инициируем проверку календаря при подтверждении входа
                    calendarDataManager.setCurrentVisibleDate(calendarDataManager.currentVisibleDate.value)
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

    // --- ПРИВАТНЫЙ ХЕЛПЕР ДЛЯ РАСЧЕТА ОБЩЕГО isLoading ---
    /** Рассчитывает общее состояние загрузки, комбинируя состояния менеджеров */
    private fun calculateIsLoading(
        authLoading: Boolean = authManager.authState.value.isLoading, // Берем текущие значения по умолчанию
        networkState: EventNetworkState = calendarDataManager.rangeNetworkState.value,
        aiState: AiVisualizerState = aiInteractionManager.aiState.value,
        createEventState: CreateEventResult = calendarDataManager.createEventResult.value
    ): Boolean {
        val calendarLoading = networkState is EventNetworkState.Loading
        val creatingEvent = createEventState is CreateEventResult.Loading
        val aiThinking = aiState == AiVisualizerState.THINKING
        // Комбинируем все источники загрузки
        return authLoading || calendarLoading || creatingEvent || aiThinking
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
        isAllDay: Boolean, description: String?, location: String?, recurrenceRule: String?
    ) {
        viewModelScope.launch {
            calendarDataManager.createEvent(summary, startTimeString, endTimeString, isAllDay, description, location)
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
    fun clearGeneralError() {
        // Сбрасываем только generalError, не трогая authError
        _uiState.update { it.copy(showGeneralError = null) }
        // Возможно, стоит сбросить и ошибку сети календаря?
        // calendarDataManager.clearNetworkError() // Опционально
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


