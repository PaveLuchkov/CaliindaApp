package com.lpavs.caliinda.util

import android.content.Context
import android.util.Log
import com.lpavs.caliinda.data.local.DateTimeUtils
import com.lpavs.caliinda.ui.screens.main.CalendarEvent
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.lpavs.caliinda.R
import java.util.Locale

object  DateTimeFormatterUtil {
    fun formatEventListTime(
        context: Context,
        event: CalendarEvent,
        zoneIdString: String
    ): String {
        if (event.isAllDay) return R.string.all_day.toString()

        val zoneId = try {
            ZoneId.of(zoneIdString.ifEmpty { ZoneId.systemDefault().id })
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }

        val startInstant = DateTimeUtils.parseToInstant(event.startTime, zoneIdString)
        val endInstant = DateTimeUtils.parseToInstant(event.endTime, zoneIdString)

        val useSystem24HourFormat = DateFormat.is24HourFormat(context)

        fun formatTime(instant: Instant?): String {
            if (instant == null) return ""
            return try { // Добавим try-catch на всякий случай
                val localTime = instant.atZone(zoneId).toLocalTime()
                val hour = localTime.hour
                val minute = localTime.minute

                // Используем системную настройку вместо параметра use12Hour
                if (!useSystem24HourFormat) { // Если НЕ 24-часовой формат (т.е. 12-часовой AM/PM)
                    val amPm =
                        if (hour < 12 || hour == 24) "AM" else "PM" // Скорректировал AM/PM для полуночи/полудня
                    val hour12 = when (hour) {
                        0, 12 -> 12 // 00:xx -> 12 AM, 12:xx -> 12 PM
                        else -> hour % 12
                    }
                    if (minute == 0) {
                        "$hour12 $amPm" // Формат без минут
                    } else {
                        String.format("%d:%02d %s", hour12, minute, amPm) // Формат с минутами
                    }
                } else { // Если 24-часовой формат
                    if (minute == 0) {
                        String.format("%02d", hour) // Формат без минут
                    } else {
                        String.format("%02d:%02d", hour, minute) // Формат с минутами
                    }
                }
            } catch (e: Exception) {
                Log.e("FormatTime", "Error formatting instant manually: $instant", e)
                ""
            }
        }
        // --- КОНЕЦ ТВОЕЙ ФУНКЦИИ formatTime ---

        return when {
            startInstant != null && endInstant != null -> {
                "${formatTime(startInstant)} - ${formatTime(endInstant)}"
            }
            startInstant != null -> formatTime(startInstant)
            else -> ""
        }
    }
    fun formatEventDetailsTime(
        context: Context,
        event: CalendarEvent,
        zoneIdString: String,
        locale: Locale
    ): String {
        val zoneId = try {
            ZoneId.of(zoneIdString.ifEmpty { ZoneId.systemDefault().id })
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }

        val startInstant = DateTimeUtils.parseToInstant(event.startTime, zoneIdString)
        val endInstant = DateTimeUtils.parseToInstant(event.endTime, zoneIdString)

        val useSystem24HourFormat = DateFormat.is24HourFormat(context)

        fun formatTime(instant: Instant?): String {
            if (instant == null) return ""
            return try { // Добавим try-catch на всякий случай
                val localTime = instant.atZone(zoneId).toLocalTime()
                val hour = localTime.hour
                val minute = localTime.minute

                // Используем системную настройку вместо параметра use12Hour
                if (!useSystem24HourFormat) { // Если НЕ 24-часовой формат (т.е. 12-часовой AM/PM)
                    val amPm =
                        if (hour < 12 || hour == 24) "AM" else "PM" // Скорректировал AM/PM для полуночи/полудня
                    val hour12 = when (hour) {
                        0, 12 -> 12 // 00:xx -> 12 AM, 12:xx -> 12 PM
                        else -> hour % 12
                    }
                    if (minute == 0) {
                        "$hour12 $amPm" // Формат без минут
                    } else {
                        String.format("%d:%02d %s", hour12, minute, amPm) // Формат с минутами
                    }
                } else { // Если 24-часовой формат
                    if (minute == 0) {
                        String.format("%02d", hour) // Формат без минут
                    } else {
                        String.format("%02d:%02d", hour, minute) // Формат с минутами
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
        // --- КОНЕЦ ТВОЕЙ ФУНКЦИИ formatTime ---

        return when {
            startInstant != null && endInstant != null -> {
                if (event.isAllDay) return formatDate(startInstant) // TODO когда будут события больше одного дней поменять
                if (formatDate(startInstant) == formatDate(endInstant)) {
                    "${formatTime(startInstant)} - ${formatTime(endInstant)}\n${formatDate(endInstant)}"
                } else
                {
                    "${formatDate(endInstant)} ${formatTime(startInstant)} - ${formatTime(endInstant)} ${formatDate(endInstant)}"
                }

            }

            startInstant != null -> formatTime(startInstant)
            else -> ""
        }
    }
    @Composable
    fun formatRRule(
        rrule: String,
        zoneIdString: String
    ): String {
        val zoneId = try {
            ZoneId.of(zoneIdString.ifEmpty { ZoneId.systemDefault().id })
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }
        val currentLocale = LocalConfiguration.current.getLocales().get(0)

        val parts = rrule
            .removePrefix("RRULE:")
            .split(";")
            .associate {
                val (key, value) = it.split("=")
                key to value
            }

        val freq = parts["FREQ"]
        val byDay = parts["BYDAY"]
        val until = parts["UNTIL"]
        val count = parts["COUNT"]

        val freqText = when (freq) {
            "DAILY" -> stringResource(R.string.recurrence_daily_lower)
            "WEEKLY" -> stringResource(R.string.recurrence_weekly_lower)
            "MONTHLY" -> stringResource(R.string.recurrence_monthly_lower)
            "YEARLY" -> stringResource(R.string.recurrence_yearly_lower)
            else -> stringResource(R.string.recurrence_unknown)
        }

        val daysText = byDay?.split(",")?.mapNotNull {
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
        }?.joinToString(", ")?.let { stringResource(R.string.recurrence_on_days, it) } ?: ""

        val untilDateFormatted = try {
            until?.let {
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                val date = LocalDateTime.parse(it, formatter).atZone(zoneId).toLocalDate()
                date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", currentLocale))
            }
        } catch (_: Exception) {
            null
        }

        val untilText = untilDateFormatted?.let {
            stringResource(R.string.recurrence_until, it)
        } ?: ""

        val countText = count?.let {
            stringResource(R.string.recurrence_count, it.toInt())
        } ?: ""

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
        }.trim()
    }
}