package com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.ui.ChipsRow
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.ui.CustomOutlinedTextField

data class SugNameChips(val name: String, val fullText: String)
val suggestedEventNames = listOf(
    SugNameChips("Test", "Testing EVENT"),
    SugNameChips("Work", "💼 Working"),
    SugNameChips("Dinner", "🍽️ Dinner"),
    SugNameChips("Meeting", "🤝 Meeting"),
    SugNameChips("Coffee", "☕ Coffee Break"),
    SugNameChips("Lunch", "🥪 Lunch Break"),
    SugNameChips("Shopping", "🛍️ Shopping"),
    SugNameChips("Appointment", "📅 Appointment"),
    SugNameChips("Travel", "✈️ Traveling"),
    SugNameChips("Party", "🎉 Party"),
    SugNameChips("Movie", "🎬 Movie Night"),
    SugNameChips("Study", "📚 Studying"),
    SugNameChips("Gym", "🏋️‍♀️ Going to the Gym"),
    SugNameChips("Relax", "🧘 Relaxing"),
    SugNameChips("Reading", "📖 Reading Time"),
    SugNameChips("Cleaning", "🧹 Cleaning the House"),
    SugNameChips("Cooking", "🍳 Cooking a Meal"),
    SugNameChips("Walking", "🚶 Walking"),
    SugNameChips("Hobby", "🎨 Hobby Time"),
    SugNameChips("Date", "❤️ Date Night"),
    SugNameChips("Doctor", "🩺 Doctor Appointment"),
    SugNameChips("Birthday", "🎂 Birthday"),
    SugNameChips("Project", "💻 Working on a Project"),
    SugNameChips("Presentation", "🗣️ Presentation"),
    SugNameChips("Call", "📞 Phone Call"),
    SugNameChips("Errand", "🏃 Errand"),
    SugNameChips("Relax", "🛀 Relax Time"),
    SugNameChips("Sleep", "😴 Sleep time"),
    SugNameChips("Breakfast", "🍳 Breakfast"),
    SugNameChips("Pet", "🦮 Walking the pet")
)

@Composable
fun EventNameSection(
    summary: String,
    summaryError: String?,
    onSummaryChange: (String) -> Unit,
    onSummaryErrorChange: (String?) -> Unit,
    isLoading: Boolean
){
    val chips = remember { suggestedEventNames }
    CustomOutlinedTextField(
        value = summary,
        onValueChange = {
            onSummaryChange(it) // Call the callback here
            onSummaryErrorChange(null) // Update the error
        },
        label = "Event Name",
        modifier = Modifier.fillMaxWidth(),
        isError = summaryError != null,
        supportingText = { if (summaryError != null) Text(summaryError) }, //summaryError is nullable, no need for !!
        enabled = !isLoading,
    )
    ChipsRow(chips = chips, onChipClick = { clickedChip ->
        onSummaryChange(clickedChip)
    }, enabled = !isLoading)
}