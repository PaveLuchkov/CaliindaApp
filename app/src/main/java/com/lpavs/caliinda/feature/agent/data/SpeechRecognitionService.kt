// file: app/src/main/java/com/lpavs/caliinda/feature/agent/data/SpeechRecognitionService.kt
package com.lpavs.caliinda.feature.agent.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.lpavs.caliinda.app.di.MainDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class SpeechRecognitionState {
  object Idle : SpeechRecognitionState()

  object Listening : SpeechRecognitionState()

  data class Success(val text: String) : SpeechRecognitionState()

  data class Error(val message: String) : SpeechRecognitionState()
}

@Singleton
class SpeechRecognitionService
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
) {
  private val TAG = "SpeechRecService"
  private val serviceScope = CoroutineScope(SupervisorJob() + mainDispatcher)

  private var speechRecognizer: SpeechRecognizer? = null
  private var isCurrentlyListening = false // Добавляем внутренний флаг

  private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
  val state = _state.asStateFlow()

  init {
    initialize()
  }

  private fun initialize() {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      Log.e(TAG, "Speech recognition not available.")
      _state.value = SpeechRecognitionState.Error("Сервис распознавания недоступен")
      return
    }
    createNewRecognizer()
    Log.d(TAG, "SpeechRecognizer initialized.")
  }

  private fun createNewRecognizer() {
    // Уничтожаем старый если есть
    speechRecognizer?.destroy()

    speechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context).apply {
          setRecognitionListener(recognitionListener)
        }
    isCurrentlyListening = false
    Log.d(TAG, "New SpeechRecognizer created")
  }

  fun startListening() {
    Log.d(
        TAG,
        "startListening called, current state: ${_state.value}, isCurrentlyListening: $isCurrentlyListening")

    // Если уже слушаем - игнорируем
    if (isCurrentlyListening || _state.value is SpeechRecognitionState.Listening) {
      Log.w(TAG, "Already listening, ignoring start request")
      return
    }

    // Создаем новый recognizer для надежности
    createNewRecognizer()

    val recognizerIntent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
          putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

    try {
      isCurrentlyListening = true
      _state.value = SpeechRecognitionState.Listening
      speechRecognizer?.startListening(recognizerIntent)
      Log.d(TAG, "Started listening successfully")
    } catch (e: Exception) {
      Log.e(TAG, "Error starting recognition", e)
      isCurrentlyListening = false
      _state.value = SpeechRecognitionState.Error("Ошибка запуска распознавания: ${e.message}")
    }
  }

  fun stopListening() {
    Log.d(TAG, "stopListening called, isCurrentlyListening: $isCurrentlyListening")

    if (!isCurrentlyListening) {
      Log.w(TAG, "Not currently listening, ignoring stop request")
      return
    }

    try {
      speechRecognizer?.stopListening()
      Log.d(TAG, "Stop listening command sent")
    } catch (e: Exception) {
      Log.e(TAG, "Error stopping recognition", e)
      forceReset()
    }
  }

  private fun forceReset() {
    Log.d(TAG, "Force resetting speech recognizer")
    isCurrentlyListening = false
    _state.value = SpeechRecognitionState.Idle
    createNewRecognizer()
  }

  fun destroy() {
    Log.d(TAG, "Destroying SpeechRecognitionService")
    isCurrentlyListening = false
    speechRecognizer?.destroy()
    speechRecognizer = null
  }

  private val recognitionListener =
      object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
          Log.d(TAG, "onReadyForSpeech")
          isCurrentlyListening = true
          _state.value = SpeechRecognitionState.Listening
        }

        override fun onBeginningOfSpeech() {
          Log.d(TAG, "onBeginningOfSpeech")
          isCurrentlyListening = true
          _state.value = SpeechRecognitionState.Listening
        }

        override fun onEndOfSpeech() {
          Log.d(TAG, "onEndOfSpeech")
          // НЕ сбрасываем isCurrentlyListening - дождемся onResults/onError
        }

        override fun onError(error: Int) {
          val errorMessage = getSpeechRecognizerErrorText(error)
          Log.e(TAG, "onError: $errorMessage (code: $error)")

          isCurrentlyListening = false

          when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> {
              _state.value = SpeechRecognitionState.Idle
            }
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
              _state.value = SpeechRecognitionState.Idle
            }
            else -> {
              _state.value = SpeechRecognitionState.Error(errorMessage)
            }
          }

          // Создаем новый recognizer для следующего использования
          serviceScope.launch { createNewRecognizer() }
        }

        override fun onResults(results: Bundle?) {
          Log.d(TAG, "onResults called")
          isCurrentlyListening = false

          val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          if (!matches.isNullOrEmpty()) {
            val recognizedText = matches[0]
            Log.d(TAG, "Recognition successful: '$recognizedText'")
            _state.value = SpeechRecognitionState.Success(recognizedText)
          } else {
            Log.w(TAG, "No recognition results found")
            _state.value = SpeechRecognitionState.Idle
          }

          // Создаем новый recognizer для следующего использования
          serviceScope.launch { createNewRecognizer() }
        }

        override fun onRmsChanged(rmsdB: Float) {
          // Можно использовать для индикации уровня звука
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onPartialResults(partialResults: Bundle?) {
          val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          if (!matches.isNullOrEmpty()) {
            Log.d(TAG, "Partial results: ${matches[0]}")
          }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
          Log.d(TAG, "onEvent: $eventType")
        }
      }

  private fun getSpeechRecognizerErrorText(error: Int): String {
    return when (error) {
      SpeechRecognizer.ERROR_AUDIO -> "Ошибка аудио"
      SpeechRecognizer.ERROR_CLIENT -> "Ошибка клиента"
      SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Недостаточно разрешений"
      SpeechRecognizer.ERROR_NETWORK -> "Ошибка сети"
      SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Таймаут сети"
      SpeechRecognizer.ERROR_NO_MATCH -> "Речь не распознана"
      SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознаватель занят"
      SpeechRecognizer.ERROR_SERVER -> "Ошибка сервера"
      SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Таймаут речи"
      else -> "Неизвестная ошибка: $error"
    }
  }
}
