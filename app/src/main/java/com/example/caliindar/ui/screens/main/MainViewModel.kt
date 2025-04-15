package com.example.caliindar.ui.screens.main

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.caliindar.data.local.EventDao
import com.example.caliindar.data.mapper.EventMapper
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.example.caliindar.data.model.ChatMessage
import com.example.caliindar.util.AudioRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONException
import java.io.File
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

enum class AiVisualizerState {
    IDLE,      // Начальное состояние / Ничего не происходит (кнопка видима)
    RECORDING, // Пользователь записывает голос (фигура большая, крутится внизу)
    THINKING,  // ИИ обрабатывает запрос (фигура меньше, крутится в центре)
    ASKING,    // ИИ задает уточняющий вопрос (фигура внизу, показывает текст)
    RESULT,     // ИИ показывает результат/событие (фигура внизу, показывает текст/данные)
    ERROR
}


// Переносим ViewModel сюда
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val okHttpClient: OkHttpClient,
    private val eventDao: EventDao
): AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _aiState = MutableStateFlow(AiVisualizerState.IDLE)
    val aiState: StateFlow<AiVisualizerState> = _aiState.asStateFlow()

    private val _aiMessage = MutableStateFlow<String?>(null) // Текст для ASKING/RESULT
    val aiMessage: StateFlow<String?> = _aiMessage.asStateFlow()

    val isAiRotating: StateFlow<Boolean> = aiState.map {
        it == AiVisualizerState.RECORDING || it == AiVisualizerState.THINKING
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Если AudioRecorder не внедряется, создаем его здесь
    private val audioRecorder = AudioRecorder(application.cacheDir)
    private var currentIdToken: String? = null
    private var recordingStartTime: Long = 0L

    // Google Sign-In
    private val gso: GoogleSignInOptions
    val googleSignInClient: GoogleSignInClient

    companion object {
        private const val TAG = "MainViewModelAuth"
        private const val BACKEND_WEB_CLIENT_ID =
            "835523232919-o0ilepmg8ev25bu3ve78kdg0smuqp9i8.apps.googleusercontent.com"
        // Лучше вынести URL в BuildConfig или другой модуль
        //private const val BACKEND_BASE_URL = "http://172.23.35.150:8000"
        private const val BACKEND_BASE_URL = "http://172.29.96.1:8000"
    }

    init {
        // Инициализация Google Sign-In Client
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar.events"))
            .requestServerAuthCode(BACKEND_WEB_CLIENT_ID)
            .requestIdToken(BACKEND_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(application, gso)


        checkInitialAuthState()
    }

    // Состояние для списка событий
    sealed interface EventsUiState {
        object Loading : EventsUiState
        data class Success(val events: List<CalendarEvent>) : EventsUiState
        data class Error(val message: String) : EventsUiState
        object Idle : EventsUiState // Начальное состояние или если не запрашивали
    }

    sealed interface EventNetworkState {
        object Idle : EventNetworkState
        object Loading : EventNetworkState
        data class Error(val message: String) : EventNetworkState
    }
    private val _eventNetworkState = MutableStateFlow<EventNetworkState>(EventNetworkState.Idle)
    val eventNetworkState: StateFlow<EventNetworkState> = _eventNetworkState.asStateFlow()

    // StateFlow для управления состоянием событий
    private val _eventsState = MutableStateFlow<EventsUiState>(EventsUiState.Idle)
    val eventsState: StateFlow<EventsUiState> = _eventsState.asStateFlow()

    // Дата, для которой отображаем события (по умолчанию - сегодня)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()


    @OptIn(ExperimentalCoroutinesApi::class) // Для flatMapLatest
    val calendarEventsState: StateFlow<List<CalendarEvent>> = selectedDate.flatMapLatest { date ->
        // Вычисляем начало и конец дня в UTC миллисекундах
        val startOfDayUTC = date.atStartOfDay(ZoneOffset.UTC)
        val startMillis = startOfDayUTC.toInstant().toEpochMilli()
        // Конец дня - это начало следующего дня
        val endMillis = startOfDayUTC.plusDays(1).toInstant().toEpochMilli()

        Log.d(TAG, "Observing DB for date: $date (Range UTC millis: $startMillis..<$endMillis)")

        // Получаем Flow из DAO
        eventDao.getEventsForDateRangeFlow(startMillis, endMillis)
            .map { entityList ->
                // Маппим Entity в Domain/UI модель
                entityList.map { EventMapper.mapToDomain(it) }
            }
            .catch { e ->
                // Обработка ошибок чтения из БД (маловероятно, но возможно)
                Log.e(TAG, "Error reading events from DB for date $date", e)
                emit(emptyList<CalendarEvent>()) // Возвращаем пустой список при ошибке
                // Можно также обновить _eventNetworkState для отображения ошибки БД
                _eventNetworkState.value = EventNetworkState.Error("Ошибка чтения локальных событий")
            }
    }.stateIn( // Преобразуем Flow в StateFlow для UI
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Начинаем сбор, когда есть подписчики
        initialValue = emptyList() // Начальное значение - пустой список
    )




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
                        fetchEventsForSelectedDate()
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
                isRecording = false,
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
                    fetchEventsForSelectedDate() // TODO Возможно, лучше сделать это в LaunchedEffect в Composable.
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
// - ---- -- -  БЛОК КАЛЕНДАРЯ

    // Функция для вызова при необходимости загрузить/обновить события
    internal fun fetchEventsForSelectedDate() {
        // Используем Job, чтобы избежать запуска нескольких одновременных запросов
        // (Хотя проверка _eventNetworkState уже частично решает это)
        if (_eventNetworkState.value is EventNetworkState.Loading) return

        viewModelScope.launch {
            val dateToFetch = _selectedDate.value // Берем текущую выбранную дату
            Log.i(TAG, "Starting background fetch for events on date: $dateToFetch")

            if (!_uiState.value.isSignedIn) {
                Log.w(TAG, "Cannot fetch events: User not signed in.")
                // Не меняем eventNetworkState, т.к. это не ошибка сети
                return@launch
            }

            // Устанавливаем состояние загрузки для UI (например, индикатора в AppBar)
            _eventNetworkState.value = EventNetworkState.Loading

            val freshToken = getFreshIdToken() // Получаем свежий ID токен
            if (freshToken == null) {
                Log.w(TAG, "Cannot fetch events: Failed to get fresh ID token.")
                _eventNetworkState.value = EventNetworkState.Error("Ошибка аутентификации для обновления")
                // signOutInternally уже был вызван внутри getFreshIdToken, если токен получить не удалось
                return@launch
            }

            // Формируем URL для эндпоинта бэкенда
            val url = "$BACKEND_BASE_URL/calendar/events?date=${dateToFetch.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Authorization", "Bearer $freshToken")
                .build()

            try {
                val networkEvents: List<CalendarEvent> = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute().use { response ->
                        val responseBodyString = response.body?.string()
                        if (!response.isSuccessful) {
                            val errorMsg = parseBackendError(responseBodyString, response.code)
                            Log.e(TAG, "Error fetching events from backend: ${response.code} - $errorMsg")
                            throw IOException("Backend error: ${response.code} - $errorMsg") // Бросаем исключение
                        }
                        if (responseBodyString.isNullOrBlank()) {
                            Log.w(TAG, "Empty response body received for events on $dateToFetch.")
                            emptyList() // Пустой ответ - значит нет событий
                        } else {
                            Log.i(TAG, "Events fetched successfully from network for $dateToFetch.")
                            parseEventsResponse(responseBodyString) // Парсим ответ
                        }
                    }
                } // End Dispatchers.IO

                // --- Успешный ответ сети ---
                // Маппим сетевые события в Entity
                val eventEntities = networkEvents.mapNotNull { EventMapper.mapToEntity(it) }

                // Определяем диапазон для очистки/вставки в БД (начало и конец дня в UTC)
                val startOfDayUTC = dateToFetch.atStartOfDay(ZoneOffset.UTC)
                val startMillis = startOfDayUTC.toInstant().toEpochMilli()
                val endMillis = startOfDayUTC.plusDays(1).toInstant().toEpochMilli()

                // Сохраняем в БД (атомарно удаляем старые для этого дня и вставляем новые)
                Log.d(TAG, "Updating DB for $dateToFetch. Deleting range $startMillis..<$endMillis, Inserting ${eventEntities.size} events.")
                eventDao.clearAndInsertEventsForRange(startMillis, endMillis, eventEntities)
                Log.i(TAG, "Successfully updated DB with events for $dateToFetch.")

                // Сбрасываем состояние ошибки/загрузки
                _eventNetworkState.value = EventNetworkState.Idle

            } catch (e: IOException) { // Ловим сетевые ошибки и ошибки ответа сервера
                Log.e(TAG, "Network error fetching events for $dateToFetch", e)
                _eventNetworkState.value = EventNetworkState.Error("Ошибка сети: ${e.message}")
            } catch (e: Exception) { // Ловим ошибки парсинга JSON или другие неожиданные
                Log.e(TAG, "Error processing events response for $dateToFetch", e)
                _eventNetworkState.value = EventNetworkState.Error("Ошибка обработки данных: ${e.message}")
            }
            // 'finally' не нужен, т.к. состояние сбрасывается в Idle или Error внутри try/catch
        }
    }

    private suspend fun fetchEvents(date: LocalDate) {
        if (!_uiState.value.isSignedIn) {
            Log.w(TAG, "Cannot fetch events: User not signed in.")
            _eventsState.value = EventsUiState.Error("Требуется вход в аккаунт")
            return
        }

        _eventsState.value = EventsUiState.Loading // Показываем загрузку
        Log.d(TAG, "Fetching events for date: $date")

        val freshToken = getFreshIdToken() // Получаем свежий ID токен для аутентификации на НАШЕМ бэкенде
        if (freshToken == null) {
            Log.w(TAG, "Cannot fetch events: Failed to get fresh ID token.")
            // getFreshIdToken уже обновит uiState с ошибкой аутентификации
            _eventsState.value = EventsUiState.Error("Ошибка аутентификации")
            return
        }

        // Формируем URL для нового эндпоинта бэкенда
        // Передаем дату как параметр запроса
        val url = "$BACKEND_BASE_URL/calendar/events?date=${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}"

        val request = Request.Builder()
            .url(url)
            .get() // GET запрос
            .header("Authorization", "Bearer $freshToken") // Передаем ID токен для аутентификации на бэкенде
            .build()

        withContext(Dispatchers.IO) {
            try {
                okHttpClient.newCall(request).execute().use { response ->
                    val responseBodyString = response.body?.string()
                    if (response.isSuccessful && responseBodyString != null) {
                        Log.i(TAG, "Events fetched successfully for $date. Response: $responseBodyString")
                        try {
                            // Парсим JSON ответ от нашего бэкенда
                            val eventsList = parseEventsResponse(responseBodyString)
                            _eventsState.value = EventsUiState.Success(eventsList)
                        } catch (e: JSONException) {
                            Log.e(TAG, "Error parsing events JSON", e)
                            _eventsState.value = EventsUiState.Error("Ошибка парсинга ответа сервера")
                        }
                    } else {
                        Log.e(TAG, "Error fetching events from backend: ${response.code} - $responseBodyString")
                        val errorMsg = parseBackendError(responseBodyString, response.code)
                        _eventsState.value = EventsUiState.Error(errorMsg)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error fetching events", e)
                _eventsState.value = EventsUiState.Error("Сетевая ошибка: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error fetching events", e)
                _eventsState.value = EventsUiState.Error("Неизвестная ошибка: ${e.message}")
            }
        }
    }

    private fun parseEventsResponse(jsonString: String): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        try {
            val jsonArray = org.json.JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val eventObject = jsonArray.getJSONObject(i)
                events.add(
                    CalendarEvent(
                        id = eventObject.getString("id"),
                        summary = eventObject.getString("summary"),
                        startTime = eventObject.getString("startTime"), // Строка ISO
                        endTime = eventObject.getString("endTime"),     // Строка ISO
                        description = eventObject.optString("description", null),
                        location = eventObject.optString("location", null)
                    )
                )
            }
        } catch (e: org.json.JSONException) {
            Log.e(TAG, "Failed to parse events JSON array", e)
            // Можно бросить исключение, чтобы оно было поймано в fetchEventsForSelectedDate
            throw e
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

    // Функция для изменения выбранной даты (если захотите добавить выбор даты)
    fun setSelectedDate(newDate: LocalDate) {
        if (newDate != _selectedDate.value) {
            Log.d(TAG, "Selected date changed to: $newDate")
            _selectedDate.value = newDate
            fetchEventsForSelectedDate()
        }
    }

    fun refreshEvents() {
        if (_eventNetworkState.value is EventNetworkState.Loading) {
            Log.d(TAG, "Refresh request ignored, already loading events.")
            return // Не запускаем, если уже идет загрузка
        }
        Log.d(TAG, "Manual refresh triggered for date: ${_selectedDate.value}")
        fetchEventsForSelectedDate()
    }

    fun formatEventTimeForDisplay(timeString: String?, pattern: String = "HH:mm"): String {
        if (timeString.isNullOrBlank()) return "--:--" // Или другой плейсхолдер
        return try {
            // 1. Парсим строку (она должна быть ISO 8601, скорее всего UTC из маппера)
            val offsetDateTime = OffsetDateTime.parse(timeString)

            // 2. Получаем системный часовой пояс устройства
            val localZoneId = ZoneId.systemDefault()

            // 3. Конвертируем время В ЛОКАЛЬНЫЙ часовой пояс
            val localDateTime = offsetDateTime.atZoneSameInstant(localZoneId)

            // 4. Создаем форматтер для вывода
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale("ru"))

            // 5. Форматируем ЛОКАЛЬНОЕ время
            localDateTime.format(formatter)

        } catch (e: DateTimeParseException) {
            // Пробуем распарсить как дату (для событий "весь день")
            try {
                // Если парсится как LocalDate, это событие на весь день
                java.time.LocalDate.parse(timeString)
                "Весь день" // Возвращаем специальную строку
            } catch (e2: DateTimeParseException) {
                Log.e(TAG, "Error parsing date/time string for display: $timeString", e)
                "Ошибка времени"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date/time for display: $timeString", e)
            "Ошибка времени"
        }
    }

    // Используем новую функцию в методе для списка
    fun formatEventListTime(startTimeStr: String, endTimeStr: String): String {
        val startTimeFormatted = formatEventTimeForDisplay(startTimeStr)
        val endTimeFormatted = formatEventTimeForDisplay(endTimeStr)

        // Если оба "Весь день", возвращаем просто "Весь день"
        if (startTimeFormatted == "Весь день" && endTimeFormatted == "Весь день") {
            return "Весь день"
        }
        // Если только начало "Весь день" (маловероятно, но все же)
        if (startTimeFormatted == "Весь день") {
            return "до $endTimeFormatted"
        }
        // Если только конец "Весь день" (тоже маловероятно)
        if (endTimeFormatted == "Весь день") {
            return "$startTimeFormatted весь день"
        }

        // Обычный случай с временем
        return "$startTimeFormatted - $endTimeFormatted"
    }

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

    fun startRecording() {
        if (_uiState.value.isRecording) { Log.w(TAG, "Already recording."); return }
        if (currentIdToken == null) { Log.w(TAG, "Cannot record: Token missing."); return }
        if (!_uiState.value.isPermissionGranted) { Log.w(TAG, "Cannot record: Permission not granted."); return }
        // Добавим проверку на isLoading, чтобы не начать запись во время другого запроса
        if (_uiState.value.isLoading) { Log.w(TAG, "Cannot record: App is busy (isLoading)."); return }


        Log.d(TAG, "All checks passed. Launching recording coroutine...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                audioRecorder.startRecording()
                recordingStartTime = System.currentTimeMillis()
                withContext(Dispatchers.Main) {
                    // Update UI state (isRecording for button logic if needed)
                    _uiState.update { it.copy(isRecording = true, message = "Запись...") }
                    // Set AI Visualizer State
                    _aiState.value = AiVisualizerState.RECORDING
                    _aiMessage.value = null // Clear any previous message
                }
                Log.i(TAG, "Recording started successfully at $recordingStartTime.")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e); recordingStartTime = 0L
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isRecording = false, showGeneralError = "Ошибка начала записи: ${e.message}") }
                    _aiState.value = AiVisualizerState.IDLE // Revert state on error
                }
                audioRecorder.cancelRecording() // Ensure cleanup
            }
        }
    }

    fun stopRecordingAndSend() {
        if (!_uiState.value.isRecording) { Log.w(TAG, "Not recording, cannot stop."); return }

        val duration = System.currentTimeMillis() - recordingStartTime
        val MIN_RECORDING_DURATION_MS = 500

        Log.d(TAG, "Attempting to stop recording. Duration: $duration ms")

        viewModelScope.launch(Dispatchers.Main.immediate) {
            _uiState.update { it.copy(isRecording = false, isLoading = true, message = "Обработка...") }
            _aiState.value = AiVisualizerState.THINKING
            _aiMessage.value = null
        }


        if (duration < MIN_RECORDING_DURATION_MS) {
            Log.w(TAG, "Recording too short ($duration ms). Cancelling without saving.")
            viewModelScope.launch(Dispatchers.IO) {
                audioRecorder.cancelRecording()
                withContext(Dispatchers.Main) {
                    // Reset isLoading, show message, revert AI state
                    _uiState.update { it.copy(isLoading = false, message = "Удерживайте кнопку дольше") }
                    _aiState.value = AiVisualizerState.IDLE
                }
            }
            recordingStartTime = 0L
            return
        }

        var audioFile: File? = null
        viewModelScope.launch {
            try {
                audioFile = withContext(Dispatchers.IO) {
                    Log.i(TAG, "Stopping audio recorder...")
                    audioRecorder.stopRecording()
                }

                if (audioFile != null && currentIdToken != null) {
                    Log.d(TAG, "Audio file obtained: ${audioFile?.absolutePath}. Sending...")
                    // AI state is already THINKING
                    sendAudioToServer(audioFile!!)
                } else {
                    // Error getting file or token
                    withContext(Dispatchers.Main) { // Ensure UI update on Main
                        _uiState.update { it.copy( showGeneralError = "Не удалось получить аудиофайл или токен.", message = "Ошибка файла", isLoading = false) }
                        _aiState.value = AiVisualizerState.IDLE // Revert state
                    }
                    audioFile?.let { file -> withContext(Dispatchers.IO) { file.delete() } }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping/sending recording", e)
                withContext(Dispatchers.Main) { // Ensure UI update on Main
                    _uiState.update {
                        it.copy(
                            showGeneralError = "Ошибка записи/отправки: ${e.message}",
                            message = "Ошибка записи",
                            isLoading = false // Reset loading on error
                        )
                    }
                    _aiState.value = AiVisualizerState.IDLE // Revert state
                }
                audioFile?.let { file -> withContext(Dispatchers.IO) { file.delete() } }
            } finally {
                recordingStartTime = 0L
                // isLoading and aiState are handled within the success/error paths of sendAudioToServer -> handleProcessResponse
            }
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
            .url("$BACKEND_BASE_URL/auth/google/exchange")
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
            .build()

        val request = Request.Builder()
            .url("$BACKEND_BASE_URL/process")
            .header("Authorization", "Bearer $freshToken")
            .post(requestBody)
            .build()

        executeProcessRequest(request)
    }

    // Отправка Аудио (теперь использует getFreshIdToken)
    private suspend fun sendAudioToServer(audioFile: File) { // Убрали idToken из аргументов
        val freshToken = getFreshIdToken() // Пытаемся получить свежий токен ПЕРЕД запросом

        if (freshToken == null) {
            Log.w(TAG, "sendAudioToServer failed: Could not get fresh token.")
            _uiState.update { it.copy(isLoading = false) }
            _aiState.value = AiVisualizerState.IDLE
            // Удаляем файл, так как отправка не удалась из-за токена
            withContext(Dispatchers.IO) {
                val deleted = audioFile.delete()
                Log.d(TAG, "Temp audio file ${audioFile.name} deleted due to token error: $deleted")
            }
            return
        }
        val currentDateTime = LocalDateTime.now()
        val timeZone = ZoneId.systemDefault()
        Log.i(TAG, "Sending audio and FRESH ID token to /process")
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "audio",
                audioFile.name,
                audioFile.asRequestBody("audio/ogg".toMediaTypeOrNull())
            )
            .addFormDataPart("time", currentDateTime.toString())
            .addFormDataPart("timeZone", timeZone.toString())
            .build()

        val request = Request.Builder()
            .url("$BACKEND_BASE_URL/process")
            .header("Authorization", "Bearer $freshToken")
            .post(requestBody)
            .build()

        // Выполняем запрос и удаляем файл после (в блоке finally)
        try {
            executeProcessRequest(request)
        } finally {
            withContext(Dispatchers.IO) {
                val deleted = audioFile.delete()
                Log.d(TAG, "Temp audio file ${audioFile.name} deleted after request attempt: $deleted")
            }
        }
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
                        fetchEventsForSelectedDate()
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
    fun formatDisplayTime(isoTimeString: String?): String {
        if (isoTimeString.isNullOrBlank()) return "Время не указано"
        // Используем ту же логику форматирования с учетом пояса, но другой паттерн
        val localTimeStr =
            formatEventTimeForDisplay(isoTimeString, "d MMMM") // Получаем дату в лок. поясе
        // Если вернулась ошибка или "Весь день", используем это
        if (localTimeStr == "Ошибка времени" || localTimeStr == "Весь день") {
            return localTimeStr
        }
        // Иначе возвращаем отформатированную дату
        return localTimeStr
    }

    fun clearNetworkError() { _eventNetworkState.update { EventNetworkState.Idle } }

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
        // Остановить запись и освободить ресурсы, если ViewModel уничтожается во время записи
        if (_uiState.value.isRecording) {
            viewModelScope.launch(Dispatchers.IO) {
                audioRecorder.cancelRecording() // Используем новый метод отмены
            }
        }
        Log.d(TAG, "MainViewModel cleared")
    }


}


