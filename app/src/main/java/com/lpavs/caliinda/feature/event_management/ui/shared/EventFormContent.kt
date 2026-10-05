package com.lpavs.caliinda.feature.event_management.ui.shared

import java.time.ZoneId
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import com.lpavs.caliinda.core.ui.theme.cuid
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.DurationChips
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
    zone: ZoneId,
    isLoading: Boolean,
    suggestedChips: List<SugNameChips>,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    nameFocusRequester: FocusRequester? = null,
) {
    var activePicker by remember { mutableStateOf<ActivePicker>(ActivePicker.None) }
    val context = LocalContext.current
    // Описание и место нужны редко — прячем за кнопкой, но сразу показываем, если они уже есть.
    var showDetails by rememberSaveable {
        mutableStateOf(description.isNotEmpty() || location.isNotEmpty())
    }

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
                suggestedChips = suggestedChips,
                onImeDone = onSave,
                focusRequester = nameFocusRequester,
            )
        }

        // Секция даты и времени
        AdaptiveContainer {
            EventDateTimePicker(
                state = dateTimeState,
                onStateChange = onDateTimeStateChange,
                zone = zone,
                isLoading = isLoading,
                onRequestShowStartDatePicker = { activePicker = ActivePicker.StartDate },
                onRequestShowStartTimePicker = { activePicker = ActivePicker.StartTime },
                onRequestShowEndDatePicker = { activePicker = ActivePicker.EndDate },
                onRequestShowEndTimePicker = { activePicker = ActivePicker.EndTime },
                onRequestShowRecurrenceEndDatePicker = { activePicker = ActivePicker.RecurrenceEnd },
                modifier = Modifier.fillMaxWidth()
            )
            DurationChips(
                state = dateTimeState,
                onStateChange = onDateTimeStateChange,
                enabled = !isLoading,
                modifier = Modifier.padding(top = cuid.padding)
            )
        }

        // Описание и Локация
        AnimatedContent(
            targetState = showDetails,
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
            label = "details") { expanded ->
            if (!expanded) {
                TextButton(onClick = { showDetails = true }, enabled = !isLoading) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.add_details))
                }
            } else {
                AdaptiveContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = onDescriptionChange,
                            label = { Text(stringResource(R.string.description)) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                            enabled = !isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = colorScheme.surfaceContainerHighest,
                                unfocusedContainerColor = colorScheme.surfaceContainerHigh,
                            ),
                            shape = RoundedCornerShape(cuid.ContainerCornerRadius)
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
                            shape = RoundedCornerShape(cuid.ContainerCornerRadius)
                        )
                    }
                }
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
