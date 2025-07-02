package com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.sections

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
import com.lpavs.caliinda.ui.screens.main.components.UIDefaults.cuid
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.ui.DatePickerField
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.ui.TimePickerField
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

// Состояние, которое компонент будет возвращать наружу
data class EventDateTimeState(
    val startDate: LocalDate,
    val startTime: LocalTime?,
    val endDate: LocalDate,
    val endTime: LocalTime?,
    val isAllDay: Boolean,
    val isRecurring: Boolean,
    val recurrenceRule: String? = null,
    val selectedWeekdays: Set<DayOfWeek> = emptySet(),
    val recurrenceEndType: RecurrenceEndType = RecurrenceEndType.NEVER, // Тип окончания
    val recurrenceEndDate: LocalDate? = null, // Дата для UNTIL
    val recurrenceCount: Int? = null
)

sealed class RecurrenceOption(@StringRes val labelResId: Int, val rruleValue: String?) {
  data object None : RecurrenceOption(R.string.recurrence_none, null)

  data object Daily : RecurrenceOption(R.string.recurrence_daily, "FREQ=DAILY")

  data object Weekly : RecurrenceOption(R.string.recurrence_weekly, "FREQ=WEEKLY")

  data object Monthly : RecurrenceOption(R.string.recurrence_monthly, "FREQ=MONTHLY")

  data object Yearly : RecurrenceOption(R.string.recurrence_yearly, "FREQ=YEARLY")

  // Можно добавить Custom для ручного ввода или сложных правил

