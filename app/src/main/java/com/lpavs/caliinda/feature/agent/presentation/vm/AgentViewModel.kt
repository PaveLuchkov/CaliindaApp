package com.lpavs.caliinda.feature.agent.presentation.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.remote.agent.domain.AgentResponseContent
import com.lpavs.caliinda.core.data.remote.agent.domain.ErrorResponse
import com.lpavs.caliinda.core.data.utils.UiText
import com.lpavs.caliinda.feature.agent.data.AgentRepository
import com.lpavs.caliinda.feature.agent.data.SpeechRecognitionService
import com.lpavs.caliinda.feature.agent.data.SpeechRecognitionState
import com.lpavs.caliinda.feature.agent.data.model.AgentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgentViewModel
@Inject
constructor(
    private val agentRepository: AgentRepository,
    private val speechRecognitionService: SpeechRecognitionService,
) : ViewModel() {

  private val _agentState = MutableStateFlow(AgentState.IDLE)
  val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

  private val _agentResponse = MutableStateFlow<AgentResponseContent?>(null)
  val agentResponse: StateFlow<AgentResponseContent?> = _agentResponse.asStateFlow()

  private val _recordingState = MutableStateFlow(RecordingState())
  val recState: StateFlow<RecordingState> = _recordingState.asStateFlow()

  private val _eventFlow = MutableSharedFlow<AgentUiEvent>()
  val eventFlow: SharedFlow<AgentUiEvent> = _eventFlow.asSharedFlow()

  init {
    observeSpeechRecognition()
  }

  private fun observeSpeechRecognition() {
    speechRecognitionService.state
        .onEach { state ->
          Log.d(TAG, "Speech Recognition State changed: $state")
          when (state) {
            is SpeechRecognitionState.Idle -> {
              _agentState.value = AgentState.IDLE
              _recordingState.update { it.copy(isListening = false, isLoading = false) }
            }
            is SpeechRecognitionState.Listening -> {
              _agentState.value = AgentState.LISTENING
              _recordingState.update { it.copy(isListening = true, isLoading = false) }
            }
            is SpeechRecognitionState.Success -> {
              // Сразу сбрасываем состояние прослушивания
              _recordingState.update { it.copy(isListening = false, isLoading = true) }
              processTextMessage(state.text)
            }
            is SpeechRecognitionState.Error -> {
              _agentState.value = AgentState.ERROR
              _agentResponse.value =
                  ErrorResponse(mainText = "Ошибка распознавания речи: ${state.message}")
              _recordingState.update { it.copy(isListening = false, isLoading = false) }
            }
          }
        }
        .launchIn(viewModelScope)
  }

  private fun processTextMessage(text: String) {
    if (text.isBlank()) {
      _agentState.value = AgentState.IDLE
      _recordingState.update { it.copy(isLoading = false) }
      return
    }

    _agentResponse.value = null

    viewModelScope.launch {
      _agentState.value = AgentState.THINKING
      _recordingState.update { it.copy(isLoading = true) }

      agentRepository
          .sendMessage(text)
          .onSuccess { responseContent ->
            _agentResponse.value = responseContent

            _agentState.value = AgentState.RESULT
          }
          .onFailure { error ->
            Log.e(TAG, "Failed to send message", error)
            _agentResponse.value =
                ErrorResponse(mainText = error.message ?: "Произошла неизвестная ошибка")
            _agentState.value = AgentState.ERROR
          }

      _recordingState.update { it.copy(isLoading = false) }
    }
  }

  // --- ДЕЙСТВИЯ AI ---
  fun startListening() {
    if (!_recordingState.value.isPermissionGranted) {
      viewModelScope.launch {
        _eventFlow.emit(AgentUiEvent.ShowMessage(UiText.from(R.string.voice_no_permission)))
      }
      return
    }

    Log.d(TAG, "Starting speech recognition")
    speechRecognitionService.startListening()
  }

  fun stopListening() {
    Log.d(TAG, "Stopping speech recognition")
    speechRecognitionService.stopListening()
  }

  fun sendTextMessage(text: String) {
    processTextMessage(text)
  }

  fun deleteSession() {
    viewModelScope.launch {
      agentRepository
          .deleteSession()
          .onSuccess { _agentResponse.value = null }
          .onFailure { error -> Log.e(TAG, "Failed to delete session", error) }
    }
  }

  fun updatePermissionStatus(isGranted: Boolean) {
    _recordingState.update { it.copy(isPermissionGranted = isGranted) }
  }

  override fun onCleared() {
    super.onCleared()
    speechRecognitionService.destroy()
  }

  companion object {
    private const val TAG = "AgentViewModel"
  }
}

data class RecordingState(
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val message: String? = "Требуется вход.",
)

sealed class AgentUiEvent {
  data class ShowMessage(val message: UiText) : AgentUiEvent()
}
