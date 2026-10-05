package com.lpavs.caliinda.core.data.calendar.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Данные события из формы создания/редактирования. endDate — включительно. */
data class EventDraft(
    val summary: String,
    val description: String?,
    val location: String?,
    val isAllDay: Boolean,
    val startDate: LocalDate,
    val startTime: LocalTime?,
    val endDate: LocalDate,
    val endTime: LocalTime?,
    val zone: ZoneId,
    /** RRULE без префикса "RRULE:", null — событие не повторяется. */
    val recurrenceRule: String?,
)

enum class EventUpdateMode {
  SINGLE_INSTANCE,
  THIS_AND_FOLLOWING,
  ALL_IN_SERIES
}

enum class EventDeleteMode {
  /** Обычное (не повторяющееся) событие. */
  DEFAULT,
  INSTANCE_ONLY,
  THIS_AND_FOLLOWING,
  ALL_IN_SERIES
}

data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val isPrimary: Boolean,
)
