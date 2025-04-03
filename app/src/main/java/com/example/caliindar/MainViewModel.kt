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

// Состояния UI
data class MainUiState(
    val message: String = "Нажмите Войти, затем Записать",
    val recordButtonText: String = "Записать",
    val isRecording: Boolean = false,
    val isSignedIn: Boolean = false,
    val isPermissionGranted: Boolean = false, // Добавим состояние разрешения
    val isLoading: Boolean = false, // Для индикации загрузки
    val userEmail: String? = null,
    val showAuthError: String? = null, // Для показа ошибок аутентификации
    val showGeneralError: String? = null // Для показа общих ошибок
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val audioRecorder = AudioRecorder(application.cacheDir)
    private var currentIdToken: String? = null

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
        private const val BACKEND_BASE_URL = "http://172.23.35.166:8000" // ВАШ ЛОКАЛЬНЫЙ IP
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
                recordButtonText = "Записать",
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

    // Вызывается при нажатии кнопки записи/стоп
    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            stopRecordingAndSend()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        if (!_uiState.value.isPermissionGranted) {
            _uiState.update { it.copy(showGeneralError = "Нет разрешения на запись аудио") }
            // UI должен запросить разрешение
            return
        }
        if (currentIdToken == null) {
            _uiState.update { it.copy(showAuthError = "Сначала войдите в аккаунт Google") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) { // Запись/чтение файла лучше в IO
            try {
                Log.i(TAG, "Starting audio recording...")
                audioRecorder.startRecording()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isRecording = true,
                            recordButtonText = "Стоп",
                            message = "Запись началась..."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isRecording = false, // Сбрасываем состояние
                            recordButtonText = "Записать",
                            showGeneralError = "Ошибка начала записи: ${e.message}"
                        )
                    }
                }
            }
        }
    }

    private fun stopRecordingAndSend() {
        var audioFile: File? = null // <--- Объявляем переменную здесь
        viewModelScope.launch(Dispatchers.IO) { // IO для работы с файлом и сетью
            try {
                Log.i(TAG, "Stopping audio recording...")
                audioFile = audioRecorder.stopRecording() // <--- Присваиваем значение здесь

                if (audioFile != null && currentIdToken != null) {
                    // Переключаемся на Main для обновления UI перед отправкой
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isRecording = false,
                                recordButtonText = "Записать",
                                isLoading = true, // Показываем загрузку во время отправки
                                message = "Отправка аудио на сервер..."
                            )
                        }
                    }
                    Log.d(TAG, "Audio file to send: ${audioFile?.absolutePath}, Size: ${audioFile?.length()} bytes")
                    // Отправляем аудио и ID токен (используем !! т.к. проверили на null)
                    sendAudioToBackend(audioFile!!, currentIdToken!!)

                } else {
                    val errorMsg = when {
                        audioFile == null -> "Не удалось получить файл записи."
                        currentIdToken == null -> "Ошибка: ID токен отсутствует. Войдите снова."
                        else -> "Неизвестная ошибка при остановке записи."
                    }
                    Log.e(TAG, errorMsg)
                    // Обновляем UI с ошибкой на главном потоке
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isRecording = false,
                                recordButtonText = "Записать",
                                isLoading = false,
                                showGeneralError = errorMsg
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping/sending recording", e)
                // Обновляем UI с ошибкой на главном потоке
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            recordButtonText = "Записать",
                            isLoading = false,
                            showGeneralError = "Ошибка остановки/отправки: ${e.message}"
                        )
                    }
                }
            } finally {
                // Убедимся, что временный файл удален после отправки или ошибки
                // Теперь audioFile доступна здесь
                val deleted = audioFile?.delete() // <--- Удаляем файл
                if (audioFile != null) {
                    Log.d(TAG, "Temp audio file ${audioFile?.name} deleted: $deleted")
                }

                // Сбрасываем isLoading на Main потоке, если он все еще true
                // (на случай если sendAudioToBackend не был вызван или завершился досрочно)
                if (_uiState.value.isLoading) { // Проверяем текущее состояние
                    withContext(Dispatchers.Main.immediate) { // immediate чтобы выполнилось до выхода из finally, если возможно
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    // --- Сетевые Запросы (возвращают Boolean успеха или обновляют State напрямую) ---

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


    private suspend fun sendAudioToBackend(audioFile: File, idToken: String) = withContext(Dispatchers.IO) {
        Log.i(TAG, "Sending audio and ID token to /process_audio")
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "audio",
                audioFile.name,
                audioFile.asRequestBody("audio/ogg".toMediaTypeOrNull())
            )
            .addFormDataPart("id_token_str", idToken)
            .build()

        val request = Request.Builder()
            .url("$BACKEND_BASE_URL/process_audio")
            .post(requestBody)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val responseBodyString = response.body?.string() // Читаем тело ОДИН РАЗ
                if (!response.isSuccessful) {
                    Log.e(TAG, "Server error processing audio: ${response.code} - $responseBodyString")
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(showGeneralError = "Ошибка сервера (${response.code}) при обработке аудио", isLoading = false) }
                    }
                } else {
                    Log.i(TAG, "Audio processed successfully. Response: $responseBodyString")
                    // Обрабатываем ответ на главном потоке
                    withContext(Dispatchers.Main) {
                        handleProcessAudioResponse(responseBodyString)
                        // isLoading будет сброшен в handleProcessAudioResponse или при ошибке
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error sending audio", e)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(showGeneralError = "Сетевая ошибка отправки аудио: ${e.message}", isLoading = false) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing backend response for audio", e)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(showGeneralError = "Ошибка обработки ответа сервера: ${e.message}", isLoading = false) }
            }
        }
    }


    // --- Обработка Ответов ---
    // Запускается на Main потоке
    private fun handleProcessAudioResponse(responseBody: String?) {
        var finalMessage = "Ошибка: Пустой ответ от сервера."
        var errorToShow: String? = null

        if (responseBody != null) {
            try {
                val json = JSONObject(responseBody)
                if (json.optString("status") == "success") {
                    val event = json.optJSONObject("event")
                    val eventLink = json.optString("event_link", null)
                    val recognizedText = json.optString("recognized_text", "") // Получаем распознанный текст

                    finalMessage = if (event != null) {
                        val eventName = event.optString("event_name", "Без названия")
                        val eventDate = event.optString("date", "Не указана")
                        val eventTime = event.optString("time", "Не указано")
                        var details = """
                         Событие создано!
                         Название: $eventName
                         Дата: $eventDate
                         Время: $eventTime
                        """.trimIndent()
                        if (!recognizedText.isNullOrEmpty()) {
                            details += "\nРаспознано: '$recognizedText'"
                        }
                        if (eventLink != null) {
                            details += "\nСсылка: $eventLink"
                        }
                        details
                    } else {
                        // Если события нет, просто показываем текст
                        "Аудио обработано. Распознано: '$recognizedText'"
                    }
                } else {
                    val detail = json.optString("detail", "Неизвестная ошибка обработки аудио")
                    errorToShow = "Ошибка обработки аудио: $detail"
                    finalMessage = "Не удалось обработать аудио." // Общее сообщение об ошибке
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing /process_audio response", e)
                errorToShow = "Ошибка парсинга ответа: ${e.message}"
                finalMessage = "Ошибка ответа сервера."
            }
        }

        // Обновляем UI один раз
        _uiState.update {
            it.copy(
                isLoading = false, // Завершили обработку/отправку
                message = finalMessage,
                showGeneralError = errorToShow // Показываем ошибку если была
            )
        }
    }


    // --- Управление Состояниями UI ---

    fun setAudioPermissionGranted(isGranted: Boolean) {
        _uiState.update { it.copy(isPermissionGranted = isGranted) }
        if (!isGranted) {
            // Можно добавить сообщение, если разрешение не дали
            // _uiState.update { it.copy(showGeneralError = "Разрешение на запись необходимо для работы.") }
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