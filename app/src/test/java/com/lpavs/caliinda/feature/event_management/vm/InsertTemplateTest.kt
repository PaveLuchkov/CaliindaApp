package com.lpavs.caliinda.feature.event_management.vm

import com.lpavs.caliinda.app.InsertEventRequest
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsertTemplateTest {
  private val zone = ZoneId.of("Europe/Moscow")
  private val now = Instant.parse("2026-10-24T09:20:00Z") // 12:20 в Москве

  @Test
  fun `время передали — берём как есть`() {
    val begin = Instant.parse("2026-10-30T15:00:00Z")
    val template =
        InsertEventRequest(
                title = " Концерт ",
                location = "Клуб",
                beginMillis = begin.toEpochMilli(),
                endMillis = begin.plusSeconds(7200).toEpochMilli())
            .toTemplate(zone, now)
    assertEquals("Концерт", template.summary)
    assertEquals("Клуб", template.location)
    assertEquals(begin, template.startTime)
    assertEquals(begin.plusSeconds(7200), template.endTime)
  }

  @Test
  fun `без времени — ближайший ровный час на час`() {
    val template = InsertEventRequest(title = "x").toTemplate(zone, now)
    assertEquals(Instant.parse("2026-10-24T10:00:00Z"), template.startTime)
    assertEquals(Instant.parse("2026-10-24T11:00:00Z"), template.endTime)
    assertTrue(template.description == null)
  }

  @Test
  fun `весь день — полночь UTC и местная полночь дают один день`() {
    val utcMidnight = Instant.parse("2026-10-30T00:00:00Z").toEpochMilli()
    val localMidnight = Instant.parse("2026-10-29T21:00:00Z").toEpochMilli()
    for (begin in listOf(utcMidnight, localMidnight)) {
      val template = InsertEventRequest(beginMillis = begin, isAllDay = true).toTemplate(zone, now)
      assertEquals(Instant.parse("2026-10-29T21:00:00Z"), template.startTime)
      assertEquals(Instant.parse("2026-10-30T21:00:00Z"), template.endTime)
      assertTrue(template.isUntitled)
    }
  }

  @Test
  fun `конец раньше начала — час по умолчанию`() {
    val begin = Instant.parse("2026-10-30T15:00:00Z").toEpochMilli()
    val template =
        InsertEventRequest(beginMillis = begin, endMillis = begin - 1).toTemplate(zone, now)
    assertEquals(Instant.ofEpochMilli(begin).plusSeconds(3600), template.endTime)
  }
}
