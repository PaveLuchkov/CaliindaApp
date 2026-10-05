package com.lpavs.caliinda.core.ui.util

import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.runtime.Composable
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.calendar.recurrence.Rrule
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface IDateTimeFormatterUtil {
  fun formatEventListTime(context: Context, event: EventDto, zoneId: ZoneId, project: Boolean, locale: Locale): String

  fun formatEventDetailsTime(
      context: Context,
      event: EventDto,
      zoneId: ZoneId,
      locale: Locale,
  ): String
}

@Singleton
class DateTimeFormatterUtilImpl @Inject constructor() :
    IDateTimeFormatterUtil {
  override fun formatEventListTime(
      context: Context,
      event: EventDto,
      zoneId: ZoneId,
      project: Boolean,
      locale: Locale
  ): String {
//    if (event.isAllDay) return context.getString(R.string.all_day)

    val startInstant = event.startTime
    val endInstant = event.endTime

    val useSystem24HourFormat = DateFormat.is24HourFormat(context)

    fun formatTime(instant: Instant): String {
      return try {
        val localTime = instant.atZone(zoneId).toLocalTime()
        val hour = localTime.hour
        val minute = localTime.minute

        if (!useSystem24HourFormat) {
          val amPm = if (hour < 12) "AM" else "PM"
          val hour12 =
              when (hour) {
                0,
                12 -> 12
                else -> hour % 12
              }
          if (minute == 0) {
            "$hour12 $amPm"
          } else {
            String.format("%d:%02d %s", hour12, minute, amPm)
          }
        } else {
          if (minute == 0) {
            String.format("%02d", hour)
          } else {
            String.format("%02d:%02d", hour, minute)
          }
        }
      } catch (e: Exception) {
        Log.e("FormatTime", "Error formatting instant manually: $instant", e)
        ""
      }
    }
      fun formatDate(instant: Instant): String {
          return try {
              val localDate = instant.atZone(zoneId).toLocalDate()
              val formatter = DateTimeFormatter.ofPattern("d MMMM", locale)
              localDate.format(formatter)
          } catch (e: Exception) {
              Log.e("FormatDate", "Error formatting date: $instant", e)
              ""
          }
      }

      return when {
          event.isAllDay ->
              "${formatDate(startInstant)} - ${formatDate(endInstant.minus(1, ChronoUnit.DAYS))}"
          formatDate(startInstant) == formatDate(endInstant) ->
              "${formatTime(startInstant)} - ${formatTime(endInstant)}"
          else ->
              "${formatDate(startInstant)} ${formatTime(startInstant)} - ${formatTime(endInstant)} ${formatDate(endInstant)}"
      }
  }

  override fun formatEventDetailsTime(
      context: Context,
      event: EventDto,
      zoneId: ZoneId,
      locale: Locale
  ): String {
    val startInstant = event.startTime
    val endInstant = event.endTime

    val useSystem24HourFormat = DateFormat.is24HourFormat(context)

    fun formatTime(instant: Instant): String {
      return try {
        val localTime = instant.atZone(zoneId).toLocalTime()
        val hour = localTime.hour
        val minute = localTime.minute

        if (!useSystem24HourFormat) {
          val amPm = if (hour < 12) "AM" else "PM"
          val hour12 =
              when (hour) {
                0,
                12 -> 12
                else -> hour % 12
              }
          if (minute == 0) {
            "$hour12 $amPm"
          } else {
            String.format("%d:%02d %s", hour12, minute, amPm)
          }
        } else {
          if (minute == 0) {
            String.format("%02d", hour)
          } else {
            String.format("%02d:%02d", hour, minute)
          }
        }
      } catch (e: Exception) {
        Log.e("FormatTime", "Error formatting instant manually: $instant", e)
        ""
      }
    }

    fun formatDate(instant: Instant): String {
      return try {
        val localDate = instant.atZone(zoneId).toLocalDate()
        val formatter = DateTimeFormatter.ofPattern("d MMMM", locale)
        localDate.format(formatter)
      } catch (e: Exception) {
        Log.e("FormatDate", "Error formatting date: $instant", e)
        ""
      }
    }

    return when {
      event.isAllDay ->
          "${formatDate(startInstant)} - ${formatDate(endInstant.minus(1, ChronoUnit.DAYS))}"
      formatDate(startInstant) == formatDate(endInstant) ->
          "${formatTime(startInstant)} - ${formatTime(endInstant)}\n${formatDate(endInstant)}"
      else ->
          "${formatDate(startInstant)} ${formatTime(startInstant)} - ${formatTime(endInstant)} ${formatDate(endInstant)}"
    }
  }
}

/** Правило из календаря одной строкой — так же, как его показывает форма. */
@Composable
fun formatRRule(rrule: String, zoneId: ZoneId): String {
  val rule = Rrule.parse(rrule)
  return recurrenceText(
      freq = rule.freq,
      interval = rule.interval ?: 1,
      byDay = rule.byDay,
      untilDate = rule.untilDate(zoneId),
      count = rule.count)
}
