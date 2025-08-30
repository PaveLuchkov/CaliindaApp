package com.lpavs.caliinda.feature.agent.presentation.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.R
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class AgentViewModel
@Inject
constructor(
  private val agentRepository: AgentRepository,
    private val speechRecognitionService: SpeechRecognitionService,
) : ViewModel() {
  private val _agentState = MutableStateFlow(AgentState.IDLE)
  val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

  private val _agentMessage = MutableStateFlow<String?>(null)
  val agentMessage: StateFlow<String?> = _agentMessage.asStateFlow()

  private val _recordingState = MutableStateFlow(RecordingState())
  val recState: StateFlow<RecordingState> = _recordingState.asStateFlow()

  init {
    observeSpeechRecognition()
  }

  val mock_suggestions = listOf("Delete", "Approve", "Create a meeting at 10 am")

  private fun observeSpeechRecognition() {
    speechRecognitionService.state
      .onEach { state ->
        Log.d(TAG, "Speech Recognition State changed: $state")
        when (state) {
          is SpeechRecognitionState.Idle -> {
            if (_agentState.value == AgentState.LISTENING) {
              _agentState.value = AgentState.IDLE
            }
          }
          is SpeechRecognitionState.Listening -> {
            _agentState.value = AgentState.LISTENING
          }
          is SpeechRecognitionState.Success -> {
            processTextMessage(state.text)
          }
          is SpeechRecognitionState.Error -> {
            _agentState.value = AgentState.ERROR
            _agentMessage.value = state.message
          }
        }
        _recordingState.update { it.copy(isListening = state is SpeechRecognitionState.Listening) }
      }.launchIn(viewModelScope)
  }

  private fun processTextMessage(text: String) {
    if (text.isBlank()) {
      _agentState.value = AgentState.IDLE
      return
    }

    viewModelScope.launch {
      _agentState.value = AgentState.THINKING
      _recordingState.update { it.copy(isLoading = true) }

      agentRepository.sendMessage(text)
        .onSuccess {
          _agentState.value = AgentState.RESULT
        }
        .onFailure { error ->
          Log.e(TAG, "Failed to send message", error)
          _agentState.value = AgentState.ERROR
          _agentMessage.value = error.message ?: "Неизвестная ошибка"
        }

      _recordingState.update { it.copy(isLoading = false) }
    }
  }

  private val _eventFlow = MutableSharedFlow<AgentUiEvent>()
  val eventFlow: SharedFlow<AgentUiEvent> = _eventFlow.asSharedFlow()

  // --- ДЕЙСТВИЯ AI ---
  fun startListening() {
    if (!_recordingState.value.isPermissionGranted) {
      viewModelScope.launch {
        _eventFlow.emit(AgentUiEvent.ShowMessage(UiText.from(R.string.voice_no_permission)))
      }
      return
    }
    speechRecognitionService.startListening()
  }

  fun stopListening() = speechRecognitionService.stopListening()

  fun sendTextMessage(text: String) {
    processTextMessage(text)
  }

  // --- ОБРАБОТКА UI СОБЫТИЙ / РАЗРЕШЕНИЙ ---
  fun updatePermissionStatus(isGranted: Boolean) {
    _recordingState.update { it.copy(isPermissionGranted = isGranted) }
  }
  fun resetAiState() {
    val currentState = agentState.value
    if (currentState == AgentState.RESULT || currentState == AgentState.ERROR || currentState == AgentState.ASKING) {
      _agentState.value = AgentState.IDLE
      _agentMessage.value = null
    }
  }

  override fun onCleared() {
    super.onCleared()
    speechRecognitionService.destroy()
  }

  companion object {
    private const val TAG = "AgentViewModel" // Используем один TAG
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
