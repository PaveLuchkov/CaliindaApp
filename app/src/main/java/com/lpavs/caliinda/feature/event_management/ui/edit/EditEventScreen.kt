package com.lpavs.caliinda.feature.event_management.ui.edit

import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import com.lpavs.caliinda.core.data.remote.EventUpdateMode
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.data.remote.dto.EventRequest
import com.lpavs.caliinda.core.ui.util.DateTimeUtils
import com.lpavs.caliinda.feature.event_management.ui.shared.AdaptiveContainer
import com.lpavs.caliinda.feature.event_management.ui.shared.TimePickerDialog
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimePicker
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimeState
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventNameSection
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.RecurrenceEndType
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.RecurrenceOption
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions.SuggestionsViewModel
import com.lpavs.caliinda.feature.event_management.vm.EventManagementUiEvent
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditEventScreen(
    viewModel: EventManagementViewModel,
    suggestionsViewModel: SuggestionsViewModel = hiltViewModel(),
    userTimeZone: String,
    eventToEdit: EventDto,
    selectedUpdateMode: EventUpdateMode,
    onDismiss: () -> Unit,
    currentSheetValue: SheetValue
) {
  var summary by remember(eventToEdit.id) { mutableStateOf(eventToEdit.summary) }
  var description by remember(eventToEdit.id) { mutableStateOf(eventToEdit.description ?: "") }
  var location by remember(eventToEdit.id) { mutableStateOf(eventToEdit.location ?: "") }

  var summaryError by remember { mutableStateOf<String?>(null) }
  var validationError by remember { mutableStateOf<String?>(null) }

  val uiState by viewModel.uiState.collectAsState()

  var generalError by remember { mutableStateOf<String?>(null) }
  val userTimeZoneId = remember { ZoneId.of(userTimeZone) }

  val context = LocalContext.current
  val untilFormatter = remember { DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'") }

  val initialEventDateTimeState =
      remember(eventToEdit.id, userTimeZone) {
        parseCalendarEventToDateTimeState(eventToEdit, userTimeZone)
      }
  var eventDateTimeState by remember(eventToEdit.id) { mutableStateOf(initialEventDateTimeState) }
  LaunchedEffect(initialEventDateTimeState) {
    Log.d("EditEventScreen", "Initial EventDateTimeState for UI: $initialEventDateTimeState")
  }

  var showStartDatePicker by remember { mutableStateOf(false) }
  var showStartTimePicker by remember { mutableStateOf(false) }
  var showEndDatePicker by remember { mutableStateOf(false) }
  var showEndTimePicker by remember { mutableStateOf(false) }
  var showRecurrenceEndDatePicker by remember { mutableStateOf(false) }

  fun formatEventTimesForSaving(
      state: EventDateTimeState,
      timeZoneId: String?
  ): Pair<String?, String?> {
    return if (state.isAllDay) {
      val formatter = DateTimeFormatter.ISO_LOCAL_DATE
      val startDateStr =
          try {
            state.startDate.format(formatter)
          } catch (_: Exception) {
            null
          }
      val effectiveEndDate = state.endDate.plusDays(1)
      val endDateStr =
          try {
            effectiveEndDate.format(formatter)
          } catch (_: Exception) {
            null
          }
      Log.d("CreateEvent", "Formatting All-Day: Start Date=$startDateStr, End Date=$endDateStr")
      Pair(startDateStr, endDateStr)
    } else {
      if (timeZoneId == null) {
        Log.e("CreateEvent", "Cannot format timed event without TimeZone ID!")
        return Pair(null, null)
      }
      val startTimeNaiveIso =
          DateTimeUtils.formatLocalDateTimeToNaiveIsoString(state.startDate, state.startTime)
      val endTimeNaiveIso =
          DateTimeUtils.formatLocalDateTimeToNaiveIsoString(state.endDate, state.endTime)
      Log.d(
          "CreateEvent",
          "Formatting Timed: Start DateTime=$startTimeNaiveIso, End DateTime=$endTimeNaiveIso")
      Pair(startTimeNaiveIso, endTimeNaiveIso)
    }
  }

  fun validateInput(): Boolean {
    summaryError =
        if (summary.isBlank()) R.string.error_summary_cannot_be_empty.toString() else null
    validationError = null
    val state = eventDateTimeState
    if (!state.isAllDay && (state.startTime == null || state.endTime == null)) {
      validationError = R.string.error_specify_start_and_end_time.toString()
      return false
    }
    val (testStartTimeStr, testEndTimeStr) = formatEventTimesForSaving(state, userTimeZone)
    if (testStartTimeStr == null || testEndTimeStr == null) {
      validationError = R.string.error_failed_to_format_datetime.toString()
      return false
    }
    return summaryError == null && validationError == null
  }

  LaunchedEffect(key1 = true) {
    viewModel.eventFlow.collect { event ->
      when (event) {
        is EventManagementUiEvent.ShowMessage -> {}
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
    generalError = null
    if (validateInput()) {
      val (startStr, endStr) = formatEventTimesForSaving(eventDateTimeState, userTimeZone)
      if (startStr == null || endStr == null) {
        return@saveLambda
      }

      val baseRule = eventDateTimeState.recurrenceRule?.takeIf { it.isNotBlank() }
      var finalRecurrenceRule: String? = null

      if (baseRule != null) {
        val ruleParts = mutableListOf(baseRule)

        if (baseRule == RecurrenceOption.Weekly.rruleValue &&
            eventDateTimeState.selectedWeekdays.isNotEmpty()) {
          val bydayString =
              eventDateTimeState.selectedWeekdays.sorted().joinToString(",") { day ->
                when (day) {
                  DayOfWeek.MONDAY -> "MO"
                  DayOfWeek.TUESDAY -> "TU"
                  DayOfWeek.WEDNESDAY -> "WE"
                  DayOfWeek.THURSDAY -> "TH"
                  DayOfWeek.FRIDAY -> "FR"
                  DayOfWeek.SATURDAY -> "SA"
                  DayOfWeek.SUNDAY -> "SU"
                }
              }
          ruleParts.add("BYDAY=$bydayString")
        }

        when (eventDateTimeState.recurrenceEndType) {
          RecurrenceEndType.DATE -> {
            eventDateTimeState.recurrenceEndDate?.let { endDate ->
              val userTimeZone = userTimeZoneId
              endDate.atTime(LocalTime.MAX).atZone(userTimeZone)

              val endDateTimeUtc =
                  endDate
                      .atTime(23, 59, 59)
                      .atZone(userTimeZone)
                      .withZoneSameInstant(ZoneOffset.UTC)

              val untilString = untilFormatter.format(endDateTimeUtc)
              ruleParts.add("UNTIL=$untilString")
            }
          }

          RecurrenceEndType.COUNT -> {
            eventDateTimeState.recurrenceCount?.let { count -> ruleParts.add("COUNT=$count") }
          }

          RecurrenceEndType.NEVER -> {}
        }

        finalRecurrenceRule = ruleParts.joinToString(";")
      }

      val updateRequest =
          buildUpdateEventApiRequest(
              originalEvent = eventToEdit,
              currentSummary = summary.trim(),
              currentDescription = description.trim(),
              currentLocation = location.trim(),
              currentDateTimeState = eventDateTimeState,
              formattedStartStr = startStr,
              formattedEndStr = endStr,
              finalRRuleStringFromUi = finalRecurrenceRule,
              userTimeZoneIdForTimed = userTimeZone,
              selectedUpdateMode = selectedUpdateMode)

      if (updateRequest == null) {
        Toast.makeText(context, R.string.no_changes_to_save, Toast.LENGTH_SHORT).show()
        onDismiss()
        return@saveLambda
      }

      viewModel.confirmEventUpdate(
          updatedEventData = updateRequest, modeFromUi = selectedUpdateMode)
    } else {
      Toast.makeText(context, R.string.error_check_input_data, Toast.LENGTH_SHORT).show()
    }
  }

  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 0.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center) {
        AnimatedContent(
            targetState = currentSheetValue,
            transitionSpec = {
              (EnterTransition.None)
                  .togetherWith(ExitTransition.None)
                  .using(
                      SizeTransform(
                          clip = false,
                          sizeAnimationSpec = { _, _ ->
                            spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow)
                          }))
            },
            label = "SaveButtonAnimation") { targetSheetValue
              -> // TODO исправить чтобы выключалась на загрузке
              val expandedSize = ButtonDefaults.LargeContainerHeight
              val defaultSize = ButtonDefaults.MediumContainerHeight

              val isNotCompactState = targetSheetValue == SheetValue.Expanded

              val size = if (!isNotCompactState) defaultSize else expandedSize

              Button(
                  onClick = onSaveClick,
                  enabled = !uiState.isLoading,
                  modifier = Modifier.heightIn(size),
                  contentPadding = ButtonDefaults.contentPaddingFor(size)) {
                    if (uiState.isLoading) {
                      LoadingIndicator(
                          color = colorScheme.onPrimary,
                          modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)))
                    } else {
                      Icon(
                          imageVector = Icons.Filled.Check,
                          contentDescription = stringResource(R.string.save),
                          modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)))
                    }
                  }
            }
      }
  Column(
      modifier =
          Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).fillMaxWidth(),
  ) {
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
                if (currentDateTimeState.startDate == currentDateTimeState.endDate &&
                    currentDateTimeState.startTime != null &&
                    !currentDateTimeState.startTime.isBefore(selectedTime)) {
                  Toast.makeText(
                          context, R.string.error_end_time_not_after_start, Toast.LENGTH_SHORT)
                      .show()
                } else {
                  eventDateTimeState = currentDateTimeState.copy(endTime = selectedTime)
                  showEndTimePicker = false
                }
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

// TODO
fun parseCalendarEventToDateTimeState(
    event: EventDto,
    userTimeZoneId: String,
): EventDateTimeState {
  val isAllDay = event.isAllDay

  var parsedStartDate: LocalDate = LocalDate.now()
  var parsedStartTime: LocalTime? = null
  var parsedEndDate: LocalDate = LocalDate.now()
  var parsedEndTime: LocalTime? = null

  try {
    if (isAllDay) {
      parsedStartDate = LocalDate.parse(event.startTime, DateTimeFormatter.ISO_LOCAL_DATE)
      parsedEndDate = LocalDate.parse(event.endTime, DateTimeFormatter.ISO_LOCAL_DATE).minusDays(1)
    } else {

      val startInstant = DateTimeUtils.parseToInstant(event.startTime, userTimeZoneId)
      val endInstant = DateTimeUtils.parseToInstant(event.endTime, userTimeZoneId)

      if (startInstant != null) {
        val startZonedDateTime = startInstant.atZone(ZoneId.of(userTimeZoneId))
        parsedStartDate = startZonedDateTime.toLocalDate()
        parsedStartTime = startZonedDateTime.toLocalTime().withNano(0)
      }
      if (endInstant != null) {
        val endZonedDateTime = endInstant.atZone(ZoneId.of(userTimeZoneId))
        parsedEndDate = endZonedDateTime.toLocalDate()
        parsedEndTime = endZonedDateTime.toLocalTime().withNano(0)
      }
    }
  } catch (e: Exception) {
    Log.e("ParseToState", "Error parsing event date/time for editing: ${e.message}")
    val now = ZonedDateTime.now(ZoneId.of(userTimeZoneId))
    parsedStartDate = now.toLocalDate()
    parsedStartTime = if (!isAllDay) now.toLocalTime().plusHours(1).withMinute(0) else null
    parsedEndDate = parsedStartDate
    parsedEndTime = if (!isAllDay) parsedStartTime?.plusHours(1) else null
  }

  var recurrenceOption: RecurrenceOption? = null
  var selectedWeekdays: Set<DayOfWeek> = emptySet()
  var recurrenceEndType = RecurrenceEndType.NEVER
  var recurrenceEndDate: LocalDate? = null
  var recurrenceCount: Int? = null
  var isRecurring = false

  event.recurrenceRule?.let { rruleString ->
    isRecurring = true
    val rules = rruleString.split(';')
    rules.forEach { rulePart ->
      val parts = rulePart.split('=')
      if (parts.size == 2) {
        val key = parts[0]
        val value = parts[1]
        when (key) {
          "FREQ" -> {
            recurrenceOption = RecurrenceOption.ALL_OPTIONS.find { it.rruleValue == "FREQ=$value" }
          }
          "BYDAY" -> {
            selectedWeekdays =
                value
                    .split(',')
                    .mapNotNull { dayStr ->
                      when (dayStr) {
                        "MO" -> DayOfWeek.MONDAY
                        "TU" -> DayOfWeek.TUESDAY
                        "WE" -> DayOfWeek.WEDNESDAY
                        "TH" -> DayOfWeek.THURSDAY
                        "FR" -> DayOfWeek.FRIDAY
                        "SA" -> DayOfWeek.SATURDAY
                        "SU" -> DayOfWeek.SUNDAY
                        else -> null
                      }
                    }
                    .toSet()
          }
          "UNTIL" -> {
            try {
              val zonedDateTime =
                  ZonedDateTime.parse(
                      value,
                      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC))
              recurrenceEndDate =
                  zonedDateTime.withZoneSameInstant(ZoneId.of(userTimeZoneId)).toLocalDate()
              recurrenceEndType = RecurrenceEndType.DATE
            } catch (e: Exception) {
              Log.e("ParseToState", "Error parsing UNTIL value: $value - ${e.message}")
            }
          }
          "COUNT" -> {
            recurrenceCount = value.toIntOrNull()
            if (recurrenceCount != null) recurrenceEndType = RecurrenceEndType.COUNT
          }
        }
      }
    }
    if (recurrenceOption == null) {
      Log.w("ParseToState", "Could not map FREQ from RRULE: $rruleString to known RecurrenceOption")
    }
  }

  Log.d("ParseToState", "--- Parsing Event to EventDateTimeState ---")
  Log.d("ParseToState", "Original Event ID: ${event.id}")
  Log.d("ParseToState", "Original Event Summary: ${event.summary}")
  Log.d("ParseToState", "Original RRULE String: ${event.recurrenceRule}")
  Log.d("ParseToState", "Parsed isAllDay: $isAllDay")
  Log.d("ParseToState", "Parsed StartDate: $parsedStartDate, StartTime: $parsedStartTime")
  Log.d("ParseToState", "Parsed EndDate: $parsedEndDate, EndTime: $parsedEndTime")
  Log.d("ParseToState", "Parsed isRecurring: $isRecurring")
  Log.d("ParseToState", "Parsed recurrenceRule (FREQ): ${recurrenceOption?.rruleValue}")
  Log.d("ParseToState", "Parsed selectedWeekdays: $selectedWeekdays")
  Log.d("ParseToState", "Parsed recurrenceEndType: $recurrenceEndType")
  Log.d("ParseToState", "Parsed recurrenceEndDate: $recurrenceEndDate")
  Log.d("ParseToState", "Parsed recurrenceCount: $recurrenceCount")
  Log.d("ParseToState", "-----------------------------------------")

  return EventDateTimeState(
      startDate = parsedStartDate,
      startTime = parsedStartTime,
      endDate = parsedEndDate,
      endTime = parsedEndTime,
      isAllDay = isAllDay,
      isRecurring = isRecurring,
      recurrenceRule = recurrenceOption?.rruleValue,
      selectedWeekdays = selectedWeekdays,
      recurrenceEndType = recurrenceEndType,
      recurrenceEndDate = recurrenceEndDate,
      recurrenceCount = recurrenceCount)
}

