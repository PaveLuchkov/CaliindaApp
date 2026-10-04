package com.lpavs.caliinda.feature.calendar.data

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Проект на таймлайне месяца: в какой колонке и с какого по какой день (индексы с 0). */
data class ProjectBar(
    val event: EventDto,
    val lane: Int,
    val startDay: Int,
    val endDayExclusive: Int,
    /** Начался в прошлом месяце — верх капсулы растворяется. */
    val continuesBefore: Boolean,
    /** Продолжится в следующем месяце — низ капсулы растворяется. */
    val continuesAfter: Boolean,
    val isPast: Boolean,
    val isCurrent: Boolean,
)

data class MonthLayout(
    val month: YearMonth,
    val bars: List<ProjectBar>,
    val laneCount: Int,
    val eventsPerDay: Map<LocalDate, Int>,
)

/** Первый и последний день события в поясе пользователя (конец события эксклюзивный). */
fun EventDto.dateRange(zone: ZoneId): ClosedRange<LocalDate> {
  val first = startTime.atZone(zone).toLocalDate()
  val last = endTime.minusNanos(1).atZone(zone).toLocalDate()
  return first..maxOf(first, last)
}

/**
 * Раскладывает проекты месяца по колонкам: каждый занимает первую колонку, свободную на всём его
 * промежутке. Раньше начавшиеся и более длинные идут левее — так длинные проекты стоят ровной
 * опорой, а короткие заполняют просветы.
 */
fun layoutMonth(
    month: YearMonth,
    projects: List<EventDto>,
    eventsPerDay: Map<LocalDate, Int>,
    zone: ZoneId,
    today: LocalDate,
): MonthLayout {
  val firstDay = month.atDay(1)
  val lastDay = month.atEndOfMonth()
  val laneEnds = mutableListOf<Int>() // эксклюзивный конец последней капсулы в каждой колонке

  val bars =
      projects
          .map { it to it.dateRange(zone) }
          .filter { (_, range) -> range.start <= lastDay && range.endInclusive >= firstDay }
          .sortedWith(
              compareBy<Pair<EventDto, ClosedRange<LocalDate>>> { it.second.start }
                  .thenByDescending { it.second.endInclusive })
          .map { (event, range) ->
            val start = ChronoUnit.DAYS.between(firstDay, maxOf(range.start, firstDay)).toInt()
            val end =
                ChronoUnit.DAYS.between(firstDay, minOf(range.endInclusive, lastDay)).toInt() + 1
            var lane = laneEnds.indexOfFirst { it <= start }
            if (lane == -1) {
              lane = laneEnds.size
              laneEnds += end
            } else {
              laneEnds[lane] = end
            }
            ProjectBar(
                event = event,
                lane = lane,
                startDay = start,
                endDayExclusive = end,
                continuesBefore = range.start < firstDay,
                continuesAfter = range.endInclusive > lastDay,
                isPast = range.endInclusive < today,
                isCurrent = today in range)
          }

  return MonthLayout(month, bars, laneEnds.size, eventsPerDay)
}
