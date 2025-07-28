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
      val isAllDayEvent = event.isAllDay
      val startTimeInstant: Instant? = DateTimeUtils.parseToInstant(event.startTime, zoneIdString)

      if (startTimeInstant == null) {
        Log.w(
            TAG,
            "Failed to parse start time for event '${event.summary}' ('${event.startTime}'), skipping.")
        return null
      }
      val startTimeMillis = startTimeInstant.toEpochMilli()

      val endTimeInstant: Instant? = DateTimeUtils.parseToInstant(event.endTime, zoneIdString)

      val endTimeMillis: Long =
          when {
            endTimeInstant != null && endTimeInstant.toEpochMilli() > startTimeMillis -> {
              endTimeInstant.toEpochMilli()
            }
            isAllDayEvent -> {
              startTimeInstant.plus(1, ChronoUnit.DAYS).toEpochMilli()
            }
            else -> {
              Log.w(
                  TAG,
                  "Event '${event.summary}' (not all-day or no end time) using start time as end time.")
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
          recurringEventId = event.recurringEventId, // Из event (domain)
          originalStartTimeString = event.originalStartTime,
          recurrenceRuleString = event.recurrenceRule)
    } catch (e: Exception) {
      Log.e(TAG, "Error mapping CalendarEvent to Entity: ${event.id}", e)
      return null
    }
  }


  fun mapToDomain(entity: CalendarEventEntity, zoneIdString: String): CalendarEvent {
    val startTimeStr = DateTimeUtils.formatMillisToIsoString(entity.startTimeMillis, zoneIdString)
    val endTimeStr = DateTimeUtils.formatMillisToIsoString(entity.endTimeMillis, zoneIdString)

    return CalendarEvent(
        id = entity.id,
        summary = entity.summary,
        startTime = startTimeStr ?: "",
        endTime = endTimeStr ?: "",
        description = entity.description,
        location = entity.location,
        isAllDay = entity.isAllDay,
        recurringEventId = entity.recurringEventId,
        originalStartTime = entity.originalStartTimeString,
        recurrenceRule = entity.recurrenceRuleString)
  }
}
