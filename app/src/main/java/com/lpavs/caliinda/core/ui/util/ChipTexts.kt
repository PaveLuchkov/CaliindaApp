package com.lpavs.caliinda.core.ui.util

import com.lpavs.caliinda.core.data.suggestions.ChipTerms
import android.content.Context
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.suggestions.SuggestionChip
import com.lpavs.caliinda.core.data.suggestions.ALL_DAYS
import com.lpavs.caliinda.core.data.suggestions.dayRuns
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Тексты встроенных чипов: id → (подпись, полное название). */
private val BUILT_IN_TEXTS: Map<String, Pair<Int, Int>> =
    mapOf(
        "project" to (R.string.suggested_event_project to R.string.suggested_event_project_full),
        "work" to (R.string.suggested_event_work to R.string.suggested_event_work_full),
        "dinner" to (R.string.suggested_event_dinner to R.string.suggested_event_dinner_full),
        "meeting" to (R.string.suggested_event_meeting to R.string.suggested_event_meeting_full),
        "coffee" to (R.string.suggested_event_coffee to R.string.suggested_event_coffee_full),
        "lunch" to (R.string.suggested_event_lunch to R.string.suggested_event_lunch_full),
        "shopping" to (R.string.suggested_event_shopping to R.string.suggested_event_shopping_full),
        "road" to (R.string.suggested_event_road to R.string.suggested_event_road_full),
        "appointment" to (R.string.suggested_event_appointment to R.string.suggested_event_appointment_full),
        "travel" to (R.string.suggested_event_travel to R.string.suggested_event_travel_full),
        "party" to (R.string.suggested_event_party to R.string.suggested_event_party_full),
        "movie" to (R.string.suggested_event_movie to R.string.suggested_event_movie_full),
        "study" to (R.string.suggested_event_study to R.string.suggested_event_study_full),
        "gym" to (R.string.suggested_event_gym to R.string.suggested_event_gym_full),
        "relax" to (R.string.suggested_event_relax to R.string.suggested_event_relax_full),
        "reading" to (R.string.suggested_event_reading to R.string.suggested_event_reading_full),
        "cleaning" to (R.string.suggested_event_cleaning to R.string.suggested_event_cleaning_full),
        "cooking" to (R.string.suggested_event_cooking to R.string.suggested_event_cooking_full),
        "walking" to (R.string.suggested_event_walking to R.string.suggested_event_walking_full),
        "hobby" to (R.string.suggested_event_hobby to R.string.suggested_event_hobby_full),
        "date" to (R.string.suggested_event_date to R.string.suggested_event_date_full),
        "doctor" to (R.string.suggested_event_doctor to R.string.suggested_event_doctor_full),
        "birthday" to (R.string.suggested_event_birthday to R.string.suggested_event_birthday_full),
        "presentation" to (R.string.suggested_event_presentation to R.string.suggested_event_presentation_full),
        "call" to (R.string.suggested_event_call to R.string.suggested_event_call_full),
        "errand" to (R.string.suggested_event_errand to R.string.suggested_event_errand_full),
        "sleep" to (R.string.suggested_event_sleep to R.string.suggested_event_sleep_full),
        "breakfast" to (R.string.suggested_event_breakfast to R.string.suggested_event_breakfast_full),
        "pet" to (R.string.suggested_event_pet to R.string.suggested_event_pet_full))

/** Ключевые слова встроенных чипов для поиска по набранному названию. */
private val BUILT_IN_KEYWORDS: Map<String, Int> =
    mapOf(
        "project" to R.string.chip_keywords_project,
        "work" to R.string.chip_keywords_work,
        "dinner" to R.string.chip_keywords_dinner,
        "meeting" to R.string.chip_keywords_meeting,
        "coffee" to R.string.chip_keywords_coffee,
        "lunch" to R.string.chip_keywords_lunch,
        "shopping" to R.string.chip_keywords_shopping,
        "road" to R.string.chip_keywords_road,
        "appointment" to R.string.chip_keywords_appointment,
        "travel" to R.string.chip_keywords_travel,
        "party" to R.string.chip_keywords_party,
        "movie" to R.string.chip_keywords_movie,
        "study" to R.string.chip_keywords_study,
        "gym" to R.string.chip_keywords_gym,
        "relax" to R.string.chip_keywords_relax,
        "reading" to R.string.chip_keywords_reading,
        "cleaning" to R.string.chip_keywords_cleaning,
        "cooking" to R.string.chip_keywords_cooking,
        "walking" to R.string.chip_keywords_walking,
        "hobby" to R.string.chip_keywords_hobby,
        "date" to R.string.chip_keywords_date,
        "doctor" to R.string.chip_keywords_doctor,
        "birthday" to R.string.chip_keywords_birthday,
        "presentation" to R.string.chip_keywords_presentation,
        "call" to R.string.chip_keywords_call,
        "errand" to R.string.chip_keywords_errand,
        "sleep" to R.string.chip_keywords_sleep,
        "breakfast" to R.string.chip_keywords_breakfast,
        "pet" to R.string.chip_keywords_pet)

/** Подпись чипа: своя или переведённая встроенная. */
fun SuggestionChip.shortText(context: Context): String =
    shortName ?: BUILT_IN_TEXTS[id]?.let { context.getString(it.first) } ?: id

/** Что попадёт в название события. */
fun SuggestionChip.fullText(context: Context): String =
    fullName ?: BUILT_IN_TEXTS[id]?.let { context.getString(it.second) } ?: shortText(context)

/** Что ищем при наборе названия: подпись, полное название и ключевые слова встроенного. */
fun SuggestionChip.searchTerms(context: Context): ChipTerms =
    ChipTerms(
        names = listOf(shortText(context), fullText(context)),
        keywords =
            BUILT_IN_KEYWORDS[id]?.let { context.getString(it).split(',') }.orEmpty())

/** «Пн–Пт», «Пт, Сб», «Пн, Ср–Пт»; все дни — null: подписывать нечего. */
fun weekdaysText(days: Set<DayOfWeek>, locale: Locale = Locale.getDefault()): String? {
  if (days == ALL_DAYS) return null
  fun name(day: DayOfWeek) = day.getDisplayName(TextStyle.SHORT, locale)
  return dayRuns(days).joinToString(", ") { run ->
    if (run.size >= 3) "${name(run.first())}–${name(run.last())}"
    else run.joinToString(", ", transform = ::name)
  }
}
