package com.lpavs.caliinda.core.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.TimeZone

class PickerDatesTest {

  @Test
  fun `дата не сдвигается ни в одном поясе`() {
    val original = TimeZone.getDefault()
    try {
      listOf("Europe/Moscow", "America/New_York", "Pacific/Kiritimati", "Pacific/Pago_Pago")
          .forEach { zone ->
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            val date = LocalDate.of(2026, 10, 4)
            assertEquals(zone, date, date.toPickerMillis().fromPickerMillis())
          }
    } finally {
      TimeZone.setDefault(original)
    }
  }

  @Test
  fun `millis соответствуют полуночи UTC, как ждёт DatePicker`() {
    assertEquals(1_791_072_000_000L, LocalDate.of(2026, 10, 4).toPickerMillis())
  }
}
