package com.lpavs.caliinda.core.ui.util

import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.runtime.Composable
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.calendar.recurrence.Rrule
import java.time.Instant
import java.time.LocalDate
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

  /** Начало и конец многодневного события по отдельности — кнопками; однодневное — null. */
  fun formatEventEnds(context: Context, event: EventDto, zoneId: ZoneId, locale: Locale): EventEnds?
}

/** Концы многодневного события: дата (куда перейти по тапу) и подпись. */
data class EventEnds(
    val startDate: LocalDate,
    val startText: String,
    val endDate: LocalDate,
    val endText: String,
)

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
    fun time(instant: Instant) = formatTime(context, instant, zoneId)
    fun date(instant: Instant) = formatDate(instant, zoneId, locale)
    val start = event.startTime
    val end = event.endTime
    return when {
      event.isAllDay -> "${date(start)} - ${date(end.minus(1, ChronoUnit.DAYS))}"
      // В списке проектов многодневному событию хватает дат — время тут шум. Конец ровно в
      // полночь — это предыдущий день.
      project && date(start) != date(end) -> "${date(start)} - ${date(end.minusMillis(1))}"
      date(start) == date(end) -> "${time(start)} - ${time(end)}"
      else -> "${date(start)} ${time(start)} - ${time(end)} ${date(end)}"
    }
  }

  override fun formatEventDetailsTime(
      context: Context,
      event: EventDto,
      zoneId: ZoneId,
      locale: Locale
  ): String {
    fun time(instant: Instant) = formatTime(context, instant, zoneId)
    fun date(instant: Instant) = formatDate(instant, zoneId, locale)
    val start = event.startTime
    val end = event.endTime
    return when {
      event.isAllDay -> "${date(start)} - ${date(end.minus(1, ChronoUnit.DAYS))}"
      date(start) == date(end) -> "${time(start)} - ${time(end)}\n${date(end)}"
      else -> "${date(start)} ${time(start)} - ${time(end)} ${date(end)}"
    }
  }

  override fun formatEventEnds(
      context: Context,
      event: EventDto,
      zoneId: ZoneId,
      locale: Locale
  ): EventEnds? {
    val startDate = event.startTime.atZone(zoneId).toLocalDate()
    // Конец исключающий: all-day «по 5-е» хранится как 6-е 00:00, событие до полуночи — тоже
    // предыдущий день.
    val endDate =
        if (event.isAllDay) event.endTime.minus(1, ChronoUnit.DAYS).atZone(zoneId).toLocalDate()
        else event.endTime.minusMillis(1).atZone(zoneId).toLocalDate()
    if (!endDate.isAfter(startDate)) return null
    fun label(instant: Instant, date: LocalDate) =
        date.format(DateTimeFormatter.ofPattern("d MMMM", locale)) +
            if (event.isAllDay) "" else ", " + formatTime(context, instant, zoneId)
    return EventEnds(
        startDate, label(event.startTime, startDate), endDate, label(event.endTime, endDate))
  }

  private fun formatTime(context: Context, instant: Instant, zoneId: ZoneId): String =
      try {
        val localTime = instant.atZone(zoneId).toLocalTime()
        val hour = localTime.hour
        val minute = localTime.minute
        if (!DateFormat.is24HourFormat(context)) {
          val amPm = if (hour < 12) "AM" else "PM"
          val hour12 =
              when (hour) {
                0,
                12 -> 12
                else -> hour % 12
              }
          if (minute == 0) "$hour12 $amPm" else String.format("%d:%02d %s", hour12, minute, amPm)
        } else {
          if (minute == 0) String.format("%02d", hour) else String.format("%02d:%02d", hour, minute)
        }
      } catch (e: Exception) {
        Log.e("FormatTime", "Error formatting instant manually: $instant", e)
        ""
      }

  private fun formatDate(instant: Instant, zoneId: ZoneId, locale: Locale): String =
      try {
        instant.atZone(zoneId).toLocalDate().format(DateTimeFormatter.ofPattern("d MMMM", locale))
      } catch (e: Exception) {
        Log.e("FormatDate", "Error formatting date: $instant", e)
        ""
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
