package com.lpavs.caliinda.core.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectHeightTest {
  private fun days(d: Double) = (d * 24 * 60).toLong()

  @Test
  fun `месяц растянут от минимума до максимума`() {
    assertEquals(0f, projectHeightFraction(days(1.0)), 0.001f)
    assertEquals(1f, projectHeightFraction(days(31.0)), 0.001f)
    // За пределами месяца — упор, а не выход за границы.
    assertEquals(1f, projectHeightFraction(days(90.0)), 0.001f)
  }

  @Test
  fun `рост плавный и монотонный по дням`() {
    val fractions = (1..31).map { projectHeightFraction(days(it.toDouble())) }
    fractions.zipWithNext().forEach { (a, b) -> assertTrue(b > a) }
    // Никакого скачка: соседние дни отличаются не больше чем на 7 % высоты.
    assertTrue(fractions.zipWithNext().all { (a, b) -> b - a < 0.07f })
    // Неделя — примерно четверть, две — заметно больше половины.
    assertEquals(0.25f, projectHeightFraction(days(7.0)), 0.05f)
    assertEquals(0.7f, projectHeightFraction(days(14.0)), 0.05f)
  }
}
