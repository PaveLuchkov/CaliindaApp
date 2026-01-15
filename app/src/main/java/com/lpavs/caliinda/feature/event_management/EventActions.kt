package com.lpavs.caliinda.feature.event_management

import com.lpavs.caliinda.core.data.remote.calendar.dto.EventDto

data class EventActions(
    val onDelete: (EventDto) -> Unit,
    val onEdit: (EventDto) -> Unit,
    val onDetails: (EventDto) -> Unit
)
