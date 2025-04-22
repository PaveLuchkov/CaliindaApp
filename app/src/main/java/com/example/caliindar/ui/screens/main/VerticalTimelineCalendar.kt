package com.example.caliindar.ui.screens.main

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// import com.example.caliindar.data.local.CalendarEvent
import com.example.caliindar.data.mapper.EventMapper.parseIsoToLocalDate
import com.example.caliindar.ui.screens.main.MainViewModel.DayData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

// Форматтер для заголовка дня (можно вынести)
private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru"))

/**
 * Основной Composable для отображения вертикальной ленты календаря.
 * @param modifier Модификатор для LazyColumn.
 * @param eventsByDate Карта событий, сгруппированных по дате.
 * @param listState Состояние LazyList для отслеживания прокрутки.
 * @param onVisibleDatesChanged Колбэк, вызываемый при изменении видимого диапазона дат.
 */
@Composable
fun VerticalTimelineCalendar(
    modifier: Modifier = Modifier,
    // eventsByDate переименован в eventsData
    eventsData: Map<LocalDate, DayData>,
    listState: LazyListState = rememberLazyListState(),
    onVisibleDatesChanged: (first: LocalDate, last: LocalDate) -> Unit,
    initialScrollTargetDate: LocalDate = LocalDate.now()
) {
    // Получаем отсортированный список дат из карты для построения списка
    // Сортируем, чтобы дни шли последовательно
    val sortedDates = remember(eventsData) { eventsData.keys.sorted() }
    var initialScrollDone by remember(sortedDates) { mutableStateOf(false) }

    // Отслеживаем видимые элементы для вызова onVisibleDatesChanged
    LaunchedEffect(listState, sortedDates) { // Добавляем sortedDates в ключ
        snapshotFlow { listState.layoutInfo } // Следим за layoutInfo целиком
            .collect { layoutInfo ->
                val visibleItems = layoutInfo.visibleItemsInfo
                // Проверяем, есть ли вообще элементы и отсортированные даты
                if (sortedDates.isNotEmpty()) {
                    val firstDate: LocalDate
                    val lastDate: LocalDate

                    if (visibleItems.isEmpty()) {
                        // Если видимых элементов НЕТ (например, при самой первой загрузке пустого списка)
                        // Используем первую и последнюю дату из ВСЕХ загруженных дат
                        firstDate = sortedDates.first()
                        lastDate = sortedDates.last()
                        Log.d(
                            "TimelineCalendar",
                            "No visible items, using full loaded range: $firstDate to $lastDate"
                        )
                    } else {
                        // Определяем индексы видимых элементов
                        val firstVisibleIndex = visibleItems.first().index
                        val lastVisibleIndex = visibleItems.last().index

                        // --- БОЛЕЕ НАДЕЖНОЕ ОПРЕДЕЛЕНИЕ ДАТ ---
                        // Получаем ключи видимых элементов (если мы их задали)
                        val firstKey = visibleItems.first().key as? Long
                        val lastKey = visibleItems.last().key as? Long

                        if (firstKey != null && lastKey != null) {
                            // Ищем даты по ключам (которые мы задали как date.toEpochDay())
                            firstDate = sortedDates.find { it.toEpochDay() == firstKey }
                                ?: sortedDates.first() // Фоллбэк
                            lastDate = sortedDates.find { it.toEpochDay() == lastKey }
                                ?: sortedDates.last() // Фоллбэк
                            Log.d(
                                "TimelineCalendar",
                                "Visible items by key: $firstDate to $lastDate (Indices: $firstVisibleIndex..$lastVisibleIndex)"
                            )
                        } else {
                            // Фоллбэк на старую логику с индексами, если ключи не сработали
                            val firstDateIndex = firstVisibleIndex.coerceIn(0, sortedDates.size - 1)
                            val lastDateIndex = lastVisibleIndex.coerceIn(0, sortedDates.size - 1)
                            if (firstDateIndex <= lastDateIndex) {
                                firstDate = sortedDates[firstDateIndex]
                                lastDate = sortedDates[lastDateIndex]
                                Log.d(
                                    "TimelineCalendar",
                                    "Visible items by index: $firstDate to $lastDate (Indices: $firstVisibleIndex..$lastVisibleIndex)"
                                )
                            } else {
                                // Невалидное состояние индексов, используем весь диапазон
                                firstDate = sortedDates.first()
                                lastDate = sortedDates.last()
                                Log.w(
                                    "TimelineCalendar",
                                    "Invalid visible indices, using full range: $firstDate..$lastDate"
                                )
                            }
                        }
                        // ---------------------------------------

                    }

                    // Проверяем, НЕ мал ли контент (не весь ли он помещается на экране)
                    val totalItemsCount = layoutInfo.totalItemsCount
                    val visibleItemsCount = visibleItems.size
                    // Если все элементы видны И их не очень много (эвристика) -> возможно нужно еще
                    val isContentSmall =
                        visibleItemsCount == totalItemsCount && totalItemsCount < 15 // Подбери порог

                    // Вызываем колбэк всегда, когда есть видимые даты
                    onVisibleDatesChanged(firstDate, lastDate)

                    // --- Дополнительная проверка ---
                    // Если контент мал, возможно, стоит вызвать загрузку с расширенным диапазоном?
                    // Хотя логика в ViewModel уже должна это делать через PRELOAD_DAYS.
                    // Эта проверка может быть излишней, если ViewModel работает правильно.
                    // if (isContentSmall) {
                    //    Log.d("TimelineCalendar", "Content might be too small ($visibleItemsCount/$totalItemsCount), ensuring range load")
                    //    // Можно передать флаг или вызвать другую функцию в VM, если нужно
                    // }
                } else {
                    Log.d("TimelineCalendar", "No visible items or no sorted dates yet.")
                    // Можно вызвать onVisibleDatesChanged с null или не вызывать вовсе?
                    // Лучше не вызывать, пока нет дат.
                }
            }
    }

    LaunchedEffect(listState, sortedDates, initialScrollTargetDate, initialScrollDone) {
        // Выполняем только если даты есть И начальный скролл еще НЕ был выполнен
        if (sortedDates.isNotEmpty() && !initialScrollDone) {
            val targetIndex = sortedDates.indexOf(initialScrollTargetDate)
            if (targetIndex != -1) {
                // Прокручиваем к этому индексу СРАЗУ
                listState.scrollToItem(index = targetIndex, scrollOffset = 0)
                initialScrollDone = true // <<< Устанавливаем флаг, что скролл выполнен
                Log.d(
                    "TimelineCalendar",
                    "Initial scroll performed to index $targetIndex for date $initialScrollTargetDate"
                )
            } else {
                Log.w(
                    "TimelineCalendar",
                    "Initial scroll target date $initialScrollTargetDate not found in loaded dates."
                )
                // Можно добавить фоллбэк, но главное - установить флаг, если попытка была
                initialScrollDone =
                    true // Устанавливаем флаг даже если не нашли, чтобы не пытаться снова
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp) // Отступы сверху/снизу списка
    ) {
        itemsIndexed(
            items = sortedDates,
            key = { _, date -> date.toEpochDay() } // Ключ обязателен для стабильности
        ) { index, date ->

            // Получаем DayData для текущей даты
            when (val dayData = eventsData[date]) {

                // --- Состояние: Данные Загружены ---
                is DayData.Loaded -> {
                    // Разделяем события на "весь день" и "по времени"
                    // Используем remember для оптимизации partition
                    val (allDayEvents, timedEvents) = remember(dayData.events) {
                        dayData.events.partition { event ->
                            isAllDayEvent(event.startTime, event.endTime)
                        }
                    }

                    // --- Компонент для Загруженного Дня ---
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 1. Разделитель Дня (Дата + События на весь день)
                        DaySeparatorItem(
                            date = date,
                            allDayEvents = allDayEvents
                        )

                        // 2. Блок с Событиями по Времени
                        if (timedEvents.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp)) // Отступ

                            // TODO: ЗАМЕНИТЬ ЭТО НА РЕАЛЬНЫЙ DayEventsBlock !!!
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    "--- Заглушка DayEventsBlock ---",
                                    style = typography.labelSmall
                                )
                                timedEvents.forEach { event ->
                                    Text(
                                        "  - ${event.summary} (${
                                            formatEventTime(
                                                event.startTime,
                                                event.endTime
                                            )
                                        })"
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            // ----------------------------------------------------

                            /* // Когда DayEventsBlock будет готов:
                            DayEventsBlock(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp), // Отступы для блока событий
                                events = timedEvents
                            )
                            */
                        } else {
                            // Если временных событий нет, отступ до следующего дня
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    } // Конец Column для загруженного дня
                } // Конец DayData.Loaded

                // --- Состояние: Данные Загружаются ---
                is DayData.Loading -> {
                    DayLoadingIndicator(date = date) // Показываем индикатор загрузки
                }

                // --- Состояние: Данные Отсутствуют (не должно быть, но на всякий случай) ---
                null -> {
                    // Отображаем что-то или логируем ошибку
                    Log.e("TimelineCalendar", "DayData is null for date $date in itemsIndexed.")
                    // Можно показать пустой разделитель дня или сообщение об ошибке
                    DaySeparatorItem(date = date, allDayEvents = emptyList())
                    Text("Ошибка загрузки данных для этой даты", modifier = Modifier.padding(16.dp))
                }

            } // Конец when(dayData)
        } // Конец itemsIndexed
    }
}

    // --- Вспомогательная функция для определения "весь день" ---
