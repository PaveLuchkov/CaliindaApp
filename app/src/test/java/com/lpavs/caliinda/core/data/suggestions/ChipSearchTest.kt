package com.lpavs.caliinda.core.data.suggestions

import org.junit.Assert.assertEquals
import org.junit.Test

class ChipSearchTest {
  private val chips =
      mapOf(
          "Ужин" to ChipTerms(listOf("Ужин", "Ужин"), listOf("ресторан", "поесть")),
          "Спортзал" to ChipTerms(listOf("Спортзал", "Спортзал"), listOf("трениров", "фитнес", "бег")),
          "Вечеринка" to ChipTerms(listOf("Вечеринка", "Вечеринка"), listOf("праздник", "друз")),
          "Кофе" to ChipTerms(listOf("Кофе", "Кофе"), listOf("кофейн", "кафе")),
      )

  private fun rank(query: String) = rankByQuery(chips.keys.toList(), query) { chips.getValue(it) }

  @Test
  fun `пустой запрос не меняет порядок`() {
    assertEquals(chips.keys.toList(), rank(""))
    assertEquals(chips.keys.toList(), rank("  "))
  }

  @Test
  fun `первые буквы названия`() {
    assertEquals("Спортзал", rank("спо").first())
    assertEquals("Вечеринка", rank("Веч").first())
  }

  @Test
  fun `ключевое слово с другим окончанием`() {
    assertEquals("Спортзал", rank("тренировку").first())
    assertEquals("Спортзал", rank("Утренняя трен").first())
    assertEquals("Кофе", rank("кофейня").first())
  }

  @Test
  fun `совпадение с названием важнее ключевого слова`() {
    // «Ужин» — по названию, «Вечеринка» — только по слову «друзья».
    assertEquals(listOf("Ужин", "Вечеринка"), rank("ужин с друзьями").take(2))
  }

  @Test
  fun `неподходящие остаются в прежнем порядке после подходящих`() {
    assertEquals(listOf("Кофе", "Ужин", "Спортзал", "Вечеринка"), rank("кафе"))
  }

  @Test
  fun `ё и е не различаются, короткие слова не мешают`() {
    val terms = mapOf("Учёба" to ChipTerms(listOf("Учёба"), emptyList()), "Сон" to ChipTerms(listOf("Сон"), emptyList()))
    assertEquals("Учёба", rankByQuery(terms.keys.toList(), "в учебу") { terms.getValue(it) }.first())
  }

  @Test
  fun `полное начало ключевого слова важнее похожей основы в названии`() {
    val terms =
        mapOf(
            "Work" to ChipTerms(listOf("Work", "Working"), listOf("office")),
            "Gym" to ChipTerms(listOf("Gym", "Gym"), listOf("workout", "training")))
    assertEquals("Gym", rankByQuery(terms.keys.toList(), "workou") { terms.getValue(it) }.first())
  }
}
