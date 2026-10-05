@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import java.time.ZoneId
import com.lpavs.caliinda.core.ui.theme.AppMotion
import androidx.compose.animation.core.snap
import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.feature.event_management.ui.shared.DatePickerField
import com.lpavs.caliinda.feature.event_management.ui.shared.TimePickerField
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun EventDateTimePicker(
    modifier: Modifier = Modifier,
    state: EventDateTimeState,
    onStateChange: (EventDateTimeState) -> Unit,
    /** Пояс из настроек — «сейчас» для кнопок времени считается в нём, а не в поясе устройства. */
    zone: ZoneId,
    isLoading: Boolean = false,
    onRequestShowStartDatePicker: () -> Unit,
    onRequestShowStartTimePicker: () -> Unit,
    onRequestShowEndDatePicker: () -> Unit,
    onRequestShowEndTimePicker: () -> Unit,
    onRequestShowRecurrenceEndDatePicker: () -> Unit,
    /** false — правка одного экземпляра серии: своего правила у него быть не может. */
    showRecurrence: Boolean = true,
) {
    val isAllDay = state.isAllDay
    val isOneDay = remember(state.startDate, state.endDate) {
        state.startDate == state.endDate
    }


  val context = LocalContext.current

  val allDay = stringResource(R.string.all_day)
  val oneDay = stringResource(R.string.one_day)
  val recEvent = stringResource(R.string.recurrence_event)


  val deviceDateFormatter = remember {
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.getDefault())
  }
  val deviceTimeFormatter =
      remember(context) {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
      }

  val dateTimeError = state.validationError?.let { stringResource(it) }

  Column(modifier = modifier) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
      FilterChip(
          selected = isAllDay,
          onClick = { onStateChange(state.toggledAllDay(LocalTime.now(zone))) },
          label = { Text(allDay) },
          enabled = !isLoading)

      FilterChip(
          selected = isOneDay,
          onClick = { onStateChange(state.toggledOneDay()) },
          label = { Text(oneDay) },
          enabled = !isLoading)

      if (showRecurrence) {
        FilterChip(
            selected = state.isRecurring,
            onClick = { onStateChange(state.toggledRecurring()) },
            label = { Text(recEvent) },
            enabled = !isLoading)
      }
    }

    AnimatedVisibility(
        visible = dateTimeError != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()) {
          Column {
            Text(
                text = dateTimeError ?: "",
                color = colorScheme.error,
                style = typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(modifier = Modifier.height(4.dp))
          }
        }

    Column(
        modifier = Modifier.animateContentSize(animationSpec = AppMotion.defaultSpatialSpec()),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
          AnimatedContent(
              targetState = Pair(isAllDay, isOneDay),
              transitionSpec = {
                if (targetState.first != initialState.first ||
                        targetState.second != initialState.second) {
                      (fadeIn(animationSpec = AppMotion.fastEffectsSpec()) +
                          slideInVertically(
                              initialOffsetY = { it / 4 },
                              animationSpec = AppMotion.defaultSpatialSpec())) togetherWith
                          (fadeOut(animationSpec = AppMotion.fastEffectsSpec()) +
                              slideOutVertically(
                                  targetOffsetY = { -it / 4 }, animationSpec = AppMotion.defaultSpatialSpec()))
                    } else {
                      fadeIn(animationSpec = snap()) togetherWith
                          fadeOut(animationSpec = snap())
                    }
                    .using(SizeTransform(clip = true, sizeAnimationSpec = { _, _ -> AppMotion.defaultSpatialSpec() }))
              },
              label = "DateTimeFieldsAnimation") { targetLayoutState ->
                val (showAllDay, showOneDay) = targetLayoutState
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  when {
                    showAllDay && showOneDay -> {
                      DatePickerField(
                          state.startDate,
                          deviceDateFormatter,
                          isLoading,
                          onRequestShowStartDatePicker,
                          Modifier.fillMaxWidth().padding(horizontal = 50.dp))
                    }

                    showAllDay -> {
                      Row(
                          Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                          Arrangement.spacedBy(8.dp),
                          Alignment.Top) {
                            DatePickerField(
                                state.startDate,
                                deviceDateFormatter,
                                isLoading,
                                onRequestShowStartDatePicker,
                                Modifier.weight(1f))
                            DatePickerField(
                                state.endDate,
                                deviceDateFormatter,
                                isLoading,
                                onRequestShowEndDatePicker,
                                Modifier.weight(1f))
                          }
                    }

                    showOneDay -> {
                      Column(
                          horizontalAlignment = Alignment.CenterHorizontally,
                          verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                Alignment.CenterVertically) {
                                  TimePickerField(
                                      state.startTime,
                                      deviceTimeFormatter,
                                      isLoading,
                                      onRequestShowStartTimePicker,
                                      Modifier.width(100.dp),
                                      onLongClick = { onStateChange(state.startingAt(LocalTime.now(zone))) })
                                  Box(
                                      modifier =
                                          Modifier.width(10.dp)
                                              .height(1.dp)
                                              .background(color = colorScheme.onBackground))
                                  TimePickerField(
                                      state.endTime,
                                      deviceTimeFormatter,
                                      isLoading,
                                      onRequestShowEndTimePicker,
                                      Modifier.width(100.dp),
                                  )
                                }
                            DatePickerField(
                                state.startDate,
                                deviceDateFormatter,
                                isLoading,
                                onRequestShowStartDatePicker,
                                Modifier.width(218.dp))
                          }
                    }

                    else -> {
                      Row(
                          Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                          Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                          Alignment.Top) {
                            TimePickerField(
                                state.startTime,
                                deviceTimeFormatter,
                                isLoading,
                                onRequestShowStartTimePicker,
                                Modifier.weight(1f),
                                onLongClick = { onStateChange(state.startingAt(LocalTime.now(zone))) })
                            TimePickerField(
                                state.endTime,
                                deviceTimeFormatter,
                                isLoading,
                                onRequestShowEndTimePicker,
                                Modifier.weight(1f))
                          }
                      Row(
                          Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                          Arrangement.spacedBy(8.dp),
                          Alignment.Top) {
                            DatePickerField(
                                state.startDate,
                                deviceDateFormatter,
                                isLoading,
                                onRequestShowStartDatePicker,
                                Modifier.weight(1f))
                            DatePickerField(
                                state.endDate,
                                deviceDateFormatter,
                                isLoading,
                                onRequestShowEndDatePicker,
                                Modifier.weight(1f))
                          }
                    }
                  }
                }
              }
        }

    // Повтор одной строкой; все настройки — в отдельной шторке, форма остаётся короткой.
    var showRecurrenceSheet by rememberSaveable { mutableStateOf(false) }
    AnimatedVisibility(
        visible = showRecurrence && state.isRecurring,
        enter =
            fadeIn(animationSpec = AppMotion.fastEffectsSpec()) +
                expandVertically(animationSpec = AppMotion.defaultSpatialSpec()),
        exit = fadeOut(animationSpec = AppMotion.fastEffectsSpec()) + shrinkVertically(animationSpec = AppMotion.defaultSpatialSpec()),
        modifier = Modifier.padding(top = 8.dp)) {
          RecurrenceSummaryField(
              state = state,
              isLoading = isLoading,
              onClick = { showRecurrenceSheet = true },
              modifier = Modifier.padding(horizontal = 8.dp))
        }
    if (showRecurrenceSheet && showRecurrence && state.isRecurring) {
      RecurrenceSheet(
          state = state,
          onStateChange = onStateChange,
          onRequestEndDatePicker = onRequestShowRecurrenceEndDatePicker,
          onDismiss = { showRecurrenceSheet = false })
    }
  }
}

sealed class RecurrenceOption(@StringRes val labelResId: Int, val rruleValue: String?) {
  data object None : RecurrenceOption(R.string.recurrence_none, null)

  data object Daily : RecurrenceOption(R.string.recurrence_daily, "FREQ=DAILY")

  data object Weekly : RecurrenceOption(R.string.recurrence_weekly, "FREQ=WEEKLY")

  data object Monthly : RecurrenceOption(R.string.recurrence_monthly, "FREQ=MONTHLY")

  data object Yearly : RecurrenceOption(R.string.recurrence_yearly, "FREQ=YEARLY")

  companion object {
    // Геттер, а не поле: поле компаньона инициализируется вместе с классом, и если первым
    // тронули, например, Weekly, на его месте в списке оказался бы null.
    val ALL_OPTIONS: List<RecurrenceOption>
      get() = listOf(None, Daily, Weekly, Monthly, Yearly)

    fun fromRule(rule: String?): RecurrenceOption {
      return when (rule) {
        Daily.rruleValue -> Daily
        Weekly.rruleValue -> Weekly
        Monthly.rruleValue -> Monthly
        Yearly.rruleValue -> Yearly
        else -> None
      }
    }
  }
}
