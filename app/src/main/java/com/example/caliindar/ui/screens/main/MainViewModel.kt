package com.example.caliindar.ui.screens.main

import android.app.Application
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
import org.json.JSONObject
import com.example.caliindar.data.model.ChatMessage
import com.example.caliindar.util.AudioRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.io.IOException
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
    private val okHttpClient: OkHttpClient
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
        private const val BACKEND_BASE_URL = "http://172.23.35.150:8000"
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
            sendTextToServer(text = text, idToken = currentIdToken!!)
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

        // --- Если длительность достаточная ---
        // Добавляем плейсхолдер в чат (можно сделать и после успешной остановки)
    //    addChatMessage("[Аудиозапись]", isUser = true) // <-- Плейсхолдер

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
                    sendAudioToServer(audioFile!!, currentIdToken!!)
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

    // Отправка Аудио (вызывает executeProcessRequest)
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
                        val event = json.optJSONObject("event")
                        val eventLink = json.optString("event_link", null)
                        val eventName = event?.optString("event_name", "Событие") ?: "Событие"
                        statusMessage = "Событие создано" // Shorter status
                        // Format message for visualizer
                        messageForVisualizer = "✅ ${eventName} создано!" + (eventLink?.let { "\n[Ссылка]($it)" } ?: "")
                        finalAiState = AiVisualizerState.RESULT
                        Log.i(TAG, "Event creation successful.")
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
                isLoading = false, // Processing finished
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


    // Сброс флага ошибки после показа (например, в Snackbar или Toast)
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