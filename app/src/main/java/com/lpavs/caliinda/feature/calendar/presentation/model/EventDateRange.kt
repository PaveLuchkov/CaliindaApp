package com.lpavs.caliinda.feature.calendar.presentation.model

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import java.time.LocalDate
import java.time.ZoneId

/** Первый и последний день события в поясе пользователя (конец события эксклюзивный). */
fun EventDto.dateRange(zone: ZoneId): ClosedRange<LocalDate> {
  val first = startTime.atZone(zone).toLocalDate()
  val last = endTime.minusNanos(1).atZone(zone).toLocalDate()
  return first..maxOf(first, last)
}
