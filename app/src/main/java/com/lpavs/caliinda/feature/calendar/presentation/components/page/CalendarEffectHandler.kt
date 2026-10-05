package com.lpavs.caliinda.feature.calendar.presentation.components.page

import kotlinx.coroutines.launch
import com.lpavs.caliinda.R
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import android.content.Context
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.lpavs.caliinda.feature.calendar.presentation.CalendarUiEvent
import com.lpavs.caliinda.feature.calendar.presentation.CalendarViewModel
import com.lpavs.caliinda.feature.event_management.vm.EventManagementUiEvent
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

@Composable
fun CalendarEffectHandler(
    calendarViewModel: CalendarViewModel,
    eventManagementViewModel: EventManagementViewModel,
    pagerState: PagerState,
    snackbarHostState: SnackbarHostState,
    context: Context = LocalContext.current,
    initialPageIndex: Int,
    anchorDate: LocalDate,
) {
  // Разрешение могли выдать/отозвать в системных настройках, пока приложение было свёрнуто.
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { calendarViewModel.onCalendarPermissionChanged() }

  LaunchedEffect(pagerState.targetPage) {
    val settledDate = anchorDate.plusDays((pagerState.targetPage - initialPageIndex).toLong())
    calendarViewModel.onVisibleDateChanged(settledDate)
  }

  val accessibilityManager = LocalAccessibilityManager.current
  val undoLabel = stringResource(R.string.undo)
  LaunchedEffect(Unit) {
    merge(calendarViewModel.events, eventManagementViewModel.events).collect { event ->
      when (event) {
        is CalendarUiEvent.ShowMessage -> {
          snackbarHostState.showSnackbar(event.message.asString(context))
        }
        is EventManagementUiEvent.ShowMessage -> {
          snackbarHostState.showSnackbar(event.message.asString(context))
        }
        is EventManagementUiEvent.ShowUndoDelete ->
            // Отдельной корутиной: пока висит снекбар, остальные события тоже должны доходить.
            launch {
              // Long держится 10 с — слишком долго. Своё окно, но с поправкой на настройки
              // доступности; по таймауту снекбар закрывается вместе с корутиной.
              val window =
                  accessibilityManager?.calculateRecommendedTimeoutMillis(
                      UNDO_WINDOW_MS, containsIcons = false, containsText = true,
                      containsControls = true) ?: UNDO_WINDOW_MS
              // Таймер идёт и в очереди — убираем текущий снекбар, чтобы окно не съелось ожиданием.
              snackbarHostState.currentSnackbarData?.dismiss()
              val result =
                  withTimeoutOrNull(window) {
                    snackbarHostState.showSnackbar(
                        message = event.message.asString(context),
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Indefinite)
                  }
              if (result == SnackbarResult.ActionPerformed) {
                eventManagementViewModel.undoDelete(event.eventId)
              } else {
                eventManagementViewModel.commitDelete(event.eventId)
              }
            }
      }
    }
  }
}

/** Сколько висит снекбар «Отменить» после удаления. */
private const val UNDO_WINDOW_MS = 5_000L
