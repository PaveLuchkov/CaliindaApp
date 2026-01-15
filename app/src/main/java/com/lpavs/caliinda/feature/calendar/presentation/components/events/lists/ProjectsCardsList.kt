package com.lpavs.caliinda.feature.calendar.presentation.components.events.lists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.core.data.remote.agent.domain.AgentResponseContent
import com.lpavs.caliinda.core.data.remote.agent.domain.DaysPlanContent
import com.lpavs.caliinda.core.data.remote.agent.domain.ErrorResponse
import com.lpavs.caliinda.core.data.remote.agent.domain.SuggestionPlan
import com.lpavs.caliinda.core.data.remote.agent.domain.TextMessageResponse
import com.lpavs.caliinda.core.data.remote.calendar.dto.EventDto
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.feature.calendar.data.EventUiModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.agent.AgentDayPlanItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.agent.AgentMessageItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.agent.AgentRecommendItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarEventItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarProjectItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.LogInEvent
import com.lpavs.caliinda.feature.event_management.EventActions
import java.time.LocalDate

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
