package com.lpavs.caliinda.feature.calendar.ui.components.events

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
    events: List<EventUiModel>,
    listState: LazyListState,
    onDeleteRequest: (EventDto) -> Unit,
    onEditRequest: (EventDto) -> Unit,
    onDetailsRequest: (EventDto) -> Unit,
) {

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

              EventItem(
                  uiModel = event,
                  isExpanded = isExpanded,
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
                  // --------------------------------
                  modifier =
                      Modifier
                          .fillMaxWidth()
                          .padding(
                              horizontal = CalendarUiDefaults.ItemHorizontalPadding,
                              vertical = CalendarUiDefaults.ItemVerticalPadding
                          ),
              )
              }
        }
      }
  Box(modifier = Modifier.height(70.dp))
}
