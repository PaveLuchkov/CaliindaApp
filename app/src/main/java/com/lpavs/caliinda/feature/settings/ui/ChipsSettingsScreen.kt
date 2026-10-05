package com.lpavs.caliinda.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.suggestions.HourWindow
import com.lpavs.caliinda.core.data.suggestions.SuggestionChip
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.util.fullText
import com.lpavs.caliinda.core.ui.util.shortText
import com.lpavs.caliinda.feature.settings.vm.ChipsSettingsViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Полный список чипов-подсказок: свои и встроенные, правятся одинаково. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChipsSettingsScreen(viewModel: ChipsSettingsViewModel, onNavigateBack: () -> Unit) {
  val chips by viewModel.chips.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  // Открытая шторка: id чипа или «новый»; null — закрыта.
  var editingId by rememberSaveable { mutableStateOf<String?>(null) }
  val listState = rememberLazyListState()
  // Новый чип встаёт в начало, а список держится за первый видимый элемент — прокручиваем
  // к нему, иначе добавленный остался бы выше края экрана.
  var scrollToTop by remember { mutableStateOf(false) }
  LaunchedEffect(chips) {
    if (scrollToTop) {
      listState.animateScrollToItem(0)
      scrollToTop = false
    }
  }
  val undoLabel = stringResource(R.string.undo)
  val resetDone = stringResource(R.string.chips_reset_done)

  fun offerUndo(message: String, before: List<SuggestionChip>) {
    scope.launch {
      snackbarHostState.currentSnackbarData?.dismiss()
      val result =
          snackbarHostState.showSnackbar(
              message, actionLabel = undoLabel, duration = SnackbarDuration.Short)
      if (result == SnackbarResult.ActionPerformed) viewModel.restore(before)
    }
  }

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        LargeFlexibleTopAppBar(
            title = { Text(stringResource(R.string.chips_settings_title)) },
            subtitle = { Text(stringResource(R.string.chips_settings_summary)) },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back))
              }
            },
            actions = {
              IconButton(
                  onClick = { scope.launch { offerUndo(resetDone, viewModel.reset()) } }) {
                    Icon(
                        Icons.Rounded.RestartAlt,
                        contentDescription = stringResource(R.string.chips_reset))
                  }
            },
            scrollBehavior = scrollBehavior)
      },
      floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = { editingId = NEW_CHIP },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.chip_add)) })
      }) { paddingValues ->
        LazyColumn(
            state = listState,
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
              itemsIndexed(chips, key = { _, it -> it.id }) { index, chip ->
                val name = chip.shortText(context)
                val deletedMessage = stringResource(R.string.chip_deleted, name)
                SegmentedListItem(
                    onClick = { editingId = chip.id },
                    shapes = settingsShapes(index = index, count = chips.size),
                    colors =
                        ListItemDefaults.segmentedColors(
                            containerColor = colorScheme.surfaceContainer),
                    supportingContent = {
                      Text(
                          listOf(chip.fullText(context), hoursText(chip.window))
                              .joinToString(" · "),
                          maxLines = 1)
                    },
                    trailingContent = {
                      IconButton(
                          onClick = {
                            scope.launch { offerUndo(deletedMessage, viewModel.delete(chip.id)) }
                          }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.delete))
                          }
                    }) {
                      Text(name)
                    }
              }
              // Место под FAB, чтобы он не закрывал последний чип.
              item { Spacer(Modifier.height(96.dp)) }
            }
      }

  editingId?.let { id ->
    val existing = chips.firstOrNull { it.id == id }
    ChipEditSheet(
        initial = existing,
        onSave = { short, full, window ->
          if (existing == null) scrollToTop = true
          val base = existing ?: SuggestionChip(id = viewModel.newChipId())
          // Нетронутые названия встроенного чипа не фиксируем — пусть дальше переводятся.
          val defaults = base.copy(shortName = null, fullName = null)
          val fullOrShort = full.ifBlank { short }
          viewModel.save(
              base.copy(
                  shortName = short.takeUnless { base.builtIn && it == defaults.shortText(context) },
                  fullName =
                      fullOrShort.takeUnless { base.builtIn && it == defaults.fullText(context) },
                  window = window))
          editingId = null
        },
        onDismiss = { editingId = null })
  }
}

