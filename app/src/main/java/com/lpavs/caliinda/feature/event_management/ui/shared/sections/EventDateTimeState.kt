package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import androidx.annotation.StringRes
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.calendar.recurrence.Rrule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class RecurrenceEndType {
  NEVER,
  DATE,
  COUNT
}

/** Дата, время и повторение в форме события. endDate — включительно. */
data class EventDateTimeState(
    val startDate: LocalDate,
    val startTime: LocalTime?,
    val endDate: LocalDate,
    val endTime: LocalTime?,
    val isAllDay: Boolean,
    val isRecurring: Boolean,
    /** Выбранная частота в виде "FREQ=…" (см. [RecurrenceOption]), null — не повторяется. */
    val recurrenceRule: String? = null,
    val selectedWeekdays: Set<DayOfWeek> = emptySet(),
    val recurrenceEndType: RecurrenceEndType = RecurrenceEndType.NEVER,
    val recurrenceEndDate: LocalDate? = null,
    val recurrenceCount: Int? = null
) {
  /** Что не так с датами и временем — для подписи под полями; null — всё в порядке. */
  @get:StringRes
  val validationError: Int?
    get() {
      if (endDate.isBefore(startDate)) return R.string.error_end_date_before_start
      if (isAllDay) return null
      val start = startTime ?: return R.string.error_start_time_missing
      val end = endTime ?: return R.string.error_end_time_missing
      if (!start.atDate(startDate).isBefore(end.atDate(endDate))) {
        return R.string.error_end_time_not_after_start
      }
      return null
    }

  /**
   * Переключатель «Весь день». Обратно ко времени: начало — прежнее или следующий ровный час
   * от [now], конец — не раньше начала (через час), при переходе за полночь — на следующий день.
   */
  fun toggledAllDay(now: LocalTime): EventDateTimeState {
    if (!isAllDay) return copy(isAllDay = true, startTime = null, endTime = null)
    val start = startTime ?: now.plusHours(1).truncatedTo(ChronoUnit.HOURS)
    var end = endTime
    var newEndDate = endDate
    if (startDate == endDate) {
      if (end == null || !start.isBefore(end)) end = start.plusHours(1)
      end = end.withNano(0)
      if (end.isBefore(start)) newEndDate = startDate.plusDays(1)
    } else {
      end = (end ?: start.plusHours(1)).withNano(0)
    }
    return copy(isAllDay = false, startTime = start, endTime = end, endDate = newEndDate)
  }

  /** Переключатель «Один день»: схлопнуть в день начала или растянуть на два дня. */
  fun toggledOneDay(): EventDateTimeState {
    if (startDate == endDate) return copy(endDate = startDate.plusDays(1))
    var end = endTime
    val start = startTime
    if (!isAllDay && start != null && (end == null || !start.isBefore(end))) {
      end = start.plusHours(1).withNano(0)
      // Час после 23:xx уходит за полночь — в пределах дня остаётся только 23:59.
      if (end.isBefore(start)) end = LocalTime.of(23, 59)
    }
    return copy(endDate = startDate, endTime = end)
  }

  /** Переключатель «Повтор»: по умолчанию — ежедневно, прежняя частота сохраняется. */
  fun toggledRecurring(): EventDateTimeState =
      if (isRecurring) copy(isRecurring = false, recurrenceRule = null)
      else copy(isRecurring = true, recurrenceRule = recurrenceRule ?: RecurrenceOption.Daily.rruleValue)

  /** Начать в [time] (долгое нажатие — «сейчас»); конец сдвигается, если оказался раньше. */
  fun startingAt(time: LocalTime): EventDateTimeState {
    val end = endTime
    val newEnd =
        if (startDate == endDate && end != null && !time.isBefore(end)) time.plusHours(1).withNano(0)
        else end
    return copy(startTime = time, endTime = newEnd)
  }

  /** Правило повторения из полей формы; null — событие не повторяется. */
  fun toRrule(zone: ZoneId): Rrule? {
    val freq = recurrenceRule?.takeIf { it.isNotBlank() }?.removePrefix("FREQ=") ?: return null
    val weekly = recurrenceRule == RecurrenceOption.Weekly.rruleValue
    return Rrule(
        freq = freq,
        byDay = if (weekly) selectedWeekdays.sorted() else emptyList(),
        until =
            recurrenceEndDate
                ?.takeIf { recurrenceEndType == RecurrenceEndType.DATE }
                ?.let { date ->
                  // Для all-day серий UNTIL должен быть датой (RFC 5545), иначе — конец дня.
                  if (isAllDay) Rrule.Until.Date(date)
                  else Rrule.Until.DateTime(date.atTime(23, 59, 59).atZone(zone).toInstant())
                },
        count = recurrenceCount?.takeIf { recurrenceEndType == RecurrenceEndType.COUNT })
  }
}

/** Сохранить форму: название, описание, место, дата и время. */
typealias OnSaveEvent =
    (summary: String, description: String, location: String, dateTime: EventDateTimeState) -> Unit

/** Состояние формы для правки существующего события. */
fun EventDto.toDateTimeState(zone: ZoneId): EventDateTimeState {
  val start = startTime.atZone(zone)
  val end = endTime.atZone(zone)
  val startDate = start.toLocalDate()
  val rule = recurrenceRule?.let(Rrule::parse)
  val untilDate = rule?.untilDate(zone)
  val count = rule?.count

  return EventDateTimeState(
      startDate = startDate,
      startTime = if (isAllDay) null else start.toLocalTime().withNano(0),
      // Конец all-day события эксклюзивный: последний день — предыдущий.
      endDate =
          if (isAllDay) end.toLocalDate().let { if (it.isAfter(startDate)) it.minusDays(1) else it }
          else end.toLocalDate(),
      endTime = if (isAllDay) null else end.toLocalTime().withNano(0),
      isAllDay = isAllDay,
      isRecurring = recurrenceRule != null,
      recurrenceRule =
          RecurrenceOption.ALL_OPTIONS.find { it.rruleValue == "FREQ=${rule?.freq}" }?.rruleValue,
      selectedWeekdays = rule?.byDay.orEmpty().toSet(),
      recurrenceEndType =
          when {
            untilDate != null -> RecurrenceEndType.DATE
            count != null -> RecurrenceEndType.COUNT
            else -> RecurrenceEndType.NEVER
          },
      recurrenceEndDate = untilDate,
      recurrenceCount = count)
}

/**
 * RRULE, который надо сохранить после правки. Повторение не трогали — исходное правило как
 * есть. Сменили концовку или дни, но не частоту — переносим части, которых форма не показывает
 * (INTERVAL, BYMONTHDAY…), иначе «каждые 2 недели» молча стали бы «каждую неделю».
 */
fun recurrenceRuleAfterEdit(original: EventDto, form: EventDateTimeState, zone: ZoneId): String? {
  val formRule = form.toRrule(zone)
  if (formRule == original.toDateTimeState(zone).toRrule(zone)) return original.recurrenceRule
  if (formRule == null) return null
  val originalRule = original.recurrenceRule?.let(Rrule::parse)
  return (if (originalRule != null && originalRule.freq == formRule.freq)
          formRule.copy(other = originalRule.other)
      else formRule)
      .format()
}