  companion object {
    // --- ИЗМЕНЕНИЕ ЗДЕСЬ ---
    val ALL_OPTIONS: List<RecurrenceOption> = listOf(None, Daily, Weekly, Monthly, Yearly)

    // ----------------------

    // Ваша функция fromRule выглядит хорошо, но ее можно немного улучшить
    // для большей безопасности и чтобы она использовала ALL_OPTIONS, если это предпочтительнее.
    // Текущая реализация fromRule не использует ALL_OPTIONS и это нормально.
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
    onRequestShowRecurrenceEndDatePicker: () -> Unit
) {
  Log.d("EventDateTimePicker", "Received state: $state")
  var isAllDay by remember { mutableStateOf(state.isAllDay) }
  var isOneDay by remember { mutableStateOf(state.startDate == state.endDate) }

  var dateTimeError by remember { mutableStateOf<String?>(null) }
  val context = LocalContext.current

  val timeStart = stringResource(R.string.time_start)
  val timeEnd = stringResource(R.string.time_end)
  val dateStart = stringResource(R.string.date_start)
  val dateEnd = stringResource(R.string.date_end)
  val dateSingle = stringResource(R.string.date_single)
  val allDay = stringResource(R.string.all_day)
  val oneDay = stringResource(R.string.one_day)
  val recEvent = stringResource(R.string.recurrence_event)
  val endsLabel = stringResource(R.string.recurrence_ends) // <-- Строка "Ends"
  val endNeverLabel = stringResource(R.string.recurrence_end_never) // <-- Строка "Never"
  val endDateLabel = stringResource(R.string.recurrence_end_date) // <-- Строка "On date"
  val endCountLabel = stringResource(R.string.recurrence_end_count) // <-- Строка "After..."
  val occurrencesLabel = stringResource(R.string.recurrence_occurrences) // <-- Строка "occurrences"
  val recurrenceEndDateFieldLabel =
      stringResource(R.string.recurrence_end_date_field) // <-- Строка "End Date"
  val recurrenceCountFieldLabel =
      stringResource(R.string.recurrence_count_field) // <-- Строка "Number of times"

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
      // isOneDay = state.startDate == state.endDate
    }
  }
  // --- Эффект для валидации и оповещения об изменениях ---
  // Зависимости теперь включают isAllDay и isOneDay
  LaunchedEffect(state.startDate, state.endDate) {
    val actualIsOneDay = state.startDate == state.endDate
    if (isOneDay != actualIsOneDay) {
      isOneDay = actualIsOneDay
    }
  }

  LaunchedEffect(state) {
    dateTimeError = null // Сброс
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
            // isAllDay обновится через LaunchedEffect(state.isAllDay)
            // onStateChange вызовет рекомпозицию и эффект сработает

            val newState: EventDateTimeState
            if (newIsAllDay) {
              newState = state.copy(isAllDay = true, startTime = null, endTime = null)
            } else {
              // Turning All Day OFF
              val defaultStartTime =
                  state.startTime
                      ?: LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)

              var newEndTime = state.endTime
              var newEndDate = state.endDate // Начинаем с текущей даты конца

              // Если событие было однодневным (или даты совпадали до этого изменения)
              if (state.startDate == state.endDate) {
                // Если endTime не было или оно было некорректным для однодневного
                if (newEndTime == null || !defaultStartTime.isBefore(newEndTime)) {
                  newEndTime = defaultStartTime.plusHours(1)
                }
                if (newEndTime != null) {
                  newEndTime = newEndTime.withNano(0)
                }

                // Если после установки endTime оно оказалось раньше startTime (переход через
                // полночь)
                if (newEndTime != null) {
                  if (newEndTime.isBefore(defaultStartTime)) {
                    newEndDate = state.startDate.plusDays(1)
                  }
                }
              } else { // Событие было многодневным
                if (newEndTime == null) { // Если endTime не было
                  // Для многодневного, если время конца не установлено, можно сделать его равным
                  // времени начала
                  // или на час позже, на той же endDate.
                  newEndTime = defaultStartTime.plusHours(1) // или defaultStartTime
                }
                if (newEndTime != null) {
                  newEndTime = newEndTime.withNano(0)
                }
                // newEndDate уже state.endDate (т.е. многодневное)
                // Валидатор проверит, что startTime на startDate не позже endTime на newEndDate
              }

              newState =
                  state.copy(
                      isAllDay = false,
                      startTime = defaultStartTime,
                      endTime = newEndTime,
                      endDate = newEndDate // Обновленная дата конца
                      )
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
            val targetIsOneDay = !currentActualIsOneDay // Целевое состояние для "One Day"

            val newEndDateCandidate: LocalDate
            var newEndTimeCandidate = state.endTime

            if (targetIsOneDay) {
              // Включаем "One Day": endDate делаем равным startDate
              newEndDateCandidate = state.startDate

              if (!state.isAllDay && state.startTime != null) {
                val currentStartTime = state.startTime
                // Если endTime не задано или (на одной дате) endTime раньше или равно startTime,
                // корректируем endTime.
                if (newEndTimeCandidate == null ||
                    !currentStartTime.isBefore(newEndTimeCandidate)) {
                  newEndTimeCandidate = currentStartTime.plusHours(1).withNano(0)
                  // Если добавление часа привело к переходу через полночь (endTime раньше
                  // startTime)
                  if (newEndTimeCandidate.isBefore(currentStartTime)) {
                    newEndTimeCandidate =
                        LocalTime.of(23, 59, 0, 0) // Устанавливаем на конец текущего дня
                  }
                }
              }
            } else {
              newEndDateCandidate = state.startDate.plusDays(1)
              // Если событие уже было многодневным, endDate и endTime не меняются.
            }
            onStateChange(state.copy(endDate = newEndDateCandidate, endTime = newEndTimeCandidate))
          },
          label = { Text(oneDay) },
          // Чип "One Day" доступен всегда (кроме isLoading), т.к. управляет равенством дат
          // независимо от "All Day".
          enabled = !isLoading)

      // --- Чип "Recurring" ---
      FilterChip(
          selected = state.isRecurring,
          onClick = {
            val newIsRecurring = !state.isRecurring
            val newRule =
                if (!newIsRecurring) {
                  // Выключаем повторение - сбрасываем правило
                  null
                } else {
                  // Включаем повторение - ставим Daily по умолчанию, если правила нет
                  state.recurrenceRule ?: RecurrenceOption.Daily.rruleValue
                }
            onStateChange(state.copy(isRecurring = newIsRecurring, recurrenceRule = newRule))
          },
          label = { Text(recEvent) }, // Текст из ресурса
          enabled = !isLoading)
    }

    // Animate error visibility for a smoother appearance/disappearance
    AnimatedVisibility(
        visible = dateTimeError != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()) {
          Column { // Wrap in column to allow spacer even when text invisible
            Text(
                text = dateTimeError ?: "", // Provide default empty string
                color = colorScheme.error,
                style = typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(modifier = Modifier.height(4.dp))
          }
        }

    // --- Поля ввода Даты/Времени (АНИМИРОВАННЫЕ) ---
    // Apply animateContentSize to the container whose size changes
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)) { // Inner column for spacing
                      when {
                        // Case: All Day + One Day
                        showAllDay && showOneDay -> {
                          DatePickerField(
                              dateSingle,
                              state.startDate,
                              deviceDateFormatter,
                              dateTimeError != null,
                              isLoading,
                              onRequestShowStartDatePicker,
                              Modifier.fillMaxWidth().padding(horizontal = 50.dp))
                        }
                        // Case: Only All Day
                        showAllDay -> {
                          Row(
                              Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                              Arrangement.spacedBy(8.dp),
                              Alignment.Top) {
                                DatePickerField(
                                    dateStart,
                                    state.startDate,
                                    deviceDateFormatter,
                                    dateTimeError != null,
                                    isLoading,
                                    onRequestShowStartDatePicker,
                                    Modifier.weight(1f))
                                DatePickerField(
                                    dateEnd,
                                    state.endDate,
                                    deviceDateFormatter,
                                    dateTimeError != null,
                                    isLoading,
                                    onRequestShowEndDatePicker,
                                    Modifier.weight(1f))
                              }
                        }
                        // Case: Only One Day
                        showOneDay -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)

                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                    Alignment.CenterVertically) {
                                    TimePickerField(
                                        timeStart,
                                        state.startTime,
                                        deviceTimeFormatter,
                                        dateTimeError != null && state.startTime == null,
                                        isLoading,
                                        onRequestShowStartTimePicker,
                                        Modifier.width(100.dp))
                                    Box(modifier = Modifier.width(10.dp).height(1.dp).background(color = colorScheme.onBackground))
                                    TimePickerField(
                                        timeEnd,
                                        state.endTime,
                                        deviceTimeFormatter,
                                        dateTimeError != null && state.endTime == null,
                                        isLoading,
                                        onRequestShowEndTimePicker,
                                        Modifier.width(100.dp))
                                }
                                DatePickerField(
                                    dateSingle,
                                    state.startDate,
                                    deviceDateFormatter,
                                    dateTimeError != null,
                                    isLoading,
                                    onRequestShowStartDatePicker,
                                    Modifier.width(218.dp))
                            }
                        }
                        // Case: Neither (Full Range)
                        else -> {
                          Row(
                              Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                              Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                              Alignment.Top) {
                                TimePickerField(
                                    timeStart,
                                    state.startTime,
                                    deviceTimeFormatter,
                                    dateTimeError != null && state.startTime == null,
                                    isLoading,
                                    onRequestShowStartTimePicker,
                                    Modifier.weight(1f))
                                TimePickerField(
                                    timeEnd,
                                    state.endTime,
                                    deviceTimeFormatter,
                                    dateTimeError != null && state.endTime == null,
                                    isLoading,
                                    onRequestShowEndTimePicker,
                                    Modifier.weight(1f))
                              }
                          Row(
                              Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                              Arrangement.spacedBy(8.dp),
                              Alignment.Top) {
                                DatePickerField(
                                    dateStart,
                                    state.startDate,
                                    deviceDateFormatter,
                                    dateTimeError != null,
                                    isLoading,
                                    onRequestShowStartDatePicker,
                                    Modifier.weight(1f))
                                DatePickerField(
                                    dateEnd,
                                    state.endDate,
                                    deviceDateFormatter,
                                    dateTimeError != null,
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
                horizontalAlignment = Alignment.CenterHorizontally) { // Добавим отступ
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
                              // Устанавливаем тип, ставим дату по умолчанию (если нет), сбрасываем
                              // количество
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
                              val defaultCount = state.recurrenceCount ?: 10 // Пример: 10 раз
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
                            label = recurrenceEndDateFieldLabel, // "End Date"
                            date = state.recurrenceEndDate,
                            dateFormatter = deviceDateFormatter,
                            isError = false,
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
                              // Пытаемся преобразовать в Int, игнорируем невалидный ввод
                              val count =
                                  text
                                      .filter { it.isDigit() }
                                      .toIntOrNull()
                                      ?.coerceAtLeast(1) // Минимум 1 повторение
                              // Обновляем только количество
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
    state: EventDateTimeState, // Нужен для copy()
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
            // Можно вернуть иконку, если хотите
            // { Icon(Icons.Filled.Check, contentDescription = null, modifier =
            // Modifier.size(FilterChipDefaults.IconSize)) }
            null
          } else null)
}
/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipForFrequencyOption(
    option: RecurrenceOption,
    currentSelection: RecurrenceOption,
    isLoading: Boolean,
    state: EventDateTimeState, // Принимаем полное состояние
    onStateChange: (EventDateTimeState) -> Unit // Принимаем лямбду
) {
    val isSelected = option == currentSelection
    FilterChip(
        selected = isSelected,
        onClick = {
            val newBaseRule = option.rruleValue
            val newIsRecurring = option != RecurrenceOption.None

            // --- Логика для selectedWeekdays при смене частоты ---
            val newSelectedDays = if (option == RecurrenceOption.Weekly) {
                // Если выбрали Weekly, оставляем текущие дни или ставим день старта по умолчанию
                state.selectedWeekdays.ifEmpty { setOf(state.startDate.dayOfWeek) }
            } else {
                // Если выбрали НЕ Weekly, очищаем дни
                emptySet()
            }

            onStateChange(state.copy(
                recurrenceRule = newBaseRule,
                isRecurring = newIsRecurring,
                selectedWeekdays = newSelectedDays // Обновляем дни
            ))
        },
        label = {
            Text(stringResource(option.labelResId))
        },
        enabled = !isLoading,
        leadingIcon = if (isSelected) {
            // { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            null
        } else null
    )
}*/
