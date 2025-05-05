package com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import android.text.format.DateFormat // Для определения формата времени 12/24
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.ui.DatePickerField
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.ui.TimePickerField
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import com.example.caliindar.R
import com.example.caliindar.ui.screens.main.components.UIDefaults.cuid
import java.time.DayOfWeek
import java.time.format.TextStyle


enum class RecurrenceEndType {
    NEVER, DATE, COUNT
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
    object None : RecurrenceOption(R.string.recurrence_none, null)
    object Daily : RecurrenceOption(R.string.recurrence_daily, "FREQ=DAILY")
    object Weekly : RecurrenceOption(R.string.recurrence_weekly, "FREQ=WEEKLY")
    object Monthly : RecurrenceOption(R.string.recurrence_monthly, "FREQ=MONTHLY")
    object Yearly : RecurrenceOption(R.string.recurrence_yearly, "FREQ=YEARLY")
    // Можно добавить Custom для ручного ввода или сложных правил

    companion object {
        fun fromRule(rule: String?): RecurrenceOption {
            return when (rule) {
                Daily.rruleValue -> Daily
                Weekly.rruleValue -> Weekly
                Monthly.rruleValue -> Monthly
                Yearly.rruleValue -> Yearly
                else -> None // Если null или не совпало - считаем "None"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val recurrenceEndDateFieldLabel = stringResource(R.string.recurrence_end_date_field) // <-- Строка "End Date"
    val recurrenceCountFieldLabel = stringResource(R.string.recurrence_count_field) // <-- Строка "Number of times"

    val weekdays = remember { DayOfWeek.entries.toTypedArray() }

    // --- Форматтеры для отображения (без изменений) ---
    val deviceDateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
    }
    val deviceTimeFormatter = remember(context) {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    }
    LaunchedEffect(state.isAllDay) {
        if (isAllDay != state.isAllDay) {
            isAllDay = state.isAllDay
            // Пересчитаем isOneDay на всякий случай, т.к. логика могла поменяться
            isOneDay = state.startDate == state.endDate
        }
    }
    // --- Эффект для валидации и оповещения об изменениях ---
    // Зависимости теперь включают isAllDay и isOneDay
    LaunchedEffect(state.startDate, state.endDate) {
        val derivedOneDay = state.startDate == state.endDate
        if (isOneDay != derivedOneDay) {
            isOneDay = derivedOneDay
            if (derivedOneDay && !isAllDay && state.startTime != null && state.endTime != null && !state.startTime.isBefore(state.endTime)) {
            }
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

    // --- Общая ошибка даты/времени ---
    // --- Поля ввода Даты/Времени (логика отображения изменена) ---
    Column(
        modifier = modifier
    ) {
        // --- Filter Chips ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            // --- Чип "All Day" ---
            FilterChip(
                selected = isAllDay, // Use internal flag for display
                onClick = {
                    val newIsAllDay = !isAllDay
                    isAllDay = newIsAllDay // Update internal flag FIRST for animation trigger

                    // Prepare the new external state based on the change
                    val newState = if (newIsAllDay) {
                        state.copy(
                            isAllDay = true,
                            startTime = null,
                            endTime = null
                        )
                    } else {
                        // Turning All Day OFF
                        val defaultStartTime = state.startTime // Try to keep existing
                            ?: LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
                        // Check internal isOneDay flag *after* it might have been toggled
                        val currentIsOneDay = state.startDate == state.endDate // Check actual state dates

                        val defaultEndTime = if (currentIsOneDay) {
                            (state.endTime ?: defaultStartTime.plusHours(1)).withNano(0)
                        } else {
                            (state.endTime ?: defaultStartTime).withNano(0)
                        }
                        val finalEndTime = if (currentIsOneDay && !defaultStartTime.isBefore(defaultEndTime)) {
                            defaultStartTime.plusHours(1).withNano(0)
                        } else {
                            defaultEndTime
                        }

                        state.copy(
                            isAllDay = false,
                            startTime = defaultStartTime,
                            endTime = finalEndTime
                        )
                    }
                    onStateChange(newState)
                },
                label = { Text(allDay) },
                enabled = !isLoading
            )

            // --- Чип "One Day" ---
            FilterChip(
                selected = isOneDay, // Use internal flag for display
                onClick = {
                    val newIsOneDay = !isOneDay
                    isOneDay = newIsOneDay // Update internal flag FIRST for animation trigger

                    // Prepare the new external state
                    val newState = if (newIsOneDay) {
                        // If turning One Day ON, ensure end date matches start date
                        state.copy(endDate = state.startDate)
                    } else {
                        // If turning One Day OFF, the user needs to pick an end date.
                        // We don't automatically set an end date here.
                        // We might need to adjust time logic if isAllDay is false.
                        // For now, just reflect the potential date structure change.
                        // If the user intended a range, they'll pick endDate later.
                        // If they were already in range mode, this doesn't change the dates.
                        state.copy() // Keep dates as they are for now
                    }
                    onStateChange(newState)
                },
                label = { Text(oneDay) },
                enabled = !isLoading
            )

            // --- Чип "Recurring" ---
            FilterChip(
                selected = state.isRecurring,
                onClick = {
                    val newIsRecurring = !state.isRecurring
                    val newRule = if (!newIsRecurring) {
                        // Выключаем повторение - сбрасываем правило
                        null
                    } else {
                        // Включаем повторение - ставим Daily по умолчанию, если правила нет
                        state.recurrenceRule ?: RecurrenceOption.Daily.rruleValue
                    }
                    onStateChange(state.copy(
                        isRecurring = newIsRecurring,
                        recurrenceRule = newRule
                    ))
                },
                label = { Text(recEvent) }, // Текст из ресурса
                enabled = !isLoading
            )
        }

        // Animate error visibility for a smoother appearance/disappearance
        AnimatedVisibility(
            visible = dateTimeError != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column { // Wrap in column to allow spacer even when text invisible
                Text(
                    text = dateTimeError ?: "", // Provide default empty string
                    color = colorScheme.error,
                    style = typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }


        // --- Поля ввода Даты/Времени (АНИМИРОВАННЫЕ) ---
        // Apply animateContentSize to the container whose size changes
        Column(
            modifier = Modifier.animateContentSize(animationSpec = tween(300)),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedContent(
                targetState = Pair(isAllDay, isOneDay),
                transitionSpec = {
                    if (targetState.first != initialState.first || targetState.second != initialState.second) {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(270, delayMillis = 90))) togetherWith
                                (fadeOut(animationSpec = tween(90)) +
                                        slideOutVertically(targetOffsetY = { -it / 4 }, animationSpec = tween(120)))
                    } else {
                        fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                    }.using(
                        SizeTransform(clip = true, sizeAnimationSpec = { _, _ -> tween(250) })
                    )
                },
                label = "DateTimeFieldsAnimation"
            ) { targetLayoutState ->
                val (showAllDay, showOneDay) = targetLayoutState
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // Inner column for spacing
                    when {
                        // Case: All Day + One Day
                        showAllDay && showOneDay -> {
                            DatePickerField(dateSingle, state.startDate, deviceDateFormatter, dateTimeError != null, isLoading, onRequestShowStartDatePicker, Modifier.fillMaxWidth().padding(horizontal = 50.dp))
                        }
                        // Case: Only All Day
                        showAllDay -> {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), Arrangement.spacedBy(8.dp), Alignment.Top) {
                                DatePickerField(dateStart, state.startDate, deviceDateFormatter, dateTimeError != null, isLoading, onRequestShowStartDatePicker, Modifier.weight(1f))
                                DatePickerField(dateEnd, state.endDate, deviceDateFormatter, dateTimeError != null, isLoading, onRequestShowEndDatePicker, Modifier.weight(1f))
                            }
                        }
                        // Case: Only One Day
                        showOneDay -> {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), Arrangement.spacedBy(8.dp), Alignment.Top) {
                                TimePickerField(timeStart, state.startTime, deviceTimeFormatter, dateTimeError != null && state.startTime == null, isLoading, onRequestShowStartTimePicker, Modifier.weight(1f))
                                TimePickerField(timeEnd, state.endTime, deviceTimeFormatter, dateTimeError != null && state.endTime == null, isLoading, onRequestShowEndTimePicker, Modifier.weight(1f))
                            }
                            DatePickerField(dateSingle, state.startDate, deviceDateFormatter, dateTimeError != null, isLoading, onRequestShowStartDatePicker, Modifier.fillMaxWidth().padding(horizontal = 50.dp))
                        }
                        // Case: Neither (Full Range)
                        else -> {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), Arrangement.spacedBy(8.dp), Alignment.Top) {
                                TimePickerField(timeStart, state.startTime, deviceTimeFormatter, dateTimeError != null && state.startTime == null, isLoading, onRequestShowStartTimePicker, Modifier.weight(0.5f))
                                TimePickerField(timeEnd, state.endTime, deviceTimeFormatter, dateTimeError != null && state.endTime == null, isLoading, onRequestShowEndTimePicker, Modifier.weight(0.5f))
                            }
                            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), Arrangement.spacedBy(8.dp), Alignment.Top) {
                                DatePickerField(dateStart, state.startDate, deviceDateFormatter, dateTimeError != null, isLoading, onRequestShowStartDatePicker, Modifier.weight(1f))
                                DatePickerField(dateEnd, state.endDate, deviceDateFormatter, dateTimeError != null, isLoading, onRequestShowEndDatePicker, Modifier.weight(1f))
                            }
                        }
                    }
                }
            } // End AnimatedContent
        } // End Column for Date/Time fields with animateContentSize

        // --- Настройки повторения (АНИМИРОВАННЫЕ) ---
        AnimatedVisibility(
            visible = state.isRecurring,
            enter = fadeIn(animationSpec = tween(150, delayMillis = 50)) + expandVertically(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = tween(300)),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Column {
                // --- Чипы для выбора частоты RRULE ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .horizontalScroll(rememberScrollState()), // Горизонтальная прокрутка
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    // Вычисляем текущий выбор один раз
                    val currentSelection = remember(state.recurrenceRule) {
                        RecurrenceOption.fromRule(state.recurrenceRule)
                    }

                    // --- Явно создаем каждый чип ---
                    // Чип "Ежедневно"
                    FilterChipForOption(
                        option = RecurrenceOption.Daily,
                        currentSelection = currentSelection,
                        isLoading = isLoading,
                        onStateChange = onStateChange,
                        state = state
                    )

                    // Чип "Еженедельно"
                    FilterChipForOption(
                        option = RecurrenceOption.Weekly,
                        currentSelection = currentSelection,
                        isLoading = isLoading,
                        onStateChange = onStateChange,
                        state = state
                    )

                    // Чип "Ежемесячно"
                    FilterChipForOption(
                        option = RecurrenceOption.Monthly,
                        currentSelection = currentSelection,
                        isLoading = isLoading,
                        onStateChange = onStateChange,
                        state = state
                    )

                    // Чип "Ежегодно"
                    FilterChipForOption(
                        option = RecurrenceOption.Yearly,
                        currentSelection = currentSelection,
                        isLoading = isLoading,
                        onStateChange = onStateChange,
                        state = state
                    )
                }
                AnimatedVisibility(
                    visible = state.recurrenceRule == RecurrenceOption.Weekly.rruleValue, // Показываем только для Weekly
                    enter = fadeIn(animationSpec = tween(150, delayMillis = 50)) + expandVertically(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = tween(300)),
                ) {
                    Column {
                        // --- Чипы для дней недели ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            // Центрируем дни недели, если их немного
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                        ) {
                            weekdays.forEach { day ->
                                val isSelected = day in state.selectedWeekdays
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val currentDays = state.selectedWeekdays
                                        val newDays = if (isSelected) {
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
                                    enabled = !isLoading
                                )
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(top = 16.dp)) { // Добавим отступ
                    Text(
                        text = endsLabel, // "Ends"
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // --- Чипы для выбора типа окончания (NEVER, DATE, COUNT) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        // Чип "Never"
                        FilterChip(
                            selected = state.recurrenceEndType == RecurrenceEndType.NEVER,
                            onClick = {
                                // Устанавливаем тип, сбрасываем дату и количество
                                onStateChange(state.copy(
                                    recurrenceEndType = RecurrenceEndType.NEVER,
                                    recurrenceEndDate = null,
                                    recurrenceCount = null
                                ))
                            },
                            label = { Text(endNeverLabel) },
                            enabled = !isLoading
                        )
                        // Чип "On date"
                        FilterChip(
                            selected = state.recurrenceEndType == RecurrenceEndType.DATE,
                            onClick = {
                                // Устанавливаем тип, ставим дату по умолчанию (если нет), сбрасываем количество
                                val defaultEndDate = state.recurrenceEndDate
                                    ?: state.startDate.plusMonths(1) // Пример: через месяц
                                onStateChange(state.copy(
                                    recurrenceEndType = RecurrenceEndType.DATE,
                                    recurrenceEndDate = defaultEndDate,
                                    recurrenceCount = null
                                ))
                                // Запрашиваем показ пикера, если дата была null
                                if (state.recurrenceEndDate == null) {
                                    onRequestShowRecurrenceEndDatePicker()
                                }
                            },
                            label = { Text(endDateLabel) },
                            enabled = !isLoading
                        )
                        // Чип "After..."
                        FilterChip(
                            selected = state.recurrenceEndType == RecurrenceEndType.COUNT,
                            onClick = {
                                val defaultCount = state.recurrenceCount ?: 10 // Пример: 10 раз
                                onStateChange(state.copy(
                                    recurrenceEndType = RecurrenceEndType.COUNT,
                                    recurrenceEndDate = null,
                                    recurrenceCount = defaultCount
                                ))
                            },
                            label = { Text(endCountLabel) },
                            enabled = !isLoading
                        )
                    }

                    // --- Поле для выбора даты окончания (UNTIL) ---
                    AnimatedVisibility(
                        visible = state.recurrenceEndType == RecurrenceEndType.DATE,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        DatePickerField(
                            label = recurrenceEndDateFieldLabel, // "End Date"
                            date = state.recurrenceEndDate,
                            dateFormatter = deviceDateFormatter,
                            isError = false, // TODO: Добавить валидацию даты окончания? (должна быть >= даты начала)
                            isLoading = isLoading,
                            onClick = onRequestShowRecurrenceEndDatePicker, // Открываем пикер
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 50.dp)
                        )
                    }

                    // --- Поле для ввода количества (COUNT) ---
                    AnimatedVisibility(
                        visible = state.recurrenceEndType == RecurrenceEndType.COUNT,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.recurrenceCount?.toString() ?: "",
                            onValueChange = { text ->
                                // Пытаемся преобразовать в Int, игнорируем невалидный ввод
                                val count = text.filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(1) // Минимум 1 повторение
                                // Обновляем только количество
                                onStateChange(state.copy(recurrenceCount = count))
                            },
                            label = { Text(recurrenceCountFieldLabel) }, // "Number of times"
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
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
@OptIn(ExperimentalMaterial3Api::class)
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
            onStateChange(state.copy(
                recurrenceRule = option.rruleValue,
                isRecurring = (option != RecurrenceOption.None)
            ))
        },
        label = {
            Text(stringResource(option.labelResId))
        },
        enabled = !isLoading,
        leadingIcon = if (option == currentSelection) {
            // Можно вернуть иконку, если хотите
            // { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            null
        } else null
    )
}

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
}
