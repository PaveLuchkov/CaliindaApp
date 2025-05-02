package com.example.caliindar.ui.screens.main.components

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule // Иконка для времени
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.DatePickerDialog
import com.example.caliindar.ui.screens.main.MainViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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

@OptIn(ExperimentalMaterial3Api::class) // Для TimePickerDialog
@Composable
fun CreateEventScreen(
    viewModel: MainViewModel, // Используем MainViewModel (или создай CreateEventViewModel)
    initialDate: LocalDate,
    onNavigateBack: () -> Unit
) {
    // Подписываемся на состояние из ViewModel (нужно будет добавить в ViewModel)
    // val screenState by viewModel.createEventState.collectAsStateWithLifecycle()
    // Пока используем локальное состояние для примера
    var summary by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(initialDate) }
    var startTime by remember { mutableStateOf<LocalTime?>(LocalTime.now().plusHours(1).withMinute(0).withSecond(0)) } // Пример: следующий час
    var endDate by remember { mutableStateOf<LocalDate?>(initialDate) }
    var endTime by remember { mutableStateOf<LocalTime?>(startTime?.plusHours(1)) } // Пример: +1 час от начала
    var isAllDay by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) } // Локальное состояние загрузки
    var error by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Состояния для Date/Time Picker Dialogs
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") } // Всегда 24ч для выбора

    // TODO: Добавить логику для сброса времени при переключении isAllDay
    // TODO: Добавить валидацию (конец не раньше начала)

    // Функция сохранения (вызывает ViewModel)
    val onSaveClick: () -> Unit = {
        // TODO: Валидация данных
        // ...
        // TODO: Сформировать startTimeString, endTimeString в ISO формате с учетом TimeZone и isAllDay
        // val zoneIdString = viewModel.timeZone.value // Получаем текущий пояс
        // val startTimeIso = formatDateTimeToIso(...)
        // val endTimeIso = formatDateTimeToIso(...)
        // TODO: Вызвать функцию ViewModel для сохранения
        // viewModel.createEvent(
        //     summary = summary,
        //     startTimeString = startTimeIso,
        //     endTimeString = endTimeIso,
        //     isAllDay = isAllDay,
        //     description = description,
        //     location = location
        // )
        // Временно просто навигация назад
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новое событие") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                .verticalScroll(rememberScrollState()) // Делаем контент скроллящимся
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it },
                label = { Text("Название события") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = error != null // Пример подсветки ошибки
            )

            // --- Блок All Day ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Весь день", modifier = Modifier.weight(1f))
                Switch(
                    checked = isAllDay,
                    onCheckedChange = {
                        isAllDay = it
                        // Если включили "весь день", сбрасываем время
                        if (it) {
                            startTime = null
                            endTime = null
                        } else { // Если выключили, ставим время по умолчанию
                            startTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0)
                            endTime = startTime?.plusHours(1)
                        }
                    }
                )
            }

            // --- Блок Начала ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Выбор Даты Начала
                OutlinedTextField(
                    value = startDate?.format(dateFormatter) ?: "Выберите дату",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Дата начала") },
                    trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStartDatePicker = true }
                )
                // Выбор Времени Начала (если не all day)
                if (!isAllDay) {
                    OutlinedTextField(
                        value = startTime?.format(timeFormatter) ?: "Время",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Время") },
                        trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                        modifier = Modifier
                            .weight(0.6f) // Чуть меньше места для времени
                            .clickable { showStartTimePicker = true }
                    )
                }
            }

            // --- Блок Конца ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Выбор Даты Конца
                OutlinedTextField(
                    value = endDate?.format(dateFormatter) ?: "Выберите дату",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Дата конца") },
                    trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showEndDatePicker = true }
                )
                // Выбор Времени Конца (если не all day)
                if (!isAllDay) {
                    OutlinedTextField(
                        value = endTime?.format(timeFormatter) ?: "Время",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Время") },
                        trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                        modifier = Modifier
                            .weight(0.6f)
                            .clickable { showEndTimePicker = true }
                    )
                }
            }

            // Описание
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание (опционально)") },
                modifier = Modifier.fillMaxWidth().height(100.dp), // Задаем высоту
                maxLines = 4
            )

            // Местоположение
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Местоположение (опционально)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Сообщение об ошибке
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка Сохранить
            Button(
                onClick = onSaveClick,
                enabled = !isLoading, // Блокируем во время загрузки
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Сохранить")
                }
            }
        } // End Column

        // --- Диалоги выбора Даты/Времени ---
        if (showStartDatePicker) {
            ShowDatePicker(
                context = context,
                initialDate = startDate ?: LocalDate.now(),
                onDateSelected = { selectedDate ->
                    startDate = selectedDate
                    // Если дата конца раньше новой даты начала, сдвигаем конец
                    if (endDate == null || endDate!!.isBefore(selectedDate)) {
                        endDate = selectedDate
                    }
                    showStartDatePicker = false
                },
                onDismiss = { showStartDatePicker = false }
            )
        }
        if (showStartTimePicker) {
            ShowTimePicker(
                context = context,
                initialTime = startTime ?: LocalTime.now(),
                onTimeSelected = { selectedTime ->
                    startTime = selectedTime
                    // Если время конца раньше или равно новому времени начала (при той же дате), сдвигаем конец
                    if (startDate != null && endDate != null && startDate == endDate && endTime != null && !selectedTime.isBefore(endTime)) {
                        endTime = selectedTime.plusHours(1) // Сдвигаем на час
                    }
                    showStartTimePicker = false
                },
                onDismiss = { showStartTimePicker = false }
            )
        }
        if (showEndDatePicker) {
            ShowDatePicker(
                context = context,
                initialDate = endDate ?: startDate ?: LocalDate.now(),
                minDate = startDate, // Нельзя выбрать дату конца раньше даты начала
                onDateSelected = { selectedDate ->
                    endDate = selectedDate
                    showEndDatePicker = false
                },
                onDismiss = { showEndDatePicker = false }
            )
        }
        if (showEndTimePicker) {
            ShowTimePicker(
                context = context,
                initialTime = endTime ?: startTime?.plusHours(1) ?: LocalTime.now(),
                onTimeSelected = { selectedTime ->
                    // TODO: Добавить валидацию, чтобы время конца (если дата та же) не было раньше времени начала
                    endTime = selectedTime
                    showEndTimePicker = false
                },
                onDismiss = { showEndTimePicker = false }
            )
        }

    } // End Scaffold
}

