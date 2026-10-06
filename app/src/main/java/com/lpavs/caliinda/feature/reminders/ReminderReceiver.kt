package com.lpavs.caliinda.feature.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Календарь сообщает, что подошло время напоминания. Широковещательный и неявный по замыслу —
 * провайдер не знает, какое приложение календаря у пользователя, — поэтому доходит и в фоне.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
  @Inject lateinit var notifier: ReminderNotifier

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != CalendarContract.ACTION_EVENT_REMINDER) return
    val alarmTime =
        intent.getLongExtra(CalendarContract.CalendarAlerts.ALARM_TIME, -1L).takeIf { it > 0 }
            ?: intent.data?.lastPathSegment?.toLongOrNull()
            ?: return
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        notifier.onReminder(alarmTime)
      } finally {
        pending.finish()
      }
    }
  }
}
