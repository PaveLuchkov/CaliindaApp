package com.lpavs.caliinda.feature.agent.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.lpavs.caliinda.R
import com.lpavs.caliinda.app.di.IoDispatcher
import com.lpavs.caliinda.app.di.MainDispatcher
import com.lpavs.caliinda.core.data.auth.AuthManager
import com.lpavs.caliinda.core.data.di.BackendUrl
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.feature.agent.data.model.AiVisualizerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiInteractionManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val authManager: AuthManager, // Для получения токена
    private val settingsRepository: SettingsRepository, // Для получения temper
    @BackendUrl private val backendBaseUrl: String,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher // Нужен для SpeechRecognizer
) {
  private val TAG = "AiInteractionManager"
  // Важно: Используем SupervisorJob, чтобы ошибка в одной корутине не отменила весь скоуп
  private val managerScope = CoroutineScope(SupervisorJob() + ioDispatcher) // Основной скоуп для IO

  // --- Состояния для ViewModel ---
  private val _aiState = MutableStateFlow(AiVisualizerState.IDLE)
  val aiState: StateFlow<AiVisualizerState> = _aiState.asStateFlow()

  private val _aiMessage = MutableStateFlow<String?>(null) // Сообщение для UI (Asking/Result)
  val aiMessage: StateFlow<String?> = _aiMessage.asStateFlow()

  // --- SpeechRecognizer ---
  private var speechRecognizer: SpeechRecognizer? = null
  private val speechRecognizerIntent: Intent
  private var isRecognizerInitialized = false
  private var isListening = false // Внутренний флаг для управления

  init {
    Log.d(TAG, "Initializing AiInteractionManager...")
    speechRecognizerIntent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(
              RecognizerIntent.EXTRA_LANGUAGE,
              Locale.getDefault().toString()) // Или язык из настроек
          putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...")
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    // Инициализируем распознаватель заранее в основном потоке
    initializeSpeechRecognizer()
  }

  // --- Публичные методы для ViewModel ---

  fun startListening() {
    if (isListening) {
      Log.w(TAG, "Already listening.")
      return
    }
    if (!isRecognizerInitialized) {
      Log.w(TAG, "Recognizer not ready, attempting to initialize...")
      // Попробуем инициализировать еще раз на всякий случай
      initializeSpeechRecognizer()
      if (!isRecognizerInitialized) {
        Log.e(TAG, "Cannot start listening: SpeechRecognizer failed to initialize.")
        // Можно передать ошибку в ViewModel через _errorEvent.emit("...")
        _aiState.value = AiVisualizerState.IDLE // Сброс состояния
        return
      }
    }

    Log.d(TAG, "Attempting to start listening...")
    // Важно: Вызовы SpeechRecognizer должны быть на Main потоке
    managerScope.launch(mainDispatcher) {
      try {
        isListening = true
        _aiState.value = AiVisualizerState.LISTENING
        _aiMessage.value = null // Очищаем сообщение
        speechRecognizer?.startListening(speechRecognizerIntent)
        Log.d(TAG, "Called startListening on SpeechRecognizer")
      } catch (e: Exception) {
        Log.e(TAG, "Error starting listening", e)
        handleRecognitionError("Ошибка старта: ${e.message}")
      }
    }
  }

  fun stopListening() {
    if (!isListening) {
      // Log.w(TAG, "Not currently listening.") // Можно не логировать
      return
    }
    Log.d(TAG, "Stopping listening (user action)...")
    // Вызов stopListening также на Main потоке
    managerScope.launch(mainDispatcher) {
      try {
        // Не меняем isListening и _aiState здесь, это сделает listener в
        // onEndOfSpeech/onError/onResults
        speechRecognizer?.stopListening()
        Log.d(TAG, "Called stopListening on SpeechRecognizer")
      } catch (e: Exception) {
        Log.e(TAG, "Error stopping listening", e)
        // Возможно, стоит обработать ошибку, но обычно listener все равно сработает
        handleRecognitionError("Ошибка остановки: ${e.message}")
      }
    }
  }

  /** Отправляет текстовое сообщение на обработку */
  fun sendTextMessage(text: String) {
    if (text.isBlank()) return
    // Проверяем состояние перед отправкой
    if (_aiState.value == AiVisualizerState.LISTENING ||
        _aiState.value == AiVisualizerState.THINKING) {
      Log.w(TAG, "Cannot send text message while listening or thinking.")
      return
    }

    Log.d(TAG, "Sending text message: '$text'")
    // Сразу переводим в состояние THINKING
    _aiState.value = AiVisualizerState.THINKING
    _aiMessage.value = null // Очищаем предыдущее сообщение
    // Запускаем отправку в IO скоупе
    managerScope.launch { processText(text) }
  }

  /** Сбрасывает состояние AI в IDLE (например, после показа ASKING/RESULT) */
  fun resetAiState() {
    if (_aiState.value == AiVisualizerState.ASKING || _aiState.value == AiVisualizerState.RESULT) {
      Log.d(TAG, "Resetting AI state from ${_aiState.value} to IDLE")
      _aiState.value = AiVisualizerState.IDLE
      _aiMessage.value = null
    }
  }

  /** Вызывать в onCleared ViewModel */
  @OptIn(DelicateCoroutinesApi::class)
  fun destroy() {
    Log.d(TAG, "Destroying AiInteractionManager...")
    managerScope.cancel() // Отменяем все корутины скоупа
    // Уничтожаем SpeechRecognizer на главном потоке
    // Используем GlobalScope + MainDispatcher, т.к. managerScope уже может быть отменен
    GlobalScope.launch(mainDispatcher) {
      speechRecognizer?.destroy()
      speechRecognizer = null
      isRecognizerInitialized = false
      Log.d(TAG, "SpeechRecognizer destroyed.")
    }
  }

  // --- Приватные методы ---

  /** Инициализация SpeechRecognizer, выполняется на Main потоке */
  @OptIn(DelicateCoroutinesApi::class)
  private fun initializeSpeechRecognizer() {
    // Запускаем в GlobalScope, т.к. init может вызываться до того, как ViewModelScope будет готов
    GlobalScope.launch(mainDispatcher) {
      if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        Log.e(TAG, "Speech recognition not available.")
        isRecognizerInitialized = false
        return@launch
      }
      try {
        if (speechRecognizer == null) { // Создаем только если еще не создан
          speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
          speechRecognizer?.setRecognitionListener(recognitionListener)
          isRecognizerInitialized = true
          Log.d(TAG, "SpeechRecognizer initialized successfully.")
        } else {
          Log.d(TAG, "SpeechRecognizer already initialized.")
          isRecognizerInitialized = true // Убедимся, что флаг установлен
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize SpeechRecognizer", e)
        speechRecognizer = null // Сбрасываем, если ошибка
        isRecognizerInitialized = false
      }
    }
  }

  /** Обрабатывает распознанный текст (вызывается из listener) */
  private fun processRecognizedText(text: String) {
    if (text.isBlank()) {
      Log.w(TAG, "processRecognizedText called with blank text.")
      _aiState.value = AiVisualizerState.IDLE // Просто сброс, если текст пуст
      return
    }
    Log.i(TAG, "Processing recognized text: '$text'")
    // Состояние уже должно быть THINKING (установлено в onEndOfSpeech или onResults)
    // Если нет, устанавливаем на всякий случай
    if (_aiState.value != AiVisualizerState.THINKING) {
      _aiState.value = AiVisualizerState.THINKING
      _aiMessage.value = null
    }
    managerScope.launch {
      processText(text) // Вызываем общую функцию обработки текста
    }
  }

  /** Общая функция для отправки текста на бэкенд и обработки ответа */
  private suspend fun processText(text: String) {
    // Получаем токен
    val freshToken = authManager.getBackendAuthToken()
    if (freshToken == null) {
      Log.e(TAG, "processText failed: Could not get fresh token.")
      handleBackendError("Ошибка аутентификации") // Обрабатываем как ошибку
      return
    }

    // Получаем настройки
    val currentTemper = settingsRepository.botTemperFlow.first() // Получаем последнее значение
    val timeZoneId = settingsRepository.timeZoneFlow.first().ifEmpty { ZoneId.systemDefault().id }

    Log.i(TAG, "Sending text to /process. Temper: $currentTemper, TimeZone: $timeZoneId")

    val requestBody =
        try {
          MultipartBody.Builder()
              .setType(MultipartBody.FORM)
              .addFormDataPart("text", text)
              .addFormDataPart("time", LocalDateTime.now().toString()) // Текущее время устройства
              .addFormDataPart("timeZone", timeZoneId) // Таймзона из настроек
              .addFormDataPart("temper", currentTemper) // Темперамент из настроек
              .build()
        } catch (e: Exception) {
          Log.e(TAG, "Failed to build request body", e)
          handleBackendError("Ошибка подготовки запроса")
          return
        }

    val request =
        Request.Builder()
            .url("$backendBaseUrl/process")
            .header("Authorization", "Bearer $freshToken")
            .post(requestBody)
            .build()

    // Выполняем запрос
    executeProcessRequest(request)
  }

  /** Выполняет запрос к /process и обрабатывает базовые ответы/ошибки */
  private suspend fun executeProcessRequest(request: Request) =
      withContext(ioDispatcher) {
        try {
          val response = okHttpClient.newCall(request).execute()
          val responseBodyString = response.body?.string() // Читаем тело ОДИН раз

          if (!response.isSuccessful) {
            Log.e(TAG, "Server error processing request: ${response.code} - $responseBodyString")
            var errorDetail = "Server error: (${response.code})"
            try {
              if (!responseBodyString.isNullOrBlank()) {
                val jsonError = JSONObject(responseBodyString)
                errorDetail = jsonError.optString("detail", errorDetail)
              }
            } catch (e: JSONException) {
              Log.w(TAG, "Could not parse error response body: $e")
            }
            // Обрабатываем ошибку бэкенда
            handleBackendError(errorDetail)
          } else {
            Log.i(TAG, "Request processed successfully. Response: $responseBodyString")
            // Обрабатываем успешный ответ (парсим JSON)
            handleProcessResponse(responseBodyString)
          }
        } catch (e: IOException) {
          Log.e(TAG, "Network error during /process request", e)
          handleBackendError("Network error: ${e.message}")
        } catch (e: Exception) {
          Log.e(TAG, "Error executing /process request", e)
          handleBackendError("Request processing error: ${e.message}")
        }
      }

  /** Обрабатывает успешный JSON-ответ от /process */
  private fun handleProcessResponse(responseBody: String?) {
    // Логика парсинга JSON и обновления _aiState и _aiMessage
    // Взята из ViewModel, но обновляет состояния менеджера
    var finalAiState: AiVisualizerState
    var messageForVisualizer: String? = null
    // var errorForViewModel: String? = null // Если нужно передавать ошибку отдельно

    if (responseBody.isNullOrBlank()) {
      Log.w(TAG, "Empty success response from /process")
      // errorForViewModel = "Пустой ответ от сервера."
      finalAiState = AiVisualizerState.IDLE // Или ERROR? Решаем как обрабатывать.
    } else {
      try {
        val json = JSONObject(responseBody)
        val status = json.optString("status")
        val message = json.optString("message")

        when (status) {
          "success" -> {
            messageForVisualizer = "Success!" // Или что-то другое?
            finalAiState = AiVisualizerState.RESULT
            Log.i(TAG, "Backend status: success. Message: $message")
            // Тут можно инициировать обновление календаря, но как?
            // Вариант 1: ViewModel наблюдает за aiState == RESULT и вызывает refresh
            // Вариант 2: Менеджер имеет SharedFlow для событий типа "CalendarNeedsRefresh"
            // Вариант 3: Менеджер напрямую вызывает метод CalendarDataManager (не очень хорошо
            // из-за связности)
            // Пока оставляем на откуп ViewModel (Вариант 1)
          }
          "clarification_needed" -> {
            messageForVisualizer = message
            finalAiState = AiVisualizerState.ASKING
            Log.i(TAG, "Backend status: clarification_needed. Message: $message")
          }
          "info",
          "unsupported" -> {
            messageForVisualizer = message
            finalAiState = AiVisualizerState.RESULT // Показываем как результат
            Log.i(TAG, "Backend status: $status. Message: $message")
          }
          "error" -> {
            Log.e(TAG, "Backend processing error: $message")
            // errorForViewModel = message
            finalAiState = AiVisualizerState.IDLE // Сбрасываем в IDLE при ошибке
          }
          else -> {
            Log.w(TAG, "Unknown status from backend: $status")
            // errorForViewModel = "Неизвестный статус ответа: '$status'"
            finalAiState = AiVisualizerState.IDLE
          }
        }
      } catch (e: JSONException) {
        Log.e(TAG, "Error parsing /process response", e)
        // errorForViewModel = "Ошибка парсинга ответа: ${e.message}"
        finalAiState = AiVisualizerState.IDLE
      } catch (e: Exception) { // Ловим другие ошибки парсинга/обработки
        Log.e(TAG, "Error handling /process response content", e)
        // errorForViewModel = "Ошибка обработки ответа: ${e.message}"
        finalAiState = AiVisualizerState.IDLE
      }
    }

    // Обновляем состояния менеджера
    _aiMessage.value = messageForVisualizer
    _aiState.value = finalAiState

    // Если нужно передать ошибку отдельно в ViewModel:
    // if (errorForViewModel != null) {
    //     managerScope.launch { _errorEvent.emit(errorForViewModel) }
    // }

    // Сбрасываем сообщение, если перешли в IDLE
    if (_aiState.value == AiVisualizerState.IDLE) {
      _aiMessage.value = null
    }
  }

  /** Обрабатывает ошибку распознавания речи */
  private fun handleRecognitionError(errorMessage: String) {
    Log.e(TAG, "Recognition error: $errorMessage")
    isListening = false // Сбрасываем флаг прослушивания
    _aiState.value = AiVisualizerState.IDLE // Сбрасываем состояние AI
    _aiMessage.value = null // Очищаем сообщение
    // Можно передать ошибку в ViewModel через _errorEvent
    // managerScope.launch { _errorEvent.emit(errorMessage) }
  }

  /** Обрабатывает ошибку сети или сервера при запросе к /process */
  private fun handleBackendError(errorMessage: String) {
    Log.e(TAG, "Backend processing error: $errorMessage")
    _aiState.value = AiVisualizerState.IDLE // Сбрасываем состояние AI
    _aiMessage.value = null // Очищаем сообщение
    // Можно передать ошибку в ViewModel через _errorEvent
    // managerScope.launch { _errorEvent.emit(errorMessage) }
  }

  // --- Recognition Listener ---
  private val recognitionListener =
      object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
          Log.d(TAG, "Listener: onReadyForSpeech")
          // UI обновится через _aiState = LISTENING
        }

        override fun onBeginningOfSpeech() {
          Log.d(TAG, "Listener: onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
          /* Можно игнорировать */
        }

        override fun onBufferReceived(buffer: ByteArray?) {
          /* Можно игнорировать */
        }

        override fun onEndOfSpeech() {
          Log.d(TAG, "Listener: onEndOfSpeech")
          // Пользователь закончил говорить, переходим в режим обработки
          if (isListening) { // Проверяем, что мы действительно слушали
            isListening = false // Сбрасываем флаг
            _aiState.value = AiVisualizerState.THINKING // Меняем состояние
            _aiMessage.value = null
            Log.d(TAG, "Listener: Switched state to THINKING")
          }
        }

        override fun onError(error: Int) {
          if (!isListening && error == SpeechRecognizer.ERROR_NO_MATCH) {
            // Игнорируем NO_MATCH, если мы уже не слушаем (например, после onEndOfSpeech)
            Log.d(TAG, "Listener: Ignored ERROR_NO_MATCH after onEndOfSpeech")
            return
          }
          val errorMessage = getSpeechRecognizerErrorText(error)
          Log.w(TAG, "Listener: onError: $errorMessage (code: $error)")
          // Обрабатываем ошибку через общую функцию
          handleRecognitionError(errorMessage)
        }

        override fun onResults(results: Bundle?) {
          Log.d(TAG, "Listener: onResults")
          if (isListening) { // Обрабатываем только если еще слушали
            isListening = false // Сбрасываем флаг
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
              val recognizedText = matches[0]
              // Передаем текст на обработку
              processRecognizedText(recognizedText) // Это установит state в THINKING
            } else {
              Log.w(TAG, "Listener: No recognition results in bundle.")
              handleRecognitionError("Ничего не распознано") // Обрабатываем как ошибку
            }
          } else {
            Log.d(TAG, "Listener: Ignoring onResults because listening was already stopped.")
          }
        }

        override fun onPartialResults(partialResults: Bundle?) {
          val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          if (!matches.isNullOrEmpty()) {
            val partialText = matches[0]
            // Log.d(TAG, "Listener: Partial result: '$partialText'")
            // НЕ обновляем _aiMessage здесь, чтобы не мешать сообщениям Asking/Result
            // Можно передавать partial results через отдельный Flow, если нужно для UI
          }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
          /* Можно игнорировать */
        }
      }

  // Вспомогательная функция для текста ошибок SpeechRecognizer
  private fun getSpeechRecognizerErrorText(error: Int): String {
    return when (error) {
      SpeechRecognizer.ERROR_AUDIO -> context.getString(R.string.speech_recognizer_error_audio)
      SpeechRecognizer.ERROR_CLIENT -> context.getString(R.string.speech_recognizer_error_client)
      SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
          context.getString(R.string.speech_recognizer_error_insufficient_permissions)
      SpeechRecognizer.ERROR_NETWORK -> context.getString(R.string.speech_recognizer_error_network)
      SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
          context.getString(R.string.speech_recognizer_error_network_timeout)
      SpeechRecognizer.ERROR_NO_MATCH ->
          context.getString(R.string.speech_recognizer_error_no_match)
      SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
          context.getString(R.string.speech_recognizer_error_recognizer_busy)
      SpeechRecognizer.ERROR_SERVER -> context.getString(R.string.speech_recognizer_error_server)
      SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
          context.getString(R.string.speech_recognizer_error_speech_timeout)
      else -> context.getString(R.string.speech_recognizer_error_unknown, error)
    }
  }
}
