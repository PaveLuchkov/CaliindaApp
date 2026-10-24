package com.lpavs.caliinda.core.data.utils

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsTest {
  private val zone = ZoneId.of("Europe/Moscow")
  private val now = Instant.parse("2026-10-24T09:00:00Z")

  @Test
  fun `событие со временем — в UTC, спецсимволы экранированы`() {
    val event =
        EventDto(
            id = "42_1761300000000",
            summary = "Обед; встреча, план",
            startTime = Instant.parse("2026-10-25T09:00:00Z"),
            endTime = Instant.parse("2026-10-25T10:30:00Z"),
            location = "Кафе \"Пушкин\"",
            description = "Взять ноутбук\nи зарядку")
    val ics = eventToIcs(event, zone, now)
    assertTrue(ics.contains("DTSTART:20261025T090000Z\r\n"))
    assertTrue(ics.contains("DTEND:20261025T103000Z\r\n"))
    assertTrue(ics.contains("SUMMARY:Обед\\; встреча\\, план\r\n"))
    assertTrue(ics.contains("DESCRIPTION:Взять ноутбук\\nи зарядку\r\n"))
    assertTrue(ics.contains("UID:42_1761300000000@caliinda\r\n"))
  }

  @Test
  fun `весь день — датами с исключающим концом`() {
    val start = Instant.parse("2026-10-24T21:00:00Z") // 25-е 00:00 в Москве
    val event =
        EventDto(
            id = "7",
            summary = "Поход",
            startTime = start,
            endTime = start.plusSeconds(2 * 86_400),
            isAllDay = true)
    val ics = eventToIcs(event, zone, now)
    assertTrue(ics.contains("DTSTART;VALUE=DATE:20261025\r\n"))
    assertTrue(ics.contains("DTEND;VALUE=DATE:20261027\r\n"))
  }

  @Test
  fun `длинные строки переносятся не длиннее 75 байт`() {
    val event =
        EventDto(
            id = "1",
            summary = "Ж".repeat(100),
            startTime = now,
            endTime = now.plusSeconds(3600))
    val lines = eventToIcs(event, zone, now).split("\r\n")
    assertTrue(lines.all { it.toByteArray().size <= 75 })
    val summary =
        lines.dropWhile { !it.startsWith("SUMMARY:") }.takeWhile { it.startsWith("SUMMARY:") || it.startsWith(" ") }
    assertEquals("SUMMARY:" + "Ж".repeat(100), summary.joinToString("") { it.removePrefix(" ") })
  }
}
