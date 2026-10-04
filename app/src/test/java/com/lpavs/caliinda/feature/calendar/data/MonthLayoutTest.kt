package com.lpavs.caliinda.feature.calendar.data

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MonthLayoutTest {
  private val zone = ZoneId.of("Europe/Moscow")
  private val october = YearMonth.of(2026, 10)

  /** All-day проект: конец эксклюзивный, как отдаёт репозиторий. */
  private fun project(id: String, first: LocalDate, last: LocalDate) =
      EventDto(
          id = id,
          summary = id,
          startTime = first.atStartOfDay(zone).toInstant(),
          endTime = last.plusDays(1).atStartOfDay(zone).toInstant(),
          isAllDay = true)

  private fun layout(vararg projects: EventDto, today: LocalDate = LocalDate.of(2026, 10, 15)) =
      layoutMonth(october, projects.toList(), emptyMap(), zone, today)

  @Test
  fun `непересекающиеся проекты делят одну колонку`() {
    val result =
        layout(
            project("a", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)),
            project("b", LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 9)))
    assertEquals(1, result.laneCount)
    assertEquals(listOf(0, 0), result.bars.map { it.lane })
  }

  @Test
  fun `пересекающиеся расходятся по колонкам, освободившаяся используется снова`() {
    val result =
        layout(
            project("long", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20)),
            project("mid", LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 8)),
            project("late", LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12)))
    assertEquals(2, result.laneCount)
    assertEquals(mapOf("long" to 0, "mid" to 1, "late" to 1), result.bars.associate { it.event.id to it.lane })
  }

  @Test
  fun `проект через границы месяца обрезается и помечается`() {
    val bar =
        layout(project("trip", LocalDate.of(2026, 9, 25), LocalDate.of(2026, 11, 3))).bars.single()
    assertEquals(0, bar.startDay)
    assertEquals(31, bar.endDayExclusive)
    assertTrue(bar.continuesBefore)
    assertTrue(bar.continuesAfter)
  }

  @Test
  fun `индексы дней и признаки прошлого и текущего`() {
    val result =
        layout(
            project("past", LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 4)),
            project("now", LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 16)))
    val past = result.bars.first { it.event.id == "past" }
    val now = result.bars.first { it.event.id == "now" }
    assertEquals(1, past.startDay)
    assertEquals(4, past.endDayExclusive)
    assertTrue(past.isPast)
    assertFalse(past.isCurrent)
    assertTrue(now.isCurrent)
    assertFalse(now.isPast)
  }

  @Test
  fun `проекты вне месяца не попадают`() {
    assertEquals(0, layout(project("sept", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))).bars.size)
  }
}
