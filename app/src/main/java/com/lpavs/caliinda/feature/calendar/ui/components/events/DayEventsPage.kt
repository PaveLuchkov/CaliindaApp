package com.lpavs.caliinda.feature.calendar.ui.components.events

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.common.EventNetworkState
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.feature.calendar.ui.CalendarViewModel
import com.lpavs.caliinda.feature.event_management.ui.shared.DeleteConfirmationDialog
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringEventDeleteOptionsDialog
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DayEventsPage(
    isLoading: Boolean,
    date: LocalDate,
    viewModel: CalendarViewModel,
    eventManagementViewModel: EventManagementViewModel
) {
    val pageState by viewModel.getDayPageUiState(date)
        .collectAsStateWithLifecycle(initialValue = DayPageUiState(isLoading = true))
    val listState = rememberLazyListState()

  val eventManagementState by eventManagementViewModel.uiState.collectAsStateWithLifecycle()

  val rangeNetworkState by viewModel.rangeNetworkState.collectAsStateWithLifecycle()
  val isBusy = isLoading || rangeNetworkState is EventNetworkState.Loading

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

  var expandedAllDayEventId by remember { mutableStateOf<String?>(null) }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      Spacer(modifier = Modifier.height(3.dp))
      if (pageState.allDayEvents.isNotEmpty()) {
        Spacer(modifier = Modifier.height(3.dp)) // Отступ после заголовка даты
        Column(
            modifier =
                Modifier.fillMaxWidth().padding(horizontal = 16.dp) // Общий горизонтальный отступ
            ) {
            pageState.allDayEvents.forEach { event ->
                val isExpanded = event.id == expandedAllDayEventId
                  AllDayEventItem(
                      event = event,
                      isExpanded = isExpanded,
                      onToggleExpand = {
                          expandedAllDayEventId =
                              if (expandedAllDayEventId == event.id) {
                                  null
                              } else {
                                  event.id
                              }
                      },
                      onDeleteClick = { eventManagementViewModel.requestDeleteConfirmation(event) },
                      onDetailsClick = { eventManagementViewModel.requestEventDetails(event) },
                      onEditClick = { eventManagementViewModel.requestEditEvent(event) },
                  )
                Spacer(modifier = Modifier.height(6.dp))
              }
            }
      }
      Spacer(modifier = Modifier.height(8.dp))

      if (pageState.timedEvents.isNotEmpty()) {
          CardsList(
              events = pageState.timedEvents,
              listState = listState,
              onDeleteRequest = eventManagementViewModel::requestDeleteConfirmation,
              onEditRequest = eventManagementViewModel::requestEditEvent,
              onDetailsRequest = eventManagementViewModel::requestEventDetails,
          )
      } else if (pageState.allDayEvents.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          if (isBusy) {
            LoadingIndicator(modifier = Modifier.size(80.dp))
          } else {
            Box(
                modifier =
                    Modifier.shadow(
                            elevation = 5.dp,
                            shape = RoundedCornerShape(CalendarUiDefaults.EventItemCornerRadius),
                            clip = false,
                        )
                        .clip(RoundedCornerShape(CalendarUiDefaults.EventItemCornerRadius))
                        .background(color = MaterialTheme.colorScheme.secondaryContainer)
                        .padding(16.dp),
                contentAlignment = Alignment.Center // Центрируем сообщение
                ) {
                  Text(
                      stringResource(R.string.no_events),
                      style = MaterialTheme.typography.bodyLarge,
                      color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
          }
        }
      } else {
        Spacer(modifier = Modifier.weight(1f))
      }
      if (eventManagementState.showDeleteConfirmationDialog &&
          eventManagementState.eventPendingDeletion != null) {
        DeleteConfirmationDialog(
            onConfirm = { eventManagementViewModel.confirmDeleteEvent() },
            onDismiss = { eventManagementViewModel.cancelDelete() })
      } else if (eventManagementState.showRecurringDeleteOptionsDialog &&
          eventManagementState.eventPendingDeletion != null) {
        RecurringEventDeleteOptionsDialog(
            eventName = eventManagementState.eventPendingDeletion!!.summary,
            onDismiss = { eventManagementViewModel.cancelDelete() },
            onOptionSelected = { choice ->
              eventManagementViewModel.confirmRecurringDelete(choice)
            })
      }
    }
  }
}
