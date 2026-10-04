package com.lpavs.caliinda.feature.calendar.presentation.components.page

import com.lpavs.caliinda.feature.calendar.data.EventUiModel

data class EventsPageUiState(
    val isLoading: Boolean = true,
    val events: List<EventUiModel> = emptyList(),
)
