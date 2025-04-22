package com.example.caliindar.data.mapper

import android.util.Log
import com.example.caliindar.data.local.CalendarEventEntity
import com.example.caliindar.ui.screens.main.CalendarEvent // Твоя модель для UI/сети
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

object EventMapper {

    private const val TAG = "EventMapper"

    // Преобразует сетевую/UI модель в Entity для БД
    fun mapToEntity(event: CalendarEvent): CalendarEventEntity? {
        try {
            var isAllDayEvent = false
            val startTimeInstant: Instant? = parseToInstant(event.startTime) { isAllDay ->
                isAllDayEvent = isAllDay // Устанавливаем флаг на основе парсинга startTime
            }
            // Если startTime не парсится, событие некорректно, пропускаем
            if (startTimeInstant == null) {
                Log.w(TAG, "Failed to parse start time for event '${event.summary}', skipping.")
                return null
            }
            val startTimeMillis = startTimeInstant.toEpochMilli()


            val endTimeMillis: Long = run { // Используем run для ясности
                val endTimeInstant: Instant? = parseToInstant(event.endTime) { /* lambda игнорируется */ }

                when {
                    // 1. Есть явное время конца и оно парсится
                    endTimeInstant != null -> {
                        val parsedEndTimeMillis = endTimeInstant.toEpochMilli()
                        // Если событие "весь день", но время конца некорректно (<= startTime),
                        // исправляем на startTime + 24 часа. Иначе используем распарсенное.
                        if (isAllDayEvent && parsedEndTimeMillis <= startTimeMillis) {
                            startTimeInstant.plus(1, ChronoUnit.DAYS).toEpochMilli()
                        } else {
                            parsedEndTimeMillis
                        }
                    }
                    // 2. Нет явного времени конца, НО это событие "весь день"
                    isAllDayEvent -> {
                        // Для "весь день" конец должен быть +24 часа от начала
                        startTimeInstant.plus(1, ChronoUnit.DAYS).toEpochMilli()
                    }
                    // 3. Нет явного времени конца И это НЕ событие "весь день"
                    else -> {
                        // В этом случае (например, событие без длительности),
                        // разумно использовать время начала как время конца.
                        startTimeMillis
                    }
                }
            }

            return CalendarEventEntity(
                id = event.id,
                summary = event.summary,
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis, // Используем скорректированное время
                description = event.description,
                location = event.location,
                isAllDay = isAllDayEvent // <-- Сохраняем флаг
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
            startTime = formatMillisToIsoString(entity.startTimeMillis),
            endTime = formatMillisToIsoString(entity.endTimeMillis),
            description = entity.description,
            location = entity.location,
            isAllDay = entity.isAllDay
        )
    }

    // --- Вспомогательные функции для времени ---

    private fun parseToInstant(isoString: String?, onParseType: (isAllDay: Boolean) -> Unit): Instant? {
        if (isoString.isNullOrBlank()) return null
        return try {
            // Пытаемся как OffsetDateTime (есть время)
            val instant = OffsetDateTime.parse(isoString).toInstant()
            onParseType(false) // Не весь день
            instant
        } catch (e: DateTimeParseException) {
            try {
                // Пытаемся как LocalDate (весь день)
                val instant = LocalDate.parse(isoString)
                    .atStartOfDay(ZoneOffset.UTC) // 00:00 UTC
                    .toInstant()
                onParseType(true) // Весь день
                instant
            } catch (e2: DateTimeParseException) {
                Log.e(TAG, "Failed to parse date/time string: $isoString", e2)
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Generic error parsing date/time string: $isoString", e)
            null
        }
    }


    fun parseIsoToLocalDate(isoString: String?): LocalDate? {
        if (isoString.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(isoString).toLocalDate()
        } catch (e: DateTimeParseException) {
            try {
                LocalDate.parse(isoString) // Для формата YYYY-MM-DD
            } catch (e2: DateTimeParseException) {
                null
            }
        } catch (e: Exception) {
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