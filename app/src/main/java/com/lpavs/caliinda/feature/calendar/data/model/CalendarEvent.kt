package com.lpavs.caliinda.feature.calendar.data.model

data class CalendarEvent(
    val id: String,
    val summary: String,
    val startTime: String?,
    val endTime: String?,
    val description: String? = null,
    val location: String? = null,
    val isAllDay: Boolean = false,
    val recurringEventId: String? = null,
    val originalStartTime: String? = null,
    val recurrenceRule: String? = null
)
