package com.example.caliindar.ui.screens.main.components


import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.caliindar.data.calendar.CreateEventResult
import com.example.caliindar.data.local.DateTimeUtils
import com.example.caliindar.ui.screens.main.MainViewModel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    viewModel: MainViewModel,
    initialDate: LocalDate,
    onNavigateBack: () -> Unit
) {
    var summary by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate>(initialDate) }
    var startTime by remember { mutableStateOf<LocalTime?>(LocalTime.now().plusHours(1).withMinute(0).withSecond(0)) }
    var endDate by remember { mutableStateOf<LocalDate>(initialDate) }
    var endTime by remember { mutableStateOf<LocalTime?>(startTime?.plusHours(1)) }
    var isAllDay by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var summaryError by remember { mutableStateOf<String?>(null) }
    var dateTimeError by remember { mutableStateOf<String?>(null) }

    val createEventState by viewModel.createEventResult.collectAsState()
    val userTimeZoneId by viewModel.timeZone.collectAsState() // <--- Получаем String

    var isLoading by remember { mutableStateOf(false) }
    var generalError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // --- Логика валидации ---
    fun validateInput(): Boolean {
        summaryError = if (summary.isBlank()) "Название не может быть пустым" else null
        dateTimeError = null // Сбрасываем

        // Проверяем даты
        if (endDate.isBefore(startDate)) {
            dateTimeError = "Дата конца не может быть раньше даты начала"
            return false
        }

        // Проверяем время, если не "весь день"
        if (!isAllDay) {
            if (startTime == null || endTime == null) {
                dateTimeError = "Укажите время начала и конца"
                return false
            }
            // Собираем LocalDateTime для сравнения
            val startDateTime = LocalDateTime.of(startDate, startTime)
            val endDateTime = LocalDateTime.of(endDate, endTime)
            if (endDateTime.isBefore(startDateTime)) {
                dateTimeError = "Время/дата конца не может быть раньше времени/даты начала"
                return false
            }
        }

        // Дополнительно проверим, что форматирование сработает (хотя ошибки маловероятны тут)
        val testStartTimeStr = DateTimeUtils.formatDateTimeToIsoWithOffset(startDate, startTime, isAllDay, userTimeZoneId)
        // Для конца all-day берем следующую дату
        val effectiveEndDate = if (isAllDay) endDate.plusDays(1) else endDate
        val testEndTimeStr = DateTimeUtils.formatDateTimeToIsoWithOffset(effectiveEndDate, endTime, isAllDay, userTimeZoneId)

        if (testStartTimeStr == null || testEndTimeStr == null) {
            // Ошибка произошла внутри formatDateTimeToIsoWithOffset (например, невалидный пояс, хотя мы ставим дефолтный)
            // Или время не было указано для non-all-day (уже проверено выше)
            dateTimeError = "Не удалось сформировать дату/время для отправки"
            return false
        }


        return summaryError == null && dateTimeError == null
    }

    // --- Обработка состояния ViewModel ---
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
                // Очищаем состояние в ViewModel, чтобы ошибка не висела вечно
                viewModel.consumeCreateEventResult()
            }
            is CreateEventResult.Loading -> {
                generalError = null
            }
            is CreateEventResult.Idle -> {
                // Можно убрать generalError, если нужно
                // generalError = null
            }
        }
    }

    // Функция сохранения
    val onSaveClick: () -> Unit = saveLambda@ {
        generalError = null
        if (validateInput()) {
            // Используем DateTimeUtils для форматирования
            val startTimeIso = DateTimeUtils.formatDateTimeToIsoWithOffset(
                date = startDate,
                time = startTime, // Утилита сама разберется с null для isAllDay
                isAllDay = isAllDay,
                zoneIdString = userTimeZoneId // Передаем ID строкой
            )

            // ВАЖНО: Для события "весь день" конец - это начало *следующего* дня после endDate
            val effectiveEndDate = if (isAllDay) endDate.plusDays(1) else endDate
            val endTimeIso = DateTimeUtils.formatDateTimeToIsoWithOffset(
                date = effectiveEndDate, // Используем скорректированную дату конца для all-day
                time = endTime,
                isAllDay = isAllDay, // Флаг isAllDay нужен для выбора времени (00:00)
                zoneIdString = userTimeZoneId
            )

            // Проверяем, что строки сформировались (хотя validateInput уже это сделал)
            if (startTimeIso == null || endTimeIso == null) {
                generalError = "Ошибка форматирования даты/времени. Проверьте настройки часового пояса."
                return@saveLambda  // Выход из лямбды onClick
            }


            viewModel.createEvent(
                summary = summary.trim(),
                startTimeString = startTimeIso,
                endTimeString = endTimeIso,
                isAllDay = isAllDay, // Отправляем сам флаг тоже
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
                    IconButton(onClick = onNavigateBack, enabled = !isLoading) { // Блокируем навигацию во время загрузки
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
            verticalArrangement = Arrangement.spacedBy(12.dp) // Немного уменьшил отступ
        ) {
            OutlinedTextField(
                value = summary,
                onValueChange = {
                    summary = it
                    summaryError = null // Сбрасываем ошибку при изменении
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
                        dateTimeError = null // Сбрасываем ошибку времени
                        if (checked) {
                            // Если включили "весь день", время не нужно
                            startTime = null
                            endTime = null
                        } else {
                            // Если выключили, ставим время по умолчанию (или последнее выбранное)
                            val defaultStartTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0)
                            startTime = defaultStartTime
                            // Устанавливаем время конца через час после начала,
                            // если дата конца совпадает с датой начала
                            if (startDate == endDate) {
                                endTime = defaultStartTime.plusHours(1)
                            } else {
                                // Если даты разные, можно установить конец на то же время, что и начало, но на другую дату
                                // или на час позже - зависит от предпочтений UX
                                endTime = defaultStartTime // Или defaultStartTime.plusHours(1)
                            }
                        }
                    },
                    enabled = !isLoading
                )
            }

            // --- Общая ошибка даты/времени ---
            dateTimeError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp)) // Небольшой отступ после ошибки
            }

            // --- Блок Начала ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top // Выравниваем по верху для полей с ошибками
            ) {
                // Выбор Даты Начала
                OutlinedTextField(
                    value = startDate.format(dateFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Дата начала*") },
                    trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !isLoading) { showStartDatePicker = true }, // Блокируем клик при загрузке
                    isError = dateTimeError != null, // Подсвечиваем оба поля при ошибке даты/времени
                    enabled = !isLoading
                )
                // Выбор Времени Начала (если не all day)
                if (!isAllDay) {
                    OutlinedTextField(
                        value = startTime?.format(timeFormatter) ?: "Время*",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Время*") },
                        trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                        modifier = Modifier
                            .weight(0.7f) // Дал чуть больше места времени
                            .clickable(enabled = !isLoading) { showStartTimePicker = true },
                        isError = dateTimeError != null,
                        enabled = !isLoading
                    )
                } else {
                    Spacer(Modifier.weight(0.7f)) // Занимаем место, если времени нет
                }
            }

            // --- Блок Конца ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Выбор Даты Конца
                OutlinedTextField(
                    value = endDate.format(dateFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Дата конца*") },
                    trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !isLoading) { showEndDatePicker = true },
                    isError = dateTimeError != null,
                    enabled = !isLoading
                )
                // Выбор Времени Конца (если не all day)
                if (!isAllDay) {
                    OutlinedTextField(
                        value = endTime?.format(timeFormatter) ?: "Время*",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Время*") },
                        trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                        modifier = Modifier
                            .weight(0.7f)
                            .clickable(enabled = !isLoading) { showEndTimePicker = true },
                        isError = dateTimeError != null,
                        enabled = !isLoading
                    )
                } else {
                    Spacer(Modifier.weight(0.7f))
                }
            }

            // Описание
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") }, // Убрал (опционально) для краткости
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
                enabled = !isLoading, // Блокируем только во время загрузки
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary, // Цвет индикатора на кнопке
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Сохранить")
                }
            }
        } // End Column

        // --- Диалоги выбора Даты/Времени ---
        if (showStartDatePicker) {
            ShowDatePickerDialog( // Переименовал для ясности
                context = context,
                initialDate = startDate,
                onDateSelected = { selectedDate ->
                    startDate = selectedDate
                    dateTimeError = null // Сбрасываем ошибку при изменении
                    // Если дата конца раньше новой даты начала, сдвигаем конец
                    if (endDate.isBefore(selectedDate)) {
                        endDate = selectedDate
                        // Если время было установлено и даты стали одинаковыми,
                        // нужно проверить и время конца
                        if (!isAllDay && startTime != null && endTime != null && !startTime!!.isBefore(endTime!!)) {
                            endTime = startTime!!.plusHours(1) // Сдвигаем время конца
                        }
                    }
                    showStartDatePicker = false
                },
                onDismiss = { showStartDatePicker = false }
            )
        }
        if (showStartTimePicker && startTime != null) { // Показываем только если время не null
            ShowTimePickerDialog( // Переименовал
                context = context,
                initialTime = startTime!!, // Теперь не nullable
                onTimeSelected = { selectedTime ->
                    startTime = selectedTime
                    dateTimeError = null
                    // Если время конца раньше или равно новому времени начала (при той же дате), сдвигаем конец
                    // Сравнение нужно только если дата начала и конца совпадают
                    if (startDate == endDate && endTime != null && !selectedTime.isBefore(endTime!!)) {
                        endTime = selectedTime.plusHours(1).withSecond(0).withNano(0) // Сдвигаем на час, обнуляем секунды
                    }
                    showStartTimePicker = false
                },
                onDismiss = { showStartTimePicker = false }
            )
        }
        if (showEndDatePicker) {
            ShowDatePickerDialog(
                context = context,
                initialDate = endDate,
                minDate = startDate, // Нельзя выбрать дату конца раньше даты начала
                onDateSelected = { selectedDate ->
                    endDate = selectedDate
                    dateTimeError = null
                    // Если время было установлено и даты разные,
                    // возможно, нужно скорректировать время конца (например, оставить как есть или сбросить?)
                    // Оставим как есть пока.
                    // Но если дата конца стала равна дате начала, а время конца раньше времени начала, сдвинем
                    if (startDate == selectedDate && !isAllDay && startTime != null && endTime != null && !startTime!!.isBefore(endTime!!)) {
                        endTime = startTime!!.plusHours(1)
                    }
                    showEndDatePicker = false
                },
                onDismiss = { showEndDatePicker = false }
            )
        }
        if (showEndTimePicker && endTime != null) {
            ShowTimePickerDialog(
                context = context,
                initialTime = endTime!!,
                onTimeSelected = { selectedTime ->
                    // Валидация: если дата та же, время конца не должно быть раньше начала
                    if (startDate == endDate && startTime != null && selectedTime.isBefore(startTime!!)) {
                        // Можно показать Toast или установить ошибку
                        Toast.makeText(context, "Время конца не может быть раньше времени начала", Toast.LENGTH_SHORT).show()
                        // Не меняем время или меняем на валидное? Пока не меняем.
                        // endTime = startTime!!.plusHours(1) // Альтернатива: сразу сдвинуть
                    } else {
                        endTime = selectedTime
                        dateTimeError = null // Сбрасываем ошибку, если выбор корректен
                    }
                    showEndTimePicker = false
                },
                onDismiss = { showEndTimePicker = false }
            )
        }

    } // End Scaffold
}

