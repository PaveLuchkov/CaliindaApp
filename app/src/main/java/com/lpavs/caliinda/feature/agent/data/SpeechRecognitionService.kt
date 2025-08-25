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
class SpeechRecognitionService @Inject constructor(
    @ApplicationContext private val context: Context,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
) {
    private val TAG = "SpeechRecService"
    private val serviceScope = CoroutineScope(SupervisorJob() + mainDispatcher)

    private var speechRecognizer: SpeechRecognizer? = null

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
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(recognitionListener)
        }
        Log.d(TAG, "SpeechRecognizer initialized.")
    }

    fun startListening() {
        if (_state.value is SpeechRecognitionState.Listening) {
            Log.w(TAG, "Already listening.")
            return
        }

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        serviceScope.launch {
            _state.value = SpeechRecognitionState.Listening
            speechRecognizer?.startListening(recognizerIntent)
            Log.d(TAG, "Started listening.")
        }
    }

    fun stopListening() {
        if (_state.value !is SpeechRecognitionState.Listening) return
        serviceScope.launch {
            speechRecognizer?.stopListening()
            Log.d(TAG, "Stopped listening by user.")
        }
    }

    fun destroy() {
        serviceScope.launch {
            speechRecognizer?.destroy()
            speechRecognizer = null
            Log.d(TAG, "SpeechRecognizer destroyed.")
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech")
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
            // Переход в состояние обработки произойдет после onResults
        }

        override fun onError(error: Int) {
            val errorMessage = getSpeechRecognizerErrorText(error)
            Log.e(TAG, "onError: $errorMessage (code: $error)")
            // Игнорируем NO_MATCH, если это не единственная ошибка
            if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                _state.value = SpeechRecognitionState.Error(errorMessage)
            } else {
                _state.value = SpeechRecognitionState.Idle // Не удалось ничего распознать
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _state.value = SpeechRecognitionState.Success(matches[0])
            } else {
                Log.w(TAG, "No recognition results found.")
                _state.value = SpeechRecognitionState.Idle
            }
        }

        // Остальные методы можно оставить пустыми
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    // Этот метод можно скопировать из твоего AgentManager
    private fun getSpeechRecognizerErrorText(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Ошибка аудио"
            SpeechRecognizer.ERROR_CLIENT -> "Ошибка клиента"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Недостаточно разрешений"
            SpeechRecognizer.ERROR_NETWORK -> "Ошибка сети"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Таймаут сети"
            SpeechRecognizer.ERROR_NO_MATCH -> "Ничего не распознано"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознаватель занят"
            SpeechRecognizer.ERROR_SERVER -> "Ошибка сервера"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Таймаут речи"
            else -> "Неизвестная ошибка: $error"
        }
    }
}