package com.lpavs.caliinda.data.mapper

import android.util.Log
import com.lpavs.caliinda.data.local.CalendarEventEntity
import com.lpavs.caliinda.data.local.DateTimeUtils
import com.lpavs.caliinda.ui.screens.main.CalendarEvent // Твоя модель для UI/сети
import java.time.Instant
import java.time.temporal.ChronoUnit

object EventMapper {

    private const val TAG = "EventMapper"

    // Принимает ID часового пояса как параметр
    fun mapToEntity(event: CalendarEvent, zoneIdString: String): CalendarEventEntity? {
        try {
            // 1. Используем поле isAllDay из CalendarEvent (самый надежный способ)
            val isAllDayEvent = event.isAllDay // <--- Читаем поле

            // 2. Парсим startTime с учетом часового пояса
            val startTimeInstant: Instant? = DateTimeUtils.parseToInstant(event.startTime, zoneIdString)

            if (startTimeInstant == null) {
                Log.w(TAG, "Failed to parse start time for event '${event.summary}' ('${event.startTime}'), skipping.")
                return null
            }
            val startTimeMillis = startTimeInstant.toEpochMilli()

            // 3. Парсим endTime с учетом часового пояса
            val endTimeInstant: Instant? = DateTimeUtils.parseToInstant(event.endTime, zoneIdString)

            val endTimeMillis: Long = when {
                // Есть валидный endTimeInstant И он строго после startTime
                endTimeInstant != null && endTimeInstant.toEpochMilli() > startTimeMillis -> {
                    endTimeInstant.toEpochMilli()
                }
                // Это событие "весь день" (определено по полю isAllDay)
                isAllDayEvent -> {
                    // Конец = начало + 24 часа (начало следующего дня UTC)
                    startTimeInstant.plus(1, ChronoUnit.DAYS).toEpochMilli()
                }
                // Иначе (нет валидного endTime ИЛИ это не "весь день")
                else -> {
                    Log.w(TAG, "Event '${event.summary}' (not all-day or no end time) using start time as end time.")
                    startTimeMillis
                }
            }

            return CalendarEventEntity(
                id = event.id,
                summary = event.summary,
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                description = event.description,
                location = event.location,
                isAllDay = isAllDayEvent,
                recurringEventId = event.recurringEventId,       // Из event (domain)
                originalStartTimeString = event.originalStartTime // Из event (domain)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping CalendarEvent to Entity: ${event.id}", e)
            return null
        }
    }

    // Преобразует Entity из БД в модель для UI/ViewModel
    // Принимает ID часового пояса для корректного форматирования
    fun mapToDomain(entity: CalendarEventEntity, zoneIdString: String): CalendarEvent {
        // Форматируем время из UTC millis в строки ISO с учетом НУЖНОГО пояса
        // Обычно для UI нужен пояс пользователя (zoneIdString)
        val startTimeStr = DateTimeUtils.formatMillisToIsoString(entity.startTimeMillis, zoneIdString)
        val endTimeStr = DateTimeUtils.formatMillisToIsoString(entity.endTimeMillis, zoneIdString)

        return CalendarEvent(
            id = entity.id,
            summary = entity.summary,
            startTime = startTimeStr ?: "", // Передаем отформатированную строку
            endTime = endTimeStr ?: "",   // Передаем отформатированную строку
            description = entity.description,
            location = entity.location,
            isAllDay = entity.isAllDay,
            recurringEventId = entity.recurringEventId,
            originalStartTime = entity.originalStartTimeString
        )
    }
}