// --- Вспомогательные Composable для диалогов ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDatePickerDialog(
    context: Context,
    initialDate: LocalDate,
    minDate: LocalDate? = null,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val year = initialDate.year
    val month = initialDate.monthValue - 1 // DatePicker months are 0-indexed
    val day = initialDate.dayOfMonth

    // Non-Composable block to create DatePickerDialog
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
            onDateSelected(LocalDate.of(selectedYear, selectedMonth + 1, selectedDayOfMonth))
        }, year, month, day
    ).apply {
        setOnDismissListener { onDismiss() }
        minDate?.let {
            // Convert LocalDate to milliseconds for DatePicker
            val cal = Calendar.getInstance().apply {
                clear() // Clear time to get the start of the day
                set(it.year, it.monthValue - 1, it.dayOfMonth)
            }
            datePicker.minDate = cal.timeInMillis
        }
    }
    datePickerDialog.show()
}

@Composable
fun ShowTimePickerDialog( // Переименовал
    context: Context,
    initialTime: LocalTime,
    is24HourView: Boolean = true, // Лучше использовать системную настройку? Но для выбора обычно 24ч удобнее.
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val hour = initialTime.hour
    val minute = initialTime.minute

    val timePickerDialog = remember(context, initialTime, is24HourView) { // Добавил is24HourView в ключ
        TimePickerDialog(
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
}