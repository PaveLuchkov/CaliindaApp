package com.example.caliindar.data.local

import android.util.Log
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
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
        } catch (e: DateTimeParseException) {
            try {
                // Попытка 2: Как LocalDateTime (строка содержит дату и время, но без смещения/зоны)
                // Применяем выбранный пользователем часовой пояс для получения Instant (UTC)
                LocalDateTime.parse(dateTimeString).atZone(zoneId).toInstant()
            } catch (e2: DateTimeParseException) {
                try {
                    // Попытка 3: Как LocalDate (строка содержит только дату - событие "весь день")
                    // Представляем начало дня (00:00) в UTC.
                    // Это стандартное представление Instant для начала all-day событий.
                    LocalDate.parse(dateTimeString)
                        .atStartOfDay(ZoneOffset.UTC) // Используем UTC для консистентности хранения
                        .toInstant()
                } catch (e3: DateTimeParseException) {
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
}