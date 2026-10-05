package com.lpavs.caliinda.feature.calendar.presentation.components.dialogs

import com.lpavs.caliinda.core.ui.util.displayTitle
import androidx.compose.runtime.Composable
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringEventDeleteOptionsDialog
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringEventEditOptionsDialog

import com.lpavs.caliinda.core.data.calendar.model.EventUpdateMode
import com.lpavs.caliinda.feature.event_management.ui.shared.RecurringDeleteChoice
import com.lpavs.caliinda.feature.event_management.vm.EventDialog

/** Диалоги выбора режима для повторяющихся событий (обычные удаляются с «Отменить»). */
@Composable
fun EventManagementDialogs(
    dialog: EventDialog,
    onEditModeSelected: (EventUpdateMode) -> Unit,
    onDeleteModeSelected: (RecurringDeleteChoice) -> Unit,
    onDismiss: () -> Unit,
) {
  when (dialog) {
    is EventDialog.ChooseEditMode ->
        RecurringEventEditOptionsDialog(
            eventName = dialog.event.displayTitle(),
            onDismiss = onDismiss,
            onOptionSelected = onEditModeSelected)
    is EventDialog.ChooseDeleteMode ->
        RecurringEventDeleteOptionsDialog(
            eventName = dialog.event.displayTitle(),
            onDismiss = onDismiss,
            onOptionSelected = onDeleteModeSelected)
    else -> {}
  }
}
