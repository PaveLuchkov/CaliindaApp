package com.lpavs.caliinda.feature.widget.date

import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayEmojiTest {
  private val ru = Locale.forLanguageTag("ru-RU")
  private val us = Locale.US
  private val moscow = ZoneId.of("Europe/Moscow")

  @Test
  fun easterDates() {
    assertEquals(LocalDate.of(2026, 4, 5), DayEmoji.westernEaster(2026))
    assertEquals(LocalDate.of(2027, 3, 28), DayEmoji.westernEaster(2027))
    assertEquals(LocalDate.of(2026, 4, 12), DayEmoji.orthodoxEaster(2026))
    assertEquals(LocalDate.of(2027, 5, 2), DayEmoji.orthodoxEaster(2027))
  }

  @Test
  fun easterDependsOnLanguage() {
    assertEquals("🐣", DayEmoji.holiday(LocalDate.of(2026, 4, 12), ru))
    assertEquals("🐣", DayEmoji.holiday(LocalDate.of(2026, 4, 5), us))
    assertNull(DayEmoji.holiday(LocalDate.of(2026, 4, 5), ru))
  }

  @Test
  fun localHolidays() {
    assertEquals("🚀", DayEmoji.holiday(LocalDate.of(2027, 4, 12), ru))
    assertNull(DayEmoji.holiday(LocalDate.of(2026, 4, 12), Locale.GERMANY))
    assertEquals("🎆", DayEmoji.holiday(LocalDate.of(2026, 7, 4), us))
    assertNull(DayEmoji.holiday(LocalDate.of(2026, 7, 4), Locale.UK))
    assertEquals("🎄", DayEmoji.holiday(LocalDate.of(2027, 1, 7), ru))
    assertEquals("🎃", DayEmoji.forDate(LocalDate.of(2026, 10, 31), us, moscow))
  }

  @Test
  fun seasonFlipsInSouth() {
    val july = LocalDate.of(2026, 7, 10)
    assertTrue(DayEmoji.season(july, ZoneId.of("Australia/Sydney")) in listOf("❄️", "⛄", "🧣"))
    assertTrue(DayEmoji.season(july, moscow) in listOf("☀️", "🍉", "🏖️"))
  }

  @Test
  fun sameDaySameEmoji() {
    val day = LocalDate.of(2026, 10, 7)
    assertEquals(DayEmoji.forDate(day, ru, moscow), DayEmoji.forDate(day, ru, moscow))
  }
}
