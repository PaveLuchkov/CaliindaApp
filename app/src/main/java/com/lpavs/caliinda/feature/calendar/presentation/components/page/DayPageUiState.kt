package com.lpavs.caliinda.feature.calendar.presentation.components.page

import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.feature.calendar.data.EventUiModel

data class DayPageUiState(
    val isLoading: Boolean = true,
    val allDayEvents: List<EventDto> = emptyList(),
    val timedEvents: List<EventUiModel> = emptyList(),
    val targetScrollIndex: Int = -1
)
