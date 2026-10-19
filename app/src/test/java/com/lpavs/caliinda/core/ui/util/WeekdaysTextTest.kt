package com.lpavs.caliinda.core.ui.util

import com.lpavs.caliinda.core.data.suggestions.ALL_DAYS
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeekdaysTextTest {
  private val en = Locale.ENGLISH

  @Test
  fun `будни — диапазоном, пара — через запятую, все дни — без подписи`() {
    assertEquals("Mon–Fri", weekdaysText(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), en))
    assertEquals("Fri, Sat", weekdaysText(setOf(SATURDAY, FRIDAY), en))
    assertEquals("Mon, Wed–Fri", weekdaysText(setOf(MONDAY, WEDNESDAY, THURSDAY, FRIDAY), en))
    assertNull(weekdaysText(ALL_DAYS, en))
  }
}
