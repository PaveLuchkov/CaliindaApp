package com.lpavs.caliinda.core.data.suggestions

import java.time.LocalTime

/**
 * Чип-подсказка названия. Встроенные и свои живут в одном списке — пользователь правит его
 * целиком. У встроенного, пока его не трогали, названия null: они берутся из ресурсов и
 * переводятся вместе с языком.
 */
data class SuggestionChip(
    /** Встроенный — его ключ («breakfast»), свой — «custom_…». Им же считаются нажатия. */
    val id: String,
    val builtIn: Boolean = false,
    /** Подпись на чипе; null у нетронутого встроенного. */
    val shortName: String? = null,
    /** Что попадёт в название события; null у нетронутого встроенного. */
    val fullName: String? = null,
    /** Рекомендуемые часы: в них чип идёт первым. null — в любое время. */
    val window: HourWindow? = null,
)

/** Часы [from, to). Может переходить через полночь: 22:00–02:00. */
data class HourWindow(val from: LocalTime, val to: LocalTime) {
  operator fun contains(time: LocalTime): Boolean =
      if (from <= to) time >= from && time < to else time >= from || time < to
}

/** Встроенные чипы и их часы по умолчанию — в порядке показа без истории нажатий. */
object BuiltInChips {
  private fun w(from: Int, to: Int) = HourWindow(LocalTime.of(from, 0), LocalTime.of(to % 24, 0))

  val defaults: List<SuggestionChip> =
      listOf(
              "project" to w(11, 18),
              "work" to w(9, 18),
              "dinner" to w(18, 23),
              "meeting" to w(9, 18),
              "coffee" to null,
              "lunch" to w(11, 15),
              "shopping" to w(11, 18),
              "road" to null,
              "appointment" to w(11, 18),
              "travel" to null,
              "party" to w(19, 24),
              "movie" to w(19, 24),
              "study" to w(11, 18),
              "gym" to w(5, 11),
              "relax" to w(18, 23),
              "reading" to w(21, 24),
              "cleaning" to null,
              "cooking" to w(17, 20),
              "walking" to w(6, 12),
              "hobby" to w(17, 23),
              "date" to w(19, 24),
              "doctor" to w(6, 12),
              "birthday" to null,
              "presentation" to w(11, 18),
              "call" to w(11, 18),
              "errand" to w(11, 18),
              "sleep" to w(22, 2),
              "breakfast" to w(5, 11),
              "pet" to w(6, 12))
          .map { (id, window) -> SuggestionChip(id = id, builtIn = true, window = window) }
}

/**
 * Порядок чипов для времени начала события: сначала те, чьи часы его захватывают (свои —
 * впереди встроенных), потом остальные; внутри — по числу нажатий, при равенстве — как в списке.
 */
fun orderChips(
    chips: List<SuggestionChip>,
    clicks: Map<String, Int>,
    time: LocalTime,
): List<SuggestionChip> =
    chips
        .withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<SuggestionChip>> { it.value.window?.contains(time) == true }
                .thenByDescending { it.value.window?.contains(time) == true && !it.value.builtIn }
                .thenByDescending { clicks[it.value.id] ?: 0 }
                .thenBy { it.index })
        .map { it.value }
