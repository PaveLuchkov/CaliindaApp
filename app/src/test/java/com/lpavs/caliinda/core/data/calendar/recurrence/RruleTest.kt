package com.lpavs.caliinda.core.data.calendar.recurrence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RruleTest {

  @Test
  fun `разбирает то, что умеет форма`() {
    val rule = Rrule.parse("FREQ=WEEKLY;BYDAY=WE,MO;COUNT=5")
    assertEquals("WEEKLY", rule.freq)
    assertEquals(listOf(WEDNESDAY, MONDAY), rule.byDay)
    assertEquals(5, rule.count)
    assertEquals(emptyList<String>(), rule.other)
  }

  @Test
  fun `префикс RRULE и регистр не мешают`() {
    assertEquals("DAILY", Rrule.parse("RRULE:freq=daily").freq)
  }

  @Test
  fun `незнакомые части сохраняются при форматировании`() {
    val rule = Rrule.parse("FREQ=WEEKLY;INTERVAL=2;WKST=SU;BYDAY=MO")
    assertEquals(listOf("INTERVAL=2", "WKST=SU"), rule.other)
    assertEquals("FREQ=WEEKLY;BYDAY=MO;INTERVAL=2;WKST=SU", rule.format())
  }

  @Test
  fun `BYDAY с номером недели уходит в other целиком`() {
    val rule = Rrule.parse("FREQ=MONTHLY;BYDAY=1MO,-1FR")
    assertEquals(emptyList<Any>(), rule.byDay)
    assertEquals(listOf("BYDAY=1MO,-1FR"), rule.other)
  }

  @Test
  fun `кривая часть без знака равенства не роняет разбор`() {
    val rule = Rrule.parse("FREQ=DAILY;;GARBAGE")
    assertEquals("DAILY", rule.freq)
    assertEquals(listOf("GARBAGE"), rule.other)
  }

  @Test
  fun `UNTIL датой и UNTIL временем в UTC`() {
    val byDate = Rrule.parse("FREQ=DAILY;UNTIL=20261031")
    assertEquals(Rrule.Until.Date(LocalDate.of(2026, 10, 31)), byDate.until)

    val byTime = Rrule.parse("FREQ=DAILY;UNTIL=20261031T205959Z")
    assertEquals(Rrule.Until.DateTime(Instant.parse("2026-10-31T20:59:59Z")), byTime.until)
    assertEquals("FREQ=DAILY;UNTIL=20261031T205959Z", byTime.format())
  }

  @Test
  fun `последний день серии считается в поясе пользователя`() {
    val rule = Rrule.parse("FREQ=DAILY;UNTIL=20261031T220000Z")
    assertEquals(LocalDate.of(2026, 11, 1), rule.untilDate(ZoneId.of("Europe/Moscow")))
    assertEquals(LocalDate.of(2026, 10, 31), rule.untilDate(ZoneId.of("America/New_York")))
    assertNull(Rrule.parse("FREQ=DAILY").untilDate(ZoneId.of("UTC")))
  }

  @Test
  fun `обрезка серии со временем — за секунду до экземпляра, COUNT убирается`() {
    val instance = Instant.parse("2026-10-10T07:00:00Z").toEpochMilli()
    val cut = Rrule.parse("FREQ=DAILY;COUNT=30;INTERVAL=2").endingBefore(instance, allDay = false)
    assertEquals("FREQ=DAILY;INTERVAL=2;UNTIL=20261010T065959Z", cut.format())
  }

  @Test
  fun `обрезка all-day серии — датой накануне`() {
    val instance = Instant.parse("2026-10-10T00:00:00Z").toEpochMilli()
    val cut = Rrule.parse("FREQ=WEEKLY;BYDAY=FR").endingBefore(instance, allDay = true)
    assertEquals(listOf(FRIDAY), cut.byDay)
    assertEquals("FREQ=WEEKLY;BYDAY=FR;UNTIL=20261009", cut.format())
  }

  @Test
  fun `EXDATE в формате провайдера`() {
    val begin = Instant.parse("2026-10-10T07:30:00Z").toEpochMilli()
    assertEquals("20261010T073000Z", Rrule.exdateValue(begin, allDay = false))
    val allDayBegin = Instant.parse("2026-10-10T00:00:00Z").toEpochMilli()
    assertEquals("20261010", Rrule.exdateValue(allDayBegin, allDay = true))
  }

  @Test
  fun `EXDATE обратно в миллисекунды`() {
    val begin = Instant.parse("2026-10-10T07:30:00Z").toEpochMilli()
    assertEquals(begin, Rrule.exdateMillis(Rrule.exdateValue(begin, allDay = false)))
    assertEquals(
        Instant.parse("2026-10-10T00:00:00Z").toEpochMilli(), Rrule.exdateMillis("20261010"))
    assertEquals(begin, Rrule.exdateMillis("TZID=Europe/Moscow:20261010T073000"))
    assertEquals(Long.MAX_VALUE, Rrule.exdateMillis("мусор"))
  }
}
