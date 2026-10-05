package com.lpavs.caliinda.core.data.repository

import com.lpavs.caliinda.core.data.calendar.model.EventDraft
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/** Время события в том виде, в каком его пишем в CalendarContract.Events. */
internal data class EventTiming(
    val dtStart: Long,
    val dtEnd: Long,
    val timeZone: String,
    val isAllDay: Boolean
) {
  /** DURATION для повторяющихся событий: провайдер требует его вместо DTEND. */
  val duration: String
    get() {
      val millis = (dtEnd - dtStart).coerceAtLeast(0)
      return if (isAllDay) "P${TimeUnit.MILLISECONDS.toDays(millis).coerceAtLeast(1)}D"
      else "P${TimeUnit.MILLISECONDS.toSeconds(millis)}S"
    }
}

/** All-day события провайдер хранит в UTC-полночи, конец эксклюзивный. */
internal fun timingOf(
    draft: EventDraft,
    startDate: LocalDate = draft.startDate,
    endDate: LocalDate = draft.endDate
): EventTiming {
  if (draft.isAllDay) {
    return EventTiming(
        dtStart = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        dtEnd = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        timeZone = "UTC",
        isAllDay = true)
  }
  val startTime = requireNotNull(draft.startTime) { "Timed event without start time" }
  val endTime = requireNotNull(draft.endTime) { "Timed event without end time" }
  return EventTiming(
      dtStart = startDate.atTime(startTime).atZone(draft.zone).toInstant().toEpochMilli(),
      dtEnd = endDate.atTime(endTime).atZone(draft.zone).toInstant().toEpochMilli(),
      timeZone = draft.zone.id,
      isAllDay = false)
}

/**
 * Время для правки всей серии: сдвигаем дату начала серии на столько же дней, на сколько
 * пользователь сдвинул редактируемый экземпляр, а время/длительность берём из формы.
 */
internal fun seriesTiming(
    event: EventDto,
    masterStart: Long,
    masterAllDay: Boolean,
    draft: EventDraft,
): EventTiming {
  // Повторение убрали — событие становится одиночным там, где его поставили в форме.
  if (draft.recurrenceRule == null) return timingOf(draft)

  val zone = draft.zone
  val span = ChronoUnit.DAYS.between(draft.startDate, draft.endDate)
  val instanceDate = (event.originalStartTime ?: event.startTime).atZone(zone).toLocalDate()
  val masterDate =
      if (masterAllDay) Instant.ofEpochMilli(masterStart).atZone(ZoneOffset.UTC).toLocalDate()
      else Instant.ofEpochMilli(masterStart).atZone(zone).toLocalDate()
  val newStart = masterDate.plusDays(ChronoUnit.DAYS.between(instanceDate, draft.startDate))
  return timingOf(draft, newStart, newStart.plusDays(span))
}
