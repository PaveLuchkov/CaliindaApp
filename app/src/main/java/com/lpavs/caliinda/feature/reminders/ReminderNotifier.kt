package com.lpavs.caliinda.feature.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CalendarContract.CalendarAlerts
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lpavs.caliinda.R
import com.lpavs.caliinda.app.MainActivity
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Напоминание, которое календарь поставил на это время. */
private data class Alert(
    val id: Long,
    val eventId: Long,
    val title: String?,
    val location: String?,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
)

/**
 * Показывает напоминания о событиях, если этого не делает Google Календарь (его нет или он
 * отключён) — или если пользователь включил их явно.
 */
@Singleton
class ReminderNotifier
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val googleCalendar: GoogleCalendarApp,
) {
  /** Включены ли наши напоминания: явная настройка или «сами, если нет Google Календаря». */
  suspend fun isEnabled(): Boolean =
      settingsRepository.reminderNotificationsFlow.first() ?: !googleCalendar.isActive()

  suspend fun onReminder(alarmTime: Long) {
    if (!isEnabled() || !canPost()) return
    val zone = settingsRepository.zoneFlow.first()
    runCatching { scheduledAlerts(alarmTime) }
        .getOrDefault(emptyList())
        .forEach { alert ->
          post(alert, zone)
          markFired(alert.id)
        }
  }

  private fun scheduledAlerts(alarmTime: Long): List<Alert> =
      context.contentResolver
          .query(
              CalendarAlerts.CONTENT_URI,
              arrayOf(
                  CalendarAlerts._ID,
                  CalendarAlerts.EVENT_ID,
                  CalendarAlerts.TITLE,
                  CalendarAlerts.EVENT_LOCATION,
                  CalendarAlerts.BEGIN,
                  CalendarAlerts.END,
                  CalendarAlerts.ALL_DAY),
              "${CalendarAlerts.ALARM_TIME} = ? AND ${CalendarAlerts.STATE} = ?",
              arrayOf(alarmTime.toString(), CalendarAlerts.STATE_SCHEDULED.toString()),
              null)
          ?.use { c ->
            buildList {
              while (c.moveToNext()) {
                add(
                    Alert(
                        id = c.getLong(0),
                        eventId = c.getLong(1),
                        title = c.getString(2),
                        location = c.getString(3),
                        begin = c.getLong(4),
                        end = c.getLong(5),
                        allDay = c.getInt(6) == 1))
              }
            }
          } ?: emptyList()

  /** Отмечаем показанным — иначе календарь посчитает напоминание пропущенным. */
  private fun markFired(alertId: Long) {
    runCatching {
      context.contentResolver.update(
          ContentUris.withAppendedId(CalendarAlerts.CONTENT_URI, alertId),
          ContentValues().apply { put(CalendarAlerts.STATE, CalendarAlerts.STATE_FIRED) },
          null,
          null)
    }
  }

  private fun post(alert: Alert, zone: ZoneId) {
    ensureChannel()
    val title = alert.title?.takeIf { it.isNotBlank() } ?: context.getString(R.string.no_title)
    val whenText =
        if (alert.allDay) {
          // All-day хранится в UTC-полночи — дата без сдвига пояса.
          Instant.ofEpochMilli(alert.begin)
              .atZone(ZoneOffset.UTC)
              .toLocalDate()
              .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        } else {
          val time =
              DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a")
                  .withZone(zone)
          "${time.format(Instant.ofEpochMilli(alert.begin))} – ${time.format(Instant.ofEpochMilli(alert.end))}"
        }
    val text = listOfNotNull(whenText, alert.location?.takeIf { it.isNotBlank() }).joinToString(" · ")

    val openApp =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    val notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_event)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(alert.begin)
            .setShowWhen(!alert.allDay)
            .build()
    // Своё уведомление на каждый экземпляр события: у серии их несколько в разные дни.
    val id = (alert.eventId * 31 + alert.begin / 60_000).toInt()
    runCatching { NotificationManagerCompat.from(context).notify(TAG, id, notification) }
  }

  private fun canPost(): Boolean {
    val allowed =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    return allowed && NotificationManagerCompat.from(context).areNotificationsEnabled()
  }

  private fun ensureChannel() {
    val manager = context.getSystemService(NotificationManager::class.java)
    if (manager.getNotificationChannel(CHANNEL_ID) != null) return
    manager.createNotificationChannel(
        NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminders_channel_name),
                NotificationManager.IMPORTANCE_HIGH)
            .apply { description = context.getString(R.string.reminders_channel_desc) })
  }

  private companion object {
    const val CHANNEL_ID = "event_reminders"
    const val TAG = "reminder"
  }
}
