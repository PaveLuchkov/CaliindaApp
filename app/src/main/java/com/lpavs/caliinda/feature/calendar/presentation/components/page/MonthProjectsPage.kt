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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.feature.calendar.presentation.CalendarViewModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarCreateEventItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroState
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.ProjectsCardsList
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.SystemEventsList
import com.lpavs.caliinda.feature.event_management.EventActions
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import java.time.YearMonth

/** Страница месяца на экране проектов: карточки проектов, которые идут в этом месяце. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MonthProjectsPage(
    month: YearMonth,
    hasCalendarAccess: Boolean,
    onGrantAccessClick: () -> Unit,
    viewModel: CalendarViewModel,
    eventManagementViewModel: EventManagementViewModel,
    createEventClick: () -> Unit,
    introductionState: IntroState,
) {
  val listState = rememberLazyListState()
  val pageStateFlow = remember(viewModel, month) { viewModel.getMonthPageUiState(month) }
  val pageState by pageStateFlow.collectAsStateWithLifecycle(initialValue = MonthPageUiState())
  val actions =
      remember(eventManagementViewModel, viewModel) {
        EventActions(
            onDelete = eventManagementViewModel::requestDelete,
            onEdit = eventManagementViewModel::requestEditEvent,
            onDetails = viewModel::requestEventDetails)
      }

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
