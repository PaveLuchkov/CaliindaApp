package com.lpavs.caliinda.core.data.utils

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import java.net.URLEncoder
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Ссылка «Добавить в Google Календарь»: всё событие — в самой ссылке, сервер не нужен.
 * Без описания: каждый символ кириллицы в ссылке — 6 байт, QR от длинного текста — мелкая каша.
 * [compact] — для QR: ещё и без места, адрес легко удваивает длину.
 */
fun googleCalendarLink(event: EventDto, zoneId: ZoneId, compact: Boolean = false): String {
  val dates =
      if (event.isAllDay) {
        // Конец исключающий — как в .ics.
        "${DATE.format(event.startTime.atZone(zoneId))}/${DATE.format(event.endTime.atZone(zoneId))}"
      } else {
        "${UTC.format(event.startTime)}/${UTC.format(event.endTime)}"
      }
  val params = buildList {
    add("action" to "TEMPLATE")
    event.summary.takeIf { it.isNotBlank() }?.let { add("text" to it.trim()) }
    add("dates" to dates)
    if (!compact) event.location?.trim()?.takeIf { it.isNotEmpty() }?.let { add("location" to it) }
  }
  // Даты — как есть: «/» между ними Google ждёт буквально, а QR короче.
  return "https://calendar.google.com/calendar/render?" +
      params.joinToString("&") { (key, value) -> "$key=${if (key == "dates") value else encode(value)}" }
}

private fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

private val UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
private val DATE = DateTimeFormatter.ofPattern("yyyyMMdd")
