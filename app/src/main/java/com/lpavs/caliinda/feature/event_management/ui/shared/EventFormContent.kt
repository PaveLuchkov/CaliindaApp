package com.lpavs.caliinda.feature.event_management.ui.shared

import androidx.compose.material.icons.rounded.Alarm
import com.lpavs.caliinda.core.ui.util.TIMED_REMINDER_OPTIONS
import com.lpavs.caliinda.core.ui.util.ALL_DAY_REMINDER_OPTIONS
import com.lpavs.caliinda.core.ui.util.reminderLabel
import com.lpavs.caliinda.core.ui.components.ChoiceOption
import com.lpavs.caliinda.core.ui.components.ChoiceDialog
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.minutes
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.ReminderDefaults
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.ReminderChoice
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.DetailsSection
import java.time.ZoneId
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
    zone: ZoneId,
    reminder: ReminderChoice,
    onReminderChange: (ReminderChoice) -> Unit,
    reminderDefaults: ReminderDefaults,
    isLoading: Boolean,
    suggestedChips: List<SugNameChips>,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    nameFocusRequester: FocusRequester? = null,
    showRecurrence: Boolean = true,
) {
    var activePicker by remember { mutableStateOf<ActivePicker>(ActivePicker.None) }
    val context = LocalContext.current
    var showReminderPicker by rememberSaveable { mutableStateOf(false) }
    val reminderMinutes = reminder.minutes(reminderDefaults, dateTimeState.isAllDay)

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
                showRecurrence = showRecurrence,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Место, описание, напоминание — по запросу.
        AdaptiveContainer {
            DetailsSection(
                location = location,
                onLocationChange = onLocationChange,
                description = description,
                onDescriptionChange = onDescriptionChange,
                reminderMinutes = reminderMinutes,
                isAllDay = dateTimeState.isAllDay,
                onReminderClick = { showReminderPicker = true },
                enabled = !isLoading)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
    if (showReminderPicker) {
        val allDay = dateTimeState.isAllDay
        ChoiceDialog(
            icon = Icons.Rounded.Alarm,
            title = stringResource(R.string.reminder),
            message = null,
            options =
                (if (allDay) ALL_DAY_REMINDER_OPTIONS else TIMED_REMINDER_OPTIONS).map { minutes ->
                    ChoiceOption(reminderLabel(minutes, allDay), selected = minutes == reminderMinutes) {
                        onReminderChange(ReminderChoice.Custom(minutes))
                        showReminderPicker = false
                    }
                },
            cancelText = stringResource(R.string.cancel),
            onDismiss = { showReminderPicker = false })
    }
    HandlePickerLogic(
        activePicker = activePicker,
        onDismiss = { activePicker = ActivePicker.None },
        state = dateTimeState,
        onStateChange = onDateTimeStateChange,
        context = context
    )
}
