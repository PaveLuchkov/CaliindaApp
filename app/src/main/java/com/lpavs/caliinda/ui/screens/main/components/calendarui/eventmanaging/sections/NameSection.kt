package com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.sections

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.lpavs.caliinda.R
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.ui.ChipsRow
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.ui.CustomOutlinedTextField

data class SugNameChips(val name: String, val fullText: String)
@Composable
fun getSuggestedEventNames(): List<SugNameChips> {
    val context = LocalContext.current
    return listOf(
        SugNameChips(context.getString(R.string.suggested_event_test), context.getString(R.string.suggested_event_test_full)),
        SugNameChips(context.getString(R.string.suggested_event_work), context.getString(R.string.suggested_event_work_full)),
        SugNameChips(context.getString(R.string.suggested_event_dinner), context.getString(R.string.suggested_event_dinner_full)),
        SugNameChips(context.getString(R.string.suggested_event_meeting), context.getString(R.string.suggested_event_meeting_full)),
        SugNameChips(context.getString(R.string.suggested_event_coffee), context.getString(R.string.suggested_event_coffee_full)),
        SugNameChips(context.getString(R.string.suggested_event_lunch), context.getString(R.string.suggested_event_lunch_full)),
        SugNameChips(context.getString(R.string.suggested_event_shopping), context.getString(R.string.suggested_event_shopping_full)),
        SugNameChips(context.getString(R.string.suggested_event_appointment), context.getString(R.string.suggested_event_appointment_full)),
        SugNameChips(context.getString(R.string.suggested_event_travel), context.getString(R.string.suggested_event_travel_full)),
        SugNameChips(context.getString(R.string.suggested_event_party), context.getString(R.string.suggested_event_party_full)),
        SugNameChips(context.getString(R.string.suggested_event_movie), context.getString(R.string.suggested_event_movie_full)),
        SugNameChips(context.getString(R.string.suggested_event_study), context.getString(R.string.suggested_event_study_full)),
        SugNameChips(context.getString(R.string.suggested_event_gym), context.getString(R.string.suggested_event_gym_full)),
        SugNameChips(context.getString(R.string.suggested_event_relax), context.getString(R.string.suggested_event_relax_full)),
        SugNameChips(context.getString(R.string.suggested_event_reading), context.getString(R.string.suggested_event_reading_full)),
        SugNameChips(context.getString(R.string.suggested_event_cleaning), context.getString(R.string.suggested_event_cleaning_full)),
        SugNameChips(context.getString(R.string.suggested_event_cooking), context.getString(R.string.suggested_event_cooking_full)),
        SugNameChips(context.getString(R.string.suggested_event_walking), context.getString(R.string.suggested_event_walking_full)),
        SugNameChips(context.getString(R.string.suggested_event_hobby), context.getString(R.string.suggested_event_hobby_full)),
        SugNameChips(context.getString(R.string.suggested_event_date), context.getString(R.string.suggested_event_date_full)),
        SugNameChips(context.getString(R.string.suggested_event_doctor), context.getString(R.string.suggested_event_doctor_full)),
        SugNameChips(context.getString(R.string.suggested_event_birthday), context.getString(R.string.suggested_event_birthday_full)),
        SugNameChips(context.getString(R.string.suggested_event_project), context.getString(R.string.suggested_event_project_full)),
        SugNameChips(context.getString(R.string.suggested_event_presentation), context.getString(R.string.suggested_event_presentation_full)),
        SugNameChips(context.getString(R.string.suggested_event_call), context.getString(R.string.suggested_event_call_full)),
        SugNameChips(context.getString(R.string.suggested_event_errand), context.getString(R.string.suggested_event_errand_full)),
        SugNameChips(context.getString(R.string.suggested_event_relax_bath), context.getString(R.string.suggested_event_relax_bath_full)),
        SugNameChips(context.getString(R.string.suggested_event_sleep), context.getString(R.string.suggested_event_sleep_full)),
        SugNameChips(context.getString(R.string.suggested_event_breakfast), context.getString(R.string.suggested_event_breakfast_full)),
        SugNameChips(context.getString(R.string.suggested_event_pet), context.getString(R.string.suggested_event_pet_full))
    )
}

@Composable
fun EventNameSection(
    summary: String,
    summaryError: String?,
    onSummaryChange: (String) -> Unit,
    onSummaryErrorChange: (String?) -> Unit,
    isLoading: Boolean
){
    val chips = getSuggestedEventNames()
    CustomOutlinedTextField(
        value = summary,
        onValueChange = {
            onSummaryChange(it) // Call the callback here
            onSummaryErrorChange(null) // Update the error
        },
        label = stringResource(R.string.event_name),
        modifier = Modifier.fillMaxWidth(),
        isError = summaryError != null,
        supportingText = { if (summaryError != null) Text(summaryError) }, //summaryError is nullable, no need for !!
        enabled = !isLoading,
    )
    ChipsRow(chips = chips, onChipClick = { clickedChip ->
        onSummaryChange(clickedChip)
    }, enabled = !isLoading)
}