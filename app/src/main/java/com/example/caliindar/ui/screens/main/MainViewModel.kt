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
import com.example.caliindar.util.AudioRecorder
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
import com.example.caliindar.data.local.DateTimeUtils
import com.example.caliindar.di.ITimeTicker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import com.example.caliindar.di.BackendUrl // Импорт квалификатора
import com.example.caliindar.di.WebClientId // Импорт квалификатора
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit

enum class AiVisualizerState {
    IDLE,      // Начальное состояние / Ничего не происходит (кнопка видима)
    LISTENING, // Пользователь записывает голос (фигура большая, крутится внизу)
    THINKING,  // ИИ обрабатывает запрос (фигура меньше, крутится в центре)
    ASKING,    // ИИ задает уточняющий вопрос (фигура внизу, показывает текст)
    RESULT,     // ИИ показывает результат/событие (фигура внизу, показывает текст/данные)
    ERROR
}


// Переносим ViewModel сюда
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val eventDao: EventDao,
    private val settingsRepository: SettingsRepository,
    timeTicker: ITimeTicker,
    @BackendUrl private val backendBaseUrl: String, // Внедряем URL
    @WebClientId private val webClientId: String    // Внедряем Client ID
): ViewModel() {

    val currentTime: StateFlow<Instant> = timeTicker.currentTime

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _aiState = MutableStateFlow(AiVisualizerState.IDLE)
    val aiState: StateFlow<AiVisualizerState> = _aiState.asStateFlow()

    private val _aiMessage = MutableStateFlow<String?>(null) // Текст для ASKING/RESULT
    val aiMessage: StateFlow<String?> = _aiMessage.asStateFlow()
    val botTemperState: StateFlow<String> = settingsRepository.botTemperFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ZoneId.systemDefault().id // Начальное значение (пока DataStore не загрузится)
        )

    val timeZone: StateFlow<String> = settingsRepository.timeZoneFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ZoneId.systemDefault().id // Начальное значение (пока DataStore не загрузится)
        )
    fun updateTimeZoneSetting(zoneId: String) {
        if (ZoneId.getAvailableZoneIds().contains(zoneId)) {
            viewModelScope.launch {
                settingsRepository.saveTimeZone(zoneId)
            }
        } else {
            Log.e(TAG, "Attempted to save invalid time zone ID: $zoneId")
            // Можно показать ошибку пользователю, если нужно
        }
    }

    val use12HourFormat: StateFlow<Boolean> = settingsRepository.use12HourFormatFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false // По умолчанию 24-часовой формат
        )

    fun updateUse12HourFormat(use12Hour: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveUse12HourFormat(use12Hour)
        }
    }


    val isAiRotating: StateFlow<Boolean> = aiState.map {
        it == AiVisualizerState.LISTENING || it == AiVisualizerState.THINKING
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Если AudioRecorder не внедряется, создаем его здесь
    private val audioRecorder = AudioRecorder(application.cacheDir)
    private var currentIdToken: String? = null
    private var LISTENINGStartTime: Long = 0L

    // Google Sign-In
    private val gso: GoogleSignInOptions
    val googleSignInClient: GoogleSignInClient

    private var speechRecognizer: SpeechRecognizer? = null
    private val speechRecognizerIntent: Intent

    private var recognitionSuccessful = false


    companion object {
        const val PREFETCH_DAYS_FORWARD = 5
        const val PREFETCH_DAYS_BACKWARD = 3
        const val TRIGGER_PREFETCH_THRESHOLD_FORWARD = 2 // За сколько дней до конца загруженного диапазона начать новую загрузку
        const val TRIGGER_PREFETCH_THRESHOLD_BACKWARD = 1 // За сколько
        const val JUMP_DETECTION_BUFFER_DAYS = 10
        private const val TAG = "MainViewModelAuth"
    }

    init {
        // Инициализация Google Sign-In Client
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar.events"))
            .requestServerAuthCode(webClientId)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(application, gso)

        // Проверяем доступность распознавания
        if (!SpeechRecognizer.isRecognitionAvailable(context)) { // <-- Используем context
            Log.e(TAG, "Speech recognition not available on this device.")
            _uiState.update { it.copy(showGeneralError = "Распознавание речи недоступно на этом устройстве") }
        } else {
            initializeSpeechRecognizer()
        }

        // Инициализация Intent для распознавания
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Укажи язык, если нужно (иначе будет язык системы)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU") // Например, русский
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...") // Подсказка пользователю (может отображаться системным UI)
            // partial results - получать промежуточные результаты
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    //    ensureDateRangeLoadedAround(LocalDate.now())
        // Наблюдение за состоянием входа (остается)
        checkInitialAuthState()
    }

    // Состояние для списка событий

    sealed interface EventNetworkState {
        object Idle : EventNetworkState
        object Loading : EventNetworkState
        data class Error(val message: String) : EventNetworkState
    }

    private val _currentVisibleDate = MutableStateFlow(LocalDate.now()) // Дата, видимая в Pager
    val currentVisibleDate: StateFlow<LocalDate> = _currentVisibleDate.asStateFlow()


    // Отслеживаем диапазон дат, которые УЖЕ были ЗАПРОШЕНЫ с бэкенда
    private val _loadedDateRange = MutableStateFlow<ClosedRange<LocalDate>?>(null)
    val loadedDateRange: StateFlow<ClosedRange<LocalDate>?> = _loadedDateRange.asStateFlow()


    // Состояние сетевой загрузки ДИАПАЗОНА (можно сделать сложнее, но начнем с простого)
    // TODO: А что усложнять
    private val _rangeNetworkState = MutableStateFlow<EventNetworkState>(EventNetworkState.Idle)
    val rangeNetworkState: StateFlow<EventNetworkState> = _rangeNetworkState.asStateFlow()

    val currentZoneId = timeZone.value

// - ---- -- -  БЛОК КАЛЕНДАРЯ
    // --- ЛОГИКА ЗАГРУЗКИ ДИАПАЗОНА ---
    fun onVisibleDateChanged(newDate: LocalDate) {
        // Оптимизация: не обновляем, если дата та же
        if (newDate == _currentVisibleDate.value) {
            Log.d(TAG, "Visible date $newDate is already set. Ignoring.")
            return
        }
        Log.d(TAG, "Visible date changed requested for: $newDate")
        _currentVisibleDate.value = newDate
        // Используем обновленную логику загрузки
        ensureDateRangeLoadedAround(newDate)
    }
    /**
     * Проверяет, нужно ли загружать/расширять диапазон дат вокруг centerDate.
     * Различает prefetching при близком скролле и загрузку нового диапазона при "прыжке".
     */
    internal fun ensureDateRangeLoadedAround(centerDate: LocalDate) {
        viewModelScope.launch {
            val currentlyLoaded = _loadedDateRange.value
            val isLoading = _rangeNetworkState.value is EventNetworkState.Loading

            if (isLoading) {
                Log.d(TAG, "Load check skipped for $centerDate, range fetch already in progress.")
                return@launch
            }

            // Рассчитываем идеальный диапазон (как и раньше)
            val idealTargetRange = centerDate.minusDays(PREFETCH_DAYS_BACKWARD.toLong()) .. centerDate.plusDays(PREFETCH_DAYS_FORWARD.toLong())

            // Логируем начальные данные
            Log.d(TAG, "ensureDateRange: center=$centerDate, current=$currentlyLoaded, ideal=$idealTargetRange, isLoading=$isLoading")

            if (currentlyLoaded == null) {
                // --- Initial Load ---
                Log.i(TAG, "No range loaded yet. Initial load: $idealTargetRange")
                fetchAndStoreDateRange(idealTargetRange.start, idealTargetRange.endInclusive, true) // replace=true
            } else {
                // --- Проверяем, нужно ли действие ---

                // 1. Проверка на "прыжок" (остается)
                val isJump = centerDate < currentlyLoaded.start.minusDays(JUMP_DETECTION_BUFFER_DAYS.toLong()) ||
                        centerDate > currentlyLoaded.endInclusive.plusDays(JUMP_DETECTION_BUFFER_DAYS.toLong())

                if (isJump) {
                    // --- Случай 1: Прыжок ---
                    Log.i(TAG, "Jump detected! Center: $centerDate is far from loaded $currentlyLoaded.")
                    Log.i(TAG, "Loading new focused range: $idealTargetRange. Discarding old range.")
                    fetchAndStoreDateRange(idealTargetRange.start, idealTargetRange.endInclusive, true) // replace=true
                } else {
                    // --- Не прыжок. Проверяем, нужна ли предзагрузка/расширение ---

                    // 2. Проверка, нужно ли грузить ВПЕРЕД (Forward)
                    // Условие: centerDate находится близко к КОНЦУ загруженного диапазона
                    val needsForwardPrefetch = centerDate >= currentlyLoaded.endInclusive.minusDays(TRIGGER_PREFETCH_THRESHOLD_FORWARD.toLong())

                    // 3. Проверка, нужно ли грузить НАЗАД (Backward)
                    // Условие: centerDate находится близко к НАЧАЛУ загруженного диапазона
                    val needsBackwardPrefetch = centerDate <= currentlyLoaded.start.plusDays(TRIGGER_PREFETCH_THRESHOLD_BACKWARD.toLong())

                    if (needsForwardPrefetch || needsBackwardPrefetch) {
                        // --- Случай 2: Предзагрузка/Расширение необходимо ---
                        Log.i(TAG, "Prefetch needed. Forward=$needsForwardPrefetch, Backward=$needsBackwardPrefetch. Center: $centerDate, Current: $currentlyLoaded.")
                        // Объединяем текущий диапазон с идеальным вокруг НОВОЙ centerDate, чтобы расширить в нужную сторону
                        val rangeToLoad = currentlyLoaded.union(idealTargetRange)
                        Log.d(TAG, "Expanding range to: $rangeToLoad")
                        fetchAndStoreDateRange(rangeToLoad.start, rangeToLoad.endInclusive, false) // replace=false
                    } else {
                        // --- Случай 3: Не прыжок и предзагрузка не нужна ---
                        // centerDate находится достаточно далеко от краев currentlyLoaded.
                        Log.d(TAG, "No load needed. Center $centerDate is comfortably within loaded range $currentlyLoaded and thresholds.")
                        // Ничего не делаем
                    }
                }
            }
        }
    }





    // --- ИЗМЕНЕННАЯ ФУНКЦИЯ ЗАГРУЗКИ ---
    // Загружает данные для ДИАПАЗОНА дат с нового эндпоинта
    /**
     * Загружает данные для ДИАПАЗОНА дат с бэкенда и сохраняет в БД.
     * @param startDate Начальная дата диапазона.
     * @param endDate Конечная дата диапазона.
     * @param replaceLoadedRange Если true, то `_loadedDateRange` будет ЗАМЕНЕН новым диапазоном.
     *                          Если false, то новый диапазон будет ОБЪЕДИНЕН с текущим `_loadedDateRange`.
     */
    private fun fetchAndStoreDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
        replaceLoadedRange: Boolean // <-- Новый параметр
    ) {
        // Предотвращаем множественные одновременные загрузки (можно улучшить, разрешая непересекающиеся)
        if (_rangeNetworkState.value is EventNetworkState.Loading) {
            Log.d(TAG, "Range fetch request ignored for $startDate..$endDate (replace=$replaceLoadedRange), already loading.")
            return
        }
        if (!_uiState.value.isSignedIn) {
            Log.w(TAG, "Cannot fetch range: User not signed in.")
            return
        }

        viewModelScope.launch {
            Log.i(TAG, "Starting background fetch for date range: $startDate to $endDate (replace=$replaceLoadedRange)")
            _rangeNetworkState.value = EventNetworkState.Loading

            val freshToken = getFreshIdToken()
            if (freshToken == null) {
                Log.w(TAG, "Cannot fetch range: Failed to get fresh ID token.")
                _rangeNetworkState.value = EventNetworkState.Error("Ошибка аутентификации")
                return@launch
            }

            val url = "$BACKEND_BASE_URL/calendar/events/range" +
                    "?startDate=${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}" +
                    "&endDate=${endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}"

            val request = Request.Builder()
                .url(url)
                .get()
                .header("Authorization", "Bearer $freshToken")
                .build()

            try {
                val networkEvents: List<CalendarEvent> = withContext(Dispatchers.IO) {
                    // ... (логика выполнения запроса okHttpClient) ...
                    okHttpClient.newCall(request).execute().use { response ->
                        val responseBodyString = response.body?.string()
                        if (!response.isSuccessful) {
                            val errorMsg = parseBackendError(responseBodyString, response.code)
                            Log.e(TAG, "Error fetching range from backend: ${response.code} - $errorMsg")
                            throw IOException("Backend error: ${response.code} - $errorMsg")
                        }
                        if (responseBodyString.isNullOrBlank()) {
                            Log.w(TAG, "Empty response body received for range $startDate to $endDate.")
                            emptyList()
                        } else {
                            // Логируем УСПЕШНЫЙ ответ перед парсингом
                            Log.i(TAG, "Range fetched successfully from network for $startDate to $endDate. Response size: ${responseBodyString.length}")
                            // Опционально: логировать сам ответ, если он небольшой, для отладки
                            // Log.v(TAG, "Response body: $responseBodyString")
                            parseEventsResponse(responseBodyString) // Парсим ответ
                        }
                    }
                } // End Dispatchers.IO

                // --- Успешное получение и парсинг ---
                val eventEntities = networkEvents.mapNotNull { EventMapper.mapToEntity(it, currentZoneId) }

                // --- Сохранение в БД ---
                val startRangeMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                // Конец диапазона - НАЧАЛО следующего дня после endDate
                val endRangeMillis = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

                Log.d(TAG, "Updating DB for range $startDate to $endDate. Deleting range $startRangeMillis..< $endRangeMillis, Inserting ${eventEntities.size} events.")
                // Важно: Используем транзакцию в DAO для атомарности clear + insert
                eventDao.clearAndInsertEventsForRange(startRangeMillis, endRangeMillis, eventEntities)
                Log.i(TAG, "Successfully updated DB with events for range $startDate to $endDate.")

                // --- Обновление _loadedDateRange в зависимости от флага ---
                val fetchedRange = startDate..endDate
                if (replaceLoadedRange) {
                    _loadedDateRange.value = fetchedRange // Просто заменяем
                    Log.d(TAG, "Replaced loaded range with: $fetchedRange")
                } else {
                    _loadedDateRange.update { current ->
                        val updatedRange = current?.union(fetchedRange) ?: fetchedRange // Объединяем
                        Log.d(TAG, "Merged fetched range $fetchedRange with current ${current}. New loaded range: $updatedRange")
                        updatedRange
                    }
                }
                // --- Сброс состояния сети ---
                _rangeNetworkState.value = EventNetworkState.Idle // Успех

            } catch (e: IOException) {
                Log.e(TAG, "Network error fetching range $startDate to $endDate", e)
                _rangeNetworkState.value = EventNetworkState.Error("Ошибка сети: ${e.message}")
            } catch (e: JSONException) { // Ловим конкретно ошибку парсинга JSON
                Log.e(TAG, "JSON parsing error for range $startDate to $endDate", e)
                _rangeNetworkState.value = EventNetworkState.Error("Ошибка чтения ответа сервера.")
            } catch (e: Exception) { // Ловим остальные ошибки (маппинг, БД и т.д.)
                Log.e(TAG, "Error processing range response for $startDate to $endDate", e)
                _rangeNetworkState.value = EventNetworkState.Error("Ошибка обработки данных: ${e.message}")
            }
        }
    }

    // --- НОВАЯ ФУНКЦИЯ ДЛЯ UI (ДЛЯ СТРАНИЦЫ PAGER) ---
    /**
     * Предоставляет Flow списка событий ТОЛЬКО для указанной даты.
     * Данные берутся из локальной БД (Room).
     */
    fun getEventsFlowForDate(date: LocalDate): Flow<List<CalendarEvent>> {
        val startOfDayUTC = date.atStartOfDay(ZoneOffset.UTC)
        val startMillis = startOfDayUTC.toInstant().toEpochMilli()
        // endMillis - это НАЧАЛО следующего дня (00:00)
        val endMillis = startOfDayUTC.plusDays(1).toInstant().toEpochMilli()

        // Log.d(TAG, "Requesting event Flow from DB for date: $date (Range UTC millis: $startMillis <= t < $endMillis)")

        return eventDao.getEventsForDateRangeFlow(startMillis, endMillis)
            // Эта лямбда получает список ВСЕХ событий, НАЧИНАЮЩИХСЯ в этот день из БД
            .map { entityList ->
                // Фильтруем этот список перед маппингом в Domain модель
                entityList.filter { entity ->
                    if (!entity.isAllDay) {
                        if (true) {

                            entity.endTimeMillis <= endMillis // Время конца должно быть МЕНЬШЕ ИЛИ РАВНО началу следующего дня
                        } else {
                            // Если нет времени начала или конца, не отображаем (или реши как обрабатывать)
                            false
                        }
                    } else {
                        // Правило для all-day:
                        // Отображаем только если длительность ~ 1 день.
                        if (true) {
                            val durationMillis = entity.endTimeMillis - entity.startTimeMillis
                            val twentyFourHoursMillis = TimeUnit.HOURS.toMillis(24)
                            // Допускаем небольшую погрешность (напр. из-за летнего времени, если хранишь в локальной зоне)
                            // Если хранишь строго в UTC, погрешность может быть меньше.
                            val toleranceMillis = TimeUnit.MINUTES.toMillis(5) // Например, 5 минут
                            (durationMillis >= twentyFourHoursMillis - toleranceMillis) && (durationMillis <= twentyFourHoursMillis + toleranceMillis)
                        } else {
                            false // Не можем рассчитать длительность
                        }
                    }
                } // Конец filter
                    .mapNotNull { filteredEntity ->
                        // Маппим только те entity, которые прошли фильтр
                        EventMapper.mapToDomain(filteredEntity, currentZoneId)
                    } // Конец mapNotNull
            } // Конец map
            .catch { e ->
                Log.e(TAG, "Error processing events Flow for date $date", e)
                emit(emptyList<CalendarEvent>())
            }
            .flowOn(Dispatchers.IO) // Операции с БД, фильтрация и маппинг в IO потоке
    }


    private fun parseEventsResponse(jsonString: String): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        try {
            val jsonArray = JSONArray(jsonString) // Используем org.json.JSONArray
            for (i in 0 until jsonArray.length()) {
                try { // Добавляем try-catch для каждого объекта
                    val eventObject = jsonArray.getJSONObject(i)

                    // Читаем isAllDay (он теперь приходит от бэкенда)
                    val isAllDay = eventObject.optBoolean("isAllDay", false)

                    // --- ВСЕГДА читаем startTime и endTime ---
                    val startTimeStr = eventObject.optString("startTime")
                    val endTimeStr = eventObject.optString("endTime")
                    // ------------------------------------------

                    val id = eventObject.optString("id") // Лучше optString и проверить на null/empty
                    val summary = eventObject.optString("summary", "Без названия")
                    val description = eventObject.optString("description")
                    val location = eventObject.optString("location")

                    // Проверяем наличие обязательных полей
                    if (id.isNullOrEmpty() || startTimeStr.isNullOrEmpty()) {
                        Log.w(TAG, "Skipping event due to missing id or startTime in JSON object: ${eventObject.optString("summary")}")
                        continue // Пропускаем невалидное событие
                    }

                    events.add(
                        CalendarEvent(
                            id = id, // id точно не будет null после проверки выше
                            summary = summary,
                            startTime = startTimeStr, // Передаем строку как есть (может быть дата или дата+время)
                            // Если endTime отсутствует, используем startTime (особенно важно для all-day)
                            endTime = endTimeStr ?: startTimeStr,
                            description = description,
                            location = location,
                            isAllDay = isAllDay // Используем флаг из JSON
                        )
                    )
                } catch (e: JSONException) {
                    Log.e(TAG, "Error parsing individual event object at index $i: ${e.localizedMessage}")
                    // Продолжаем со следующим объектом
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to parse events JSON array", e)
            // Можно вернуть пустой список или пробросить ошибку, если это критично
            // return emptyList()
            throw e // Пробрасываем, чтобы поймать выше
        }
        return events
    }


    fun refreshCurrentVisibleDate() {
        val currentDate = _currentVisibleDate.value
        Log.d(TAG, "Manual refresh triggered for date: $currentDate")
        // Перезагружаем только один день, но используем флаг replace=false,
        // чтобы не сломать большой загруженный диапазон, если он есть.
        // Хотя, возможно, лучше использовать replace=true для гарантии свежести? Зависит от требований.
        // Пока оставим replace=false.
        fetchAndStoreDateRange(currentDate, currentDate, false)
    }

    private fun parseBackendError(responseBody: String?, code: Int): String {
        return try {
            val json = JSONObject(responseBody ?: "{}")
            json.optString("detail", "Ошибка сервера ($code)")
        } catch (e: JSONException) {
            "Ошибка сервера ($code)"
        }
    }



    fun formatEventTimeForDisplay(isoString: String?, isAllDayEvent: Boolean, pattern: String = "HH:mm"): String {
        if (isAllDayEvent) return "Весь день" // Главное изменение - проверяем флаг
        if (isoString.isNullOrBlank()) return "--:--"

        return try {
            val offsetDateTime = OffsetDateTime.parse(isoString)
            val localZoneId = ZoneId.systemDefault()
            val localDateTime = offsetDateTime.atZoneSameInstant(localZoneId)
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale("ru"))
            localDateTime.format(formatter)
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "Error parsing non-all-day time string: $isoString", e)
            "Ошибка времени" // Ошибки парсинга для НЕ all-day событий - это проблема
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting time for display: $isoString", e)
            "Ошибка времени"
        }
    }

    fun formatDisplayDate(isoTimeString: String?): String { // Переименуем для ясности
        if (isoTimeString.isNullOrBlank()) return "Дата не указана"

        return try {
            // Пытаемся распарсить строку. Нам подойдет любой из форматов,
            // содержащих дату (OffsetDateTime или LocalDate).
            val temporalAccessor: java.time.temporal.TemporalAccessor = try {
                OffsetDateTime.parse(isoTimeString)
            } catch (e: DateTimeParseException) {
                // Если не парсится как OffsetDateTime, пробуем как LocalDate
                LocalDate.parse(isoTimeString)
            }

            // Получаем системный часовой пояс для корректного отображения даты
            // (хотя для формата "d MMMM" это может быть не критично, но лучше сделать правильно)
            val localZoneId = ZoneId.systemDefault()
            // Создаем форматтер ТОЛЬКО для даты
            val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))

            // Преобразуем к ZonedDateTime, если это OffsetDateTime, чтобы учесть пояс
            // Или просто форматируем LocalDate
            when (temporalAccessor) {
                is OffsetDateTime -> temporalAccessor.atZoneSameInstant(localZoneId).format(formatter)
                is LocalDate -> temporalAccessor.format(formatter)
                // Добавим LocalDateTime на всякий случай, если вдруг такой формат придет
                is LocalDateTime -> temporalAccessor.atZone(localZoneId).format(formatter)
                else -> {
                    Log.w(TAG, "Unsupported TemporalAccessor type in formatDisplayDate: ${temporalAccessor::class.java}")
                    "Неверный формат даты"
                }
            }

        } catch (e: DateTimeParseException) {
            Log.e(TAG, "Error parsing date string for display date: $isoTimeString", e)
            "Ошибка даты"
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting display date: $isoTimeString", e)
            "Ошибка даты"
        }
    }

    private val timeOnlyFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    fun formatEventListTime(event: CalendarEvent, zoneIdString: String, use12Hour: Boolean): String {
        if (event.isAllDay) return "Весь день"

        val zoneId = try { ZoneId.of(zoneIdString.ifEmpty { ZoneId.systemDefault().id }) }
        catch (e: Exception) { ZoneId.systemDefault() }

        val startInstant = DateTimeUtils.parseToInstant(event.startTime, zoneIdString)
        val endInstant = DateTimeUtils.parseToInstant(event.endTime, zoneIdString)

        fun formatTime(instant: Instant?): String {
            if (instant == null) return ""
            val localTime = instant.atZone(zoneId).toLocalTime()
            val hour = localTime.hour
            val minute = localTime.minute

            return if (use12Hour) {
                val amPm = if (hour < 12) "AM" else "PM"
                val hour12 = if (hour % 12 == 0) 12 else hour % 12
                if (minute == 0)
                    "$hour12 $amPm"
                else
                    String.format("%d:%02d %s", hour12, minute, amPm)
            } else {
                if (minute == 0)
                    String.format("%02d", hour)
                else
                    String.format("%02d:%02d", hour, minute)
            }
        }

        return when {
            startInstant != null && endInstant != null -> {
                "${formatTime(startInstant)} - ${formatTime(endInstant)}"
            }
            startInstant != null -> formatTime(startInstant)
            else -> ""
        }
    }


    private fun ClosedRange<LocalDate>.containsRange(other: ClosedRange<LocalDate>): Boolean {
        return this.start <= other.start && this.endInclusive >= other.endInclusive
    }

    // Объединяет два диапазона, создавая минимальный охватывающий диапазон
    private fun ClosedRange<LocalDate>.union(other: ClosedRange<LocalDate>): ClosedRange<LocalDate> {
        val newStart = minOf(this.start, other.start)
        val newEnd = maxOf(this.endInclusive, other.endInclusive)
        return newStart..newEnd
    }

    // --- Очистка ошибок для нового состояния ---
    fun clearRangeNetworkError() { _rangeNetworkState.update { EventNetworkState.Idle } }

    // Используем новую функцию и в методе для AI Visualizer (если нужно время)




        // --------------- БЛОК АИ
    // --- Отправка Текстового Сообщения ---
    fun sendTextMessage(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "sendTextMessage called with blank text.")
            return
        }
        if (!_uiState.value.isSignedIn || currentIdToken == null) { Log.w(TAG, "Cannot send message: Not signed in or token missing."); return }
        if (_uiState.value.isLoading) { Log.w(TAG, "Cannot send message: Already loading."); return }

        Log.d(TAG, "Attempting to send text message: '$text'")
 //       addChatMessage(text, isUser = true) // <-- Добавляем сообщение пользователя в чат

        viewModelScope.launch {
            withContext(Dispatchers.Main.immediate) {
                _uiState.update { it.copy(isLoading = true, message = "Отправка...") }
                // Set AI state to THINKING immediately for text input too
                _aiState.value = AiVisualizerState.THINKING
                _aiMessage.value = null
            }
            sendTextToServer(text = text)
        }
    }

    fun updateBotTemperSetting(newTemper: String) {
        viewModelScope.launch {
            settingsRepository.saveBotTemper(newTemper)
            // Опционально: показать сообщение об успехе
            // _uiState.update { it.copy(message = "Настройка поведения сохранена") }
        }
    }

    private fun initializeSpeechRecognizer() {
        viewModelScope.launch(Dispatchers.Main) {
            // Используем context для создания
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(recognitionListener)
            Log.d(TAG, "SpeechRecognizer initialized.")
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "SpeechRecognizer: onReadyForSpeech")
            // Уже обновили UI в startListening
            recognitionSuccessful = false // Сбрасываем флаг перед началом
            _uiState.update { it.copy(message = "Слушаю...") } // Обновляем сообщение
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "SpeechRecognizer: onBeginningOfSpeech")
            _uiState.update { it.copy(message = "Говорите...") } // Можно менять сообщение
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Можно использовать для визуализации уровня громкости (например, анимировать иконку)
            // Log.v(TAG, "SpeechRecognizer: onRmsChanged: $rmsdB")
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            Log.v(TAG, "SpeechRecognizer: onBufferReceived")
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "SpeechRecognizer: onEndOfSpeech")
            // Пользователь закончил говорить, ждем результатов
            _uiState.update { it.copy(isListening = false, isLoading = true, message = "Обработка...") } // Переходим в isLoading
            _aiState.value = AiVisualizerState.THINKING // Меняем состояние AI
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Ошибка аудио"
                SpeechRecognizer.ERROR_CLIENT -> "Ошибка клиента"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет разрешений"
                SpeechRecognizer.ERROR_NETWORK -> "Ошибка сети"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Таймаут сети"
                SpeechRecognizer.ERROR_NO_MATCH -> "Ничего не распознано"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознаватель занят"
                SpeechRecognizer.ERROR_SERVER -> "Ошибка сервера распознавания"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Время вышло, попробуйте снова"
                else -> "Неизвестная ошибка распознавания ($error)"
            }
            Log.e(TAG, "SpeechRecognizer: onError: $errorMessage")

            // Сбрасываем состояние UI и AI
            _uiState.update { it.copy(isListening = false, isLoading = false, showGeneralError = errorMessage, message = null) }
            _aiState.value = AiVisualizerState.IDLE

            // Если была ошибка ERROR_NO_MATCH или ERROR_SPEECH_TIMEOUT, возможно, не нужно останавливать явно
            // stopListeningInternal() // Можно вызвать для надежности
        }

        override fun onResults(results: Bundle?) {
            Log.d(TAG, "SpeechRecognizer: onResults")
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            _aiState.value = AiVisualizerState.THINKING
            _uiState.update { it.copy(isListening = false, isLoading = true, message = "Обработка...") }
            if (matches != null && matches.isNotEmpty()) {
                val recognizedText = matches[0] // Берем самый вероятный результат
                Log.i(TAG, "SpeechRecognizer: Recognized text: '$recognizedText'")
                recognitionSuccessful = true // Успех

                // --- Вот здесь отправляем текст на бэкенд ---
                viewModelScope.launch {
                    sendTextToServer(recognizedText) // Используем существующую функцию!
                }
            } else {
                Log.w(TAG, "SpeechRecognizer: No recognition results.")
                recognitionSuccessful = false
                // По идее, если нет результатов, должен был сработать onError(NO_MATCH)
                // Но на всякий случай сбросим состояние здесь тоже
                _uiState.update { it.copy(isListening = false, isLoading = false, message = "Ничего не распознано") }
                _aiState.value = AiVisualizerState.IDLE
            }
            // stopListeningInternal() // Вызов stopListening() здесь обычно не нужен, т.к. onResults - финальный колбек
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches != null && matches.isNotEmpty()) {
                val partialText = matches[0]
                Log.d(TAG, "SpeechRecognizer: Partial result: '$partialText'")
                // Можно обновить UI, чтобы показывать текст по мере распознавания
                _uiState.update { it.copy(message = partialText) }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d(TAG, "SpeechRecognizer: onEvent $eventType")
        }
    }

    fun startListening() {
        if (_uiState.value.isListening) { Log.w(TAG, "Already listening."); return }
        // Убрали проверку токена отсюда, она будет перед отправкой текста
        if (!_uiState.value.isPermissionGranted) { Log.w(TAG, "Cannot listen: Permission not granted."); return }
        if (_uiState.value.isLoading) { Log.w(TAG, "Cannot listen: App is busy (isLoading)."); return }

        if (speechRecognizer == null) {
            Log.e(TAG, "Cannot start listening: SpeechRecognizer is null.")
            initializeSpeechRecognizer() // Попробуем инициализировать снова
            _uiState.update { it.copy(showGeneralError = "Ошибка инициализации распознавания") }
            return
        }

        Log.d(TAG, "Starting speech recognition listening...")
        viewModelScope.launch(Dispatchers.Main) { // startListening нужно вызывать на Main
            try {
                _uiState.update { it.copy(isListening = true, message = "Инициализация...") }
                _aiState.value = AiVisualizerState.LISTENING // Новое состояние для визуала
                _aiMessage.value = null // Clear any previous message
                speechRecognizer?.startListening(speechRecognizerIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting listening", e)
                _uiState.update { it.copy(isListening = false, showGeneralError = "Ошибка начала распознавания: ${e.message}") }
                _aiState.value = AiVisualizerState.IDLE
            }
        }
    }

    fun stopListening() {
        if (!_uiState.value.isListening) { Log.w(TAG, "Not listening, cannot stop."); return }
        Log.d(TAG, "Stopping speech recognition listening (user action)...")
        stopListeningInternal()

        // Важно: Мы вызываем stopListening(), но результат придет асинхронно в onResults или onError.
        // UI частично обновится в onEndOfSpeech (isLoading=true), а финально - после ответа сервера или в onError.
        // Не нужно здесь менять isLoading или aiState на IDLE, это сделают колбеки.
    }

    private fun stopListeningInternal() {
        viewModelScope.launch(Dispatchers.Main) {
            speechRecognizer?.stopListening()
            Log.d(TAG, "Called speechRecognizer.stopListening()")
            // Не меняем isListening здесь, пусть это делают колбеки (onEndOfSpeech/onError)
            // _uiState.update { it.copy(isListening = false) } // <- Не здесь
        }
    }



    // Возвращает true при успехе, false при ошибке
    private suspend fun sendAuthInfoToBackend(idToken: String, authCode: String): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Sending Auth Info (JSON) to /auth/google/exchange")
        val jsonObject = JSONObject().apply {
            put("id_token", idToken)
            put("auth_code", authCode)
        }
        val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("$backendBaseUrl/auth/google/exchange")
            .post(requestBody)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response -> // Используем execute для suspend функции
                val responseBodyString = response.body?.string() // Читаем тело ОДИН РАЗ
                if (!response.isSuccessful) {
                    Log.e(TAG, "Backend error exchanging code (JSON): ${response.code} - $responseBodyString")
                    // Обновляем UI с ошибкой на главном потоке
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(showAuthError = "Ошибка сервера при обмене токенов: ${response.code}", isLoading = false) }
                    }
                    return@withContext false // Неудача
                } else {
                    Log.i(TAG, "Backend successfully exchanged tokens (JSON). Response: $responseBodyString")
                    // Можно обновить UI здесь же или просто вернуть true
                    withContext(Dispatchers.Main) {
                        // Не обновляем сообщение здесь, это сделает handleSignInResult
                        // _uiState.update { it.copy(message = "Авторизация календаря успешна!", isLoading = false) }
                    }
                    return@withContext true // Успех
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error sending auth info (JSON)", e)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(showAuthError = "Сетевая ошибка обмена токенов: ${e.message}", isLoading = false) }
            }
            return@withContext false // Неудача
        } catch (e: Exception) { // Ловим другие возможные ошибки (e.g., JSON parsing)
            Log.e(TAG, "Error processing backend response for auth exchange", e)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(showAuthError = "Ошибка обработки ответа сервера: ${e.message}", isLoading = false) }
            }
            return@withContext false // Неудача
        }
    }

    // Отправка Текста (теперь использует multipart и вызывает executeProcessRequest)
    private suspend fun sendTextToServer(text: String) {
        val freshToken = getFreshIdToken()
        val currentDateTime = LocalDateTime.now()
        val timeZone = ZoneId.systemDefault()
        val currentTemper = botTemperState.value

        if (freshToken == null) {
            Log.w(TAG, "sendTextToServer failed: Could not get fresh token.")
            // UI уже должен быть обновлен в getFreshIdToken или signOutInternally
            _uiState.update { it.copy(isLoading = false) } // Убедимся, что isLoading сброшен
            _aiState.value = AiVisualizerState.IDLE // Сбросим состояние AI
            return
        }

        Log.i(TAG, "Sending text and FRESH ID token to /process")
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("text", text)
            .addFormDataPart("time", currentDateTime.toString())
            .addFormDataPart("timeZone", timeZone.toString())
            .addFormDataPart("temper", currentTemper)
            .build()

        val request = Request.Builder()
            .url("$backendBaseUrl/process")
            .header("Authorization", "Bearer $freshToken")
            .post(requestBody)
            .build()

        executeProcessRequest(request)
    }


    private suspend fun executeProcessRequest(request: Request) = withContext(Dispatchers.IO) {
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val responseBodyString = response.body?.string()
                withContext(Dispatchers.Main) {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Server error processing request: ${response.code} - $responseBodyString")
                        var errorDetail = "Ошибка сервера (${response.code})"
                        try {
                            val jsonError = JSONObject(responseBodyString ?: "{}")
                            errorDetail = jsonError.optString("detail", errorDetail)
                        } catch (_: Exception) {}
                        // Update general state AND revert AI state
                        _uiState.update { it.copy(showGeneralError = errorDetail, isLoading = false, message = "Ошибка обработки") }
                        _aiState.value = AiVisualizerState.IDLE // Revert on network/server error
                        _aiMessage.value = null
                    } else {
                        Log.i(TAG, "Request processed successfully. Response: $responseBodyString")
                        // AI state is THINKING here, handleProcessResponse will change it
                        handleProcessResponse(responseBodyString)
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during /process request", e)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(showGeneralError = "Сетевая ошибка: ${e.message}", isLoading = false, message = "Ошибка сети") }
                _aiState.value = AiVisualizerState.IDLE // Revert on network/server error
                _aiMessage.value = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing /process request", e)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(showGeneralError = "Ошибка обработки запроса: ${e.message}", isLoading = false, message = "Ошибка") }
                _aiState.value = AiVisualizerState.IDLE // Revert on network/server error
                _aiMessage.value = null
            }
        } finally {
            // Ensure isLoading is false if an exception occurs before response handling
            if (_uiState.value.isLoading && _aiState.value != AiVisualizerState.THINKING) {
                withContext(Dispatchers.Main.immediate) { // Use immediate to avoid race conditions
                    if (_uiState.value.isLoading) { // Double check
                        _uiState.update { it.copy(isLoading = false) }
                        Log.w(TAG,"Reset isLoading flag in executeProcessRequest finally block")
                    }
                }
            }
        }
    }


    // --- Новый Обработчик Ответов от /process ---
    // Эта функция должна вызываться в Main потоке, т.к. обновляет UI
    private fun handleProcessResponse(responseBody: String?) {
        var finalAiState = AiVisualizerState.IDLE // Default to IDLE if processing finishes or errors out badly
        var messageForVisualizer: String? = null
        var statusMessage = "Готово."
        var errorToShow: String? = null

        if (responseBody.isNullOrBlank()) {
            errorToShow = "Пустой ответ от сервера."
            statusMessage = "Ошибка ответа"
            finalAiState = AiVisualizerState.ERROR // Or IDLE
        } else {
            try {
                val json = JSONObject(responseBody)
                val status = json.optString("status")
                val message = json.optString("message")

                when (status) {
                    "success" -> {
                        statusMessage = "Создал как вы и просили" // Shorter status
                        // Format message for visualizer
                        messageForVisualizer = "Success!"
                        finalAiState = AiVisualizerState.RESULT
                        Log.i(TAG, "Event creation successful.")
                        ensureDateRangeLoadedAround(_currentVisibleDate.value)
                        viewModelScope.launch { // Запускаем в корутине, чтобы не блокировать
                            refreshCurrentVisibleDate() // Вызываем функцию обновления текущего дня
                            Log.d(TAG, "Calendar refresh triggered after successful LLM processing.")
                        }
                    }
                    "clarification_needed" -> {
                        statusMessage = "Требуется уточнение"
                        messageForVisualizer = message // Use the backend message directly
                        finalAiState = AiVisualizerState.ASKING
                        Log.i(TAG, "Clarification needed: $messageForVisualizer")
                    }
                    "info", "unsupported" -> { // Treat info like a result/message
                        statusMessage = message // Use backend message as status
                        messageForVisualizer = message
                        finalAiState = AiVisualizerState.RESULT // Show info like a result
                        Log.i(TAG, "Backend info/unsupported response: $message")
                    }
                    "error" -> {
                        errorToShow = message
                        statusMessage = "Ошибка обработки"
                        finalAiState = AiVisualizerState.ERROR // Or IDLE
                        Log.e(TAG, "Backend processing error: $errorToShow")
                    }
                    else -> {
                        errorToShow = "Неизвестный статус ответа: '$status'"
                        statusMessage = "Ошибка ответа"
                        finalAiState = AiVisualizerState.ERROR // Or IDLE
                        Log.w(TAG, "Unknown status from backend: $status")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing /process response", e)
                errorToShow = "Ошибка парсинга ответа: ${e.message}"
                statusMessage = "Ошибка ответа"
                finalAiState = AiVisualizerState.ERROR // Or IDLE
            }
        }

        // --- Single UI Update ---
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                message = statusMessage,
                showGeneralError = errorToShow
                // REMOVED: chatHistory update
            )
        }
        // Update AI Visualizer state AFTER uiState update
        _aiMessage.value = messageForVisualizer
        _aiState.value = if (finalAiState == AiVisualizerState.ERROR) AiVisualizerState.IDLE else finalAiState // Go IDLE on error
        // If there was an error, maybe flash ERROR state briefly? For now, go IDLE.

        // If we went back to IDLE or showed an error, clear the message
        if (_aiState.value == AiVisualizerState.IDLE) {
            _aiMessage.value = null
        }
        // isRotating is derived automatically from aiState, no need to set it here.
    }

    fun resetAiStateAfterResult() {
        // Проверяем, что текущее состояние все еще RESULT (или ASKING, если для него тоже нужен сброс)
        // чтобы не сбросить случайно другое состояние, если оно изменилось во время задержки
        if (_aiState.value == AiVisualizerState.RESULT) {
            _aiState.value = AiVisualizerState.IDLE
            _aiMessage.value = null // Очищаем сообщение при переходе в IDLE
            Log.d("MainViewModel", "Resetting AI state to IDLE after result timeout.")
        }
    }
    fun resetAiStateAfterAsking() {
        // Проверяем, что текущее состояние все еще RESULT (или ASKING, если для него тоже нужен сброс)
        // чтобы не сбросить случайно другое состояние, если оно изменилось во время задержки
        if (_aiState.value == AiVisualizerState.ASKING) {
            _aiState.value = AiVisualizerState.IDLE
            _aiMessage.value = null // Очищаем сообщение при переходе в IDLE
            Log.d("MainViewModel", "Resetting AI state to IDLE after result timeout.")
        }
    }


    fun clearGeneralError() {
        _uiState.update { it.copy(showGeneralError = null) }
    }
    // Optionally reset AI state if an error message is cleared
    // if (_aiState.value == AiVisualizerState.ERROR) { // Or just always reset?
    //     _aiState.value = AiVisualizerState.IDLE
    //     _aiMessage.value = null
    // }

    fun clearAuthError() {
        _uiState.update { it.copy(showAuthError = null) }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.Main) { // И уничтожаться на Main
            speechRecognizer?.destroy()
            speechRecognizer = null
            Log.d(TAG, "SpeechRecognizer destroyed.")
        }
    }


    /// GOOGLE ВХОД И ТОКЕНЫ

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    // Вызывается после получения результата от Google Sign-In Activity
    fun handleSignInResult(completedTask: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        viewModelScope.launch {
            try {
                val account = completedTask.getResult(ApiException::class.java)
                val idToken = account?.idToken
                val serverAuthCode = account?.serverAuthCode
                val userEmail = account?.email

                Log.i(TAG, "Sign-In Success! Email: $userEmail")
                Log.d(TAG, "ID Token received: ${idToken != null}")
                Log.d(TAG, "Server Auth Code received: ${serverAuthCode != null}")

                if (idToken != null && serverAuthCode != null) {
                    currentIdToken = idToken // Сохраняем токен
                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            message = "Вход выполнен: $userEmail. Авторизация календаря...",
                            showAuthError = null
                        )
                    }
                    // Отправляем данные на бэкенд
                    val exchangeSuccess = sendAuthInfoToBackend(idToken, serverAuthCode)
                    if (exchangeSuccess) {
                        _uiState.update {
                            it.copy(
                                isSignedIn = true,
                                userEmail = userEmail,
                                message = "Авторизация успешна! Готово к записи.",
                                isLoading = false
                            )
                        }
                        ensureDateRangeLoadedAround(_currentVisibleDate.value)
                    } else {
                        // Ошибка обмена, сбрасываем состояние
                        signOutInternally("Ошибка авторизации на сервере.")
                    }
                } else {
                    Log.w(TAG, "ID Token or Server Auth Code is null after sign-in.")
                    signOutInternally("Не удалось получить токен/код от Google.")
                }

            } catch (e: ApiException) {
                Log.w(TAG, "signInResult:failed code=" + e.statusCode, e)
                signOutInternally("Ошибка входа Google: ${e.statusCode}")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling sign in result", e)
                signOutInternally("Неизвестная ошибка при входе: ${e.message}")
            }
        }
    }

    // Обработка ошибок аутентификации (внутренняя)
    private fun signOutInternally(authError: String) {
        currentIdToken = null
        _uiState.update {
            it.copy(
                isSignedIn = false,
                isLoading = false,
                userEmail = null,
                message = "Требуется вход.",
                isListening = false,
                showAuthError = authError // Показываем ошибку
            )
        }
    }

    private suspend fun getFreshIdToken(): String? {
        Log.d(TAG, "Attempting to get fresh ID token via silent sign-in...")
        return try {
            val account = googleSignInClient.silentSignIn().await() // await() вместо addOnCompleteListener
            val freshToken = account?.idToken
            if (freshToken != null) {
                Log.i(TAG, "Silent sign-in successful, got fresh token for: ${account.email}")
                currentIdToken = freshToken // Обновляем сохраненный токен
                // Убедимся, что UI тоже в актуальном состоянии (на случай, если silentSignIn сработал после ошибки)
                _uiState.update {
                    it.copy(
                        isSignedIn = true,
                        userEmail = account.email,
                        message = "Аккаунт: ${account.email} (Авторизован)",
                        isLoading = it.isLoading, // Не меняем isLoading здесь
                        showAuthError = null
                    )
                }
                freshToken
            } else {
                Log.w(TAG, "Silent sign-in successful but ID token is null.")
                signOutInternally("Ошибка: Не удалось получить токен после тихого входа.")
                null
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Silent sign-in failed: ${e.statusCode}", e)
            // Частые коды: SIGN_IN_REQUIRED (нужен явный вход), RESOLUTION_REQUIRED
            signOutInternally("Сессия истекла или отозвана. Требуется вход.") // Важно сбросить состояние
            null // Возвращаем null, сигнализируя о неудаче
        } catch (e: Exception) {
            Log.e(TAG, "Silent sign-in failed with generic exception", e)
            // Оставляем пользователя "залогиненным" в UI, но токен получить не можем
            // Возможно, временная сетевая проблема. Не делаем signOutInternally сразу.
            _uiState.update { it.copy(showGeneralError = "Не удалось обновить сессию: ${e.message}") }
            null // Возвращаем null
        }
    }

    // Вызывается при нажатии кнопки выхода
    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "Выход...") }
            try {
                googleSignInClient.signOut().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "User signed out successfully from Google.")
                        signOutInternally("Вы успешно вышли.") // Используем общий метод сброса
                        // Опционально: уведомить бэкенд об выходе
                    } else {
                        Log.w(TAG, "Google Sign out failed.", task.exception)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                showGeneralError = "Не удалось выйти из Google аккаунта."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during sign out", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showGeneralError = "Ошибка при выходе: ${e.message}"
                    )
                }
            }
        }
    }

    private fun checkInitialAuthState() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Checking initial auth state via silent sign-in...")
                // Пытаемся войти тихо и получить СВЕЖИЙ токен
                val account = googleSignInClient.silentSignIn().await()
                val idToken = account?.idToken
                val serverAuthCode = account?.serverAuthCode // Получаем auth code снова, если нужен
                val userEmail = account?.email

                if (idToken != null && userEmail != null /* && serverAuthCode != null - возможно, он не нужен здесь? */) {
                    Log.i(TAG, "Silent sign-in success, processing result for: $userEmail")
                    currentIdToken = idToken // Сохраняем свежий токен
                    // Обновляем UI, показывая, что вход выполнен
                    _uiState.update {
                        it.copy(
                            isSignedIn = true,
                            userEmail = userEmail,
                            message = "Аккаунт: $userEmail (Авторизован)",
                            isLoading = false,
                            showAuthError = null
                        )
                    }
                    ensureDateRangeLoadedAround(_currentVisibleDate.value)
                    // НЕ НУЖНО снова вызывать sendAuthInfoToBackend, если пользователь уже был авторизован
                    // на бэкенде. Тихого входа достаточно для обновления idToken на клиенте.
                    // Если нужно проверить связь с бэкендом, можно сделать отдельный "ping" или "getUserInfo" запрос.

                } else {
                    // Silent sign-in не вернул аккаунт или токен
                    Log.i(TAG, "Silent sign-in did not return a valid account/token.")
                    signOutInternally("Требуется вход или обновление сессии.") // Считаем не вошедшим
                }

            } catch (e: ApiException) {
                Log.w(TAG, "Silent sign-in failed: ${e.statusCode}", e)
                signOutInternally("Требуется вход (ошибка ${e.statusCode})") // Считаем не вошедшим
            } catch (e: Exception) {
                Log.e(TAG, "Error checking initial auth state", e)
                signOutInternally("Ошибка проверки аккаунта: ${e.message}") // Считаем не вошедшим
            }
        }
    }
    fun updatePermissionStatus(isGranted: Boolean) {
        if (_uiState.value.isPermissionGranted != isGranted) { // Обновляем только если изменилось
            _uiState.update { it.copy(isPermissionGranted = isGranted) }
            Log.d(TAG, "Audio permission status updated to: $isGranted")
        }
    }


}


