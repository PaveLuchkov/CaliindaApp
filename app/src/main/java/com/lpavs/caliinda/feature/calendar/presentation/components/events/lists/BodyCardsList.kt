package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.core.data.remote.agent.domain.AgentResponseContent
import com.lpavs.caliinda.core.data.remote.agent.domain.DaysPlan
import com.lpavs.caliinda.core.data.remote.agent.domain.ErrorResponse
import com.lpavs.caliinda.core.data.remote.agent.domain.SuggestionPlan
import com.lpavs.caliinda.core.data.remote.agent.domain.TextMessageResponse
import com.lpavs.caliinda.core.data.remote.calendar.dto.EventDto
import com.lpavs.caliinda.feature.calendar.data.EventUiModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.agent.AgentMessageItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.agent.AgentRecommendItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarEventItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.LogInEvent
import java.time.LocalDate

@Composable
fun BodyCardsList(
    events: List<EventUiModel>,
    listState: LazyListState,
    date: LocalDate,
    isSignIn: Boolean,
    onDeleteRequest: (EventDto) -> Unit,
    onEditRequest: (EventDto) -> Unit,
    onDetailsRequest: (EventDto) -> Unit,
    onSignInClick: () -> Unit,
    onSessionDelete: () -> Unit,
    onPlanConfirm: (String) -> Unit,
    agentResponse: AgentResponseContent?
) {
  var expandedEventId by remember { mutableStateOf<String?>(null) }
    var expandedAgentId by remember { mutableStateOf<String?>(null) }
  var expandedAgent by remember { mutableStateOf(false) }
  val haptic = LocalHapticFeedback.current
  val highlightedInfo = (agentResponse as? TextMessageResponse)?.highlightedEventInfo ?: emptyMap()
    val today = LocalDate.now()
  LazyColumn(
      modifier = Modifier.fillMaxSize(),
      state = listState,
      contentPadding = PaddingValues(bottom = 100.dp)) {
        if (isSignIn) {
          item { LogInEvent(onSignInClick = onSignInClick) }
        } else {
                when (agentResponse) {
                    is TextMessageResponse -> {
                        if (date == today) {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            item {
                                AgentMessageItem(
                                    message = agentResponse.mainText,
                                    isExpanded = expandedAgent,
                                    onToggleExpand = { expandedAgent = !expandedAgent },
                                    onSessionDelete = onSessionDelete
                                )
                            }
                        }
                    }
                    is DaysPlan -> {
                        item {
                            AgentMessageItem(
                                message = agentResponse.mainText,
                                isExpanded = expandedAgent,
                                onToggleExpand = { expandedAgent = !expandedAgent },
                                onSessionDelete = onSessionDelete
                            )
                        }
                    }

                    is SuggestionPlan -> {
                        if (date == today) {
                            item {
                                AgentMessageItem(
                                    message = agentResponse.mainText,
                                    isExpanded = expandedAgent,
                                    onToggleExpand = { expandedAgent = !expandedAgent },
                                    onSessionDelete = onSessionDelete
                                )
                            }
                            val suggestionItems = agentResponse.suggestionItems
                            items(
                                items = suggestionItems,
                                key = { suggestion -> suggestion.hashCode() }) { suggestion ->
                                val isExpandedAgent = suggestion.title == expandedAgentId
                                AgentRecommendItem(
                                    suggestion = suggestion,
                                    isExpanded = isExpandedAgent,
                                    onToggleExpand = {
                                        expandedAgentId =
                                            if (expandedAgentId == suggestion.title) {
                                                null
                                            } else {
                                                suggestion.title
                                            }
                                    },
                                    onConfirm = onPlanConfirm
                                )
                            }
                        }

                    }

                    is ErrorResponse -> {
                        item {
                            AgentMessageItem(
                                message = agentResponse.mainText,
                                isExpanded = expandedAgent,
                                onToggleExpand = { expandedAgent = !expandedAgent },
                                onSessionDelete = onSessionDelete
                            )
                        }
                    }

                    null -> {}
                }
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
                    slideInVertically(
                        initialOffsetY = { it / 2 }, animationSpec = sliderSpringSpec),
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
                  val highlightAction = highlightedInfo[event.id]
                  CalendarEventItem(
                      uiModel = event,
                      isExpanded = isExpanded,
                      highlightAction = highlightAction,
                      onToggleExpand = {
                        expandedEventId =
                            if (expandedEventId == event.id) {
                              null
                            } else {
                              event.id
                            }
                      },
                      onDeleteClickFromList = { onDeleteRequest(event.originalEvent) },
                      onEditClickFromList = { onEditRequest(event.originalEvent) },
                      onDetailsClickFromList = { onDetailsRequest(event.originalEvent) },
                  )
                }
          }
        }
      }
  Box(modifier = Modifier.height(70.dp))
}
