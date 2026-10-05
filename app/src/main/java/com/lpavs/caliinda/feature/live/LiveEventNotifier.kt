package com.lpavs.caliinda.feature.live

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lpavs.caliinda.R
import com.lpavs.caliinda.app.MainActivity
import com.lpavs.caliinda.core.data.calendar.CalendarPermissionManager
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import com.lpavs.caliinda.core.ui.util.displayTitle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live-уведомление о текущем событии: прогресс до конца, обратный отсчёт и следующее событие,
 * если оно сразу за текущим. На Android 16 — ProgressStyle и Live Update (чип в статус-баре).
 * Само себя будит будильником: на шаг прогресса, конец события или начало следующего.
 */
@Singleton
class LiveEventNotifier
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val calendarRepository: CalendarRepository,
    private val settingsRepository: SettingsRepository,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val mutex = Mutex()

  /** Пересчитать в фоне — после правок, при старте, по настройке. */
  fun refreshAsync() {
    scope.launch { refresh() }
  }

  suspend fun refresh() =
      mutex.withLock {
        runCatching { update() }
      }

  private suspend fun update() {
    if (!settingsRepository.liveNotificationFlow.first() || !canPost()) {
      cancel()
      return
    }
    val zone = settingsRepository.zoneFlow.first()
    val now = Instant.now()
    val today = LocalDate.now(zone)
    // Завтра — чтобы после полуночи и вечером видеть ближайшее следующее.
    val events =
        (calendarRepository.getEventsFlowForDate(today).first() +
                calendarRepository.getEventsFlowForDate(today.plusDays(1)).first())
            .distinctBy { it.id }
    val plan = planLiveEvent(events, now)

    val current = plan.current
    if (current == null) {
      NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    } else {
      post(current, plan.next, now, zone)
    }
    // Ничего не ждём — всё равно проверим после полуночи: события могли появиться.
    schedule(plan.refreshAt ?: today.plusDays(1).atStartOfDay(zone).toInstant())
  }

  private fun canPost(): Boolean {
    val calendar =
        CalendarPermissionManager.PERMISSIONS.all {
          ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    val notifications =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    return calendar && notifications && NotificationManagerCompat.from(context).areNotificationsEnabled()
  }

  private fun post(current: EventDto, next: EventDto?, now: Instant, zone: ZoneId) {
    ensureChannel()
    val timeFormat =
        DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a")
            .withZone(zone)
    val total = Duration.between(current.startTime, current.endTime).toMinutes().coerceAtLeast(1)
    val elapsed = Duration.between(current.startTime, now).toMinutes().coerceIn(0, total)
    val text =
        if (next != null) {
          context.getString(
              R.string.live_next, next.displayTitle(context), timeFormat.format(next.startTime))
        } else {
          context.getString(R.string.live_until, timeFormat.format(current.endTime))
        }

    val openApp =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    val builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_event)
            .setContentTitle(current.displayTitle(context))
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Обратный отсчёт до конца — тикает сам, без пересборки уведомления.
            .setWhen(current.endTime.toEpochMilli())
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setRequestPromotedOngoing(true)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
      // Сегменты: текущее событие и, если оно вплотную, следующее — как этапы доставки.
      val style =
          NotificationCompat.ProgressStyle()
              .setStyledByProgress(true)
              .addProgressSegment(NotificationCompat.ProgressStyle.Segment(total.toInt()))
              .setProgress(elapsed.toInt())
      if (next != null) {
        val nextLength =
            Duration.between(next.startTime, next.endTime).toMinutes().coerceAtLeast(1).toInt()
        style
            .addProgressSegment(NotificationCompat.ProgressStyle.Segment(nextLength))
            .addProgressPoint(NotificationCompat.ProgressStyle.Point(total.toInt()))
      }
      builder.setStyle(style)
    } else {
      builder.setProgress(total.toInt(), elapsed.toInt(), false)
    }
    runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build()) }
  }

  private fun ensureChannel() {
    val manager = context.getSystemService(NotificationManager::class.java)
    if (manager.getNotificationChannel(CHANNEL_ID) != null) return
    val channel =
        NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.live_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT)
            .apply {
              description = context.getString(R.string.live_channel_desc)
              // Тихо: это табло, а не напоминание.
              setSound(null, null)
              enableVibration(false)
            }
    manager.createNotificationChannel(channel)
  }

  private fun schedule(at: Instant) {
    val alarms = context.getSystemService(AlarmManager::class.java)
    val time = at.toEpochMilli() + ALARM_SLACK_MS
    // Календарю точный будильник положен (USE_EXACT_ALARM); нет — пусть будет неточный.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
      alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarmIntent())
    } else {
      alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarmIntent())
    }
  }

  private fun cancel() {
    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    context.getSystemService(AlarmManager::class.java).cancel(alarmIntent())
  }

  private fun alarmIntent(): PendingIntent =
      PendingIntent.getBroadcast(
          context,
          0,
          Intent(context, LiveEventReceiver::class.java).setAction(LiveEventReceiver.ACTION_REFRESH),
          PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

  companion object {
    private const val CHANNEL_ID = "live_event"
    private const val NOTIFICATION_ID = 1001
    private const val ALARM_SLACK_MS = 2_000L
  }
}
