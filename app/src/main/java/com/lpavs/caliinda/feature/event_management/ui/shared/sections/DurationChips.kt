package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.cuid
import java.time.Duration
import java.time.LocalDateTime

private val QUICK_DURATIONS =
    listOf(Duration.ofMinutes(15), Duration.ofMinutes(30), Duration.ofHours(1), Duration.ofHours(2))

/**
 * Быстрая длительность: конец = начало + выбранное время, без открытия пикера. Конец может
 * перейти на следующий день — дату конца двигаем вместе с ним.
 */
@Composable
fun DurationChips(
    state: EventDateTimeState,
    onStateChange: (EventDateTimeState) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
  val startTime = state.startTime ?: return
  if (state.isAllDay) return
  val start = LocalDateTime.of(state.startDate, startTime)
  val current = state.endTime?.let { Duration.between(start, LocalDateTime.of(state.endDate, it)) }
  val haptic = LocalHapticFeedback.current

  LazyRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(cuid.padding, Alignment.CenterHorizontally)) {
    items(QUICK_DURATIONS, key = { it.toMinutes() }) { duration ->
      FilterChip(
          selected = current == duration,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            val end = start.plus(duration)
            onStateChange(state.copy(endDate = end.toLocalDate(), endTime = end.toLocalTime()))
          },
          label = { Text(durationLabel(duration)) },
          enabled = enabled)
    }
  }
}

@Composable
private fun durationLabel(duration: Duration): String =
    if (duration.toMinutes() < 60) stringResource(R.string.duration_minutes_short, duration.toMinutes())
    else stringResource(R.string.duration_hours_short, duration.toHours())
