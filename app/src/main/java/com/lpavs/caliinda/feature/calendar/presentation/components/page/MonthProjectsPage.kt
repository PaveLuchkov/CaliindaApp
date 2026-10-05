package com.lpavs.caliinda.feature.calendar.presentation.components.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarCreateEventItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroState
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.ProjectsCardsList
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.SystemEventsList
import com.lpavs.caliinda.feature.event_management.EventActions

/** Страница месяца на экране проектов: карточки проектов, которые идут в этом месяце. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MonthProjectsPage(
    pageState: MonthPageUiState,
    actions: EventActions,
    hasCalendarAccess: Boolean,
    onGrantAccessClick: () -> Unit,
    createEventClick: () -> Unit,
    introductionState: IntroState,
) {
  val listState = rememberLazyListState()

  Column(modifier = Modifier.fillMaxSize()) {
    when {
      !hasCalendarAccess || (!introductionState.isFinished && !pageState.isLoading) ->
          SystemEventsList(
              hasCalendarAccess = hasCalendarAccess,
              introStep = introductionState.currentStep,
              onGrantAccessClick = onGrantAccessClick,
              projectView = true,
              onIntroNext = {})
      pageState.events.isNotEmpty() ->
          ProjectsCardsList(events = pageState.events, listState = listState, actions = actions)
      pageState.isLoading ->
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator(modifier = Modifier.size(80.dp))
          }
      else ->
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            CalendarCreateEventItem(onCreateEventClick = createEventClick)
          }
    }
  }
}
