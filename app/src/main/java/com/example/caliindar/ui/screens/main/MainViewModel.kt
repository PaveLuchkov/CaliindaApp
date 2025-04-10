package com.example.caliindar.ui.screens.main


import android.app.Application
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.caliindar.util.parseErrorDetail
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
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import com.google.android.gms.common.Scopes

// Переносим ViewModel сюда
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context, // Inject Context
    private val okHttpClient: OkHttpClient // Твой OkHttpClient
    // private val backendApi: YourBackendApi // Раскомментируй, если перейдешь на Retrofit/Ktor
) : AndroidViewModel(applicationContext as Application) { // Остаемся на AndroidViewModel, если нужен Application

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // --- Старые и Новые Клиенты ---
    private val credentialManager: CredentialManager = CredentialManager.create(applicationContext)
    private val authorizationClient: AuthorizationClient = Identity.getAuthorizationClient(applicationContext)
    private val googleSignInClient: GoogleSignInClient // Только для signOut

    // --- Конфигурация ---
    companion object {
        private const val TAG = "MainViewModelAuth"
        // ID ВЕБ-КЛИЕНТА твоего бэкенда
        private const val BACKEND_WEB_CLIENT_ID = "835523232919-o0ilepmg8ev25bu3ve78kdg0smuqp9i8.apps.googleusercontent.com"
        // Базовый URL бэкенда (лучше вынести)
        private const val BACKEND_BASE_URL = "http://10.4.70.185:8000"
    }

    // Скоупы, необходимые бэкенду для Calendar API
    private val requiredScopes = listOf(
        Scope("https://www.googleapis.com/auth/calendar.events")
    )

    // --- Вспомогательные переменные состояния ---
    private var pendingIdToken: String? = null // Для передачи между шагами
    private var currentIdToken: String? = null // Для запросов к /process после входа
    private var currentSignInJob: Job? = null // Для отмены процесса входа/авторизации

    // AudioRecorder и время записи (без изменений)
    private val audioRecorder = AudioRecorder(applicationContext.cacheDir)
    private var recordingStartTime: Long = 0L

    init {
        // Инициализируем GoogleSignInClient ТОЛЬКО для signOut
        val gsoForSignOut = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail() // Email может быть полезен для логов при выходе
            .build()
        googleSignInClient = GoogleSignIn.getClient(applicationContext, gsoForSignOut)

        // Проверяем начальное состояние (упрощено)
        checkInitialAuthStateSimplified()
    }

    // --- Шаг 1: Инициация всего процесса входа/авторизации ---
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun startSignInProcess() {
        // Отменяем предыдущий процесс, если он был запущен
        currentSignInJob?.cancel()
        currentSignInJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "Запрос аккаунта Google...", showAuthError = null, showGeneralError = null) }
            try {
                // --- Запускаем Шаг 1.1: Аутентификация через Credential Manager ---
                signInWithCredentialManager()
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isLoading = false, message = "Вход отменен.") }
                Log.i(TAG, "Sign-in process cancelled.")
            } catch (e: Exception) {
                // Общая ошибка на старте процесса
                Log.e(TAG, "Error starting sign in process", e)
                resetAuthState("Ошибка инициации входа: ${e.message}")
            }
        }
    }

    // --- Шаг 1.1: Аутентификация (Получение idToken) ---
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun signInWithCredentialManager() {
        // val nonce = generateNonce() // Генерируем Nonce

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BACKEND_WEB_CLIENT_ID)
            .build()
            // TODO .setNonce(nonce) // <--- Передавай Nonce, если бэкенд его проверяет ДЛЯ БЕЗОПАСНОСТИ!!!!


        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            Log.d(TAG, "Requesting credential from CredentialManager...")
            val result = credentialManager.getCredential(applicationContext, request)
            Log.d(TAG, "Credential received successfully.")
            handleCredentialResultAndProceed(result) // Успех -> Шаг 1.2

        } catch (e: GetCredentialException) {
            Log.e(TAG, "getCredential failed", e)
            handleCredentialError(e) // Обработка ошибок Credential Manager
        }
        // Другие Exception пробросятся в startSignInProcess
    }

    // --- Шаг 1.2: Обработка результата Credential Manager и запуск Шага 2 ---
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun handleCredentialResultAndProceed(result: GetCredentialResponse) {
        // Лог перед обработкой
        Log.d(TAG, "Processing credential result...")

        when (val credential = result.credential) {
            // --- ИЗМЕНЕНИЕ ЗДЕСЬ ---
            is CustomCredential -> {
                Log.i(TAG, "Received CustomCredential. Checking type...")
                // Проверяем ТИП внутри CustomCredential
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    Log.i(TAG, "CustomCredential type matches Google ID Token. Creating specific credential...")
                    try {
                        // Используем статический метод для создания GoogleIdTokenCredential из данных CustomCredential
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                        // --- Дальше логика как и была, но используем googleIdTokenCredential ---
                        Log.i(TAG, "Credential check 'is GoogleIdTokenCredential' (via CustomCredential) SUCCEEDED.")
                        val idToken = googleIdTokenCredential.idToken // Получаем токен из созданного объекта
                        pendingIdToken = idToken // Временно сохраняем

                        val userEmail = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id
                        Log.i(TAG, "Credential Manager Sign-In Success! Email/ID: $userEmail")
                        Log.d(TAG, "ID Token length: ${idToken.length}") // Возможно, idToken здесь не null

                        _uiState.update {
                            it.copy(
                                isLoading = true,
                                message = "Аккаунт получен (${userEmail}). Запрос доступа к календарю...",
                            )
                        }
                        requestAuthorizationCode() // Запускаем Шаг 2

                    } catch (e: Exception) {
                        // Ошибка при извлечении данных из CustomCredential
                        Log.e(TAG, "Failed to create GoogleIdTokenCredential from CustomCredential data", e)
                        resetAuthState("Ошибка обработки учетных данных Google: ${e.message}")
                    }
                } else {
                    // Получили CustomCredential, но с другим типом (маловероятно для Google Sign-In)
                    Log.e(TAG, "Received CustomCredential with unexpected type: ${credential.type}")
                    Log.e(TAG, "-> Check 'is GoogleIdTokenCredential' FAILED (was CustomCredential with wrong type).")
                    Log.e(TAG, "-> Credential data Bundle: ${credential.data}") // Логируем бандл на всякий случай
                    resetAuthState("Неожиданный тип CustomCredential: ${credential.type}")
                }
            }
            // Опционально: можно добавить обработку других *конкретных* типов, если они ожидаются
            // is PasswordCredential -> { ... }
            // is PublicKeyCredential -> { ... }

            else -> { // Сюда попадает, если это НЕ CustomCredential и НЕ другие обработанные типы
                val actualClassName = credential::class.java.name
                val typeProperty = credential.type
                Log.e(TAG, "Unexpected credential class type!")
                Log.e(TAG, "-> Check 'is GoogleIdTokenCredential' FAILED.")
                Log.e(TAG, "-> Actual credential object class: $actualClassName") // Логируем имя класса
                Log.e(TAG, "-> Credential.type property value: $typeProperty") // Логируем свойство type
                try {
                    Log.e(TAG, "-> Credential data Bundle: ${credential.data}")
                } catch (e: Exception) {
                    Log.e(TAG, "-> Failed to get credential data Bundle: $e")
                }
                resetAuthState("Неожиданный класс учетных данных (Класс: $actualClassName, Тип: $typeProperty)")
            }
        }
    }

    private suspend fun requestAuthorizationCode() {
        val idToken = pendingIdToken // Получаем сохраненный idToken
        if (idToken == null) {
            resetAuthState("Внутренняя ошибка: ID токен отсутствует перед запросом Authorize.") // Уточнил сообщение
            Log.w(TAG, "requestAuthorizationCode called but pendingIdToken is null.") // Добавил лог
            return
        }

        // val nonce = generateNonce() // Новый Nonce для этого шага, если нужен

        Log.d(TAG, "Building AuthorizationRequest with scopes: ${requiredScopes.joinToString { it.scopeUri }}") // Лог перед созданием запроса

        val request: AuthorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requiredScopes) // Явно указываем скоупы!
            // .setServerClientId(BACKEND_WEB_CLIENT_ID), нет в библиотеке
            // .setNonce(nonce) // <--- Передавай Nonce, если бэкенд проверяет его при ОБМЕНЕ КОДА
            .setOptOutIncludingGrantedScopes(true)
            .build()

        try {
            // --- ЛОГ ПЕРЕД ВЫЗОВОМ ---
            Log.i(TAG, "Attempting to call authorizationClient.authorize()...")

            val result: AuthorizationResult = authorizationClient.authorize(request).await()

            // --- ЛОГ СРАЗУ ПОСЛЕ ВЫЗОВА ---
            val authCode = result.serverAuthCode
            val grantedScopes = result.grantedScopes?.map { it.toString() } ?: emptyList() // Безопасное получение скоупов
            Log.i(TAG, "authorizationClient.authorize() completed.")
            Log.d(TAG, " -> Returned Auth Code: ${authCode ?: "null"}") // Логируем код (или null)
            Log.d(TAG, " -> Returned Granted Scopes: $grantedScopes") // Логируем полученные скоупы

            // --- Успех Шага 2 -> Переходим к Шагу 3 ---
            if (authCode != null) {
                Log.i(TAG, "Authorization Success! Auth Code received. Proceeding to send to backend.")
                // --- Шаг 3: Отправка данных на бэкенд ---
                sendAuthInfoToBackend(idToken, authCode) // Передаем оба токена
            } else {
                // --- ЛОГ, ЕСЛИ КОД = NULL ---
                Log.w(TAG, "Authorization completed BUT auth code is NULL. Granted scopes: $grantedScopes.")
                // Возможно, разрешения уже были даны ранее ИЛИ Google не вернул код по другой причине.
                // Если grantedScopes содержит нужный скоуп календаря, возможно, можно продолжить без нового authCode?
                // НО! Для ОБМЕНА на бэкенде обычно нужен свежий authCode при первом запросе скоупа.
                // Вероятнее всего, это состояние ошибки или неожиданное поведение.
                resetAuthState("Ошибка авторизации: не получен одноразовый код доступа от Google, хотя операция завершилась. Проверьте разрешения аккаунта.") // Более подробное сообщение
            }

        } catch (e: ApiException) {
            // --- УЛУЧШЕННОЕ ЛОГИРОВАНИЕ ОШИБКИ ApiException ---
            val statusCode = e.statusCode
            val statusMessage = e.statusMessage // Может быть null
            val statusCodeString = CommonStatusCodes.getStatusCodeString(statusCode) // Человекочитаемый статус
            Log.e(TAG, "Authorization failed with ApiException. Status: $statusCodeString ($statusCode), Message: $statusMessage", e) // Полный лог ошибки

            if (statusCode == CommonStatusCodes.CANCELED) {
                resetAuthState("Авторизация календаря отменена пользователем.") // Уточнил
            } else {
                // Показываем более детальную ошибку
                resetAuthState("Ошибка авторизации Google: $statusCodeString ($statusCode)")
            }
        } catch (e: Exception) {
            // --- ЛОГ ДРУГИХ ОШИБОК ---
            Log.e(TAG, "Unexpected error during authorizationClient.authorize()", e)
            resetAuthState("Неизвестная ошибка при авторизации календаря: ${e.message}")
        } finally {
            // pendingIdToken = null // Пока не будем очищать здесь, чтобы можно было попробовать еще раз, если это была временная ошибка? Или очищать? Решим по логам.
            // Очищать нужно, если мы УСПЕШНО отправили на бэкенд или точно знаем, что процесс прерван с ошибкой.
            Log.d(TAG, "Exiting requestAuthorizationCode function.") // Лог выхода
            // Оставляем очистку здесь, т.к. предполагаем, что каждый вызов startSignInProcess должен начинаться чисто.
            pendingIdToken = null
        }
    }

    // Вызывается при нажатии кнопки выхода
    fun signOut() {
        currentSignInJob?.cancel() // Отменяем любой текущий процесс входа
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "Выход...") }
            try {
                // Выходим из Google аккаунта на устройстве
                googleSignInClient.signOut().await()
                Log.i(TAG, "User signed out successfully from Google.")
                resetAuthState("Вы успешно вышли.") // Сбрасываем состояние
                // TODO: Опционально уведомить бэкенд об выходе (если он хранит сессии)
                // backendApi.notifySignOut(currentIdToken) // Пример
            } catch (e: Exception) {
                Log.e(TAG, "Error during sign out", e)
                // Даже если ошибка, сбрасываем состояние локально
                resetAuthState("Ошибка при выходе: ${e.message}")
            }
        }
    }

    private fun checkInitialAuthStateSimplified() {
        // Просто проверяем, есть ли последняя учетная запись для отображения email
        // НЕ ПЫТАЕМСЯ сделать silent sign-in или получить токены здесь
        viewModelScope.launch {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(applicationContext)
            if (lastAccount != null) {
                // Если есть аккаунт, покажем email, но состояние останется isSignedIn=false
                // Пользователю все равно нужно будет нажать "Войти", чтобы получить свежие токены/код
                _uiState.update {
                    it.copy(
                        userEmail = lastAccount.email,
                        message = "Аккаунт: ${lastAccount.email}. Нажмите 'Войти'."
                    )
                }
                Log.i(TAG, "Found last signed-in account: ${lastAccount.email}, but require explicit sign-in.")
            } else {
                // Нет аккаунта, сбрасываем состояние
                resetAuthState()
                Log.i(TAG, "No last signed-in account found.")
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
        if (!_uiState.value.isSignedIn || currentIdToken == null) { Log.w(TAG, "Cannot send message: Not signed in or token missing."); return }
        if (_uiState.value.isLoading) { Log.w(TAG, "Cannot send message: Already loading."); return }

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
                    _uiState.update { it.copy(isRecording = true, message = "Запись...") }
                }
                Log.i(TAG, "Recording started successfully at $recordingStartTime.")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording", e); recordingStartTime = 0L
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isRecording = false, showGeneralError = "Ошибка начала записи: ${e.message}") }
                }
                audioRecorder.cancelRecording()
            }
        }
    }

    fun stopRecordingAndSend() {
        if (!_uiState.value.isRecording) { Log.w(TAG, "Not recording, cannot stop."); return }

        val duration = System.currentTimeMillis() - recordingStartTime
        val MIN_RECORDING_DURATION_MS = 500

        Log.d(TAG, "Attempting to stop recording. Duration: $duration ms")

        // Сначала обновляем UI, что запись остановлена, но началась обработка
        // Делаем это до проверки длительности, чтобы кнопка сразу среагировала
        // Используем immediate, чтобы избежать "залипания" кнопки
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _uiState.update { it.copy(isRecording = false, isLoading = true, message = "Обработка...") }
        }


        if (duration < MIN_RECORDING_DURATION_MS) {
            Log.w(TAG, "Recording too short ($duration ms). Cancelling without saving.")
            viewModelScope.launch(Dispatchers.IO) { // Отмена записи в IO потоке
                audioRecorder.cancelRecording()
                withContext(Dispatchers.Main) { // Обновление UI обратно в Main
                    _uiState.update { it.copy(isLoading = false, message = "Удерживайте кнопку дольше") }
                }
            }
            recordingStartTime = 0L
            return
        }

        // --- Если длительность достаточная ---
        // Добавляем плейсхолдер в чат (можно сделать и после успешной остановки)
        addChatMessage("[Аудиозапись]", isUser = true) // <-- Плейсхолдер

        var audioFile: File? = null
        viewModelScope.launch { // Используем основной scope, так как isLoading уже true
            try {
                // Получаем файл (может быть IO)
                audioFile = withContext(Dispatchers.IO) {
                    Log.i(TAG, "Stopping audio recorder...")
                    audioRecorder.stopRecording() // Может бросить исключение
                }

                if (audioFile != null && currentIdToken != null) {
                    Log.d(TAG, "Audio file obtained: ${audioFile?.absolutePath}. Sending...")
                    // Вызываем sendAudioToServer (он уже suspend)
                    sendAudioToServer(audioFile!!, currentIdToken!!)
                    // Не удаляем файл здесь, удалим в finally sendAudioToServer
                } else {
                    // isLoading уже true, обновляем только сообщение/ошибку
                    _uiState.update { it.copy( showGeneralError = "Не удалось получить аудиофайл или токен.", message = "Ошибка файла") }
                    // Попытка удалить файл, если он был создан
                    audioFile?.let { file ->
                        withContext(Dispatchers.IO) { file.delete() }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping/sending recording", e)
                // isLoading уже true, обновляем только сообщение/ошибку
                _uiState.update {
                    it.copy(
                        showGeneralError = "Ошибка записи/отправки: ${e.message}",
                        message = "Ошибка записи"
                    )
                }
                // Попытка удалить файл, если он был создан, но отправка не началась
                audioFile?.let { file ->
                    withContext(Dispatchers.IO) { file.delete() }
                }
            } finally {
                recordingStartTime = 0L
                // isLoading сбросится в sendAudioToServer -> executeProcessRequest -> handleProcessResponse или в catch блоках
            }
        }
    }


    // Возвращает true при успехе, false при ошибке
    private suspend fun sendAuthInfoToBackend(idToken: String, authCode: String): Boolean = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(isLoading = true, message = "Обмен данными с сервером...") }
        Log.i(TAG, "Sending Auth Info (JSON) to /auth/google/exchange")
        val jsonObject = JSONObject().apply {
            put("id_token", idToken)
            put("auth_code", authCode) // Имя поля должно совпадать с FastAPI моделью
        }
        val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("$BACKEND_BASE_URL/auth/google/exchange")
            .post(requestBody)
            .build()

        var success = false
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val responseBodyString = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Backend error exchanging code (JSON): ${response.code} - $responseBodyString")
                    val detail = parseErrorDetail(responseBodyString) ?: "Ошибка сервера (${response.code})"
                    // Обновляем UI с ошибкой на главном потоке
                    withContext(Dispatchers.Main.immediate) { // immediate для быстрого UI update
                        resetAuthState(detail) // Сбрасываем состояние с ошибкой
                    }
                } else {
                    Log.i(TAG, "Backend successfully exchanged tokens (JSON). Response: $responseBodyString")
                    // Парсим email из ответа, если бэкенд его возвращает
                    val responseJson = try { JSONObject(responseBodyString ?: "{}") } catch (_: Exception) { JSONObject() }
                    val userEmail = responseJson.optString("user_email", null)

                    // УСПЕХ! Обновляем UI
                    withContext(Dispatchers.Main.immediate) {
                        currentIdToken = idToken // Сохраняем для запросов к /process
                        _uiState.update {
                            it.copy(
                                isSignedIn = true,
                                userEmail = userEmail, // Отображаем email от бэкенда
                                message = "Авторизация успешна! Готово к записи.",
                                isLoading = false,
                                showAuthError = null,
                                showGeneralError = null
                            )
                        }
                    }
                    success = true
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error sending auth info (JSON)", e)
            withContext(Dispatchers.Main.immediate) {
                resetAuthState("Сетевая ошибка обмена токенов: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing backend response for auth exchange", e)
            withContext(Dispatchers.Main.immediate) {
                resetAuthState("Ошибка обработки ответа сервера: ${e.message}")
            }
        }
        return@withContext success // Возвращаем результат операции
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun handleCredentialError(e: GetCredentialException) {
        val message = when (e) {
            // Добавь другие типы ошибок по необходимости
            else -> "Не найден аккаунт Google для входа."
        }
        resetAuthState(message) // Сбрасываем состояние с ошибкой
    }

    // --- Сброс состояния аутентификации (бывший signOutInternally) ---
    private fun resetAuthState(errorMessage: String? = null) {
        pendingIdToken = null
        currentIdToken = null // Сбрасываем основной токен
        _uiState.update {
            // Создаем НОВЫЙ объект состояния, сбрасывая все поля, кроме, возможно, истории чата и пермишенов
            MainUiState(
                message = errorMessage ?: "Требуется вход.", // Показываем ошибку или стандартное сообщение
                showAuthError = errorMessage, // Дублируем в поле ошибки для показа Snackbar/Toast
                isPermissionGranted = it.isPermissionGranted, // Сохраняем статус пермишена
                chatHistory = it.chatHistory // Сохраняем историю чата
            )
        }
        Log.i(TAG, "Auth state reset. Message: $errorMessage")
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
                // Важно: Обработка ответа должна быть в Main потоке для обновления UI
                withContext(Dispatchers.Main) {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Server error processing request: ${response.code} - $responseBodyString")
                        var errorDetail = "Ошибка сервера (${response.code})"
                        try {
                            val jsonError = JSONObject(responseBodyString ?: "{}")
                            errorDetail = jsonError.optString("detail", errorDetail)
                        } catch (_: Exception) {}
                        _uiState.update { it.copy(showGeneralError = errorDetail, isLoading = false, message = "Ошибка обработки") }
                    } else {
                        Log.i(TAG, "Request processed successfully. Response: $responseBodyString")
                        handleProcessResponse(responseBodyString) // Вызываем обработчик (он уже Main safe)
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
    // Эта функция должна вызываться в Main потоке, т.к. обновляет UI
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
                        botResponse = "✅ $eventName создано!" + eventLink.let { "\n[Ссылка на событие]($it)" }
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