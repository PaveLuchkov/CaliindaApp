package com.lpavs.caliinda.core.data.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiSuggestionsTest {

  @Test
  fun `эмодзи достаются целиком — кожа, ZWJ, флаги, старые символы`() {
    assertEquals(listOf("🏋🏽‍♀️"), extractEmojis("🏋🏽‍♀️ Зал"))
    assertEquals(listOf("👨‍👩‍👧"), extractEmojis("Ужин 👨‍👩‍👧"))
    assertEquals(listOf("🇩🇪"), extractEmojis("Поездка 🇩🇪"))
    // Без VS16 и с ним — одно и то же.
    assertEquals(listOf("✈️", "✈️"), extractEmojis("✈ Рейс ✈️"))
    assertTrue(extractEmojis("Встреча в 10:00, без смайлов").isEmpty())
  }

  @Test
  fun `эмодзи в начале ставится или заменяется`() {
    assertEquals("🏋️ Зал", withLeadingEmoji("Зал", "🏋️"))
    assertEquals("🧘 Зал", withLeadingEmoji("🏋️ Зал", "🧘"))
    assertEquals("☕ ", withLeadingEmoji("", "☕"))
    assertEquals("🏋️", leadingEmoji("  🏋️ Зал"))
    assertNull(leadingEmoji("Зал 🏋️"))
  }

  @Test
  fun `подходит то, что ставили к похожим словам, — склонения не мешают`() {
    val index =
        EmojiIndex(
            listOf(
                PastTitle("🏋️ Зал", ageDays = 3),
                PastTitle("🏋️ Зал с тренером", ageDays = 10),
                PastTitle("🦷 Стоматолог", ageDays = 40),
                PastTitle("Без эмодзи", ageDays = 1)))
    assertEquals(listOf("🏋️"), index.suggest("зала").matching)
    assertEquals(listOf("🦷"), index.suggest("Стоматологу").matching)
    assertTrue(index.suggest("Кино").matching.isEmpty())
  }

  @Test
  fun `ваши — частые и свежие первыми, без истории — стандартный набор`() {
    val index =
        EmojiIndex(
            listOf(
                PastTitle("☕ Кофе", ageDays = 1),
                PastTitle("🎂 ДР", ageDays = 300),
                PastTitle("🎂 ДР мамы", ageDays = 330),
                PastTitle("☕ Кофе с Олей", ageDays = 2)))
    // Одиночный символ из старых блоков приходит с VS16 — так «☕» и «☕️» не двоятся.
    assertEquals(listOf("☕\uFE0F", "🎂"), index.suggest("").yours)
    assertEquals(DEFAULT_EMOJIS, EmojiIndex(emptyList()).suggest("").yours)
  }
}
