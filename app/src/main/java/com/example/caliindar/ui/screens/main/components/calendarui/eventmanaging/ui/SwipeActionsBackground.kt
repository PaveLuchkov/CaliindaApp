package com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete // Иконка удаления
import androidx.compose.material.icons.filled.Edit   // Иконка редактирования (для мока)
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.caliindar.ui.screens.main.components.UIDefaults.CalendarUiDefaults

@Composable
fun SwipeActionsBackground(
    isMicroOrAllDay: Boolean, // Объединяем Micro и AllDay, т.к. у них иконки
    isLargeEvent: Boolean,    // Флаг для события > HeightSigmoidMidpointMinutes
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit, // Пока мок
    modifier: Modifier = Modifier
) {
    // Определяем цвета для кнопок
    val deleteButtonColor = colorScheme.errorContainer
    val onDeleteButtonColor = colorScheme.onErrorContainer
    val editButtonColor = colorScheme.secondaryContainer
    val onEditButtonColor = colorScheme.onSecondaryContainer

    Box(
        modifier = modifier
            .fillMaxSize()
            // Можно добавить фон для всей области свайпа, если нужно
            // .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = CalendarUiDefaults.ItemHorizontalPadding, vertical = CalendarUiDefaults.ItemVerticalPadding), // Внешние отступы как у элемента
        contentAlignment = Alignment.CenterEnd // Выравниваем кнопки по правому краю
    ) {
        if (isMicroOrAllDay) {
            // --- Микро-события или AllDay: Иконки в ряд ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End // Прижимаем к правому краю
            ) {
                // Кнопка Редактировать (мок)
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Редактировать", // Для доступности
                        tint = editButtonColor // Цвет иконки
                    )
                }
                Spacer(modifier = Modifier.width(8.dp)) // Отступ между иконками
                // Кнопка Удалить
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Удалить", // Для доступности
                        tint = deleteButtonColor // Цвет иконки
                    )
                }
            }
        } else if (isLargeEvent) {
            // --- Большие события: FAB'ы в колонку ---
            Column(
                horizontalAlignment = Alignment.End, // Прижимаем к правому краю
                verticalArrangement = Arrangement.Center // Центрируем вертикально
            ) {
                // Кнопка Редактировать (мок) - Small FAB
                SmallFloatingActionButton(
                    onClick = onEditClick,
                    containerColor = editButtonColor,
                    contentColor = onEditButtonColor
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Редактировать")
                }
                Spacer(modifier = Modifier.height(8.dp)) // Отступ между кнопками
                // Кнопка Удалить - Small FAB
                SmallFloatingActionButton(
                    onClick = onDeleteClick,
                    containerColor = deleteButtonColor,
                    contentColor = onDeleteButtonColor
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                }
            }
        } else {
            // --- Стандартные события: FAB'ы в ряд ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End // Прижимаем к правому краю
            ) {
                // Кнопка Редактировать (мок) - Small FAB
                SmallFloatingActionButton(
                    onClick = onEditClick,
                    containerColor = editButtonColor,
                    contentColor = onEditButtonColor
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Редактировать")
                }
                Spacer(modifier = Modifier.width(8.dp)) // Отступ между кнопками
                // Кнопка Удалить - Small FAB
                SmallFloatingActionButton(
                    onClick = onDeleteClick,
                    containerColor = deleteButtonColor,
                    contentColor = onDeleteButtonColor
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                }
            }
        }
    }
}