// TODO: Реализовать более надежно, возможно, нужен флаг с бэкенда
// Эта версия очень упрощенная
private fun isAllDayEvent(startTimeStr: String?, endTimeStr: String?): Boolean {
    if (startTimeStr == null) return false
    return try {
        // Если startTime парсится как LocalDate (нет времени), считаем "весь день"
        LocalDate.parse(startTimeStr)
        true // Успешно распарсили как дату без времени
    } catch (e: DateTimeParseException) {
        // Если не парсится как LocalDate, значит есть время - не "весь день"
        false
    }
    // Более сложная проверка: если startTime и endTime - это dateTime,
    // и разница между ними >= 24 часа и startTime = 00:00, endTime = 00:00 след. дня
}

// --- Вспомогательная функция для форматирования времени (заглушка) ---
// Используй свою функцию из ViewModel или передай ее как параметр
private fun formatEventTime(startTimeStr: String?, endTimeStr: String?): String {
    // Заглушка, замени на реальное форматирование
    val start = startTimeStr?.substringAfter('T')?.substringBefore('+')?.substringBefore('Z') ?: ""
    val end = endTimeStr?.substringAfter('T')?.substringBefore('+')?.substringBefore('Z') ?: ""
    return "$start - $end"
}

// --- Заглушки для Компонентов Дня ---

