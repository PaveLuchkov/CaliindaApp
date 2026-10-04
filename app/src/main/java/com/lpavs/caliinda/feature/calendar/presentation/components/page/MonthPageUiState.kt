package com.lpavs.caliinda.feature.calendar.presentation.components.page

import com.lpavs.caliinda.feature.calendar.data.EventUiModel

data class MonthPageUiState(
    val isLoading: Boolean = true,
    val events: List<EventUiModel> = emptyList(),
)
