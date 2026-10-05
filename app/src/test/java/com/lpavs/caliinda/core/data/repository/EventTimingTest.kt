package com.lpavs.caliinda.core.data.repository

import com.lpavs.caliinda.core.data.calendar.model.EventDraft
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class EventTimingTest {
  private val moscow = ZoneId.of("Europe/Moscow")

  private fun draft(
      start: LocalDate,
      end: LocalDate = start,
      startTime: LocalTime? = LocalTime.of(10, 0),
      endTime: LocalTime? = LocalTime.of(11, 30),
      allDay: Boolean = false,
      rrule: String? = null,
  ) =
      EventDraft(
          summary = "x",
          description = null,
          location = null,
          isAllDay = allDay,
          startDate = start,
          startTime = startTime,
          endDate = end,
          endTime = endTime,
          zone = moscow,
          recurrenceRule = rrule)

  @Test
  fun `событие со временем пишется в поясе пользователя`() {
    val t = timingOf(draft(LocalDate.of(2026, 10, 5)))
    assertEquals(Instant.parse("2026-10-05T07:00:00Z").toEpochMilli(), t.dtStart)
    assertEquals(Instant.parse("2026-10-05T08:30:00Z").toEpochMilli(), t.dtEnd)
    assertEquals("Europe/Moscow", t.timeZone)
    assertEquals("P5400S", t.duration)
  }

  @Test
  fun `all-day — UTC-полночи и эксклюзивный конец`() {
    val t =
        timingOf(
            draft(
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 7),
                startTime = null,
                endTime = null,
                allDay = true))
    assertEquals(Instant.parse("2026-10-05T00:00:00Z").toEpochMilli(), t.dtStart)
    assertEquals(Instant.parse("2026-10-08T00:00:00Z").toEpochMilli(), t.dtEnd)
    assertEquals("UTC", t.timeZone)
    assertEquals("P3D", t.duration)
  }

  @Test
  fun `правка серии сдвигает начало серии на тот же сдвиг, что и экземпляр`() {
    // Серия с 1 октября, правим экземпляр 8 октября и переносим его на 10-е.
    val masterStart = Instant.parse("2026-10-01T07:00:00Z").toEpochMilli()
    val instance =
        EventDto(
            id = "1_x",
            summary = "x",
            startTime = Instant.parse("2026-10-08T07:00:00Z"),
            endTime = Instant.parse("2026-10-08T08:00:00Z"),
            recurrenceRule = "FREQ=WEEKLY")
    val edited =
        draft(
            LocalDate.of(2026, 10, 10),
            startTime = LocalTime.of(12, 0),
            endTime = LocalTime.of(13, 0),
            rrule = "FREQ=WEEKLY")

    val t = seriesTiming(instance, masterStart, masterAllDay = false, draft = edited)
    assertEquals(Instant.parse("2026-10-03T09:00:00Z").toEpochMilli(), t.dtStart)
    assertEquals(Instant.parse("2026-10-03T10:00:00Z").toEpochMilli(), t.dtEnd)
  }

  @Test
  fun `повторение убрали — событие встаёт туда, где его поставили в форме`() {
    val instance =
        EventDto(
            id = "1_x",
            summary = "x",
            startTime = Instant.parse("2026-10-08T07:00:00Z"),
            endTime = Instant.parse("2026-10-08T08:00:00Z"))
    val edited = draft(LocalDate.of(2026, 10, 10))
    val t = seriesTiming(instance, masterStart = 0L, masterAllDay = false, draft = edited)
    assertEquals(timingOf(edited), t)
  }
}
