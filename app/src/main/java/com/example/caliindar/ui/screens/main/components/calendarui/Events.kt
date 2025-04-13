package com.example.caliindar.ui.screens.main.components.calendarui

import android.util.Log
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
    //TODO Можно улучшить: если startTime/endTime это только дата (YYYY-MM-DD), писать "Весь день"
    val timeText = try {
        // Простая проверка: если строка содержит 'T', считаем, что есть время
        if (event.startTime.contains("T") && event.endTime.contains("T")) {
            timeFormatter(event.startTime, event.endTime)
        } else {
            "Весь день"
        }
    } catch (e: Exception) {
        Log.w("EventListItem", "Could not format time: ${e.message}")
        "Время?" // Запасной вариант
    }

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
            /*
            Text(
                text = event.description,
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 2, // Ограничим описание
                overflow = TextOverflow.Ellipsis
            )

             */
        }
        // Можно добавить местоположение и т.д.
    }
}


@Composable
fun EventsList(
    events: List<CalendarEvent>,
    timeFormatter: (String, String) -> String,
    modifier: Modifier = Modifier
) {
    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopCenter) {
            Text("На эту дату событий нет", style = typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(items = events, key = { it.id }) { event ->
                EventListItem(event = event, timeFormatter = timeFormatter)
            }
        }
    }
}