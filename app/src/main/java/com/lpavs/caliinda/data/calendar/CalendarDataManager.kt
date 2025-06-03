package com.lpavs.caliinda.data.calendar

import android.util.Log
import com.google.gson.Gson
import com.lpavs.caliinda.data.auth.AuthManager
import com.lpavs.caliinda.data.local.CalendarEventEntity
import com.lpavs.caliinda.data.local.EventDao
import com.lpavs.caliinda.data.local.UpdateEventApiRequest
import com.lpavs.caliinda.data.mapper.EventMapper
import com.lpavs.caliinda.data.repo.SettingsRepository
import com.lpavs.caliinda.di.BackendUrl
import com.lpavs.caliinda.di.IoDispatcher
import com.lpavs.caliinda.ui.screens.main.CalendarEvent
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

enum class ApiDeleteEventMode(val value: String) {
  DEFAULT("default"), // Поведение по умолчанию (обычно вся серия для мастер-события)
  INSTANCE_ONLY("instance_only") // Только этот экземпляр
  // Можно добавить ALL_SERIES("all_series"), если бэкенд будет его явно обрабатывать,
  // но пока DEFAULT часто выполняет эту роль.
}

enum class ClientEventUpdateMode(val value: String) {
  SINGLE_INSTANCE("single_instance"),
  ALL_IN_SERIES("all_in_series")
  // THIS_AND_FOLLOWING("this_and_following") // Пока не поддерживается
}

