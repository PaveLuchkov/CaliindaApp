package com.lpavs.caliinda.feature.event_management.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimePicker
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimeState
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventNameSection
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.HandlePickerLogic
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.SugNameChips

@Composable
fun EventFormContent(
    summary: String,
    onSummaryChange: (String) -> Unit,
    summaryError: String?,
    onSummaryErrorChange: (String?) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    dateTimeState: EventDateTimeState,
    onDateTimeStateChange: (EventDateTimeState) -> Unit,
    isLoading: Boolean,
    suggestedChips: List<SugNameChips>,
    modifier: Modifier = Modifier
) {
    var activePicker by remember { mutableStateOf<ActivePicker>(ActivePicker.None) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Секция названия
        AdaptiveContainer {
            EventNameSection(
                summary = summary,
                summaryError = summaryError,
                onSummaryChange = onSummaryChange,
                onSummaryErrorChange = onSummaryErrorChange,
                isLoading = isLoading,
                suggestedChips = suggestedChips
            )
        }

        // Секция даты и времени
        AdaptiveContainer {
            EventDateTimePicker(
                state = dateTimeState,
                onStateChange = onDateTimeStateChange,
                isLoading = isLoading,
                onRequestShowStartDatePicker = { activePicker = ActivePicker.StartDate },
                onRequestShowStartTimePicker = { activePicker = ActivePicker.StartTime },
                onRequestShowEndDatePicker = { activePicker = ActivePicker.EndDate },
                onRequestShowEndTimePicker = { activePicker = ActivePicker.EndTime },
                onRequestShowRecurrenceEndDatePicker = { activePicker = ActivePicker.RecurrenceEnd },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Описание и Локация
        AdaptiveContainer {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min=100.dp),
//                    maxLines = 4,
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = colorScheme.surfaceContainerHigh,
                    ),
                    shape = RoundedCornerShape(25.dp)
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = onLocationChange,
                    label = { Text(stringResource(R.string.location)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = colorScheme.surfaceContainerHigh,
                    ),
                    shape = RoundedCornerShape(25.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
    HandlePickerLogic(
        activePicker = activePicker,
        onDismiss = { activePicker = ActivePicker.None },
        state = dateTimeState,
        onStateChange = onDateTimeStateChange,
        context = context
    )
}