fun buildUpdateEventApiRequest(
    originalEvent: EventDto,
    currentSummary: String,
    currentDescription: String,
    currentLocation: String,
    currentDateTimeState: EventDateTimeState,
    formattedStartStr: String,
    formattedEndStr: String,
    finalRRuleStringFromUi: String?,
    userTimeZoneIdForTimed: String,
    selectedUpdateMode: EventUpdateMode
): EventRequest? {
  var hasChanges = false

  val summaryUpdate =
      currentSummary.takeIf { it != originalEvent.summary }?.also { hasChanges = true }
  val descriptionUpdate =
      currentDescription
          .takeIf { it != (originalEvent.description ?: "") }
          ?.also { hasChanges = true }
  val locationUpdate =
      currentLocation.takeIf { it != (originalEvent.location ?: "") }?.also { hasChanges = true }

  var startTimeUpdate: String? = null
  var endTimeUpdate: String? = null
  var isAllDayUpdate: Boolean? = null
  var timeZoneIdUpdate: String? = null

  // Проверяем изменение правила повторения
  val originalRRuleString = originalEvent.recurrenceRule?.takeIf { it.isNotBlank() }
  val currentRRuleString = finalRRuleStringFromUi?.takeIf { it.isNotBlank() }
  val recurrenceRuleChanged = currentRRuleString != originalRRuleString

  // Определяем, изменяется ли только правило повторения для всех событий
  val isOnlyRecurrenceChangeForAllEvents =
      recurrenceRuleChanged &&
          selectedUpdateMode == EventUpdateMode.ALL_IN_SERIES &&
          currentSummary == originalEvent.summary &&
          currentDescription == (originalEvent.description ?: "") &&
          currentLocation == (originalEvent.location ?: "") &&
          currentDateTimeState.isAllDay == originalEvent.isAllDay

  // Обновляем даты только если это НЕ случай изменения только правила повторения для всех событий
  if (!isOnlyRecurrenceChangeForAllEvents) {
    if (currentDateTimeState.isAllDay != originalEvent.isAllDay) {
      isAllDayUpdate = currentDateTimeState.isAllDay
      hasChanges = true
    }

    if (formattedStartStr != originalEvent.startTime) {
      startTimeUpdate = formattedStartStr
      hasChanges = true
    }
    if (formattedEndStr != originalEvent.endTime) {
      endTimeUpdate = formattedEndStr
      hasChanges = true
    }

    // Обновляем timezone только если обновляются даты
    if (!currentDateTimeState.isAllDay) {
      if (userTimeZoneIdForTimed.isNotBlank()) {
        if (isAllDayUpdate == false ||
            (isAllDayUpdate == null && (startTimeUpdate != null || endTimeUpdate != null))) {
          timeZoneIdUpdate = userTimeZoneIdForTimed
        }
      }
    }
  }

  var recurrenceForApiRequest: List<String>? = null

  if (recurrenceRuleChanged) {
    hasChanges = true

    if (currentRRuleString != null) {
      recurrenceForApiRequest = listOf("RRULE:$currentRRuleString")
    } else {
      recurrenceForApiRequest = emptyList()
    }
  }

  if (selectedUpdateMode == EventUpdateMode.SINGLE_INSTANCE && recurrenceForApiRequest != null) {
    Log.w(
        "BuildUpdateRequest",
        "Recurrence data was calculated but will be ignored for SINGLE_INSTANCE update mode.")
    recurrenceForApiRequest = null
  }

  val noPrimaryFieldChanges =
      summaryUpdate == null &&
          descriptionUpdate == null &&
          locationUpdate == null &&
          startTimeUpdate == null &&
          endTimeUpdate == null &&
          isAllDayUpdate == null &&
          timeZoneIdUpdate == null

  if (noPrimaryFieldChanges && recurrenceForApiRequest == null) {
    Log.d(
        "BuildUpdateRequest",
        "No actual changes to save after considering all fields and update mode.")
    return null
  }

  Log.d("BuildUpdateRequest", "Update mode: $selectedUpdateMode")
  Log.d("BuildUpdateRequest", "Recurrence rule changed: $recurrenceRuleChanged")
  Log.d(
      "BuildUpdateRequest",
      "Is only recurrence change for all events: $isOnlyRecurrenceChangeForAllEvents")
  Log.d("BuildUpdateRequest", "Start time update: $startTimeUpdate")
  Log.d("BuildUpdateRequest", "End time update: $endTimeUpdate")

  return EventRequest(
      summary = summaryUpdate,
      description = descriptionUpdate,
      location = locationUpdate,
      startTime = startTimeUpdate,
      endTime = endTimeUpdate,
      isAllDay = isAllDayUpdate,
      timeZoneId = timeZoneIdUpdate,
      recurrence = recurrenceForApiRequest)
}