// Вспомогательные Composable для диалогов (можно вынести в отдельный файл)
@Composable
fun ShowDatePicker(
    context: android.content.Context,
    initialDate: LocalDate,
    minDate: LocalDate? = null, // Опционально: минимальная дата
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val year = initialDate.year
    val month = initialDate.monthValue - 1 // Месяцы в DatePickerDialog 0-11
    val day = initialDate.dayOfMonth

    val datePickerDialog = remember(context, initialDate, minDate) {
        android.app.DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                onDateSelected(LocalDate.of(selectedYear, selectedMonth + 1, selectedDayOfMonth))
            }, year, month, day
        ).apply {
            setOnDismissListener { onDismiss() }
            // Устанавливаем минимальную дату, если она есть
            minDate?.let {
                val cal = Calendar.getInstance().apply { timeInMillis = it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
                datePicker.minDate = cal.timeInMillis
            }
        }
    }

    DisposableEffect(datePickerDialog) {
        datePickerDialog.show()
        onDispose {
            datePickerDialog.dismiss()
        }
    }
}

@Composable
fun ShowTimePicker(
    context: android.content.Context,
    initialTime: LocalTime,
    is24HourView: Boolean = true, // Использовать 24-часовой формат для выбора
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val hour = initialTime.hour
    val minute = initialTime.minute

    val timePickerDialog = remember(context, initialTime) {
        android.app.TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                onTimeSelected(LocalTime.of(selectedHour, selectedMinute))
            }, hour, minute, is24HourView
        ).apply {
            setOnDismissListener { onDismiss() }
        }
    }

    DisposableEffect(timePickerDialog) {
        timePickerDialog.show()
        onDispose {
            timePickerDialog.dismiss()
        }
    }

    // Для Compose Material 3 TimePickerDialog (если хочешь использовать его)
    /*
     val timeState = rememberTimePickerState(
         initialHour = initialTime.hour,
         initialMinute = initialTime.minute,
         is24Hour = true // Или использовать настройку пользователя
     )
     TimePickerDialog( // Это Composable диалог
         onCancel = onDismiss,
         onConfirm = {
             onTimeSelected(LocalTime.of(timeState.hour, timeState.minute))
             onDismiss()
         }
     ) {
         TimePicker(state = timeState) // Сам пикер
     }
    */
}