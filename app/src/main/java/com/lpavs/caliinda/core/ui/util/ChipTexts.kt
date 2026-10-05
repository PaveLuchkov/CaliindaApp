package com.lpavs.caliinda.core.ui.util

import android.content.Context
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.suggestions.SuggestionChip

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

/** Подпись чипа: своя или переведённая встроенная. */
fun SuggestionChip.shortText(context: Context): String =
    shortName ?: BUILT_IN_TEXTS[id]?.let { context.getString(it.first) } ?: id

/** Что попадёт в название события. */
fun SuggestionChip.fullText(context: Context): String =
    fullName ?: BUILT_IN_TEXTS[id]?.let { context.getString(it.second) } ?: shortText(context)
