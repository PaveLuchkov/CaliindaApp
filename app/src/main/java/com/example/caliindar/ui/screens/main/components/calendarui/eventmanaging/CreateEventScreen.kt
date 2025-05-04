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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import com.example.caliindar.data.calendar.CreateEventResult
import com.example.caliindar.data.local.DateTimeUtils
import com.example.caliindar.ui.screens.main.MainViewModel
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.sections.EventDateTimePicker
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.sections.EventDateTimeState
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.sections.EventNameSection
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.ui.AdaptiveContainer
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.ui.TimePickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

    // Форматер
    val systemZoneId = remember { ZoneId.systemDefault() }

    var eventDateTimeState by remember {
        mutableStateOf(
            EventDateTimeState(
                startDate = initialDate,
                startTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0),
                endDate = initialDate,
                endTime = LocalTime.now().plusHours(2).withMinute(0).withSecond(0).withNano(0),
                isAllDay = false,
                isRecurring = false, // Добавлено, если нужно управлять этим
                recurrenceRule = null // Добавлено
            )
        )
    }
    fun formatEventTimesForSaving(
        state: EventDateTimeState,
        timeZoneId: String
    ): Pair<String?, String?> {
        val startTimeIso: String?
        val endTimeIso: String?

        if (state.isAllDay) {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            startTimeIso = try { state.startDate.format(formatter) } catch (_: Exception) { null }
            val effectiveEndDate = state.endDate.plusDays(1)
            // Вариант 2: Конец = Фактический конец (если событие на неск. дней)
            // val effectiveEndDate = state.endDate // Требует проверки API
            endTimeIso = try { effectiveEndDate.format(formatter) } catch (_: Exception) { null }
            Log.d("CreateEvent", "Formatting All-Day: Start=${startTimeIso}, End=${endTimeIso} (Derived from ${state.endDate})")
        } else {
            // validateInput уже проверил, что startTime и endTime не null
            startTimeIso = DateTimeUtils.formatDateTimeToIsoWithOffset(state.startDate, state.startTime!!, false, timeZoneId)
            endTimeIso = DateTimeUtils.formatDateTimeToIsoWithOffset(state.endDate, state.endTime!!, false, timeZoneId)
            Log.d("CreateEvent", "Formatting Timed: Start=$startTimeIso, End=$endTimeIso")
        }
        return Pair(startTimeIso, endTimeIso)
    }

    // --- Логика валидации (без изменений) ---
    fun validateInput(): Boolean {
        summaryError = if (summary.isBlank()) "Название не может быть пустым" else null
        validationError = null // Сбрасываем ошибку валидации перед отправкой

        // Используем eventDateTimeState для проверок
        val state = eventDateTimeState

        // Проверка DateTimeComponent уже делает базовые проверки (конец >= начало)
        // Но можно добавить дополнительные проверки здесь, если нужно

        // Проверка на null времени, если не AllDay (уже проверяется в компоненте?)
        // Проверим еще раз на всякий случай перед отправкой
        if (!state.isAllDay && (state.startTime == null || state.endTime == null)) {
            validationError = "Укажите время начала и конца"
            return false
        }

        // Проверка возможности форматирования (остается полезной)
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
        generalError = null // Сброс ошибки от VM
        if (validateInput()) {
            val (startTimeIso, endTimeIso) = formatEventTimesForSaving(eventDateTimeState, userTimeZoneId)

            if (startTimeIso == null || endTimeIso == null) {
                validationError = "Ошибка форматирования даты/времени." // Ошибка форматирования
                Log.e("CreateEvent", "Failed to format ISO strings based on state: $eventDateTimeState")
                return@saveLambda
            }

            viewModel.createEvent(
                summary = summary.trim(),
                startTimeString = startTimeIso,
                endTimeString = endTimeIso,
                isAllDay = eventDateTimeState.isAllDay,
                description = description.trim().takeIf { it.isNotEmpty() },
                location = location.trim().takeIf { it.isNotEmpty() },
                recurrenceRule = eventDateTimeState.recurrenceRule // Передаем правило повторения
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
                    initialState = eventDateTimeState,
                    onStateChange = { newState ->
                        eventDateTimeState = newState
                        validationError = null // Сброс ошибки валидации при любом изменении
                    },
                    isLoading = isLoading,
                    onRequestShowStartDatePicker = { showStartDatePicker = true },
                    onRequestShowStartTimePicker = { showStartTimePicker = true },
                    onRequestShowEndDatePicker = { showEndDatePicker = true },
                    onRequestShowEndTimePicker = { showEndTimePicker = true },
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

    } // End Scaffold
}


