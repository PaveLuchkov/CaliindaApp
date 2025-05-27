package com.lpavs.caliinda.data.local

import android.util.Log
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateTimeUtils {
    fun parseToInstant(dateTimeString: String?, zoneIdString: String): Instant? {
        if (dateTimeString.isNullOrBlank()) return null

        // Определяем ZoneId, используя systemDefault как fallback
        val zoneId = try {
            ZoneId.of(zoneIdString.takeIf { it.isNotEmpty() } ?: ZoneId.systemDefault().id)
        } catch (e: Exception) {
            Log.w("DateTimeUtils", "Invalid zoneId '$zoneIdString', using system default.", e)
            ZoneId.systemDefault()
        }

        return try {
            // Попытка 1: Как OffsetDateTime (строка содержит время и смещение/зону)
            OffsetDateTime.parse(dateTimeString).toInstant()
        } catch (_: DateTimeParseException) {
            try {
                // Попытка 2: Как LocalDateTime (строка содержит дату и время, но без смещения/зоны)
                // Применяем выбранный пользователем часовой пояс для получения Instant (UTC)
                LocalDateTime.parse(dateTimeString).atZone(zoneId).toInstant()
            } catch (_: DateTimeParseException) {
                try {
                    // Попытка 3: Как LocalDate (строка содержит только дату - событие "весь день")
                    // Представляем начало дня (00:00) в UTC.
                    // Это стандартное представление Instant для начала all-day событий.
                    LocalDate.parse(dateTimeString)
                        .atStartOfDay(ZoneOffset.UTC) // Используем UTC для консистентности хранения
                        .toInstant()
                } catch (_: DateTimeParseException) {
                    // Если ни один из форматов не подошел
                    Log.w("DateTimeUtils", "Could not parse '$dateTimeString' as OffsetDateTime, LocalDateTime, or LocalDate.")
                    null // Все попытки парсинга не удались
                }
            }
        } catch (e: Exception) { // Ловим другие возможные ошибки (не только парсинг)
            Log.e("DateTimeUtils", "Generic error parsing date/time string: '$dateTimeString'", e)
            null
        }
    }

    fun formatMillisToIsoString(millis: Long?, zoneIdString: String): String? {
        if (millis == null) return null
        val zoneId = try { ZoneId.of(zoneIdString.ifEmpty { ZoneId.systemDefault().id }) }
        catch (e: Exception) { ZoneId.systemDefault() }
        return try {
            Instant.ofEpochMilli(millis)
                .atZone(zoneId) // Применяем нужный пояс
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) // Формат со смещением
        } catch (e: Exception) {
            Log.e("DateTimeUtils", "Error formatting millis $millis to ISO string for zone $zoneIdString", e)
            null // Возвращаем null при ошибке
        }
    }

    fun formatDateTimeToIsoWithOffset(
        date: LocalDate?,
        time: LocalTime?,
        isAllDay: Boolean, // Этот параметр здесь, возможно, не так нужен, если isAllDay обрабатывается выше
        zoneIdString: String
    ): String? {
        if (date == null) return null

        val zoneId = try {
            ZoneId.of(zoneIdString.takeIf { it.isNotEmpty() } ?: ZoneId.systemDefault().id)
        } catch (e: Exception) {
            Log.w("DateTimeUtils", "Invalid zoneId '$zoneIdString', using system default.", e)
            ZoneId.systemDefault()
        }

        if (time == null && !isAllDay) { // Явно проверяем, что для timed-события время должно быть
            Log.w("DateTimeUtils", "Time is required for non-all-day event formatting to ISO offset string.")
            return null
        }

        val effectiveTime = if (isAllDay) LocalTime.MIDNIGHT else time!! // Если не all-day, time не должен быть null

        return try {
            val zonedDateTime = date.atTime(effectiveTime).atZone(zoneId)
            zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) // Возвращает с offset'ом
        } catch (e: Exception) {
            Log.e("DateTimeUtils", "Error formatting date/time to ISO offset string", e)
            null
        }
    }

    // --- НОВАЯ ФУНКЦИЯ ---
    /**
     * Форматирует LocalDate и LocalTime в строку ISO 8601 вида "yyyy-MM-dd'T'HH:mm:ss"
     * БЕЗ информации о таймзоне или смещении.
     * Предполагается, что timeZoneId будет отправлен отдельно.
     *
     * @param date Дата события.
     * @param time Время события.
     * @return Строка "yyyy-MM-dd'T'HH:mm:ss" или null при ошибке.
     */
    fun formatLocalDateTimeToNaiveIsoString( // Назовем "Naive", чтобы подчеркнуть отсутствие TZ info
        date: LocalDate?,
        time: LocalTime?
    ): String? {
        if (date == null || time == null) {
            Log.w("DateTimeUtils", "Date and Time are required for formatting to naive ISO string.")
            return null
        }

        return try {
            // Собираем LocalDateTime (без информации о зоне)
            val localDateTime = LocalDateTime.of(date, time)
            // Форматируем в ISO 8601 без смещения и без 'Z'
            // Стандартный DateTimeFormatter.ISO_LOCAL_DATE_TIME как раз это и делает.
            localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) // "YYYY-MM-DDTHH:MM:SS"
        } catch (e: Exception) {
            Log.e("DateTimeUtils", "Error formatting LocalDateTime to naive ISO string", e)
            null
        }
    }
}

