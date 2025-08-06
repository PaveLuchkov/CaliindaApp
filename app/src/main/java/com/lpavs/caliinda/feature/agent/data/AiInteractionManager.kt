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
    private val authManager: AuthManager,
    private val settingsRepository: SettingsRepository,
    @BackendUrl private val backendBaseUrl: String,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
) {
  private val TAG = "AiInteractionManager"
  private val managerScope = CoroutineScope(SupervisorJob() + ioDispatcher)

  private val _aiState = MutableStateFlow(AiVisualizerState.IDLE)
  val aiState: StateFlow<AiVisualizerState> = _aiState.asStateFlow()

  private val _aiMessage = MutableStateFlow<String?>(null)
  val aiMessage: StateFlow<String?> = _aiMessage.asStateFlow()

  private var speechRecognizer: SpeechRecognizer? = null
  private val speechRecognizerIntent: Intent
  private var isRecognizerInitialized = false
  private var isListening = false

  init {
    Log.d(TAG, "Initializing AiInteractionManager...")
    speechRecognizerIntent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(
              RecognizerIntent.EXTRA_LANGUAGE,
              Locale.getDefault().toString())
          putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...")
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    initializeSpeechRecognizer()
  }

  fun startListening() {
    if (isListening) {
      Log.w(TAG, "Already listening.")
      return
    }
    if (!isRecognizerInitialized) {
      Log.w(TAG, "Recognizer not ready, attempting to initialize...")
      initializeSpeechRecognizer()
      if (!isRecognizerInitialized) {
        Log.e(TAG, "Cannot start listening: SpeechRecognizer failed to initialize.")
        _aiState.value = AiVisualizerState.IDLE
        return
      }
    }

    Log.d(TAG, "Attempting to start listening...")
    managerScope.launch(mainDispatcher) {
      try {
        isListening = true
        _aiState.value = AiVisualizerState.LISTENING
        _aiMessage.value = null
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
      return
    }
    Log.d(TAG, "Stopping listening (user action)...")
    managerScope.launch(mainDispatcher) {
      try {
        speechRecognizer?.stopListening()
        Log.d(TAG, "Called stopListening on SpeechRecognizer")
      } catch (e: Exception) {
        Log.e(TAG, "Error stopping listening", e)
        handleRecognitionError("Ошибка остановки: ${e.message}")
      }
    }
  }

  fun sendTextMessage(text: String) {
    if (text.isBlank()) return
    if (_aiState.value == AiVisualizerState.LISTENING ||
        _aiState.value == AiVisualizerState.THINKING) {
      Log.w(TAG, "Cannot send text message while listening or thinking.")
      return
    }

    Log.d(TAG, "Sending text message: '$text'")
    _aiState.value = AiVisualizerState.THINKING
    _aiMessage.value = null
    managerScope.launch { processText(text) }
  }

  fun resetAiState() {
    if (_aiState.value == AiVisualizerState.ASKING || _aiState.value == AiVisualizerState.RESULT) {
      Log.d(TAG, "Resetting AI state from ${_aiState.value} to IDLE")
      _aiState.value = AiVisualizerState.IDLE
      _aiMessage.value = null
    }
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun destroy() {
    Log.d(TAG, "Destroying AiInteractionManager...")
    managerScope.cancel()
    GlobalScope.launch(mainDispatcher) {
      speechRecognizer?.destroy()
      speechRecognizer = null
      isRecognizerInitialized = false
      Log.d(TAG, "SpeechRecognizer destroyed.")
    }
  }

  @OptIn(DelicateCoroutinesApi::class)
  private fun initializeSpeechRecognizer() {
    GlobalScope.launch(mainDispatcher) {
      if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        Log.e(TAG, "Speech recognition not available.")
        isRecognizerInitialized = false
        return@launch
      }
      try {
        if (speechRecognizer == null) {
          speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
          speechRecognizer?.setRecognitionListener(recognitionListener)
          isRecognizerInitialized = true
          Log.d(TAG, "SpeechRecognizer initialized successfully.")
        } else {
          Log.d(TAG, "SpeechRecognizer already initialized.")
          isRecognizerInitialized = true
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize SpeechRecognizer", e)
        speechRecognizer = null
        isRecognizerInitialized = false
      }
    }
  }

  private fun processRecognizedText(text: String) {
    if (text.isBlank()) {
      Log.w(TAG, "processRecognizedText called with blank text.")
      _aiState.value = AiVisualizerState.IDLE
      return
    }
    Log.i(TAG, "Processing recognized text: '$text'")
    if (_aiState.value != AiVisualizerState.THINKING) {
      _aiState.value = AiVisualizerState.THINKING
      _aiMessage.value = null
    }
    managerScope.launch {
      processText(text)
    }
  }

  private suspend fun processText(text: String) {
    val freshToken = authManager.getBackendAuthToken()
    if (freshToken == null) {
      Log.e(TAG, "processText failed: Could not get fresh token.")
      handleBackendError("Ошибка аутентификации")
      return
    }

    val currentTemper = settingsRepository.botTemperFlow.first()
    val timeZoneId = settingsRepository.timeZoneFlow.first().ifEmpty { ZoneId.systemDefault().id }

    Log.i(TAG, "Sending text to /process. Temper: $currentTemper, TimeZone: $timeZoneId")

    val requestBody =
        try {
          MultipartBody.Builder()
              .setType(MultipartBody.FORM)
              .addFormDataPart("text", text)
              .addFormDataPart("time", LocalDateTime.now().toString())
            .addFormDataPart("timeZone", timeZoneId)
            .addFormDataPart("temper", currentTemper)
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

    executeProcessRequest(request)
  }

  private suspend fun executeProcessRequest(request: Request) =
      withContext(ioDispatcher) {
        try {
          val response = okHttpClient.newCall(request).execute()
          val responseBodyString = response.body?.string()

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
            handleBackendError(errorDetail)
          } else {
            Log.i(TAG, "Request processed successfully. Response: $responseBodyString")
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

  private fun handleProcessResponse(responseBody: String?) {
    var finalAiState: AiVisualizerState
    var messageForVisualizer: String? = null

    if (responseBody.isNullOrBlank()) {
      Log.w(TAG, "Empty success response from /process")
      finalAiState = AiVisualizerState.IDLE
    } else {
      try {
        val json = JSONObject(responseBody)
        val status = json.optString("status")
        val message = json.optString("message")

        when (status) {
          "success" -> {
            messageForVisualizer = "Success!"
            finalAiState = AiVisualizerState.RESULT
            Log.i(TAG, "Backend status: success. Message: $message")
          }
          "clarification_needed" -> {
            messageForVisualizer = message
            finalAiState = AiVisualizerState.ASKING
            Log.i(TAG, "Backend status: clarification_needed. Message: $message")
          }
          "info",
          "unsupported" -> {
            messageForVisualizer = message
            finalAiState = AiVisualizerState.RESULT
            Log.i(TAG, "Backend status: $status. Message: $message")
          }
          "error" -> {
            Log.e(TAG, "Backend processing error: $message")
            finalAiState = AiVisualizerState.IDLE
          }
          else -> {
            Log.w(TAG, "Unknown status from backend: $status")
            finalAiState = AiVisualizerState.IDLE
          }
        }
      } catch (e: JSONException) {
        Log.e(TAG, "Error parsing /process response", e)
        finalAiState = AiVisualizerState.IDLE
      } catch (e: Exception) {
        Log.e(TAG, "Error handling /process response content", e)
        finalAiState = AiVisualizerState.IDLE
      }
    }

    _aiMessage.value = messageForVisualizer
    _aiState.value = finalAiState

    if (_aiState.value == AiVisualizerState.IDLE) {
      _aiMessage.value = null
    }
  }

  private fun handleRecognitionError(errorMessage: String) {
    Log.e(TAG, "Recognition error: $errorMessage")
    isListening = false
    _aiState.value = AiVisualizerState.IDLE
    _aiMessage.value = null
  }

  private fun handleBackendError(errorMessage: String) {
    Log.e(TAG, "Backend processing error: $errorMessage")
    _aiState.value = AiVisualizerState.IDLE
    _aiMessage.value = null
  }

  private val recognitionListener =
      object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
          Log.d(TAG, "Listener: onReadyForSpeech")
        }

        override fun onBeginningOfSpeech() {
          Log.d(TAG, "Listener: onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
        }

        override fun onBufferReceived(buffer: ByteArray?) {
        }

        override fun onEndOfSpeech() {
          Log.d(TAG, "Listener: onEndOfSpeech")
          if (isListening) {
            isListening = false
            _aiState.value = AiVisualizerState.THINKING
            _aiMessage.value = null
            Log.d(TAG, "Listener: Switched state to THINKING")
          }
        }

        override fun onError(error: Int) {
          if (!isListening && error == SpeechRecognizer.ERROR_NO_MATCH) {
            Log.d(TAG, "Listener: Ignored ERROR_NO_MATCH after onEndOfSpeech")
            return
          }
          val errorMessage = getSpeechRecognizerErrorText(error)
          Log.w(TAG, "Listener: onError: $errorMessage (code: $error)")
          handleRecognitionError(errorMessage)
        }

        override fun onResults(results: Bundle?) {
          Log.d(TAG, "Listener: onResults")
          if (isListening) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
              val recognizedText = matches[0]
              processRecognizedText(recognizedText)
            } else {
              Log.w(TAG, "Listener: No recognition results in bundle.")
              handleRecognitionError("Ничего не распознано")
            }
          } else {
            Log.d(TAG, "Listener: Ignoring onResults because listening was already stopped.")
          }
        }

        override fun onPartialResults(partialResults: Bundle?) {
          val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          if (!matches.isNullOrEmpty()) {
            val partialText = matches[0]
          }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
        }
      }

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
