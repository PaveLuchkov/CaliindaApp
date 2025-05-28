package com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging

import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.data.calendar.ClientEventUpdateMode
import com.lpavs.caliinda.data.calendar.UpdateEventResult
import com.lpavs.caliinda.data.local.DateTimeUtils
import com.lpavs.caliinda.data.local.UpdateEventApiRequest
import com.lpavs.caliinda.ui.screens.main.CalendarEvent
import com.lpavs.caliinda.ui.screens.main.MainViewModel
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.sections.EventDateTimePicker
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.sections.EventDateTimeState
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.sections.EventNameSection
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.sections.RecurrenceEndType
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.sections.RecurrenceOption
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.ui.AdaptiveContainer
import com.lpavs.caliinda.ui.screens.main.components.calendarui.eventmanaging.ui.TimePickerDialog
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditEventScreen(
    viewModel: MainViewModel,
    eventToEdit: CalendarEvent, // Оригинальное событие для предзаполнения
    selectedUpdateMode: ClientEventUpdateMode, // Режим обновления, выбранный пользователем
    onDismiss: () -> Unit,
    currentSheetValue: SheetValue // Если используется в BottomSheet
) {
    // --- Получаем оригинальные данные и парсим их для состояния формы ---
    val userTimeZoneId by viewModel.timeZone.collectAsState() // Нужен для парсинга времени

    // Инициализируем состояние формы данными из eventToEdit
    var summary by remember(eventToEdit.id) { mutableStateOf(eventToEdit.summary) }
    var description by remember(eventToEdit.id) { mutableStateOf(eventToEdit.description ?: "") }
    var location by remember(eventToEdit.id) { mutableStateOf(eventToEdit.location ?: "") }

    var summaryError by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) } // Общая ошибка валидации полей даты/времени

    val updateEventState by viewModel.updateEventResult.collectAsState() // Наблюдаем за результатом обновления
    var isLoading by remember { mutableStateOf(false) }
    var generalError by remember { mutableStateOf<String?>(null) } // Ошибка от ViewModel/DataManager

    val context = LocalContext.current
    val systemZoneId = remember { ZoneId.systemDefault() }
    val untilFormatter = remember { DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'") }


    // --- ИНИЦИАЛИЗАЦИЯ EventDateTimeState ---
    val initialEventDateTimeState = remember(eventToEdit.id, userTimeZoneId) {
        // Здесь нужна сложная логика для парсинга eventToEdit.startTime, endTime,
        // isAllDay и правил повторения в EventDateTimeState.
        // Это ключевой момент!
        parseCalendarEventToDateTimeState(eventToEdit, userTimeZoneId, systemZoneId)
    }
    var eventDateTimeState by remember(eventToEdit.id) { mutableStateOf(initialEventDateTimeState) }
    // ----------------------------------------
    LaunchedEffect(initialEventDateTimeState) { // Или просто Log.d без LaunchedEffect, если это только для отладки
        Log.d("EditEventScreen", "Initial EventDateTimeState for UI: $initialEventDateTimeState")
    }

    // Состояния для управления видимостью диалогов M3 (остаются)
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showRecurrenceEndDatePicker by remember { mutableStateOf(false) }

    fun formatEventTimesForSaving(
        state: EventDateTimeState,
        timeZoneId: String? // Теперь может быть null
    ): Pair<String?, String?> {
        return if (state.isAllDay) {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            val startDateStr = try {
                state.startDate.format(formatter)
            } catch (_: Exception) {
                null
            }
            // Для all-day события конец должен быть на следующий день после фактического последнего дня
            val effectiveEndDate = state.endDate.plusDays(1)
            val endDateStr = try {
                effectiveEndDate.format(formatter)
            } catch (_: Exception) {
                null
            }
            Log.d(
                "CreateEvent",
                "Formatting All-Day: Start Date=$startDateStr, End Date=$endDateStr"
            )
            Pair(startDateStr, endDateStr)
        } else {
            if (timeZoneId == null) {
                Log.e("CreateEvent", "Cannot format timed event without TimeZone ID!")
                return Pair(null, null) // Не можем форматировать без таймзоны
            }
            // validateInput уже проверил, что startTime и endTime не null
            val startTimeNaiveIso = DateTimeUtils.formatLocalDateTimeToNaiveIsoString(
                state.startDate,
                state.startTime
            )
            val endTimeNaiveIso = DateTimeUtils.formatLocalDateTimeToNaiveIsoString(
                state.endDate,
                state.endTime
            )
            Log.d(
                "CreateEvent",
                "Formatting Timed: Start DateTime=$startTimeNaiveIso, End DateTime=$endTimeNaiveIso"
            )
            Pair(startTimeNaiveIso, endTimeNaiveIso)
        }
    }

    // Функция validateInput (остается такой же или с небольшими адаптациями)
    fun validateInput(): Boolean {
        summaryError = if (summary.isBlank()) "Название не может быть пустым" else null
        validationError = null
        val state = eventDateTimeState
        if (!state.isAllDay && (state.startTime == null || state.endTime == null)) {
            validationError = "Укажите время начала и конца"
            return false
        }
        val (testStartTimeStr, testEndTimeStr) = formatEventTimesForSaving(state, userTimeZoneId)
        if (testStartTimeStr == null || testEndTimeStr == null) {
            validationError = "Не удалось сформировать дату/время для отправки"
            return false
        }
        return summaryError == null && validationError == null
    }

    // --- Обработка состояния ViewModel для ОБНОВЛЕНИЯ ---
    LaunchedEffect(updateEventState) {
        isLoading = updateEventState is UpdateEventResult.Loading
        when (val result = updateEventState) {
            is UpdateEventResult.Success -> {
                Toast.makeText(context, "Событие успешно обновлено", Toast.LENGTH_SHORT).show()
                viewModel.consumeUpdateEventResult() // Используем новый consume
                onDismiss()
            }
            is UpdateEventResult.Error -> {
                generalError = result.message
                viewModel.consumeUpdateEventResult()
            }
            is UpdateEventResult.Loading -> generalError = null
            is UpdateEventResult.Idle -> { /* Сброс generalError, если нужно */ }
        }
    }

    val onSaveClick: () -> Unit = saveLambda@{
        generalError = null
        if (validateInput()) {
            val (startStr, endStr) = formatEventTimesForSaving(eventDateTimeState, userTimeZoneId)
            if (startStr == null || endStr == null) { /* ... обработка ошибки ... */ return@saveLambda }

            // --- Формирование RRULE (код идентичен CreateEventScreen) ---
            val baseRule = eventDateTimeState.recurrenceRule?.takeIf { it.isNotBlank() }
            var finalRecurrenceRule: String? = null

            if (baseRule != null) {
                val ruleParts = mutableListOf(baseRule) // Начинаем с FREQ=...

                // Добавляем BYDAY, если нужно
                if (baseRule == RecurrenceOption.Weekly.rruleValue && eventDateTimeState.selectedWeekdays.isNotEmpty()) {
                    val bydayString = eventDateTimeState.selectedWeekdays
                        .sorted()
                        .joinToString(",") { day ->
                            when (day) {
                                DayOfWeek.MONDAY -> "MO"
                                DayOfWeek.TUESDAY -> "TU"
                                DayOfWeek.WEDNESDAY -> "WE"
                                DayOfWeek.THURSDAY -> "TH"
                                DayOfWeek.FRIDAY -> "FR"
                                DayOfWeek.SATURDAY -> "SA"
                                DayOfWeek.SUNDAY -> "SU"
                            }
                        }
                    ruleParts.add("BYDAY=$bydayString")
                }

                // Добавляем UNTIL или COUNT
                when (eventDateTimeState.recurrenceEndType) {
                    RecurrenceEndType.DATE -> {
                        eventDateTimeState.recurrenceEndDate?.let { endDate ->
                            // Форматируем дату окончания в UTC (конец дня)
                            // Важно: UNTIL включает указанный момент. Часто берут конец дня.
                            // Для простоты возьмем начало следующего дня и отформатируем.
                            // Или, как в примере Google, T000000Z - начало дня UTC.
                            // Используем конец дня даты окончания в системной таймзоне и конвертируем в UTC
                            val systemZone = ZoneId.systemDefault()
                            val endOfDay = endDate.atTime(LocalTime.MAX).atZone(systemZone)
                            // Google часто использует T000000Z на *следующий* день, что эквивалентно концу дня
                            // val endDateTimeUtc = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC)

                            // Более точный подход: конец дня в системной таймзоне -> UTC
                            val endDateTimeUtc = endDate.atTime(23, 59, 59).atZone(systemZone)
                                .withZoneSameInstant(ZoneOffset.UTC)

                            val untilString = untilFormatter.format(endDateTimeUtc)
                            ruleParts.add("UNTIL=$untilString")
                        }
                    }

                    RecurrenceEndType.COUNT -> {
                        eventDateTimeState.recurrenceCount?.let { count ->
                            ruleParts.add("COUNT=$count")
                        }
                    }

                    RecurrenceEndType.NEVER -> {
                        // Ничего не добавляем
                    }
                }

                // Собираем все части через точку с запятой
                finalRecurrenceRule = ruleParts.joinToString(";")
            }
            // -----------------------------------------------------------

            // --- Формируем объект UpdateEventApiRequest только с измененными полями ---
            val updateRequest = buildUpdateEventApiRequest(
                originalEvent = eventToEdit,
                currentSummary = summary.trim(),
                currentDescription = description.trim(),
                currentLocation = location.trim(),
                currentDateTimeState = eventDateTimeState,
                formattedStartStr = startStr,
                formattedEndStr = endStr,
                finalRRuleStringFromUi = finalRecurrenceRule,
                userTimeZoneIdForTimed = userTimeZoneId,
                selectedUpdateMode = selectedUpdateMode
            )

            if (updateRequest == null) {
                Toast.makeText(context, "Нет изменений для сохранения", Toast.LENGTH_SHORT).show()
                onDismiss()
                return@saveLambda
            }

            viewModel.confirmEventUpdate(
                updatedEventData = updateRequest,
                modeFromUi = selectedUpdateMode
            )
        } else {
            Toast.makeText(context, "Проверьте введенные данные", Toast.LENGTH_SHORT).show()
        }
    } // Конец onSaveClick

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 0.dp), // Меньше отступы для иконки
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = currentSheetValue,
            transitionSpec = {
                (EnterTransition.None)
                    .togetherWith(ExitTransition.None)
                    .using(SizeTransform(
                        clip = false, // false - чтобы контент не обрезался во время анимации размера
                        sizeAnimationSpec = { _, _ ->
                            // Используем spring для более "живой" анимации размера
                            spring(
                                dampingRatio = Spring.DampingRatioLowBouncy, // Попробуйте LowBouncy или MediumBouncy
                                stiffness = Spring.StiffnessMediumLow // Попробуйте Medium или Low
                            )
                        }
                    )
                    )
            },
            label = "SaveButtonAnimation"
        ) { targetSheetValue ->
            val expandedSize = ButtonDefaults.LargeContainerHeight
            val defaultSize = ButtonDefaults.MediumContainerHeight

            val isNotCompactState =
                targetSheetValue == SheetValue.Expanded

            // Define icon sizes for different states

            val size = if (!isNotCompactState) defaultSize else expandedSize

            Button(
                onClick = onSaveClick,
                enabled = !isLoading,
                modifier = Modifier.heightIn(size),
                contentPadding = ButtonDefaults.contentPaddingFor(size)
            ) {
                if (isLoading) {
                    LoadingIndicator(color = colorScheme.onPrimary, modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)))
                } else {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Сохранить", // Consider using stringResource here too
                        modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)) // Animated icon size
                    )
