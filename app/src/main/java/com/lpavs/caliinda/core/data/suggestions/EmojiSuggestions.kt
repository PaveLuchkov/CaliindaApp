package com.lpavs.caliinda.core.data.suggestions

import kotlin.math.exp

/** Название события из истории и когда оно было — свежие весят больше. */
data class PastTitle(val title: String, val ageDays: Long)

/** Что показать в панели эмодзи. */
data class EmojiSuggestions(
    /** Эмодзи, которые ставили к похожим названиям; пусто — похожих не было. */
    val matching: List<String>,
    /** Все эмодзи из истории — частые и недавние первыми; пусто — стандартный набор. */
    val yours: List<String>,
    /** false — в истории эмодзи нет и [yours] — стандартный набор. */
    val fromHistory: Boolean,
)

/**
 * Эмодзи из названий в календаре пользователя. Без словарей и сети: что человек сам ставил,
 * то и предлагаем. Индекс строится один раз на открытие формы, запрос — дёшево.
 */
class EmojiIndex(history: List<PastTitle>) {
  /** Эмодзи → вес: каждое употребление, затухающее с возрастом. */
  private val weights = mutableMapOf<String, Double>()

  /** Слово названия → (эмодзи → вес): «Зал» чаще всего с 🏋️. */
  private val byWord = mutableMapOf<String, MutableMap<String, Double>>()

  init {
    for (past in history) {
      val emojis = extractEmojis(past.title).distinct()
      if (emojis.isEmpty()) continue
      // Полураспад ~2 месяца: привычка годовой давности почти не мешает новой.
      val weight = exp(-past.ageDays / DECAY_DAYS)
      val words = words(stripEmojis(past.title))
      for (emoji in emojis) {
        weights.merge(emoji, weight, Double::plus)
        for (word in words) byWord.getOrPut(word) { mutableMapOf() }.merge(emoji, weight, Double::plus)
      }
    }
  }

  val isEmpty: Boolean
    get() = weights.isEmpty()

  fun suggest(title: String): EmojiSuggestions {
    val tokens = words(stripEmojis(title))
    val matching = mutableMapOf<String, Double>()
    for (token in tokens) {
      for ((word, emojis) in byWord) {
        if (!similar(token, word)) continue
        for ((emoji, w) in emojis) matching.merge(emoji, w, Double::plus)
      }
    }
    val yours = weights.entries.sortedByDescending { it.value }.map { it.key }.take(MAX_YOURS)
    return EmojiSuggestions(
        matching = matching.entries.sortedByDescending { it.value }.map { it.key }.take(MAX_MATCHING),
        yours = yours.ifEmpty { DEFAULT_EMOJIS },
        fromHistory = yours.isNotEmpty())
  }

  private companion object {
    const val DECAY_DAYS = 90.0
    const val MAX_MATCHING = 3
    const val MAX_YOURS = 24
  }
}

/** Набор на старт, пока в календаре нет своих эмодзи. */
val DEFAULT_EMOJIS =
    listOf(
        "💼", "📞", "🤝", "💻", "📚", "🎓", "🍳", "🍽️", "☕", "🛒", "🏋️", "🏃",
        "🧘", "🚶", "🩺", "🦷", "✈️", "🚗", "🎉", "🎂", "🎬", "🎵", "🧹", "❤️")

/**
 * Вставляет [emoji] на место курсора (или выделения) отдельным словом: пробел перед ним, если
 * слева текст, и после — курсор встаёт за пробел, можно сразу печатать дальше.
 * Возвращает новый текст и позицию курсора.
 */
fun insertEmoji(text: String, selStart: Int, selEnd: Int, emoji: String): Pair<String, Int> {
  val start = minOf(selStart, selEnd).coerceIn(0, text.length)
  val end = maxOf(selStart, selEnd).coerceIn(start, text.length)
  val before = if (start > 0 && !text[start - 1].isWhitespace()) " " else ""
  // Справа уже пробел — второй не добавляем, курсор просто переходит за него.
  val nextIsSpace = end < text.length && text[end].isWhitespace()
  val inserted = before + emoji + if (nextIsSpace) "" else " "
  val result = text.substring(0, start) + inserted + text.substring(end)
  return result to start + inserted.length + if (nextIsSpace) 1 else 0
}

