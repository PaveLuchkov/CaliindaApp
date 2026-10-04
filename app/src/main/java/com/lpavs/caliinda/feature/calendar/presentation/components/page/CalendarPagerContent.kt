package com.lpavs.caliinda.feature.calendar.presentation.components.page

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.lpavs.caliinda.feature.calendar.presentation.CalendarViewModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroState
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalendarPagerScreen(
    calendarViewModel: CalendarViewModel,
    eventManagementViewModel: EventManagementViewModel,
    calendarPagerState: PagerState,
    dailyViewPagerState: PagerState,
    weekViewPagerState: PagerState,
    hasCalendarAccess: Boolean,
    onGrantAccessClick: () -> Unit,
    createEventAction: () -> Unit,
    initialPageIndex: Int,
    today: LocalDate,
    introductionState: IntroState
) {

    HorizontalPager(
      state = calendarPagerState,
      modifier = Modifier.fillMaxSize(),
      userScrollEnabled = hasCalendarAccess
    ) { page ->
        when (page) {
          0 ->
              VerticalPager(
                  state = weekViewPagerState,
                  modifier = Modifier.fillMaxSize(),
                  flingBehavior =
                      PagerDefaults.flingBehavior(
                          state = weekViewPagerState, snapPositionalThreshold = 0.05f),
                  userScrollEnabled = false,
                  beyondViewportPageCount = 1) { _ ->
                    ProjectEventsPage(
                        hasCalendarAccess = hasCalendarAccess,
                        onGrantAccessClick = onGrantAccessClick,
                        viewModel = calendarViewModel,
                        eventManagementViewModel = eventManagementViewModel,
                        createEventClick = createEventAction,
                        introductionState = introductionState)
                  }

          1 ->
              VerticalPager(
                  state = dailyViewPagerState,
                  modifier = Modifier.fillMaxSize(),
                  key = { index ->
                    today.plusDays((index - initialPageIndex).toLong()).toEpochDay()
                  },
                  flingBehavior =
                      PagerDefaults.flingBehavior(
                          state = dailyViewPagerState, snapPositionalThreshold = 0.05f),
                  userScrollEnabled = hasCalendarAccess,
                  beyondViewportPageCount = 1) { pageIndex ->
                    val pageDate =
                        remember(pageIndex) {
                          today.plusDays((pageIndex - initialPageIndex).toLong())
                        }
                    DayEventsPage(
                        // Пока пейджер листается, жест должен достаться ему, а не списку
                        // страницы — иначе они "дерутся" и пейджер застревает между днями.
                        listScrollEnabled = !dailyViewPagerState.isScrollInProgress,
                        hasCalendarAccess = hasCalendarAccess,
                        onGrantAccessClick = onGrantAccessClick,
                        date = pageDate,
                        viewModel = calendarViewModel,
                        eventManagementViewModel = eventManagementViewModel,
                        createEventClick = createEventAction,
                        introductionState = introductionState)
                  }
        }
      }
}
