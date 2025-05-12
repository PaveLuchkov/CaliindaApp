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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caliindar.data.local.DateTimeUtils
import com.example.caliindar.data.local.DateTimeUtils.parseToInstant
import com.example.caliindar.ui.screens.main.components.UIDefaults.CalendarUiDefaults
import com.example.caliindar.ui.screens.main.components.UIDefaults.cuid
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.ui.SwipeActionsBackground
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.caliindar.ui.theme.LocalFixedAccentColors
import com.example.caliindar.util.DateTimeFormatterUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    modifier: Modifier = Modifier,
    onDeleteRequest: (CalendarEvent) -> Unit,
    onEditRequest: (CalendarEvent) -> Unit, // Пока просто передаем
) {
    val transitionWindowDurationMillis = remember { // Запоминаем длительность окна в мс
        Duration.ofMinutes(cuid.EVENT_TRANSITION_WINDOW_MINUTES).toMillis()
    }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
    ) {

        items(items = events, key = { it.id }) { event ->

            val eventDurationMinutes = remember(event.startTime, event.endTime, currentTimeZoneId) {
                val start = parseToInstant(event.startTime, currentTimeZoneId)
                val end = parseToInstant(event.endTime, currentTimeZoneId)
                if (start != null && end != null && end.isAfter(start)) {
                    Duration.between(start, end).toMinutes()
                } else {
                    0L
                }
            }
            val isMicroEvent = remember(eventDurationMinutes) {
                eventDurationMinutes > 0 && eventDurationMinutes <= cuid.MicroEventMaxDurationMinutes
            }
            // Новый флаг для больших событий
            val isLargeEvent = remember(eventDurationMinutes) {
                // Условие, когда кнопки должны быть в колонку
                eventDurationMinutes > cuid.HeightSigmoidMidpointMinutes // Используем твой порог
            }
            val targetHeight = remember(isMicroEvent, eventDurationMinutes) {
                calculateEventHeight(eventDurationMinutes, isMicroEvent)
            }

            // --- Состояние для SwipeToDismissBox ---
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { targetValue ->
                    if (targetValue == SwipeToDismissBoxValue.EndToStart) {
                        Log.d("SwipeToDismissM3", "confirmValueChange: Allowing transition to EndToStart")
                        true
                    } else {
                        Log.d("SwipeToDismissM3", "confirmValueChange: Denying transition to $targetValue")
                        false
                    }
                },
                positionalThreshold = { totalDistance -> totalDistance * 0.4f }
            )

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
            SwipeToDismissBox(
                state = dismissState,
                // Нам нужен свайп ВЛЕВО (контент двигается влево, фон появляется справа)
                // Это соответствует направлению EndToStart
                enableDismissFromStartToEnd = false, // Отключаем свайп вправо
                enableDismissFromEndToStart = true,  // Включаем свайп влево
                modifier = Modifier.animateItemPlacement(), // Анимация при изменении списка
                backgroundContent = {
                    // Передаем вычисленные флаги и колбэки в SwipeActionsBackground
                    SwipeActionsBackground(
                        isMicroOrAllDay = isMicroEvent, // AllDay здесь нет, используем флаг
                        isLargeEvent = isLargeEvent,
                        onDeleteClick = {
                            Log.d("SwipeEventList", "onDeleteClick from background received for ${event.id}") // Новый лог
                            scope.launch {
                                // 1. СНАЧАЛА вызываем ViewModel
                                Log.d("SwipeEventList", "Calling onDeleteRequest FIRST for ${event.id}")
                                onDeleteRequest(event) // <--- ВЫЗОВ VM

                                // 2. ПОТОМ сбрасываем состояние
                                Log.d("SwipeEventList", "Resetting swipe state AFTER request for ${event.id}")
                                dismissState.reset()
                            }
                        },
// Аналогично для onEditClick
                        onEditClick = {
                            Log.d("SwipeEventList", "onEditClick from background received for ${event.id}")
                            scope.launch {
                                Log.d("SwipeEventList", "Calling onEditRequest FIRST for ${event.id}")
                                onEditRequest(event)
                                Log.d("SwipeEventList", "Resetting swipe state AFTER edit request for ${event.id}")
                                dismissState.reset()
                            }
                        }
                    )
                }
            ) { // Этот лямбда-блок теперь является `content`
                // Оригинальный EventListItem идет сюда
                EventListItem(
                    event = event,
                    timeFormatter = timeFormatter,
                    isCurrentEvent = isCurrent,
                    isNextEvent = isNext,
                    proximityRatio = proximityRatio,
                    isMicroEvent = isMicroEvent, // Передаем внутрь
                    targetHeight = targetHeight, // Передаем высоту
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CalendarUiDefaults.ItemHorizontalPadding, vertical = CalendarUiDefaults.ItemVerticalPadding),
                    currentTimeZoneId = currentTimeZoneId
                )
            }
        }
    }
}


