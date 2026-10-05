package com.lpavs.caliinda.feature.calendar.presentation.components.dialogs

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lpavs.caliinda.feature.event_management.vm.EventDialog

/**
 * Шторка формы события. Открыта, пока [dialog] — создание или редактирование; закрывается
 * анимацией, когда состояние сменилось (сохранили или отменили).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormSheet(
    dialog: EventDialog,
    onDismiss: () -> Unit,
    content: @Composable (dialog: EventDialog, sheetSettled: Boolean) -> Unit,
) {
  val target = dialog.takeIf { it is EventDialog.Creating || it is EventDialog.Editing }
  // Последнее открытое содержимое держим, пока шторка уезжает, — иначе она закрывалась бы пустой.
  var shown by remember { mutableStateOf(target) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  LaunchedEffect(target) {
    if (target != null) {
      shown = target
    } else if (shown != null) {
      sheetState.hide()
      shown = null
    }
  }

  val current = shown ?: return
  ModalBottomSheet(
      sheetState = sheetState,
      onDismissRequest = onDismiss,
      contentWindowInsets = { WindowInsets.navigationBars }) {
        // Шторка встала на место — форма может звать клавиатуру, не сбивая анимацию появления.
        content(current, sheetState.currentValue != SheetValue.Hidden)
      }
}
