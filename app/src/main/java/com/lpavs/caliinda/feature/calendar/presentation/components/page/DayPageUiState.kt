package com.lpavs.caliinda.feature.calendar.presentation.components.page

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.feature.calendar.data.EventUiModel

data class DayPageUiState(
    val isLoading: Boolean = true,
    val allDayEvents: List<EventDto> = emptyList(),
    val timedEvents: List<EventUiModel> = emptyList(),
    /** Проекты, которые идут в этот день, — тонкие ленты под шапкой. */
    val projects: List<ProjectRibbon> = emptyList(),
    val targetScrollIndex: Int = -1
)

/** Проект на странице дня: «день [dayNumber] из [totalDays]». */
data class ProjectRibbon(val event: EventDto, val dayNumber: Int, val totalDays: Int) {
  val progress: Float
    get() = (dayNumber.toFloat() / totalDays).coerceIn(0f, 1f)
}
