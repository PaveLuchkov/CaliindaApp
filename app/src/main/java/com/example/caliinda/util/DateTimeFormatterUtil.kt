package com.example.caliinda.util

import android.content.Context
import android.util.Log
import com.example.caliinda.data.local.DateTimeUtils
import com.example.caliinda.ui.screens.main.CalendarEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import android.text.format.DateFormat // <-- Импорт для проверки системной настройки

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

    fun formatEventListTime(
        context: Context, // <-- Контекст для проверки системной настройки
        event: CalendarEvent,
        zoneIdString: String
    ): String {
        if (event.isAllDay) return "Весь день"

        val zoneId = try { ZoneId.of(zoneIdString.ifEmpty { ZoneId.systemDefault().id }) }
        catch (_: Exception) { ZoneId.systemDefault() }

        val startInstant = DateTimeUtils.parseToInstant(event.startTime, zoneIdString)
        val endInstant = DateTimeUtils.parseToInstant(event.endTime, zoneIdString)

        // Определяем системную настройку ОДИН РАЗ
        val useSystem24HourFormat = DateFormat.is24HourFormat(context)

        // --- ТВОЯ ФУНКЦИЯ formatTime С НЕБОЛЬШИМ ИЗМЕНЕНИЕМ ---
        fun formatTime(instant: Instant?): String {
            if (instant == null) return ""
            return try { // Добавим try-catch на всякий случай
                val localTime = instant.atZone(zoneId).toLocalTime()
                val hour = localTime.hour
                val minute = localTime.minute

                // Используем системную настройку вместо параметра use12Hour
                if (!useSystem24HourFormat) { // Если НЕ 24-часовой формат (т.е. 12-часовой AM/PM)
                    val amPm = if (hour < 12 || hour == 24) "AM" else "PM" // Скорректировал AM/PM для полуночи/полудня
                    val hour12 = when(hour) {
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

}