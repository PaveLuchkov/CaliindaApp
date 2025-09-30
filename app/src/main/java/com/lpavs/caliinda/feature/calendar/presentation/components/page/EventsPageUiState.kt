package com.lpavs.caliinda.feature.calendar.presentation.components.page

import com.lpavs.caliinda.core.data.remote.calendar.dto.EventDto
import com.lpavs.caliinda.feature.calendar.data.EventUiModel

data class EventsPageUiState(
    val isLoading: Boolean = true,
    val events: List<EventUiModel> = emptyList(),
)


enum class WeekState {
    current_projects,
    past_projects,
    future_projects
}