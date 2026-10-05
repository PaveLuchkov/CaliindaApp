package com.lpavs.caliinda.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.lpavs.caliinda.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * Повтор одной строкой: «Каждые 2 недели · пн, чт · до 1 дек. 2026 г.». Общая для формы, шторки
 * повтора и деталей события — правило везде читается одинаково.
 */
@Composable
fun recurrenceText(
    freq: String?,
    interval: Int,
    byDay: Collection<DayOfWeek>,
    untilDate: LocalDate?,
    count: Int?,
): String {
  val base =
      if (interval <= 1) {
        stringResource(
            when (freq) {
              "DAILY" -> R.string.recurrence_daily
              "WEEKLY" -> R.string.recurrence_weekly
              "MONTHLY" -> R.string.recurrence_monthly
              "YEARLY" -> R.string.recurrence_yearly
              else -> R.string.recurrence_event
            })
      } else {
        val plural =
            when (freq) {
              "WEEKLY" -> R.plurals.recurrence_every_n_weeks
              "MONTHLY" -> R.plurals.recurrence_every_n_months
              "YEARLY" -> R.plurals.recurrence_every_n_years
              else -> R.plurals.recurrence_every_n_days
            }
        pluralStringResource(plural, interval, interval)
      }
  val days =
      byDay
          .takeIf { freq == "WEEKLY" && it.isNotEmpty() }
          ?.sorted()
          ?.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
  val formatter = remember {
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
  }
  val end =
      untilDate?.let { stringResource(R.string.recurrence_until, it.format(formatter)) }
          ?: count?.let { pluralStringResource(R.plurals.recurrence_times, it, it) }
  return listOfNotNull(base, days, end).joinToString(" · ")
}