@Composable
fun DaySeparatorItem(
    date: LocalDate,
    allDayEvents: List<CalendarEvent>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp) // Отступы для разделителя
    ) {
        // Отображение Даты
        Text(
            text = date.format(dayFormatter).replaceFirstChar { it.titlecase(Locale("ru")) }, // Форматируем и делаем первую букву заглавной
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium, // Используй стили из MaterialTheme
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            // modifier = Modifier.padding(bottom = 4.dp)
        )

        // Отображение Событий на весь день (если есть)
        if (allDayEvents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                allDayEvents.forEach { event ->
                    // TODO: Заменить на красивую карточку для AllDayEvent
                    Text(
                        text = "- ${event.summary} (Весь день)",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        // Можно добавить горизонтальный разделитель Divider() здесь, если нужно
        // androidx.compose.material3.Divider(modifier = Modifier.padding(top = 8.dp))
    }
}

// Заглушка для будущего сложного компонента
@Composable
fun DayEventsBlock(
    modifier: Modifier = Modifier,
    events: List<CalendarEvent>
) {
    Box(modifier = modifier.height(200.dp)) { // Просто Box с фиксированной высотой пока
        Text("Здесь будут события с временной шкалой для ${events.firstOrNull()?.startTime?.let { parseIsoToLocalDate(it) }}")
        // TODO: Реализовать сложный Layout для событий
    }
}

@Composable
fun DayLoadingIndicator(date: LocalDate, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = date.format(dayFormatter).replaceFirstChar { it.titlecase(Locale("ru")) },
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Добавь сюда ProgressBar или другой индикатор
        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(16.dp)) // Отступ снизу
    }
}