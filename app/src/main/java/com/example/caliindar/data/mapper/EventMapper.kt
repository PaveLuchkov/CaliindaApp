package com.example.caliindar.data.mapper

import android.util.Log
import com.example.caliindar.data.local.CalendarEventEntity
import com.example.caliindar.ui.screens.main.CalendarEvent // Твоя модель для UI/сети
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object EventMapper {

    private const val TAG = "EventMapper"

    // Преобразует сетевую/UI модель в Entity для БД
    fun mapToEntity(event: CalendarEvent): CalendarEventEntity? {
        try {
            val startTimeMillis = parseIsoToUtcMillis(event.startTime)
            // Если startTime не парсится, событие некорректно, пропускаем
            if (startTimeMillis == null) {
                Log.w(TAG, "Failed to parse start time for event '${event.summary}', skipping.")
                return null
            }
            // endTime может отсутствовать или быть такой же как startTime (для событий на весь день с 'date')
            // Если не парсится или отсутствует, используем startTime
            val endTimeMillis = parseIsoToUtcMillis(event.endTime) ?: startTimeMillis

            return CalendarEventEntity(
                id = event.id,
                summary = event.summary,
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                description = event.description,
                location = event.location
            )
        } catch (e: Exception) { // Ловим любые ошибки при маппинге
            Log.e(TAG, "Error mapping CalendarEvent to Entity: ${event.id}", e)
            return null // Возвращаем null, чтобы не сломать всю вставку
        }
    }

    // Преобразует Entity из БД в модель для UI/ViewModel
    fun mapToDomain(entity: CalendarEventEntity): CalendarEvent {
        return CalendarEvent(
            id = entity.id,
            summary = entity.summary,
            // Форматируем миллисекунды обратно в ISO строку для совместимости с остальным кодом
            // Важно: используем UTC или системную зону? Для UI лучше системную.
            startTime = formatMillisToIsoString(entity.startTimeMillis),
            endTime = formatMillisToIsoString(entity.endTimeMillis),
            description = entity.description,
            location = entity.location
        )
    }

    // --- Вспомогательные функции для времени ---

    private fun parseIsoToUtcMillis(isoString: String?): Long? {
        if (isoString.isNullOrBlank()) return null
        return try {
            // Пытаемся распарсить как OffsetDateTime (есть смещение или Z)
            OffsetDateTime.parse(isoString).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            try {
                // Если не вышло, пытаемся как LocalDate (для событий 'all-day', YYYY-MM-DD)
                // Считаем, что такое событие начинается в 00:00 UTC этого дня
                java.time.LocalDate.parse(isoString)
                    .atStartOfDay(ZoneOffset.UTC) // Начало дня в UTC
                    .toInstant()
                    .toEpochMilli()
            } catch (e2: DateTimeParseException) {
                Log.e(TAG, "Failed to parse date/time string: $isoString", e2)
                null // Не удалось распознать формат
            }
        } catch (e: Exception) {
            Log.e(TAG, "Generic error parsing date/time string: $isoString", e)
            null
        }
    }

    private fun formatMillisToIsoString(millis: Long): String {
        // Форматируем обратно в ISO 8601 строку со смещением системного пояса
        // или в UTC (Z) - выбери, что нужно твоему UI/ViewModel
        return try {
            Instant.ofEpochMilli(millis)
                .atOffset(ZoneOffset.UTC) // Форматируем в UTC (добавляет Z)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) // Стандартный ISO формат
            // Если нужно в системной зоне:
            // .atZone(ZoneId.systemDefault())
            // .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting millis to ISO string: $millis", e)
            "1970-01-01T00:00:00Z" // Заглушка при ошибке
        }
    }
}