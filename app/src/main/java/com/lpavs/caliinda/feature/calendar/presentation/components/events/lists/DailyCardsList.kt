package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import com.lpavs.caliinda.feature.calendar.data.EventUiModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarEventItem
import com.lpavs.caliinda.feature.event_management.EventActions

@Composable
fun DailyCardsList(
    events: List<EventUiModel>,
    listState: LazyListState,
    actions: EventActions,
    userScrollEnabled: Boolean = true,
    pagePosition: (() -> Float)? = null,
) {
    BaseEventList(
        items = events,
        key = { it.id },
        listState = listState,
        userScrollEnabled = userScrollEnabled,
        pagePosition = pagePosition
    ) { event, isExpanded, toggleExpand ->
        CalendarEventItem(
            uiModel = event,
            isExpanded = isExpanded,
            onToggleExpand = toggleExpand,
            onDeleteClickFromList = { actions.onDelete(event.originalEvent) },
            onEditClickFromList = { actions.onEdit(event.originalEvent) },
            onDetailsClickFromList = { actions.onDetails(event.originalEvent) }
        )
    }
}
