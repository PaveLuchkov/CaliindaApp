package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import com.lpavs.caliinda.feature.calendar.data.EventUiModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarProjectItem
import com.lpavs.caliinda.feature.event_management.EventActions

@Composable
fun ProjectsCardsList(
    events: List<EventUiModel>,
    listState: LazyListState,
    actions: EventActions
) {
    BaseEventList(
        items = events,
        key = { it.id },
        listState = listState
    ) { event, isExpanded, toggleExpand ->
        CalendarProjectItem(
            uiModel = event,
            isExpanded = isExpanded,
            onToggleExpand = toggleExpand,
            onDeleteClickFromList = { actions.onDelete(event.originalEvent) },
            onEditClickFromList = { actions.onEdit(event.originalEvent) },
            onDetailsClickFromList = { actions.onDetails(event.originalEvent) }
        )
    }
}
