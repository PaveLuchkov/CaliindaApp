package com.lpavs.caliinda.feature.live

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import java.time.Duration
import java.time.Instant

/** Что показать в live-уведомлении и когда пересчитать. */
data class LiveEventPlan(
    /** Идущее событие; null — уведомление не нужно. */
    val current: EventDto?,
    /** Следующее, если начинается почти сразу после текущего («Следующее событие: …»). */
    val next: EventDto?,
    /** Когда пересчитать: прогресс, конец текущего или начало следующего. */
    val refreshAt: Instant?,
)

/** Следующее показываем, только если до него после текущего не больше этого. */
internal val NEXT_EVENT_GAP: Duration = Duration.ofMinutes(15)

/** Как часто обновлять полосу прогресса, пока событие идёт. */
internal val PROGRESS_STEP: Duration = Duration.ofMinutes(5)

/**
 * Только события со временем: «весь день» и проекты идут сутками, прогресс по ним в шторке —
 * шум. Из пересекающихся текущих берём то, что закончится раньше.
 */
fun planLiveEvent(events: List<EventDto>, now: Instant): LiveEventPlan {
  val timed = events.filter { !it.isAllDay && it.endTime.isAfter(it.startTime) }.sortedBy { it.startTime }
  val current =
      timed.filter { !now.isBefore(it.startTime) && now.isBefore(it.endTime) }.minByOrNull { it.endTime }
  val upcoming = timed.firstOrNull { it.startTime.isAfter(now) }
  if (current == null) return LiveEventPlan(null, null, upcoming?.startTime)

  val next =
      upcoming?.takeIf {
        !it.startTime.isAfter(current.endTime.plus(NEXT_EVENT_GAP))
      }
  val refreshAt = listOfNotNull(current.endTime, now.plus(PROGRESS_STEP), upcoming?.startTime).min()
  return LiveEventPlan(current, next, refreshAt)
}
