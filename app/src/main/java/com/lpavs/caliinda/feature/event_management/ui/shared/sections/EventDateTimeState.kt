package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import androidx.annotation.StringRes
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.calendar.recurrence.Rrule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

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
