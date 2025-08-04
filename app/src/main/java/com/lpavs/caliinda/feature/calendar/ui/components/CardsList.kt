package com.lpavs.caliinda.feature.calendar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.util.DateTimeUtils.parseToInstant
import java.time.Duration
import java.time.Instant

data class GeneratedShapeParams(
    val numVertices: Int,
    val radiusSeed: Float,
    val rotationAngle: Float,
    val shadowOffsetYSeed: Dp,
    val shadowOffsetXSeed: Dp,
    val offestParam: Float,
)

@Composable
fun CardsList(
    events: List<EventDto>,
    timeFormatter: (EventDto) -> String,
    currentTime: Instant,
    isToday: Boolean,
    currentTimeZoneId: String,
    listState: LazyListState,
    nextStartTime: Instant?,
    onDeleteRequest: (EventDto) -> Unit,
    onEditRequest: (EventDto) -> Unit,
    onDetailsRequest: (EventDto) -> Unit,
) {
  val transitionWindowDurationMillis = remember {
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
              visible = true,
              enter =
                  slideInVertically(initialOffsetY = { it / 2 }, animationSpec = sliderSpringSpec),
              exit =
                  fadeOut(animationSpec = fadeSpringSpec) +
                      slideOutVertically(
                          targetOffsetY = { it / 2 }, animationSpec = sliderSpringSpec),
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

                val buttonsRowHeight = 56.dp
                val expandedAdditionalHeight =
                    remember(isMicroEvent) {
                      if (isMicroEvent && baseHeight < buttonsRowHeight * 1.5f) {
                        buttonsRowHeight * 1.2f
                      } else {
                        buttonsRowHeight
                      }
                    }

                val expandedCalculatedHeight =
                    remember(baseHeight, expandedAdditionalHeight) {
                      if (eventDurationMinutes > 120 && !isMicroEvent) {
                        (baseHeight + expandedAdditionalHeight * 0.9f).coerceAtLeast(baseHeight)
                      } else {
                        baseHeight + expandedAdditionalHeight
                      }
                    }

                val animatedHeight by
                    animateDpAsState(
                        targetValue = if (isExpanded) expandedCalculatedHeight else baseHeight,
                        animationSpec = tween(durationMillis = 250),
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
                val isNext =
                    remember(event.startTime, nextStartTime, currentTimeZoneId) {
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
                          if (!isToday) {
                            0f
                          } else {
                            val start = parseToInstant(event.startTime, currentTimeZoneId)
                            if (start == null || currentTime.isAfter(start)) {
                              0f
                            } else {
                              val timeUntilStartMillis =
                                  Duration.between(currentTime, start).toMillis()
                              if (timeUntilStartMillis > transitionWindowDurationMillis ||
                                  transitionWindowDurationMillis <= 0) {
                                0f
                              } else {
                                (1.0f -
                                        (timeUntilStartMillis.toFloat() /
                                            transitionWindowDurationMillis.toFloat()))
                                    .coerceIn(0f, 1f)
                              }
                            }
                          }
                        }

                EventItem(
                    event = event,
                    timeFormatter = timeFormatter,
                    isCurrentEvent = isCurrent,
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
                    onEditClickFromList = { onEditRequest(event) },
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
