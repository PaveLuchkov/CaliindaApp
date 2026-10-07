package com.lpavs.caliinda.feature.widget.date

import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import java.util.Locale

/**
 * Эмодзи, который виджет даты прячет на заднем плане: праздник, если он сегодня, иначе примета
 * сезона. Из пула сезона берём по номеру дня — картинка меняется каждый день, но в течение дня
 * одна и та же.
 */
object DayEmoji {

  fun forDate(date: LocalDate, locale: Locale, zone: ZoneId): String =
      holiday(date, locale) ?: season(date, zone)

  internal fun holiday(date: LocalDate, locale: Locale): String? {
    val lang = locale.language
    val day = MonthDay.from(date)
    // Пасха у православных и западных церквей в разные дни.
    val easter = if (lang == "ru") orthodoxEaster(date.year) else westernEaster(date.year)
    if (date == easter) return "🐣"
    common[day]?.let {
      return it
    }
    byLanguage[lang]?.get(day)?.let {
      return it
    }
    if (locale.country == "US" && day == MonthDay.of(7, 4)) return "🎆"
    if (day.monthValue == 12 && day.dayOfMonth in 20..30) return "🎄"
    // Новогодние каникулы и Рождество 7 января.
    if (lang == "ru" && day.monthValue == 1 && day.dayOfMonth in 2..8) return "🎄"
    return null
  }

  internal fun season(date: LocalDate, zone: ZoneId): String {
    // В южном полушарии времена года наоборот: в июле там зима.
    val month = if (isSouthern(zone)) (date.monthValue + 5) % 12 + 1 else date.monthValue
    val pool = seasons.getValue(month)
    return pool[Math.floorMod(date.toEpochDay(), pool.size.toLong()).toInt()]
  }

  private val common =
      mapOf(
          MonthDay.of(1, 1) to "🎆",
          MonthDay.of(2, 14) to "💝",
          MonthDay.of(3, 8) to "💐",
          MonthDay.of(10, 31) to "🎃",
          MonthDay.of(12, 31) to "🥂",
      )

  private val byLanguage =
      mapOf(
          "ru" to mapOf(MonthDay.of(4, 12) to "🚀", MonthDay.of(9, 1) to "📚"),
          "en" to mapOf(MonthDay.of(3, 17) to "☘️"),
          "fr" to mapOf(MonthDay.of(7, 14) to "🎆"),
          "es" to mapOf(MonthDay.of(11, 2) to "💀"),
      )

  private val seasons =
      mapOf(
          1 to listOf("❄️", "⛄", "🧣"),
          2 to listOf("❄️", "⛸️", "🧤"),
          3 to listOf("🌱", "🌷", "🐦"),
          4 to listOf("🌷", "🌦️", "🌱"),
          5 to listOf("🌸", "🐝", "🌼"),
          6 to listOf("☀️", "🍓", "🌻"),
          7 to listOf("☀️", "🍉", "🏖️"),
          8 to listOf("🌻", "🍑", "🌊"),
          9 to listOf("🍁", "🍎", "🍇"),
          10 to listOf("🍂", "🍁", "🍄"),
          11 to listOf("🍂", "☔", "🧣"),
          12 to listOf("❄️", "⛄", "🎁"),
      )

  private val southernPrefixes =
      listOf(
          "Australia/",
          "Antarctica/",
          "Pacific/Auckland",
          "Pacific/Chatham",
          "America/Argentina",
          "America/Buenos_Aires",
          "America/Sao_Paulo",
          "America/Santiago",
          "America/Montevideo",
          "America/Asuncion",
          "Africa/Johannesburg",
          "Africa/Maputo",
          "Africa/Windhoek",
          "Indian/Mauritius",
          "Indian/Reunion",
      )

  internal fun isSouthern(zone: ZoneId) = southernPrefixes.any { zone.id.startsWith(it) }

  /** Алгоритм Мееуса/Джонса/Бутчера. */
  internal fun westernEaster(year: Int): LocalDate {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day = (h + l - 7 * m + 114) % 31 + 1
    return LocalDate.of(year, month, day)
  }

  /** Алгоритм Мееуса для юлианского календаря + сдвиг на григорианский (верен для 1900–2099). */
  internal fun orthodoxEaster(year: Int): LocalDate {
    val a = year % 4
    val b = year % 7
    val c = year % 19
    val d = (19 * c + 15) % 30
    val e = (2 * a + 4 * b - d + 34) % 7
    val month = (d + e + 114) / 31
    val day = (d + e + 114) % 31 + 1
    return LocalDate.of(year, month, day).plusDays(13)
  }
}
