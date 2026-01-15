package com.lpavs.caliinda.feature.calendar.presentation.components.page

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.core.common.EventNetworkState
import com.lpavs.caliinda.feature.agent.presentation.vm.AgentViewModel
import com.lpavs.caliinda.feature.calendar.presentation.CalendarViewModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarCreateEventItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.DailyCardsList
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.HeadCardsList
import com.lpavs.caliinda.feature.event_management.EventActions
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DayEventsPage(
    isLoading: Boolean,
    isSignIn: Boolean,
    onSignInClick: () -> Unit,
    date: LocalDate,
    viewModel: CalendarViewModel,
    eventManagementViewModel: EventManagementViewModel,
    agentViewModel: AgentViewModel,
    createEventClick: () -> Unit
) {
  val pageState by
      viewModel
          .getDayPageUiState(date)
          .collectAsStateWithLifecycle(initialValue = DayPageUiState(isLoading = true))
  val listState = rememberLazyListState()
  val actions =
      remember(eventManagementViewModel, viewModel) {
        EventActions(
            onDelete = eventManagementViewModel::requestDeleteConfirmation,
            onEdit = eventManagementViewModel::requestEditEvent,
            onDetails = viewModel::requestEventDetails)
      }
  val rangeNetworkState by viewModel.rangeNetworkState.collectAsStateWithLifecycle()
  val isBusy = isLoading || rangeNetworkState is EventNetworkState.Loading
  val agentResponse by agentViewModel.agentResponse.collectAsStateWithLifecycle()
  LaunchedEffect(pageState.targetScrollIndex) {
    if (pageState.targetScrollIndex != -1) {
      launch {
        try {
          listState.animateScrollToItem(index = pageState.targetScrollIndex)
        } catch (e: Exception) {
          Log.e("DayEventsPageScroll", "Error scrolling to index ${pageState.targetScrollIndex}", e)
        }
      }
    }
  }
  Column(modifier = Modifier.fillMaxSize()) {
    HeadCardsList(
        events = pageState.allDayEvents,
        onDeleteRequest = eventManagementViewModel::requestDeleteConfirmation,
        onEditRequest = eventManagementViewModel::requestEditEvent,
        onDetailsRequest = viewModel::requestEventDetails,
    )

    Spacer(modifier = Modifier.height(2.dp))

    if (pageState.timedEvents.isNotEmpty() or isSignIn ||
        !(agentResponse?.mainText.isNullOrBlank())) {
      DailyCardsList(
          events = pageState.timedEvents,
          listState = listState,
          isSignIn = isSignIn,
          actions = actions,
          onSignInClick = onSignInClick)
    } else if (pageState.allDayEvents.isEmpty()) {

      if (isBusy) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          LoadingIndicator(modifier = Modifier.size(80.dp))
        }
      } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
          CalendarCreateEventItem(onCreateEventClick = createEventClick)
        }
      }
    } else {
      Spacer(modifier = Modifier.weight(1f))
    }
  }
}
