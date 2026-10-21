package com.lpavs.caliinda.feature.calendar.presentation.components.page

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalAccessibilityManager
import kotlinx.coroutines.delay

/** Тон сообщения: обычное или ошибка (шапка перекрашивается в цвет ошибки). */
enum class NoticeTone {
  INFO,
  ERROR,
}

/** Что сказать в шапке; [onClick] — сообщение-переход («Создано на чт, 22 окт →»). */
data class Notice(
    val text: String,
    val tone: NoticeTone = NoticeTone.INFO,
    val onClick: (() -> Unit)? = null,
    /** Отличает одинаковые сообщения подряд — каждое показывается заново. */
    val key: Long = SystemClock.uptimeMillis(),
)

/** Текущее сообщение в шапке. Новое заменяет старое — очереди нет: снекбары копились. */
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
 * Сколько держится сообщение в шапке: 3 с, с переходом — 4, с поправкой на настройки
 * доступности, как было у снекбара. Само сообщение показывает шапка (CalendarDateTitle).
 */
@Composable
fun NoticeAutoDismiss(state: TopNoticeState) {
  val accessibility = LocalAccessibilityManager.current
  val notice = state.current
  LaunchedEffect(notice) {
    notice ?: return@LaunchedEffect
    val base = if (notice.onClick != null) ACTION_MS else INFO_MS
    delay(
        accessibility?.calculateRecommendedTimeoutMillis(
            base, containsIcons = false, containsText = true,
            containsControls = notice.onClick != null) ?: base)
    state.dismiss(notice)
  }
}

private const val INFO_MS = 3_000L
private const val ACTION_MS = 4_000L
