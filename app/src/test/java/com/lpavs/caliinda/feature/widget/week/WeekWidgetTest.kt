package com.lpavs.caliinda.feature.widget.week

import org.junit.Assert.assertEquals
import org.junit.Test

class WeekWidgetTest {
  @Test
  fun busyDotsScale() {
    assertEquals(listOf(0, 1, 1, 2, 2, 3, 3), listOf(0, 1, 2, 3, 4, 5, 12).map(::busyDots))
  }
}
