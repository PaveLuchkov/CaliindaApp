package com.lpavs.caliinda.core.data.utils

/** Сервисы созвонов, которые узнаём по домену ссылки. */
enum class MeetingService(val hosts: List<String>) {
  ZOOM(listOf("zoom.us", "zoom.com")),
  MEET(listOf("meet.google.com")),
  TEAMS(listOf("teams.microsoft.com", "teams.live.com")),
  TELEMOST(listOf("telemost.yandex.ru", "telemost.360.yandex.ru")),
  WEBEX(listOf("webex.com")),
  DISCORD(listOf("discord.gg", "discord.com")),
  TELEGRAM(listOf("t.me")),
  WHEREBY(listOf("whereby.com"));

  /** Домен сервиса или его поддомен: us02web.zoom.us — тоже Zoom. */
  fun matches(host: String): Boolean = hosts.any { host == it || host.endsWith(".$it") }
}

/** Что лежит в «месте» события: адрес для карт или ссылка. */
sealed interface EventPlace {
  data class Address(val text: String) : EventPlace

  /** [service] — известный сервис созвонов; null — просто ссылка, подписываем доменом [host]. */
  data class Link(val url: String, val host: String, val service: MeetingService?) : EventPlace
}

/**
 * Место события: ссылка в «месте» — это ссылка (часто туда кладут Zoom), иначе адрес. Пустое
 * место — ищем ссылку на созвон в описании: Google и Zoom кладут её туда. Произвольные ссылки
 * из описания не берём — там бывает что угодно.
 */
fun eventPlace(location: String?, description: String?): EventPlace? {
  val place = location?.trim()?.takeIf { it.isNotEmpty() }
  if (place != null) return findLink(place) ?: EventPlace.Address(place)
  return description?.let(::findMeetingLink)
}

private fun findLink(text: String): EventPlace.Link? =
    URL.findAll(text).firstNotNullOfOrNull { toLink(it.value) }

private fun findMeetingLink(text: String): EventPlace.Link? =
    URL.findAll(text).firstNotNullOfOrNull { match -> toLink(match.value)?.takeIf { it.service != null } }

private fun toLink(raw: String): EventPlace.Link? {
  // Хвостовая пунктуация из текста («…?pwd=abc).») — не часть ссылки.
  val trimmed = raw.trimEnd('.', ',', ';', ':', ')', ']', '>', '"', '\'', '»')
  val url = if (trimmed.startsWith("http", ignoreCase = true)) trimmed else "https://$trimmed"
  val host =
      url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
          .substringBefore(':').lowercase().removePrefix("www.")
  if ('.' !in host) return null
  return EventPlace.Link(url, host, MeetingService.entries.firstOrNull { it.matches(host) })
}

/**
 * Ссылка со схемой или без неё: «https://…», «zoom.us/j/…». Без схемы — только домен с путём,
 * иначе «ул. Ленина 5.» или «10.30» сошли бы за ссылки.
 */
private val URL =
    Regex(
        """https?://[^\s<>"]+|(?<![\w@.])(?:[a-z0-9-]+\.)+[a-z]{2,}/[^\s<>"]*""",
        RegexOption.IGNORE_CASE)
