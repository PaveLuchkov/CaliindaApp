package com.lpavs.caliinda.feature.event_management.vm

import com.lpavs.caliinda.app.InsertEventRequest
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Поля от другого приложения — в шаблон формы создания. All-day в шаблоне, как и везде в
 * EventDto, — полночь в поясе пользователя с исключающим концом.
 */
internal fun InsertEventRequest.toTemplate(zone: ZoneId, now: Instant): EventDto {
  val (start, end) =
      if (isAllDay) {
        val startDate = beginMillis?.let { allDayDate(it, zone) } ?: now.atZone(zone).toLocalDate()
        val endDate =
            endMillis?.let { allDayDate(it, zone) }?.takeIf { it.isAfter(startDate) }
                ?: startDate.plusDays(1)
        startDate.atStartOfDay(zone).toInstant() to endDate.atStartOfDay(zone).toInstant()
      } else {
        // Как и у обычного создания: начало — ближайший ровный час.
        val begin =
            beginMillis?.let(Instant::ofEpochMilli)
                ?: now.atZone(zone).plusHours(1).truncatedTo(ChronoUnit.HOURS).toInstant()
        val finish =
            endMillis?.let(Instant::ofEpochMilli)?.takeIf { it.isAfter(begin) }
                ?: begin.plus(Duration.ofHours(1))
        begin to finish
      }
  return EventDto(
      id = "insert",
      summary = title.trim(),
      startTime = start,
      endTime = end,
      description = description.trim().takeIf { it.isNotEmpty() },
      location = location.trim().takeIf { it.isNotEmpty() },
      isAllDay = isAllDay)
}

// По контракту all-day — полночь UTC, но часть приложений шлёт местную полночь: в поясе восточнее
// UTC это вчерашний день по UTC. Ровная полночь UTC — дата по UTC, иначе — дата в поясе.
private fun allDayDate(millis: Long, zone: ZoneId): LocalDate {
  val instant = Instant.ofEpochMilli(millis)
  val utc = instant.atZone(ZoneOffset.UTC)
  return if (utc.toLocalTime() == LocalTime.MIDNIGHT) utc.toLocalDate()
  else instant.atZone(zone).toLocalDate()
}