private const val NEW_CHIP = "__new__"

@Composable
private fun hoursText(window: HourWindow?): String {
  if (window == null) return stringResource(R.string.chip_any_time)
  val formatter = remember {
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
  }
  return "${window.from.format(formatter)}–${window.to.format(formatter)}"
}

/** Шторка правки: подпись, полное название и рекомендуемые часы. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChipEditSheet(
    initial: SuggestionChip?,
    onSave: (short: String, full: String, window: HourWindow?) -> Unit,
    onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var short by rememberSaveable { mutableStateOf(initial?.shortText(context).orEmpty()) }
  var full by rememberSaveable { mutableStateOf(initial?.fullText(context).orEmpty()) }
  var hasHours by rememberSaveable { mutableStateOf(initial?.window != null) }
  var from by remember { mutableStateOf(initial?.window?.from ?: LocalTime.of(13, 0)) }
  var to by remember { mutableStateOf(initial?.window?.to ?: LocalTime.of(15, 0)) }
  var picking by remember { mutableStateOf<Boolean?>(null) } // true — «с», false — «до»

  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                  stringResource(
                      if (initial == null) R.string.chip_new_title else R.string.chip_edit_title),
                  style = typography.headlineSmall)
              OutlinedTextField(
                  value = short,
                  onValueChange = { short = it.take(MAX_SHORT) },
                  label = { Text(stringResource(R.string.chip_short_name)) },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                  shape = RoundedCornerShape(cuid.ContainerCornerRadius),
                  modifier = Modifier.fillMaxWidth())
              OutlinedTextField(
                  value = full,
                  onValueChange = { full = it },
                  label = { Text(stringResource(R.string.chip_full_name)) },
                  placeholder = { Text(stringResource(R.string.chip_full_name_hint)) },
                  singleLine = true,
                  shape = RoundedCornerShape(cuid.ContainerCornerRadius),
                  modifier = Modifier.fillMaxWidth())

              Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                  Text(stringResource(R.string.chip_hours), style = typography.titleMedium)
                  Text(
                      stringResource(R.string.chip_hours_summary),
                      style = typography.bodyMedium,
                      color = colorScheme.onSurfaceVariant)
                }
                Switch(checked = hasHours, onCheckedChange = { hasHours = it })
              }
              if (hasHours) {
                val formatter = remember {
                  DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  FilledTonalButton(onClick = { picking = true }, modifier = Modifier.weight(1f)) {
                    Text("${stringResource(R.string.chip_from)} ${from.format(formatter)}")
                  }
                  FilledTonalButton(onClick = { picking = false }, modifier = Modifier.weight(1f)) {
                    Text("${stringResource(R.string.chip_to)} ${to.format(formatter)}")
                  }
                }
              }

              Button(
                  onClick = {
                    onSave(short.trim(), full.trim(), if (hasHours) HourWindow(from, to) else null)
                  },
                  enabled = short.isNotBlank(),
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(vertical = cuid.ContainerPadding)
                          .height(ButtonDefaults.MediumContainerHeight),
                  shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight)) {
                    Text(
                        stringResource(R.string.save),
                        style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
                  }
            }
      }

  picking?.let { isFrom ->
    val initialTime = if (isFrom) from else to
    val state =
        rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute)
    AlertDialog(
        onDismissRequest = { picking = null },
        confirmButton = {
          TextButton(
              onClick = {
                val time = LocalTime.of(state.hour, state.minute)
                if (isFrom) from = time else to = time
                picking = null
              }) {
                Text(stringResource(android.R.string.ok))
              }
        },
        dismissButton = {
          TextButton(onClick = { picking = null }) { Text(stringResource(R.string.cancel)) }
        },
        text = { TimePicker(state = state) })
  }
}

/** Подпись на чипе короткая — длинное уходит в полное название. */
private const val MAX_SHORT = 24
