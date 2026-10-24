package com.lpavs.caliinda.core.data.utils

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarLinkTest {
  private val zone = ZoneId.of("Europe/Moscow")

  @Test
  fun `событие со временем — UTC, пробелы как %20`() {
    val event =
        EventDto(
            id = "1",
            summary = "Lunch & talk",
            startTime = Instant.parse("2026-10-25T09:00:00Z"),
            endTime = Instant.parse("2026-10-25T10:30:00Z"),
            location = "Cafe 26")
    assertEquals(
        "https://calendar.google.com/calendar/render?action=TEMPLATE&text=Lunch%20%26%20talk" +
            "&dates=20261025T090000Z/20261025T103000Z&location=Cafe%2026",
        googleCalendarLink(event, zone))
    assertEquals(
        "https://calendar.google.com/calendar/render?action=TEMPLATE&text=Lunch%20%26%20talk" +
            "&dates=20261025T090000Z/20261025T103000Z",
        googleCalendarLink(event, zone, compact = true))
  }

  @Test
  fun `весь день — даты, без названия и места не пишем`() {
    val start = Instant.parse("2026-10-24T21:00:00Z")
    val event =
        EventDto(id = "2", summary = " ", startTime = start, endTime = start.plusSeconds(86_400), isAllDay = true)
    assertEquals(
        "https://calendar.google.com/calendar/render?action=TEMPLATE&dates=20261025/20261026",
        googleCalendarLink(event, zone))
  }
}
