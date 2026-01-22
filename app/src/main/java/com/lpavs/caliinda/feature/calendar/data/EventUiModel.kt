package com.lpavs.caliinda.feature.calendar.data

import androidx.compose.ui.unit.Dp
import com.lpavs.caliinda.core.data.remote.calendar.dto.EventDto

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
    val originalEvent: EventDto,
    val isPhantom: Boolean
)

data class EventDetailsUiModel(
    val summary: String,
    val formattedTimeString: String,
    val isCurrent: Boolean,
    val originalEvent: EventDto
)

data class HabbitModel(
    val id: String,
    val title: String,
    val desciption: String,
    val emoji: String,
    val primary: Boolean
)