package com.lpavs.caliinda.feature.event_management

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.repository.DeletionCountdown
import com.lpavs.caliinda.feature.event_management.vm.Celebration

data class EventActions(
    val onDelete: (EventDto) -> Unit,
    val onEdit: (EventDto) -> Unit,
    val onDetails: (EventDto) -> Unit,
    /** Удаления в обратном отсчёте: карточка с этим id показывает вместо себя призрак. */
    val deletions: Map<String, DeletionCountdown> = emptyMap(),
    val onUndoDelete: (id: String) -> Unit = {},
    /** Только что сохранённое — его карточка «рождается». */
    val celebration: Celebration? = null,
)
