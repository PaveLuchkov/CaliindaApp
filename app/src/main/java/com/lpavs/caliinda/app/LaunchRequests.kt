package com.lpavs.caliinda.app

import android.content.Intent
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Просьбы к главному экрану снаружи — от виджетов. Флаг, а не событие: просьба должна дожить до
 * момента, когда экран календаря соберётся (холодный старт, возврат из настроек).
 */
object LaunchRequests {
  const val ACTION_CREATE_EVENT = "com.lpavs.caliinda.action.CREATE_EVENT"
  const val ACTION_OPEN_EVENT = "com.lpavs.caliinda.action.OPEN_EVENT"
  const val EXTRA_EVENT_ID = "event_id"
  const val EXTRA_INSTANCE_BEGIN = "instance_begin"
  const val EXTRA_EPOCH_DAY = "epoch_day"

  private val _createEvent = MutableStateFlow(false)
  val createEvent: StateFlow<Boolean> = _createEvent.asStateFlow()

  private val _openEvent = MutableStateFlow<OpenEventRequest?>(null)
  /** Тап по событию на виджете: открыть его день и подробности. */
  val openEvent: StateFlow<OpenEventRequest?> = _openEvent.asStateFlow()

  fun handle(intent: Intent?) {
    when (intent?.action) {
      ACTION_CREATE_EVENT -> _createEvent.value = true
      ACTION_OPEN_EVENT ->
          _openEvent.value =
              OpenEventRequest(
                  eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1),
                  instanceBegin = intent.getLongExtra(EXTRA_INSTANCE_BEGIN, -1),
                  date = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_EPOCH_DAY, 0)))
    }
  }

  fun consumeOpenEvent() {
    _openEvent.value = null
  }

  fun consumeCreateEvent() {
    _createEvent.value = false
  }
}

/** Какое событие открыть: экземпляр серии — по началу; [date] — день, который показать. */
data class OpenEventRequest(val eventId: Long, val instanceBegin: Long, val date: LocalDate)
