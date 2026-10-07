package com.lpavs.caliinda.app

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Просьбы к главному экрану снаружи — от виджетов. Флаг, а не событие: просьба должна дожить до
 * момента, когда экран календаря соберётся (холодный старт, возврат из настроек).
 */
object LaunchRequests {
  const val ACTION_CREATE_EVENT = "com.lpavs.caliinda.action.CREATE_EVENT"

  private val _createEvent = MutableStateFlow(false)
  val createEvent: StateFlow<Boolean> = _createEvent.asStateFlow()

  fun handle(intent: Intent?) {
    if (intent?.action == ACTION_CREATE_EVENT) _createEvent.value = true
  }

  fun consumeCreateEvent() {
    _createEvent.value = false
  }
}
