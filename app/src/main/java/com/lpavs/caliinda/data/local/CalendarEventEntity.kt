package com.lpavs.caliinda.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val summary: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val description: String?,
    val location: String?,
    val isAllDay: Boolean = false,
    val recurringEventId: String? = null,
    val originalStartTimeString: String? = null,
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
