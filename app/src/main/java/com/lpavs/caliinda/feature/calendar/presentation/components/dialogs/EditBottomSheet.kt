package com.lpavs.caliinda.feature.calendar.presentation.components.dialogs

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.lpavs.caliinda.core.data.remote.calendar.EventUpdateMode
import com.lpavs.caliinda.core.data.remote.calendar.dto.EventDto
import com.lpavs.caliinda.feature.event_management.ui.edit.EditEventScreen
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBottomSheet(
    show: Boolean,
    sheetState: SheetState,
    eventToEdit: EventDto?,
    mode: EventUpdateMode?,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!show || eventToEdit == null || mode == null) return

    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            scope.launch { sheetState.hide() }
                .invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismiss()
                    }
                }
        },
        contentWindowInsets = { WindowInsets.navigationBars }
    ) {
        content()
    }
}
