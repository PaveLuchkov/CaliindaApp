package com.lpavs.caliinda.feature.live

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lpavs.caliinda.feature.widget.WidgetRefresher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Будит live-уведомление и виджеты: свой будильник, перезагрузка, обновление приложения, смена
 * времени или пояса и изменения в календаре (их провайдер шлёт и фоновым приложениям) — иначе
 * событие, поправленное в Google Calendar, дошло бы до виджета только через полчаса.
 */
@AndroidEntryPoint
class LiveEventReceiver : BroadcastReceiver() {
  @Inject lateinit var notifier: LiveEventNotifier
  @Inject lateinit var widgetRefresher: WidgetRefresher

  override fun onReceive(context: Context, intent: Intent) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
      try {
        notifier.refresh()
        widgetRefresher.update()
      } finally {
        pending.finish()
      }
    }
  }

  companion object {
    const val ACTION_REFRESH = "com.lpavs.caliinda.action.LIVE_EVENT_REFRESH"
  }
}
