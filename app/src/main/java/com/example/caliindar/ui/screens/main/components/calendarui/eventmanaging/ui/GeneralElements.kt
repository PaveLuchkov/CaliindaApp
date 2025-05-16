package com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.caliindar.R
import com.example.caliindar.ui.screens.main.components.UIDefaults.CalendarUiDefaults
import com.example.caliindar.ui.screens.main.components.UIDefaults.cuid

/**
 * Content
[AdaptiveContainer] - container for any content.
[TimePickerDialog] - timepicker
[DeleteConfirmationDialog] - delete confirmation dialog
 **/


@Composable
fun AdaptiveContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val cornerRadius = cuid.SettingsItemCornerRadius
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(color = colorScheme.surfaceContainerLow)
            .padding(cuid.ContainerPadding)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
fun TimePickerDialog(
    title: String = "Выберите время",
    onDismissRequest: () -> Unit,
    confirmButton: @Composable (() -> Unit),
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                content()
            }
        },
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.delete_conf))
        },
        text = {
            Text(text = stringResource(R.string.delete_confirmation_message))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                }
            ) {
                Text(
                    text = stringResource(R.string.delete),
                    color = colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

enum class RecurringDeleteChoice { SINGLE_INSTANCE, ALL_IN_SERIES }


@Composable
fun RecurringEventDeleteOptionsDialog(
    eventName: String,
    onDismiss: () -> Unit,
    onOptionSelected: (RecurringDeleteChoice) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Удалить повторяющееся событие") // stringResource(R.string.delete_recurring_event_title)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Событие \"$eventName\" является частью серии. Как вы хотите его удалить?", // stringResource(R.string.delete_recurring_event_prompt, eventName)
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Кнопки выбора
                TextButton(
                    onClick = { onOptionSelected(RecurringDeleteChoice.SINGLE_INSTANCE) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Удалить только это событие") // stringResource(R.string.delete_single_instance)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { onOptionSelected(RecurringDeleteChoice.ALL_IN_SERIES) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Удалить всю серию", // stringResource(R.string.delete_all_in_series)
                        color = colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            // Этот слот нам не нужен, так как действия - это выбор опций
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена") // stringResource(R.string.cancel)
            }
        }
    )
}