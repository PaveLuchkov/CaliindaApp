package com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.NavigationBarDefaults.Elevation
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.lpavs.caliinda.ui.screens.main.components.UIDefaults.cuid
import com.lpavs.caliinda.R
import com.lpavs.caliinda.data.calendar.ClientEventUpdateMode
import androidx.compose.foundation.layout.height

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
            Button(
                onClick = {
                    onConfirm()
                },
                colors = ButtonDefaults.buttonColors(
                    // Сделаем кнопку "Удалить" красной, если выбрана опция "Удалить всю серию"
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
            )) {
                Text(
                    text = stringResource(R.string.delete)
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
                        colorScheme.error
                    } else {
                        colorScheme.primary
                    },
                    contentColor = if (selectedOption == RecurringDeleteChoice.ALL_IN_SERIES) {
                        colorScheme.onError
                    } else {
                        colorScheme.onPrimary
                    }
                )
            ) {
                Text(stringResource(R.string.delete)) // stringResource(R.string.delete_action)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel)) // stringResource(R.string.cancel)
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class) // Для компонентов Material 3 внутри диалога
@Composable
fun RecurringEventEditOptionsDialog(
    eventName: String,
    onDismiss: () -> Unit,
    onOptionSelected: (ClientEventUpdateMode) -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties() // Можно передавать кастомные свойства, если нужно
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.widthIn(min = 280.dp, max = 560.dp), // Рекомендации по ширине диалогов M3
        properties = properties,
    ) {
        // Обертка для Material 3 стилей (фон, форма, elevation)
        Surface(
            shape = shapes.extraLarge, // Стандартная форма для диалогов M3
            color = colorScheme.surface, // Цвет фона диалога
            tonalElevation = Elevation, // Стандартная тень для диалогов
            modifier = Modifier.wrapContentSize() // Чтобы Surface обернул контент
        ) {
            Column(
                modifier = Modifier
                    // Замени DialogDefaults.внутренниеОтступы на правильное имя, если оно другое
                    // или просто Modifier.padding(all = 24.dp)
                    .padding(top = 24.dp, bottom = 24.dp, start = 24.dp, end = 24.dp) // Явные отступы
            ) {
                // Заголовок
                Text(
                    text = "Редактировать повторяющееся событие",
                    style = typography.headlineSmall, // Стиль заголовка M3
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Текст с именем события
                Text(
                    text = "Как вы хотите отредактировать \"$eventName\"?",
                    style = typography.bodyMedium, // Стиль основного текста M3
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Опции выбора (кнопки)
                // Кнопка "Только это событие"
                TextButton(
                    onClick = { onOptionSelected(ClientEventUpdateMode.SINGLE_INSTANCE) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Только это событие")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Кнопка "Все события в серии"
                TextButton(
                    onClick = { onOptionSelected(ClientEventUpdateMode.ALL_IN_SERIES) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Все события в серии")
                }

                // Опционально: "Это и последующие", если поддерживается
                // Spacer(modifier = Modifier.height(8.dp))
                // TextButton(
                //     onClick = { onOptionSelected(ClientEventUpdateMode.THIS_AND_FOLLOWING) },
                //     modifier = Modifier.fillMaxWidth()
                // ) {
                //     Text("Это и последующие события")
                // }

                Spacer(modifier = Modifier.height(24.dp)) // Отступ перед кнопкой "Отмена"

                // Кнопка "Отмена"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End // Выравниваем кнопку "Отмена" вправо
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Отмена")
                    }
                }
            }
        }
    }
}