package com.lpavs.caliinda.feature.calendar.presentation.components.dialogs

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.content.FileProvider
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.utils.eventToIcs
import com.lpavs.caliinda.core.data.utils.readableDescription
import com.lpavs.caliinda.core.ui.util.displayTitle
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.TimeZone

/**
 * Поделиться событием: текст — чтобы прочитать прямо в мессенджере, и .ics — чтобы получатель
 * добавил событие в свой календарь одним тапом.
 */
fun Context.shareEvent(event: EventDto, zoneId: ZoneId) {
  val title = event.displayTitle(this)
  val dir = File(cacheDir, SHARE_DIR).apply { mkdirs() }
  // Имя файла видно получателю — по названию события; старые файлы не копим.
  dir.listFiles()?.forEach { it.delete() }
  val name = title.replace(Regex("[\\\\/:*?\"<>|\\n]"), " ").trim().take(60).ifEmpty { "event" }
  val file = File(dir, "$name.ics").apply { writeText(eventToIcs(event, zoneId)) }
  val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

  val send =
      Intent(Intent.ACTION_SEND)
          // text/calendar: в списке появляются и календари — сразу добавить к себе.
          .setType("text/calendar")
          .putExtra(Intent.EXTRA_SUBJECT, title)
          .putExtra(Intent.EXTRA_TEXT, shareText(event, title, zoneId))
          .putExtra(Intent.EXTRA_STREAM, uri)
          .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  // Без ClipData разрешение на файл не доходит до приложения, выбранного в шторке.
  send.clipData = ClipData.newRawUri(name, uri)
  try {
    startActivity(Intent.createChooser(send, getString(R.string.share_event)))
  } catch (_: ActivityNotFoundException) {}
}

/** «Название / дата и время / место» и короткое описание — длинные приглашения не тащим. */
private fun Context.shareText(event: EventDto, title: String, zoneId: ZoneId): String {
  val locale = resources.configuration.locales[0]
  val today = LocalDate.now(zoneId)
  fun date(d: LocalDate) =
      d.format(
          DateTimeFormatter.ofPattern(if (d.year == today.year) "EEEE, d MMMM" else "EEEE, d MMMM yyyy", locale))
          .replaceFirstChar { it.titlecase(locale) }
  val timeFormat = DateFormat.getTimeFormat(this).apply { timeZone = TimeZone.getTimeZone(zoneId) }
  fun time(i: Instant) = timeFormat.format(Date.from(i))

  val start = event.startTime.atZone(zoneId).toLocalDate()
  // Конец исключающий: all-day «по 5-е» хранится как 6-е 00:00, событие до полуночи — тот же день.
  val end =
      if (event.isAllDay) event.endTime.minus(1, ChronoUnit.DAYS).atZone(zoneId).toLocalDate()
      else event.endTime.minusMillis(1).atZone(zoneId).toLocalDate()
  val whenText =
      when {
        event.isAllDay && start == end -> date(start)
        event.isAllDay -> "${date(start)} – ${date(end)}"
        start == end -> "${date(start)} · ${time(event.startTime)} – ${time(event.endTime)}"
        else -> "${date(start)}, ${time(event.startTime)} – ${date(end)}, ${time(event.endTime)}"
      }
  val description =
      event.description
          ?.let(::readableDescription)
          ?.takeIf { it.isNotBlank() && it.length <= MAX_DESCRIPTION }
  return listOfNotNull(title, whenText, event.location?.trim()?.takeIf { it.isNotEmpty() })
      .joinToString("\n") + (description?.let { "\n\n$it" } ?: "")
}

private const val SHARE_DIR = "shared"
private const val MAX_DESCRIPTION = 300
