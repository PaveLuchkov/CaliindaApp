package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import android.text.format.DateFormat
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.feature.event_management.ui.shared.DatePickerField
import com.lpavs.caliinda.feature.event_management.ui.shared.TimePickerField
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

enum class RecurrenceEndType {
  NEVER,
  DATE,
  COUNT
}

data class EventDateTimeState(
    val startDate: LocalDate,
    val startTime: LocalTime?,
    val endDate: LocalDate,
    val endTime: LocalTime?,
    val isAllDay: Boolean,
    val isRecurring: Boolean,
    val recurrenceRule: String? = null,
    val selectedWeekdays: Set<DayOfWeek> = emptySet(),
    val recurrenceEndType: RecurrenceEndType = RecurrenceEndType.NEVER,
    val recurrenceEndDate: LocalDate? = null,
    val recurrenceCount: Int? = null
)

@Composable
fun EventDateTimePicker(
    modifier: Modifier = Modifier,
    state: EventDateTimeState,
    onStateChange: (EventDateTimeState) -> Unit,
    isLoading: Boolean = false,
    onRequestShowStartDatePicker: () -> Unit,
    onRequestShowStartTimePicker: () -> Unit,
    onRequestShowEndDatePicker: () -> Unit,
    onRequestShowEndTimePicker: () -> Unit,
    onRequestShowRecurrenceEndDatePicker: () -> Unit,
) {
  Log.d("EventDateTimePicker", "Received state: $state")
  var isAllDay by remember { mutableStateOf(state.isAllDay) }
  var isOneDay by remember { mutableStateOf(state.startDate == state.endDate) }

  var dateTimeError by remember { mutableStateOf<String?>(null) }
  val context = LocalContext.current

  val allDay = stringResource(R.string.all_day)
  val oneDay = stringResource(R.string.one_day)
  val recEvent = stringResource(R.string.recurrence_event)
  val endsLabel = stringResource(R.string.recurrence_ends)
  val endNeverLabel = stringResource(R.string.recurrence_end_never)
  val endDateLabel = stringResource(R.string.recurrence_end_date)
  val endCountLabel = stringResource(R.string.recurrence_end_count)
  val recurrenceCountFieldLabel = stringResource(R.string.recurrence_count_field)

  val weekdays = remember { DayOfWeek.entries.toTypedArray() }

  // --- Форматтеры для отображения (без изменений) ---
  val deviceDateFormatter = remember {
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.getDefault())
  }
  val deviceTimeFormatter =
      remember(context) {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
      }
  LaunchedEffect(state.isAllDay) {
    if (isAllDay != state.isAllDay) {
      isAllDay = state.isAllDay
    }
  }
  LaunchedEffect(state.startDate, state.endDate) {
    val actualIsOneDay = state.startDate == state.endDate
    if (isOneDay != actualIsOneDay) {
      isOneDay = actualIsOneDay
    }
  }

  LaunchedEffect(state) {
    dateTimeError = null
    val actualIsOneDay = state.startDate == state.endDate

    if (!actualIsOneDay && state.endDate.isBefore(state.startDate)) {
      dateTimeError = context.getString(R.string.error_end_date_before_start)
    } else if (!state.isAllDay) {
      if (state.startTime == null) {
        dateTimeError = context.getString(R.string.error_start_time_missing)
      } else if (state.endTime == null) {
        dateTimeError = context.getString(R.string.error_end_time_missing)
      } else {
        val startDateTime = state.startTime.atDate(state.startDate)
        val endDateTime = state.endTime.atDate(state.endDate)
        if (!startDateTime.isBefore(endDateTime)) {
          dateTimeError = context.getString(R.string.error_end_time_not_after_start)
        }
      }
    }
  }

  Column(modifier = modifier) {
    // --- Filter Chips ---
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
      // --- Чип "All Day" ---
      FilterChip(
          selected = isAllDay,
          onClick = {
            val newIsAllDay = !isAllDay

            val newState: EventDateTimeState
            if (newIsAllDay) {
              newState = state.copy(isAllDay = true, startTime = null, endTime = null)
            } else {
              val defaultStartTime =
                  state.startTime
                      ?: LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)

              var newEndTime = state.endTime
              var newEndDate = state.endDate

              if (state.startDate == state.endDate) {
                if (newEndTime == null || !defaultStartTime.isBefore(newEndTime)) {
                  newEndTime = defaultStartTime.plusHours(1)
                }
                if (newEndTime != null) {
                  newEndTime = newEndTime.withNano(0)
                }

                if (newEndTime != null) {
                  if (newEndTime.isBefore(defaultStartTime)) {
                    newEndDate = state.startDate.plusDays(1)
                  }
                }
              } else {
                if (newEndTime == null) {
                  newEndTime = defaultStartTime.plusHours(1)
                }
                if (newEndTime != null) {
                  newEndTime = newEndTime.withNano(0)
                }
              }

              newState =
                  state.copy(
                      isAllDay = false,
                      startTime = defaultStartTime,
                      endTime = newEndTime,
                      endDate = newEndDate)
            }
            onStateChange(newState)
          },
          label = { Text(allDay) },
          enabled = !isLoading)

      // --- Чип "One Day" ---
      FilterChip(
          selected = isOneDay,
          onClick = {
            val currentActualIsOneDay = state.startDate == state.endDate
            val targetIsOneDay = !currentActualIsOneDay

            val newEndDateCandidate: LocalDate
            var newEndTimeCandidate = state.endTime

            if (targetIsOneDay) {
              newEndDateCandidate = state.startDate

              if (!state.isAllDay && state.startTime != null) {
                val currentStartTime = state.startTime
                if (newEndTimeCandidate == null ||
                    !currentStartTime.isBefore(newEndTimeCandidate)) {
                  newEndTimeCandidate = currentStartTime.plusHours(1).withNano(0)
                  if (newEndTimeCandidate.isBefore(currentStartTime)) {
                    newEndTimeCandidate = LocalTime.of(23, 59, 0, 0)
                  }
                }
              }
            } else {
              newEndDateCandidate = state.startDate.plusDays(1)
            }
            onStateChange(state.copy(endDate = newEndDateCandidate, endTime = newEndTimeCandidate))
          },
          label = { Text(oneDay) },
          enabled = !isLoading)

      // --- Чип "Recurring" ---
      FilterChip(
          selected = state.isRecurring,
          onClick = {
            val newIsRecurring = !state.isRecurring
            val newRule =
                if (!newIsRecurring) {
                  null
                } else {
                  state.recurrenceRule ?: RecurrenceOption.Daily.rruleValue
                }
            onStateChange(state.copy(isRecurring = newIsRecurring, recurrenceRule = newRule))
          },
          label = { Text(recEvent) },
          enabled = !isLoading)
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

    // --- Поля ввода Даты/Времени ---
    Column(
        modifier = Modifier.animateContentSize(animationSpec = tween(300)),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
          AnimatedContent(
              targetState = Pair(isAllDay, isOneDay),
              transitionSpec = {
                if (targetState.first != initialState.first ||
                        targetState.second != initialState.second) {
                      (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                          slideInVertically(
                              initialOffsetY = { it / 4 },
                              animationSpec = tween(270, delayMillis = 90))) togetherWith
                          (fadeOut(animationSpec = tween(90)) +
                              slideOutVertically(
                                  targetOffsetY = { -it / 4 }, animationSpec = tween(120)))
                    } else {
                      fadeIn(animationSpec = tween(0)) togetherWith
                          fadeOut(animationSpec = tween(0))
                    }
                    .using(SizeTransform(clip = true, sizeAnimationSpec = { _, _ -> tween(250) }))
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
                                      onLongClick = {
                                        val selectedTime = LocalTime.now()
                                        var newEndTime = state.endTime
                                        if (state.startDate == state.endDate &&
                                            state.endTime != null &&
                                            !selectedTime.isBefore(state.endTime)) {
                                          newEndTime = selectedTime.plusHours(1).withNano(0)
                                        }
                                        onStateChange(
                                            state.copy(
                                                startTime = selectedTime, endTime = newEndTime))
                                      })
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
                                onLongClick = {
                                  val selectedTime = LocalTime.now()
                                  var newEndTime = state.endTime
                                  if (state.startDate == state.endDate &&
                                      state.endTime != null &&
                                      !selectedTime.isBefore(state.endTime)) {
                                    newEndTime = selectedTime.plusHours(1).withNano(0)
                                  }
                                  onStateChange(
                                      state.copy(startTime = selectedTime, endTime = newEndTime))
                                })
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
              } // End AnimatedContent
        } // End Column for Date/Time fields with animateContentSize

    // --- Настройки повторения (АНИМИРОВАННЫЕ) ---
    AnimatedVisibility(
        visible = state.isRecurring,
        enter =
            fadeIn(animationSpec = tween(150, delayMillis = 50)) +
                expandVertically(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = tween(300)),
        modifier = Modifier.padding(top = 8.dp)) {
          Column {
            // --- Чипы для выбора частоты RRULE ---
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .horizontalScroll(rememberScrollState()), // Горизонтальная прокрутка
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
              // Вычисляем текущий выбор один раз
              val currentSelection =
                  remember(state.recurrenceRule) { RecurrenceOption.fromRule(state.recurrenceRule) }

              // --- Явно создаем каждый чип ---
              // Чип "Ежедневно"
              FilterChipForOption(
                  option = RecurrenceOption.Daily,
                  currentSelection = currentSelection,
                  isLoading = isLoading,
                  onStateChange = onStateChange,
                  state = state)

              // Чип "Еженедельно"
              FilterChipForOption(
                  option = RecurrenceOption.Weekly,
                  currentSelection = currentSelection,
                  isLoading = isLoading,
                  onStateChange = onStateChange,
                  state = state)

              // Чип "Ежемесячно"
              FilterChipForOption(
                  option = RecurrenceOption.Monthly,
                  currentSelection = currentSelection,
                  isLoading = isLoading,
                  onStateChange = onStateChange,
                  state = state)

              // Чип "Ежегодно"
              FilterChipForOption(
                  option = RecurrenceOption.Yearly,
                  currentSelection = currentSelection,
                  isLoading = isLoading,
                  onStateChange = onStateChange,
                  state = state)
            }
            AnimatedVisibility(
                visible =
                    state.recurrenceRule ==
                        RecurrenceOption.Weekly.rruleValue, // Показываем только для Weekly
                enter =
                    fadeIn(animationSpec = tween(150, delayMillis = 50)) +
                        expandVertically(animationSpec = tween(300)),
                exit =
                    fadeOut(animationSpec = tween(150)) +
                        shrinkVertically(animationSpec = tween(300)),
            ) {
              Column {
                // --- Чипы для дней недели ---
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                    // Центрируем дни недели, если их немного
                    horizontalArrangement =
                        Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                      weekdays.forEach { day ->
                        val isSelected = day in state.selectedWeekdays
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                              val currentDays = state.selectedWeekdays
                              val newDays =
                                  if (isSelected) {
                                    // Не даем убрать последний выбранный день, если он один
                                    if (currentDays.size > 1) currentDays - day else currentDays
                                  } else {
                                    currentDays + day
                                  }
                              onStateChange(state.copy(selectedWeekdays = newDays))
                            },
                            label = {
                              Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                            },
                            enabled = !isLoading)
                      }
                    }
              }
            }
            Column(
                modifier = Modifier.padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                      text = endsLabel, // "Ending"
                      style = typography.titleSmall,
                      modifier = Modifier.padding(horizontal = 8.dp),
                      textAlign = TextAlign.Center)
                  // --- Чипы для выбора типа окончания (NEVER, DATE, COUNT) ---
                  Row(
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(horizontal = 8.dp)
                              .horizontalScroll(rememberScrollState()),
                      horizontalArrangement =
                          Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        // Чип "Never"
                        FilterChip(
                            selected = state.recurrenceEndType == RecurrenceEndType.NEVER,
                            onClick = {
                              // Устанавливаем тип, сбрасываем дату и количество
                              onStateChange(
                                  state.copy(
                                      recurrenceEndType = RecurrenceEndType.NEVER,
                                      recurrenceEndDate = null,
                                      recurrenceCount = null))
                            },
                            label = { Text(endNeverLabel) },
                            enabled = !isLoading)
                        // Чип "On date"
                        FilterChip(
                            selected = state.recurrenceEndType == RecurrenceEndType.DATE,
                            onClick = {
                              val defaultEndDate =
                                  state.recurrenceEndDate
                                      ?: state.startDate.plusMonths(1) // Пример: через месяц
                              onStateChange(
                                  state.copy(
                                      recurrenceEndType = RecurrenceEndType.DATE,
                                      recurrenceEndDate = defaultEndDate,
                                      recurrenceCount = null))
                              // Запрашиваем показ пикера, если дата была null
                              if (state.recurrenceEndDate == null) {
                                onRequestShowRecurrenceEndDatePicker()
                              }
                            },
                            label = { Text(endDateLabel) },
                            enabled = !isLoading)
                        // Чип "After..."
                        FilterChip(
                            selected = state.recurrenceEndType == RecurrenceEndType.COUNT,
                            onClick = {
                              val defaultCount = state.recurrenceCount ?: 10
                              onStateChange(
                                  state.copy(
                                      recurrenceEndType = RecurrenceEndType.COUNT,
                                      recurrenceEndDate = null,
                                      recurrenceCount = defaultCount))
                            },
                            label = { Text(endCountLabel) },
                            enabled = !isLoading)
                      }

                  // --- Поле для выбора даты окончания (UNTIL) ---
                  AnimatedVisibility(
                      visible = state.recurrenceEndType == RecurrenceEndType.DATE,
                      modifier = Modifier.padding(top = 8.dp)) {
                        DatePickerField(
                            // "End Date"
                            date = state.recurrenceEndDate,
                            dateFormatter = deviceDateFormatter,
                            isLoading = isLoading,
                            onClick = onRequestShowRecurrenceEndDatePicker, // Открываем пикер
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 50.dp))
                      }

                  // --- Поле для ввода количества (COUNT) ---
                  AnimatedVisibility(
                      visible = state.recurrenceEndType == RecurrenceEndType.COUNT,
                      modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = state.recurrenceCount?.toString() ?: "",
                            onValueChange = { text ->
                              val count =
                                  text.filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(1)
                              onStateChange(state.copy(recurrenceCount = count))
                            },
                            label = { Text(recurrenceCountFieldLabel) }, // "Number of times"
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 50.dp),
                            shape = RoundedCornerShape(cuid.ContainerCornerRadius),
                            enabled = !isLoading,
                            isError = state.recurrenceCount == null // Ошибка, если пусто
                            )
                      }
                } // --- КОНЕЦ СЕКЦИИ ОКОНЧАНИЯ ---
          }
        } // End AnimatedVisibility for Recurrence
  } // End Outer Column
}

