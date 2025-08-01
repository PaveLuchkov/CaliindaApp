package com.lpavs.caliinda.ui.screens.main.components.calendar

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.data.calendar.EventNetworkState
import com.lpavs.caliinda.data.local.DateTimeUtils.parseToInstant
import com.lpavs.caliinda.ui.screens.main.CalendarEvent
import com.lpavs.caliinda.ui.screens.main.MainViewModel
import com.lpavs.caliinda.ui.screens.main.shared.CalendarUiDefaults
import com.lpavs.caliinda.ui.screens.main.shared.cuid
import com.lpavs.caliinda.ui.screens.main.components.calendar.eventmanaging.ui.DeleteConfirmationDialog
import com.lpavs.caliinda.ui.screens.main.components.calendar.eventmanaging.ui.RecurringEventDeleteOptionsDialog
import com.lpavs.caliinda.core.ui.util.DateTimeFormatterUtil
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

data class GeneratedShapeParams(
    val numVertices: Int,
    val radiusSeed: Float,
    val rotationAngle: Float,
    val shadowOffsetYSeed: Dp,
    val shadowOffsetXSeed: Dp,
    val offestParam: Float,
)

@Composable
fun EventsList(
    events: List<CalendarEvent>,
    timeFormatter: (CalendarEvent) -> String,
    currentTime: Instant,
    isToday: Boolean,
    currentTimeZoneId: String,
    listState: LazyListState,
    nextStartTime: Instant?,
    onDeleteRequest: (CalendarEvent) -> Unit,
    onEditRequest: (CalendarEvent) -> Unit,
    onDetailsRequest: (CalendarEvent) -> Unit,
) {
  val transitionWindowDurationMillis = remember { // Запоминаем длительность окна в мс
    Duration.ofMinutes(cuid.EVENT_TRANSITION_WINDOW_MINUTES).toMillis()
  }

  var expandedEventId by remember { mutableStateOf<String?>(null) }

  LazyColumn(
      modifier = Modifier.fillMaxSize(),
      state = listState,
      contentPadding = PaddingValues(bottom = 100.dp)) {
        items(items = events, key = { event -> event.id }) { event ->
          val fadeSpringSpec =
              spring<Float>(
                  dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
          val sliderSpringSpec =
              spring<IntOffset>(
                  dampingRatio = Spring.DampingRatioHighBouncy,
                  stiffness = Spring.StiffnessMediumLow)
          val popUndUpSpec =
              spring<IntOffset>(
                  dampingRatio = Spring.DampingRatioMediumBouncy,
                  stiffness = Spring.StiffnessMediumLow)
          AnimatedVisibility(
              visible = true, // Всегда true, т.к. если элемента нет в `events`, он не будет здесь.
              // AnimatedVisibility здесь для управления enter/exit анимацией.
              enter =
                  slideInVertically(
                      initialOffsetY = { it / 2 }, // Начать с половины высоты выше/ниже
                      animationSpec = sliderSpringSpec),
              exit =
                  fadeOut(animationSpec = fadeSpringSpec) +
                      slideOutVertically(
                          targetOffsetY = { it / 2 }, animationSpec = sliderSpringSpec),
              // Этот модификатор анимирует позицию элемента при изменении списка
              // (когда другие элементы добавляются/удаляются)
              modifier =
                  Modifier.animateItem(
                      placementSpec = popUndUpSpec,
                      fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                      fadeOutSpec = spring(stiffness = Spring.StiffnessHigh))) {
                val isExpanded = event.id == expandedEventId

                val eventDurationMinutes =
                    remember(event.startTime, event.endTime, currentTimeZoneId) {
                      val start = parseToInstant(event.startTime, currentTimeZoneId)
                      val end = parseToInstant(event.endTime, currentTimeZoneId)
                      if (start != null && end != null && end.isAfter(start)) {
                        Duration.between(start, end).toMinutes()
                      } else {
                        0L
                      }
                    }

                val isMicroEvent =
                    remember(eventDurationMinutes) {
                      eventDurationMinutes > 0 &&
                          eventDurationMinutes <= cuid.MicroEventMaxDurationMinutes
                    }

                val baseHeight =
                    remember(isMicroEvent, eventDurationMinutes) {
                      calculateEventHeight(eventDurationMinutes, isMicroEvent)
                    }

                val buttonsRowHeight = 56.dp // Увеличил немного для стандартных кнопок
                val expandedAdditionalHeight =
                    remember(isMicroEvent) {
                      // Для микро-событий можно добавить чуть меньше высоты или стандартную,
                      // в зависимости от того, как кнопки будут выглядеть.
                      // Если кнопки стандартного размера, то и добавка стандартная.
                      if (isMicroEvent &&
                          baseHeight < buttonsRowHeight * 1.5f) { // Если микро совсем маленькое
                        buttonsRowHeight * 1.2f // Чуть больше, чем сами кнопки
                      } else {
                        buttonsRowHeight
                      }
                    }

                val expandedCalculatedHeight =
                    remember(baseHeight, expandedAdditionalHeight) {
                      if (eventDurationMinutes > 120 &&
                          !isMicroEvent) { // Пример: для событий > 2 часов
                        (baseHeight + expandedAdditionalHeight * 0.9f).coerceAtLeast(
                            baseHeight) // Небольшая добавка или ничего
                      } else {
                        baseHeight + expandedAdditionalHeight
                      }
                    }

                val animatedHeight by
                    animateDpAsState(
                        targetValue = if (isExpanded) expandedCalculatedHeight else baseHeight,
                        animationSpec = tween(durationMillis = 250), // Скорость анимации
                        label = "eventItemHeightAnimation")

                val isCurrent =
                    remember(currentTime, event.startTime, event.endTime, currentTimeZoneId) {
                      val start = parseToInstant(event.startTime, currentTimeZoneId)
                      val end = parseToInstant(event.endTime, currentTimeZoneId)
                      start != null &&
                          end != null &&
                          !currentTime.isBefore(start) &&
                          currentTime.isBefore(end)
                    }
              val isPast =
                  remember(currentTime, event.endTime, currentTimeZoneId) {
                      val end = parseToInstant(event.endTime, currentTimeZoneId)
                          end != null &&
                          currentTime.isAfter(end)
                  }

                val isNext =
                    remember(event.startTime, nextStartTime, currentTimeZoneId) {
                      // isNext вычисляется ТОЛЬКО если nextStartTime не null (т.е. мы на сегодня и
                      // следующее событие есть)
                      if (nextStartTime == null) false
                      else {
                        val currentEventStart = parseToInstant(event.startTime, currentTimeZoneId)
                        currentEventStart != null && currentEventStart == nextStartTime
                      }
                    }

                val proximityRatio =
                    remember(
                        currentTime,
                        event.startTime,
                        isToday,
                        currentTimeZoneId,
                        transitionWindowDurationMillis) {
                          // Коэффициент рассчитывается только для СЕГОДНЯ и для БУДУЩИХ событий
                          if (!isToday) {
                            0f // Не сегодня - нет перехода
                          } else {
                            val start = parseToInstant(event.startTime, currentTimeZoneId)
                            if (start == null || currentTime.isAfter(start)) {
                              // Событие в прошлом или не парсится - максимальный переход (или без
                              // перехода?)
                              // Если хотим переход только для будущих, то 0f. Если для
                              // текущих/прошлых тоже, то 1f.
                              // Сделаем 0f для простоты - переход только для будущих.
                              0f
                            } else {
                              val timeUntilStartMillis =
                                  Duration.between(currentTime, start).toMillis()
                              if (timeUntilStartMillis > transitionWindowDurationMillis ||
                                  transitionWindowDurationMillis <= 0) {
                                0f // Слишком далеко или некорректное окно - нет перехода
                              } else {
                                // Рассчитываем коэффициент: 1.0 (близко) -> 0.0 (далеко в пределах
                                // окна)
                                (1.0f -
                                        (timeUntilStartMillis.toFloat() /
                                            transitionWindowDurationMillis.toFloat()))
                                    .coerceIn(0f, 1f)
                              }
                            }
                          }
                        }

                //    Spacer(modifier = Modifier.height(2.dp))
                EventListItem(
                    event = event,
                    timeFormatter = timeFormatter,
                    isCurrentEvent = isCurrent,
                    isPastEvent = isPast,
                    isNextEvent = isNext,
                    proximityRatio = proximityRatio,
                    isMicroEventFromList = isMicroEvent,
                    targetHeightFromList = animatedHeight,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                      expandedEventId =
                          if (expandedEventId == event.id) {
                            null
                          } else {
                            event.id
                          }
                    },
                    onDeleteClickFromList = { onDeleteRequest(event) },
                    onEditClickFromList = {
                      onEditRequest(event)
                      //                        expandedEventId = null // Схлопываем после действия
                    },
                    onDetailsClickFromList = { onDetailsRequest(event) },
                    // --------------------------------
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(
                                horizontal = CalendarUiDefaults.ItemHorizontalPadding,
                                vertical = CalendarUiDefaults.ItemVerticalPadding),
                    currentTimeZoneId = currentTimeZoneId)
              }
        }
      }
  Box(modifier = Modifier.height(70.dp))
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DayEventsPage(
    date: LocalDate,
    viewModel: MainViewModel,
) {
  val eventsFlow = remember(date) { viewModel.getEventsFlowForDate(date) }
  val eventsState =
      eventsFlow.collectAsStateWithLifecycle(
          initialValue = emptyList()) // Returns State<List<CalendarEvent>>
  val events = eventsState.value
  Log.d(
      "DayEventsPage",
      "Events received from flow: ${events.joinToString { it.summary + " (allDay=" + it.isAllDay + ")" }}")
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val currentTimeZoneId by viewModel.timeZone.collectAsStateWithLifecycle()

  val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()

  val isToday = date == LocalDate.now()

  val (allDayEvents, timedEvents) =
      remember(events, currentTimeZoneId) { // Добавим зависимость от пояса
        val (allDay, timed) = events.partition { it.isAllDay } // Используем флаг isAllDay
        val sortedTimed =
            timed.sortedBy { event ->
              parseToInstant(event.startTime, currentTimeZoneId) ?: Instant.MAX
            }
        allDay to sortedTimed
      }
  Log.d("DayEventsPage", "Partitioned: AllDay=${allDayEvents.size}, Timed=${timedEvents.size}")

  val nextStartTime: Instant? =
      remember(timedEvents, currentTime, isToday, currentTimeZoneId) {
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
  val targetScrollIndex =
      remember(timedEvents, currentTime, nextStartTime, isToday, currentTimeZoneId) {
        if (!isToday || timedEvents.isEmpty()) -1
        else {
          val currentEventIndex =
              timedEvents.indexOfFirst { event ->
                val start = parseToInstant(event.startTime, currentTimeZoneId)
                val end = parseToInstant(event.endTime, currentTimeZoneId)
                start != null &&
                    end != null &&
                    !currentTime.isBefore(start) &&
                    currentTime.isBefore(end)
              }
          if (currentEventIndex != -1) currentEventIndex
          else if (nextStartTime != null) {
            timedEvents.indexOfFirst { event ->
              val start = parseToInstant(event.startTime, currentTimeZoneId)
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
  var expandedAllDayEventId by remember { mutableStateOf<String?>(null) }
  //    val fixedColors = LocalFixedAccentColors.current

  Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      Spacer(modifier = Modifier.height(3.dp))
      if (allDayEvents.isNotEmpty()) {
        Spacer(modifier = Modifier.height(3.dp)) // Отступ после заголовка даты
        Column(
            modifier =
                Modifier.fillMaxWidth().padding(horizontal = 16.dp) // Общий горизонтальный отступ
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
                      // expandedAllDayEventId = null // Схлопываем после запроса на удаление
                    },
                    onDetailsClick = {
                      viewModel.requestEventDetails(event)
                      // expandedAllDayEventId = null // Схлопываем после запроса на удаление
                    },
                    onEditClick = { // Переименовал колбэк
                      viewModel.requestEditEvent(event) // Вызываем оригинальный onEditRequest
                      //                        expandedEventId = null // Схлопываем после действия
                    },
                    // modifier = Modifier.padding(vertical = 2.dp) // Небольшой отступ между
                    // AllDayEventItem, если нужно
                )
                Spacer(modifier = Modifier.height(6.dp)) // Отступ между элементами "весь день"
              }
            }
      }
      Spacer(modifier = Modifier.height(8.dp))

      // Список Событий для этого дня
      if (timedEvents.isNotEmpty()) {
        val timeFormatterLambda: (CalendarEvent) -> String =
            remember(viewModel, currentTimeZoneId) {
              { event ->
                DateTimeFormatterUtil.formatEventListTime(context, event, currentTimeZoneId)
              } // Передаем оба параметра
            }
        EventsList(
            events = timedEvents,
            timeFormatter = timeFormatterLambda,
            isToday = isToday,
            nextStartTime = nextStartTime,
            currentTime = currentTime,
            listState = listState,
            // --- Убедись, что имена параметров соответствуют определению EventsList ---
            onDeleteRequest = viewModel::requestDeleteConfirmation,
            onEditRequest = viewModel::requestEditEvent,
            onDetailsRequest = viewModel::requestEventDetails,
            currentTimeZoneId = currentTimeZoneId)
      } else if (allDayEvents.isEmpty()) {
        // Показываем сообщение "нет событий", только если НЕТ НИКАКИХ событий
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isBusy && !isListening) {
                LoadingIndicator(modifier = Modifier.size(80.dp))
            } else {
                Box( // Корневой Box для тени, фона, высоты и кликабельности
                    modifier =
                        Modifier.shadow(
                            elevation = 5.dp,
                            shape = RoundedCornerShape(cuid.EventItemCornerRadius),
                            clip = false,
                        )
                            .clip(RoundedCornerShape(cuid.EventItemCornerRadius))
                            .background(color = colorScheme.secondaryContainer)
                            .padding(16.dp),
                    contentAlignment = Alignment.Center // Центрируем сообщение
                ) {
                    Text(
                        stringResource(R.string.no_events),
                        style = typography.bodyLarge,
                        color = colorScheme.onSecondaryContainer)
                }
            }
        }
      } else {
        Spacer(modifier = Modifier.weight(1f))
      }
    } // End Column
    if (uiState.showDeleteConfirmationDialog && uiState.eventPendingDeletion != null) {
      DeleteConfirmationDialog(
          onConfirm = { viewModel.confirmDeleteEvent() }, onDismiss = { viewModel.cancelDelete() })
    } else if (uiState.showRecurringDeleteOptionsDialog && uiState.eventPendingDeletion != null) {
      RecurringEventDeleteOptionsDialog(
          eventName = uiState.eventPendingDeletion!!.summary,
          onDismiss = { viewModel.cancelDelete() },
          onOptionSelected = { choice -> viewModel.confirmRecurringDelete(choice) })
    }
  } // End Box
}
