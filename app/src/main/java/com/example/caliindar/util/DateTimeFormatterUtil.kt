package com.example.caliindar.util

import android.util.Log
import com.example.caliindar.data.local.DateTimeUtils
import com.example.caliindar.ui.screens.main.CalendarEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

object  DateTimeFormatterUtil {
    private val TAG = "CalendarDataManager"
    fun formatEventTimeForDisplay(isoString: String?, isAllDayEvent: Boolean, pattern: String = "HH:mm"): String {
        if (isAllDayEvent) return "Весь день" // Главное изменение - проверяем флаг
        if (isoString.isNullOrBlank()) return "--:--"

        return try {
            val offsetDateTime = OffsetDateTime.parse(isoString)
            val localZoneId = ZoneId.systemDefault()
            val localDateTime = offsetDateTime.atZoneSameInstant(localZoneId)
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale("ru"))
            localDateTime.format(formatter)
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "Error parsing non-all-day time string: $isoString", e)
            "Ошибка времени" // Ошибки парсинга для НЕ all-day событий - это проблема
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting time for display: $isoString", e)
            "Ошибка времени"
        }
    }

    fun formatDisplayDate(isoTimeString: String?): String { // Переименуем для ясности
        if (isoTimeString.isNullOrBlank()) return "Дата не указана"

        return try {
            val temporalAccessor: java.time.temporal.TemporalAccessor = try {
                OffsetDateTime.parse(isoTimeString)
            } catch (e: DateTimeParseException) {
                LocalDate.parse(isoTimeString)
            }
            val localZoneId = ZoneId.systemDefault()
            val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))

            when (temporalAccessor) {
                is OffsetDateTime -> temporalAccessor.atZoneSameInstant(localZoneId).format(formatter)
                is LocalDate -> temporalAccessor.format(formatter)
                is LocalDateTime -> temporalAccessor.atZone(localZoneId).format(formatter)
                else -> {
                    Log.w(TAG, "Unsupported TemporalAccessor type in formatDisplayDate: ${temporalAccessor::class.java}")
                    "Неверный формат даты"
                }
            }

        } catch (e: DateTimeParseException) {
            Log.e(TAG, "Error parsing date string for display date: $isoTimeString", e)
            "Ошибка даты"
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting display date: $isoTimeString", e)
            "Ошибка даты"
        }
    }

    private val timeOnlyFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    fun formatEventListTime(event: CalendarEvent, zoneIdString: String, use12Hour: Boolean): String {
        if (event.isAllDay) return "Весь день"

        val zoneId = try { ZoneId.of(zoneIdString.ifEmpty { ZoneId.systemDefault().id }) }
        catch (e: Exception) { ZoneId.systemDefault() }

        val startInstant = DateTimeUtils.parseToInstant(event.startTime, zoneIdString)
        val endInstant = DateTimeUtils.parseToInstant(event.endTime, zoneIdString)

        fun formatTime(instant: Instant?): String {
            if (instant == null) return ""
            val localTime = instant.atZone(zoneId).toLocalTime()
            val hour = localTime.hour
            val minute = localTime.minute

            return if (use12Hour) {
                val amPm = if (hour < 12) "AM" else "PM"
                val hour12 = if (hour % 12 == 0) 12 else hour % 12
                if (minute == 0)
                    "$hour12 $amPm"
                else
                    String.format("%d:%02d %s", hour12, minute, amPm)
            } else {
                if (minute == 0)
                    String.format("%02d", hour)
                else
                    String.format("%02d:%02d", hour, minute)
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

}