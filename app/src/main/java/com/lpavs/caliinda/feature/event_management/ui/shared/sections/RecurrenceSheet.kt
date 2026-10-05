package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.util.recurrenceText
import java.time.DayOfWeek
import java.time.LocalDate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import com.lpavs.caliinda.core.ui.theme.CaliindaTheme
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Частоты, которые предлагает шторка, по порядку кнопок. */
private val FREQUENCIES =
    listOf(
        RecurrenceOption.Daily to R.plurals.recurrence_unit_day,
        RecurrenceOption.Weekly to R.plurals.recurrence_unit_week,
        RecurrenceOption.Monthly to R.plurals.recurrence_unit_month,
        RecurrenceOption.Yearly to R.plurals.recurrence_unit_year)

private const val MAX_INTERVAL = 99
private const val MAX_COUNT = 999
private const val DEFAULT_COUNT = 10

/** Повтор из полей формы одной строкой — для строки-сводки и заголовка шторки. */
@Composable
fun recurrenceSummary(state: EventDateTimeState): String {
  val freq = RecurrenceOption.fromRule(state.recurrenceRule).rruleValue?.removePrefix("FREQ=")
  return recurrenceText(
      freq = freq,
      interval = state.recurrenceInterval,
      byDay = state.selectedWeekdays,
      untilDate = state.recurrenceEndDate.takeIf { state.recurrenceEndType == RecurrenceEndType.DATE },
      count = state.recurrenceCount.takeIf { state.recurrenceEndType == RecurrenceEndType.COUNT })
}

/** Строка-сводка в форме: весь повтор одной строкой, тап — шторка с настройками. */
@Composable
internal fun RecurrenceSummaryField(
    state: EventDateTimeState,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Row(
      modifier =
          modifier
              .fillMaxWidth()
              // Как поля даты, но длинное правило переносится, а не обрезается.
              .heightIn(min = 45.dp)
              .clip(RoundedCornerShape(cuid.ContainerCornerRadius))
              .background(colorScheme.secondaryContainer)
              .clickable(enabled = !isLoading, onClick = onClick)
              .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.Repeat,
            contentDescription = null,
            tint = colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = recurrenceSummary(state),
            style = typography.bodyLarge,
            color = colorScheme.onSecondaryContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = stringResource(R.string.recurrence_settings_title),
            tint = colorScheme.onSecondaryContainer)
      }
}

