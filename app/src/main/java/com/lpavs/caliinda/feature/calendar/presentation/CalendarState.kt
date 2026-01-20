package com.lpavs.caliinda.feature.calendar.presentation

import com.lpavs.caliinda.core.data.remote.calendar.EventUpdateMode
import com.lpavs.caliinda.core.data.remote.calendar.dto.EventDto
import com.lpavs.caliinda.feature.calendar.data.EventDetailsUiModel
import java.time.LocalDate

data class CalendarState(
    val isSignedIn: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = "Требуется вход.",
    val signInRequired: Boolean = false,
    val eventForDetailedView: EventDetailsUiModel? = null,
    val showEventDetailedView: Boolean = false,
    val currentMode: AppMode = AppMode.CALENDAR,
    )

enum class AppMode {
    CALENDAR,
    MANAGEMENT
}
