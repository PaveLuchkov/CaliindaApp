package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.core.data.remote.calendar.dto.EventDto
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.AllDayEventItem

@Composable
fun HeadCardsList(
    events: List<EventDto>,
    onDeleteRequest: (EventDto) -> Unit,
    onEditRequest: (EventDto) -> Unit,
    onDetailsRequest: (EventDto) -> Unit,
) {
  var expandedEventId by remember { mutableStateOf<String?>(null) }

  LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    items(items = events, key = { event -> event.id }) { event ->
      val fadeSpringSpec =
          spring<Float>(
              dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
      val sliderSpringSpec =
          spring<IntOffset>(
              dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMediumLow)
      val popUndUpSpec =
          spring<IntOffset>(
              dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

      AnimatedVisibility(
          visible = true,
          enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = sliderSpringSpec),
          exit =
              fadeOut(animationSpec = fadeSpringSpec) +
                  slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = sliderSpringSpec),
          modifier =
              Modifier.animateItem(
                  placementSpec = popUndUpSpec,
                  fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                  fadeOutSpec = spring(stiffness = Spring.StiffnessHigh))) {
            val isExpanded = event.id == expandedEventId

            AllDayEventItem(
                event = event,
                isExpanded = isExpanded,
                onToggleExpand = {
                  expandedEventId =
                      if (expandedEventId == event.id) {
                        null
                      } else {
                        event.id
                      }
                },
                onDeleteClick = { onDeleteRequest(event) },
                onEditClick = { onEditRequest(event) },
                onDetailsClick = { onDetailsRequest(event) },
            )
          }
    }
  }
}
