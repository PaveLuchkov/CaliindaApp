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
import com.lpavs.caliinda.core.common.EventNetworkState
import com.lpavs.caliinda.feature.calendar.presentation.CalendarViewModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarCreateEventItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.ProjectsCardsList
import com.lpavs.caliinda.feature.event_management.EventActions
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProjectEventsPage(
    isLoading: Boolean,
    viewModel: CalendarViewModel,
    eventManagementViewModel: EventManagementViewModel,
    createEventClick: () -> Unit
) {
  val listState = rememberLazyListState()
  val pageState by
      viewModel
          .getProjectsPageUiState(LocalDate.now())
          .collectAsStateWithLifecycle(initialValue = EventsPageUiState(isLoading = true))
  val rangeNetworkState by viewModel.rangeNetworkState.collectAsStateWithLifecycle()
  val isBusy = isLoading || rangeNetworkState is EventNetworkState.Loading
  val actions =
      remember(eventManagementViewModel, viewModel) {
        EventActions(
            onDelete = eventManagementViewModel::requestDeleteConfirmation,
            onEdit = eventManagementViewModel::requestEditEvent,
            onDetails = viewModel::requestEventDetails)
      }

  val noEmptyEvents = pageState.events.isNotEmpty()
  Column(modifier = Modifier.fillMaxSize()) {
    if (noEmptyEvents) {
      ProjectsCardsList(events = pageState.events, listState = listState, actions = actions)
    } else {
      if (isBusy) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          LoadingIndicator(modifier = Modifier.size(80.dp))
        }
      } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
          CalendarCreateEventItem(onCreateEventClick = createEventClick)
        }
      }
    }
  }
}
