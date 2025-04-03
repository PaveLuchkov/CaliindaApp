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

    fun startRecording() {
        // Проверка записи и токена остается
        if (_uiState.value.isRecording) {
            Log.w(TAG, "Start recording called but already recording.")
            return
        }
        if (currentIdToken == null) {
            Log.w(TAG, "Start recording called but user not signed in (no token).")
            _uiState.update { it.copy(showGeneralError = "Ошибка: Войдите в систему для записи.") }
            return
        }

        // Проверка разрешения ПЕРЕД запуском корутины
        if (!_uiState.value.isPermissionGranted) {
            Log.w(TAG, "Start recording called but permission check failed (isPermissionGranted is false).")
            // НЕ показываем ошибку через state, как просили.
            // Пользователь должен увидеть запрос разрешения из Composable
            // и нажать кнопку снова после предоставления разрешения.
            _uiState.update { it.copy(message = "Требуется разрешение на запись аудио.") } // Можно обновить сообщение
            return
        }

        // Если все проверки пройдены
        Log.d(TAG, "All checks passed. Launching recording coroutine...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                audioRecorder.startRecording()
                // Обновляем UI на главном потоке
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isRecording = true,
                            message = "Запись..." // Обновляем сообщение
                        )
                    }
                }
                Log.i(TAG, "Recording started successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e)
                // Обработка ошибок старта записи
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isRecording = false, // Убедимся, что флаг сброшен
                            message = "Готово к записи.", // Сброс сообщения
                            showGeneralError = "Ошибка начала записи: ${e.message}"
                        )
                    }
                }
                // Попытка очистить ресурсы рекордера
                audioRecorder.cancelRecording()
            }
        }
    }

    // Остается без изменений по сравнению с предыдущим ответом
    fun stopRecordingAndSend() {
        if (!_uiState.value.isRecording) {
            Log.w(TAG, "Stop recording called but not currently recording.")
            return
        }

        Log.d(TAG, "Attempting to stop recording and send...")
        var audioFile: File? = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main.immediate) {
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            isLoading = true, // Показываем загрузку (отправка)
                            message = "Обработка и отправка..."
                        )
                    }
                }

                Log.i(TAG, "Stopping audio recorder...")
                audioFile = audioRecorder.stopRecording()

                if (audioFile != null && currentIdToken != null) {
                    Log.d(TAG, "Audio file obtained: ${audioFile?.absolutePath}, Size: ${audioFile?.length()} bytes. Sending...")
                    sendAudioToBackend(audioFile!!, currentIdToken!!)

                } else {
                    // Обработка ошибок получения файла / токена
                    val errorMsg = when {
                        audioFile == null -> "Не удалось получить файл записи после остановки."
                        currentIdToken == null -> "Ошибка: ID токен отсутствует перед отправкой. Войдите снова."
                        else -> "Неизвестная ошибка перед отправкой аудио."
                    }
                    Log.e(TAG, errorMsg)
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                showGeneralError = errorMsg,
                                message = "Готово к записи." // Сброс сообщения
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Обработка ошибок остановки / отправки
                Log.e(TAG, "Error stopping/sending recording", e)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            isLoading = false,
                            showGeneralError = "Ошибка остановки/отправки: ${e.message}",
                            message = "Готово к записи." // Сброс сообщения
                        )
                    }
                }
            } finally {
                // Удаление файла и сброс isLoading (как и раньше)
                val deleted = audioFile?.delete()
                if (audioFile != null) {
                    Log.d(TAG, "Temp audio file ${audioFile?.name} deleted: $deleted")
                }
                if (_uiState.value.isLoading && audioFile == null) {
                    withContext(Dispatchers.Main.immediate) {
                        _uiState.update { it.copy(isLoading = false, message = "Готово к записи.") }
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
                        /*
                        if (!recognizedText.isNullOrEmpty()) {
                            details += "\nРаспознано: '$recognizedText'"
                        }
                        if (eventLink != null) {
                            details += "\nСсылка: $eventLink"
                        }

                         */
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