//                        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size))) // Or use animated spacerWidth
//                        Text(stringResource(R.string.save), style = ButtonDefaults.textStyleFor(size)) // Ensure R.string.save exists
                }
            }
        }
    }
    // Пример UI (основные части)
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp) // Горизонтальные отступы для контента
            .fillMaxWidth(),
    ) {
        // BackgroundShapes здесь может быть не нужен или потребует адаптации
        // BackgroundShapes(BackgroundShapeContext.EventCreation)
        AdaptiveContainer {
            EventNameSection(
                summary = summary,
                summaryError = summaryError,
                onSummaryChange = { summary = it },
                onSummaryErrorChange = { summaryError = it },
                isLoading = isLoading
            )
        }
        AdaptiveContainer {
            EventDateTimePicker(
                state = eventDateTimeState,
                onStateChange = { newState ->
                    eventDateTimeState = newState
                    validationError = null
                },
                isLoading = isLoading,
                onRequestShowStartDatePicker = { showStartDatePicker = true },
                onRequestShowStartTimePicker = { showStartTimePicker = true },
                onRequestShowEndDatePicker = { showEndDatePicker = true },
                onRequestShowEndTimePicker = { showEndTimePicker = true },
                onRequestShowRecurrenceEndDatePicker = { showRecurrenceEndDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            )
        }
        validationError?.let {
            Text(it, color = colorScheme.error, style = typography.bodySmall)
        }


        AdaptiveContainer {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4,
                enabled = !isLoading,
                shape = RoundedCornerShape(25.dp)
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Местоположение") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(25.dp)
            )
        }
        generalError?.let {
            Text(it, color = colorScheme.error, style = typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(16.dp)) // Отступ перед кнопкой сохранения
    } // End Scrollable Column

    // Кнопка сохранения внизу листа

    val currentDateTimeState = eventDateTimeState // Захватываем текущее состояние для лямбд

    // Диалог выбора Даты Начала
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDateTimeState.startDate.atStartOfDay(
                systemZoneId
            ).toInstant().toEpochMilli()
            // Добавьте selectableDates, если нужно ограничить выбор
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate =
                                Instant.ofEpochMilli(millis).atZone(systemZoneId)
                                    .toLocalDate()
                            // Обновляем состояние через copy
                            eventDateTimeState = currentDateTimeState.copy(
                                startDate = selectedDate,
                                // Если дата конца стала раньше начала, подтягиваем ее
                                // Или если режим "Один день", дата конца = дате начала
                                endDate = if (selectedDate.isAfter(currentDateTimeState.endDate) || currentDateTimeState.startTime == null)
                                    selectedDate
                                else currentDateTimeState.endDate
                            )
                        }
                        showStartDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    // Диалог выбора Времени Начала
    if (showStartTimePicker) { // Компонент не запросит, если не нужно
        val initialTime = currentDateTimeState.startTime
            ?: LocalTime.now() // Безопасное значение по умолчанию
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = DateFormat.is24HourFormat(context) // Учитываем настройку системы
        )
        TimePickerDialog( // Используем кастомную обертку
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedTime =
                        LocalTime.of(timePickerState.hour, timePickerState.minute)
                            .withNano(0)
                    // Обновляем состояние через copy
                    var newEndTime = currentDateTimeState.endTime
                    // Если дата конца совпадает с датой начала и новое время начала >= времени конца,
                    // сдвигаем время конца на час вперед
                    if (currentDateTimeState.startDate == currentDateTimeState.endDate &&
                        currentDateTimeState.endTime != null &&
                        !selectedTime.isBefore(currentDateTimeState.endTime)
                    ) {
                        newEndTime = selectedTime.plusHours(1).withNano(0)
                    }
                    eventDateTimeState = currentDateTimeState.copy(
                        startTime = selectedTime,
                        endTime = newEndTime
                    )
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Отмена") }
            }
        ) {
            TimePicker(state = timePickerState) // Вставляем сам пикер
        }
    }

    // Диалог выбора Даты Конца
    if (showEndDatePicker) { // Компонент не запросит, если не нужно
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDateTimeState.endDate.atStartOfDay(
                systemZoneId
            ).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates { // Ограничение выбора
                val startMillis =
                    currentDateTimeState.startDate.atStartOfDay(systemZoneId).toInstant()
                        .toEpochMilli()

                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= startMillis // Нельзя выбрать дату раньше даты начала
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year >= currentDateTimeState.startDate.year // Оптимизация для годов
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate =
                                Instant.ofEpochMilli(millis).atZone(systemZoneId)
                                    .toLocalDate()
                            // Обновляем состояние через copy
                            eventDateTimeState =
                                currentDateTimeState.copy(endDate = selectedDate)
                            // Дополнительно можно проверить и скорректировать endTime, если нужно
                        }
                        showEndDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Диалог выбора Времени Конца
    if (showEndTimePicker) { // Компонент не запросит, если не нужно
        val initialTime =
            currentDateTimeState.endTime ?: currentDateTimeState.startTime?.plusHours(1)
            ?: LocalTime.now()
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = DateFormat.is24HourFormat(context)
        )
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedTime =
                        LocalTime.of(timePickerState.hour, timePickerState.minute)
                            .withNano(0)
                    // Проверка: если даты одинаковые, время конца не может быть раньше или равно времени начала
                    if (currentDateTimeState.startDate == currentDateTimeState.endDate &&
                        currentDateTimeState.startTime != null &&
                        !currentDateTimeState.startTime.isBefore(selectedTime) // Если startTime НЕ раньше selectedTime (т.е. >=)
                    ) {
                        Toast.makeText(
                            context,
                            "Время конца должно быть после времени начала",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Обновляем состояние через copy
                        eventDateTimeState =
                            currentDateTimeState.copy(endTime = selectedTime)
                        showEndTimePicker = false
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("Отмена") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
    if (showRecurrenceEndDatePicker) {
        // Рассчитываем начальную дату для пикера
        val initialSelectedDateMillis = eventDateTimeState.recurrenceEndDate
            ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            ?: eventDateTimeState.startDate.plusMonths(1) // По умолчанию через месяц от старта
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Состояние DatePicker'а
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedDateMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val selectedLocalDate = Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    // Не раньше даты начала события
                    return !selectedLocalDate.isBefore(eventDateTimeState.startDate)
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year >= eventDateTimeState.startDate.year
                }
            }
        )
        // Создаем DatePickerDialog с правильной сигнатурой
        DatePickerDialog(
            onDismissRequest = { showRecurrenceEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault()).toLocalDate()
                            eventDateTimeState = eventDateTimeState.copy(
                                recurrenceEndDate = selectedDate,
                                recurrenceEndType = RecurrenceEndType.DATE,
                                recurrenceCount = null
                            )
                        }
                        showRecurrenceEndDatePicker = false
                    },
                    // Можно добавить enabled = datePickerState.selectedDateMillis != null
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRecurrenceEndDatePicker = false
                }) { Text("Cancel") }
            }
            // --- ИСПОЛЬЗУЕМ ИМЕНОВАННЫЙ ПАРАМЕТР content ---
        ) { // Начало лямбды для content
            DatePicker(state = datePickerState) // Передаем DatePicker как контент
        } // Конец лямбды для content
    }
} // End Scaffold

