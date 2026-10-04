package com.lpavs.caliinda.feature.calendar.presentation

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.feature.calendar.data.EventDetailsUiModel
import java.time.LocalDate

data class CalendarState(
    val hasCalendarPermission: Boolean = false,
    val eventForDetailedView: EventDetailsUiModel? = null,
    val showEventDetailedView: Boolean = false,
    val currentMode: AppMode = AppMode.CALENDAR,
    )

enum class AppMode {
    CALENDAR,
    MANAGEMENT
}
