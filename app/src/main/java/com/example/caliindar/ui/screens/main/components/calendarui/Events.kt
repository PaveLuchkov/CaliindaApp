package com.example.caliindar.ui.screens.main.components.calendarui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.caliindar.ui.screens.main.CalendarEvent
import com.example.caliindar.ui.screens.main.MainViewModel
import androidx.compose.foundation.lazy.items

@Composable
fun EventListItem(
    event: CalendarEvent,
    timeFormatter: (String, String) -> String // Передаем функцию форматирования
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp) // Внутренний отступ
    ) {
        Text(
            text = event.summary,
            style = typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = timeFormatter(event.startTime, event.endTime), // Используем переданный форматтер
            style = typography.bodyMedium,
            color = colorScheme.onSurfaceVariant
        )
        if (!event.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.description,
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 2, // Ограничим описание
                overflow = TextOverflow.Ellipsis
            )
        }
        // Можно добавить местоположение и т.д.
    }
}


@Composable
fun EventsList(
    eventsState: MainViewModel.EventsUiState,
    timeFormatter: (String, String) -> String,
    onRetry: () -> Unit // Добавим кнопку "Повторить" для ошибок
) {
    when (eventsState) {
        is MainViewModel.EventsUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is MainViewModel.EventsUiState.Success -> {
            if (eventsState.events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("На сегодня событий нет", style = typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(eventsState.events, key = { it.id }) { event ->
                        EventListItem(event = event, timeFormatter = timeFormatter)
                    }
                }
            }
        }
        is MainViewModel.EventsUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ошибка загрузки событий:", style = typography.bodyLarge, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(eventsState.message, style = typography.bodyMedium, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("Повторить")
                    }
                }
            }
        }
        is MainViewModel.EventsUiState.Idle -> {
            // Ничего не показываем или сообщение "Загрузка данных..."
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Загрузка событий...", style = typography.bodyLarge)
            }
        }
    }
}