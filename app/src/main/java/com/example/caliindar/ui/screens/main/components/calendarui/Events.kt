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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EventListItem(
    event: CalendarEvent,
    timeFormatter: (CalendarEvent) -> String
) {
    val timeText = timeFormatter(event)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp) // Отступы элемента списка
            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)) // Увеличим скругление
            .padding(16.dp) // Внутренний отступ контента карточки
    ) {
        Text(
            text = event.summary,
            style = typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = timeText, // Используем результат нового форматтера
            style = typography.bodyMedium,
            color = colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun EventsList(
    events: List<CalendarEvent>,
    timeFormatter: (CalendarEvent) -> String,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items = events, key = { it.id }) { event ->
            EventListItem(
                event = event,
                timeFormatter = timeFormatter
            )
        }
    }
}


@Composable
fun DayEventsPage(
    date: LocalDate,
    viewModel: MainViewModel,
) {
    val eventsFlow = remember(date) { viewModel.getEventsFlowForDate(date) }
    val eventsState = eventsFlow.collectAsStateWithLifecycle(initialValue = emptyList()) //Returns State<List<CalendarEvent>>
    val events = eventsState.value

    val (allDayEvents, timedEvents) = remember(events) { // Запоминаем результат разделения
        events.partition { it.isAllDay }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Заголовок Дня (можно вынести в отдельный Composable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color = colorScheme.primary)
            ){
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))),
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth(),// Больше отступы
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                )
            }
            if (allDayEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp)) // Отступ после заголовка даты
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp) // Общий горизонтальный отступ
                ) {
                    allDayEvents.forEach { event ->
                        AllDayEventItem(event = event) // Используем новый Composable
                        Spacer(modifier = Modifier.height(6.dp)) // Отступ между элементами "весь день"
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Список Событий для этого дня
            if (timedEvents.isNotEmpty()) {
                EventsList(
                    events = timedEvents, // Передаем только события со временем
                    timeFormatter = viewModel::formatEventListTime,
                    modifier = Modifier
                        .weight(1f) // Занимает оставшееся место
                        .fillMaxWidth()
                )
            } else if (allDayEvents.isEmpty()) {
                // Показываем сообщение "нет событий", только если НЕТ НИКАКИХ событий
                Box(
                    modifier = Modifier
                        .weight(1f) // Занимает место списка
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center // Центрируем сообщение
                ) {
                    Text("На эту дату событий нет", style = typography.bodyLarge)
                }
            } else {
                // Если есть события "весь день", но нет событий со временем,
                // просто оставляем пустое место или можно добавить маленький Spacer
                Spacer(modifier = Modifier.weight(1f))
            }
        } // End Column
    } // End Box
}


@Composable
fun AllDayEventItem(event: CalendarEvent) {
    val colorScheme = colorScheme
    val typography = typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.tertiary)
            .padding(horizontal = 16.dp, vertical = 6.dp) // Вертикальный отступ чуть больше
    ) {
        Text(
            text = event.summary,
            style = typography.bodyLarge, // Стиль можно подобрать
            fontWeight = FontWeight.Medium,
            color = colorScheme.onTertiary,
            textAlign = TextAlign.Center, // Или TextAlign.Start
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
        )
    }
}