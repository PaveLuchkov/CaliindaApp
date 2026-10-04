package com.lpavs.caliinda.core.data.calendar.model

import java.time.Instant

/**
 * Один экземпляр события из системного календаря (CalendarContract.Instances).
 *
 * Для all-day событий startTime/endTime — полночь в часовом поясе пользователя, endTime
 * эксклюзивная (следующий день после последнего).
 */
data class EventDto(
    /** Уникальный ключ экземпляра (событие + время начала), годится для ключей списков. */
    val id: String,
    val summary: String,
    val startTime: Instant,
    val endTime: Instant,
    val description: String? = null,
    val location: String? = null,
    val isAllDay: Boolean = false,
    /** id мастер-события серии, если экземпляр относится к повторяющемуся событию. */
    val recurringEventId: String? = null,
    /** Исходное время экземпляра в серии (для перенесённых — до переноса). */
    val originalStartTime: Instant? = null,
    /** RRULE без префикса "RRULE:". */
    val recurrenceRule: String? = null,
    /** _ID строки в CalendarContract.Events. */
    val eventId: Long = 0L,
    /** Сырое значение Instances.BEGIN (для all-day — UTC-полночь). */
    val instanceBegin: Long = 0L,
    /** Для исключений из серии — _ID мастер-события. */
    val originalEventId: Long? = null,
    /** Для исключений — сырое ORIGINAL_INSTANCE_TIME (для all-day — UTC-полночь). */
    val originalInstanceBegin: Long? = null,
    val calendarId: Long = 0L,
    val color: Int? = null,
)
