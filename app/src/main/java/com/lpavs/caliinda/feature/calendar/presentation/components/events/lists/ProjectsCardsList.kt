package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import com.lpavs.caliinda.feature.calendar.presentation.model.EventUiModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarProjectItem
import com.lpavs.caliinda.feature.event_management.EventActions
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.GhostWhenDeleting

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
        GhostWhenDeleting(
            countdown = actions.deletions[event.id],
            title = event.summary,
            cardHeight = event.baseHeight,
            onUndo = { actions.onUndoDelete(event.id) }) {
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
}
