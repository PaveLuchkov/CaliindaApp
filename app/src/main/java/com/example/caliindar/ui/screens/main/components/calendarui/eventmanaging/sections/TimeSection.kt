package com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.ui.ChipsRow
import com.example.caliindar.ui.screens.main.components.calendarui.eventmanaging.ui.CustomOutlinedTextField
import android.text.format.DateFormat // Для определения формата времени 12/24
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

// Состояние, которое компонент будет возвращать наружу
data class EventDateTimeState(
    val startDate: LocalDate,
    val startTime: LocalTime?,
    val endDate: LocalDate,
    val endTime: LocalTime?,
    val isAllDay: Boolean,
    val isRecurring: Boolean,
    val recurrenceRule: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDateTimePicker(
    modifier: Modifier = Modifier,
    initialState: EventDateTimeState,
    onStateChange: (EventDateTimeState) -> Unit,
    isLoading: Boolean = false,
    // Callbacks для запроса показа диалогов выбора
    onRequestShowStartDatePicker: () -> Unit,
    onRequestShowStartTimePicker: () -> Unit,
    onRequestShowEndDatePicker: () -> Unit,
    onRequestShowEndTimePicker: () -> Unit,
) {
    // --- Внутреннее состояние компонента ---
    // Используем флаги напрямую вместо selectedMode
    var isAllDay by remember { mutableStateOf(initialState.isAllDay) }
    // Добавляем флаг isOneDay
    var isOneDay by remember {
        mutableStateOf(
            // Инициализируем isOneDay если начальные даты равны и время конца не задано (или all day)
            initialState.startDate == initialState.endDate && (initialState.isAllDay || initialState.endTime == null)
        )
    }
    var startDate by remember { mutableStateOf(initialState.startDate) }
    var startTime by remember { mutableStateOf(initialState.startTime) }
    var endDate by remember { mutableStateOf(initialState.endDate) }
    var endTime by remember { mutableStateOf(initialState.endTime) }
    var isRecurring by remember { mutableStateOf(initialState.isRecurring) }
    var recurrenceRule by remember { mutableStateOf(initialState.recurrenceRule) }

    var dateTimeError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // --- Форматтеры для отображения (без изменений) ---
    val deviceDateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
    }
    val deviceTimeFormatter = remember(context) {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    }

    // --- Эффект для валидации и оповещения об изменениях ---
    // Зависимости теперь включают isAllDay и isOneDay
    LaunchedEffect(startDate, startTime, endDate, endTime, isAllDay, isOneDay, isRecurring, recurrenceRule) {
        dateTimeError = null // Сброс ошибки

        // Определяем *актуальное* состояние на основе флагов для валидации и отправки
        val actualStartDate = startDate
        // endDate всегда равен startDate если isOneDay=true, иначе берем endDate
        val actualEndDate = if (isOneDay) startDate else endDate
        // startTime null только если isAllDay=true
        val actualStartTime = if (isAllDay) null else startTime
        // --- ИЗМЕНЕНИЕ ЗДЕСЬ ---
        // endTime null только если isAllDay=true
        val actualEndTime = if (isAllDay) null else {
            // Если isOneDay, но не isAllDay, нам нужно endTime
            if (isOneDay) {
                // Если endTime не установлено, пытаемся его установить (например, +1 час от startTime)
                // Важно: Не изменяем здесь локальные 'var endTime', только вычисляем для state/валидации
                endTime ?: startTime?.plusHours(1)?.withNano(0) // Используем существующее или вычисляем
            } else {
                // Обычный режим Date Range, используем endTime
                endTime
            }
        }
        // Валидация (учитываем актуальные значения)
        // 1. Даты: Конец >= Начала (только если не OneDay)
        if (!isOneDay && actualEndDate.isBefore(actualStartDate)) {
            dateTimeError = "Дата окончания не может быть раньше начала"
        }
        // 2. Время: Конец > Начала (только если не AllDay, не OneDay, и оба времени заданы)
        else if (!isAllDay && !isOneDay && actualStartTime != null && actualEndTime != null) {
            val startDateTime = actualStartTime.atDate(actualStartDate)
            val endDateTime = actualEndTime.atDate(actualEndDate)
            if (!startDateTime.isBefore(endDateTime)) { // Должно быть строго раньше
                dateTimeError = "Время окончания должно быть позже начала"
            }
        }
        // 3. Обязательность времени (если не AllDay)
        else if (!isAllDay && actualStartTime == null) {
            dateTimeError = "Укажите время начала" // Ошибка, если нужно время, но его нет
        }
        // 4. Обязательность времени конца (если не AllDay и не OneDay)
        else if (!isAllDay && !isOneDay && actualEndTime == null) {
            dateTimeError = "Укажите время конца" // Ошибка, если нужно время конца, но его нет
        }


        // Оповещение родителя с актуальным состоянием
        onStateChange(
            EventDateTimeState(
                startDate = actualStartDate,
                startTime = actualStartTime,
                endDate = actualEndDate, // endDate будет равен startDate если isOneDay=true
                endTime = actualEndTime, // endTime будет null если isAllDay=true или isOneDay=true
                isAllDay = isAllDay,
                isRecurring = isRecurring,
                recurrenceRule = recurrenceRule
            )
        )
    }

    // --- UI Компонента ---
    Column(modifier = modifier) {

        // --- Filter Chips ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- Чип "All Day" ---
            FilterChip(
                selected = isAllDay,
                onClick = {
                    val newState = !isAllDay
                    isAllDay = newState
                    if (newState) { // Включили All Day
                        startTime = null
                        endTime = null
                    } else { // Выключили All Day
                        // Восстанавливаем время по умолчанию
                        val defaultStartTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
                        startTime = defaultStartTime
                        // Если не One Day, восстанавливаем и время конца
                        if (!isOneDay) {
                            val defaultEndTime = if (startDate == endDate) defaultStartTime.plusHours(1) else defaultStartTime
                            endTime = defaultEndTime.withNano(0)
                        } else {
                            endTime = null // Для One Day время конца не нужно
                        }
                    }
                },
                label = { Text("Весь день") },
                enabled = !isLoading
            )

            // --- Чип "One Day" ---
            FilterChip(
                selected = isOneDay,
                onClick = {
                    val newState = !isOneDay
                    isOneDay = newState
                    if (newState) { // Включили One Day
                        endDate = startDate // Устанавливаем дату конца = дате начала
                        // --- ИЗМЕНЕНИЕ ЗДЕСЬ ---
                        // Время конца нужно, если НЕ All Day
                        if (!isAllDay) {
                            // Устанавливаем endTime, если его нет или оно некорректно
                            if (endTime == null || (startTime != null && !startTime!!.isBefore(endTime!!))) {
                                endTime = startTime?.plusHours(1)?.withNano(0) ?: LocalTime.now().plusHours(2).withMinute(0).withSecond(0).withNano(0)
                            }
                            // startTime тоже должен быть установлен, если его нет
                            if (startTime == null) {
                                startTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
                                // Пересчитываем endTime на основе нового startTime
                                endTime = startTime!!.plusHours(1).withNano(0)
                            }
                        } else {
                            // Если All Day включен, время конца не нужно
                            endTime = null
                        }
                        // --- КОНЕЦ ИЗМЕНЕНИЯ ---
                    } else { // Выключили One Day
                        // При выключении One Day, даты/время остаются как есть,
                        // пользователь должен будет настроить endDate/endTime сам, если переходит в Date Range режим.
                        // Ничего не меняем здесь при выключении.
                    }
                },
                label = { Text("Один день") },
                enabled = !isLoading
            )

            // --- Чип "Recurring" ---
            FilterChip(
                selected = isRecurring,
                onClick = { isRecurring = !isRecurring },
                label = { Text("Повторение") },
                leadingIcon = if (isRecurring) {
                    { Icon(Icons.Filled.Check, contentDescription = "Повторение вкл.") }
                } else null,
                enabled = !isLoading
            )
        }

        // --- Общая ошибка даты/времени ---
        dateTimeError?.let {
            Text(
                it,
                color = colorScheme.error, // Используем MaterialTheme
                style = typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // --- Поля ввода Даты/Времени (логика отображения изменена) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // Используем Column для полей

            // --- Определяем, что показывать ---
            if (isAllDay && isOneDay) {
                // Случай 1: All Day + One Day = Только Дата Начала
                DatePickerField(
                    label = "Дата*",
                    date = startDate,
                    dateFormatter = deviceDateFormatter,
                    isError = dateTimeError != null,
                    isLoading = isLoading,
                    onClick = onRequestShowStartDatePicker,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
            } else if (isAllDay) {
                // Случай 2: Только All Day = Дата Начала + Дата Конца
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    DatePickerField("Дата начала*", startDate, deviceDateFormatter, dateTimeError != null, isLoading, onRequestShowStartDatePicker, Modifier.weight(1f))
                    DatePickerField("Дата конца*", endDate, deviceDateFormatter, dateTimeError != null, isLoading, onRequestShowEndDatePicker, Modifier.weight(1f))
                }
            } else if (isOneDay) {
                // Случай 3: Только One Day = Дата Начала + Время Начала + Время Конца
                // Отобразим в двух строках для лучшего вида на малых экранах
                // Строка 1: Дата
                DatePickerField(
                    label = "Дата*",
                    date = startDate,
                    dateFormatter = deviceDateFormatter,
                    isError = dateTimeError != null,
                    isLoading = isLoading,
                    onClick = onRequestShowStartDatePicker,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
                // Строка 2: Время начала и конца
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    TimePickerField(
                        label = "Время начала*",
                        time = startTime,
                        timeFormatter = deviceTimeFormatter,
                        isError = dateTimeError != null && startTime == null,
                        isLoading = isLoading,
                        onClick = onRequestShowStartTimePicker,
                        modifier = Modifier.weight(1f) // Делим поровну
                    )
                    TimePickerField(
                        label = "Время конца*",
                        time = endTime, // Используем endTime
                        timeFormatter = deviceTimeFormatter,
                        isError = dateTimeError != null && endTime == null, // Проверяем endTime
                        isLoading = isLoading,
                        onClick = onRequestShowEndTimePicker, // Нужен callback для времени конца
                        modifier = Modifier.weight(1f) // Делим поровну
                    )
                }
            } else {
                // Случай 4: Ни All Day, ни One Day = Полный набор полей
                // Блок Начала
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    DatePickerField("Дата начала*", startDate, deviceDateFormatter, dateTimeError != null, isLoading, onRequestShowStartDatePicker, Modifier.weight(0.6f))
                    TimePickerField("Время начала*", startTime, deviceTimeFormatter, dateTimeError != null && startTime == null, isLoading, onRequestShowStartTimePicker, Modifier.weight(0.4f))
                }
                // Блок Конца
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    DatePickerField("Дата конца*", endDate, deviceDateFormatter, dateTimeError != null, isLoading, onRequestShowEndDatePicker, Modifier.weight(0.6f))
                    TimePickerField("Время конца*", endTime, deviceTimeFormatter, dateTimeError != null && endTime == null, isLoading, onRequestShowEndTimePicker, Modifier.weight(0.4f))
                }
            }
        } // Конец Column для полей ввода

        // --- Настройки Повторения (если включено) ---
        if (isRecurring) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Настройки повторения (TODO)",
                style = typography.titleMedium, // Используем MaterialTheme
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            OutlinedTextField(
                value = recurrenceRule ?: "",
                onValueChange = { recurrenceRule = it }, // Пока просто строка
                label = { Text("Правило (RRULE - TODO)") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                enabled = !isLoading
            )
        }
    }
}

