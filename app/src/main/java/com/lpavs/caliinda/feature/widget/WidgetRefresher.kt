package com.lpavs.caliinda.feature.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import androidx.glance.appwidget.updateAll
import com.lpavs.caliinda.feature.widget.combo.ComboWidget
import com.lpavs.caliinda.feature.widget.countdown.CountdownWidget
import com.lpavs.caliinda.feature.widget.date.DateWidget
import com.lpavs.caliinda.feature.widget.stack.StackWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Обновляет виджет, когда календарь поменялся — в приложении или снаружи. */
@Singleton
class WidgetRefresher @Inject constructor(@ApplicationContext private val context: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  fun refresh() {
    scope.launch { update() }
  }

  /** Для ресивера: дождаться обновления, пока процесс жив. */
  suspend fun update() {
    runCatching { CaliindaWidget().updateAll(context) }
    runCatching { DateWidget().updateAll(context) }
    runCatching { ComboWidget().updateAll(context) }
    runCatching { CountdownWidget().updateAll(context) }
    runCatching { StackWidgetProvider.updateAll(context) }
  }
}

/**
 * Просит систему разбудить виджет к [at]: там начнётся или закончится событие, и «сейчас»
 * должно переключиться. Будильник неточный — точный требует отдельного разрешения, а сдвиг
 * на пару минут для виджета не страшен.
 */
internal fun scheduleWidgetUpdate(
    context: Context,
    at: Instant,
    receiver: Class<out BroadcastReceiver> = CaliindaWidgetReceiver::class.java,
) {
  val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, receiver))
  if (ids.isEmpty()) return
  // У каждого виджета свой ресивер, а значит, и свой будильник: интенты различаются компонентом.
  val intent =
      Intent(context, receiver)
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
