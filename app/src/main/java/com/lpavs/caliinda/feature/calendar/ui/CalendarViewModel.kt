package com.lpavs.caliinda.feature.calendar.ui

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lpavs.caliinda.core.common.EventNetworkState
import com.lpavs.caliinda.core.data.auth.AuthManager
import com.lpavs.caliinda.core.data.di.ITimeTicker
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.feature.agent.data.AiInteractionManager
import com.lpavs.caliinda.feature.agent.data.model.AiVisualizerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel
@Inject
constructor(
    private val authManager: AuthManager,
    private val calendarRepository: CalendarRepository,
    private val aiInteractionManager: AiInteractionManager,
    timeTicker: ITimeTicker,
) : ViewModel() {

  // --- ОСНОВНОЕ СОСТОЯНИЕ UI ---
  private val _uiState = MutableStateFlow(CalendarState())
  val state: StateFlow<CalendarState> = _uiState.asStateFlow()

  private var initialAuthCheckCompletedAndProcessed = false

  // --- ДЕЛЕГИРОВАННЫЕ И ПРОИЗВОДНЫЕ СОСТОЯНИЯ ДЛЯ UI ---
  val currentTime: StateFlow<Instant> = timeTicker.currentTime

  // Состояния Календаря
  private val _currentVisibleDate = MutableStateFlow(LocalDate.now())
  val currentVisibleDate: StateFlow<LocalDate> = _currentVisibleDate.asStateFlow()
  val rangeNetworkState: StateFlow<EventNetworkState> = calendarRepository.rangeNetworkState

  private val _eventFlow = MutableSharedFlow<CalendarUiEvent>()
  val eventFlow: SharedFlow<CalendarUiEvent> = _eventFlow.asSharedFlow()

  // Состояния AI
  val aiState: StateFlow<AiVisualizerState> = aiInteractionManager.aiState
  val aiMessage: StateFlow<String?> =
      aiInteractionManager.aiMessage // Сообщение от AI (Asking/Result)

  init {
    observeAuthState()
    observeAiState()
    observeCalendarNetworkState()
  }

  private fun observeAuthState() {
    viewModelScope.launch {
      authManager.authState.collect { authState ->
        val previousUiState = _uiState.value
        _uiState.update { currentState ->
          currentState.copy(
              isSignedIn = authState.isSignedIn,
              userEmail = authState.userEmail,
              displayName = authState.displayName,
              photo = authState.photoUrl,
              isLoading = calculateIsLoading(authLoading = authState.isLoading),
              authorizationIntent = authState.authorizationIntent)
        }

        authState.authError?.let { error ->
          _eventFlow.emit(CalendarUiEvent.ShowMessage(error))
          authManager.clearAuthError()
        }
        if (!initialAuthCheckCompletedAndProcessed && !authState.isLoading) {
          initialAuthCheckCompletedAndProcessed = true
          Log.d(TAG, "Initial auth check completed and processed.")
          if (!authState.isSignedIn && authState.authError == null) {
            Log.d(TAG, "Initial auth check: Showing sign-in required dialog.")
            _uiState.update { it.copy(showSignInRequiredDialog = true) }
          }
        }
        if (authState.isSignedIn && _uiState.value.showSignInRequiredDialog) {
          _uiState.update { it.copy(showSignInRequiredDialog = false) }
        }
        if (authState.isSignedIn && !previousUiState.isSignedIn) {
          _uiState.update { it.copy(showSignInRequiredDialog = false) }
        }
        if (authState.isSignedIn && !previousUiState.isSignedIn) {
          Log.d(TAG, "Auth observer: User signed in. Triggering calendar refresh")
          calendarRepository.setCurrentVisibleDate(currentVisibleDate.value, forceRefresh = true)
        }
      }
    }
  }

  private fun observeAiState() {
    viewModelScope.launch {
      aiInteractionManager.aiState.collect { ai ->
        _uiState.update { currentUiState ->
          // Обновляем только состояние isLoading и isListening
          currentUiState.copy(
              isListening = ai == AiVisualizerState.LISTENING,
              isLoading = calculateIsLoading(aiState = ai))
        }
        if (ai == AiVisualizerState.RESULT) {
          Log.d(TAG, "AI observer: Interaction finished with RESULT, triggering calendar refresh.")
          refreshCurrentVisibleDate()
        }
      }
    }
  }

  private fun observeCalendarNetworkState() {
    viewModelScope.launch {
      calendarRepository.rangeNetworkState.collect { network ->
        _uiState.update { it.copy(isLoading = calculateIsLoading(networkState = network)) }

        if (network is EventNetworkState.Error) {
          if (authManager.authState.value.authError == null) {
            _eventFlow.emit(CalendarUiEvent.ShowMessage(network.message))
          }
        }
      }
    }
  }

  // --- ПРИВАТНЫЙ ХЕЛПЕР ДЛЯ РАСЧЕТА ОБЩЕГО isLoading ---
  /** Рассчитывает общее состояние загрузки, комбинируя состояния менеджеров */
  private fun calculateIsLoading(
      authLoading: Boolean =
          authManager.authState.value.isLoading, // Берем текущие значения по умолчанию
      networkState: EventNetworkState = calendarRepository.rangeNetworkState.value,
      aiState: AiVisualizerState = aiInteractionManager.aiState.value,
  ): Boolean {
    val calendarLoading = networkState is EventNetworkState.Loading
    val aiThinking = aiState == AiVisualizerState.THINKING

    return authLoading || calendarLoading || aiThinking
  }

  // --- ДЕЙСТВИЯ АУТЕНТИФИКАЦИИ ---
  fun signIn(activity: Activity) {
    if (_uiState.value.showSignInRequiredDialog) {
      _uiState.update { it.copy(showSignInRequiredDialog = false) }
    }
    authManager.signIn(activity)
  }

  fun handleAuthorizationResult(intent: Intent) {
    authManager.handleAuthorizationResult(intent)
  }

  fun signOut() {
    if (_uiState.value.showSignInRequiredDialog) {
      _uiState.update { it.copy(showSignInRequiredDialog = false) }
    }
    authManager.signOut()
  }

  fun clearAuthorizationIntent() {
    authManager.clearAuthorizationIntent()
  }

  fun onSignInRequiredDialogDismissed() {
    _uiState.update { it.copy(showSignInRequiredDialog = false) }
    Log.d(TAG, "Sign-in required dialog was dismissed by the user.")
  }

  // --- ДЕЙСТВИЯ КАЛЕНДАРЯ ---
  fun onVisibleDateChanged(newDate: LocalDate) {
    if (newDate == _currentVisibleDate.value) return
    _currentVisibleDate.value = newDate
    viewModelScope.launch { calendarRepository.setCurrentVisibleDate(newDate) }
  }

  fun getEventsFlowForDate(date: LocalDate): Flow<List<EventDto>> =
      calendarRepository.getEventsFlowForDate(date)

  fun refreshCurrentVisibleDate() {
    viewModelScope.launch { calendarRepository.refreshDate(currentVisibleDate.value) }
  }

  // --- ДЕЙСТВИЯ AI ---
  fun startListening() {
    if (!_uiState.value.isPermissionGranted) {
      viewModelScope.launch {
        _eventFlow.emit(CalendarUiEvent.ShowMessage("Нет разрешения на запись аудио"))
      }
      return
    }
    aiInteractionManager.startListening()
  }

  fun stopListening() = aiInteractionManager.stopListening()

  fun sendTextMessage(text: String) {
    if (!_uiState.value.isSignedIn) {
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

  // --- LIFECYCLE ---
  override fun onCleared() {
    super.onCleared()
    aiInteractionManager.destroy() // Вызываем очистку менеджера AI
  }

  // --- COMPANION ---
  companion object {
    private const val TAG = "CalendarViewModel" // Используем один TAG
  }
}

sealed class CalendarUiEvent {
  data class ShowMessage(val message: String) : CalendarUiEvent()
}
