package com.lpavs.caliinda.core.ui.util

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.lpavs.caliinda.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Варианты напоминания для событий со временем (минуты до начала; null — без него). */
val TIMED_REMINDER_OPTIONS: List<Int?> = listOf(null, 0, 5, 10, 15, 30, 60, 24 * 60)

/**
 * Для событий на весь день: начало — полночь, поэтому «накануне в 9:00» — это 15 ч до него,
 * «накануне в 18:00» — 6 ч, «за 2 дня в 9:00» — 39 ч.
 */
val ALL_DAY_REMINDER_OPTIONS: List<Int?> = listOf(null, 15 * 60, 6 * 60, 39 * 60)

/** Подпись напоминания: «За 15 минут», «Накануне в 9:00», «Без напоминания». */
@Composable
fun reminderLabel(minutes: Int?, allDay: Boolean): String {
  if (minutes == null) return stringResource(R.string.reminder_none)
  if (minutes == 0) return stringResource(R.string.reminder_at_start)
  if (allDay) {
    // Сколько полуночей назад и в какое время того дня.
    val days = (minutes + MINUTES_IN_DAY - 1) / MINUTES_IN_DAY
    val time = LocalTime.MIDNIGHT.plusMinutes((days * MINUTES_IN_DAY - minutes).toLong())
    val context = LocalContext.current
    val pattern = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm a"
    val formatted = time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    return if (days == 1) stringResource(R.string.reminder_day_before_at, formatted)
    else stringResource(R.string.reminder_days_before_at, days, formatted)
  }
  return when {
    minutes % MINUTES_IN_DAY == 0 ->
        pluralStringResource(R.plurals.reminder_days_before, minutes / MINUTES_IN_DAY, minutes / MINUTES_IN_DAY)
    minutes % 60 == 0 -> pluralStringResource(R.plurals.reminder_hours_before, minutes / 60, minutes / 60)
    else -> pluralStringResource(R.plurals.reminder_minutes_before, minutes, minutes)
  }
}

private const val MINUTES_IN_DAY = 24 * 60