@Composable
fun DayEventsPage(
    date: LocalDate,
    viewModel: MainViewModel,
) {
    val scope = rememberCoroutineScope()
    val eventsFlow = remember(date) { viewModel.getEventsFlowForDate(date) }
    val eventsState = eventsFlow.collectAsStateWithLifecycle(initialValue = emptyList()) //Returns State<List<CalendarEvent>>
    val events = eventsState.value
    Log.d("DayEventsPage", "Events received from flow: ${events.joinToString { it.summary + " (allDay=" + it.isAllDay + ")" }}")

    val currentTimeZoneId by viewModel.timeZone.collectAsStateWithLifecycle()

    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()

    val isToday = date == LocalDate.now()

    val (allDayEvents, timedEvents) = remember(events, currentTimeZoneId) { // Добавим зависимость от пояса
        val (allDay, timed) = events.partition { it.isAllDay } // Используем флаг isAllDay
        val sortedTimed = timed.sortedBy { event ->
            parseToInstant(event.startTime, currentTimeZoneId) ?: Instant.MAX
        }
        allDay to sortedTimed
    }
    Log.d("DayEventsPage", "Partitioned: AllDay=${allDayEvents.size}, Timed=${timedEvents.size}")

    val nextStartTime: Instant? = remember(timedEvents, currentTime, isToday, currentTimeZoneId) {
        if (!isToday) null
        else {
            timedEvents.firstNotNullOfOrNull { event ->
                val start = parseToInstant(event.startTime, currentTimeZoneId)
                if (start != null && start.isAfter(currentTime)) start else null
            }
        }
    }
    val context = LocalContext.current

    // --- ОПРЕДЕЛЯЕМ ЦЕЛЕВОЙ ИНДЕКС ДЛЯ ПРОКРУТКИ ---
    val targetScrollIndex = remember(timedEvents, currentTime, nextStartTime, isToday, currentTimeZoneId) {
        if (!isToday || timedEvents.isEmpty()) -1
        else {
            val currentEventIndex = timedEvents.indexOfFirst { event ->
                val start = parseToInstant(event.startTime, currentTimeZoneId)
                val end = parseToInstant(event.endTime, currentTimeZoneId)
                start != null && end != null && !currentTime.isBefore(start) && currentTime.isBefore(end)
            }
            if (currentEventIndex != -1) currentEventIndex
            else if (nextStartTime != null) {
                timedEvents.indexOfFirst { event ->
                    val start = DateTimeUtils.parseToInstant(event.startTime, currentTimeZoneId)
                    start != null && start == nextStartTime
                }
            } else -1
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
        Column(modifier = Modifier
            .fillMaxSize())
        {
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
                    // TODO: ЗАМЕНИТЬ ОТОБРАЖЕНИЕ ВРЕМЕНИ НА ЛОКАЛЬ
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
                val timeFormatterLambda: (CalendarEvent) -> String = remember(viewModel, currentTimeZoneId) {
                    { event -> DateTimeFormatterUtil.formatEventListTime(context, event, currentTimeZoneId) } // Передаем оба параметра
                }
                EventsList(
                    events = timedEvents, // Передаем только события со временем
                    timeFormatter = timeFormatterLambda,
                    isToday = isToday,
                    nextStartTime = nextStartTime,
                    currentTime = currentTime,
                    listState = listState,
                    onDeleteRequest = viewModel::requestDeleteConfirmation,
                    onEditRequest = { /* TODO */ },
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
            .padding(
                horizontal = CalendarUiDefaults.AllDayItemPadding,
                vertical = CalendarUiDefaults.AllDayItemVerticalContentPadding
            ) // Вертикальный отступ чуть больше
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





