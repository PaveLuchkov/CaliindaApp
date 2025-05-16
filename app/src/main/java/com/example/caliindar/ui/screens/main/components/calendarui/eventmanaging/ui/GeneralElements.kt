package com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
    var selectedOption by remember { mutableStateOf(RecurringDeleteChoice.SINGLE_INSTANCE) } // По умолчанию выбираем первый вариант
    val radioOptions = listOf(
        RecurringDeleteChoice.SINGLE_INSTANCE to "Удалить только это событие", // stringResource(R.string.delete_single_instance)
        RecurringDeleteChoice.ALL_IN_SERIES to "Удалить всю серию" // stringResource(R.string.delete_all_in_series)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Delete, contentDescription = "Удалить") },
        title = {
            Text(text = "Удалить повторяющееся событие") // stringResource(R.string.delete_recurring_event_title)
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) { // Важно для доступности
                Text(
                    text = "Событие \"$eventName\" является частью серии. Как вы хотите его удалить?", // stringResource(R.string.delete_recurring_event_prompt, eventName)
                    style = typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                radioOptions.forEach { (option, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (option == selectedOption),
                                onClick = { selectedOption = option },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp), // Увеличим область клика
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = null, // null, так как selectable обрабатывает клик
                            colors = RadioButtonDefaults.colors(
                                selectedColor = if (option == RecurringDeleteChoice.ALL_IN_SERIES && selectedOption == option) {
                                    colorScheme.error // Красный, если выбрана опасная опция
                                } else {
                                    colorScheme.primary
                                }
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = typography.bodyLarge,
                            color = if (option == RecurringDeleteChoice.ALL_IN_SERIES) {
                                colorScheme.error // Текст "Удалить всю серию" всегда красный
                            } else {
                                LocalContentColor.current
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onOptionSelected(selectedOption)
                    onDismiss() // Закрываем диалог после подтверждения
                },
                colors = ButtonDefaults.buttonColors(
                    // Сделаем кнопку "Удалить" красной, если выбрана опция "Удалить всю серию"
                    containerColor = if (selectedOption == RecurringDeleteChoice.ALL_IN_SERIES) {
                        colorScheme.errorContainer
                    } else {
                        colorScheme.primary
                    },
                    contentColor = if (selectedOption == RecurringDeleteChoice.ALL_IN_SERIES) {
                        colorScheme.onErrorContainer
                    } else {
                        colorScheme.onPrimary
                    }
                )
            ) {
                Text("Удалить") // stringResource(R.string.delete_action)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена") // stringResource(R.string.cancel)
            }
        }
    )
}