package com.lpavs.caliinda.core.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.util.fromPickerMillis
import com.lpavs.caliinda.core.ui.util.toPickerMillis
import java.time.LocalDate

/**
 * Выбор даты — один на всё приложение. Снаружи только LocalDate: перевод в UTC-полночь, в
 * которой живёт M3 DatePicker, спрятан здесь. Сегодняшний день — роль tertiary, как и везде.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    /** Раньше этой даты выбрать нельзя (конец события не раньше начала). */
    minDate: LocalDate? = null,
) {
  val state =
      rememberDatePickerState(
          initialSelectedDateMillis = initialDate.toPickerMillis(),
          selectableDates =
              object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    minDate == null || utcTimeMillis >= minDate.toPickerMillis()
              })
  val colors =
      DatePickerDefaults.colors(
          todayContentColor = colorScheme.tertiary,
          todayDateBorderColor = colorScheme.tertiary)
  DatePickerDialog(
      onDismissRequest = onDismiss,
      colors = colors,
      confirmButton = {
        TextButton(
            enabled = state.selectedDateMillis != null,
            onClick = {
              state.selectedDateMillis?.let { onDateSelected(it.fromPickerMillis()) }
              onDismiss()
            }) {
              Text(stringResource(android.R.string.ok))
            }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }) {
        DatePicker(state = state, colors = colors)
      }
}
