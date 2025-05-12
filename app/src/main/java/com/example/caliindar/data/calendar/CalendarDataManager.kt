package com.example.caliindar.data.calendar

import android.util.Log
import com.example.caliindar.data.auth.AuthManager // Нужен для токена
import com.example.caliindar.data.local.CalendarEventEntity
import com.example.caliindar.data.local.EventDao
import com.example.caliindar.data.mapper.EventMapper
import com.example.caliindar.data.repo.SettingsRepository
import com.example.caliindar.di.BackendUrl
import com.example.caliindar.di.IoDispatcher
import com.example.caliindar.ui.screens.main.CalendarEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException


@Singleton
class CalendarDataManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val eventDao: EventDao,
    private val authManager: AuthManager, // Зависимость от AuthManager для токена
    @BackendUrl private val backendBaseUrl: String,
    private val settingsRepository: SettingsRepository, // Для таймзоны
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher // Внедряем IO диспатчер
) {
    private val TAG = "CalendarDataManager"
    private val managerScope = CoroutineScope(SupervisorJob() + ioDispatcher) // Свой скоуп

    // --- Константы ---
    companion object {
        const val PREFETCH_DAYS_FORWARD = 5
        const val PREFETCH_DAYS_BACKWARD = 3
        const val TRIGGER_PREFETCH_THRESHOLD_FORWARD = 2
        const val TRIGGER_PREFETCH_THRESHOLD_BACKWARD = 1
        const val JUMP_DETECTION_BUFFER_DAYS = 10
    }

    // --- Внутренние состояния ---
    private val _currentVisibleDate = MutableStateFlow(LocalDate.now())
    private val _loadedDateRange = MutableStateFlow<ClosedRange<LocalDate>?>(null)
    private val _rangeNetworkState = MutableStateFlow<EventNetworkState>(EventNetworkState.Idle)
    private val _createEventResult = MutableStateFlow<CreateEventResult>(CreateEventResult.Idle)
    private val _deleteEventResult = MutableStateFlow<DeleteEventResult>(DeleteEventResult.Idle)

    // --- Публичные StateFlow для ViewModel ---
    val currentVisibleDate: StateFlow<LocalDate> = _currentVisibleDate.asStateFlow()
    val loadedDateRange: StateFlow<ClosedRange<LocalDate>?> = _loadedDateRange.asStateFlow()
    val rangeNetworkState: StateFlow<EventNetworkState> = _rangeNetworkState.asStateFlow()
    val createEventResult: StateFlow<CreateEventResult> = _createEventResult.asStateFlow()

    val deleteEventResult: StateFlow<DeleteEventResult> = _deleteEventResult.asStateFlow()

    private var activeFetchJob: Job? = null

    // --- Публичные методы для ViewModel ---

    /** Устанавливает текущую видимую дату и запускает проверку/загрузку диапазона */
    fun setCurrentVisibleDate(newDate: LocalDate, forceRefresh: Boolean = false) {
        Log.d(TAG, "setCurrentVisibleDate: CALLED with $newDate. Current value: ${_currentVisibleDate.value}")

        // Only proceed if date is different OR range is not loaded yet
        val needsDateUpdate = newDate != _currentVisibleDate.value
        val needsRangeCheck = _loadedDateRange.value == null || forceRefresh || needsDateUpdate
        if (!needsDateUpdate && !forceRefresh && _loadedDateRange.value != null) {
            Log.d(TAG, "setCurrentVisibleDate: Date $newDate is same, no forceRefresh, range loaded. Skipping.")
            return
        }

        Log.i(TAG, "setCurrentVisibleDate: Proceeding to set date and check range for $newDate")

        // --- Explicit Cancellation ---
        activeFetchJob?.cancel(CancellationException("New date set: $newDate, forceRefresh=$forceRefresh"))
        Log.d(TAG, "setCurrentVisibleDate: Previous activeFetchJob cancelled (if existed).")
        // --------------------------

        if (needsDateUpdate) {
            _currentVisibleDate.value = newDate
        }

        // --- ВАЖНО: ПРОВЕРКА АУТЕНТИФИКАЦИИ ПЕРЕД ЗАПУСКОМ ЗАГРУЗКИ ---
        if (authManager.authState.value.isSignedIn) {
            // Запускаем корутину ТОЛЬКО если пользователь вошел
            activeFetchJob = managerScope.launch {
                Log.d(TAG, "setCurrentVisibleDate: New coroutine launched for ensureDateRangeLoadedAround($newDate, forceRefresh=$forceRefresh). Job: ${coroutineContext[Job]}")
                try {
                    ensureDateRangeLoadedAround(newDate, forceRefresh)
                } catch (ce: CancellationException) {
                    Log.d(TAG, "setCurrentVisibleDate: Job ${coroutineContext[Job]} cancelled while ensuring range for $newDate: ${ce.message}")
                    throw ce // Перебрасываем отмену
                } catch (e: Exception) {
                    Log.e(TAG, "setCurrentVisibleDate: Error in ensureDateRangeLoadedAround for $newDate", e)
                    // Возможно, обновить _rangeNetworkState здесь, если ensureDateRangeLoadedAround не сделает этого
                } finally {
                    Log.d(TAG, "setCurrentVisibleDate: Job ${coroutineContext[Job]} finished for $newDate")
                }
            }
            Log.d(TAG, "setCurrentVisibleDate: Assigned new activeFetchJob: ${activeFetchJob}")
        } else {
            // Пользователь не вошел - не запускаем загрузку
            Log.w(TAG, "setCurrentVisibleDate: Skipping range load for $newDate because user is not signed in.")
            // Опционально: установить состояние ошибки или ожидания входа
            // _rangeNetworkState.value = EventNetworkState.Error("Требуется вход")
            activeFetchJob = null // Убедимся, что нет активной задачи
        }
    }

    /** Предоставляет Flow событий из БД для указанной даты */
    fun getEventsFlowForDate(date: LocalDate): Flow<List<CalendarEvent>> {
        val startOfDayUTC = date.atStartOfDay(ZoneOffset.UTC)
        val startMillis = startOfDayUTC.toInstant().toEpochMilli()
        val endMillis = startOfDayUTC.plusDays(1).toInstant().toEpochMilli()
        // Комбинируем Flow из DAO и Flow таймзоны из настроек
        return combine(
            eventDao.getEventsForDateRangeFlow(startMillis, endMillis),
            settingsRepository.timeZoneFlow // Получаем Flow таймзоны
        ) { entityList, timeZoneIdString ->
            // Получаем ZoneId внутри combine, чтобы использовать актуальное значение
            val zoneId = try { ZoneId.of(timeZoneIdString.ifEmpty { ZoneId.systemDefault().id }) }
            catch (e: Exception) { ZoneId.systemDefault() }

            // --- Логика из ViewModel ---
            entityList.filter { entity: CalendarEventEntity -> // Указываем тип entity
                // Используем startTimeMillis, endTimeMillis, isAllDay из ТВОЕГО CalendarEventEntity
                if (!entity.isAllDay) {
                    // Оригинальная логика для не-all-day: событие должно заканчиваться до начала следующего дня
                    entity.endTimeMillis < endMillis
                    // ЗАМЕЧАНИЕ: Эта логика может не показывать события, которые *пересекают* полночь,
                    // но заканчиваются позже начала следующего дня. Если это не то, что нужно,
                    // стандартная проверка на пересечение: entity.startTimeMillis < endMillis && entity.endTimeMillis > startMillis
                } else {
                    // Оригинальная логика для all-day: длительность примерно 1 день
                    val durationMillis = entity.endTimeMillis - entity.startTimeMillis
                    val twentyFourHoursMillis = TimeUnit.HOURS.toMillis(24)
                    val toleranceMillis = TimeUnit.MINUTES.toMillis(5)
                    (durationMillis >= twentyFourHoursMillis - toleranceMillis) && (durationMillis <= twentyFourHoursMillis + toleranceMillis)
                }
            } // Конец filter
                .map { filteredEntity ->
                    // Маппим только отфильтрованные entity
                    EventMapper.mapToDomain(filteredEntity, zoneId.toString()) // Используем актуальный zoneId
                } // Конец mapNotNull
            // --- Конец логики из ViewModel ---
        } // Конец combine
            .catch { e ->
                Log.e(TAG, "Error processing events Flow for date $date", e)
                emit(emptyList<CalendarEvent>())
            }
            .flowOn(ioDispatcher) // Выполняем combine, filter, map, catch на IO потоке
    }


    /** Создает новое событие через бэкенд */
    suspend fun createEvent(
        summary: String,
        startTimeString: String, // <-- Возвращаем строковое поле
        endTimeString: String,   // <-- Возвращаем строковое поле
        isAllDay: Boolean,       // <-- Возвращаем булево поле
        timeZoneId: String?,     // <-- Передаем ID таймзоны (может быть null для all day)
        description: String?,
        location: String?,
        recurrenceRule: String?
    ) { // Теперь suspend
        if (summary.isBlank()) {
            _createEventResult.value = CreateEventResult.Error("Название не может быть пустым")
            return
        }
        if (startTimeString.isBlank() || endTimeString.isBlank()) {
            _createEventResult.value = CreateEventResult.Error("Время начала и конца должны быть указаны")
            return
        }

        _createEventResult.value = CreateEventResult.Loading

        // Получаем токен через AuthManager
        val freshToken = authManager.getFreshIdToken()
        if (freshToken == null) {
            Log.w(TAG, "Cannot create event: Failed to get fresh ID token.")
            _createEventResult.value = CreateEventResult.Error("Ошибка аутентификации")
            return // Выход, если нет токена
        }

        val requestBody = try {
            JSONObject().apply {
                put("summary", summary)
                // --- ВОЗВРАЩАЕМ ПОЛЯ В КОРЕНЬ ---
                put("startTime", startTimeString) // Строка (date или dateTime)
                put("endTime", endTimeString)   // Строка (date или dateTime)
                put("isAllDay", isAllDay)       // Булево значение
                // Таймзона нужна бэкенду для формирования запроса к Google
                if (!isAllDay && timeZoneId != null) {
                    put("timeZoneId", timeZoneId) // Отправляем ID таймзоны
                } else if (!isAllDay && timeZoneId == null) {
                    Log.w(TAG, "Sending timed event without timeZoneId to backend!")
                    // Бэкенд должен будет обработать этот случай или вернуть ошибку
                }
                // --- КОНЕЦ ВОЗВРАЩЕНИЯ ПОЛЕЙ ---

                description?.takeIf { it.isNotBlank() }?.let { put("description", it) }
                location?.takeIf { it.isNotBlank() }?.let { put("location", it) }

                // --- Добавляем recurrence (как и раньше) ---
                recurrenceRule?.takeIf { it.isNotBlank() }?.let { ruleString ->
                    val fullRuleString = "RRULE:$ruleString"
                    val recurrenceArray = JSONArray().apply { put(fullRuleString) }
                    // Убедитесь, что ключ "recurrence" ожидает ваш бэкенд
                    put("recurrence", recurrenceArray)
                }

            }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        } catch (e: JSONException) {
            Log.e(TAG, "Error creating JSON for new event", e)
            _createEventResult.value = CreateEventResult.Error("Ошибка подготовки данных")
            return
        }

        val request = Request.Builder()
            .url("$backendBaseUrl/calendar/events")
            .header("Authorization", "Bearer $freshToken") // Используем полученный токен
            .post(requestBody)
            .build()

        try {
            // Выполняем запрос в IO dispatcher
            val response = withContext(ioDispatcher) {
                okHttpClient.newCall(request).execute()
            }
            if (response.isSuccessful) {
                Log.i(TAG, "Event created successfully via backend.")
                _createEventResult.value = CreateEventResult.Success
                // Обновляем данные для текущего видимого дня после успеха
                refreshDate(_currentVisibleDate.value) // Используем новую функцию refreshDate
            } else {
                val errorMsg = parseBackendError(response.body?.string(), response.code)
                Log.e(TAG, "Error creating event via backend: ${response.code} - $errorMsg")
                _createEventResult.value = CreateEventResult.Error(errorMsg)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error creating event", e)
            _createEventResult.value = CreateEventResult.Error("Сетевая ошибка: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating event", e)
            _createEventResult.value = CreateEventResult.Error("Неизвестная ошибка: ${e.message}")
        } finally {
            // Сбросить состояние Loading, если Success/Error не установились (маловероятно, но для надежности)
            if (_createEventResult.value is CreateEventResult.Loading) {
                _createEventResult.value = CreateEventResult.Idle
            }
        }
    }

    /** Сбрасывает состояние результата создания события */
    fun consumeCreateEventResult() {
        _createEventResult.value = CreateEventResult.Idle
    }

    /**
     * Запускает процесс удаления события на бэкенде и в локальной БД.
     * Результат операции будет доступен через [deleteEventResult] StateFlow.
     *
     * @param eventId Уникальный идентификатор события для удаления.
     */
    suspend fun deleteEvent(eventId: String) {
        // Проверяем, не идет ли уже удаление (простая блокировка)
        if (_deleteEventResult.value is DeleteEventResult.Loading) {
            Log.w(TAG, "deleteEvent called while another deletion is in progress for ID: $eventId. Ignoring.")
            return // Можно вернуть ошибку или просто игнорировать
        }

        _deleteEventResult.value = DeleteEventResult.Loading
        Log.i(TAG, "Attempting to delete event with ID: $eventId")

        // Получаем токен
        val freshToken = authManager.getFreshIdToken()
        if (freshToken == null) {
            Log.e(TAG, "Cannot delete event $eventId: Failed to get fresh ID token.")
            _deleteEventResult.value = DeleteEventResult.Error("Ошибка аутентификации")
            return
        }

        // Формируем запрос
        val url = "$backendBaseUrl/calendar/events/$eventId"
        val request = Request.Builder()
            .url(url)
            .delete() // Используем метод DELETE
            .header("Authorization", "Bearer $freshToken")
            .build()

        try {
            // Выполняем запрос в IO dispatcher
            val response = withContext(ioDispatcher) {
                okHttpClient.newCall(request).execute()
            }

            if (response.isSuccessful) { // Успех, если код 2xx (особенно 204 No Content)
                Log.i(TAG, "Event $eventId successfully deleted on backend (Code: ${response.code}). Deleting locally.")
                // Удаляем из локальной БД ТОЛЬКО после успешного удаления на бэкенде
                try {
                    withContext(ioDispatcher) {
                        eventDao.deleteEventById(eventId)
                    }
                    Log.i(TAG, "Event $eventId successfully deleted from local DB.")
                    _deleteEventResult.value = DeleteEventResult.Success
                } catch (dbError: Exception) {
                    Log.e(TAG, "Failed to delete event $eventId from local DB after successful backend deletion.", dbError)
                    // Бэкенд удалил, но локально не смогли. Сложная ситуация.
                    // Можно сообщить об ошибке, но событие уже удалено на сервере.
                    _deleteEventResult.value = DeleteEventResult.Error("Ошибка синхронизации: событие удалено на сервере, но не локально.")
                }
            } else {
                // Ошибка от бэкенда
                val errorMsg = parseBackendError(response.body?.string(), response.code)
                Log.e(TAG, "Error deleting event $eventId via backend: ${response.code} - $errorMsg")
                _deleteEventResult.value = DeleteEventResult.Error(errorMsg) // Используем распарсенное сообщение
            }

        } catch (e: IOException) {
            Log.e(TAG, "Network error deleting event $eventId", e)
            _deleteEventResult.value = DeleteEventResult.Error("Сетевая ошибка: ${e.message}")
        } catch (e: Exception) {
            // Ловим другие возможные ошибки (например, CancellationException)
            if (e is CancellationException) {
                Log.w(TAG, "Delete event $eventId job was cancelled.", e)
                _deleteEventResult.value = DeleteEventResult.Idle // Или Error("Отменено")? Idle лучше, т.к. непонятно состояние
                throw e // Перебрасываем отмену
            }
            Log.e(TAG, "Unexpected error deleting event $eventId", e)
            _deleteEventResult.value = DeleteEventResult.Error("Неизвестная ошибка: ${e.message}")
        } finally {
            // Можно добавить сброс в Idle через некоторое время, если Success/Error не обработаны в UI,
            // но лучше использовать consume метод.
        }
    }

    /**
     * Сбрасывает состояние результата удаления события в Idle.
     * Вызывать после того, как UI обработал результат (например, показал Snackbar).
     */
    fun consumeDeleteEventResult() {
        _deleteEventResult.value = DeleteEventResult.Idle
    }

    /** Принудительно обновляет данные для указанной даты */
    suspend fun refreshDate(date: LocalDate) {
        Log.d(TAG, "Manual refresh triggered for date: $date")

        // --- Explicit Cancellation ---
        activeFetchJob?.cancel(CancellationException("Manual refresh triggered for $date"))
        Log.d(TAG, "refreshDate: Previous activeFetchJob cancelled (if existed).")
        // --------------------------

        // Launching within the existing suspend fun's context might be okay,
        // or launch a new job and store it. Let's launch and store for consistency.
        val refreshJob = managerScope.launch { // Launch separately
            Log.d(TAG, "refreshDate: New coroutine launched for fetchAndStoreDateRange($date). Job: ${coroutineContext[Job]}")
            try {
                // Note: Using replace=true for manual refresh might be more robust
                // to clear out any potentially inconsistent state for that specific day.
                fetchAndStoreDateRange(date, date, true) // Consider replace=true
            } catch (ce: CancellationException) {
                Log.d(TAG, "refreshDate: Job ${coroutineContext[Job]} cancelled during refresh for $date: ${ce.message}")
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "refreshDate: Error during fetchAndStoreDateRange for $date", e)
                // Update network state?
            } finally {
                Log.d(TAG, "refreshDate: Job ${coroutineContext[Job]} finished for $date")
            }
        }
        activeFetchJob = refreshJob // Assign the new job as the active one
        Log.d(TAG, "refreshDate: Assigned new activeFetchJob: ${activeFetchJob}")
        refreshJob.join() // Wait for the refresh job to complete if refreshDate needs to be blocking? Or remove join() if not.
        // If you remove join(), refreshDate returns immediately, and the refresh happens in the background.
    }

    /** Сбрасывает состояние ошибки сети */
    fun clearNetworkError() {
        if (_rangeNetworkState.value is EventNetworkState.Error) {
            _rangeNetworkState.value = EventNetworkState.Idle
        }
    }


    // --- Приватные/внутренние методы ---

    /** Проверяет, нужно ли загружать/расширять диапазон дат */
    internal suspend fun ensureDateRangeLoadedAround(centerDate: LocalDate, forceLoad: Boolean) = withContext(ioDispatcher) { // В IO
        val currentlyLoaded = _loadedDateRange.value
        val isLoading = _rangeNetworkState.value is EventNetworkState.Loading

        if (isLoading) {
            Log.d(TAG, "Load check skipped for $centerDate, range fetch already in progress.")
            return@withContext
        }

        val idealTargetRange = centerDate.minusDays(PREFETCH_DAYS_BACKWARD.toLong())..centerDate.plusDays(PREFETCH_DAYS_FORWARD.toLong())
        Log.d(TAG, "ensureDateRange: center=$centerDate, current=$currentlyLoaded, ideal=$idealTargetRange")

        if (forceLoad) {
            Log.i(TAG, "Force load requested for $centerDate. Fetching ideal range: $idealTargetRange and replacing.")
            fetchAndStoreDateRange(idealTargetRange.start, idealTargetRange.endInclusive, true)
            return@withContext
        }

        Log.d(TAG, "ensureDateRangeLoadedAround: Proceeding with standard checks/fetch for $centerDate (forceLoad=false)")
        if (currentlyLoaded == null) {
            Log.i(TAG, "Initial load: $idealTargetRange")
            fetchAndStoreDateRange(idealTargetRange.start, idealTargetRange.endInclusive, true)
        } else {
            val isJump = centerDate < currentlyLoaded.start.minusDays(JUMP_DETECTION_BUFFER_DAYS.toLong()) ||
                    centerDate > currentlyLoaded.endInclusive.plusDays(JUMP_DETECTION_BUFFER_DAYS.toLong())

            if (isJump) {
                Log.i(TAG, "Jump detected! Loading new range: $idealTargetRange")
                fetchAndStoreDateRange(idealTargetRange.start, idealTargetRange.endInclusive, true)
            } else {
                val needsForwardPrefetch = centerDate >= currentlyLoaded.endInclusive.minusDays(TRIGGER_PREFETCH_THRESHOLD_FORWARD.toLong())
                val needsBackwardPrefetch = centerDate <= currentlyLoaded.start.plusDays(TRIGGER_PREFETCH_THRESHOLD_BACKWARD.toLong())

                if (needsForwardPrefetch || needsBackwardPrefetch) {
                    Log.i(TAG, "Prefetch needed. Forward=$needsForwardPrefetch, Backward=$needsBackwardPrefetch.")
                    val rangeToLoad = currentlyLoaded.union(idealTargetRange)
                    Log.d(TAG, "Expanding range to: $rangeToLoad")
                    fetchAndStoreDateRange(rangeToLoad.start, rangeToLoad.endInclusive, false)
                } else {
                    // Log.d(TAG, "No load needed. Center $centerDate is within loaded range $currentlyLoaded.")
                }
            }
        }
    }

    /** Загружает данные для диапазона дат с бэкенда и сохраняет в БД */
    private suspend fun fetchAndStoreDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
        replaceLoadedRange: Boolean
    ) = withContext(ioDispatcher) { // Гарантированно в IO
        // Предотвращаем двойную загрузку (простая проверка)
        if (_rangeNetworkState.value is EventNetworkState.Loading && !replaceLoadedRange) { // Разрешаем replace=true перекрыть загрузку
            Log.d(TAG, "Range fetch request ignored for $startDate..$endDate (replace=$replaceLoadedRange), already loading.")
            return@withContext
        }

        // Получаем токен через AuthManager
        var freshToken = authManager.getFreshIdToken() // Вызываем suspend функцию менеджера
        var attempts = 0
        val maxAttempts = 3 // Максимум попыток
        val retryDelay = 500L // Задержка между попытками (мс)

        while (freshToken == null && attempts < maxAttempts) {
            attempts++
            Log.w(TAG, "fetchAndStoreDateRange: Failed to get token (attempt $attempts/$maxAttempts). Retrying in ${retryDelay}ms...")
            delay(retryDelay) // Ждем перед повторной попыткой
            freshToken = authManager.getFreshIdToken()
        }

        if (freshToken == null) {
            Log.w(TAG, "Cannot fetch range: Failed to get fresh ID token.")
            _rangeNetworkState.value = EventNetworkState.Error("Ошибка аутентификации")
            return@withContext // Выход, если нет токена
        }

        Log.i(TAG, "Starting fetch for date range: $startDate to $endDate (replace=$replaceLoadedRange)")
        _rangeNetworkState.value = EventNetworkState.Loading

        val url = "$backendBaseUrl/calendar/events/range" +
                "?startDate=${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}" +
                "&endDate=${endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}"

        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $freshToken") // Используем свежий токен
            .build()

        try {
            val responseBodyString = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = parseBackendError(response.body?.string(), response.code)
                    Log.e(TAG, "Error fetching range from backend: ${response.code} - $errorMsg")
                    throw IOException("Backend error: ${response.code} - $errorMsg")
                }
                response.body?.string() // Возвращаем тело ответа
            }

            if (responseBodyString.isNullOrBlank()) {
                Log.w(TAG, "Empty response body for range $startDate to $endDate.")
                // Если ответ пустой, все равно считаем диапазон загруженным и чистим БД
                // (на случай, если там были старые события для этого диапазона)
                saveEventsToDb(emptyList(), startDate, endDate) // Сохраняем пустой список
                updateLoadedRange(startDate..endDate, replaceLoadedRange) // Обновляем диапазон
                _rangeNetworkState.value = EventNetworkState.Idle // Успех (пустой)
                return@withContext
            }

            Log.i(TAG,"Range received from network for $startDate to $endDate. Size: ${responseBodyString.length}")
            val networkEvents = parseEventsResponse(responseBodyString)
            saveEventsToDb(networkEvents, startDate, endDate)
            updateLoadedRange(startDate..endDate, replaceLoadedRange) // Обновляем _loadedDateRange
            _rangeNetworkState.value = EventNetworkState.Idle // Успех

        } catch (e: IOException) {
            Log.e(TAG, "Network error fetching range $startDate to $endDate", e)
            _rangeNetworkState.value = EventNetworkState.Error("Ошибка сети: ${e.message}")
        } catch (e: JSONException) {
            Log.e(TAG, "JSON parsing error for range $startDate to $endDate", e)
            _rangeNetworkState.value = EventNetworkState.Error("Ошибка чтения ответа сервера.")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing range response for $startDate to $endDate", e)
            _rangeNetworkState.value = EventNetworkState.Error("Ошибка обработки данных: ${e.message}")
        }
    }

    /** Сохраняет события в БД, очищая старые данные для диапазона */
    private suspend fun saveEventsToDb(
        networkEvents: List<CalendarEvent>,
        startDate: LocalDate,
        endDate: LocalDate
    ) = withContext(ioDispatcher) { // Явно указываем IO
        // Получаем АКТУАЛЬНУЮ таймзону перед маппингом
        val currentTimeZoneId = try {
            settingsRepository.timeZoneFlow.first() // Берем последнее значение из Flow настроек
                .ifEmpty { ZoneId.systemDefault().id }
        } catch (e: Exception) {
            Log.w(TAG, "Could not get timezone from settings, using system default.", e)
            ZoneId.systemDefault().id
        }
        val zoneId = try { ZoneId.of(currentTimeZoneId) } catch (e: Exception) { ZoneId.systemDefault() }


        val eventEntities = networkEvents.mapNotNull { EventMapper.mapToEntity(it,
            zoneId.toString()
        ) }

        val startRangeMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endRangeMillis = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        Log.d(TAG, "Updating DB for range $startDate to $endDate. Deleting range [$startRangeMillis, $endRangeMillis), Inserting ${eventEntities.size} events.")
        eventDao.clearAndInsertEventsForRange(startRangeMillis, endRangeMillis, eventEntities)
        Log.i(TAG, "DB updated for range $startDate to $endDate.")
    }

    /** Обновляет состояние _loadedDateRange */
    private fun updateLoadedRange(fetchedRange: ClosedRange<LocalDate>, replace: Boolean) {
        if (replace) {
            _loadedDateRange.value = fetchedRange
            Log.d(TAG, "Replaced loaded range with: $fetchedRange")
        } else {
            _loadedDateRange.update { current ->
                val updatedRange = current?.union(fetchedRange) ?: fetchedRange
                Log.d(TAG, "Merged fetched range $fetchedRange with current ${current}. New loaded range: $updatedRange")
                updatedRange
            }
        }
    }


    /** Парсит JSON-ответ со списком событий */
    private fun parseEventsResponse(jsonString: String): List<CalendarEvent> {
        // --- ВЕСЬ КОД ТВОЕГО МЕТОДА parseEventsResponse ---
        val events = mutableListOf<CalendarEvent>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                try {
                    val eventObject = jsonArray.getJSONObject(i)
                    val isAllDay = eventObject.optBoolean("isAllDay", false)
                    val startTimeStr = eventObject.optString("startTime")
                    val endTimeStr = eventObject.optString("endTime")
                    val id = eventObject.optString("id")
                    val summary = eventObject.optString("summary", "Без названия")
                    val description = eventObject.optString("description")
                    val location = eventObject.optString("location")

                    if (id.isNullOrEmpty() || startTimeStr.isNullOrEmpty()) {
                        Log.w(TAG, "Skipping event due to missing id or startTime in JSON object: ${eventObject.optString("summary")}")
                        continue
                    }

                    events.add(
                        CalendarEvent(
                            id = id,
                            summary = summary,
                            startTime = startTimeStr,
                            endTime = endTimeStr.takeIf { !it.isNullOrEmpty() } ?: startTimeStr, // Используем твой fallback
                            description = description,
                            location = location,
                            isAllDay = isAllDay
                        )
                    )
                } catch (e: JSONException) {
                    Log.e(TAG, "Error parsing individual event object at index $i: ${e.localizedMessage}")
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to parse events JSON array", e)
            throw e // Пробрасываем выше
        }
        return events
    }

    private fun parseBackendError(responseBody: String?, code: Int): String {
        return try {
            val json = JSONObject(responseBody ?: "{}")
            json.optString("detail", "Ошибка сервера ($code)")
        } catch (e: JSONException) {
            "Ошибка сервера ($code)"
        }
    }

    // --- Утилиты для диапазонов дат ---
    private fun ClosedRange<LocalDate>.containsRange(other: ClosedRange<LocalDate>): Boolean {
        return this.start <= other.start && this.endInclusive >= other.endInclusive
    }

    private fun ClosedRange<LocalDate>.union(other: ClosedRange<LocalDate>): ClosedRange<LocalDate> {
        val newStart = minOf(this.start, other.start)
        val newEnd = maxOf(this.endInclusive, other.endInclusive)
        return newStart..newEnd
    }

    // Helper minOf/maxOf для LocalDate (если нет в стандартной библиотеке нужной версии)
    private fun minOf(a: LocalDate, b: LocalDate): LocalDate = if (a.isBefore(b)) a else b
    private fun maxOf(a: LocalDate, b: LocalDate): LocalDate = if (a.isAfter(b)) a else b

}