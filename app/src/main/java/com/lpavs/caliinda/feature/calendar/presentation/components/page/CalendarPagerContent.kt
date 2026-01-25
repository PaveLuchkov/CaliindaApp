package com.lpavs.caliinda.feature.calendar.presentation.components.page

import android.app.Activity
import android.util.Log
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
import com.lpavs.caliinda.core.data.auth.AuthViewModel
import com.lpavs.caliinda.feature.agent.presentation.vm.AgentViewModel
import com.lpavs.caliinda.feature.calendar.presentation.CalendarViewModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system.IntroState
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalendarPagerScreen(
    calendarViewModel: CalendarViewModel,
    eventManagementViewModel: EventManagementViewModel,
    authViewModel: AuthViewModel,
    agentViewModel: AgentViewModel,
    calendarPagerState: PagerState,
    dailyViewPagerState: PagerState,
    weekViewPagerState: PagerState,
    signedIn: Boolean,
    createEventAction: () -> Unit,
    isOverallLoading: Boolean,
    initialPageIndex: Int,
    today: LocalDate,
    activity: Activity?,
    introductionState: IntroState
) {
  val userScrollEnabled = signedIn
  HorizontalPager(
      state = calendarPagerState,
      modifier = Modifier.fillMaxSize(),
      userScrollEnabled = userScrollEnabled) { page ->
        when (page) {
          0 ->
              VerticalPager(
                  state = weekViewPagerState,
                  modifier = Modifier.fillMaxSize(),
                  flingBehavior =
                      PagerDefaults.flingBehavior(
                          state = weekViewPagerState, snapPositionalThreshold = 0.05f),
                  userScrollEnabled = false,
                  beyondViewportPageCount = 1) { pageIndex ->
                    ProjectEventsPage(
                        isLoading = isOverallLoading,
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
                  userScrollEnabled = userScrollEnabled,
                  beyondViewportPageCount = 1) { pageIndex ->
                    val pageDate =
                        remember(pageIndex) {
                          today.plusDays((pageIndex - initialPageIndex).toLong())
                        }
                    DayEventsPage(
                        isLoading = isOverallLoading,
                        isSignedIn = signedIn,
                        date = pageDate,
                        viewModel = calendarViewModel,
                        eventManagementViewModel = eventManagementViewModel,
                        onSignInClick = {
                          if (activity != null) {
                            authViewModel.signIn(activity)
                          } else {
                            Log.e("MainScreen", "Activity is null, cannot start sign-in flow.")
                          }
                        },
                        agentViewModel = agentViewModel,
                        createEventClick = createEventAction,
                        introductionState = introductionState)
                  }
        }
      }
}
