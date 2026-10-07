package com.lpavs.caliinda.feature.widget.countdown

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.feature.calendar.presentation.model.dateRange
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Что показывает виджет обратного отсчёта. */
sealed interface Countdown {
  /** Событие ещё не выбрали. */
  data object NotSet : Countdown

  /** Выбранного события больше нет впереди: прошло или удалено. [title] — как было при выборе. */
  data class Gone(val title: String) : Countdown

  /** До начала [days] дней (от 1). */
  data class Ahead(val event: EventDto, val days: Int) : Countdown

  /** Начнётся сегодня. */
  data class Today(val event: EventDto) : Countdown

  /** Уже идёт; у многодневного — «день [day] из [totalDays]». */
  data class Ongoing(val event: EventDto, val day: Int, val totalDays: Int) : Countdown
}

/**
 * Дни считаем по календарным датам, а не по 24 часам: событие завтра в 9 утра — «1 день», даже
 * если сейчас 23:00.
 */
fun countdownOf(event: EventDto?, savedTitle: String?, now: Instant, zone: ZoneId): Countdown {
  if (event == null) return if (savedTitle == null) Countdown.NotSet else Countdown.Gone(savedTitle)
  val today = now.atZone(zone).toLocalDate()
  val range = event.dateRange(zone)
  return when {
    !event.endTime.isAfter(now) -> Countdown.Gone(savedTitle ?: event.summary)
    !event.startTime.isAfter(now) ->
        Countdown.Ongoing(
            event,
            day = ChronoUnit.DAYS.between(range.start, today).toInt() + 1,
            totalDays = ChronoUnit.DAYS.between(range.start, range.endInclusive).toInt() + 1)
    range.start == today -> Countdown.Today(event)
    else -> Countdown.Ahead(event, ChronoUnit.DAYS.between(today, range.start).toInt())
  }
}
