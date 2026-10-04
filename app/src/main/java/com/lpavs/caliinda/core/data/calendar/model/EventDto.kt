package com.lpavs.caliinda.core.data.calendar.model

/**
 * Один экземпляр события из системного календаря (CalendarContract.Instances).
 *
 * startTime/endTime — ISO_OFFSET_DATE_TIME в часовом поясе пользователя. Для all-day событий это
 * локальная полночь, endTime — эксклюзивная (следующий день после последнего).
 */
data class EventDto(
    /** Уникальный ключ экземпляра (событие + время начала), годится для ключей списков. */
    val id: String,
    val summary: String,
    val startTime: String?,
    val endTime: String?,
    val description: String? = null,
    val location: String? = null,
    val isAllDay: Boolean = false,
    /** id мастер-события серии, если экземпляр относится к повторяющемуся событию. */
    val recurringEventId: String? = null,
    val originalStartTime: String? = null,
    /** RRULE без префикса "RRULE:". */
    val recurrenceRule: String? = null,
    /** _ID строки в CalendarContract.Events. */
    val eventId: Long = 0L,
    /** Сырое значение Instances.BEGIN (для all-day — UTC-полночь). */
    val instanceBegin: Long = 0L,
    /** Для исключений из серии — _ID мастер-события. */
    val originalEventId: Long? = null,
    val calendarId: Long = 0L,
    val color: Int? = null,
)
