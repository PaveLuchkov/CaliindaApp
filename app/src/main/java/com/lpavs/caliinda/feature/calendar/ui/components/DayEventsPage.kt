package com.lpavs.caliinda.feature.calendar.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.util.DateTimeFormatterUtil
import com.lpavs.caliinda.data.calendar.EventNetworkState
import com.lpavs.caliinda.data.local.DateTimeUtils
import com.lpavs.caliinda.ui.screens.main.CalendarEvent
import com.lpavs.caliinda.ui.screens.main.MainViewModel
import com.lpavs.caliinda.feature.event_management.ui.shared.DeleteConfirmationDialog
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringEventDeleteOptionsDialog
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DayEventsPage(
    date: LocalDate,
    viewModel: MainViewModel,
) {
    val eventsFlow = remember(date) { viewModel.getEventsFlowForDate(date) }
    val eventsState =
        eventsFlow.collectAsStateWithLifecycle(
            initialValue = emptyList()
        )
    val events = eventsState.value
    Log.d(
        "DayEventsPage",
        "Events received from flow: ${events.joinToString { it.summary + " (allDay=" + it.isAllDay + ")" }}"
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTimeZoneId by viewModel.timeZone.collectAsStateWithLifecycle()

    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()

    val isToday = date == LocalDate.now()

    val (allDayEvents, timedEvents) =
        remember(events, currentTimeZoneId) { // Добавим зависимость от пояса
            val (allDay, timed) = events.partition { it.isAllDay } // Используем флаг isAllDay
            val sortedTimed =
                timed.sortedBy { event ->
                    DateTimeUtils.parseToInstant(event.startTime, currentTimeZoneId) ?: Instant.MAX
                }
            allDay to sortedTimed
        }
    Log.d("DayEventsPage", "Partitioned: AllDay=${allDayEvents.size}, Timed=${timedEvents.size}")

    val nextStartTime: Instant? =
        remember(timedEvents, currentTime, isToday, currentTimeZoneId) {
            if (!isToday) null
            else {
                timedEvents.firstNotNullOfOrNull { event ->
                    val start = DateTimeUtils.parseToInstant(event.startTime, currentTimeZoneId)
                    if (start != null && start.isAfter(currentTime)) start else null
                }
            }
        }
    val context = LocalContext.current

    // --- ОПРЕДЕЛЯЕМ ЦЕЛЕВОЙ ИНДЕКС ДЛЯ ПРОКРУТКИ ---
    val targetScrollIndex =
        remember(timedEvents, currentTime, nextStartTime, isToday, currentTimeZoneId) {
            if (!isToday || timedEvents.isEmpty()) -1
            else {
                val currentEventIndex =
                    timedEvents.indexOfFirst { event ->
                        val start = DateTimeUtils.parseToInstant(event.startTime, currentTimeZoneId)
                        val end = DateTimeUtils.parseToInstant(event.endTime, currentTimeZoneId)
                        start != null &&
                                end != null &&
                                !currentTime.isBefore(start) &&
                                currentTime.isBefore(end)
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
    val rangeNetworkState by viewModel.rangeNetworkState.collectAsStateWithLifecycle()
    val isBusy = uiState.isLoading || rangeNetworkState is EventNetworkState.Loading
    val isListening = uiState.isListening

    LaunchedEffect(
        targetScrollIndex,
        isToday
    ) { // Запускаем, если изменился индекс или флаг isToday
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
    var expandedAllDayEventId by remember { mutableStateOf<String?>(null) }
    //    val fixedColors = LocalFixedAccentColors.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(3.dp))
            if (allDayEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp)) // Отступ после заголовка даты
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp) // Общий горизонтальный отступ
                ) {
                    allDayEvents.forEach { event ->
                        val isExpanded = event.id == expandedAllDayEventId
                        AllDayEventItem(
                            event = event,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedAllDayEventId =
                                    if (expandedAllDayEventId == event.id) {
                                        null
                                    } else {
                                        event.id
                                    }
                            },
                            onDeleteClick = {
                                viewModel.requestDeleteConfirmation(event)
                            },
                            onDetailsClick = {
                                viewModel.requestEventDetails(event)
                            },
                            onEditClick = {
                                viewModel.requestEditEvent(event)
                            },
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (timedEvents.isNotEmpty()) {
                val timeFormatterLambda: (CalendarEvent) -> String =
                    remember(viewModel, currentTimeZoneId) {
                        { event ->
                            DateTimeFormatterUtil.formatEventListTime(
                                context,
                                event,
                                currentTimeZoneId
                            )
                        }
                    }
                CardsList(
                    events = timedEvents,
                    timeFormatter = timeFormatterLambda,
                    isToday = isToday,
                    nextStartTime = nextStartTime,
                    currentTime = currentTime,
                    listState = listState,
                    onDeleteRequest = viewModel::requestDeleteConfirmation,
                    onEditRequest = viewModel::requestEditEvent,
                    onDetailsRequest = viewModel::requestEventDetails,
                    currentTimeZoneId = currentTimeZoneId
                )
            } else if (allDayEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isBusy && !isListening) {
                        LoadingIndicator(modifier = Modifier.size(80.dp))
                    } else {
                        Box(
                            modifier =
                                Modifier.shadow(
                                    elevation = 5.dp,
                                    shape = RoundedCornerShape(CalendarUiDefaults.EventItemCornerRadius),
                                    clip = false,
                                )
                                    .clip(RoundedCornerShape(CalendarUiDefaults.EventItemCornerRadius))
                                    .background(color = MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(16.dp),
                            contentAlignment = Alignment.Center // Центрируем сообщение
                        ) {
                            Text(
                                stringResource(R.string.no_events),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            if (uiState.showDeleteConfirmationDialog && uiState.eventPendingDeletion != null) {
                DeleteConfirmationDialog(
                    onConfirm = { viewModel.confirmDeleteEvent() },
                    onDismiss = { viewModel.cancelDelete() })
            } else if (uiState.showRecurringDeleteOptionsDialog && uiState.eventPendingDeletion != null) {
                RecurringEventDeleteOptionsDialog(
                    eventName = uiState.eventPendingDeletion!!.summary,
                    onDismiss = { viewModel.cancelDelete() },
                    onOptionSelected = { choice -> viewModel.confirmRecurringDelete(choice) })
            }
        }
    }
}