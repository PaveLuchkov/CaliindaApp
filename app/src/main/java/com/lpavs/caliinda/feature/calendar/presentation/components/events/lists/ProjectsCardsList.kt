package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.lpavs.caliinda.feature.calendar.presentation.model.EventUiModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarProjectItem
import com.lpavs.caliinda.feature.event_management.EventActions
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.GhostWhenDeleting

@Composable
fun ProjectsCardsList(
    events: List<EventUiModel>,
    listState: LazyListState,
    actions: EventActions,
    userScrollEnabled: Boolean = true,
) {
    // Сохранили событие с этой страницы — докручиваем ленту к его карточке, где идёт «рождение».
    val celebration = actions.celebration
    LaunchedEffect(celebration?.startedAt) {
      val index = celebration?.let { c -> events.indexOfFirst { c.matches(it.originalEvent) } } ?: -1
      if (index >= 0) listState.animateScrollToItem(index)
    }
    BaseEventList(
        items = events,
        key = { it.id },
        listState = listState,
        userScrollEnabled = userScrollEnabled
    ) { event, isExpanded, toggleExpand ->
        GhostWhenDeleting(
            countdown = actions.deletions[event.id],
            onUndo = { actions.onUndoDelete(event.id) }) {
            val celebrated = actions.celebration?.takeIf { it.matches(event.originalEvent) }
            CalendarProjectItem(
                celebration = celebrated?.message?.asString(LocalContext.current),
                celebrationKey = celebrated?.startedAt,
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
