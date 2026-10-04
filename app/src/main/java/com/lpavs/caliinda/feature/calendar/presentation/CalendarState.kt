package com.lpavs.caliinda.feature.calendar.presentation

import com.lpavs.caliinda.feature.calendar.data.EventDetailsUiModel

data class CalendarState(
    val hasCalendarPermission: Boolean = false,
    val eventForDetailedView: EventDetailsUiModel? = null,
    val showEventDetailedView: Boolean = false,
)

