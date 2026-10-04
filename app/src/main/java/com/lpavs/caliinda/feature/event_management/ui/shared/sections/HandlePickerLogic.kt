package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lpavs.caliinda.R
import com.lpavs.caliinda.feature.event_management.ui.shared.ActivePicker
import com.lpavs.caliinda.feature.event_management.ui.shared.TimePickerDialog
import com.lpavs.caliinda.core.ui.util.fromPickerMillis
import com.lpavs.caliinda.core.ui.util.toPickerMillis
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandlePickerLogic(
    activePicker: ActivePicker,
    onDismiss: () -> Unit,
    state: EventDateTimeState,
    onStateChange: (EventDateTimeState) -> Unit,
    context: Context
) {
  when (activePicker) {
    ActivePicker.StartDate -> {
      val datePickerState =
          rememberDatePickerState(
              initialSelectedDateMillis =
                  state.startDate.toPickerMillis())
      DatePickerDialog(
          onDismissRequest = onDismiss,
          confirmButton = {
            TextButton(
                onClick = {
                  datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate =
                        millis.fromPickerMillis()

                    onStateChange(
                        state.copy(
                            startDate = selectedDate,
                            endDate =
                                if (selectedDate.isAfter(state.endDate)) selectedDate
                                else state.endDate))
                  }
                  onDismiss()
                }) {
                  Text(stringResource(android.R.string.ok))
                }
          },
          dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
          }) {
            DatePicker(state = datePickerState)
          }
    }

    ActivePicker.StartTime -> {
      val initialTime = state.startTime ?: LocalTime.now()
      val timePickerState =
          rememberTimePickerState(
              initialHour = initialTime.hour,
              initialMinute = initialTime.minute,
              is24Hour = DateFormat.is24HourFormat(context))
      TimePickerDialog(
          onDismissRequest = onDismiss,
          confirmButton = {
            TextButton(
                onClick = {
                  val selectedTime =
                      LocalTime.of(timePickerState.hour, timePickerState.minute).withNano(0)
                  var newEndTime = state.endTime

                  // Если это тот же день и время конца становится раньше начала — сдвигаем конец на
                  // час
                  if (state.startDate == state.endDate &&
                      state.endTime != null &&
                      !selectedTime.isBefore(state.endTime)) {
                    newEndTime = selectedTime.plusHours(1).withNano(0)
                  }

                  onStateChange(state.copy(startTime = selectedTime, endTime = newEndTime))
                  onDismiss()
                }) {
                  Text(stringResource(android.R.string.ok))
                }
          }) {
            TimePicker(state = timePickerState)
          }
    }

    ActivePicker.EndDate -> {
      val datePickerState =
          rememberDatePickerState(
              initialSelectedDateMillis =
                  state.endDate.toPickerMillis(),
              selectableDates =
                  object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                      val startMillis =
                          state.startDate.toPickerMillis()
                      return utcTimeMillis >= startMillis
                    }
                  })
      DatePickerDialog(
          onDismissRequest = onDismiss,
          confirmButton = {
            TextButton(
                onClick = {
                  datePickerState.selectedDateMillis?.let { millis ->
                    onStateChange(
                        state.copy(
                            endDate =
                                millis.fromPickerMillis()))
                  }
                  onDismiss()
                }) {
                  Text(stringResource(android.R.string.ok))
                }
          }) {
            DatePicker(state = datePickerState)
          }
    }

    ActivePicker.EndTime -> {
      val initialTime = state.endTime ?: state.startTime?.plusHours(1) ?: LocalTime.now()
      val timePickerState =
          rememberTimePickerState(
              initialHour = initialTime.hour,
              initialMinute = initialTime.minute,
              is24Hour = DateFormat.is24HourFormat(context))
      TimePickerDialog(
          onDismissRequest = onDismiss,
          confirmButton = {
            TextButton(
                onClick = {
                  val selectedTime =
                      LocalTime.of(timePickerState.hour, timePickerState.minute).withNano(0)

                  // Проверка: время конца не может быть раньше начала в тот же день
                  if (state.startDate == state.endDate &&
                      state.startTime != null &&
                      !state.startTime.isBefore(selectedTime)) {
                    Toast.makeText(
                            context, R.string.error_end_time_not_after_start, Toast.LENGTH_SHORT)
                        .show()
                  } else {
                    onStateChange(state.copy(endTime = selectedTime))
                    onDismiss()
                  }
                }) {
                  Text(stringResource(android.R.string.ok))
                }
          }) {
            TimePicker(state = timePickerState)
          }
    }

    ActivePicker.RecurrenceEnd -> {
      val datePickerState =
          rememberDatePickerState(
              initialSelectedDateMillis =
                  (state.recurrenceEndDate ?: state.startDate.plusMonths(1)).toPickerMillis())
      DatePickerDialog(
          onDismissRequest = onDismiss,
          confirmButton = {
            TextButton(
                onClick = {
                  datePickerState.selectedDateMillis?.let { millis ->
                    onStateChange(
                        state.copy(
                            recurrenceEndDate =
                                millis.fromPickerMillis(),
                            recurrenceEndType = RecurrenceEndType.DATE,
                            recurrenceCount = null))
                  }
                  onDismiss()
                }) {
                  Text(stringResource(android.R.string.ok))
                }
          }) {
            DatePicker(state = datePickerState)
          }
    }

    ActivePicker.None -> {
    }
  }
}
