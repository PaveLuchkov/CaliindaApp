package com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.caliindar.ui.screens.main.components.UIDefaults.CalendarUiDefaults
import com.example.caliindar.ui.screens.main.components.UIDefaults.cuid

/**
 * Content
[AdaptiveContainer] - container for any content.
[TimePickerDialog] - timepicker
 **/


@Composable // Адаптивный контейнер для любого контента
fun AdaptiveContainer(
    modifier: Modifier = Modifier, // Позволяем модифицировать контейнер извне
    content: @Composable ColumnScope.() -> Unit // Слот для любого содержимого
) {
    val cornerRadius = cuid.SettingsItemCornerRadius // Общий радиус скругления
    Column( // Используем Row, чтобы контент мог располагаться горизонтально
        modifier = Modifier
            .fillMaxWidth() // Занимает всю доступную ширину
            .clip(RoundedCornerShape(cornerRadius))
            .background(color = colorScheme.surfaceContainer) // Цвет фона
            .padding(cuid.ContainerPadding) // Внутренний отступ
            .then(modifier), // Применяем внешние модификации, если они есть
        horizontalAlignment = Alignment.CenterHorizontally, // Выравнивание по вертикали по центру
        verticalArrangement = Arrangement.Center,
        content = content // Вставляем содержимое из слота
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
    // Используем стандартный AlertDialog из M3 как контейнер
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            // Обертка для центрирования TimePicker, если нужно
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                content() // Сюда передается TimePicker(state = ...)
            }
        },
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}
