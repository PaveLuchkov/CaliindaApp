package com.lpavs.caliinda.feature.calendar.presentation.components.page

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.feature.calendar.presentation.CalendarViewModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.FullScreenLoader
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarCreateEventItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroState
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.DailyCardsList
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.HeadCardsList
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.SystemEventsList
import com.lpavs.caliinda.feature.event_management.EventActions
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DayEventsPage(
    listScrollEnabled: Boolean,
    hasCalendarAccess: Boolean,
    onGrantAccessClick: () -> Unit,
    date: LocalDate,
    viewModel: CalendarViewModel,
    eventManagementViewModel: EventManagementViewModel,
    createEventClick: () -> Unit,
    introductionState: IntroState
) {
  // Flow создаём один раз на дату: иначе каждая рекомпозиция перезапускает запрос к календарю.
  val pageStateFlow = remember(viewModel, date) { viewModel.getDayPageUiState(date) }
  val pageState by
      pageStateFlow.collectAsStateWithLifecycle(initialValue = DayPageUiState(isLoading = true))
  val listState = rememberLazyListState()
  val actions =
      remember(eventManagementViewModel, viewModel) {
        EventActions(
            onDelete = eventManagementViewModel::requestDeleteConfirmation,
            onEdit = eventManagementViewModel::requestEditEvent,
            onDetails = viewModel::requestEventDetails)
      }
  val isBusy = pageState.isLoading
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
        userScrollEnabled = listScrollEnabled,
    )

    Spacer(modifier = Modifier.height(2.dp))

    val showIntro = !introductionState.isFinished && !isBusy
    val showAccess = !hasCalendarAccess

    if (showIntro || showAccess) {
      SystemEventsList(
          hasCalendarAccess = hasCalendarAccess,
          introStep = introductionState.currentStep,
          onGrantAccessClick = onGrantAccessClick,
          onIntroNext = { viewModel.onIntroNext() },
          projectView = false)
      return
    }

    val hasContent = pageState.timedEvents.isNotEmpty()
    val hasAllDayEvents = pageState.allDayEvents.isNotEmpty()

    when {
      hasContent -> {
        DailyCardsList(
            events = pageState.timedEvents,
            listState = listState,
            actions = actions,
            userScrollEnabled = listScrollEnabled)
      }
      isBusy -> {
        FullScreenLoader()
      }
      !hasAllDayEvents -> {
        CalendarCreateEventItem(onCreateEventClick = createEventClick)
      }
    }
  }
}
