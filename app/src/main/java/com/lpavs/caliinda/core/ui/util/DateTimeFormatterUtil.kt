package com.lpavs.caliinda.core.ui.util

import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.remote.calendar.dto.EventDto
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface IDateTimeFormatterUtil {
  fun formatEventListTime(context: Context, event: EventDto, zoneIdString: String): String

  fun formatEventDetailsTime(
      context: Context,
      event: EventDto,
      zoneIdString: String,
      locale: Locale
  ): String
}

@Singleton
class DateTimeFormatterUtilImpl @Inject constructor(private val dateTimeUtils: IDateTimeUtils) :
    IDateTimeFormatterUtil {
  override fun formatEventListTime(
      context: Context,
      event: EventDto,
      zoneIdString: String
  ): String {
    if (event.isAllDay) return context.getString(R.string.all_day)

    val zoneId =
        try {
          ZoneId.of(zoneIdString.ifEmpty { ZoneId.systemDefault().id })
        } catch (_: Exception) {
          ZoneId.systemDefault()
        }

    val startInstant = dateTimeUtils.parseToInstant(event.startTime, zoneIdString)
    val endInstant = dateTimeUtils.parseToInstant(event.endTime, zoneIdString)

    val useSystem24HourFormat = DateFormat.is24HourFormat(context)

    fun formatTime(instant: Instant?): String {
      if (instant == null) return ""
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

    return when {
      startInstant != null && endInstant != null -> {
        "${formatTime(startInstant)} - ${formatTime(endInstant)}"
      }
      startInstant != null -> formatTime(startInstant)
      else -> ""
    }
  }

  override fun formatEventDetailsTime(
      context: Context,
      event: EventDto,
      zoneIdString: String,
      locale: Locale
  ): String {
    val zoneId =
        try {
          ZoneId.of(zoneIdString.ifEmpty { ZoneId.systemDefault().id })
        } catch (_: Exception) {
          ZoneId.systemDefault()
        }

    val startInstant = dateTimeUtils.parseToInstant(event.startTime, zoneIdString)
    val endInstant = dateTimeUtils.parseToInstant(event.endTime, zoneIdString)

    val useSystem24HourFormat = DateFormat.is24HourFormat(context)

    fun formatTime(instant: Instant?): String {
      if (instant == null) return ""
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

    fun formatDate(instant: Instant?): String {
      if (instant == null) return ""
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
      startInstant != null && endInstant != null -> {
        if (event.isAllDay) return formatDate(startInstant)
        if (formatDate(startInstant) == formatDate(endInstant)) {
          "${formatTime(startInstant)} - ${formatTime(endInstant)}\n${formatDate(endInstant)}"
        } else {
          "${formatDate(startInstant)} ${formatTime(startInstant)} - ${formatTime(endInstant)} ${formatDate(endInstant)}"
        }
      }
      startInstant != null -> formatTime(startInstant)
      else -> ""
    }
  }
}

@Composable
fun formatRRule(rrule: String, zoneIdString: String): String {
  val zoneId =
      try {
        ZoneId.of(zoneIdString.ifEmpty { ZoneId.systemDefault().id })
      } catch (_: Exception) {
        ZoneId.systemDefault()
      }
  val currentLocale = LocalConfiguration.current.getLocales().get(0)

  val parts =
      rrule.removePrefix("RRULE:").split(";").associate {
        val (key, value) = it.split("=")
        key to value
      }

  val freq = parts["FREQ"]
  val byDay = parts["BYDAY"]
  val until = parts["UNTIL"]
  val count = parts["COUNT"]

  val freqText =
      when (freq) {
        "DAILY" -> stringResource(R.string.recurrence_daily_lower)
        "WEEKLY" -> stringResource(R.string.recurrence_weekly_lower)
        "MONTHLY" -> stringResource(R.string.recurrence_monthly_lower)
        "YEARLY" -> stringResource(R.string.recurrence_yearly_lower)
        else -> stringResource(R.string.recurrence_unknown)
      }

  val daysText =
      byDay
          ?.split(",")
          ?.mapNotNull {
            when (it) {
              "MO" -> stringResource(R.string.day_monday)
              "TU" -> stringResource(R.string.day_tuesday)
              "WE" -> stringResource(R.string.day_wednesday)
              "TH" -> stringResource(R.string.day_thursday)
              "FR" -> stringResource(R.string.day_friday)
              "SA" -> stringResource(R.string.day_saturday)
              "SU" -> stringResource(R.string.day_sunday)
              else -> null
            }
          }
          ?.joinToString(", ")
          ?.let { stringResource(R.string.recurrence_on_days, it) } ?: ""

  val untilDateFormatted =
      try {
        until?.let {
          val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
          val date = LocalDateTime.parse(it, formatter).atZone(zoneId).toLocalDate()
          date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", currentLocale))
        }
      } catch (_: Exception) {
        null
      }

  val untilText = untilDateFormatted?.let { stringResource(R.string.recurrence_until, it) } ?: ""
  val countText = count?.let { stringResource(R.string.recurrence_count, it.toInt()) } ?: ""

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