@Singleton
class CalendarDataManager
@Inject
constructor(
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
    const val INITIAL_LOAD_DAYS_AROUND = 7L
    const val UPDATE_LOAD_DAYS_AROUND = 5L
    const val TRIGGER_PREFETCH_THRESHOLD = 2L
    const val EXPAND_CHUNK_DAYS = 14L
    const val JUMP_DETECTION_BUFFER_DAYS = 10L
  }

  // --- Внутренние состояния ---
  private val _currentVisibleDate = MutableStateFlow(LocalDate.now())
  private val _loadedDateRange = MutableStateFlow<ClosedRange<LocalDate>?>(null)
  private val _rangeNetworkState = MutableStateFlow<EventNetworkState>(EventNetworkState.Idle)
  private val _createEventResult = MutableStateFlow<CreateEventResult>(CreateEventResult.Idle)
  private val _deleteEventResult = MutableStateFlow<DeleteEventResult>(DeleteEventResult.Idle)
  private val _updateEventResult = MutableStateFlow<UpdateEventResult>(UpdateEventResult.Idle)

  // --- Публичные StateFlow для ViewModel ---
  val currentVisibleDate: StateFlow<LocalDate> = _currentVisibleDate.asStateFlow()
  val rangeNetworkState: StateFlow<EventNetworkState> = _rangeNetworkState.asStateFlow()
  val createEventResult: StateFlow<CreateEventResult> = _createEventResult.asStateFlow()
  val updateEventResult: StateFlow<UpdateEventResult> = _updateEventResult.asStateFlow()
  val deleteEventResult: StateFlow<DeleteEventResult> = _deleteEventResult.asStateFlow()

  private var activeFetchJob: Job? = null

  // --- Публичные методы для ViewModel ---

  /** Устанавливает текущую видимую дату и запускает проверку/загрузку диапазона */
  fun setCurrentVisibleDate(newDate: LocalDate, forceRefresh: Boolean = false) {
    Log.d(
        TAG,
        "setCurrentVisibleDate: CALLED with $newDate. Current value: ${_currentVisibleDate.value}")

    // Only proceed if date is different OR range is not loaded yet
    val needsDateUpdate = newDate != _currentVisibleDate.value

    if (!needsDateUpdate && !forceRefresh && _loadedDateRange.value != null) {
      Log.d(
          TAG,
          "setCurrentVisibleDate: Date $newDate is same, no forceRefresh, range loaded. Skipping.")
      return
    }

    Log.i(TAG, "setCurrentVisibleDate: Proceeding to set date and check range for $newDate")

    // --- Explicit Cancellation ---
    activeFetchJob?.cancel(
        CancellationException("New date set: $newDate, forceRefresh=$forceRefresh"))
    Log.d(TAG, "setCurrentVisibleDate: Previous activeFetchJob cancelled (if existed).")
    // --------------------------

    if (needsDateUpdate) {
      _currentVisibleDate.value = newDate
    }

    // --- ВАЖНО: ПРОВЕРКА АУТЕНТИФИКАЦИИ ПЕРЕД ЗАПУСКОМ ЗАГРУЗКИ ---
    if (authManager.authState.value.isSignedIn) {
      // Запускаем корутину ТОЛЬКО если пользователь вошел
      activeFetchJob =
          managerScope.launch {
            Log.d(
                TAG,
                "setCurrentVisibleDate: New coroutine launched for ensureDateRangeLoadedAround($newDate, forceRefresh=$forceRefresh). Job: ${coroutineContext[Job]}")
            try {
              ensureDateRangeLoadedAround(newDate, forceRefresh)
            } catch (ce: CancellationException) {
              Log.d(
                  TAG,
                  "setCurrentVisibleDate: Job ${coroutineContext[Job]} cancelled while ensuring range for $newDate: ${ce.message}")
              throw ce // Перебрасываем отмену
            } catch (e: Exception) {
              Log.e(
                  TAG,
                  "setCurrentVisibleDate: Error in ensureDateRangeLoadedAround for $newDate",
                  e)
              // Возможно, обновить _rangeNetworkState здесь, если ensureDateRangeLoadedAround не
              // сделает этого
            } finally {
              Log.d(
                  TAG, "setCurrentVisibleDate: Job ${coroutineContext[Job]} finished for $newDate")
            }
          }
      Log.d(TAG, "setCurrentVisibleDate: Assigned new activeFetchJob: $activeFetchJob")
    } else {
      // Пользователь не вошел - не запускаем загрузку
      Log.w(
          TAG,
          "setCurrentVisibleDate: Skipping range load for $newDate because user is not signed in.")
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
              val zoneId =
                  try {
                    ZoneId.of(timeZoneIdString.ifEmpty { ZoneId.systemDefault().id })
                  } catch (e: Exception) {
                    ZoneId.systemDefault()
                  }

              // --- Логика из ViewModel ---
              entityList
                  .filter { entity: CalendarEventEntity -> // Указываем тип entity
                    // Используем startTimeMillis, endTimeMillis, isAllDay из ТВОЕГО
                    // CalendarEventEntity
                    if (!entity.isAllDay) {
                      // Оригинальная логика для не-all-day: событие должно заканчиваться до начала
                      // следующего дня
                      entity.endTimeMillis < endMillis
                      // ЗАМЕЧАНИЕ: Эта логика может не показывать события, которые *пересекают*
                      // полночь,
                      // но заканчиваются позже начала следующего дня. Если это не то, что нужно,
                      // стандартная проверка на пересечение: entity.startTimeMillis < endMillis &&
                      // entity.endTimeMillis > startMillis
                    } else {
                      // Оригинальная логика для all-day: длительность примерно 1 день
                      val durationMillis = entity.endTimeMillis - entity.startTimeMillis
                      val twentyFourHoursMillis = TimeUnit.HOURS.toMillis(24)
                      val toleranceMillis = TimeUnit.MINUTES.toMillis(5)
                      (durationMillis >= twentyFourHoursMillis - toleranceMillis) &&
                          (durationMillis <= twentyFourHoursMillis + toleranceMillis)
                    }
                  } // Конец filter
                  .map { filteredEntity ->
                    // Маппим только отфильтрованные entity
                    EventMapper.mapToDomain(
                        filteredEntity, zoneId.toString()) // Используем актуальный zoneId
                  } // Конец mapNotNull
            } // Конец combine
        .catch { e ->
          Log.e(TAG, "Error processing events Flow for date $date", e)
          emit(emptyList())
        }
        .flowOn(ioDispatcher) // Выполняем combine, filter, map, catch на IO потоке
  }

  /** Создает новое событие через бэкенд */
  suspend fun createEvent(
      summary: String,
      startTimeString: String, // <-- Возвращаем строковое поле
      endTimeString: String, // <-- Возвращаем строковое поле
      isAllDay: Boolean, // <-- Возвращаем булево поле
      timeZoneId: String?, // <-- Передаем ID таймзоны (может быть null для all day)
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

    val requestBody =
        try {
          JSONObject()
              .apply {
                put("summary", summary)
                // --- ВОЗВРАЩАЕМ ПОЛЯ В КОРЕНЬ ---
                put("startTime", startTimeString) // Строка (date или dateTime)
                put("endTime", endTimeString) // Строка (date или dateTime)
                put("isAllDay", isAllDay) // Булево значение
                // Таймзона нужна бэкенду для формирования запроса к Google
                if (!isAllDay && timeZoneId != null) {
                  put("timeZoneId", timeZoneId) // Отправляем ID таймзоны
                } else {
                  Log.w(TAG, "Sending timed event without timeZoneId to backend!")
                  // Бэкенд должен будет обработать этот случай или вернуть ошибку
                }
                // --- КОНЕЦ ВОЗВРАЩЕНИЯ ПОЛЕЙ ---

                description?.takeIf { it.isNotBlank() }?.let { put("description", it) }
                location?.takeIf { it.isNotBlank() }?.let { put("location", it) }

                // --- Добавляем recurrence (как и раньше) ---
                recurrenceRule
                    ?.takeIf { it.isNotBlank() }
                    ?.let { ruleString ->
                      val fullRuleString = "RRULE:$ruleString"
                      val recurrenceArray = JSONArray().apply { put(fullRuleString) }
                      // Убедитесь, что ключ "recurrence" ожидает ваш бэкенд
                      put("recurrence", recurrenceArray)
                    }
              }
              .toString()
              .toRequestBody("application/json; charset=utf-8".toMediaType())
        } catch (e: JSONException) {
          Log.e(TAG, "Error creating JSON for new event", e)
          _createEventResult.value = CreateEventResult.Error("Ошибка подготовки данных")
          return
        }

    val request =
        Request.Builder()
            .url("$backendBaseUrl/calendar/events")
            .header("Authorization", "Bearer $freshToken") // Используем полученный токен
            .post(requestBody)
            .build()

    try {
      // Выполняем запрос в IO dispatcher
      val response = withContext(ioDispatcher) { okHttpClient.newCall(request).execute() }
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
      // Сбросить состояние Loading, если Success/Error не установились (маловероятно, но для
      // надежности)
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
   * Запускает процесс удаления события на бэкенде и в локальной БД. Результат операции будет
   * доступен через [deleteEventResult] StateFlow.
   *
   * @param eventId Уникальный идентификатор события для удаления.
   * @param mode Режим удаления для повторяющихся событий.
   */
  suspend fun deleteEvent(
      eventId: String,
      mode: ApiDeleteEventMode = ApiDeleteEventMode.DEFAULT
  ) { // <-- ДОБАВЛЕН ПАРАМЕТР mode
    if (_deleteEventResult.value is DeleteEventResult.Loading) {
      Log.w(
          TAG,
          "deleteEvent called while another deletion is in progress for ID: $eventId. Ignoring.")
      return
    }

    _deleteEventResult.value = DeleteEventResult.Loading
    Log.i(TAG, "Attempting to delete event with ID: $eventId, mode: ${mode.value}")

    val freshToken = authManager.getFreshIdToken()
    if (freshToken == null) {
      Log.e(TAG, "Cannot delete event $eventId: Failed to get fresh ID token.")
      _deleteEventResult.value = DeleteEventResult.Error("Ошибка аутентификации")
      return
    }

    // --- НОВОЕ: Формируем URL с query-параметром mode ---
    var url = "$backendBaseUrl/calendar/events/$eventId"
    if (mode != ApiDeleteEventMode.DEFAULT) {
      url += "?mode=${mode.value}"
    }
    Log.d(TAG, "Delete request URL: $url")
    // ---------------------------------------------------

    val request =
        Request.Builder()
            .url(url) // Используем обновленный URL
            .delete()
            .header("Authorization", "Bearer $freshToken")
            .build()

    try {
      val response = withContext(ioDispatcher) { okHttpClient.newCall(request).execute() }

      if (response.isSuccessful) {
        Log.i(
            TAG,
            "Event $eventId successfully processed for deletion on backend (Mode: ${mode.value}, Code: ${response.code}). Deleting locally.")
        try {
          withContext(ioDispatcher) { eventDao.deleteEventById(eventId) }
          Log.i(TAG, "Event $eventId (or its reference) successfully deleted from local DB.")
          _deleteEventResult.value = DeleteEventResult.Success
          refreshDate(_currentVisibleDate.value)
        } catch (dbError: Exception) {
          Log.e(
              TAG,
              "Failed to delete event $eventId from local DB after successful backend deletion.",
              dbError)
          _deleteEventResult.value =
              DeleteEventResult.Error(
                  "Ошибка синхронизации: событие обработано на сервере, но не удалено локально.")
        }
      } else {
        val errorMsg = parseBackendError(response.body?.string(), response.code)
        Log.e(
            TAG,
            "Error deleting event $eventId (Mode: ${mode.value}) via backend: ${response.code} - $errorMsg")
        _deleteEventResult.value = DeleteEventResult.Error(errorMsg)
      }
    } catch (e: IOException) {
      Log.e(TAG, "Network error deleting event $eventId (Mode: ${mode.value})", e)
      _deleteEventResult.value = DeleteEventResult.Error("Сетевая ошибка: ${e.message}")
    } catch (e: Exception) {
      if (e is CancellationException) {
        Log.w(TAG, "Delete event $eventId (Mode: ${mode.value}) job was cancelled.", e)
        _deleteEventResult.value = DeleteEventResult.Idle
        throw e
      }
      Log.e(TAG, "Unexpected error deleting event $eventId (Mode: ${mode.value})", e)
      _deleteEventResult.value = DeleteEventResult.Error("Неизвестная ошибка: ${e.message}")
    }
  }

  /**
   * Сбрасывает состояние результата удаления события в Idle. Вызывать после того, как UI обработал
   * результат (например, показал Snackbar).
   */
  fun consumeDeleteEventResult() {
    _deleteEventResult.value = DeleteEventResult.Idle
  }

  suspend fun updateEvent(
      eventId: String,
      updateData: UpdateEventApiRequest, // Данные для обновления
      mode: ClientEventUpdateMode
  ) {
    if (_updateEventResult.value is UpdateEventResult.Loading) {
      Log.w(
          TAG, "updateEvent called while another update is in progress for ID: $eventId. Ignoring.")
      return
    }
    _updateEventResult.value = UpdateEventResult.Loading
    Log.i(TAG, "Attempting to update event ID: $eventId, mode: ${mode.value}, data: $updateData")

    val freshToken = authManager.getFreshIdToken()
    if (freshToken == null) {
      Log.e(TAG, "Cannot update event $eventId: Failed to get fresh ID token.")
      _updateEventResult.value = UpdateEventResult.Error("Ошибка аутентификации")
      return
    }

    val url = "$backendBaseUrl/calendar/events/$eventId?update_mode=${mode.value}"
    val requestBodyJson =
        try {
          Gson().toJson(updateData)
        } catch (e: Exception) {
          Log.e(TAG, "Error serializing updateData to JSON for event $eventId", e)
          _updateEventResult.value =
              UpdateEventResult.Error("Ошибка подготовки данных для обновления.")
          return
        }

    val request =
        Request.Builder()
            .url(url)
            .patch(
                requestBodyJson.toRequestBody(
                    "application/json; charset=utf-8".toMediaType())) // Используем PATCH
            .header("Authorization", "Bearer $freshToken")
            .build()

    try {
      val response = withContext(ioDispatcher) { okHttpClient.newCall(request).execute() }

      if (response.isSuccessful) {
        val responseBody = response.body?.string()
        if (responseBody != null) {
          try {
            val responseObject = JSONObject(responseBody)
            val newEventId = responseObject.optString("eventId", eventId)
            Log.i(
                TAG,
                "Event $eventId successfully updated on backend (Mode: ${mode.value}). New/Confirmed ID: $newEventId")

            _updateEventResult.value = UpdateEventResult.Success(newEventId)
              refreshDate(currentVisibleDate.value)
          } catch (e: JSONException) {
            Log.e(TAG, "Error parsing successful update response for event $eventId", e)
            _updateEventResult.value =
                UpdateEventResult.Error("Ошибка ответа сервера после обновления.")
          }
        } else {
          Log.e(TAG, "Successful update response for event $eventId has empty body.")
          _updateEventResult.value =
              UpdateEventResult.Error("Пустой ответ сервера после обновления.")
        }
      } else {
        val errorMsg = parseBackendError(response.body?.string(), response.code)
        Log.e(
            TAG,
            "Error updating event $eventId (Mode: ${mode.value}) via backend: ${response.code} - $errorMsg")
        _updateEventResult.value = UpdateEventResult.Error(errorMsg)
      }
    } catch (e: IOException) {
      Log.e(TAG, "Network error updating event $eventId (Mode: ${mode.value})", e)
      _updateEventResult.value = UpdateEventResult.Error("Сетевая ошибка: ${e.message}")
    } catch (e: Exception) {
      if (e is CancellationException) {
        Log.w(TAG, "Update event $eventId (Mode: ${mode.value}) job was cancelled.", e)
        _updateEventResult.value = UpdateEventResult.Idle
        throw e
      }
      Log.e(TAG, "Unexpected error updating event $eventId (Mode: ${mode.value})", e)
      _updateEventResult.value = UpdateEventResult.Error("Неизвестная ошибка: ${e.message}")
    }
  }

  fun consumeUpdateEventResult() {
    _updateEventResult.value = UpdateEventResult.Idle
  }

  /** Принудительно обновляет данные для указанной даты */
  suspend fun refreshDate(centerDateToRefreshAround: LocalDate) {
    Log.d(TAG, "Manual refresh triggered around date: $centerDateToRefreshAround")

    // --- Explicit Cancellation ---
    activeFetchJob?.cancel(
        CancellationException("Manual refresh triggered for $centerDateToRefreshAround"))
    Log.d(TAG, "refreshDate: Previous activeFetchJob cancelled (if existed).")
    // --------------------------
    // TODO подумать может можно не две недели вокруг перезагружать
    val targetRefreshRangeStart = centerDateToRefreshAround.minusDays(UPDATE_LOAD_DAYS_AROUND)
    val targetRefreshRangeEnd = centerDateToRefreshAround.plusDays(UPDATE_LOAD_DAYS_AROUND)
    Log.d(
        TAG,
        "refreshDate: Target refresh range is [$targetRefreshRangeStart .. $targetRefreshRangeEnd]")

    val refreshJob =
        managerScope.launch { // Launch separately
          Log.d(
              TAG,
              "refreshDate: New coroutine launched for fetchAndStoreDateRange. Job: ${coroutineContext[Job]}")
          try {
            fetchAndStoreDateRange(
                targetRefreshRangeStart, targetRefreshRangeEnd, true) // Consider replace=true
          } catch (ce: CancellationException) {
            Log.d(
                TAG,
                "refreshDate: Job ${coroutineContext[Job]} cancelled during refresh for $centerDateToRefreshAround: ${ce.message}")
            throw ce
          } catch (e: Exception) {
            Log.e(
                TAG,
                "refreshDate: Error during fetchAndStoreDateRange for $centerDateToRefreshAround",
                e)
          } finally {
            Log.d(
                TAG,
                "refreshDate: Job ${coroutineContext[Job]} finished for $centerDateToRefreshAround")
          }
        }
    activeFetchJob = refreshJob // Assign the new job as the active one
    Log.d(TAG, "refreshDate: Assigned new activeFetchJob: $activeFetchJob")
    refreshJob
        .join() // Wait for the refresh job to complete if refreshDate needs to be blocking? Or
                // remove join() if not.
    // If you remove join(), refreshDate returns immediately, and the refresh happens in the
    // background.
  }

  /** Сбрасывает состояние ошибки сети */
  fun clearNetworkError() {
    if (_rangeNetworkState.value is EventNetworkState.Error) {
      _rangeNetworkState.value = EventNetworkState.Idle
    }
  }

  // --- Приватные/внутренние методы ---

  /** Проверяет, нужно ли загружать/расширять диапазон дат */
  private suspend fun ensureDateRangeLoadedAround(centerDate: LocalDate, forceLoad: Boolean) =
      withContext(ioDispatcher) { // В IO
        val currentlyLoaded = _loadedDateRange.value
        val initialOrJumpTargetRange =
            centerDate.minusDays(INITIAL_LOAD_DAYS_AROUND)..centerDate.plusDays(
                    INITIAL_LOAD_DAYS_AROUND)
        Log.d(
            TAG,
            "ensureDateRange: center=$centerDate, current=$currentlyLoaded, initialOrJumpTarget=$initialOrJumpTargetRange")

        if (forceLoad) {
          Log.i(
              TAG,
              "Force load requested for $centerDate. Fetching range: $initialOrJumpTargetRange")
          fetchAndStoreDateRange(
              initialOrJumpTargetRange.start, initialOrJumpTargetRange.endInclusive, true)
          return@withContext
        }

        if (currentlyLoaded == null) {
          Log.i(TAG, "Initial load for $centerDate. Fetching range: $initialOrJumpTargetRange")
          fetchAndStoreDateRange(
              initialOrJumpTargetRange.start, initialOrJumpTargetRange.endInclusive, true)
        } else {
          val isJump =
              centerDate < currentlyLoaded.start.minusDays(JUMP_DETECTION_BUFFER_DAYS) ||
                  centerDate > currentlyLoaded.endInclusive.plusDays(JUMP_DETECTION_BUFFER_DAYS)

          if (isJump) {
            Log.i(
                TAG, "Jump detected for $centerDate. Fetching new range: $initialOrJumpTargetRange")
            fetchAndStoreDateRange(
                initialOrJumpTargetRange.start, initialOrJumpTargetRange.endInclusive, true)
          } else {
            var rangeToFetchDeltaStart: LocalDate? = null
            var rangeToFetchDeltaEnd: LocalDate? = null

            if (centerDate >= currentlyLoaded.endInclusive.minusDays(TRIGGER_PREFETCH_THRESHOLD)) {
              rangeToFetchDeltaStart = currentlyLoaded.endInclusive.plusDays(1)
              rangeToFetchDeltaEnd = rangeToFetchDeltaStart.plusDays(EXPAND_CHUNK_DAYS - 1)
              Log.i(
                  TAG,
                  "Forward prefetch triggered at $centerDate. Need to fetch DELTA: [$rangeToFetchDeltaStart .. $rangeToFetchDeltaEnd]")
            } else if (centerDate <= currentlyLoaded.start.plusDays(TRIGGER_PREFETCH_THRESHOLD)) {
              rangeToFetchDeltaEnd = currentlyLoaded.start.minusDays(1)
              rangeToFetchDeltaStart = rangeToFetchDeltaEnd.minusDays(EXPAND_CHUNK_DAYS - 1)
              Log.i(
                  TAG,
                  "Backward prefetch triggered at $centerDate. Need to fetch DELTA: [$rangeToFetchDeltaStart .. $rangeToFetchDeltaEnd]")
            }

            if (rangeToFetchDeltaStart != null && rangeToFetchDeltaEnd != null) {
              fetchAndStoreDateRange(rangeToFetchDeltaStart, rangeToFetchDeltaEnd, false)
            } else {
              Log.d(
                  TAG,
                  "No delta load needed for $centerDate. It is comfortably within $currentlyLoaded.")
            }
          }
        }
      }

  /**
   * Загружает данные для диапазона дат с бэкенда и сохраняет в БД.
   *
   * @param startDate Начало диапазона для запроса к бэкенду (может быть дельта).
   * @param endDate Конец диапазона для запроса к бэкенду (может быть дельта).
   * @param replaceWholeLoadedRangeWithThis true - если _loadedDateRange должен быть полностью
   *   заменен на [startDate].[endDate]. false - если [startDate].[endDate] это дельта и ее нужно
   *   объединить с текущим _loadedDateRange.
   */
  private suspend fun fetchAndStoreDateRange(
      startDate: LocalDate,
      endDate: LocalDate,
      replaceWholeLoadedRangeWithThis: Boolean
  ) {

    // --- 1. ПОЛУЧЕНИЕ ТОКЕНА (ОТМЕНЯЕМАЯ ЧАСТЬ) ---
    var freshToken: String? = null
    var attempts = 0
    val maxAttempts = 3
    val retryDelay = 500L

    Log.d(
        TAG,
        "FADR: Attempting to get token for $startDate to $endDate. Current job: ${coroutineContext[Job]}")
    while (freshToken == null && attempts < maxAttempts) {
      attempts++
      try {
        // authManager.getFreshIdToken() - это suspend функция, она может быть прервана отменой.
        freshToken = authManager.getFreshIdToken()
        if (freshToken == null && attempts < maxAttempts) {
          Log.w(
              TAG,
              "FADR: Failed to get token (attempt $attempts/$maxAttempts) for $startDate to $endDate. Retrying in ${retryDelay}ms...")
          delay(retryDelay) // delay() также реагирует на отмену корутины.
        }
      } catch (e: CancellationException) {
        Log.i(
            TAG,
            "FADR: Token acquisition explicitly CANCELLED for $startDate to $endDate. Job: ${coroutineContext[Job]}")
        throw e
      }
    }

    if (freshToken == null) {
      Log.w(
          TAG,
          "FADR: Cannot fetch range $startDate-$endDate. Failed to get fresh ID token after $maxAttempts attempts.")
      // Если не удалось получить токен, устанавливаем ошибку и выходим.
      // Это не отмена, а логическая ошибка.
      _rangeNetworkState.value = EventNetworkState.Error("Ошибка аутентификации (токен не получен)")
      return
    }
    Log.d(TAG, "FADR: Token acquired for $startDate to $endDate.")

    // --- 2. УСТАНОВКА СОСТОЯНИЯ ЗАГРУЗКИ И НАЧАЛО НЕОТМЕНЯЕМОЙ ОПЕРАЦИИ ---
    _rangeNetworkState.value = EventNetworkState.Loading
    Log.i(
        TAG,
        "FADR: Set state to Loading for $startDate to $endDate. About to enter NonCancellable. Job: ${coroutineContext[Job]}")

    try {
      withContext(NonCancellable) {
        Log.i(
            TAG,
            "[NC] ENTER NonCancellable block for $startDate to $endDate. Current Job: ${coroutineContext[Job]}")

        val url =
            "$backendBaseUrl/calendar/events/range" +
                "?startDate=${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}" +
                "&endDate=${endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}"

        val request =
            Request.Builder().url(url).get().header("Authorization", "Bearer $freshToken").build()

        try {
          // okHttpClient.newCall(request).execute() - блокирующий вызов.
          // NonCancellable защищает эту корутину от внешней отмены во время выполнения этого
          // вызова.
          val responseBodyString =
              okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                  val errorMsg = parseBackendError(response.body?.string(), response.code)
                  Log.e(
                      TAG,
                      "[NC] Error fetching range from backend for $startDate to $endDate: ${response.code} - $errorMsg")
                  // Устанавливаем состояние ошибки ВНУТРИ NonCancellable блока
                  _rangeNetworkState.value =
                      EventNetworkState.Error("Ошибка сервера: $errorMsg (${response.code})")
                  return@withContext // Выходим из NonCancellable блока
                }
                response.body?.string()
              }

          // Проверяем, не установили ли мы ошибку на предыдущем шаге
          if (_rangeNetworkState.value is EventNetworkState.Error) {
            Log.w(
                TAG,
                "[NC] Skipping further processing for $startDate to $endDate due to earlier error in NC block.")
            return@withContext // Выходим, так как ошибка уже установлена
          }

          if (responseBodyString.isNullOrBlank()) {
            Log.w(TAG, "[NC] Empty response body for range $startDate to $endDate.")
            // saveEventsToDb - suspend функция, но она вызовется в NonCancellable контексте
            saveEventsToDb(emptyList(), startDate, endDate)
            updateLoadedRange(startDate..endDate, replaceWholeLoadedRangeWithThis) // Не suspend
            _rangeNetworkState.value = EventNetworkState.Idle // Успех (пустой ответ)
            Log.i(TAG, "[NC] Successfully processed EMPTY response for $startDate to $endDate.")
          } else {
            Log.i(
                TAG,
                "[NC] Range received from network for $startDate to $endDate. Size: ${responseBodyString.length}")
            val networkEvents = parseEventsResponse(responseBodyString) // Не suspend
            saveEventsToDb(networkEvents, startDate, endDate) // Suspend, но в NonCancellable
            updateLoadedRange(startDate..endDate, replaceWholeLoadedRangeWithThis) // Не suspend
            _rangeNetworkState.value = EventNetworkState.Idle // Успех
            Log.i(TAG, "[NC] Successfully processed NON-EMPTY response for $startDate to $endDate.")
          }
        } catch (e: IOException) {
          // Эти ошибки (сетевые, парсинг) происходят ВНУТРИ NonCancellable блока.
          Log.e(
              TAG, "[NC] IOException during network call/processing for $startDate to $endDate", e)
          _rangeNetworkState.value = EventNetworkState.Error("Ошибка сети (в NC): ${e.message}")
        } catch (e: JSONException) {
          Log.e(TAG, "[NC] JSONException during parsing for $startDate to $endDate", e)
          _rangeNetworkState.value =
              EventNetworkState.Error("Ошибка парсинга JSON (в NC): ${e.message}")
        } catch (e: Exception) {
          // Любые другие неожиданные ошибки ВНУТРИ NonCancellable блока.
          // Сюда не должна попадать CancellationException от *внешней* отмены.
          Log.e(TAG, "[NC] Unexpected Exception for $startDate to $endDate", e)
          _rangeNetworkState.value =
              EventNetworkState.Error("Неизвестная ошибка (в NC): ${e.message}")
        }
        Log.i(
            TAG,
            "[NC] EXIT NonCancellable block for $startDate to $endDate. Final state: ${_rangeNetworkState.value}")
      } // --- Конец NonCancellable блока ---
    } catch (e: CancellationException) {
      // Этот блок `catch` сработает, если отмена произошла *ПОСЛЕ* установки
      // `_rangeNetworkState.value = Loading`,
      // но *ДО* фактического входа в `withContext(NonCancellable)`.
      // `NonCancellable` блок сам по себе не должен пробрасывать `CancellationException` от внешней
      // отмены.
      Log.i(
          TAG,
          "FADR: Operation CANCELLED for $startDate to $endDate *before* NonCancellable block (but after setting Loading). Job: ${coroutineContext[Job]}")
      // Состояние Loading будет сброшено в `finally`.
      // Важно перебросить CancellationException, чтобы родительская корутина (activeFetchJob)
      // корректно завершилась как отмененная.
      throw e
    } catch (e: Exception) {
      // Этот блок поймает исключения, которые могли произойти *вне* `NonCancellable` блока
      // (например, если сам `withContext(NonCancellable)` бросил что-то при инициализации, что
      // маловероятно),
      // но *после* установки `Loading` и *до* или *после* `NonCancellable` блока.
      // Ошибки ВНУТРИ `NonCancellable` блока обрабатываются его собственным `try-catch`.
      Log.e(
          TAG,
          "FADR: Generic Exception for $startDate to $endDate (outside NC block, or unhandled before NC). Job: ${coroutineContext[Job]}",
          e)
      _rangeNetworkState.value =
          EventNetworkState.Error("Общая ошибка обработки данных (вне NC): ${e.message}")
    } finally {
      // Этот блок `finally` выполнится всегда:
      // - после нормального завершения `try` блока (включая `NonCancellable`).
      // - после любого исключения, пойманного в `catch` блоках.
      // - если `try` блок был прерван `CancellationException` (которая потом перебрасывается).
      // Его задача - убедиться, что состояние `Loading` не "зависло".
      // Если `NonCancellable` блок успешно выполнился, он сам установит `Idle` или `Error`.
      // Этот `finally` важен для случаев, когда отмена или ошибка произошли *до* `NonCancellable`
      // блока,
      // но *после* установки `_rangeNetworkState.value = Loading`.
      if (_rangeNetworkState.value is EventNetworkState.Loading) {
        Log.w(
            TAG,
            "FADR FINALLY: State still Loading for $startDate to $endDate. This implies cancellation/error before NC block completed its state update. Resetting to Idle. Job: ${coroutineContext[Job]}")
        _rangeNetworkState.value = EventNetworkState.Idle // Безопасный сброс в состояние Idle
      }
      Log.d(
          TAG,
          "FADR: FINALLY block executed for $startDate to $endDate. Current _rangeNetworkState: ${_rangeNetworkState.value}. Job: ${coroutineContext[Job]}")
    }
  }

  /** Сохраняет события в БД, очищая старые данные для диапазона */
  private suspend fun saveEventsToDb(
      networkEvents: List<CalendarEvent>,
      startDate: LocalDate,
      endDate: LocalDate
  ) =
      withContext(ioDispatcher) { // Явно указываем IO
        // Получаем АКТУАЛЬНУЮ таймзону перед маппингом
        val currentTimeZoneId =
            try {
              settingsRepository.timeZoneFlow
                  .first() // Берем последнее значение из Flow настроек
                  .ifEmpty { ZoneId.systemDefault().id }
            } catch (e: Exception) {
              Log.w(TAG, "Could not get timezone from settings, using system default.", e)
              ZoneId.systemDefault().id
            }
        val zoneId =
            try {
              ZoneId.of(currentTimeZoneId)
            } catch (e: Exception) {
              ZoneId.systemDefault()
            }

        val eventEntities =
            networkEvents.mapNotNull { EventMapper.mapToEntity(it, zoneId.toString()) }

        val startRangeMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endRangeMillis =
            endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        Log.d(
            TAG,
            "Updating DB for range $startDate to $endDate. Deleting range [$startRangeMillis, $endRangeMillis), Inserting ${eventEntities.size} events.")
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
        Log.d(
            TAG,
            "Merged fetched range $fetchedRange with current ${current}. New loaded range: $updatedRange")
        updatedRange
      }
    }
  }

  /** Парсит JSON-ответ со списком событий */
  private fun parseEventsResponse(jsonString: String): List<CalendarEvent> {
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
          val description: String? =
              if (eventObject.has("description") && !eventObject.isNull("description")) {
                eventObject.getString("description")
              } else {
                null // Явно присваиваем Kotlin null
              } // Объявляем как nullable String
          val location: String? =
              if (eventObject.has("location") && !eventObject.isNull("location")) {
                eventObject.getString("location")
              } else {
                null // Явно присваиваем Kotlin null
              } // Объявляем как nullable String
          val recurringEventId: String?
          if (eventObject.has("recurringEventId") && !eventObject.isNull("recurringEventId")) {
            val str = eventObject.getString("recurringEventId")
            recurringEventId = if (str != "null") str.trim().takeIf { it.isNotEmpty() } else null
          } else {
            recurringEventId = null
          }
          val originalStartTimeObj: Any? = eventObject.opt("originalStartTime")
          val originalStartTime =
              if (originalStartTimeObj is String && originalStartTimeObj != "null") {
                originalStartTimeObj.trim().takeIf { it.isNotEmpty() }
              } else {
                null
              }

          var finalRRuleForCalendarEvent: String? = null
          if (eventObject.has("recurrenceRule") && !eventObject.isNull("recurrenceRule")) {
            val rawRecurrenceRuleFromApi = eventObject.getString("recurrenceRule")

            // Проверяем, что полученная строка не является буквально "null" и не пустая
            if (rawRecurrenceRuleFromApi != "null" && rawRecurrenceRuleFromApi.isNotBlank()) {
              if (rawRecurrenceRuleFromApi.startsWith("RRULE:")) {
                finalRRuleForCalendarEvent = rawRecurrenceRuleFromApi.removePrefix("RRULE:")
              } else {
                // Если префикса нет, но строка не "null" и не пустая, берем как есть
                finalRRuleForCalendarEvent = rawRecurrenceRuleFromApi
                Log.w(
                    TAG,
                    "Received recurrenceRule from backend without 'RRULE:' prefix: $rawRecurrenceRuleFromApi for event $id")
              }
            }
          }

          if (id.isNullOrEmpty() || startTimeStr.isNullOrEmpty()) {
            Log.w(
                TAG,
                "Skipping event due to missing id or startTime in JSON object: ${eventObject.optString("summary")}")
            continue
          }

          events.add(
              CalendarEvent(
                  id = id,
                  summary = summary,
                  startTime = startTimeStr,
                  endTime =
                      endTimeStr.takeIf { !it.isNullOrEmpty() }
                          ?: startTimeStr, // Используем твой fallback
                  description = description,
                  location = location,
                  isAllDay = isAllDay,
                  recurringEventId = recurringEventId,
                  originalStartTime = originalStartTime,
                  recurrenceRule = finalRRuleForCalendarEvent))
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

    private fun ClosedRange<LocalDate>.union(other: ClosedRange<LocalDate>): ClosedRange<LocalDate> {
    val newStart = minOf(this.start, other.start)
    val newEnd = maxOf(this.endInclusive, other.endInclusive)
    return newStart..newEnd
  }

  // Helper minOf/maxOf для LocalDate (если нет в стандартной библиотеке нужной версии)
  private fun minOf(a: LocalDate, b: LocalDate): LocalDate = if (a.isBefore(b)) a else b

  private fun maxOf(a: LocalDate, b: LocalDate): LocalDate = if (a.isAfter(b)) a else b
}
