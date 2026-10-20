package com.lpavs.caliinda.feature.calendar.presentation.components.page

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.core.ui.theme.AppMotion
import kotlinx.coroutines.delay

/** Тон плашки: обычное сообщение или ошибка. */
enum class NoticeTone {
  INFO,
  ERROR,
}

/** Что сказать в плашке; [onClick] — плашка-переход («Создано на чт, 22 окт →»). */
data class Notice(
    val text: String,
    val tone: NoticeTone = NoticeTone.INFO,
    val onClick: (() -> Unit)? = null,
    /** Отличает одинаковые сообщения подряд — каждое показывается заново. */
    val key: Long = SystemClock.uptimeMillis(),
)

/** Текущая плашка экрана календаря. Новая заменяет старую — очереди нет: снекбары копились. */
@Stable
class TopNoticeState {
  var current by mutableStateOf<Notice?>(null)
    private set

  fun show(notice: Notice) {
    current = notice
  }

  fun dismiss(notice: Notice) {
    if (current == notice) current = null
  }
}

@Composable fun rememberTopNoticeState() = remember { TopNoticeState() }

/**
 * Плашка под шапкой — вместо снекбара, который перекрывал кнопку создания. Всё, что не привязано
 * к видимой карточке: ошибки, «Синхронизация…», «Нет изменений», «Создано на другой день →».
 * Время показа — с поправкой на настройки доступности, как было у снекбара.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopNoticeHost(state: TopNoticeState, modifier: Modifier = Modifier) {
  val accessibility = LocalAccessibilityManager.current
  val notice = state.current
  LaunchedEffect(notice) {
    notice ?: return@LaunchedEffect
    val base = if (notice.onClick != null) ACTION_MS else INFO_MS
    delay(
        accessibility?.calculateRecommendedTimeoutMillis(
            base, containsIcons = notice.onClick != null, containsText = true,
            containsControls = notice.onClick != null) ?: base)
    state.dismiss(notice)
  }
  Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
    AnimatedContent(
        targetState = notice,
        contentKey = { it?.key },
        transitionSpec = {
          (slideInVertically(AppMotion.defaultSpatialSpec()) { -it } +
                  scaleIn(AppMotion.defaultSpatialSpec(), initialScale = 0.9f) +
                  fadeIn(AppMotion.fastEffectsSpec()))
              .togetherWith(
                  slideOutVertically(AppMotion.fastSpatialSpec()) { -it } +
                      fadeOut(AppMotion.fastEffectsSpec()))
              .using(SizeTransform(clip = false))
        },
        label = "topNotice") { shown ->
          if (shown != null) NoticePill(shown, onTap = { shown.onClick?.invoke(); state.dismiss(shown) })
        }
  }
}

@Composable
private fun NoticePill(notice: Notice, onTap: () -> Unit) {
  val (container, content) =
      when (notice.tone) {
        // Инверсный тон: плашка лежит поверх лент и карточек — вторичный с ними сливался.
        NoticeTone.INFO -> colorScheme.inverseSurface to colorScheme.inverseOnSurface
        NoticeTone.ERROR -> colorScheme.errorContainer to colorScheme.onErrorContainer
      }
  val pill: @Composable () -> Unit = {
    Row(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              notice.text,
              style = typography.labelLarge,
              color = content,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              // Снекбар TalkBack зачитывал сам — плашка тоже.
              modifier = Modifier.weight(1f, fill = false).semantics { liveRegion = LiveRegionMode.Polite })
          if (notice.onClick != null) {
            Spacer(Modifier.size(8.dp))
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(18.dp))
          }
        }
  }
  val shape = CircleShape
  val modifier = Modifier.padding(horizontal = 24.dp).widthIn(max = 560.dp)
  if (notice.onClick != null) {
    Surface(onClick = onTap, shape = shape, color = container, shadowElevation = 3.dp, modifier = modifier) { pill() }
  } else {
    Surface(shape = shape, color = container, shadowElevation = 3.dp, modifier = modifier) { pill() }
  }
}

private const val INFO_MS = 3_000L
private const val ACTION_MS = 4_000L
