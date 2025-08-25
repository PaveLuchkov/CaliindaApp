package com.lpavs.caliinda.feature.agent.presentation.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.auth.AuthManager
import com.lpavs.caliinda.core.data.di.ICalendarStateHolder
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.utils.UiText
import com.lpavs.caliinda.feature.agent.data.AgentManager
import com.lpavs.caliinda.feature.agent.data.model.AgentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgentViewModel
@Inject
constructor(
    private val agentManager: AgentManager,
    private val authManager: AuthManager,
    private val calendarRepository: CalendarRepository,
    private val calendarStateHolder: ICalendarStateHolder
) : ViewModel() {
  val aiState: StateFlow<AgentState> = agentManager.aiState

  private val recordingState = MutableStateFlow(RecordingState())
  val recState: StateFlow<RecordingState> = recordingState.asStateFlow()

  init {
    observeAiState()
  }

  val mock_suggestions = listOf("Delete", "Approve", "Create a meeting at 10 am")

  private fun observeAiState() {
    viewModelScope.launch {
      agentManager.aiState.collect { ai ->
        recordingState.update { currentUiState ->
          currentUiState.copy(
              isListening = ai == AgentState.LISTENING, isLoading = ai == AgentState.THINKING)
        }
        if (ai == AgentState.RESULT) {
          Log.d(TAG, "AI observer: Interaction finished with RESULT, triggering calendar refresh.")
          val dateToRefresh = calendarStateHolder.currentVisibleDate.value
          viewModelScope.launch { calendarRepository.refreshDate(dateToRefresh) }
        }
      }
    }
  }

  private val _eventFlow = MutableSharedFlow<AgentUiEvent>()
  val eventFlow: SharedFlow<AgentUiEvent> = _eventFlow.asSharedFlow()

  // --- ДЕЙСТВИЯ AI ---
  fun startListening() {
    if (!recordingState.value.isPermissionGranted) {
      viewModelScope.launch {
        _eventFlow.emit(AgentUiEvent.ShowMessage(UiText.from(R.string.voice_no_permission)))
      }
      return
    }
    agentManager.startListening()
  }

  fun stopListening() = agentManager.stopListening()

  fun sendTextMessage(text: String) {
    if (!authManager.authState.value.isSignedIn) {
      Log.w(TAG, "Cannot send message: Not signed in.")
      return
    }
    agentManager.sendTextMessage(text)
  }

  // --- ОБРАБОТКА UI СОБЫТИЙ / РАЗРЕШЕНИЙ ---
  fun updatePermissionStatus(isGranted: Boolean) {
    if (recordingState.value.isPermissionGranted != isGranted) {
      recordingState.update { it.copy(isPermissionGranted = isGranted) }
      Log.d(TAG, "Audio permission status updated to: $isGranted")
    }
  }

  override fun onCleared() {
    super.onCleared()
    agentManager.destroy() // Вызываем очистку менеджера AI
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
