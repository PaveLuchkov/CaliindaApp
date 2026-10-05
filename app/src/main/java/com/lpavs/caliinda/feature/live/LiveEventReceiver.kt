package com.lpavs.caliinda.feature.live

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Будит live-уведомление: свой будильник, перезагрузка, обновление приложения, смена времени
 * или пояса и изменения в календаре (их провайдер шлёт и фоновым приложениям).
 */
@AndroidEntryPoint
class LiveEventReceiver : BroadcastReceiver() {
  @Inject lateinit var notifier: LiveEventNotifier

  override fun onReceive(context: Context, intent: Intent) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
      try {
        notifier.refresh()
      } finally {
        pending.finish()
      }
    }
  }

  companion object {
    const val ACTION_REFRESH = "com.lpavs.caliinda.action.LIVE_EVENT_REFRESH"
  }
}
