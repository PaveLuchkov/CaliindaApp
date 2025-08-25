package com.lpavs.caliinda.core.data.remote.calendar.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventDto(
    val id: String,
    val summary: String,
    @SerialName("startTime") val startTime: String?,
    @SerialName("endTime") val endTime: String?,
    val description: String? = null,
    val location: String? = null,
    @SerialName("isAllDay") val isAllDay: Boolean = false,
    @SerialName("recurringEventId") val recurringEventId: String? = null,
    @SerialName("originalStartTime") val originalStartTime: String? = null,
    @SerialName("recurrenceRule") val recurrenceRule: String? = null
)
