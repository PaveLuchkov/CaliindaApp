package com.lpavs.caliinda.feature.calendar.presentation.model

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
    val daysLeft: Long? = null,
    /** Сколько события уже прошло (0…1) — для волны у идущего события. */
    val progress: Float = 0f,
)

data class EventDetailsUiModel(
    val summary: String,
    val formattedTimeString: String,
    val isCurrent: Boolean,
    val originalEvent: EventDto
)
