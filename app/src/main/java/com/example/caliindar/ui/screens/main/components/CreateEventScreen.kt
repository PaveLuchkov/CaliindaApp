package com.example.caliindar.ui.screens.main.components


import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule // Иконка для времени
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import com.example.caliindar.data.calendar.CreateEventResult
import com.example.caliindar.data.local.DateTimeUtils
import com.example.caliindar.ui.screens.main.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar // Используем Calendar для Date/Time Picker Dialog
import java.util.Locale

data class CreateEventState(
    val summary: String = "",
    val startDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endDate: LocalDate? = null,
    val endTime: LocalTime? = null,
    val isAllDay: Boolean = false,
    val description: String = "",
    val location: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateBack: Boolean = false // Флаг для навигации назад
)

@OptIn(ExperimentalMaterial3Api::class) // Необходимо для M3 Dialogs и Pickers
@Composable
fun CreateEventScreen(
    viewModel: MainViewModel,
    initialDate: LocalDate,
    onNavigateBack: () -> Unit
) {
    var summary by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate>(initialDate) }
    var startTime by remember {
        mutableStateOf<LocalTime?>(
            LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0) // Убираем наносекунды
        )
    }
    var endDate by remember { mutableStateOf<LocalDate>(initialDate) }
    var endTime by remember { mutableStateOf<LocalTime?>(startTime?.plusHours(1)?.withNano(0)) } // Убираем наносекунды
    var isAllDay by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var summaryError by remember { mutableStateOf<String?>(null) }
    var dateTimeError by remember { mutableStateOf<String?>(null) }

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

    // Форматеры
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val systemZoneId = remember { ZoneId.systemDefault() } // Запоминаем системную зону

    // --- Логика валидации (без изменений) ---
    fun validateInput(): Boolean {
        summaryError = if (summary.isBlank()) "Название не может быть пустым" else null
        dateTimeError = null // Сбрасываем

        if (endDate.isBefore(startDate)) {
            dateTimeError = "Дата конца не может быть раньше даты начала"
            return false
        }

        if (!isAllDay) {
            if (startTime == null || endTime == null) {
                dateTimeError = "Укажите время начала и конца"
                return false
            }
            val startDateTime = LocalDateTime.of(startDate, startTime)
            val endDateTime = LocalDateTime.of(endDate, endTime)
            if (endDateTime.isBefore(startDateTime)) {
                dateTimeError = "Время/дата конца не может быть раньше времени/даты начала"
                return false
            }
        }
        // Проверка форматирования (остается полезной)
        val testStartTimeStr = DateTimeUtils.formatDateTimeToIsoWithOffset(startDate, startTime, isAllDay, userTimeZoneId)
        val effectiveEndDateForTest = if (isAllDay) endDate.plusDays(1) else endDate
        val testEndTimeStr = DateTimeUtils.formatDateTimeToIsoWithOffset(effectiveEndDateForTest, endTime, isAllDay, userTimeZoneId)

        if (testStartTimeStr == null || testEndTimeStr == null) {
            dateTimeError = "Не удалось сформировать дату/время для отправки"
            return false
        }

        return summaryError == null && dateTimeError == null
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
            val startTimeIso: String?
            val endTimeIso: String?

            if (isAllDay) {
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                startTimeIso = try { startDate.format(formatter) } catch (e: Exception) { null }
                val effectiveEndDate = endDate.plusDays(1)
                endTimeIso = try { effectiveEndDate.format(formatter) } catch (e: Exception) { null }
                Log.d("CreateEvent", "Formatting All-Day: Start=$startTimeIso, End=$endTimeIso")
            } else {
                // Используем startTime и endTime, которые точно не null после validateInput()
                startTimeIso = DateTimeUtils.formatDateTimeToIsoWithOffset(startDate, startTime!!, false, userTimeZoneId)
                endTimeIso = DateTimeUtils.formatDateTimeToIsoWithOffset(endDate, endTime!!, false, userTimeZoneId)
            }

            if (startTimeIso == null || endTimeIso == null) {
                generalError = "Ошибка форматирования даты/времени."
                Log.e("CreateEvent", "Failed to format ISO strings: start=$startTimeIso, end=$endTimeIso, isAllDay=$isAllDay")
                return@saveLambda
            }

            viewModel.createEvent(
                summary = summary.trim(),
                startTimeString = startTimeIso,
                endTimeString = endTimeIso,
                isAllDay = isAllDay,
                description = description.trim().takeIf { it.isNotEmpty() },
                location = location.trim().takeIf { it.isNotEmpty() }
            )
        } else {
            Toast.makeText(context, "Проверьте введенные данные", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новое событие") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
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
            OutlinedTextField(
                value = summary,
                onValueChange = {
                    summary = it
                    summaryError = null
                },
                label = { Text("Название события*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = summaryError != null,
                supportingText = { if (summaryError != null) Text(summaryError!!) },
                enabled = !isLoading
            )

            // --- Блок All Day ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Весь день", modifier = Modifier.weight(1f))
                Switch(
                    checked = isAllDay,
                    onCheckedChange = { checked ->
                        isAllDay = checked
                        dateTimeError = null
                        if (checked) {
                            startTime = null
                            endTime = null
                        } else {
                            val defaultStartTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
                            startTime = defaultStartTime
                            // Устанавливаем время конца на час позже, если даты совпадают
                            // или используем то же время на другую дату
                            val defaultEndTime = if (startDate == endDate) defaultStartTime.plusHours(1) else defaultStartTime
                            endTime = defaultEndTime.withNano(0)
                        }
                    },
                    enabled = !isLoading,

                )
            }

            // --- Общая ошибка даты/времени ---
            dateTimeError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
            }

            // --- Блок Начала ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // --- Поле Даты Начала с Оверлеем ---
                Box( // Родительский Box - НЕ кликабельный
                    modifier = Modifier.weight(1f)
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    OutlinedTextField(
                        value = startDate.format(dateFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Дата начала*") },
                        trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = dateTimeError != null,
                        enabled = !isLoading,
                        interactionSource = interactionSource // Можно оставить или убрать для теста
                    )
                    // Прозрачный Оверлей для клика
                    Box(
                        modifier = Modifier
                            .matchParentSize() // Занимает все место родителя
                            .clickable(enabled = !isLoading) { // Клик на оверлее
                                Log.d("ClickDebug", "Start Date Overlay Clicked")
                                showStartDatePicker = true
                            }
                    )
                } // --- Конец Поля Даты Начала ---

                // --- Поле Времени Начала с Оверлеем (если не all day) ---
                if (!isAllDay) {
                    Box( // Родительский Box - НЕ кликабельный
                        modifier = Modifier.weight(0.7f)
                    ) {
                        val interactionSource = remember { MutableInteractionSource() }
                        OutlinedTextField(
                            value = startTime?.format(timeFormatter) ?: "--:--",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Время*") },
                            trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            isError = dateTimeError != null && startTime == null,
                            enabled = !isLoading,
                            interactionSource = interactionSource // Можно оставить или убрать для теста
                        )
                        // Прозрачный Оверлей для клика
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(enabled = !isLoading && startTime != null) { // Клик на оверлее
                                    Log.d("ClickDebug", "Start Time Overlay Clicked")
                                    showStartTimePicker = true
                                }
                        )
                    }
                } else {
                    Spacer(Modifier.weight(0.7f))
                } // --- Конец Поля Времени Начала ---
            } // --- Конец Row Начала ---

            // --- Блок Конца ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // --- Поле Даты Конца с Оверлеем ---
                Box( // Родительский Box - НЕ кликабельный
                    modifier = Modifier.weight(1f)
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    OutlinedTextField(
                        value = endDate.format(dateFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Дата конца*") },
                        trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = dateTimeError != null,
                        enabled = !isLoading,
                        interactionSource = interactionSource
                    )
                    // Прозрачный Оверлей для клика
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(enabled = !isLoading) { // Клик на оверлее
                                Log.d("ClickDebug", "End Date Overlay Clicked")
                                showEndDatePicker = true
                            }
                    )
                } // --- Конец Поля Даты Конца ---

                // --- Поле Времени Конца с Оверлеем (если не all day) ---
                if (!isAllDay) {
                    Box( // Родительский Box - НЕ кликабельный
                        modifier = Modifier.weight(0.7f)
                    ) {
                        val interactionSource = remember { MutableInteractionSource() }
                        OutlinedTextField(
                            value = endTime?.format(timeFormatter) ?: "--:--",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Время*") },
                            trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            isError = dateTimeError != null && endTime == null,
                            enabled = !isLoading,
                            interactionSource = interactionSource
                        )
                        // Прозрачный Оверлей для клика
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(enabled = !isLoading && endTime != null) { // Клик на оверлее
                                    Log.d("ClickDebug", "End Time Overlay Clicked")
                                    showEndTimePicker = true
                                }
                        )
                    }
                } else {
                    Spacer(Modifier.weight(0.7f))
                } // --- Конец Поля Времени Конца ---
            } // --- Конец Row Конца ---


            // Описание
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4,
                enabled = !isLoading
            )

            // Местоположение
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Местоположение") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )

            // Сообщение об ошибке от бэкенда
            generalError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка Сохранить
            Button(
                onClick = onSaveClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Сохранить")
                }
            }
        } // End Column

        // --- Диалоги Material 3 ---

        // Диалог выбора Даты Начала
        if (showStartDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = startDate.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val selectedDate = Instant.ofEpochMilli(millis).atZone(systemZoneId).toLocalDate()
                                startDate = selectedDate
                                dateTimeError = null // Сброс ошибки
                                // Если дата конца стала раньше начала, подтягиваем дату конца
                                if (endDate.isBefore(selectedDate)) {
                                    endDate = selectedDate
                                }
                                // (Опционально) Проверка/коррекция времени конца, если даты совпали и время некорректно
                                if (!isAllDay && startDate == endDate && startTime != null && endTime != null && !startTime!!.isBefore(endTime!!)) {
                                    endTime = startTime!!.plusHours(1).withNano(0)
                                }
                            }
                            showStartDatePicker = false
                        },
                        enabled = datePickerState.selectedDateMillis != null // Кнопка активна только если дата выбрана
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
        if (showStartTimePicker && startTime != null) { // Показываем, только если startTime не null
            val timePickerState = rememberTimePickerState(
                initialHour = startTime?.hour ?: 0,
                initialMinute = startTime?.minute ?: 0,
                is24Hour = true // Или false, если нужен AM/PM
            )
            TimePickerDialog( // Используем обертку для лучшей компоновки диалога
                onDismissRequest = { showStartTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute).withNano(0)
                        startTime = selectedTime
                        dateTimeError = null // Сброс ошибки
                        // Если даты совпадают и время конца стало раньше или равно времени начала,
                        // сдвигаем время конца на час вперед
                        if (startDate == endDate && endTime != null && !selectedTime.isBefore(endTime!!)) {
                            endTime = selectedTime.plusHours(1).withNano(0)
                        }
                        showStartTimePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showStartTimePicker = false }) { Text("Отмена") }
                }
            ) {
                TimePicker(state = timePickerState)
            }
        }

        // Диалог выбора Даты Конца
        if (showEndDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = endDate.atStartOfDay(systemZoneId).toInstant().toEpochMilli(),
                // Устанавливаем минимальную дату = дате начала
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val selectedInstant = Instant.ofEpochMilli(utcTimeMillis)
                        val selectedLocalDate = selectedInstant.atZone(systemZoneId).toLocalDate()
                        return !selectedLocalDate.isBefore(startDate) // Нельзя выбрать дату раньше даты начала
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
                                endDate = selectedDate
                                dateTimeError = null // Сброс ошибки
                                // (Опционально) Проверка/коррекция времени конца, если даты совпали и время некорректно
                                if (!isAllDay && startDate == endDate && startTime != null && endTime != null && !startTime!!.isBefore(endTime!!)) {
                                    endTime = startTime!!.plusHours(1).withNano(0)
                                }
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
        if (showEndTimePicker && endTime != null) { // Показываем, только если endTime не null
            val timePickerState = rememberTimePickerState(
                initialHour = endTime?.hour ?: 0,
                initialMinute = endTime?.minute ?: 0,
                is24Hour = true
            )
            TimePickerDialog(
                onDismissRequest = { showEndTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute).withNano(0)
                        // Простая проверка: если даты одинаковые, время конца не может быть раньше времени начала
                        if (startDate == endDate && startTime != null && selectedTime.isBefore(startTime!!)) {
                            Toast.makeText(context, "Время конца не может быть раньше времени начала", Toast.LENGTH_SHORT).show()
                        } else {
                            endTime = selectedTime
                            dateTimeError = null // Сброс ошибки
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

// --- Вспомогательный Composable для TimePickerDialog (опционально, для единообразия с DatePickerDialog) ---
@Composable
fun TimePickerDialog(
    title: String = "Выберите время",
    onDismissRequest: () -> Unit,
    confirmButton: @Composable (() -> Unit),
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    // Используем стандартный AlertDialog из M3 как контейнер
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            // Обертка для центрирования TimePicker, если нужно
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                content() // Сюда передается TimePicker(state = ...)
            }
        },
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}