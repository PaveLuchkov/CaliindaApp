package com.lpavs.caliinda.feature.calendar.data

import androidx.compose.ui.unit.Dp
import com.lpavs.caliinda.core.data.calendar.model.EventDto

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
    /** Сколько календарных дней проекта осталось, включая сегодня (1 = последний день). */
    val daysLeft: Long? = null
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