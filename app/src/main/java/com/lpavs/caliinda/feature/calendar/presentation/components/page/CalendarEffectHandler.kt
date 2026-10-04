package com.lpavs.caliinda.feature.calendar.presentation.components.page

import android.content.Context
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.lpavs.caliinda.feature.calendar.presentation.CalendarUiEvent
import com.lpavs.caliinda.feature.calendar.presentation.CalendarViewModel
import com.lpavs.caliinda.feature.event_management.vm.EventManagementUiEvent
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import kotlinx.coroutines.flow.merge
import java.time.LocalDate

@Composable
fun CalendarEffectHandler(
    calendarViewModel: CalendarViewModel,
    eventManagementViewModel: EventManagementViewModel,
    pagerState: PagerState,
    snackbarHostState: SnackbarHostState,
    context: Context = LocalContext.current,
    initialPageIndex: Int,
    today: LocalDate,
) {
  // Разрешение могли выдать/отозвать в системных настройках, пока приложение было свёрнуто.
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { calendarViewModel.onCalendarPermissionChanged() }

  LaunchedEffect(pagerState.targetPage) {
    val settledDate = today.plusDays((pagerState.targetPage - initialPageIndex).toLong())
    calendarViewModel.onVisibleDateChanged(settledDate)
  }

  LaunchedEffect(Unit) {
    merge(calendarViewModel.eventFlow, eventManagementViewModel.eventFlow).collect { event ->
      when (event) {
        is CalendarUiEvent.ShowMessage -> {
          snackbarHostState.showSnackbar(event.message)
        }
        is EventManagementUiEvent.ShowMessage -> {
          snackbarHostState.showSnackbar(event.message.asString(context))
        }
        is EventManagementUiEvent.OperationSuccess -> {}
      }
    }
  }
}