fun parseCalendarEventToDateTimeState(
    event: CalendarEvent,
    userTimeZoneId: String, // Текущая таймзона пользователя для корректного отображения
    systemZoneId: ZoneId = ZoneId.systemDefault() // Для преобразований, если нужно
): EventDateTimeState {
    val isAllDay = event.isAllDay

    var parsedStartDate: LocalDate = LocalDate.now()
    var parsedStartTime: LocalTime? = null
    var parsedEndDate: LocalDate = LocalDate.now()
    var parsedEndTime: LocalTime? = null

    try {
        if (isAllDay) {
            // Для all-day startTime и endTime - это строки YYYY-MM-DD
            parsedStartDate = LocalDate.parse(event.startTime, DateTimeFormatter.ISO_LOCAL_DATE)
            // У endTime для all-day в Google Calendar обычно конец ЭКСКЛЮЗИВНЫЙ (т.е. начало следующего дня)
            // Нам для UI нужна фактическая дата последнего дня события
            parsedEndDate = LocalDate.parse(event.endTime, DateTimeFormatter.ISO_LOCAL_DATE).minusDays(1)
        } else {
            // Для timed событий, startTime и endTime - это ISO DateTime строки
            // Их нужно парсить с учетом их исходной таймзоны (если есть в строке)
            // или таймзоны события (которую Google возвращает, но у тебя ее нет в CalendarEvent)
            // или, если ничего нет, предполагать userTimeZoneId (менее точно)

            // DateTimeUtils.parseToInstant должен уметь работать с ISO строками от Google
            val startInstant = DateTimeUtils.parseToInstant(event.startTime, userTimeZoneId) // Используем userTimeZoneId как fallback
            val endInstant = DateTimeUtils.parseToInstant(event.endTime, userTimeZoneId)

            if (startInstant != null) {
                val startZonedDateTime = startInstant.atZone(ZoneId.of(userTimeZoneId)) // Конвертируем в зону юзера для UI
                parsedStartDate = startZonedDateTime.toLocalDate()
                parsedStartTime = startZonedDateTime.toLocalTime().withNano(0)
            }
            if (endInstant != null) {
                val endZonedDateTime = endInstant.atZone(ZoneId.of(userTimeZoneId))
                parsedEndDate = endZonedDateTime.toLocalDate()
                parsedEndTime = endZonedDateTime.toLocalTime().withNano(0)
            }
        }
    } catch (e: Exception) {
        Log.e("ParseToState", "Error parsing event date/time for editing: ${e.message}")
        // Устанавливаем какие-то дефолты, если парсинг не удался
        val now = ZonedDateTime.now(ZoneId.of(userTimeZoneId))
        parsedStartDate = now.toLocalDate()
        parsedStartTime = if (!isAllDay) now.toLocalTime().plusHours(1).withMinute(0) else null
        parsedEndDate = parsedStartDate
        parsedEndTime = if (!isAllDay) parsedStartTime?.plusHours(1) else null
    }

    // --- Парсинг RRULE ---
    var recurrenceOption: RecurrenceOption? = null
    var selectedWeekdays: Set<DayOfWeek> = emptySet()
    var recurrenceEndType = RecurrenceEndType.NEVER
    var recurrenceEndDate: LocalDate? = null
    var recurrenceCount: Int? = null
    var isRecurring = false

    event.recurrenceRule?.let { rruleString ->
        isRecurring = true
        val rules = rruleString.split(';')
        rules.forEach { rulePart ->
            val parts = rulePart.split('=')
            if (parts.size == 2) {
                val key = parts[0]
                val value = parts[1]
                when (key) {
                    "FREQ" -> {
                        recurrenceOption = RecurrenceOption.ALL_OPTIONS.find { it.rruleValue == "FREQ=$value" }
                    }
                    "BYDAY" -> {
                        selectedWeekdays = value.split(',').mapNotNull { dayStr ->
                            when (dayStr) {
                                "MO" -> DayOfWeek.MONDAY
                                "TU" -> DayOfWeek.TUESDAY
                                "WE" -> DayOfWeek.WEDNESDAY
                                "TH" -> DayOfWeek.THURSDAY
                                "FR" -> DayOfWeek.FRIDAY
                                "SA" -> DayOfWeek.SATURDAY
                                "SU" -> DayOfWeek.SUNDAY
                                else -> null
                            }
                        }.toSet()
                    }
                    "UNTIL" -> {
                        try {
                            // UNTIL от Google в формате YYYYMMDDTHHMMSSZ (UTC)
                            val zonedDateTime = ZonedDateTime.parse(value, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC))
                            recurrenceEndDate = zonedDateTime.withZoneSameInstant(systemZoneId).toLocalDate() // Конвертируем в системную зону
                            recurrenceEndType = RecurrenceEndType.DATE
                        } catch (e: Exception) {
                            Log.e("ParseToState", "Error parsing UNTIL value: $value - ${e.message}")
                        }
                    }
                    "COUNT" -> {
                        recurrenceCount = value.toIntOrNull()
                        if (recurrenceCount != null) recurrenceEndType = RecurrenceEndType.COUNT
                    }
                }
            }
        }
        // Если FREQ не найден, но есть другие части, ставим какой-то дефолт или оставляем null
        if (recurrenceOption == null) {
            // Это может быть кастомное правило, которое твои RecurrenceOption не покрывают.
            // Либо ошибка парсинга. Для простоты, можно не устанавливать recurrenceOption.
            Log.w("ParseToState", "Could not map FREQ from RRULE: $rruleString to known RecurrenceOption")
        }
    }

    Log.d("ParseToState", "--- Parsing Event to EventDateTimeState ---")
    Log.d("ParseToState", "Original Event ID: ${event.id}")
    Log.d("ParseToState", "Original Event Summary: ${event.summary}")
    Log.d("ParseToState", "Original RRULE String: ${event.recurrenceRule}")
    Log.d("ParseToState", "Parsed isAllDay: $isAllDay")
    Log.d("ParseToState", "Parsed StartDate: $parsedStartDate, StartTime: $parsedStartTime")
    Log.d("ParseToState", "Parsed EndDate: $parsedEndDate, EndTime: $parsedEndTime")
    Log.d("ParseToState", "Parsed isRecurring: $isRecurring")
    Log.d("ParseToState", "Parsed recurrenceRule (FREQ): ${recurrenceOption?.rruleValue}")
    Log.d("ParseToState", "Parsed selectedWeekdays: $selectedWeekdays")
    Log.d("ParseToState", "Parsed recurrenceEndType: $recurrenceEndType")
    Log.d("ParseToState", "Parsed recurrenceEndDate: $recurrenceEndDate")
    Log.d("ParseToState", "Parsed recurrenceCount: $recurrenceCount")
    Log.d("ParseToState", "-----------------------------------------")

    return EventDateTimeState(
        startDate = parsedStartDate,
        startTime = parsedStartTime,
        endDate = parsedEndDate,
        endTime = parsedEndTime,
        isAllDay = isAllDay,
        isRecurring = isRecurring, // Устанавливаем флаг
        recurrenceRule = recurrenceOption?.rruleValue, // Базовое правило FREQ=...
        selectedWeekdays = selectedWeekdays,
        recurrenceEndType = recurrenceEndType,
        recurrenceEndDate = recurrenceEndDate,
        recurrenceCount = recurrenceCount
    )
}

