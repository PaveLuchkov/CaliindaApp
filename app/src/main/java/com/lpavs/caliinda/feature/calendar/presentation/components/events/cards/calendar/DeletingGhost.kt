package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.repository.DeletionCountdown
import com.lpavs.caliinda.core.ui.theme.AppMotion
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.cuid

/** Идёт отсчёт удаления карточки: сколько осталось и как отменить. */
class CardDeletion(val countdown: DeletionCountdown, val onUndo: () -> Unit)

/** Отсчёт удаления для карточки внутри [GhostWhenDeleting]; null — карточка живая. */
val LocalCardDeletion = compositionLocalOf<CardDeletion?> { null }

/**
 * Удаление как превращение самой карточки: она теряет окрас ([ghostly]), вокруг проявляется
 * пунктир — тем же языком, что пунктирная «+ создать», — а в её ряду кнопок остаются только
 * червяк отсчёта и «Отменить» ([DeletableActions]). Ничего не дорастает снизу: карточка
 * раскрыта, как при нажатии на неё. Отменили — всё в обратную сторону.
 */
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
  val ghost = ghostProgress(countdown != null)
  val outline = colorScheme.outline
  val shape = RoundedCornerShape(cuid.EventItemCornerRadius)
  Box(modifier) {
    CompositionLocalProvider(
        LocalCardDeletion provides countdown?.let { CardDeletion(it, onUndo) }, content = card)
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

/** 0 → 1 по мере превращения в призрак; карточки берут его для [ghostly]. */
@Composable
fun ghostProgress(deleting: Boolean = LocalCardDeletion.current != null): Float {
  val ghost by
      animateFloatAsState(
          if (deleting) 1f else 0f, tween(MORPH_MS, easing = FastOutSlowInEasing), label = "ghost")
  return ghost
}

/**
 * Теряет окрас: прозрачность и насыщенность почти в серый. Вешается на фон и содержимое карточки,
 * но не на ряд кнопок — «Отменить» и отсчёт должны остаться живыми.
 */
fun Modifier.ghostly(ghost: Float) = graphicsLayer {
  alpha = 1f - (1f - GHOST_ALPHA) * ghost
  renderEffect =
      if (ghost > 0f) {
        val matrix = ColorMatrix().apply { setSaturation(1f - 0.8f * ghost) }
        RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix)).asComposeRenderEffect()
      } else null
}

/**
 * Ряд кнопок раскрытой карточки. Пока идёт отсчёт удаления, [others] (подробности, правка)
 * уходят, слева ползёт червяк отсчёта, а [delete] превращается в «Отменить».
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeletableActions(
    delete: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.End,
    wormColor: Color = colorScheme.primary,
    undoColors: ButtonColors = ButtonDefaults.buttonColors(),
    others: @Composable RowScope.() -> Unit,
) {
  val deletion = LocalCardDeletion.current
  // Последний отсчёт держим, пока червяк уходит после «Отменить», — иначе он уходил бы пустым.
  var shown by remember { mutableStateOf(deletion) }
  if (deletion != null) shown = deletion
  Row(
      modifier,
      horizontalArrangement = if (deletion != null) Arrangement.End else horizontalArrangement,
      verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(
            visible = deletion != null,
            modifier = if (deletion != null) Modifier.weight(1f) else Modifier,
            enter = fadeIn(tween(MORPH_MS, delayMillis = WORM_DELAY_MS)),
            exit = fadeOut(AppMotion.fastEffectsSpec())) {
              shown?.let { CountdownWorm(it.countdown, wormColor, Modifier.padding(end = 12.dp)) }
            }
        AnimatedVisibility(
            visible = deletion == null,
            enter = expandHorizontally(AppMotion.defaultSpatialSpec()) + fadeIn(AppMotion.fastEffectsSpec()),
            exit = shrinkHorizontally(AppMotion.fastSpatialSpec()) + fadeOut(AppMotion.fastEffectsSpec())) {
              Row(verticalAlignment = Alignment.CenterVertically, content = others)
            }
        AnimatedContent(
            targetState = deletion != null,
            transitionSpec = {
              fadeIn(AppMotion.fastEffectsSpec()) togetherWith
                  fadeOut(AppMotion.fastEffectsSpec()) using
                  SizeTransform(clip = false) { _, _ -> AppMotion.defaultSpatialSpec() }
            },
            contentAlignment = Alignment.CenterEnd,
            label = "deleteOrUndo") { deleting ->
              if (deleting) {
                Button(
                    onClick = { shown?.onUndo?.invoke() },
                    colors = undoColors,
                    contentPadding = PaddingValues(horizontal = 16.dp)) {
                      Text(stringResource(R.string.undo))
                    }
              } else {
                delete()
              }
            }
      }
}

/** Волнистый обратный отсчёт; весёлую фразу здесь не показываем, но TalkBack её зачитает. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CountdownWorm(countdown: DeletionCountdown, color: Color, modifier: Modifier = Modifier) {
  // Сколько осталось: от начала отсчёта, а не от появления на экране — страницу могли
  // перелистнуть и вернуться.
  val remaining = remember(countdown) { Animatable(fractionLeft(countdown)) }
  LaunchedEffect(countdown) {
    val left = (countdown.durationMillis * remaining.value).toLong()
    remaining.animateTo(0f, tween(left.toInt().coerceAtLeast(1), easing = LinearEasing))
  }
  val message = countdown.message.asString(LocalContext.current)
  LinearWavyProgressIndicator(
      progress = { remaining.value },
      color = color,
      trackColor = color.copy(alpha = 0.2f),
      modifier =
          modifier.fillMaxWidth().semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = message
          })
}

private fun fractionLeft(countdown: DeletionCountdown): Float {
  val elapsed = SystemClock.uptimeMillis() - countdown.startedAtMillis
  return (1f - elapsed.toFloat() / countdown.durationMillis).coerceIn(0f, 1f)
}

private const val GHOST_ALPHA = 0.45f
private const val MORPH_MS = 350
private const val WORM_DELAY_MS = 120