// --- Вспомогательные Composable для полей ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    date: LocalDate,
    dateFormatter: DateTimeFormatter,
    isError: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ClickableTextField(
        value = date.format(dateFormatter),
        label = label,
        trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = "Выбрать дату") },
        isError = isError,
        isLoading = isLoading,
        onClick = onClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerField(
    label: String,
    time: LocalTime?,
    timeFormatter: DateTimeFormatter,
    isError: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ClickableTextField(
        value = time?.format(timeFormatter) ?: "--:--",
        label = label,
        trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = "Выбрать время") },
        isError = isError,
        isLoading = isLoading,
        onClick = onClick,
        modifier = modifier
    )
}

// Общий Composable для кликабельного текстового поля (паттерн с оверлеем)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClickableTextField(
    value: String,
    label: String,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {}, // Не изменяется напрямую
            readOnly = true,
            label = { Text(label) },
            trailingIcon = trailingIcon,
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            enabled = !isLoading,
            // Убираем interactionSource, т.к. клик обрабатывается оверлеем
            // interactionSource = remember { MutableInteractionSource() }, // Не нужно
            shape = RoundedCornerShape(16.dp) // Скругление можно вынести в параметры
        )
        // Прозрачный Оверлей для клика
        Box(
            modifier = Modifier
                .matchParentSize() // Занимает все место родителя
                .clickable(
                    enabled = !isLoading,
                    onClick = onClick,
                    indication = null, // Можно убрать стандартную рябь
                    interactionSource = remember { MutableInteractionSource() } // Для обработки состояний нажатия оверлея, если нужно
                )
        )
    }
}