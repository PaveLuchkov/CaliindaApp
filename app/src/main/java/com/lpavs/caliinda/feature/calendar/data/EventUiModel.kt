package com.lpavs.caliinda.feature.calendar.data

import androidx.compose.ui.unit.Dp
import com.lpavs.caliinda.core.data.remote.dto.EventDto

data class EventUiModel(
    val id: String,
    val summary: String,
    val location: String?,
    val isAllDay: Boolean,
    val formattedTimeString: String,
    val durationMinutes: Long,
    val isMicroEvent: Boolean,
    val baseHeight: Dp,
    val expandedHeight: Dp,
    val isCurrent: Boolean,
    val isNext: Boolean,
    val proximityRatio: Float,
    val shapeParams: GeneratedShapeParams,
    val originalEvent: EventDto
)

data class EventDetailsUiModel(
    val summary: String,
    val formattedTimeString: String,
    val isCurrent: Boolean,
    val originalEvent: EventDto
)
