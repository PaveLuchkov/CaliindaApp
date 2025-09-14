package com.lpavs.caliinda.core.data.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(private val authManager: AuthManager) : ViewModel() {

  /** Предоставляет состояние аутентификации для UI. */
  val authState: StateFlow<AuthState> =
      authManager.authState.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000),
          initialValue = authManager.authState.value // Начинаем с текущего значения
          )

  /**
   * Предоставляет одноразовые события (например, успешный выход из аккаунта) для UI. UI может
   * подписаться на них для выполнения навигации или показа Snackbar.
   */
  val authEvents: SharedFlow<AuthEvent> = authManager.authEvents

  /**
   * Инициирует процесс входа в систему. UI вызывает этот метод, передавая Activity, необходимую для
   * запуска UI от Google.
   */
  fun signIn(activity: Activity) {
    authManager.signIn(activity)
  }

  /** Обрабатывает результат от PendingIntent, полученного для авторизации календаря. */
  fun handleAuthorizationResult(intent: android.content.Intent) {
    authManager.handleAuthorizationResult(intent)
  }

  /** Инициирует процесс выхода из системы. */
  fun signOut() {
    authManager.signOut()
  }

  /** Сбрасывает ошибку в состоянии, чтобы UI мог убрать сообщение об ошибке. */
  fun clearAuthError() {
    authManager.clearAuthError()
  }

  /** Сбрасывает PendingIntent после его использования. */
  fun clearAuthorizationIntent() {
    authManager.clearAuthorizationIntent()
  }
}
