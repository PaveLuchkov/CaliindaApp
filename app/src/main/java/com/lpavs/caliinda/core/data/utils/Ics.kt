package com.lpavs.caliinda.core.data.utils

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Событие файлом iCalendar (RFC 5545) — чтобы получатель добавил его в свой календарь одним
 * тапом. Только этот экземпляр, без RRULE: COUNT/UNTIL от середины серии посчитались бы неверно.
 * Время — в UTC, all-day — датами в поясе из настроек, как их видит пользователь.
 */
fun eventToIcs(event: EventDto, zoneId: ZoneId, now: Instant = Instant.now()): String {
  val lines = buildList {
    add("BEGIN:VCALENDAR")
    add("VERSION:2.0")
    add("PRODID:-//Caliinda//Caliinda//EN")
    add("METHOD:PUBLISH")
    add("BEGIN:VEVENT")
    add("UID:${event.id.filter { it.isLetterOrDigit() || it in "-_." }}@caliinda")
    add("DTSTAMP:${UTC.format(now)}")
    if (event.isAllDay) {
      // Конец исключающий — как и в календаре: однодневное — DTEND на следующий день.
      add("DTSTART;VALUE=DATE:${DATE.format(event.startTime.atZone(zoneId))}")
      add("DTEND;VALUE=DATE:${DATE.format(event.endTime.atZone(zoneId))}")
    } else {
      add("DTSTART:${UTC.format(event.startTime)}")
      add("DTEND:${UTC.format(event.endTime)}")
    }
    event.summary.takeIf { it.isNotBlank() }?.let { add("SUMMARY:${escape(it)}") }
    event.location?.takeIf { it.isNotBlank() }?.let { add("LOCATION:${escape(it.trim())}") }
    event.description
        ?.let(::readableDescription)
        ?.takeIf { it.isNotBlank() }
        ?.let { add("DESCRIPTION:${escape(it)}") }
    add("END:VEVENT")
    add("END:VCALENDAR")
  }
  return lines.joinToString("") { fold(it) + "\r\n" }
}

private val UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
private val DATE = DateTimeFormatter.ofPattern("yyyyMMdd")

private fun escape(text: String): String =
    text
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\n")
        .replace("\n", "\\n")

/** Строки длиннее 75 байт переносятся пробелом в начале; символ UTF-8 не режем пополам. */
private fun fold(line: String): String {
  val out = StringBuilder()
  var bytes = 0
  var i = 0
  while (i < line.length) {
    val cp = line.codePointAt(i)
    val len = String(Character.toChars(cp)).toByteArray(Charsets.UTF_8).size
    if (bytes + len > MAX_LINE_BYTES) {
      out.append("\r\n ")
      bytes = 1
    }
    out.appendCodePoint(cp)
    bytes += len
    i += Character.charCount(cp)
  }
  return out.toString()
}

private const val MAX_LINE_BYTES = 75

/** Первое событие из .ics: поля для формы создания. Время — как в CalendarContract (см. ниже). */
data class IcsEvent(
    val summary: String,
    val description: String,
    val location: String,
    /** Миллисекунды; у all-day — полночь UTC. */
    val beginMillis: Long,
    val endMillis: Long?,
    val isAllDay: Boolean,
)

/**
 * Разбор входящего .ics (вложение в письме, билет). Берём первый VEVENT — файл обычно и есть
 * одно приглашение. Время без пояса («плавающее») — в [floatingZone]. Без DTSTART — null.
 */
fun parseIcs(text: String, floatingZone: ZoneId): IcsEvent? {
  // Перенос строки — CRLF + пробел/таб: склеиваем обратно.
  val lines = text.replace("\r\n", "\n").replace(Regex("\n[ \t]"), "").lines()
  val start = lines.indexOfFirst { it.trim().equals("BEGIN:VEVENT", ignoreCase = true) }
  if (start < 0) return null
  val props = mutableMapOf<String, Pair<Map<String, String>, String>>()
  for (line in lines.drop(start + 1)) {
    if (line.trim().equals("END:VEVENT", ignoreCase = true)) break
    val colon = line.indexOf(':').takeIf { it > 0 } ?: continue
    val head = line.substring(0, colon).split(';')
    val params =
        head.drop(1).mapNotNull {
          val eq = it.indexOf('=')
          if (eq > 0) it.substring(0, eq).uppercase() to it.substring(eq + 1).trim('"') else null
        }
    // Вложенные VALARM и повторы свойства не нужны — оставляем первое.
    props.putIfAbsent(head[0].uppercase(), params.toMap() to line.substring(colon + 1))
  }
  val begin = props["DTSTART"]?.let { (params, value) -> parseIcsTime(params, value, floatingZone) }
  begin ?: return null
  val end = props["DTEND"]?.let { (params, value) -> parseIcsTime(params, value, floatingZone) }
  return IcsEvent(
      summary = props["SUMMARY"]?.second?.let(::unescape).orEmpty(),
      description = props["DESCRIPTION"]?.second?.let(::unescape).orEmpty(),
      location = props["LOCATION"]?.second?.let(::unescape).orEmpty(),
      beginMillis = begin.first,
      endMillis = end?.first,
      isAllDay = begin.second)
}

/** Миллисекунды и «весь день ли». */
private fun parseIcsTime(
    params: Map<String, String>,
    value: String,
    floatingZone: ZoneId
): Pair<Long, Boolean>? =
    runCatching {
          val v = value.trim()
          if (params["VALUE"].equals("DATE", ignoreCase = true) || v.length == 8) {
            LocalDate.parse(v, DATE).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() to true
          } else {
            val utc = v.endsWith("Z", ignoreCase = true)
            val local = LocalDateTime.parse(v.removeSuffix("Z").removeSuffix("z"), LOCAL)
            val zone =
                when {
                  utc -> ZoneOffset.UTC
                  else -> params["TZID"]?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: floatingZone
                }
            local.atZone(zone).toInstant().toEpochMilli() to false
          }
        }
        .getOrNull()

private val LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

private fun unescape(text: String): String {
  val out = StringBuilder()
  var i = 0
  while (i < text.length) {
    val c = text[i]
    if (c == '\\' && i + 1 < text.length) {
      val next = text[i + 1]
      out.append(if (next == 'n' || next == 'N') '\n' else next)
      i += 2
    } else {
      out.append(c)
      i++
    }
  }
  return out.toString().trim()
}
