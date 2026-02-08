package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import com.lpavs.caliinda.feature.calendar.data.EventUiModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarEventItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.suggestions.SuggestionEventCard
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.LogInEvent
import com.lpavs.caliinda.feature.event_management.EventActions
import com.lpavs.caliinda.feature.event_management.SuggestionActions
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimeState

@Composable
fun DailyCardsList(
    events: List<EventUiModel>,
    listState: LazyListState,
    actions: EventActions,
    sugActions: SuggestionActions,

) {
    BaseEventList(
        items = events,
        key = { it.id },
        listState = listState
    ) { event, isExpanded, toggleExpand ->
        if (!event.isPhantom)
            CalendarEventItem(
                uiModel = event,
                isExpanded = isExpanded,
                highlightAction = null,
                onToggleExpand = toggleExpand,
                onDeleteClickFromList = { actions.onDelete(event.originalEvent) },
                onEditClickFromList = { actions.onEdit(event.originalEvent) },
                onDetailsClickFromList = { actions.onDetails(event.originalEvent) }
            )
        else
            SuggestionEventCard(
                uiModel = event,
                onApprove = { sugActions.onApprove(event.summary, event.originalEvent.startTime, event.originalEvent.endTime) },
                onDeny = {sugActions.onDeny(event.id)}
            )
    }
}
