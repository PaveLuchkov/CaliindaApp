package com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.lpavs.caliinda.R
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.SugNameChips

fun getSuggestedEventNames(context: Context): List<SugNameChips> {
  return listOf(
      SugNameChips(
          key = "project",
          context.getString(R.string.suggested_event_project),
          context.getString(R.string.suggested_event_project_full)
      ),
      SugNameChips(
          key = "work",
          context.getString(R.string.suggested_event_work),
          context.getString(R.string.suggested_event_work_full)
      ),
      SugNameChips(
          key = "dinner",
          context.getString(R.string.suggested_event_dinner),
          context.getString(R.string.suggested_event_dinner_full)
      ),
      SugNameChips(
          key = "meeting",
          context.getString(R.string.suggested_event_meeting),
          context.getString(R.string.suggested_event_meeting_full)
      ),
      SugNameChips(
          key = "coffee",
          context.getString(R.string.suggested_event_coffee),
          context.getString(R.string.suggested_event_coffee_full)
      ),
      SugNameChips(
          key = "lunch",
          context.getString(R.string.suggested_event_lunch),
          context.getString(R.string.suggested_event_lunch_full)
      ),
      SugNameChips(
          key = "shopping",
          context.getString(R.string.suggested_event_shopping),
          context.getString(R.string.suggested_event_shopping_full)
      ),
      SugNameChips(
          key = "appointment",
          context.getString(R.string.suggested_event_appointment),
          context.getString(R.string.suggested_event_appointment_full)
      ),
      SugNameChips(
          key = "travel",
          context.getString(R.string.suggested_event_travel),
          context.getString(R.string.suggested_event_travel_full)
      ),
      SugNameChips(
          key = "party",
          context.getString(R.string.suggested_event_party),
          context.getString(R.string.suggested_event_party_full)
      ),
      SugNameChips(
          key = "movie",
          context.getString(R.string.suggested_event_movie),
          context.getString(R.string.suggested_event_movie_full)
      ),
      SugNameChips(
          key = "study",
          context.getString(R.string.suggested_event_study),
          context.getString(R.string.suggested_event_study_full)
      ),
      SugNameChips(
          key = "gym",
          context.getString(R.string.suggested_event_gym),
          context.getString(R.string.suggested_event_gym_full)
      ),
      SugNameChips(
          key = "relax",
          context.getString(R.string.suggested_event_relax),
          context.getString(R.string.suggested_event_relax_full)
      ),
      SugNameChips(
          key = "reading",
          context.getString(R.string.suggested_event_reading),
          context.getString(R.string.suggested_event_reading_full)
      ),
      SugNameChips(
          key = "cleaning",
          context.getString(R.string.suggested_event_cleaning),
          context.getString(R.string.suggested_event_cleaning_full)
      ),
      SugNameChips(
          key = "cooking",
          context.getString(R.string.suggested_event_cooking),
          context.getString(R.string.suggested_event_cooking_full)
      ),
      SugNameChips(
          key = "walking",
          context.getString(R.string.suggested_event_walking),
          context.getString(R.string.suggested_event_walking_full)
      ),
      SugNameChips(
          key = "hobby",
          context.getString(R.string.suggested_event_hobby),
          context.getString(R.string.suggested_event_hobby_full)
      ),
      SugNameChips(
          key = "date",
          context.getString(R.string.suggested_event_date),
          context.getString(R.string.suggested_event_date_full)
      ),
      SugNameChips(
          key = "doctor",
          context.getString(R.string.suggested_event_doctor),
          context.getString(R.string.suggested_event_doctor_full)
      ),
      SugNameChips(
          key = "birthday",
          context.getString(R.string.suggested_event_birthday),
          context.getString(R.string.suggested_event_birthday_full)
      ),
      SugNameChips(
          key = "presentation",
          context.getString(R.string.suggested_event_presentation),
          context.getString(R.string.suggested_event_presentation_full)
      ),
      SugNameChips(
          key = "call",
          context.getString(R.string.suggested_event_call),
          context.getString(R.string.suggested_event_call_full)
      ),
      SugNameChips(
          key = "errand",
          context.getString(R.string.suggested_event_errand),
          context.getString(R.string.suggested_event_errand_full)
      ),
      SugNameChips(
          key = "sleep",
          context.getString(R.string.suggested_event_sleep),
          context.getString(R.string.suggested_event_sleep_full)
      ),
      SugNameChips(
          key = "breakfast",
          context.getString(R.string.suggested_event_breakfast),
          context.getString(R.string.suggested_event_breakfast_full)
      ),
      SugNameChips(
          key = "pet",
          context.getString(R.string.suggested_event_pet),
          context.getString(R.string.suggested_event_pet_full)
      )
  )
}