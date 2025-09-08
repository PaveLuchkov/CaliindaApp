package com.lpavs.caliinda.core.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

interface IDateTimeUtils {
  fun parseToInstant(dateTimeString: String?, zoneIdString: String): Instant?

  fun formatMillisToIsoString(millis: Long?, zoneIdString: String): String?

  fun formatDateTimeToIsoWithOffset(
      date: LocalDate?,
      time: LocalTime?,
      isAllDay: Boolean,
      zoneIdString: String
  ): String?

  fun formatLocalDateTimeToNaiveIsoString(date: LocalDate?, time: LocalTime?): String?
}

@Singleton
class DateTimeUtilsImpl @Inject constructor() : IDateTimeUtils {
  override fun parseToInstant(dateTimeString: String?, zoneIdString: String): Instant? {
    if (dateTimeString.isNullOrBlank()) return null

    val zoneId =
        try {
          ZoneId.of(zoneIdString.takeIf { it.isNotEmpty() } ?: ZoneId.systemDefault().id)
        } catch (e: Exception) {
          ZoneId.systemDefault()
        }

    return try {
      OffsetDateTime.parse(dateTimeString).toInstant()
    } catch (_: DateTimeParseException) {
      try {
        LocalDateTime.parse(dateTimeString).atZone(zoneId).toInstant()
      } catch (_: DateTimeParseException) {
        try {
          LocalDate.parse(dateTimeString).atStartOfDay(ZoneOffset.UTC).toInstant()
        } catch (_: DateTimeParseException) {
          null
        }
      }
    } catch (e: Exception) {
      null
    }
  }

  override fun formatMillisToIsoString(millis: Long?, zoneIdString: String): String? {
    if (millis == null) return null
    val zoneId =
        try {
          ZoneId.of(zoneIdString.ifEmpty { ZoneId.systemDefault().id })
        } catch (_: Exception) {
          ZoneId.systemDefault()
        }
    return try {
      Instant.ofEpochMilli(millis).atZone(zoneId).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    } catch (e: Exception) {
      null
    }
  }

  override fun formatDateTimeToIsoWithOffset(
      date: LocalDate?,
      time: LocalTime?,
      isAllDay: Boolean,
      zoneIdString: String
  ): String? {
    if (date == null) return null

    val zoneId =
        try {
          ZoneId.of(zoneIdString.takeIf { it.isNotEmpty() } ?: ZoneId.systemDefault().id)
        } catch (e: Exception) {
          ZoneId.systemDefault()
        }

    if (time == null && !isAllDay) {
      return null
    }

    val effectiveTime = if (isAllDay) LocalTime.MIDNIGHT else time!!

    return try {
      val zonedDateTime = date.atTime(effectiveTime).atZone(zoneId)
      zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    } catch (e: Exception) {
      null
    }
  }

  override fun formatLocalDateTimeToNaiveIsoString(date: LocalDate?, time: LocalTime?): String? {
    if (date == null || time == null) {
      return null
    }

    return try {
      val localDateTime = LocalDateTime.of(date, time)
      localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    } catch (e: Exception) {
      null
    }
  }
}
