package com.lpavs.caliinda.core.data.calendar.recurrence

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Разобранное правило повторения (RFC 5545) — только то, что умеет форма. Остальные части
 * (INTERVAL, BYMONTHDAY, BYDAY с номером недели…) лежат в [other] как есть: правка формы не
 * должна молча их терять.
 */
data class Rrule(
    /** Значение FREQ: DAILY, WEEKLY, MONTHLY, YEARLY. */
    val freq: String?,
    val byDay: List<DayOfWeek> = emptyList(),
    val until: Until? = null,
    val count: Int? = null,
    val other: List<String> = emptyList(),
) {
  sealed interface Until {
    /** UNTIL=yyyyMMdd — у all-day серий. */
    data class Date(val date: LocalDate) : Until

    /** UNTIL=yyyyMMdd'T'HHmmss'Z' — у серий со временем. */
    data class DateTime(val instant: Instant) : Until
  }

  /** Строка без префикса "RRULE:" — так её хранит CalendarContract. */
  fun format(): String =
      buildList {
            freq?.let { add("FREQ=$it") }
            if (byDay.isNotEmpty()) add("BYDAY=" + byDay.sorted().joinToString(",") { it.code() })
            addAll(other)
            until?.let { add("UNTIL=" + formatUntil(it)) }
            count?.let { add("COUNT=$it") }
          }
          .joinToString(";")

  /** Последний день серии в поясе пользователя, если она ограничена датой. */
  fun untilDate(zone: ZoneId): LocalDate? =
      when (val u = until) {
        is Until.Date -> u.date
        is Until.DateTime -> u.instant.atZone(zone).toLocalDate()
        null -> null
      }

  /** Обрезает серию так, чтобы последний экземпляр был строго раньше [instantTime]. */
  fun endingBefore(instantTime: Long, allDay: Boolean): Rrule =
      copy(
          count = null,
          until =
              if (allDay) Until.Date(utcDate(instantTime).minusDays(1))
              else Until.DateTime(Instant.ofEpochMilli(instantTime - 1000)))

  companion object {
    private val UTC_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

    fun parse(rule: String): Rrule {
      var freq: String? = null
      var byDay = emptyList<DayOfWeek>()
      var until: Until? = null
      var count: Int? = null
      val other = mutableListOf<String>()

      for (part in rule.removePrefix("RRULE:").split(';')) {
        if (part.isBlank()) continue
        val key = part.substringBefore('=').uppercase()
        val value = part.substringAfter('=', missingDelimiterValue = "")
        val parsed =
            when (key) {
              "FREQ" -> value.uppercase().also { freq = it }
              "BYDAY" -> parseByDay(value)?.also { byDay = it }
              "UNTIL" -> parseUntil(value)?.also { until = it }
              "COUNT" -> value.toIntOrNull()?.also { count = it }
              else -> null
            }
        if (parsed == null) other += part
      }
      return Rrule(freq, byDay, until, count, other)
    }

    /** Значение EXDATE для экземпляра с сырым началом [instanceBegin] (у all-day — UTC-полночь). */
    fun exdateValue(instanceBegin: Long, allDay: Boolean): String =
        if (allDay) utcDate(instanceBegin).format(DateTimeFormatter.BASIC_ISO_DATE)
        else Instant.ofEpochMilli(instanceBegin).atZone(ZoneOffset.UTC).format(UTC_DATE_TIME)

    /**
     * Обратное к [exdateValue]: миллисекунды одного значения EXDATE. Префикс TZID=…: (бывает у
     * синхронизированных серий) отбрасываем — для подсчёта до точки разреза хватает и UTC.
     * Нераспознанное — Long.MAX_VALUE, то есть «после любого момента».
     */
    fun exdateMillis(value: String): Long {
      val v = value.substringAfterLast(':').trim().removeSuffix("Z")
      return runCatching {
            if (v.length == 8) {
              LocalDate.parse(v, DateTimeFormatter.BASIC_ISO_DATE)
                  .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            } else {
              java.time.LocalDateTime.parse(v, LOCAL_DATE_TIME)
                  .toInstant(ZoneOffset.UTC).toEpochMilli()
            }
          }
          .getOrDefault(Long.MAX_VALUE)
    }

    private val LOCAL_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    /** Только простые дни (MO,WE). С номером недели (1MO, -1FR) форма не справится — в other. */
    private fun parseByDay(value: String): List<DayOfWeek>? =
        value.split(',').map { code -> DAY_CODES[code.uppercase()] ?: return null }

    private fun parseUntil(value: String): Until? =
        runCatching {
              if (value.length == 8) Until.Date(LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE))
              else
                  Until.DateTime(
                      UTC_DATE_TIME.withZone(ZoneOffset.UTC).parse(value, Instant::from))
            }
            .getOrNull()

    private fun formatUntil(until: Until): String =
        when (until) {
          is Until.Date -> until.date.format(DateTimeFormatter.BASIC_ISO_DATE)
          is Until.DateTime -> until.instant.atZone(ZoneOffset.UTC).format(UTC_DATE_TIME)
        }

    private fun utcDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    private val DAY_CODES =
        mapOf(
            "MO" to DayOfWeek.MONDAY,
            "TU" to DayOfWeek.TUESDAY,
            "WE" to DayOfWeek.WEDNESDAY,
            "TH" to DayOfWeek.THURSDAY,
            "FR" to DayOfWeek.FRIDAY,
            "SA" to DayOfWeek.SATURDAY,
            "SU" to DayOfWeek.SUNDAY)

    private fun DayOfWeek.code(): String = DAY_CODES.entries.first { it.value == this }.key
  }
}
