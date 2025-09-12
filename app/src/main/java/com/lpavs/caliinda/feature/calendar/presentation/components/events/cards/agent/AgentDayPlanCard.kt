package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.core.data.remote.agent.ScheduledEvent
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.CaliindaTheme
import com.lpavs.caliinda.core.ui.theme.Typography
import com.lpavs.caliinda.core.ui.theme.cuid

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun AgentDayPlanItem(
    event: ScheduledEvent,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    val cardBackground = colorScheme.secondaryContainer
    val cardTextColor = colorScheme.onSecondaryContainer
    val textStyle = Typography.headlineSmall
    val darkerShadowColor = Color.Black
    val cardElevation = cuid.CurrentEventElevation
    val borderColor = colorScheme.tertiary

    Box(
        modifier = modifier
            .padding(
                horizontal = CalendarUiDefaults.ItemHorizontalPadding,
                vertical = CalendarUiDefaults.ItemVerticalPadding
            )
            .shadow(
                elevation = cardElevation,
                shape = RoundedCornerShape(cuid.EventItemCornerRadius),
                clip = false,
                ambientColor = if (cardElevation > 0.dp) darkerShadowColor else Color.Transparent,
                spotColor = if (cardElevation > 0.dp) darkerShadowColor else Color.Transparent
            )
            .clip(RoundedCornerShape(cuid.EventItemCornerRadius))
            .border(
                BorderStroke(2.dp, borderColor),
                shape = RoundedCornerShape(cuid.EventItemCornerRadius)
            )
            .background(cardBackground)
            .animateContentSize( // Заменил height(animatedHeight) на animateContentSize
                animationSpec = tween(durationMillis = 250),
                alignment = Alignment.TopStart
            )
            .pointerInput(event.id) {
                detectTapGestures(
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onToggleExpand()
                    },
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = cuid.ItemHorizontalPadding,
                    vertical = cuid.StandardItemContentVerticalPadding
                )
        ) {
            Text(
                text = event.title,
                color = cardTextColor,
                style = textStyle,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2, // Адаптивное количество строк
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${event.startTime} - ${event.endTime}",
                color = cardTextColor,
                style = typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                maxLines = 1
            )

            // Дополнительный контент при развернутом состоянии
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Здесь можно добавить дополнительную информацию о событии
                    if (event.description?.isNotEmpty() == true) {
                        Text(
                            text = event.description,
                            color = cardTextColor.copy(alpha = 0.8f),
                            style = typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Collapsed State", showBackground = true, widthDp = 360,
    wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE
)
@Composable
fun AgentDayPlanItemPreview_Collapsed() {
  val fakeEvent =
      ScheduledEvent(
          id = "evt1",
          startTime = "10:00",
          endTime = "11:30",
          title = "Планирование спринта",
          description = "Обсудить задачи на следующую неделю и распределить ресурсы.")
    CaliindaTheme() {
      Column(modifier = Modifier.padding(16.dp)) {
        AgentDayPlanItem(event = fakeEvent, isExpanded = false, onToggleExpand = {})
      }
    }
}
