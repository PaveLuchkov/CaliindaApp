package com.lpavs.caliinda.feature.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Обновляет виджет, когда приложение само поменяло календарь. */
@Singleton
class WidgetRefresher @Inject constructor(@ApplicationContext private val context: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  fun refresh() {
    scope.launch { runCatching { CaliindaWidget().updateAll(context) } }
  }
}

/**
 * Просит систему разбудить виджет к [at]: там начнётся или закончится событие, и «сейчас»
 * должно переключиться. Будильник неточный — точный требует отдельного разрешения, а сдвиг
 * на пару минут для виджета не страшен.
 */
internal fun scheduleWidgetUpdate(context: Context, at: Instant) {
  val ids =
      AppWidgetManager.getInstance(context)
          .getAppWidgetIds(ComponentName(context, CaliindaWidgetReceiver::class.java))
  if (ids.isEmpty()) return
  val intent =
      Intent(context, CaliindaWidgetReceiver::class.java)
          .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
          .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
  val pending =
      PendingIntent.getBroadcast(
          context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  context
      .getSystemService(AlarmManager::class.java)
      .set(AlarmManager.RTC, at.toEpochMilli() + ALARM_SLACK_MS, pending)
}

private const val ALARM_SLACK_MS = 5_000L
