package com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging


import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import com.example.caliindar.data.calendar.CreateEventResult
import com.example.caliindar.data.local.DateTimeUtils
import com.example.caliindar.ui.screens.main.MainViewModel
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.sections.EventDateTimePicker
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.sections.EventDateTimeState
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.sections.EventNameSection
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.sections.RecurrenceEndType
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.sections.RecurrenceOption
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.ui.AdaptiveContainer
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.ui.TimePickerDialog
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class) // Необходимо для M3 Dialogs и Pickers
@Composable
fun CreateEventScreen(
    viewModel: MainViewModel,
    initialDate: LocalDate,
    onNavigateBack: () -> Unit
) {
    var summary by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var summaryError by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val createEventState by viewModel.createEventResult.collectAsState()
    val userTimeZoneId by viewModel.timeZone.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var generalError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Состояния для управления видимостью диалогов M3
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showRecurrenceEndDatePicker by remember { mutableStateOf(false) }

    // Форматер
    val systemZoneId = remember { ZoneId.systemDefault() }
    val untilFormatter = remember { DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'") }

    var eventDateTimeState by remember {
        mutableStateOf(
            EventDateTimeState(
                startDate = initialDate,
                startTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0),
                endDate = initialDate,
                endTime = LocalTime.now().plusHours(2).withMinute(0).withSecond(0).withNano(0),
                isAllDay = false,
                selectedWeekdays = emptySet(),
                recurrenceEndType = RecurrenceEndType.NEVER,
                isRecurring = false, // Добавлено, если нужно управлять этим
                recurrenceRule = null // Добавлено
            )
        )
    }
    fun formatEventTimesForSaving(
        state: EventDateTimeState,
        timeZoneId: String? // Теперь может быть null
    ): Pair<String?, String?> {
        return if (state.isAllDay) {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            val startDateStr = try { state.startDate.format(formatter) } catch (_: Exception) { null }
            // Для all-day события конец должен быть на следующий день после фактического последнего дня
            val effectiveEndDate = state.endDate.plusDays(1)
            val endDateStr = try { effectiveEndDate.format(formatter) } catch (_: Exception) { null }
            Log.d("CreateEvent", "Formatting All-Day: Start Date=$startDateStr, End Date=$endDateStr")
            Pair(startDateStr, endDateStr)
        } else {
            // Для событий со временем используем ISO с оффсетом
            if (timeZoneId == null) {
                Log.e("CreateEvent", "Cannot format timed event without TimeZone ID!")
                return Pair(null, null) // Не можем форматировать без таймзоны
            }
            // validateInput уже проверил, что startTime и endTime не null
            val startTimeIso = DateTimeUtils.formatDateTimeToIsoWithOffset(state.startDate, state.startTime!!, false, timeZoneId)
            val endTimeIso = DateTimeUtils.formatDateTimeToIsoWithOffset(state.endDate, state.endTime!!, false, timeZoneId)
            Log.d("CreateEvent", "Formatting Timed: Start DateTime=$startTimeIso, End DateTime=$endTimeIso")
            Pair(startTimeIso, endTimeIso)
        }
    }

    // --- Логика валидации ---
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

    // --- Обработка состояния ViewModel (без изменений) ---
    LaunchedEffect(createEventState) {
        isLoading = createEventState is CreateEventResult.Loading
        when (val result = createEventState) {
            is CreateEventResult.Success -> {
                Toast.makeText(context, "Событие успешно создано", Toast.LENGTH_SHORT).show()
                viewModel.consumeCreateEventResult()
                onNavigateBack()
            }
            is CreateEventResult.Error -> {
                generalError = result.message
                viewModel.consumeCreateEventResult()
            }
            is CreateEventResult.Loading -> generalError = null
            is CreateEventResult.Idle -> { /* Можно убрать generalError, если нужно */ }
        }
    }

    // Функция сохранения (логика форматирования без изменений)
    val onSaveClick: () -> Unit = saveLambda@{
        generalError = null
        if (validateInput()) {
            // Передаем userTimeZoneId в функцию форматирования
            val (startStr, endStr) = formatEventTimesForSaving(eventDateTimeState, userTimeZoneId)

            if (startStr == null || endStr == null) {
                validationError = "Ошибка форматирования даты/времени."
                Log.e("CreateEvent", "Failed to format strings based on state: $eventDateTimeState and TimeZone: $userTimeZoneId")
                return@saveLambda
            }
            var baseRule = eventDateTimeState.recurrenceRule?.takeIf { it.isNotBlank() }
            var finalRecurrenceRule: String? = null

            if (baseRule != null) {
                var ruleParts = mutableListOf(baseRule) // Начинаем с FREQ=...

                // Добавляем BYDAY, если нужно
                if (baseRule == RecurrenceOption.Weekly.rruleValue && eventDateTimeState.selectedWeekdays.isNotEmpty()) {
                    val bydayString = eventDateTimeState.selectedWeekdays
                        .sorted()
                        .joinToString(",") { day ->
                            when(day) {
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
                            val endDateTimeUtc = endDate.atTime(23, 59, 59).atZone(systemZone).withZoneSameInstant(ZoneOffset.UTC)

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
            Log.d("CreateEvent", "Final RRULE to send: $finalRecurrenceRule")


            viewModel.createEvent(
                summary = summary.trim(),
                startTimeString  = startStr, // Отправляем отформатированную строку
                endTimeString  = endStr,     // Отправляем отформатированную строку
                isAllDay = eventDateTimeState.isAllDay,
                timeZoneId = if (eventDateTimeState.isAllDay) null else userTimeZoneId, // Передаем ID таймзоны только для timed событий
                description = description.trim().takeIf { it.isNotEmpty() },
                location = location.trim().takeIf { it.isNotEmpty() },
                recurrenceRule = finalRecurrenceRule
            )
        } else {
            Toast.makeText(context, "Проверьте введенные данные", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Event") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSaveClick,
                elevation = FloatingActionButtonDefaults.elevation(
                //    defaultElevation = 0.dp
                )
            ) {
                Icon(Icons.Filled.Check, "Localized description")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdaptiveContainer{
                EventNameSection(
                    summary = summary,
                    summaryError = summaryError,
                    onSummaryChange = { summary = it },
                    onSummaryErrorChange = { summaryError = it },
                    isLoading = isLoading
                )
            }
            // --- НОВОЕ: Используем EventDateTimePicker ---
            AdaptiveContainer{
                EventDateTimePicker(
                    state = eventDateTimeState,
                    onStateChange = { newState ->
                        eventDateTimeState = newState
                        validationError = null // Сброс ошибки валидации при любом изменении
                    },
                    isLoading = isLoading,
                    onRequestShowStartDatePicker = { showStartDatePicker = true },
                    onRequestShowStartTimePicker = { showStartTimePicker = true },
                    onRequestShowEndDatePicker = { showEndDatePicker = true },
                    onRequestShowEndTimePicker = { showEndTimePicker = true },
                    onRequestShowRecurrenceEndDatePicker = { showRecurrenceEndDatePicker = true },
                    modifier = Modifier.fillMaxWidth() // Занимает всю ширину
                )
            }
            validationError?.let {
                Text(
                    it,
                    color = colorScheme.error,
                    style = typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp) // Немного отступа сверху
                )
            }
            // Описание & Местоположение
            AdaptiveContainer{
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
            // Сообщение об ошибке от бэкенда
            generalError?.let {
                Text(it, color = colorScheme.error, style = typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
        } // End Column
        val currentDateTimeState = eventDateTimeState // Захватываем текущее состояние для лямбд

        // Диалог выбора Даты Начала
        if (showStartDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = currentDateTimeState.startDate.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
                // Добавьте selectableDates, если нужно ограничить выбор
            )
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val selectedDate = Instant.ofEpochMilli(millis).atZone(systemZoneId).toLocalDate()
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
            val initialTime = currentDateTimeState.startTime ?: LocalTime.now() // Безопасное значение по умолчанию
            val timePickerState = rememberTimePickerState(
                initialHour = initialTime.hour,
                initialMinute = initialTime.minute,
                is24Hour = DateFormat.is24HourFormat(context) // Учитываем настройку системы
            )
            TimePickerDialog( // Используем кастомную обертку
                onDismissRequest = { showStartTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute).withNano(0)
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
                initialSelectedDateMillis = currentDateTimeState.endDate.atStartOfDay(systemZoneId).toInstant().toEpochMilli(),
                selectableDates = object : SelectableDates { // Ограничение выбора
                    val startMillis = currentDateTimeState.startDate.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
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
                                val selectedDate = Instant.ofEpochMilli(millis).atZone(systemZoneId).toLocalDate()
                                // Обновляем состояние через copy
                                eventDateTimeState = currentDateTimeState.copy(endDate = selectedDate)
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
            val initialTime = currentDateTimeState.endTime ?: currentDateTimeState.startTime?.plusHours(1) ?: LocalTime.now()
            val timePickerState = rememberTimePickerState(
                initialHour = initialTime.hour,
                initialMinute = initialTime.minute,
                is24Hour = DateFormat.is24HourFormat(context)
            )
            TimePickerDialog(
                onDismissRequest = { showEndTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute).withNano(0)
                        // Проверка: если даты одинаковые, время конца не может быть раньше или равно времени начала
                        if (currentDateTimeState.startDate == currentDateTimeState.endDate &&
                            currentDateTimeState.startTime != null &&
                            !currentDateTimeState.startTime.isBefore(selectedTime) // Если startTime НЕ раньше selectedTime (т.е. >=)
                        )
                        {
                            Toast.makeText(context, "Время конца должно быть после времени начала", Toast.LENGTH_SHORT).show()
                        } else {
                            // Обновляем состояние через copy
                            eventDateTimeState = currentDateTimeState.copy(endTime = selectedTime)
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
                    TextButton(onClick = { showRecurrenceEndDatePicker = false }) { Text("Cancel") }
                }
                // --- ИСПОЛЬЗУЕМ ИМЕНОВАННЫЙ ПАРАМЕТР content ---
            ) { // Начало лямбды для content
                DatePicker(state = datePickerState) // Передаем DatePicker как контент
            } // Конец лямбды для content
        }
    } // End Scaffold
}


