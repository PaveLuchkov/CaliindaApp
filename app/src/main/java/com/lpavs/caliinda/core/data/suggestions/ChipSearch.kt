package com.lpavs.caliinda.core.data.suggestions

/**
 * Поиск чипа по набранному названию. Без семантики: у встроенных чипов есть ключевые слова
 * («Спортзал» — «тренировка, спорт, фитнес…»), сравниваем по общему началу слова. Так
 * «трен» и «тренировку» оба находят «тренировка» — склонения не мешают.
 */
class ChipTerms(
    /** Подпись и полное название — совпадение с ними весит больше. */
    val names: List<String>,
    val keywords: List<String>,
)

/**
 * Чипы, подходящие к [query], — вперёд (сильнее совпадение — раньше), остальные остаются
 * в прежнем порядке. Пустой запрос порядок не меняет.
 */
fun <T> rankByQuery(chips: List<T>, query: String, terms: (T) -> ChipTerms): List<T> {
  val tokens = words(query).filter { it.length >= MIN_TOKEN }
  if (tokens.isEmpty()) return chips
  return chips
      .withIndex()
      .sortedWith(
          compareByDescending<IndexedValue<T>> { score(tokens, terms(it.value)) }
              .thenBy { it.index })
      .map { it.value }
}

/**
 * Вес совпадения. Слово начинается со всего набранного («workou» → «workout») — сильное;
 * совпала только основа («тренировку» ↔ «тренировка», «workou» ↔ «working») — слабое.
 * Сильное по ключевому слову важнее слабого по названию.
 */
private fun score(tokens: List<String>, terms: ChipTerms): Int {
  val names = terms.names.flatMap(::words)
  val keywords = terms.keywords.flatMap(::words)
  return tokens.sumOf { token ->
    when {
      names.any { it.startsWith(token) } -> 4
      keywords.any { it.startsWith(token) } -> 3
      names.any { sameStem(token, it) } -> 2
      keywords.any { sameStem(token, it) } -> 1
      else -> 0
    }
  }
}

/** Общее начало не короче [STEM] символов (или всего короткого слова): разные окончания. */
private fun sameStem(token: String, word: String): Boolean =
    token.commonPrefixWith(word).length >= minOf(token.length, word.length, STEM).coerceAtLeast(MIN_TOKEN)

private fun words(text: String): List<String> =
    text.lowercase().replace('ё', 'е').split(SEPARATORS).filter { it.isNotEmpty() }

private val SEPARATORS = Regex("[\\s,.;:!?«»\"'()/-]+")
private const val MIN_TOKEN = 2
private const val STEM = 4
