package com.lpavs.caliinda.feature.calendar.presentation.components.page

import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.ProjectRibbons
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.feature.calendar.presentation.components.events.FullScreenLoader
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.CalendarCreateEventItem
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroState
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.DailyCardsList
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.HeadCardsList
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.SystemEventsList
import com.lpavs.caliinda.feature.event_management.EventActions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DayEventsPage(
    pageState: DayPageUiState,
    actions: EventActions,
    listScrollEnabled: Boolean,
    pagePosition: () -> Float,
    hasCalendarAccess: Boolean,
    onGrantAccessClick: () -> Unit,
    createEventClick: () -> Unit,
    introductionState: IntroState,
    onIntroNext: () -> Unit,
) {
  val listState = rememberLazyListState()
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
        actions = actions,
        userScrollEnabled = listScrollEnabled,
    )

    ProjectRibbons(ribbons = pageState.projects, onClick = actions.onDetails)

    Spacer(modifier = Modifier.height(2.dp))

    val showIntro = !introductionState.isFinished && !isBusy
    val showAccess = !hasCalendarAccess

    if (showIntro || showAccess) {
      SystemEventsList(
          hasCalendarAccess = hasCalendarAccess,
          introStep = introductionState.currentStep,
          onGrantAccessClick = onGrantAccessClick,
          onIntroNext = onIntroNext,
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
            userScrollEnabled = listScrollEnabled,
            pagePosition = pagePosition)
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
