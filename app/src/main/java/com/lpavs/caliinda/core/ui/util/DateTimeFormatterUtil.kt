package com.lpavs.caliinda.core.ui.util

import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.calendar.recurrence.Rrule
import java.time.Instant
import java.time.DayOfWeek
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

@Composable
fun formatRRule(rrule: String, zoneId: ZoneId): String {
  val currentLocale = LocalConfiguration.current.getLocales().get(0)

  val rule = Rrule.parse(rrule)

  val freqText =
      when (rule.freq) {
        "DAILY" -> stringResource(R.string.recurrence_daily_lower)
        "WEEKLY" -> stringResource(R.string.recurrence_weekly_lower)
        "MONTHLY" -> stringResource(R.string.recurrence_monthly_lower)
        "YEARLY" -> stringResource(R.string.recurrence_yearly_lower)
        else -> stringResource(R.string.recurrence_unknown)
      }

  val daysText =
      rule.byDay
          .takeIf { it.isNotEmpty() }
          ?.map {
            when (it) {
              DayOfWeek.MONDAY -> stringResource(R.string.day_monday)
              DayOfWeek.TUESDAY -> stringResource(R.string.day_tuesday)
              DayOfWeek.WEDNESDAY -> stringResource(R.string.day_wednesday)
              DayOfWeek.THURSDAY -> stringResource(R.string.day_thursday)
              DayOfWeek.FRIDAY -> stringResource(R.string.day_friday)
              DayOfWeek.SATURDAY -> stringResource(R.string.day_saturday)
              DayOfWeek.SUNDAY -> stringResource(R.string.day_sunday)
            }
          }
          ?.joinToString(", ")
          ?.let { stringResource(R.string.recurrence_on_days, it) } ?: ""

  val untilDateFormatted =
      rule.untilDate(zoneId)?.format(DateTimeFormatter.ofPattern("d MMMM yyyy", currentLocale))

  val untilText = untilDateFormatted?.let { stringResource(R.string.recurrence_until, it) } ?: ""
  val countText = rule.count?.let { stringResource(R.string.recurrence_count, it) } ?: ""

  return buildString {
        append(stringResource(R.string.recurrence_repeats))
        append(" ")
        append(freqText)

        if (daysText.isNotBlank()) {
          append(" ")
          append(daysText)
        }
        if (untilText.isNotBlank()) {
          append(" ")
          append(untilText)
        }
        if (countText.isNotBlank()) {
          append(" ")
          append(countText)
        }
      }
      .trim()
}