// --- Вспомогательные Composable для полей ---
@Composable
private fun FilterChipForOption(
    option: RecurrenceOption,
    currentSelection: RecurrenceOption,
    isLoading: Boolean,
    state: EventDateTimeState,
    onStateChange: (EventDateTimeState) -> Unit
) {
  FilterChip(
      selected = (option == currentSelection),
      onClick = {
        onStateChange(
            state.copy(
                recurrenceRule = option.rruleValue,
                isRecurring = (option != RecurrenceOption.None)))
      },
      label = { Text(stringResource(option.labelResId)) },
      enabled = !isLoading,
      leadingIcon =
          if (option == currentSelection) {
            null
          } else null)
}

sealed class RecurrenceOption(@StringRes val labelResId: Int, val rruleValue: String?) {
  data object None : RecurrenceOption(R.string.recurrence_none, null)

  data object Daily : RecurrenceOption(R.string.recurrence_daily, "FREQ=DAILY")

  data object Weekly : RecurrenceOption(R.string.recurrence_weekly, "FREQ=WEEKLY")

  data object Monthly : RecurrenceOption(R.string.recurrence_monthly, "FREQ=MONTHLY")

  data object Yearly : RecurrenceOption(R.string.recurrence_yearly, "FREQ=YEARLY")

  companion object {
    val ALL_OPTIONS: List<RecurrenceOption> = listOf(None, Daily, Weekly, Monthly, Yearly)

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
