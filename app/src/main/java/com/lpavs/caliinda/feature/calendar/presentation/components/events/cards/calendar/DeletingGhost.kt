package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.repository.DeletionCountdown
import com.lpavs.caliinda.core.ui.theme.AppMotion
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.cuid

/**
 * Удаление как превращение самой карточки, а не подмена: она остаётся на месте и теряет окрас
 * (насыщенность и прозрачность), вокруг проявляется пунктир — тем же языком, что пунктирная
 * «+ создать», — а снизу на пружине выезжает полоска с фразой, «Отменить» и обратным отсчётом.
 * Отменили — всё в обратную сторону. Пока идёт отсчёт, нажатия на карточку не проходят.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GhostWhenDeleting(
    countdown: DeletionCountdown?,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    /** Внешние отступы карточки — по ним идёт пунктир. */
    padding: PaddingValues =
        PaddingValues(
            horizontal = CalendarUiDefaults.ItemHorizontalPadding,
            vertical = CalendarUiDefaults.ItemVerticalPadding),
    card: @Composable () -> Unit,
) {
  val deleting = countdown != null
  // Последний отсчёт держим, пока полоска уезжает после «Отменить», — иначе она уезжала бы пустой.
  var shown by remember { mutableStateOf(countdown) }
  if (countdown != null) shown = countdown
  val ghost by
      animateFloatAsState(
          if (deleting) 1f else 0f, tween(MORPH_MS, easing = FastOutSlowInEasing), label = "ghost")
  val outline = colorScheme.outline
  val shape = RoundedCornerShape(cuid.EventItemCornerRadius)

  Box(modifier) {
    Column {
      Box(
          Modifier.graphicsLayer {
                alpha = 1f - (1f - GHOST_ALPHA) * ghost
                // Теряет окрас: насыщенность уходит почти в серый.
                renderEffect =
                    if (ghost > 0f) {
                      val matrix = ColorMatrix().apply { setSaturation(1f - 0.8f * ghost) }
                      RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix))
                          .asComposeRenderEffect()
                    } else null
              }
              .then(if (deleting) Modifier.blockTouches() else Modifier)) {
            card()
          }
      AnimatedVisibility(
          visible = deleting,
          enter =
              expandVertically(AppMotion.defaultSpatialSpec(), expandFrom = Alignment.Top) +
                  fadeIn(tween(MORPH_MS, delayMillis = STRIP_DELAY_MS)),
          exit =
              shrinkVertically(AppMotion.fastSpatialSpec(), shrinkTowards = Alignment.Top) +
                  fadeOut(AppMotion.fastEffectsSpec())) {
            shown?.let { GhostStrip(it, onUndo, Modifier.padding(padding)) }
          }
    }
    // Пунктир вокруг карточки и полоски вместе — проявляется вместе с потерей окраса.
    Box(
        Modifier.matchParentSize().padding(padding).drawWithContent {
          if (ghost > 0f) {
            drawOutline(
                outline = shape.createOutline(size, layoutDirection, this),
                color = outline.copy(alpha = outline.alpha * ghost),
                style =
                    Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))))
          }
        })
  }
}

/** Полоска под карточкой: весёлая фраза, «Отменить» и волнистый обратный отсчёт. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GhostStrip(countdown: DeletionCountdown, onUndo: () -> Unit, modifier: Modifier = Modifier) {
  val content = colorScheme.onSurfaceVariant
  // Сколько осталось: от начала отсчёта, а не от появления на экране — страницу могли
  // перелистнуть и вернуться.
  val remaining = remember(countdown) { Animatable(fractionLeft(countdown)) }
  LaunchedEffect(countdown) {
    val left = (countdown.durationMillis * remaining.value).toLong()
    remaining.animateTo(0f, tween(left.toInt().coerceAtLeast(1), easing = LinearEasing))
  }
  Column(modifier.padding(start = 16.dp, end = 8.dp, bottom = 8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      // Весёлая фраза — здесь, а не в снекбаре; TalkBack зачитает её сам.
      Text(
          countdown.message.asString(LocalContext.current),
          style = typography.bodySmall,
          color = content,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite })
      TextButton(onClick = onUndo) { Text(stringResource(R.string.undo)) }
    }
    LinearWavyProgressIndicator(
        progress = { remaining.value },
        color = content,
        trackColor = content.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth().padding(end = 8.dp))
  }
}

/** Пока идёт отсчёт, карточка не раскрывается и не открывает подробности. */
private fun Modifier.blockTouches() =
    pointerInput(Unit) {
      awaitPointerEventScope {
        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
      }
    }

private fun fractionLeft(countdown: DeletionCountdown): Float {
  val elapsed = SystemClock.uptimeMillis() - countdown.startedAtMillis
  return (1f - elapsed.toFloat() / countdown.durationMillis).coerceIn(0f, 1f)
}

private const val GHOST_ALPHA = 0.45f
private const val MORPH_MS = 350
private const val STRIP_DELAY_MS = 120
