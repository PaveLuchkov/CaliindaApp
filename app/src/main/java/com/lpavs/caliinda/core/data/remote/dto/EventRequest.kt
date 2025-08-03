package com.lpavs.caliinda.core.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class EventRequest(
    val summary: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val isAllDay: Boolean? = null,
    val timeZoneId: String? = null,
    val description: String? = null,
    val location: String? = null,
    val recurrence: List<String>? = null
)