fun buildUpdateEventApiRequest(
    originalEvent: CalendarEvent,
    currentSummary: String,
    currentDescription: String,
    currentLocation: String,
    currentDateTimeState: EventDateTimeState,
    formattedStartStr: String,
    formattedEndStr: String,
    finalRRuleStringFromUi: String?,
    userTimeZoneIdForTimed: String,
    selectedUpdateMode: ClientEventUpdateMode
): UpdateEventApiRequest? {
    var hasChanges = false

    val summaryUpdate = currentSummary.takeIf { it != originalEvent.summary }?.also { hasChanges = true }
    val descriptionUpdate = currentDescription.takeIf { it != (originalEvent.description ?: "") }?.also { hasChanges = true }
    val locationUpdate = currentLocation.takeIf { it != (originalEvent.location ?: "") }?.also { hasChanges = true }

    var startTimeUpdate: String? = null
    var endTimeUpdate: String? = null
    var isAllDayUpdate: Boolean? = null
    var timeZoneIdUpdate: String? = null // Инициализируем как null

    if (currentDateTimeState.isAllDay != originalEvent.isAllDay) {
        isAllDayUpdate = currentDateTimeState.isAllDay
        hasChanges = true
    }

    if (formattedStartStr != originalEvent.startTime) {
        startTimeUpdate = formattedStartStr
        hasChanges = true
    }
    if (formattedEndStr != originalEvent.endTime) {
        endTimeUpdate = formattedEndStr
        hasChanges = true
    }

    // Логика для timeZoneId: отправляем, если итоговое событие timed и есть изменения во времени/типе, или если isAllDay изменилось на false
    if (!currentDateTimeState.isAllDay) { // Если ИТОГОВОЕ состояние - timed
        if (userTimeZoneIdForTimed.isNotBlank()) {
            // Отправляем таймзону, если:
            // 1. Тип isAllDay изменился на timed (isAllDayUpdate стал false)
            // 2. Или если само время (start/end) изменилось для timed события (и isAllDayUpdate null, т.е. не менялся с timed)
            if (isAllDayUpdate == false || (isAllDayUpdate == null && (startTimeUpdate != null || endTimeUpdate != null))) {
                timeZoneIdUpdate = userTimeZoneIdForTimed
                // hasChanges уже будет true, если startTimeUpdate/endTimeUpdate/isAllDayUpdate не null
                // Если timeZoneId - единственное изменение (маловероятно без изменения времени),
                // то hasChanges нужно было бы выставить. Но обычно это идет вместе с другими изменениями.
                // Для простоты, если только таймзона меняется, а даты/флаги нет, это изменение может быть не учтено как hasChanges,
                // но timeZoneIdUpdate будет установлен.
            }
        }
    }
    var recurrenceForApiRequest: List<String>? = null // Это значение пойдет в UpdateEventApiRequest
    val originalRRuleString = originalEvent.recurrenceRule?.takeIf { it.isNotBlank() }
    val currentRRuleString = finalRRuleStringFromUi?.takeIf { it.isNotBlank() }

    if (currentRRuleString != originalRRuleString) {
        hasChanges = true // Отмечаем, что есть изменения

        if (currentRRuleString != null) {
            recurrenceForApiRequest = listOf("RRULE:$currentRRuleString")
        } else {
            recurrenceForApiRequest = emptyList()
        }
    }
    if (selectedUpdateMode == ClientEventUpdateMode.SINGLE_INSTANCE && recurrenceForApiRequest != null) {
        Log.w("BuildUpdateRequest", "Recurrence data was calculated but will be ignored for SINGLE_INSTANCE update mode.")
        recurrenceForApiRequest = null
    }

    val noPrimaryFieldChanges = summaryUpdate == null &&
            descriptionUpdate == null &&
            locationUpdate == null &&
            startTimeUpdate == null &&
            endTimeUpdate == null &&
            isAllDayUpdate == null &&
            timeZoneIdUpdate == null // Учитываем и timeZoneIdUpdate

    if (noPrimaryFieldChanges && recurrenceForApiRequest == null) {
        Log.d("BuildUpdateRequest", "No actual changes to save after considering all fields and update mode.")
        return null
    }

    return UpdateEventApiRequest(
        summary = summaryUpdate,
        description = descriptionUpdate,
        location = locationUpdate,
        startTime = startTimeUpdate,
        endTime = endTimeUpdate,
        isAllDay = isAllDayUpdate,
        timeZoneId = timeZoneIdUpdate,
        recurrence = recurrenceForApiRequest
    )
}
