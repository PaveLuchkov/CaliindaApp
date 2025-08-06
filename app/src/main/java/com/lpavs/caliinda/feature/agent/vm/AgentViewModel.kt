package com.lpavs.caliinda.feature.agent.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.auth.AuthManager
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.utils.UiText
import com.lpavs.caliinda.feature.agent.data.AiInteractionManager
import com.lpavs.caliinda.feature.agent.data.model.AiVisualizerState
import com.lpavs.caliinda.feature.calendar.ui.CalendarViewModel
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
class AgentViewModel @Inject constructor(
    private val aiInteractionManager: AiInteractionManager,
    private val authManager: AuthManager,
    private val calendarViewModel: CalendarViewModel,
): ViewModel() {
    val aiState: StateFlow<AiVisualizerState> = aiInteractionManager.aiState
    val aiMessage: StateFlow<String?> =
        aiInteractionManager.aiMessage

    private val _uiState = MutableStateFlow(AgentState())

    init {
        observeAiState()
    }

    private fun observeAiState() {
        viewModelScope.launch {
            aiInteractionManager.aiState.collect { ai ->
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        isListening = ai == AiVisualizerState.LISTENING,
                        isLoading = ai == AiVisualizerState.THINKING)
                }
                if (ai == AiVisualizerState.RESULT) {
                    Log.d(TAG, "AI observer: Interaction finished with RESULT, triggering calendar refresh.")
                    viewModelScope.launch { calendarViewModel.refreshCurrentVisibleDate() }
                }
            }
        }
    }

    private val _eventFlow = MutableSharedFlow<AgentUiEvent>()
    val eventFlow: SharedFlow<AgentUiEvent> = _eventFlow.asSharedFlow()

    // --- ДЕЙСТВИЯ AI ---
    fun startListening() {
        if (!_uiState.value.isPermissionGranted) {
            viewModelScope.launch {
                _eventFlow.emit(AgentUiEvent.ShowMessage(UiText.from(R.string.voice_no_permission)))
            }
            return
        }
        aiInteractionManager.startListening()
    }

    fun stopListening() = aiInteractionManager.stopListening()

    fun sendTextMessage(text: String) {
        if (!authManager.authState.value.isSignedIn) {
            Log.w(TAG, "Cannot send message: Not signed in.")
            return
        }
        aiInteractionManager.sendTextMessage(text)
    }

    fun resetAiStateAfterResult() = aiInteractionManager.resetAiState()

    fun resetAiStateAfterAsking() = aiInteractionManager.resetAiState()

    // --- ОБРАБОТКА UI СОБЫТИЙ / РАЗРЕШЕНИЙ ---
    fun updatePermissionStatus(isGranted: Boolean) {
        if (_uiState.value.isPermissionGranted != isGranted) {
            _uiState.update { it.copy(isPermissionGranted = isGranted) }
            Log.d(TAG, "Audio permission status updated to: $isGranted")
        }
    }

    override fun onCleared() {
        super.onCleared()
        aiInteractionManager.destroy() // Вызываем очистку менеджера AI
    }

    companion object {
        private const val TAG = "AgentViewModel" // Используем один TAG
    }
}


data class AgentState(
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val message: String? = "Требуется вход.",
)

sealed class AgentUiEvent {
    data class ShowMessage(val message: UiText) : AgentUiEvent()
}