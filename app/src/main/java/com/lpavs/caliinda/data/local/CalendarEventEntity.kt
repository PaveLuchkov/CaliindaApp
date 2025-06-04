package com.lpavs.caliinda.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String, // Google Calendar Event ID (естественный ключ)
    val summary: String,
    val startTimeMillis: Long, // Время начала в миллисекундах UTC
    val endTimeMillis: Long, // Время конца в миллисекундах UTC
    val description: String?,
    val location: String?,
    val isAllDay: Boolean = false,
    val recurringEventId: String? = null, // Новое поле
    val originalStartTimeString: String? = null, // Новое поле (храним как строку)
    val lastFetchedMillis: Long = System.currentTimeMillis(),
    val recurrenceRuleString: String? = null,
)

data class UpdateEventApiRequest(
    val summary: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val isAllDay: Boolean? = null,
    val timeZoneId: String? = null,
    val description: String? = null,
    val location: String? = null,
    val recurrence: List<String>? = null
)
