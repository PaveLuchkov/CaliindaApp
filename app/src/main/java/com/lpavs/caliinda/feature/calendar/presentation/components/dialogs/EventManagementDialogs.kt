package com.lpavs.caliinda.feature.calendar.presentation.components.dialogs

import androidx.compose.runtime.Composable
import com.lpavs.caliinda.feature.event_management.ui.shared.DeleteConfirmationDialog
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringEventDeleteOptionsDialog
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringEventEditOptionsDialog
import com.lpavs.caliinda.feature.event_management.vm.EventManagementUiState
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel

@Composable
fun EventManagementDialogs(
    state: EventManagementUiState, // Замените на ваш тип стейта
    viewModel: EventManagementViewModel
) {
    // 1. Диалог выбора режима редактирования повторяющегося события
    if (state.showRecurringEditOptionsDialog && state.eventBeingEdited != null) {
        RecurringEventEditOptionsDialog(
            eventName = state.eventBeingEdited.summary,
            onDismiss = { viewModel.cancelEditEvent() },
            onOptionSelected = { choice ->
                viewModel.onRecurringEditOptionSelected(choice)
            }
        )
    }

    // 2. Диалог подтверждения удаления обычного события
    if (state.showDeleteConfirmationDialog && state.eventPendingDeletion != null) {
        DeleteConfirmationDialog(
            onConfirm = { viewModel.confirmDeleteEvent() },
            onDismiss = { viewModel.cancelDelete() }
        )
    }

    // 3. Диалог выбора режима удаления повторяющегося события
    else if (state.showRecurringDeleteOptionsDialog && state.eventPendingDeletion != null) {
        RecurringEventDeleteOptionsDialog(
            eventName = state.eventPendingDeletion.summary,
            onDismiss = { viewModel.cancelDelete() },
            onOptionSelected = { choice ->
                viewModel.confirmRecurringDelete(choice)
            }
        )
    }
}