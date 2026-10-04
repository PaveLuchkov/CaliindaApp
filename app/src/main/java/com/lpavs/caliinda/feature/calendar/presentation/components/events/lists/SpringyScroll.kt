package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

// Карточка — грузик на пружинке, привязанный к своему месту в списке.
private const val STIFFNESS = 280f // 1/с²
private const val DAMPING_RATIO = 0.5f // < 1 — лёгкий перелёт при остановке
private val DAMPING = 2f * DAMPING_RATIO * sqrt(STIFFNESS)

/** Какая доля отставания доходит до карточки на максимальном удалении от пальца. */
private const val LAG_COUPLING = 1.5f
private val MAX_LAG = 96.dp

/**
 * «Вес» карточек при прокрутке: карточки позади пальца отстают от прокрутки тем сильнее, чем
 * дальше они от него, и догоняют с пружинным перелётом. Карточки впереди пальца не смещаются —
 * иначе при узких промежутках они наезжали бы друг на друга. Значение меняется только в graphicsLayer, поэтому
 * рекомпозиций нет — перерисовываются лишь слои карточек.
 */
@Stable
class SpringyScrollState internal constructor(
    private val scope: CoroutineScope,
    private val listState: LazyListState,
    private val maxLagPx: Float,
) {
  /** Отставание «грузика» от места в списке, px. */
  private val displacement = mutableFloatStateOf(0f)
  private var velocity = 0f
  private var pendingScroll = 0f
  private var anchorY = Float.NaN
  private var job: Job? = null

  internal val connection =
      object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
          if (consumed.y != 0f) {
            pendingScroll += consumed.y
            ensureRunning()
          }
          return Offset.Zero
        }
      }

  internal val anchorModifier =
      Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            event.changes.firstOrNull()?.let { anchorY = it.position.y }
          }
        }
      }

  private fun ensureRunning() {
    if (job?.isActive == true) return
    job =
        scope.launch {
          var last = withFrameNanos { it }
          while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 1f / 30f)
            last = now

            // Список уехал на pendingScroll, а грузик по инерции остался на месте.
            var x = displacement.floatValue - pendingScroll
            pendingScroll = 0f
            velocity += (-STIFFNESS * x - DAMPING * velocity) * dt
            x += velocity * dt
            displacement.floatValue = x

            if (abs(x) < 0.5f && abs(velocity) < 5f && !listState.isScrollInProgress) {
              displacement.floatValue = 0f
              velocity = 0f
              break
            }
          }
        }
  }

  /** Смещение карточки с ключом [key]: чем дальше от пальца, тем больше отставание. */
  internal fun offsetFor(key: Any): Float {
    val x = displacement.floatValue
    if (x == 0f) return 0f
    val info = listState.layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return 0f
    val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
    if (viewport <= 0f) return 0f
    val anchor = if (anchorY.isNaN()) viewport / 2f else anchorY
    val center = item.offset + item.size / 2f
    // Отстают только карточки позади пальца (со стороны, куда их тянет отставание).
    val distance = ((center - anchor) * sign(x) / viewport).coerceIn(0f, 1f)
    return (x * LAG_COUPLING * distance).coerceIn(-maxLagPx, maxLagPx)
  }
}

@Composable
fun rememberSpringyScrollState(listState: LazyListState): SpringyScrollState {
  val scope = rememberCoroutineScope()
  val maxLagPx = with(LocalDensity.current) { MAX_LAG.toPx() }
  return remember(listState, maxLagPx) { SpringyScrollState(scope, listState, maxLagPx) }
}

/** Вешается на сам список: ловит прокрутку и положение пальца. */
fun Modifier.springyScrollContainer(state: SpringyScrollState): Modifier =
    this.then(state.anchorModifier).nestedScroll(state.connection)

/** Вешается на карточку списка. */
fun Modifier.springyScrollItem(state: SpringyScrollState, key: Any): Modifier =
    graphicsLayer { translationY = state.offsetFor(key) }
