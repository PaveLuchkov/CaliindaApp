package com.lpavs.caliinda.feature.event_management.ui

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.repeatDate
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.repeatOn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RepeatOnTest {
  private val zone = ZoneId.of("Europe/Moscow")

  private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant()

  @Test
  fun keepsTimeAndDuration() {
    val gym = EventDto(id = "g", summary = "Зал", startTime = at("2026-10-05T19:00"), endTime = at("2026-10-05T20:30"))
    val state = gym.repeatOn(LocalDate.of(2026, 10, 9), zone)
    assertEquals(LocalDate.of(2026, 10, 9), state.startDate)
    assertEquals(LocalTime.of(19, 0), state.startTime)
    assertEquals(LocalDate.of(2026, 10, 9), state.endDate)
    assertEquals(LocalTime.of(20, 30), state.endTime)
  }

  @Test
  fun overnightAndMultiDayShiftWhole() {
    val party = EventDto(id = "p", summary = "Вечеринка", startTime = at("2026-10-03T22:00"), endTime = at("2026-10-04T02:00"))
    val state = party.repeatOn(LocalDate.of(2026, 10, 10), zone)
    assertEquals(LocalDate.of(2026, 10, 10), state.startDate)
    assertEquals(LocalDate.of(2026, 10, 11), state.endDate)
  }

  @Test
  fun allDayTripAndNoRecurrence() {
    // All-day хранится в UTC-полночь, конец эксклюзивный.
    val trip =
        EventDto(
            id = "t",
            summary = "Поездка",
            startTime = LocalDate.of(2026, 10, 1).atStartOfDay(ZoneOffset.UTC).toInstant(),
            endTime = LocalDate.of(2026, 10, 4).atStartOfDay(ZoneOffset.UTC).toInstant(),
            isAllDay = true,
            recurrenceRule = "FREQ=WEEKLY")
    val state = trip.repeatOn(LocalDate.of(2026, 11, 2), ZoneOffset.UTC)
    assertEquals(LocalDate.of(2026, 11, 2), state.startDate)
    assertEquals(LocalDate.of(2026, 11, 4), state.endDate)
    assertFalse(state.isRecurring)
    assertNull(state.recurrenceRule)
  }

  @Test
  fun repeatDatePastGoesToTodayOtherwiseNextDay() {
    val today = LocalDate.of(2026, 10, 8)
    fun on(day: String) = EventDto(id = day, summary = "x", startTime = at("${day}T19:00"), endTime = at("${day}T20:00"))
    assertEquals(today, on("2026-10-05").repeatDate(today, zone))
    assertEquals(LocalDate.of(2026, 10, 9), on("2026-10-08").repeatDate(today, zone))
    assertEquals(LocalDate.of(2026, 10, 13), on("2026-10-12").repeatDate(today, zone))
  }
}