/** Эмодзи в самом начале названия или null. */
fun leadingEmoji(title: String): String? =
    title.trimStart().let { t -> emojiAt(t, 0)?.let { end -> t.substring(0, end) } }

/** Все эмодзи в тексте — целиком, с модификаторами кожи, ZWJ-склейками и флагами. */
fun extractEmojis(text: String): List<String> = buildList {
  var i = 0
  while (i < text.length) {
    val end = emojiAt(text, i)
    if (end != null) {
      add(normalized(text.substring(i, end)))
      i = end
    } else {
      i += Character.charCount(text.codePointAt(i))
    }
  }
}

/**
 * «✈» и «✈️» для человека одно и то же, а без VS16 символ рисуется как текст — одиночному
 * символу из старых блоков добавляем VS16, как это делает клавиатура.
 */
private fun normalized(emoji: String): String {
  val cp = emoji.codePointAt(0)
  val single = Character.charCount(cp) == emoji.length
  return if (single && cp < 0x10000) emoji + String(Character.toChars(VS16)) else emoji
}

private fun stripEmojis(text: String): String {
  val sb = StringBuilder()
  var i = 0
  while (i < text.length) {
    val end = emojiAt(text, i)
    if (end != null) {
      sb.append(' ')
      i = end
    } else {
      val cp = text.codePointAt(i)
      sb.appendCodePoint(cp)
      i += Character.charCount(cp)
    }
  }
  return sb.toString()
}

/**
 * Конец эмодзи, начинающегося в [start], или null. Своя маленькая сегментация вместо ICU —
 * чтобы работала и в юнит-тестах на JVM: база + VS16/кожа/кейкап/теги, склейки через ZWJ,
 * пары региональных индикаторов (флаги).
 */
private fun emojiAt(text: String, start: Int): Int? {
  if (start >= text.length) return null
  val first = text.codePointAt(start)
  var i = start + Character.charCount(first)
  if (isRegionalIndicator(first)) {
    if (i < text.length && isRegionalIndicator(text.codePointAt(i))) {
      return i + Character.charCount(text.codePointAt(i))
    }
    return null
  }
  val base = isEmojiBase(first)
  // Текстовый символ (♥, ☀, цифра) — эмодзи только с VS16 или кейкапом после него.
  if (!base && !(i < text.length && text.codePointAt(i).let { it == VS16 || it == KEYCAP })) {
    return null
  }
  while (i < text.length) {
    val cp = text.codePointAt(i)
    when {
      cp == VS16 || cp == KEYCAP || isSkinTone(cp) || isTag(cp) -> i += Character.charCount(cp)
      cp == ZWJ && i + 1 < text.length -> {
        val next = text.codePointAt(i + 1)
        if (!isEmojiBase(next) && !isSymbol(next)) break
        i += 1 + Character.charCount(next)
      }
      else -> break
    }
  }
  return i
}

private fun isEmojiBase(cp: Int) =
    cp in 0x1F300..0x1FAFF || cp in 0x1F000..0x1F2FF || cp in 0x2600..0x27BF ||
        cp in 0x2B00..0x2BFF || cp in 0x2300..0x23FF

private fun isSymbol(cp: Int) = cp in 0x2000..0x2BFF

private fun isRegionalIndicator(cp: Int) = cp in 0x1F1E6..0x1F1FF

private fun isSkinTone(cp: Int) = cp in 0x1F3FB..0x1F3FF

private fun isTag(cp: Int) = cp in 0xE0020..0xE007F

private const val VS16 = 0xFE0F
private const val ZWJ = 0x200D
private const val KEYCAP = 0x20E3

private fun words(text: String): List<String> =
    text.lowercase().replace('ё', 'е').split(SEPARATORS).filter { it.length >= MIN_WORD }

/** Совпадение по началу слова — склонения не мешают: «зала» ~ «зал», «тренировку» ~ «тренировка». */
private fun similar(token: String, word: String): Boolean {
  if (word.startsWith(token) || token.startsWith(word)) return true
  return token.commonPrefixWith(word).length >= STEM
}

private val SEPARATORS = Regex("[\\s,.;:!?«»\"'()/-]+")
private const val MIN_WORD = 3
private const val STEM = 4
