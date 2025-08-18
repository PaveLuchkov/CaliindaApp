package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.cuid

@Composable
fun AllDayEventItem(
    event: EventDto,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val cardBackground = MaterialTheme.colorScheme.tertiaryContainer
  val cardTextColor = MaterialTheme.colorScheme.onTertiaryContainer
  val haptic = LocalHapticFeedback.current

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cuid.EventItemCornerRadius))
                .background(cardBackground)
                .pointerInput(event.id) {
                    detectTapGestures(
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.Companion.ContextClick)
                            onToggleExpand()
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.Companion.LongPress)
                            onDetailsClick()
                        })
                }) {
        Column(
            modifier =
                Modifier.Companion.fillMaxWidth()
                    .padding(
                        horizontal = CalendarUiDefaults.AllDayItemPadding,
                        vertical = CalendarUiDefaults.AllDayItemVerticalContentPadding
                    )
        ) {
            Text(
                text = event.summary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Companion.Medium,
                color = cardTextColor,
                textAlign = TextAlign.Companion.Center,
                modifier = Modifier.Companion.fillMaxWidth().padding(vertical = 3.dp),
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter =
                    fadeIn(animationSpec = tween(durationMillis = 150, delayMillis = 100)) +
                            expandVertically(
                                animationSpec = tween(durationMillis = 250, delayMillis = 50),
                                expandFrom = Alignment.Companion.Top
                            ),
                exit =
                    shrinkVertically(
                        animationSpec = tween(durationMillis = 250),
                        shrinkTowards = Alignment.Companion.Top
                    ) +
                            fadeOut(animationSpec = tween(durationMillis = 150))
            ) {
                Spacer(modifier = Modifier.Companion.height(8.dp))
                Row(
                    modifier = Modifier.Companion.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onDetailsClick()
                            onToggleExpand()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onTertiary,
                                contentColor = MaterialTheme.colorScheme.tertiary
                            )
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "Information",
                            modifier = Modifier.Companion.size(ButtonDefaults.IconSize)
                        )
                    }
                    Spacer(modifier = Modifier.Companion.width(8.dp))
                    Button(
                        onClick = { onEditClick() },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onTertiary,
                                contentColor = MaterialTheme.colorScheme.tertiary
                            )
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Редактировать",
                            modifier = Modifier.Companion.size(ButtonDefaults.IconSize)
                        )
                    }
                    Spacer(modifier = Modifier.Companion.width(8.dp))
                    Button(
                        onClick = { onDeleteClick() },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onTertiary,
                                contentColor = MaterialTheme.colorScheme.tertiary
                            )
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Удалить",
                            modifier = Modifier.Companion.size(ButtonDefaults.IconSize)
                        )
                    }
                }
            }
        }
    }
}