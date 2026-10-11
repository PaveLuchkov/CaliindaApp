package com.lpavs.caliinda.core.data.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY
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

    assertEquals(listOf(mine, lunch, gym), orderChips(listOf(gym, lunch, mine), clicks, MONDAY, t(14)))
    // Вне часов — просто по нажатиям.
    assertEquals(listOf(gym, lunch, mine), orderChips(listOf(lunch, mine, gym), clicks, MONDAY, t(9)))
  }

  @Test
  fun `при равенстве сохраняется порядок списка`() {
    val a = SuggestionChip("a", builtIn = true)
    val b = SuggestionChip("b", builtIn = true)
    assertEquals(listOf(b, a), orderChips(listOf(b, a), emptyMap(), MONDAY, t(10)))
  }

  @Test
  fun `в будни вперёд работа, в воскресенье она уходит в конец`() {
    val work = SuggestionChip("work", builtIn = true, days = BuiltInChips.defaultDays("work"))
    val cleaning =
        SuggestionChip("cleaning", builtIn = true, days = BuiltInChips.defaultDays("cleaning"))
    val coffee = SuggestionChip("coffee", builtIn = true)
    val clicks = mapOf("work" to 1, "cleaning" to 1, "coffee" to 9)
    val chips = listOf(coffee, work, cleaning)

    assertEquals(listOf(work, coffee, cleaning), orderChips(chips, clicks, WEDNESDAY, t(10)))
    assertEquals(listOf(cleaning, coffee, work), orderChips(chips, clicks, SUNDAY, t(10)))
  }

  @Test
  fun `уместен, только если совпали и день, и часы`() {
    val party =
        SuggestionChip("party", builtIn = true, window = HourWindow(t(19), t(0)),
            days = setOf(FRIDAY, SATURDAY))
    assertEquals(ChipFit.MATCH, party.fit(FRIDAY, t(21)))
    assertEquals(ChipFit.NEUTRAL, party.fit(FRIDAY, t(10)))
    assertEquals(ChipFit.OFF_DAY, party.fit(MONDAY, t(21)))
    assertEquals(ChipFit.NEUTRAL, SuggestionChip("coffee", builtIn = true).fit(MONDAY, t(9)))
  }

  @Test
  fun `дни подряд склеиваются`() {
    assertEquals(
        listOf(listOf(MONDAY), listOf(WEDNESDAY, java.time.DayOfWeek.THURSDAY, FRIDAY)),
        dayRuns(setOf(FRIDAY, MONDAY, WEDNESDAY, java.time.DayOfWeek.THURSDAY)))
    assertEquals(listOf(listOf(SATURDAY, SUNDAY)), dayRuns(setOf(SUNDAY, SATURDAY)))
  }

  @Test
  fun `список из старой версии получает дни по смыслу`() {
    val json = """[{"id":"work","builtIn":true},{"id":"custom_1","short":"Йога"}]"""
    val chips = SuggestionChipsCodec.decode(json)!!
    assertEquals(BuiltInChips.defaultDays("work"), chips[0].days)
    assertEquals(ALL_DAYS, chips[1].days)
  }

  @Test
  fun `кодек туда и обратно`() {
    val chips =
        listOf(
            SuggestionChip("breakfast", builtIn = true, window = HourWindow(t(5), t(11))),
            SuggestionChip("custom_1", shortName = "Йога", fullName = "🧘 Йога \"утро\"",
                days = setOf(SATURDAY, SUNDAY)),
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
