package com.example.caliinda.ui.screens.main.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class) // Для CenterAlignedTopAppBar
@Composable
fun CalendarAppBar(
    isListening: Boolean,
    onNavigateToSettings: () -> Unit,
    isBusy: Boolean, // Используем isBusy (включает загрузку и другие состояния занятости)
    onGoToTodayClick: () -> Unit,
    onTitleClick: () -> Unit, // <-- Новый колбэк для клика по заголовку
    currentDateTitle: String // <-- Новый параметр для отображения текущей даты
) {
    CenterAlignedTopAppBar(
        title = {
            // Оборачиваем текст в Modifier.clickable
            Text(
                text = "Caliinda", // Используем переданную строку даты
                fontWeight = FontWeight.Bold, // Немного изменим стиль для ясности
                fontSize = 20.sp,
                modifier = Modifier.clickable(
                    enabled = !isBusy, // Блокируем клик во время загрузки/занятости
                    onClick = onTitleClick // Вызываем колбэк
                )
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onGoToTodayClick,
            ) {
                Icon(
                    Icons.Filled.Today,
                    contentDescription = "Перейти к сегодня",
                )
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Показываем индикатор только если isBusy И НЕ isListening
                if (isBusy && !isListening) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Настройки",
                    )
                }
            }
        },
        // colors = TopAppBarDefaults.centerAlignedTopAppBarColors( containerColor = Color.Transparent)
    )
}