package com.lpavs.caliinda.core.data.repository

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