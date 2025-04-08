package com.example.caliindar.ui.screens.main.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton // Используем Material (не M3) для старых иконок
import androidx.compose.material.Icon
import androidx.compose.material.Text // Используем Material (не M3) для старого Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.* // Используем Material 3 для AppBar и ProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarAppBar(
    isLoading: Boolean, // Принимаем конкретные состояния
    isRecording: Boolean,
    onNavigateToSettings: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text( // Material Text
                "Caliinda",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                // color = MaterialTheme.colorScheme.onSurface // Цвет обычно наследуется
            )
        },
        navigationIcon = {
            IconButton(onClick = { /* TODO: Calendar View Action */ }) { // Material IconButton
                Icon( // Material Icon
                    Icons.Filled.Today,
                    contentDescription = "Calendar View",
                    // tint = MaterialTheme.colorScheme.onSurfaceVariant // Тинт обычно наследуется
                )
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading && !isRecording) { // Показываем индикатор только если не идет запись
                    CircularProgressIndicator( // Material 3 Indicator
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp // Делаем тоньше
                    )
                }
                // Используем M3 IconButton для стандартных действий
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Настройки"
                    )
                }
            }
        },
        // colors = TopAppBarDefaults.centerAlignedTopAppBarColors( containerColor = Color.Transparent) // Можно задать цвета, если нужно
    )
}