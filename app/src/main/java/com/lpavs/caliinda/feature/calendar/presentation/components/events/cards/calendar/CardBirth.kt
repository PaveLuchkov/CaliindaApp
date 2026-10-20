package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch

/**
 * «Рождение» сохранённой карточки — вместо снекбара «создано»: фигура делает оборот на пружине,
 * контейнер вспыхивает и гаснет. Новый [key] — анимация заново (сохранили то же событие ещё раз).
 */
class CardBirth internal constructor(
    private val turn: Animatable<Float, *>,
    private val glow: Animatable<Float, *>,
) {
  /** Добавка к повороту фигуры, градусы. */
  val rotation: Float
    get() = turn.value

  /** Сила вспышки контейнера: 1 — в момент сохранения, 0 — погасла. */
  val flash: Float
    get() = glow.value
}

@Composable
fun rememberCardBirth(key: Long?): CardBirth {
  val turn = remember { Animatable(0f) }
  val glow = remember { Animatable(0f) }
  LaunchedEffect(key) {
    if (key == null) return@LaunchedEffect
    turn.snapTo(0f)
    glow.snapTo(1f)
    launch { turn.animateTo(360f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessVeryLow)) }
    glow.animateTo(0f, tween(FLASH_MS, easing = FastOutSlowInEasing))
  }
  return remember(turn, glow) { CardBirth(turn, glow) }
}

private const val FLASH_MS = 900
