package com.lpavs.caliinda.feature.calendar.presentation.components.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.lpavs.caliinda.feature.calendar.presentation.CalendarViewModel
import com.lpavs.caliinda.feature.calendar.presentation.components.events.lists.ProjectsCardsList
import com.lpavs.caliinda.feature.event_management.ui.shared.DeleteConfirmationDialog
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringEventDeleteOptionsDialog
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProjectEventsPage(
    isLoading: Boolean,
    displayPosition: WeekState,
    viewModel: CalendarViewModel,
    eventManagementViewModel: EventManagementViewModel,
) {
    val listState = rememberLazyListState()
    val pageState by
    viewModel
        .getProjectsPageUiState(LocalDate.now())
        .collectAsStateWithLifecycle(initialValue = EventsPageUiState(isLoading = true))
    val eventManagementState by eventManagementViewModel.uiState.collectAsStateWithLifecycle()
    val rangeNetworkState by viewModel.rangeNetworkState.collectAsStateWithLifecycle()
    val isBusy = isLoading || rangeNetworkState is EventNetworkState.Loading
    Column(modifier = Modifier.fillMaxSize()) {
        if (pageState.events.isNotEmpty()) {
            ProjectsCardsList(
                events = pageState.events,
                listState = listState,
                onDeleteRequest = eventManagementViewModel::requestDeleteConfirmation,
                onEditRequest = eventManagementViewModel::requestEditEvent,
                onDetailsRequest = viewModel::requestEventDetails,
            )
        } else {
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
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            if (eventManagementState.showDeleteConfirmationDialog &&
                eventManagementState.eventPendingDeletion != null
            ) {
                DeleteConfirmationDialog(
                    onConfirm = { eventManagementViewModel.confirmDeleteEvent() },
                    onDismiss = { eventManagementViewModel.cancelDelete() })
            } else if (eventManagementState.showRecurringDeleteOptionsDialog &&
                eventManagementState.eventPendingDeletion != null
            ) {
                RecurringEventDeleteOptionsDialog(
                    eventName = eventManagementState.eventPendingDeletion!!.summary,
                    onDismiss = { eventManagementViewModel.cancelDelete() },
                    onOptionSelected = { choice ->
                        eventManagementViewModel.confirmRecurringDelete(
                            choice
                        )
                    })
            }
        }
    }
}