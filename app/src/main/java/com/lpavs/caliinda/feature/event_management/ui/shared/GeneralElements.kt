package com.lpavs.caliinda.feature.event_management.ui.shared

import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventUpdateMode
import com.lpavs.caliinda.core.ui.theme.cuid

/**
 * Content [AdaptiveContainer] - container for any content. [TimePickerDialog] - timepicker
 */
@Composable
fun AdaptiveContainer(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  val cornerRadius = cuid.SettingsItemCornerRadius
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(cornerRadius))
              .background(color = colorScheme.surfaceContainer)
              .padding(cuid.ContainerPadding)
              .then(modifier),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      content = content)
}

@Composable
fun TimePickerDialog(
    title: String = stringResource(R.string.pick_time),
    onDismissRequest: () -> Unit,
    confirmButton: @Composable (() -> Unit),
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
  AlertDialog(
      onDismissRequest = onDismissRequest,
      title = { Text(title) },
      text = {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
      },
      confirmButton = confirmButton,
      dismissButton = dismissButton)
}

enum class RecurringDeleteChoice {
  SINGLE_INSTANCE,
  THIS_AND_FOLLOWING,
  ALL_IN_SERIES
}

/** Что удалить у повторяющегося события — одним нажатием; вся серия — красная. */
@Composable
fun RecurringEventDeleteOptionsDialog(
    eventName: String,
    onDismiss: () -> Unit,
    onOptionSelected: (RecurringDeleteChoice) -> Unit
) {
  ChoiceDialog(
      icon = Icons.Rounded.Delete,
      title = stringResource(R.string.delete_recurring_event_title),
      message = stringResource(R.string.delete_recurring_event_prompt, eventName),
      options =
          listOf(
              ChoiceOption(stringResource(R.string.delete_single_instance)) {
                onOptionSelected(RecurringDeleteChoice.SINGLE_INSTANCE)
              },
              ChoiceOption(stringResource(R.string.delete_this_and_following)) {
                onOptionSelected(RecurringDeleteChoice.THIS_AND_FOLLOWING)
              },
              ChoiceOption(stringResource(R.string.delete_all_in_series), destructive = true) {
                onOptionSelected(RecurringDeleteChoice.ALL_IN_SERIES)
              }),
      cancelText = stringResource(R.string.cancel),
      onDismiss = onDismiss)
}

/** Что править у повторяющегося события: этот экземпляр, этот и следующие или всю серию. */
@Composable
fun RecurringEventEditOptionsDialog(
    eventName: String,
    onDismiss: () -> Unit,
    onOptionSelected: (EventUpdateMode) -> Unit,
) {
  ChoiceDialog(
      icon = Icons.Rounded.Edit,
      title = stringResource(R.string.edit_recurring_event_title),
      message = stringResource(R.string.edit_recurring_event_prompt, eventName),
      options =
          listOf(
              ChoiceOption(stringResource(R.string.edit_single_instance)) {
                onOptionSelected(EventUpdateMode.SINGLE_INSTANCE)
              },
              ChoiceOption(stringResource(R.string.edit_this_and_following)) {
                onOptionSelected(EventUpdateMode.THIS_AND_FOLLOWING)
              },
              ChoiceOption(stringResource(R.string.edit_all_in_series)) {
                onOptionSelected(EventUpdateMode.ALL_IN_SERIES)
              }),
      cancelText = stringResource(R.string.cancel),
      onDismiss = onDismiss)
}
