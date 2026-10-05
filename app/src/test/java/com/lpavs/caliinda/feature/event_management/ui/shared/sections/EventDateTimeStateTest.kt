package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class EventDateTimeStateTest {
  private val moscow = ZoneId.of("Europe/Moscow")
  private val day = LocalDate.of(2026, 10, 5)

  private fun timed(start: LocalTime? = LocalTime.of(10, 0), end: LocalTime? = LocalTime.of(11, 0)) =
      EventDateTimeState(
          startDate = day,
          startTime = start,
          endDate = day,
          endTime = end,
          isAllDay = false,
          isRecurring = false)

  private fun event(rrule: String?, allDay: Boolean = false) =
      EventDto(
          id = "1_x",
          summary = "x",
          startTime = Instant.parse("2026-10-05T07:00:00Z"),
          endTime = Instant.parse("2026-10-05T08:00:00Z"),
          isAllDay = allDay,
          recurrenceRule = rrule)

  // --- Проверка ---

  @Test
  fun `корректное время — без ошибки`() {
    assertNull(timed().validationError)
  }

  @Test
  fun `ошибки дат и времени`() {
    assertEquals(
        R.string.error_end_time_not_after_start, timed(end = LocalTime.of(10, 0)).validationError)
    assertEquals(R.string.error_start_time_missing, timed(start = null).validationError)
    assertEquals(R.string.error_end_time_missing, timed(end = null).validationError)
    assertEquals(
        R.string.error_end_date_before_start,
        timed().copy(endDate = day.minusDays(1)).validationError)
  }

  @Test
  fun `all-day без времени — без ошибки`() {
    assertNull(timed(null, null).copy(isAllDay = true).validationError)
  }

  // --- Форма → RRULE ---

  @Test
  fun `еженедельно по дням до даты`() {
    val state =
        timed()
            .copy(
                isRecurring = true,
                recurrenceRule = RecurrenceOption.Weekly.rruleValue,
                selectedWeekdays = setOf(FRIDAY, MONDAY),
                recurrenceEndType = RecurrenceEndType.DATE,
                recurrenceEndDate = LocalDate.of(2026, 10, 31))
    // Конец дня 31-го по Москве — 20:59:59 UTC.
    assertEquals("FREQ=WEEKLY;BYDAY=MO,FR;UNTIL=20261031T205959Z", state.toRrule(moscow)?.format())
  }

  @Test
  fun `дни недели учитываются только у еженедельного`() {
    val state =
        timed()
            .copy(
                recurrenceRule = RecurrenceOption.Daily.rruleValue,
                selectedWeekdays = setOf(MONDAY),
                recurrenceEndType = RecurrenceEndType.COUNT,
                recurrenceCount = 3)
    assertEquals("FREQ=DAILY;COUNT=3", state.toRrule(moscow)?.format())
  }

  @Test
  fun `без повторения — null`() {
    assertNull(timed().toRrule(moscow))
  }

  // --- Событие → форма ---

  @Test
  fun `all-day — последний день включительно`() {
    val allDay =
        event(null, allDay = true)
            .copy(
                startTime = LocalDate.of(2026, 10, 5).atStartOfDay(moscow).toInstant(),
                endTime = LocalDate.of(2026, 10, 8).atStartOfDay(moscow).toInstant())
    val state = allDay.toDateTimeState(moscow)
    assertEquals(LocalDate.of(2026, 10, 7), state.endDate)
    assertNull(state.startTime)
  }

  @Test
  fun `повторение разбирается в поля формы`() {
    val state = event("FREQ=WEEKLY;BYDAY=MO;UNTIL=20261031T205959Z").toDateTimeState(moscow)
    assertEquals(LocalTime.of(10, 0), state.startTime)
    assertEquals(RecurrenceOption.Weekly.rruleValue, state.recurrenceRule)
    assertEquals(setOf(MONDAY), state.selectedWeekdays)
    assertEquals(RecurrenceEndType.DATE, state.recurrenceEndType)
    assertEquals(LocalDate.of(2026, 10, 31), state.recurrenceEndDate)
  }

  // --- Что сохранить после правки ---

  @Test
  fun `повторение не трогали — исходная строка как есть`() {
    val original = event("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO;WKST=SU")
    val form = original.toDateTimeState(moscow)
    assertEquals(original.recurrenceRule, recurrenceRuleAfterEdit(original, form, moscow))
  }

  @Test
  fun `сменили концовку — INTERVAL остаётся`() {
    val original = event("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO")
    val form =
        original
            .toDateTimeState(moscow)
            .copy(recurrenceEndType = RecurrenceEndType.COUNT, recurrenceCount = 4)
    assertEquals(
        "FREQ=WEEKLY;BYDAY=MO;INTERVAL=2;COUNT=4", recurrenceRuleAfterEdit(original, form, moscow))
  }

  @Test
  fun `сменили частоту — части старого правила не переносятся`() {
    val original = event("FREQ=MONTHLY;BYMONTHDAY=5")
    val form =
        original.toDateTimeState(moscow).copy(recurrenceRule = RecurrenceOption.Daily.rruleValue)
    assertEquals("FREQ=DAILY", recurrenceRuleAfterEdit(original, form, moscow))
  }

  @Test
  fun `повторение выключили — null`() {
    val original = event("FREQ=DAILY")
    val form = original.toDateTimeState(moscow).copy(isRecurring = false, recurrenceRule = null)
    assertNull(recurrenceRuleAfterEdit(original, form, moscow))
  }
}
