package com.lpavs.caliinda.feature.widget.countdown

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownTest {
  private val zone = ZoneId.of("Europe/Moscow")

  private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant()

  private fun event(start: String, end: String) =
      EventDto(id = "e", summary = "Отпуск", startTime = at(start), endTime = at(end))

  @Test
  fun countsCalendarDaysNotHours() {
    val trip = event("2026-10-08T09:00", "2026-10-08T10:00")
    assertEquals(Countdown.Ahead(trip, 1), countdownOf(trip, null, at("2026-10-07T23:00"), zone))
    assertEquals(Countdown.Ahead(trip, 8), countdownOf(trip, null, at("2026-09-30T08:00"), zone))
  }

  @Test
  fun todayOngoingAndGone() {
    val call = event("2026-10-07T18:00", "2026-10-07T19:00")
    assertEquals(Countdown.Today(call), countdownOf(call, null, at("2026-10-07T10:00"), zone))
    assertEquals(
        Countdown.Ongoing(call, 1, 1), countdownOf(call, null, at("2026-10-07T18:30"), zone))
    assertEquals(Countdown.Gone("Звонок"), countdownOf(call, "Звонок", at("2026-10-07T20:00"), zone))
  }

  @Test
  fun projectInProgress() {
    val trip = event("2026-10-05T00:00", "2026-10-12T00:00")
    assertEquals(
        Countdown.Ongoing(trip, 3, 7), countdownOf(trip, null, at("2026-10-07T12:00"), zone))
  }

  @Test
  fun missingEvent() {
    assertEquals(Countdown.NotSet, countdownOf(null, null, at("2026-10-07T12:00"), zone))
    assertEquals(Countdown.Gone("Отпуск"), countdownOf(null, "Отпуск", at("2026-10-07T12:00"), zone))
  }
}