/**
 * Шторка настройки повтора: интервал и частота, дни недели, окончание. Правки применяются
 * сразу — сводка в форме меняется вместе с ними, «Готово» только закрывает.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RecurrenceSheet(
    state: EventDateTimeState,
    onStateChange: (EventDateTimeState) -> Unit,
    onRequestEndDatePicker: () -> Unit,
    onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
              text = stringResource(R.string.recurrence_settings_title),
              style = typography.headlineSmall)
          Text(
              text = recurrenceSummary(state),
              style = typography.bodyLarge,
              color = colorScheme.primary)

          IntervalSection(state, onStateChange)

          val weekly = RecurrenceOption.fromRule(state.recurrenceRule) == RecurrenceOption.Weekly
          AnimatedVisibility(
              visible = weekly,
              enter = fadeIn() + expandVertically(),
              exit = fadeOut() + shrinkVertically()) {
                WeekdaysRow(state, onStateChange)
              }

          EndSection(state, onStateChange, onRequestEndDatePicker)

          Button(
              onClick = onDismiss,
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(vertical = cuid.ContainerPadding)
                      .height(ButtonDefaults.MediumContainerHeight),
              shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
              contentPadding =
                  ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight)) {
                Text(
                    stringResource(R.string.recurrence_done),
                    style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
              }
        }
  }
}

/** «− 2 +» и связанная группа единиц: подписи склоняются по числу («2 недели»). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IntervalSection(state: EventDateTimeState, onStateChange: (EventDateTimeState) -> Unit) {
  val n = state.recurrenceInterval
  Stepper(
      value = n,
      onValueChange = { onStateChange(state.copy(recurrenceInterval = it)) },
      range = 1..MAX_INTERVAL)
  val current = RecurrenceOption.fromRule(state.recurrenceRule)
  ConnectedChoice(
      items = FREQUENCIES,
      isSelected = { it.first == current },
      label = { pluralStringResource(it.second, n) },
      onSelect = { (option, _) ->
        var next = state.copy(recurrenceRule = option.rruleValue, isRecurring = true)
        // Неделя без дней — по умолчанию день начала, чтобы кнопки сразу показывали выбор.
        if (option == RecurrenceOption.Weekly && state.selectedWeekdays.isEmpty()) {
          next = next.copy(selectedWeekdays = setOf(state.startDate.dayOfWeek))
        }
        onStateChange(next)
      })
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WeekdaysRow(state: EventDateTimeState, onStateChange: (EventDateTimeState) -> Unit) {
  // Без явных дней RRULE повторяется в день начала — его и показываем выбранным.
  val selected = state.selectedWeekdays.ifEmpty { setOf(state.startDate.dayOfWeek) }
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DayOfWeek.entries.forEach { day ->
          val checked = day in selected
          ToggleButton(
              checked = checked,
              onCheckedChange = {
                // Последний день не снимаем: неделя без дней — бессмыслица.
                val next = if (checked) selected - day else selected + day
                if (next.isNotEmpty()) onStateChange(state.copy(selectedWeekdays = next))
              },
              shapes = ToggleButtonDefaults.shapes(),
              contentPadding = ButtonDefaults.ExtraSmallContentPadding,
              modifier = Modifier.weight(1f).aspectRatio(1f)) {
                Text(
                    day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = typography.labelMedium,
                    maxLines = 1)
              }
        }
      }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EndSection(
    state: EventDateTimeState,
    onStateChange: (EventDateTimeState) -> Unit,
    onRequestEndDatePicker: () -> Unit,
) {
  Text(text = stringResource(R.string.recurrence_ends), style = typography.titleSmall)
  ConnectedChoice(
      items = RecurrenceEndType.entries,
      isSelected = { it == state.recurrenceEndType },
      label = {
        stringResource(
            when (it) {
              RecurrenceEndType.NEVER -> R.string.recurrence_end_never
              RecurrenceEndType.DATE -> R.string.recurrence_end_date
              RecurrenceEndType.COUNT -> R.string.recurrence_end_count
            })
      },
      onSelect = { type ->
        when (type) {
          RecurrenceEndType.NEVER ->
              onStateChange(
                  state.copy(
                      recurrenceEndType = type, recurrenceEndDate = null, recurrenceCount = null))
          RecurrenceEndType.DATE -> {
            onStateChange(
                state.copy(
                    recurrenceEndType = type,
                    recurrenceEndDate = state.recurrenceEndDate ?: state.startDate.plusMonths(1),
                    recurrenceCount = null))
            // Дату по умолчанию сразу предлагаем поправить.
            if (state.recurrenceEndDate == null) onRequestEndDatePicker()
          }
          RecurrenceEndType.COUNT ->
              onStateChange(
                  state.copy(
                      recurrenceEndType = type,
                      recurrenceEndDate = null,
                      recurrenceCount = state.recurrenceCount ?: DEFAULT_COUNT))
        }
      })

  AnimatedVisibility(
      visible = state.recurrenceEndType == RecurrenceEndType.DATE,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()) {
        val formatter = remember {
          DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
        }
        FilledTonalButton(
            onClick = onRequestEndDatePicker,
            modifier = Modifier.fillMaxWidth()) {
              Icon(
                  Icons.Rounded.Event,
                  contentDescription = stringResource(R.string.recurrence_change_end_date),
                  modifier = Modifier.size(ButtonDefaults.IconSize))
              Spacer(Modifier.width(ButtonDefaults.IconSpacing))
              Text(state.recurrenceEndDate?.format(formatter).orEmpty())
            }
      }

  AnimatedVisibility(
      visible = state.recurrenceEndType == RecurrenceEndType.COUNT,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()) {
        val count = state.recurrenceCount ?: DEFAULT_COUNT
        Stepper(
            value = count,
            onValueChange = { onStateChange(state.copy(recurrenceCount = it)) },
            range = 1..MAX_COUNT,
            label = pluralStringResource(R.plurals.recurrence_times, count, count))
      }
}

/** Связанная группа кнопок M3 Expressive с одним выбранным вариантом. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> ConnectedChoice(
    items: List<T>,
    isSelected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        items.forEachIndexed { index, item ->
          ToggleButton(
              checked = isSelected(item),
              onCheckedChange = { onSelect(item) },
              shapes =
                  when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    items.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                  },
              modifier = Modifier.weight(1f).semantics { role = Role.RadioButton }) {
                Text(label(item), maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
        }
      }
}

/** «− N +»: крупное число между тональными кнопками; [label] — подпись вместо числа. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String = value.toString(),
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center) {
        FilledTonalIconButton(
            onClick = { onValueChange(value - 1) },
            enabled = value > range.first,
            shapes = IconButtonDefaults.shapes()) {
              Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.recurrence_decrease))
            }
        Text(
            text = label,
            style = typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 96.dp).padding(horizontal = 8.dp))
        FilledTonalIconButton(
            onClick = { onValueChange(value + 1) },
            enabled = value < range.last,
            shapes = IconButtonDefaults.shapes()) {
              Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.recurrence_increase))
            }
      }
}

@Preview(showBackground = true, wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE)
@Composable
private fun RecurrenceSummaryFieldPreview() {
  CaliindaTheme {
    RecurrenceSummaryField(
        state =
            EventDateTimeState(
                startDate = LocalDate.of(2026, 10, 5),
                startTime = null,
                endDate = LocalDate.of(2026, 10, 5),
                endTime = null,
                isAllDay = true,
                isRecurring = true,
                recurrenceRule = RecurrenceOption.Weekly.rruleValue,
                recurrenceInterval = 2,
                selectedWeekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                recurrenceEndType = RecurrenceEndType.COUNT,
                recurrenceCount = 10),
        isLoading = false,
        onClick = {})
  }
}
