package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import com.lpavs.caliinda.feature.calendar.data.EventUiModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarEventItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.LogInEvent
import com.lpavs.caliinda.feature.event_management.EventActions

@Composable
fun DailyCardsList(
    events: List<EventUiModel>,
    listState: LazyListState,
    isSignIn: Boolean,
    actions: EventActions,
    onSignInClick: () -> Unit,
) {
    BaseEventList(
        items = events,
        key = { it.id },
        listState = listState,
        headerContent = if (isSignIn) { { LogInEvent(onSignInClick) } } else null
    ) { event, isExpanded, toggleExpand ->
        CalendarEventItem(
            uiModel = event,
            isExpanded = isExpanded,
            highlightAction = null,
            onToggleExpand = toggleExpand,
            onDeleteClickFromList = { actions.onDelete(event.originalEvent) },
            onEditClickFromList = { actions.onEdit(event.originalEvent) },
            onDetailsClickFromList = { actions.onDetails(event.originalEvent) }
        )
    }
}
