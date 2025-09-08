package com.lpavs.caliinda.core.data.repository.mapper

import android.util.Log
import com.lpavs.caliinda.core.data.remote.calendar.dto.EventDto
import com.lpavs.caliinda.core.data.repository.CalendarEventEntity
import com.lpavs.caliinda.core.ui.util.IDateTimeUtils
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventMapper @Inject constructor(private val dateTimeUtils: IDateTimeUtils) {

  companion object {
    private const val TAG = "EventMapper"
  }

  fun mapToEntity(event: EventDto, zoneIdString: String): CalendarEventEntity? {
    try {
      val isAllDayEvent = event.isAllDay
      val startTimeInstant: Instant? = dateTimeUtils.parseToInstant(event.startTime, zoneIdString)

      if (startTimeInstant == null) {
        Log.w(
            TAG,
            "Failed to parse start time for event '${event.summary}' ('${event.startTime}'), skipping.")
        return null
      }
      val startTimeMillis = startTimeInstant.toEpochMilli()

      val endTimeInstant: Instant? = dateTimeUtils.parseToInstant(event.endTime, zoneIdString)

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
          recurringEventId = event.recurringEventId,
          originalStartTimeString = event.originalStartTime,
          recurrenceRuleString = event.recurrenceRule)
    } catch (e: Exception) {
      Log.e(TAG, "Error mapping CalendarEvent to Entity: ${event.id}", e)
      return null
    }
  }

  fun mapToDomain(entity: CalendarEventEntity, zoneIdString: String): EventDto {
    val startTimeStr = dateTimeUtils.formatMillisToIsoString(entity.startTimeMillis, zoneIdString)
    val endTimeStr = dateTimeUtils.formatMillisToIsoString(entity.endTimeMillis, zoneIdString)

    return EventDto(
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
