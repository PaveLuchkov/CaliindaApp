package com.lpavs.caliinda.feature.agent.presentation.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.di.ICalendarStateHolder
import com.lpavs.caliinda.core.data.remote.agent.ChatMessage
import com.lpavs.caliinda.core.data.remote.agent.PreviewAction
import com.lpavs.caliinda.core.data.repository.CalendarRepository
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
  private val calendarRepository: CalendarRepository,
  private val speechRecognitionService: SpeechRecognitionService,
  private val calendarStateHolder: ICalendarStateHolder,
) : ViewModel() {

  private val _agentState = MutableStateFlow(AgentState.IDLE)
  val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

  private val _agentMessage = MutableStateFlow<ChatMessage?>(null)
  val agentMessage: StateFlow<ChatMessage?> = _agentMessage.asStateFlow()

  private val _highlightedEventInfo = MutableStateFlow<Map<String, PreviewAction>>(emptyMap())
  val highlightedEventInfo: StateFlow<Map<String, PreviewAction>> = _highlightedEventInfo.asStateFlow()

  private val _recordingState = MutableStateFlow(RecordingState())
  val recState: StateFlow<RecordingState> = _recordingState.asStateFlow()

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
            _recordingState.update {
              it.copy(
                isListening = false,
                isLoading = false
              )
            }
          }
          is SpeechRecognitionState.Listening -> {
            _agentState.value = AgentState.LISTENING
            _recordingState.update {
              it.copy(
                isListening = true,
                isLoading = false
              )
            }
          }
          is SpeechRecognitionState.Success -> {
            // Сразу сбрасываем состояние прослушивания
            _recordingState.update {
              it.copy(
                isListening = false,
                isLoading = true
              )
            }
            processTextMessage(state.text)
          }
          is SpeechRecognitionState.Error -> {
            _agentState.value = AgentState.ERROR
            _agentMessage.value = ChatMessage(text = "Recording error", author = "System")
            _recordingState.update {
              it.copy(
                isListening = false,
                isLoading = false
              )
            }
          }
        }
      }.launchIn(viewModelScope)
  }

  private fun processTextMessage(text: String) {
    if (text.isBlank()) {
      _agentState.value = AgentState.IDLE
      _recordingState.update {
        it.copy(
          isListening = false,
          isLoading = false
        )
      }
      return
    }

    viewModelScope.launch {
      _agentState.value = AgentState.THINKING
      _recordingState.update {
        it.copy(
          isListening = false,
          isLoading = true
        )
      }

      agentRepository.sendMessage(text)
        .onSuccess { agentMessage ->
          _agentMessage.value = null
          _highlightedEventInfo.value = emptyMap()
          _agentMessage.value = agentMessage
          val infoMap = buildMap {
            agentMessage.previews.forEach { preview ->
              preview.eventIds.forEach { id ->
                put(id, preview.action)
              }
            }
          }
          _highlightedEventInfo.value = infoMap
          _agentState.value = AgentState.RESULT
          if (agentMessage.author == "Waiter_Action") {
            calendarRepository.refreshDate(calendarStateHolder.currentVisibleDate.value)
          }
        }
        .onFailure { error ->
          Log.e(TAG, "Failed to send message", error)
          _agentMessage.value = ChatMessage(text = (error.message ?: (R.string.error)).toString(), author = "System")
          _agentState.value = AgentState.ERROR
        }

      _recordingState.update {
        it.copy(
          isListening = false,
          isLoading = false
        )
      }
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

    // Также сбрасываем состояние записи
    _recordingState.update {
      it.copy(
        isListening = false,
        isLoading = false
      )
    }
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