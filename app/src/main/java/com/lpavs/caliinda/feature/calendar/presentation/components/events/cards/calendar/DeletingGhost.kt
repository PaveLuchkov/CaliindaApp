package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.repository.DeletionCountdown
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.cuid

/**
 * Карточка или её «призрак», пока идёт отсчёт удаления. Смена — плавная: призрак может быть
 * выше короткой карточки, и соседи подъезжают, а не прыгают.
 */
@Composable
fun GhostWhenDeleting(
    countdown: DeletionCountdown?,
    title: String,
    cardHeight: Dp,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues =
        PaddingValues(
            horizontal = CalendarUiDefaults.ItemHorizontalPadding,
            vertical = CalendarUiDefaults.ItemVerticalPadding),
    card: @Composable () -> Unit,
) {
  AnimatedContent(
      targetState = countdown,
      contentKey = { it != null },
      transitionSpec = { fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS)) using SizeTransform(clip = false) },
      modifier = modifier,
      label = "deletingGhost") { state ->
        if (state == null) card()
        else DeletingGhost(title, state, cardHeight, onUndo, Modifier.padding(padding))
      }
}

/**
 * Призрак удаляемого события: карточка без окраса, граница пунктиром — тем же языком, что
 * пунктирная «+ создать», — внизу волнистый обратный отсчёт. Короткое событие дорастает до
 * [MIN_HEIGHT]: иначе «Отменить» не поместилась бы.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeletingGhost(
    title: String,
    countdown: DeletionCountdown,
    cardHeight: Dp,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(cuid.EventItemCornerRadius)
  val content = colorScheme.onSurfaceVariant
  val outline = colorScheme.outline
  // Сколько осталось: от момента начала отсчёта, а не от появления на экране — страницу могли
  // перелистнуть и вернуться.
  val remaining = remember(countdown) { Animatable(fractionLeft(countdown)) }
  LaunchedEffect(countdown) {
    val left = (countdown.durationMillis * remaining.value).toLong()
    remaining.animateTo(0f, tween(left.toInt().coerceAtLeast(1), easing = LinearEasing))
  }
  val message = countdown.message.asString(LocalContext.current)

  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .heightIn(min = maxOf(cardHeight, MIN_HEIGHT))
              .clip(shape)
              .background(colorScheme.primaryContainer.copy(alpha = GHOST_ALPHA))
              .dashedBorder(shape, outline)
              .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text(
                title,
                style = typography.titleMedium,
                color = content.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            // Весёлая фраза — здесь, а не в снекбаре; TalkBack зачитает её сам.
            Text(
                message,
                style = typography.bodySmall,
                color = content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
          }
          TextButton(onClick = onUndo) { Text(stringResource(R.string.undo)) }
        }
        LinearWavyProgressIndicator(
            progress = { remaining.value },
            color = content,
            trackColor = content.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
      }
}

private fun fractionLeft(countdown: DeletionCountdown): Float {
  val elapsed = SystemClock.uptimeMillis() - countdown.startedAtMillis
  return (1f - elapsed.toFloat() / countdown.durationMillis).coerceIn(0f, 1f)
}

/** Пунктирная граница по форме карточки. */
private fun Modifier.dashedBorder(shape: Shape, color: androidx.compose.ui.graphics.Color) =
    drawWithContent {
      drawContent()
      val width = 1.5.dp.toPx()
      drawOutline(
          outline = shape.createOutline(size, layoutDirection, this),
          color = color,
          style =
              Stroke(
                  width = width,
                  pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))))
    }

/** Короче — не помещаются название, фраза, «Отменить» и отсчёт. */
private val MIN_HEIGHT = 88.dp
private const val GHOST_ALPHA = 0.35f
private const val FADE_MS = 220
