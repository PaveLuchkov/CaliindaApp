package com.lpavs.caliinda.feature.calendar.presentation.components.dialogs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
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
      // Смахнули — шторка уже уезжает сама, со скоростью пальца. Свой hide() перезапустил бы её
      // с нуля: она замирала и уезжала рывком. Ждём, пока доедет; закрыли кнопкой — увозим сами.
      if (sheetState.targetValue == SheetValue.Hidden) {
        snapshotFlow { sheetState.currentValue }.first { it == SheetValue.Hidden }
      } else {
        sheetState.hide()
      }
      shown = null
    }
  }

  val current = shown ?: return
  // Верхний отступ по умолчанию ModalBottomSheet меняет по ходу свайпа: пока шторка под
  // статус-баром, он есть, вышла — пропал. Содержимое на столько же растёт, шторка становится
  // выше, её «развёрнутое» положение пересчитывается — и тянущуюся шторку отбрасывает наверх,
  // по кругу (на Pixel 9: статус-бар 173 px, рывок каждые ~100 px). Поэтому сверху отступа нет
  // вовсе, а высоту ограничиваем сами — шторка просто не заезжает под статус-бар.
  val density = LocalDensity.current
  val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
  val statusBar = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
  val bottom = with(density) { WindowInsets.safeDrawing.getBottom(this).toDp() }
  val maxContentHeight = windowHeight - statusBar - bottom - DRAG_HANDLE_HEIGHT - TOP_GAP
  ModalBottomSheet(
      sheetState = sheetState,
      onDismissRequest = onDismiss,
      contentWindowInsets = {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
      }) {
        // Шторка встала на место — форма может звать клавиатуру, не сбивая анимацию появления.
        Box(Modifier.heightIn(max = maxContentHeight)) {
          content(current, sheetState.currentValue != SheetValue.Hidden)
        }
      }
}

/** Ручка шторки M3: 4dp полоска и по 22dp сверху и снизу. */
private val DRAG_HANDLE_HEIGHT = 48.dp

/** Зазор под статус-баром, чтобы развёрнутая шторка не упиралась в него вплотную. */
private val TOP_GAP = 8.dp
