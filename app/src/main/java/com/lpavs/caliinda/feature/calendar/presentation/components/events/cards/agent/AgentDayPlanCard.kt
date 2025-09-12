package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.core.data.remote.agent.ScheduledEvent
import com.lpavs.caliinda.core.ui.theme.Typography
import com.lpavs.caliinda.core.ui.theme.cuid

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun AgentDayPlanItem(
    event: ScheduledEvent,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current

  val baseHeight = 72.dp
  val expandedHeight = 128.dp

  val animatedHeight by
      animateDpAsState(
          targetValue = if (isExpanded) expandedHeight else baseHeight,
          animationSpec = tween(durationMillis = 250),
          label = "eventItemHeightAnimation")

  val cardBackground = colorScheme.primaryContainer
  val cardTextColor = colorScheme.onPrimaryContainer
  val textStyle = Typography.headlineSmall
  val darkerShadowColor = Color.Black

  Box(
      modifier =
          modifier
              .padding(horizontal = cuid.ItemHorizontalPadding, vertical = cuid.ItemVerticalPadding)
              .shadow(
                  elevation = 2.dp,
                  shape = RoundedCornerShape(cuid.EventItemCornerRadius),
                  clip = false,
                  ambientColor = darkerShadowColor,
                  spotColor = darkerShadowColor)
              .clip(RoundedCornerShape(cuid.EventItemCornerRadius))
              .background(cardBackground)
              .height(animatedHeight)
              .fillMaxWidth()
              .pointerInput(event.id) {
                detectTapGestures(
                    onTap = {
                      haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                      onToggleExpand()
                    })
              }) {
        Column(modifier = Modifier.fillMaxSize()) {
          Box(
              modifier =
                  Modifier.weight(1f)
                      .fillMaxWidth()
                      .padding(horizontal = cuid.ItemHorizontalPadding, vertical = 8.dp),
              contentAlignment = Alignment.TopStart) {
                Column(
                    verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
                      Text(
                          text = event.title,
                          color = cardTextColor,
                          style = textStyle,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis)
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                          text = "${event.startTime} - ${event.endTime}",
                          color = cardTextColor,
                          style = typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                          maxLines = 1)
                    }
              }

          AnimatedVisibility(
              visible = isExpanded,
              enter =
                  fadeIn(animationSpec = tween(durationMillis = 150, delayMillis = 100)) +
                      expandVertically(
                          animationSpec = tween(durationMillis = 250, delayMillis = 50),
                          expandFrom = Alignment.Top),
              exit =
                  shrinkVertically(
                      animationSpec = tween(durationMillis = 250), shrinkTowards = Alignment.Top) +
                      fadeOut(animationSpec = tween(durationMillis = 150))) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = cuid.ItemHorizontalPadding, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                      Button(
                          onClick = { onConfirm(event.id) },
                          contentPadding = PaddingValues(horizontal = 16.dp)) {
                            Icon(Icons.Filled.Check, contentDescription = "Подтвердить")
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Подтвердить")
                          }
                    }
              }
        }
      }
}

@Preview(name = "Collapsed State", showBackground = true, widthDp = 360)
@Composable
fun AgentDayPlanItemPreview_Collapsed() {
  val fakeEvent =
      ScheduledEvent(
          id = "evt1",
          startTime = "10:00",
          endTime = "11:30",
          title = "Планирование спринта",
          description = "Обсудить задачи на следующую неделю и распределить ресурсы.")

  MaterialTheme {
    Column(modifier = Modifier.padding(16.dp)) {
      AgentDayPlanItem(event = fakeEvent, isExpanded = false, onToggleExpand = {}, onConfirm = {})
    }
  }
}
