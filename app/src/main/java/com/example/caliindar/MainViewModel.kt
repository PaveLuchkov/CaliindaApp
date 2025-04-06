package com.example.caliindar

import android.app.Application // Используем Application Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import androidx.compose.ui.text.capitalize
import java.util.Locale

// Состояния UI
data class MainUiState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val isLoading: Boolean = false,
    val isRecording: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val message: String = "Требуется вход.", // Оставляем для общих статусов/начального сообщения
    val showGeneralError: String? = null,
    val showAuthError: String? = null,
    val chatHistory: List<ChatMessage> = emptyList() // <-- Добавлено
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val audioRecorder = AudioRecorder(application.cacheDir)
    private var currentIdToken: String? = null
    private var recordingStartTime: Long = 0L // Для минимальной длительности


    // Google Sign-In
    private val gso: GoogleSignInOptions
    val googleSignInClient: GoogleSignInClient

    // Сеть
    private val okHttpClient: OkHttpClient

    // Константы
    companion object {
        private const val TAG = "MainViewModelAuth"
        private const val BACKEND_WEB_CLIENT_ID =
            "835523232919-o0ilepmg8ev25bu3ve78kdg0smuqp9i8.apps.googleusercontent.com"
        private const val BACKEND_BASE_URL = "http://172.23.35.249:8000" // ВАШ ЛОКАЛЬНЫЙ IP
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

        // Инициализация OkHttpClient
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY // Будьте осторожны с BODY в продакшене
            })
            .build()

        // Проверка состояния аутентификации при запуске ViewModel
        checkInitialAuthState()
    }

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
        viewModelScope.launch { // Запускаем в корутине
            // Silent sign-in для обновления токена, если пользователь уже входил
            try {
                val accountTask = googleSignInClient.silentSignIn()
                if (accountTask.isSuccessful) {
                    // Пользователь вошел и токен обновлен
                    Log.d(TAG, "Silent sign in success")
                    handleSignInResult(accountTask) // Обрабатываем как обычный вход
                } else {
                    // Пользователь не вошел или не дал согласия
                    Log.d(TAG, "Silent sign in failed or user not signed in")
                    // Проверим, есть ли последний залогиненный аккаунт (без обновления токена)
                    val lastAccount = GoogleSignIn.getLastSignedInAccount(getApplication())
                    val requiredScope = Scope("https://www.googleapis.com/auth/calendar.events")
                    if (lastAccount != null && GoogleSignIn.hasPermissions(lastAccount, requiredScope)) {
                        currentIdToken = lastAccount.idToken // Токен может быть старым!
                        Log.i(TAG, "User was already signed in (token might be stale): ${lastAccount.email}")
                        _uiState.update {
                            it.copy(
                                isSignedIn = true,
                                userEmail = lastAccount.email,
                                message = "Аккаунт: ${lastAccount.email} (Авторизован)",
                                isLoading = false
                            )
                        }
                        // Важно: Не отправляем на бэкенд старый authCode!
                        // Возможно, стоит запросить новый токен у бэкенда, если он может это сделать
                    } else {
                        Log.i(TAG, "User not signed in or permissions missing.")
                        signOutInternally("Требуется вход и авторизация") // Сброс состояния
                    }
                }
            } catch (e: ApiException) {
                Log.w(TAG, "Silent sign in failed with API exception", e)
                signOutInternally("Ошибка проверки аккаунта: ${e.statusCode}")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking initial auth state", e)
                signOutInternally("Ошибка проверки аккаунта: ${e.message}")
            }
        }
    }
    fun updatePermissionStatus(isGranted: Boolean) {
        if (_uiState.value.isPermissionGranted != isGranted) { // Обновляем только если изменилось
            _uiState.update { it.copy(isPermissionGranted = isGranted) }
            Log.d(TAG, "Audio permission status updated to: $isGranted")
        }
    }

    private fun addChatMessage(text: String, isUser: Boolean) {
        val newMessage = ChatMessage(text = text, isUser = isUser)
        _uiState.update {
            it.copy(chatHistory = it.chatHistory + newMessage)
        }
    }

    // --- Отправка Текстового Сообщения ---
    fun sendTextMessage(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "sendTextMessage called with blank text.")
            return
        }
        if (!_uiState.value.isSignedIn || currentIdToken == null) { /*...*/ return }
        if (_uiState.value.isLoading) { /*...*/ return }

        Log.d(TAG, "Attempting to send text message: '$text'")
        addChatMessage(text, isUser = true) // <-- Добавляем сообщение пользователя в чат

        viewModelScope.launch {
            withContext(Dispatchers.Main.immediate) {
                _uiState.update { it.copy(isLoading = true, message = "Отправка...") } // Общий статус
            }
            // Вызываем обновленный sendTextToBackend
            sendTextToServer(text = text, idToken = currentIdToken!!)
        }
    }

    fun startRecording() {
        if (_uiState.value.isRecording) { /*...*/ return }
        if (currentIdToken == null) { /*...*/ return }
        if (!_uiState.value.isPermissionGranted) { /*...*/ return }

        Log.d(TAG, "All checks passed. Launching recording coroutine...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                audioRecorder.startRecording()
                recordingStartTime = System.currentTimeMillis()
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isRecording = true, message = "Запись...") }
                }
                Log.i(TAG, "Recording started successfully at $recordingStartTime.")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e); recordingStartTime = 0L
                withContext(Dispatchers.Main) { /* Обновление UI с ошибкой */ }
                audioRecorder.cancelRecording()
            }
        }
    }

    fun stopRecordingAndSend() {
        if (!_uiState.value.isRecording) { /*...*/ return }

        val duration = System.currentTimeMillis() - recordingStartTime
        val MIN_RECORDING_DURATION_MS = 500

        Log.d(TAG, "Attempting to stop recording. Duration: $duration ms")

        if (duration < MIN_RECORDING_DURATION_MS) {
            Log.w(TAG, "Recording too short ($duration ms). Cancelling without saving.")
            viewModelScope.launch(Dispatchers.IO) {
                audioRecorder.cancelRecording()
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isRecording = false, isLoading = false, message = "Удерживайте кнопку дольше") }
                }
            }
            recordingStartTime = 0L
            return
        }

        // --- Если длительность достаточная ---
        // Добавляем плейсхолдер в чат
        addChatMessage("[Аудиозапись]", isUser = true) // <-- Плейсхолдер

        var audioFile: File? = null
        viewModelScope.launch { // Запускаем в основном scope для управления isLoading
            withContext(Dispatchers.Main.immediate) {
                _uiState.update { it.copy(isRecording = false, isLoading = true, message = "Обработка...") }
            }
            try {
                // Получаем файл (может быть IO)
                audioFile = withContext(Dispatchers.IO) {
                    Log.i(TAG, "Stopping audio recorder...")
                    audioRecorder.stopRecording() // Может бросить исключение
                }

                if (audioFile != null && currentIdToken != null) {
                    Log.d(TAG, "Audio file obtained: ${audioFile?.absolutePath}. Sending...")
                    // Вызываем обновленный sendAudioToServer
                    sendAudioToServer(audioFile!!, currentIdToken!!)
                    // Не удаляем файл здесь, удалим после отправки
                } else {
                    throw IOException("Failed to obtain audio file or token was null.") // Бросаем исключение для catch блока
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping/sending recording", e)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isRecording = false, // На всякий случай
                            isLoading = false,
                            showGeneralError = "Ошибка записи/отправки: ${e.message}",
                            message = "Ошибка записи"
                        )
                    }
                }
                // Попытка удалить файл, если он был создан, но отправка не началась
                audioFile?.let { file ->
                    withContext(Dispatchers.IO) {
                        val deleted = file.delete()
                        Log.d(TAG, "Deleted temp audio file after error: $deleted")
                    }
                }
            } finally {
                recordingStartTime = 0L
                // isLoading сбрасывается в handleProcessResponse или в catch
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

    // Отправка Текста (теперь использует multipart и вызывает handleProcessResponse)
    private suspend fun sendTextToServer(text: String, idToken: String) {
        Log.i(TAG, "Sending text and ID token to /process")
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("id_token_str", idToken)
            .addFormDataPart("text", text) // Отправляем текст как form data
            .build()

        val request = Request.Builder()
            .url("$BACKEND_BASE_URL/process") // Единый эндпоинт
            .post(requestBody)
            .build()

        executeProcessRequest(request) // Вызываем общий метод выполнения запроса
    }

    // Отправка Аудио (вызывает handleProcessResponse)
    private suspend fun sendAudioToServer(audioFile: File, idToken: String) {
        Log.i(TAG, "Sending audio and ID token to /process")
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "audio",
                audioFile.name,
                audioFile.asRequestBody("audio/ogg".toMediaTypeOrNull()) // Убедитесь, что тип правильный
            )
            .addFormDataPart("id_token_str", idToken)
            .build()

        val request = Request.Builder()
            .url("$BACKEND_BASE_URL/process") // Единый эндпоинт
            .post(requestBody)
            .build()

        // Выполняем запрос и удаляем файл после
        try {
            executeProcessRequest(request)
        } finally {
            // Удаляем файл после попытки отправки
            withContext(Dispatchers.IO) {
                val deleted = audioFile.delete()
                Log.d(TAG, "Temp audio file ${audioFile.name} deleted after request: $deleted")
            }
        }
    }

    private suspend fun executeProcessRequest(request: Request) = withContext(Dispatchers.IO) {
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val responseBodyString = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Server error processing request: ${response.code} - $responseBodyString")
                    // Пытаемся извлечь detail из JSON ошибки, если есть
                    var errorDetail = "Ошибка сервера (${response.code})"
                    try {
                        val jsonError = JSONObject(responseBodyString ?: "{}")
                        errorDetail = jsonError.optString("detail", errorDetail)
                    } catch (_: Exception) {}

                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(showGeneralError = errorDetail, isLoading = false, message = "Ошибка обработки") }
                    }
                } else {
                    Log.i(TAG, "Request processed successfully. Response: $responseBodyString")
                    withContext(Dispatchers.Main) {
                        handleProcessResponse(responseBodyString) // Вызываем новый обработчик
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during /process request", e)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(showGeneralError = "Сетевая ошибка: ${e.message}", isLoading = false, message = "Ошибка сети") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing /process request", e)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(showGeneralError = "Ошибка обработки запроса: ${e.message}", isLoading = false, message = "Ошибка") }
            }
        }
    }

    // --- Новый Обработчик Ответов от /process ---
    private fun handleProcessResponse(responseBody: String?) {
        var statusMessage = "Готово." // Статус по умолчанию
        var errorToShow: String? = null
        var botResponse: String? = null // Ответ или вопрос от бота

        if (responseBody.isNullOrBlank()) {
            errorToShow = "Пустой ответ от сервера."
            statusMessage = "Ошибка ответа"
        } else {
            try {
                val json = JSONObject(responseBody)
                val status = json.optString("status")
                val message = json.optString("message") // Сообщение от бэкенда (вопрос, ошибка, успех)

                when (status) {
                    "success" -> {
                        val event = json.optJSONObject("event")
                        val eventLink = json.optString("event_link", null)
                        statusMessage = message // "Event created successfully!"
                        val eventName = event?.optString("event_name", "Событие") ?: "Событие"
                        // Формируем подтверждение для чата
                        botResponse = "✅ ${eventName} создано!" + (eventLink?.let { "\n[Ссылка на событие]($it)" } ?: "")
                        Log.i(TAG, "Event creation successful.")
                    }
                    "clarification_needed" -> {
                        statusMessage = "Требуется уточнение..."
                        botResponse = message // Вопрос от LLM
                        Log.i(TAG, "Clarification needed: $botResponse")
                    }
                    "error" -> {
                        errorToShow = message // Сообщение об ошибке от бэкенда/LLM
                        statusMessage = "Ошибка обработки"
                        Log.e(TAG, "Backend processing error: $errorToShow")
                    }
                    "info", "unsupported" -> { // Обработка других статусов от бэкенда
                        statusMessage = message // Информационное сообщение
                        botResponse = message
                        Log.i(TAG, "Backend info/unsupported response: $message")
                    }
                    else -> { // Неизвестный статус
                        errorToShow = "Неизвестный статус ответа: '$status'"
                        statusMessage = "Ошибка ответа"
                        Log.w(TAG, "Unknown status from backend: $status")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing /process response", e)
                errorToShow = "Ошибка парсинга ответа: ${e.message}"
                statusMessage = "Ошибка ответа"
            }
        }

        // Обновляем UI один раз
        _uiState.update { currentState ->
            // Добавляем ответ бота в историю, если он есть
            val updatedHistory = botResponse?.let { responseText ->
                currentState.chatHistory + ChatMessage(text = responseText, isUser = false)
            } ?: currentState.chatHistory

            currentState.copy(
                isLoading = false, // Сбрасываем загрузку
                message = statusMessage, // Обновляем общий статус
                showGeneralError = errorToShow,
                chatHistory = updatedHistory // Обновляем историю чата
            )
        }
    }


    // Сброс флага ошибки после показа (например, в Snackbar или Toast)
    fun clearGeneralError() {
        _uiState.update { it.copy(showGeneralError = null) }
    }
    fun clearAuthError() {
        _uiState.update { it.copy(showAuthError = null) }
    }

    // Вызывается при уничтожении ViewModel
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