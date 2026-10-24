package com.lpavs.caliinda.core.data.utils

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import java.time.Instant
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
