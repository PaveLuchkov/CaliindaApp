package com.example.caliindar.ui.screens.main.components

import android.R.attr.onClick
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarAppBar(
    isLoading: Boolean, // Принимаем конкретные состояния
    isListening: Boolean,
    onNavigateToSettings: () -> Unit,
    isBusy: Boolean,
    onGoToTodayClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text( // Material Text
                "Caliinda",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
            )
        },
        navigationIcon = {
            IconButton(onClick = onGoToTodayClick) { // Material IconButton
                Icon( // Material Icon
                    Icons.Filled.Today,
                    contentDescription = "Move to today",
                )
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading && !isListening) { // Показываем индикатор только если не идет запись
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
                        contentDescription = "Настройки",
                    )
                }
            }
        },
        // colors = TopAppBarDefaults.centerAlignedTopAppBarColors( containerColor = Color.Transparent) // Можно задать цвета, если нужно
    )
}