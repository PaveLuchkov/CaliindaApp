package com.example.caliindar.ui.screens.main.components.calendarui

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.caliindar.ui.screens.main.CalendarEvent
import com.example.caliindar.ui.screens.main.MainViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caliindar.data.local.DateTimeUtils
import com.example.caliindar.data.local.DateTimeUtils.parseToInstant
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.caliindar.ui.theme.LocalFixedAccentColors
import kotlinx.coroutines.launch
import java.time.Duration

data class GeneratedShapeParams(
    val numVertices: Int,
    val radiusSeed: Float,
    val rotationAngle: Float,
    val shadowOffsetYSeed: Dp,
    val shadowOffsetXSeed: Dp,
    val offestParam: Float,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventsList(
    events: List<CalendarEvent>,
    timeFormatter: (CalendarEvent) -> String,
    currentTime: Instant,
    isToday: Boolean,
    currentTimeZoneId: String,
    listState: LazyListState,
    nextStartTime: Instant?,
    modifier: Modifier = Modifier
) {
    val transitionWindowDurationMillis = remember { // Запоминаем длительность окна в мс
        Duration.ofMinutes(cuid.EVENT_TRANSITION_WINDOW_MINUTES).toMillis()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
    ) {

        items(items = events, key = { it.id }) { event ->
            val isCurrent = remember(currentTime, event.startTime, event.endTime) {
                val start = parseToInstant(event.startTime, currentTimeZoneId)
                val end = parseToInstant(event.endTime, currentTimeZoneId)
                start != null && end != null && !currentTime.isBefore(start) && currentTime.isBefore(end)
            }



            val isNext = remember(event.startTime, nextStartTime) {
                // isNext вычисляется ТОЛЬКО если nextStartTime не null (т.е. мы на сегодня и следующее событие есть)
                if (nextStartTime == null) false
                else {
                    val currentEventStart = parseToInstant(event.startTime, currentTimeZoneId)
                    currentEventStart != null && currentEventStart == nextStartTime
                }
            }

            val proximityRatio = remember(currentTime, event.startTime, isToday) {
                // Коэффициент рассчитывается только для СЕГОДНЯ и для БУДУЩИХ событий
                if (!isToday) {
                    0f // Не сегодня - нет перехода
                } else {
                    val start = parseToInstant(event.startTime, currentTimeZoneId)
                    if (start == null || currentTime.isAfter(start)) {
                        // Событие в прошлом или не парсится - максимальный переход (или без перехода?)
                        // Если хотим переход только для будущих, то 0f. Если для текущих/прошлых тоже, то 1f.
                        // Сделаем 0f для простоты - переход только для будущих.
                        0f
                    } else {
                        val timeUntilStartMillis = Duration.between(currentTime, start).toMillis()
                        if (timeUntilStartMillis > transitionWindowDurationMillis || transitionWindowDurationMillis <= 0) {
                            0f // Слишком далеко или некорректное окно - нет перехода
                        } else {
                            // Рассчитываем коэффициент: 1.0 (близко) -> 0.0 (далеко в пределах окна)
                            (1.0f - (timeUntilStartMillis.toFloat() / transitionWindowDurationMillis.toFloat())).coerceIn(0f, 1f)
                        }
                    }
                }
            }

        //    Spacer(modifier = Modifier.height(2.dp))
            EventListItem(
                event = event,
                timeFormatter = timeFormatter,
                isCurrentEvent = isCurrent,
                isNextEvent = isNext,
                proximityRatio = proximityRatio,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CalendarUiDefaults.ItemHorizontalPadding, vertical = CalendarUiDefaults.ItemVerticalPadding ),
                    // .animateItemPlacement(),
                currentTimeZoneId = currentTimeZoneId
            )
        //    Spacer(modifier = Modifier.height(2.dp))
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

    val currentTimeZoneId by viewModel.timeZone.collectAsStateWithLifecycle()

    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()

    val isToday = date == LocalDate.now()

    val (allDayEvents, timedEvents) = remember(events) {
        val (allDay, timed) = events.partition { it.isAllDay }
        val sortedTimed = timed.sortedBy { event ->
            parseToInstant(event.startTime, currentTimeZoneId) ?: Instant.MAX
        }
        allDay to sortedTimed
    }

    val nextStartTime: Instant? = remember(timedEvents, currentTime, isToday, currentTimeZoneId) { // Добавляем зависимость от пояса
        if (!isToday) null
        else {
            timedEvents.firstNotNullOfOrNull { event ->
                val start = parseToInstant(event.startTime, currentTimeZoneId) // Используем пояс
                if (start != null && start.isAfter(currentTime)) start else null
            }
        }
    }

    // --- ОПРЕДЕЛЯЕМ ЦЕЛЕВОЙ ИНДЕКС ДЛЯ ПРОКРУТКИ ---
    val targetScrollIndex = remember(timedEvents, currentTime, nextStartTime, isToday) {
        if (!isToday || timedEvents.isEmpty()) {
            -1 // Не скроллим, если не сегодня или нет событий
        } else {
            // 1. Ищем индекс первого текущего события
            val currentEventIndex = timedEvents.indexOfFirst { event ->
                val start = parseToInstant(event.startTime, currentTimeZoneId)
                val end = parseToInstant(event.endTime, currentTimeZoneId)
                start != null && end != null && !currentTime.isBefore(start) && currentTime.isBefore(end)
            }

            if (currentEventIndex != -1) {
                currentEventIndex // Нашли текущее - скроллим к нему
            } else {
                // 2. Если текущего нет, ищем индекс первого следующего
                if (nextStartTime != null) {
                    timedEvents.indexOfFirst { event ->
                        val start = parseToInstant(event.startTime, currentTimeZoneId)
                        start != null && start == nextStartTime
                    }
                } else {
                    -1 // Нет ни текущего, ни следующего - не скроллим
                }
            }
        }
    }

    // --- СОЗДАЕМ И ЗАПОМИНАЕМ СОСТОЯНИЕ СПИСКА ---
    val listState = rememberLazyListState()

    LaunchedEffect(targetScrollIndex, isToday) { // Запускаем, если изменился индекс или флаг isToday
        if (isToday && targetScrollIndex != -1) {
            // Запускаем корутину для вызова suspend-функции animateScrollToItem
            launch {
                // Небольшая задержка может помочь, если список еще не успел отрисоваться
                // delay(100) // Раскомментируй, если нужно
                try { // Добавим try-catch на всякий случай
                    listState.animateScrollToItem(index = targetScrollIndex)
                } catch (e: Exception) {
                    Log.e("DayEventsPageScroll", "Error scrolling to index $targetScrollIndex", e)
                }
            }
        }
    }

    val fixedColors = LocalFixedAccentColors.current

    val headerBackgroundColor = if (isToday) {
        fixedColors.tertiaryFixed // Today's color
    } else {
        fixedColors.secondaryFixed // Other dates' color
    }

    val headerTextColor = if (isToday) {
        fixedColors.onTertiaryFixed // Today's text color
    } else {
        fixedColors.onSecondaryFixed// Other dates' text color
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.
        fillMaxSize()
            .padding()) {
            Spacer(modifier = Modifier.height(3.dp))
            // Заголовок Дня (можно вынести в отдельный Composable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(color = headerBackgroundColor)
            ){
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))),
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = headerTextColor,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth(),// Больше отступы
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                )
            }
            if (allDayEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp)) // Отступ после заголовка даты
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp) // Общий горизонтальный отступ
                ) {
                    allDayEvents.forEach { event ->
                        AllDayEventItem(event = event) // Используем новый Composable
                        Spacer(modifier = Modifier.height(3.dp)) // Отступ между элементами "весь день"
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Список Событий для этого дня
            if (timedEvents.isNotEmpty()) {
                EventsList(
                    events = timedEvents, // Передаем только события со временем
                    timeFormatter = viewModel::formatEventListTime,
                    isToday = isToday,
                    nextStartTime = nextStartTime,
                    currentTime = currentTime,
                    listState = listState,
                    modifier = Modifier
                        .weight(1f) // Занимает оставшееся место
                        .fillMaxWidth(),
                    currentTimeZoneId = currentTimeZoneId
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
                Spacer(modifier = Modifier.weight(1f))
            }
        } // End Column
    } // End Box
}


@Composable
fun AllDayEventItem(event: CalendarEvent) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(25.dp))
            .background(colorScheme.tertiary)
            .padding(horizontal = CalendarUiDefaults.AllDayItemPadding, vertical = CalendarUiDefaults.AllDayItemVerticalContentPadding) // Вертикальный отступ чуть больше
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





