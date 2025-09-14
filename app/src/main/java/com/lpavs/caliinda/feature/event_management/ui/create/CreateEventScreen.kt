package com.lpavs.caliinda.feature.event_management.ui.create

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.feature.event_management.ui.shared.AdaptiveContainer
import com.lpavs.caliinda.feature.event_management.ui.shared.TimePickerDialog
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimePicker
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimeState
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventNameSection
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.RecurrenceEndType
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions.SuggestionsViewModel
import com.lpavs.caliinda.feature.event_management.vm.EventManagementUiEvent
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateEventScreen(
    viewModel: EventManagementViewModel = hiltViewModel(),
    suggestionsViewModel: SuggestionsViewModel = hiltViewModel(),
    userTimeZone: String,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    currentSheetValue: SheetValue
) {
  var summary by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
  val userTimeZoneId = remember { ZoneId.of(userTimeZone) }

  var summaryError by remember { mutableStateOf<String?>(null) }
  var validationError by remember { mutableStateOf<String?>(null) }

  var generalError by remember { mutableStateOf<String?>(null) }

  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  var showStartDatePicker by remember { mutableStateOf(false) }
  var showStartTimePicker by remember { mutableStateOf(false) }
  var showEndDatePicker by remember { mutableStateOf(false) }
  var showEndTimePicker by remember { mutableStateOf(false) }
  var showRecurrenceEndDatePicker by remember { mutableStateOf(false) }

  var eventDateTimeState by remember {
    val defaultStartTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
    val defaultEndTime = LocalTime.now().plusHours(2).withMinute(0).withSecond(0).withNano(0)
    var effectiveEndDate = initialDate

    if (defaultEndTime.isBefore(defaultStartTime)) {
      effectiveEndDate = initialDate.plusDays(1)
    }

    mutableStateOf(
        EventDateTimeState(
            startDate = initialDate,
            startTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0),
            endDate = effectiveEndDate,
            endTime = LocalTime.now().plusHours(2).withMinute(0).withSecond(0).withNano(0),
            isAllDay = false,
            selectedWeekdays = emptySet(),
            recurrenceEndType = RecurrenceEndType.NEVER,
            isRecurring = false,
            recurrenceRule = null))
  }

  LaunchedEffect(key1 = true) {
    viewModel.eventFlow.collect { event ->
      when (event) {
        is EventManagementUiEvent.ShowMessage -> {
          //          Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
        }
        is EventManagementUiEvent.OperationSuccess -> {
          onDismiss()
        }
      }
    }
  }
  LaunchedEffect(eventDateTimeState.startTime) {
    suggestionsViewModel.updateSortContext(
        eventDateTimeState.startTime, eventDateTimeState.isAllDay)
  }
  val suggestedChips by suggestionsViewModel.suggestionChips.collectAsStateWithLifecycle()

  val onSaveClick: () -> Unit = saveLambda@{
    viewModel.createEvent(
        summary = summary,
        description = description,
        location = location,
        dateTimeState = eventDateTimeState)
  }

  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 0.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center) {
        Button(
            onClick = onSaveClick,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth().padding(cuid.ContainerPadding),
        ) {
          if (uiState.isLoading) {
            LoadingIndicator(
                color = colorScheme.onPrimary,
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(30.dp)))
          } else {
            Text(
                text = stringResource(R.string.save),
            )
          }
        }
      }

  Column(
      modifier =
          Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AdaptiveContainer {
          EventNameSection(
              summary = summary,
              summaryError = summaryError,
              onSummaryChange = { summary = it },
              onSummaryErrorChange = { summaryError = it },
              isLoading = uiState.isLoading,
              suggestedChips = suggestedChips)
        }
        AdaptiveContainer {
          EventDateTimePicker(
              state = eventDateTimeState,
              onStateChange = { newState ->
                eventDateTimeState = newState
                validationError = null
              },
              isLoading = uiState.isLoading,
              onRequestShowStartDatePicker = { showStartDatePicker = true },
              onRequestShowStartTimePicker = { showStartTimePicker = true },
              onRequestShowEndDatePicker = { showEndDatePicker = true },
              onRequestShowEndTimePicker = { showEndTimePicker = true },
              onRequestShowRecurrenceEndDatePicker = { showRecurrenceEndDatePicker = true },
              modifier = Modifier.fillMaxWidth())
        }
        validationError?.let { Text(it, color = colorScheme.error, style = typography.bodySmall) }

        AdaptiveContainer {
          OutlinedTextField(
              value = description,
              onValueChange = { description = it },
              label = { Text(stringResource(R.string.description)) },
              modifier = Modifier.fillMaxWidth().height(100.dp),
              maxLines = 4,
              enabled = !uiState.isLoading,
              shape = RoundedCornerShape(25.dp))
          OutlinedTextField(
              value = location,
              onValueChange = { location = it },
              label = { Text(stringResource(R.string.location)) },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              enabled = !uiState.isLoading,
              shape = RoundedCornerShape(25.dp))
        }
        generalError?.let { Text(it, color = colorScheme.error, style = typography.bodyMedium) }
        Spacer(modifier = Modifier.height(16.dp))
      }

  val currentDateTimeState = eventDateTimeState

  if (showStartDatePicker) {
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                currentDateTimeState.startDate
                    .atStartOfDay(userTimeZoneId)
                    .toInstant()
                    .toEpochMilli())
    DatePickerDialog(
        onDismissRequest = { showStartDatePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                  val selectedDate =
                      Instant.ofEpochMilli(millis).atZone(userTimeZoneId).toLocalDate()
                  eventDateTimeState =
                      currentDateTimeState.copy(
                          startDate = selectedDate,
                          endDate =
                              if (selectedDate.isAfter(currentDateTimeState.endDate) ||
                                  currentDateTimeState.startTime == null)
                                  selectedDate
                              else currentDateTimeState.endDate)
                }
                showStartDatePicker = false
              },
              enabled = datePickerState.selectedDateMillis != null) {
                Text("OK")
              }
        },
        dismissButton = {
          TextButton(onClick = { showStartDatePicker = false }) {
            Text(stringResource(R.string.cancel))
          }
        }) {
          DatePicker(state = datePickerState)
        }
  }
  if (showStartTimePicker) {
    val initialTime = currentDateTimeState.startTime ?: LocalTime.now()
    val timePickerState =
        rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = DateFormat.is24HourFormat(context))
    TimePickerDialog(
        onDismissRequest = { showStartTimePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                val selectedTime =
                    LocalTime.of(timePickerState.hour, timePickerState.minute).withNano(0)
                var newEndTime = currentDateTimeState.endTime
                if (currentDateTimeState.startDate == currentDateTimeState.endDate &&
                    currentDateTimeState.endTime != null &&
                    !selectedTime.isBefore(currentDateTimeState.endTime)) {
                  newEndTime = selectedTime.plusHours(1).withNano(0)
                }
                eventDateTimeState =
                    currentDateTimeState.copy(startTime = selectedTime, endTime = newEndTime)
                showStartTimePicker = false
              }) {
                Text("OK")
              }
        },
        dismissButton = {
          TextButton(onClick = { showStartTimePicker = false }) {
            Text(stringResource(R.string.cancel))
          }
        }) {
          TimePicker(state = timePickerState)
        }
  }

  if (showEndDatePicker) {
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                currentDateTimeState.endDate
                    .atStartOfDay(userTimeZoneId)
                    .toInstant()
                    .toEpochMilli(),
            selectableDates =
                object : SelectableDates {
                  val startMillis =
                      currentDateTimeState.startDate
                          .atStartOfDay(userTimeZoneId)
                          .toInstant()
                          .toEpochMilli()

                  override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= startMillis
                  }

                  override fun isSelectableYear(year: Int): Boolean {
                    return year >= currentDateTimeState.startDate.year
                  }
                })
    DatePickerDialog(
        onDismissRequest = { showEndDatePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                  val selectedDate =
                      Instant.ofEpochMilli(millis).atZone(userTimeZoneId).toLocalDate()
                  eventDateTimeState = currentDateTimeState.copy(endDate = selectedDate)
                }
                showEndDatePicker = false
              },
              enabled = datePickerState.selectedDateMillis != null) {
                Text("OK")
              }
        },
        dismissButton = {
          TextButton(onClick = { showEndDatePicker = false }) {
            Text(stringResource(R.string.cancel))
          }
        }) {
          DatePicker(state = datePickerState)
        }
  }

  if (showEndTimePicker) {
    val initialTime =
        currentDateTimeState.endTime
            ?: currentDateTimeState.startTime?.plusHours(1)
            ?: LocalTime.now()
    val timePickerState =
        rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = DateFormat.is24HourFormat(context))
    TimePickerDialog(
        onDismissRequest = { showEndTimePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                val selectedTime =
                    LocalTime.of(timePickerState.hour, timePickerState.minute).withNano(0)

                var newFinalEndDate = currentDateTimeState.endDate

                if (currentDateTimeState.startDate == currentDateTimeState.endDate &&
                    currentDateTimeState.startTime != null &&
                    selectedTime.isBefore(currentDateTimeState.startTime)) {
                  newFinalEndDate = currentDateTimeState.startDate.plusDays(1)
                }

                eventDateTimeState =
                    currentDateTimeState.copy(endTime = selectedTime, endDate = newFinalEndDate)
                showEndTimePicker = false
              }) {
                Text("OK")
              }
        },
        dismissButton = {
          TextButton(onClick = { showEndTimePicker = false }) {
            Text(stringResource(R.string.cancel))
          }
        }) {
          TimePicker(state = timePickerState)
        }
  }
  if (showRecurrenceEndDatePicker) {
    val initialSelectedDateMillis =
        eventDateTimeState.recurrenceEndDate
            ?.atStartOfDay(userTimeZoneId)
            ?.toInstant()
            ?.toEpochMilli()
            ?: eventDateTimeState.startDate
                .plusMonths(1)
                .atStartOfDay(userTimeZoneId)
                .toInstant()
                .toEpochMilli()

    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedDateMillis,
            selectableDates =
                object : SelectableDates {
                  override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val selectedLocalDate =
                        Instant.ofEpochMilli(utcTimeMillis).atZone(userTimeZoneId).toLocalDate()
                    return !selectedLocalDate.isBefore(eventDateTimeState.startDate)
                  }

                  override fun isSelectableYear(year: Int): Boolean {
                    return year >= eventDateTimeState.startDate.year
                  }
                })
    DatePickerDialog(
        onDismissRequest = { showRecurrenceEndDatePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                  val selectedDate =
                      Instant.ofEpochMilli(millis).atZone(userTimeZoneId).toLocalDate()
                  eventDateTimeState =
                      eventDateTimeState.copy(
                          recurrenceEndDate = selectedDate,
                          recurrenceEndType = RecurrenceEndType.DATE,
                          recurrenceCount = null)
                }
                showRecurrenceEndDatePicker = false
              },
          ) {
            Text("OK")
          }
        },
        dismissButton = {
          TextButton(onClick = { showRecurrenceEndDatePicker = false }) {
            Text(stringResource(R.string.cancel))
          }
        }) {
          DatePicker(state = datePickerState)
        }
  }
}
