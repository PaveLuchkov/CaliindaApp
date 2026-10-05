package com.lpavs.caliinda.core.data.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class SuggestionChipsTest {
  private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)

  @Test
  fun `окно часов — в том числе через полночь`() {
    val day = HourWindow(t(13), t(15))
    assertTrue(t(13) in day)
    assertTrue(t(14, 59) in day)
    assertFalse(t(15) in day)
    val night = HourWindow(t(22), t(2))
    assertTrue(t(23) in night)
    assertTrue(t(1) in night)
    assertFalse(t(12) in night)
  }

  @Test
  fun `свой чип в своих часах — первым, даже против частых встроенных`() {
    val lunch = SuggestionChip("lunch", builtIn = true, window = HourWindow(t(11), t(15)))
    val gym = SuggestionChip("gym", builtIn = true)
    val mine = SuggestionChip("custom_1", shortName = "Йога", fullName = "🧘 Йога",
        window = HourWindow(t(13), t(15)))
    val clicks = mapOf("gym" to 50, "lunch" to 3)

    assertEquals(listOf(mine, lunch, gym), orderChips(listOf(gym, lunch, mine), clicks, t(14)))
    // Вне часов — просто по нажатиям.
    assertEquals(listOf(gym, lunch, mine), orderChips(listOf(lunch, mine, gym), clicks, t(9)))
  }

  @Test
  fun `при равенстве сохраняется порядок списка`() {
    val a = SuggestionChip("a", builtIn = true)
    val b = SuggestionChip("b", builtIn = true)
    assertEquals(listOf(b, a), orderChips(listOf(b, a), emptyMap(), t(10)))
  }

  @Test
  fun `кодек туда и обратно`() {
    val chips =
        listOf(
            SuggestionChip("breakfast", builtIn = true, window = HourWindow(t(5), t(11))),
            SuggestionChip("custom_1", shortName = "Йога", fullName = "🧘 Йога \"утро\""),
            SuggestionChip("work", builtIn = true, shortName = "Офис", fullName = "Офис"))
    assertEquals(chips, SuggestionChipsCodec.decode(SuggestionChipsCodec.encode(chips)))
  }

  @Test
  fun `битая запись пропускается, мусор — null`() {
    val json = """[{"id":"ok","builtIn":true},{"builtIn":true},{"id":"x","from":"25:00","to":"1"}]"""
    assertEquals(listOf(SuggestionChip("ok", builtIn = true)), SuggestionChipsCodec.decode(json))
    assertEquals(null, SuggestionChipsCodec.decode("не json"))
  }

  @Test
  fun `у встроенных уникальные id`() {
    val ids = BuiltInChips.defaults.map { it.id }
    assertEquals(ids.size, ids.toSet().size)
  }
}
