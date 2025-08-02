package com.lpavs.caliinda.feature.event_management.ui.create

import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.data.calendar.CreateEventResult
import com.lpavs.caliinda.data.local.DateTimeUtils
import com.lpavs.caliinda.ui.screens.main.MainViewModel
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimePicker
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimeState
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventNameSection
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.RecurrenceEndType
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.RecurrenceOption
import com.lpavs.caliinda.feature.event_management.ui.shared.AdaptiveContainer
import com.lpavs.caliinda.feature.event_management.ui.shared.TimePickerDialog
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class) // Необходимо для M3 Dialogs и Pickers
@Composable
fun CreateEventScreen(
    viewModel: MainViewModel,
    userTimeZoneId: String,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    currentSheetValue: SheetValue
) {
  var summary by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }

  var summaryError by remember { mutableStateOf<String?>(null) }
  var validationError by remember { mutableStateOf<String?>(null) }

  val createEventState by viewModel.createEventResult.collectAsStateWithLifecycle()
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
    val defaultStartTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
    val defaultEndTime = LocalTime.now().plusHours(2).withMinute(0).withSecond(0).withNano(0)
    var effectiveEndDate = initialDate

    // Если время конца раньше времени начала (например, startTime=23:00, endTime=00:00),
    // это означает переход через полночь, так что endDate должен быть на следующий день.
    if (defaultEndTime.isBefore(defaultStartTime)) {
      effectiveEndDate = initialDate.plusDays(1)
    }
    mutableStateOf(
        EventDateTimeState(
            startDate = initialDate,
            startTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0),
            endDate = effectiveEndDate,
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
      val startDateStr =
          try {
            state.startDate.format(formatter)
          } catch (_: Exception) {
            null
          }
      // Для all-day события конец должен быть на следующий день после фактического последнего дня
      val effectiveEndDate = state.endDate.plusDays(1)
      val endDateStr =
          try {
            effectiveEndDate.format(formatter)
          } catch (_: Exception) {
            null
          }
      Log.d("CreateEvent", "Formatting All-Day: Start Date=$startDateStr, End Date=$endDateStr")
      Pair(startDateStr, endDateStr)
    } else {
      // Для событий со временем используем ISO с оффсетом
      if (timeZoneId == null) {
        Log.e("CreateEvent", "Cannot format timed event without TimeZone ID!")
        return Pair(null, null) // Не можем форматировать без таймзоны
      }
      // validateInput уже проверил, что startTime и endTime не null
      val startTimeIso =
          DateTimeUtils.formatDateTimeToIsoWithOffset(
              state.startDate, state.startTime!!, false, timeZoneId)
      val endTimeIso =
          DateTimeUtils.formatDateTimeToIsoWithOffset(
              state.endDate, state.endTime!!, false, timeZoneId)
      Log.d(
          "CreateEvent", "Formatting Timed: Start DateTime=$startTimeIso, End DateTime=$endTimeIso")
      Pair(startTimeIso, endTimeIso)
    }
  }

  // --- Логика валидации ---
  fun validateInput(): Boolean {
    summaryError =
        if (summary.isBlank()) R.string.error_summary_cannot_be_empty.toString() else null
    validationError = null
    val state = eventDateTimeState
    if (!state.isAllDay && (state.startTime == null || state.endTime == null)) {
      validationError = R.string.error_specify_start_and_end_time.toString()
      return false
    }
    val (testStartTimeStr, testEndTimeStr) = formatEventTimesForSaving(state, userTimeZoneId)
    if (testStartTimeStr == null || testEndTimeStr == null) {
      validationError = R.string.error_failed_to_format_datetime.toString()
      return false
    }
    return summaryError == null && validationError == null
  }

  // --- Обработка состояния ViewModel (без изменений) ---
  LaunchedEffect(createEventState) {
    isLoading = createEventState is CreateEventResult.Loading
    when (val result = createEventState) {
      is CreateEventResult.Success -> {
        Toast.makeText(context, R.string.event_updated_successfully, Toast.LENGTH_SHORT).show()
        viewModel.consumeCreateEventResult()
        onDismiss()
      }

      is CreateEventResult.Error -> {
        generalError = result.message
        viewModel.consumeCreateEventResult()
      }

      is CreateEventResult.Loading -> generalError = null
      is CreateEventResult.Idle -> {
        /* Можно убрать generalError, если нужно */
      }
    }
  }

  // Функция сохранения (логика форматирования без изменений)
  val onSaveClick: () -> Unit = saveLambda@{
    generalError = null
    if (validateInput()) {
      // Передаем userTimeZoneId в функцию форматирования
      val (startStr, endStr) = formatEventTimesForSaving(eventDateTimeState, userTimeZoneId)

      if (startStr == null || endStr == null) {
        validationError = R.string.error_failed_to_format_datetime.toString()
        Log.e(
            "CreateEvent",
            "Failed to format strings based on state: $eventDateTimeState and TimeZone: $userTimeZoneId")
        return@saveLambda
      }
      val baseRule = eventDateTimeState.recurrenceRule?.takeIf { it.isNotBlank() }
      var finalRecurrenceRule: String? = null

      if (baseRule != null) {
        val ruleParts = mutableListOf(baseRule) // Начинаем с FREQ=...

        // Добавляем BYDAY, если нужно
        if (baseRule == RecurrenceOption.Weekly.rruleValue &&
            eventDateTimeState.selectedWeekdays.isNotEmpty()) {
          val bydayString =
              eventDateTimeState.selectedWeekdays.sorted().joinToString(",") { day ->
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
              val systemZone = ZoneId.systemDefault()
              endDate.atTime(LocalTime.MAX).atZone(systemZone)

              val endDateTimeUtc =
                  endDate.atTime(23, 59, 59).atZone(systemZone).withZoneSameInstant(ZoneOffset.UTC)

              val untilString = untilFormatter.format(endDateTimeUtc)
              ruleParts.add("UNTIL=$untilString")
            }
          }

          RecurrenceEndType.COUNT -> {
            eventDateTimeState.recurrenceCount?.let { count -> ruleParts.add("COUNT=$count") }
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
          startTimeString = startStr, // Отправляем отформатированную строку
          endTimeString = endStr, // Отправляем отформатированную строку
          isAllDay = eventDateTimeState.isAllDay,
          timeZoneId =
              if (eventDateTimeState.isAllDay) null
              else userTimeZoneId, // Передаем ID таймзоны только для timed событий
          description = description.trim().takeIf { it.isNotEmpty() },
          location = location.trim().takeIf { it.isNotEmpty() },
          recurrenceRule = finalRecurrenceRule)
    } else {
      Toast.makeText(context, R.string.error_check_input_data, Toast.LENGTH_SHORT).show()
    }
  }

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 4.dp, vertical = 0.dp), // Меньше отступы для иконки
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center) {
        AnimatedContent(
            targetState = currentSheetValue,
            transitionSpec = {
              (EnterTransition.None)
                  .togetherWith(ExitTransition.None)
                  .using(
                      SizeTransform(
                          clip =
                              false, // false - чтобы контент не обрезался во время анимации размера
                          sizeAnimationSpec = { _, _ ->
                            // Используем spring для более "живой" анимации размера
                            spring(
                                dampingRatio =
                                    Spring
                                        .DampingRatioLowBouncy, // Попробуйте LowBouncy или
                                                                // MediumBouncy
                                stiffness = Spring.StiffnessMediumLow // Попробуйте Medium или Low
                                )
                          }))
            },
            label = "SaveButtonAnimation") { targetSheetValue ->
              val expandedSize = ButtonDefaults.LargeContainerHeight
              val defaultSize = ButtonDefaults.MediumContainerHeight

              val isNotCompactState = targetSheetValue == SheetValue.Expanded

              val size = if (!isNotCompactState) defaultSize else expandedSize

              Button(
                  onClick = onSaveClick,
                  enabled = !isLoading,
                  modifier = Modifier.heightIn(size),
                  contentPadding = ButtonDefaults.contentPaddingFor(size)) {
                    if (isLoading) {
                      LoadingIndicator(
                          color = colorScheme.onPrimary,
                          modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)))
                    } else {
                      Icon(
                          imageVector = Icons.Filled.Check,
                          contentDescription =
                              "Сохранить", // Consider using stringResource here too
                          modifier =
                              Modifier.size(ButtonDefaults.iconSizeFor(size)) // Animated icon size
                          )
                    }
                  }
            }
      }

  // Основной контент с прокруткой
  Column(
      modifier =
          Modifier.verticalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 4.dp) // Горизонтальные отступы для контента
              .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    AdaptiveContainer {
      EventNameSection(
          summary = summary,
          summaryError = summaryError,
          onSummaryChange = { summary = it },
          onSummaryErrorChange = { summaryError = it },
          isLoading = isLoading)
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
          modifier = Modifier.fillMaxWidth())
    }
    validationError?.let { Text(it, color = colorScheme.error, style = typography.bodySmall) }

    AdaptiveContainer {
      OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text(stringResource(R.string.description)) },
          modifier = Modifier.fillMaxWidth().height(100.dp),
          maxLines = 4,
          enabled = !isLoading,
          shape = RoundedCornerShape(25.dp))
      OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          label = { Text(stringResource(R.string.location)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          enabled = !isLoading,
          shape = RoundedCornerShape(25.dp))
    }
    generalError?.let { Text(it, color = colorScheme.error, style = typography.bodyMedium) }
    Spacer(modifier = Modifier.height(16.dp)) // Отступ перед кнопкой сохранения
  } // End Scrollable Column

  // Кнопка сохранения внизу листа

  val currentDateTimeState = eventDateTimeState // Захватываем текущее состояние для лямбд

  // Диалог выбора Даты Начала
  if (showStartDatePicker) {
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                currentDateTimeState.startDate.atStartOfDay(systemZoneId).toInstant().toEpochMilli()
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
                  eventDateTimeState =
                      currentDateTimeState.copy(
                          startDate = selectedDate,
                          // Если дата конца стала раньше начала, подтягиваем ее
                          // Или если режим "Один день", дата конца = дате начала
                          endDate =
                              if (selectedDate.isAfter(currentDateTimeState.endDate) ||
                                  currentDateTimeState.startTime == null)
                                  selectedDate
                              else currentDateTimeState.endDate)
                }
                showStartDatePicker = false
              },
              enabled = datePickerState.selectedDateMillis != null) {
                Text("OK")
              }
        },
        dismissButton = {
          TextButton(onClick = { showStartDatePicker = false }) {
            Text(stringResource(R.string.cancel))
          }
        }) {
          DatePicker(state = datePickerState)
        }
  }
  // Диалог выбора Времени Начала
  if (showStartTimePicker) { // Компонент не запросит, если не нужно
    val initialTime =
        currentDateTimeState.startTime ?: LocalTime.now() // Безопасное значение по умолчанию
    val timePickerState =
        rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = DateFormat.is24HourFormat(context) // Учитываем настройку системы
            )
    TimePickerDialog( // Используем кастомную обертку
        onDismissRequest = { showStartTimePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                val selectedTime =
                    LocalTime.of(timePickerState.hour, timePickerState.minute).withNano(0)
                // Обновляем состояние через copy
                var newEndTime = currentDateTimeState.endTime
                // Если дата конца совпадает с датой начала и новое время начала >= времени конца,
                // сдвигаем время конца на час вперед
                if (currentDateTimeState.startDate == currentDateTimeState.endDate &&
                    currentDateTimeState.endTime != null &&
                    !selectedTime.isBefore(currentDateTimeState.endTime)) {
                  newEndTime = selectedTime.plusHours(1).withNano(0)
                }
                eventDateTimeState =
                    currentDateTimeState.copy(startTime = selectedTime, endTime = newEndTime)
                showStartTimePicker = false
              }) {
                Text("OK")
              }
        },
        dismissButton = {
          TextButton(onClick = { showStartTimePicker = false }) {
            Text(stringResource(R.string.cancel))
          }
        }) {
          TimePicker(state = timePickerState) // Вставляем сам пикер
        }
  }

  // Диалог выбора Даты Конца
  if (showEndDatePicker) { // Компонент не запросит, если не нужно
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                currentDateTimeState.endDate.atStartOfDay(systemZoneId).toInstant().toEpochMilli(),
            selectableDates =
                object : SelectableDates { // Ограничение выбора
                  val startMillis =
                      currentDateTimeState.startDate
                          .atStartOfDay(systemZoneId)
                          .toInstant()
                          .toEpochMilli()

                  override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= startMillis // Нельзя выбрать дату раньше даты начала
                  }

                  override fun isSelectableYear(year: Int): Boolean {
                    return year >= currentDateTimeState.startDate.year // Оптимизация для годов
                  }
                })
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
              enabled = datePickerState.selectedDateMillis != null) {
                Text("OK")
              }
        },
        dismissButton = {
          TextButton(onClick = { showEndDatePicker = false }) {
            Text(stringResource(R.string.cancel))
          }
        }) {
          DatePicker(state = datePickerState)
        }
  }

  // Диалог выбора Времени Конца
  if (showEndTimePicker) { // Компонент не запросит, если не нужно
    val initialTime =
        currentDateTimeState.endTime
            ?: currentDateTimeState.startTime?.plusHours(1)
            ?: LocalTime.now()
    val timePickerState =
        rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = DateFormat.is24HourFormat(context))
    TimePickerDialog(
        onDismissRequest = { showEndTimePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                val selectedTime =
                    LocalTime.of(timePickerState.hour, timePickerState.minute).withNano(0)

                var newFinalEndDate = currentDateTimeState.endDate

                // Проверка: если даты одинаковые, время конца не может быть раньше или равно
                // времени начала
                if (currentDateTimeState.startDate == currentDateTimeState.endDate &&
                    currentDateTimeState.startTime != null &&
                    selectedTime.isBefore(currentDateTimeState.startTime)) {
                  // Устанавливаем дату конца на следующий день
                  newFinalEndDate = currentDateTimeState.startDate.plusDays(1)
                  // Флаг isOneDay в EventDateTimePicker автоматически обновится,
                  // так как startDate и newFinalEndDate станут разными.
                  // Сообщение об ошибке здесь не нужно, так как мы исправляем ситуацию.
                }
                // В противном случае (многодневное событие или время выбрано корректно для
                // однодневного),
                // просто устанавливаем выбранное время.
                // Валидация (например, endDate < startDate или endTime < startTime на одной дате
                // для многодневного)
                // будет обработана в LaunchedEffect внутри EventDateTimePicker.

                eventDateTimeState =
                    currentDateTimeState.copy(
                        endTime = selectedTime,
                        endDate = newFinalEndDate // Обновляем и дату конца, если она изменилась
                        )
                showEndTimePicker = false
              }) {
                Text("OK")
              }
        },
        dismissButton = {
          TextButton(onClick = { showEndTimePicker = false }) {
            Text(stringResource(R.string.cancel))
          }
        }) {
          TimePicker(state = timePickerState)
        }
  }
  if (showRecurrenceEndDatePicker) {
    // Рассчитываем начальную дату для пикера
    //
    val initialSelectedDateMillis =
        eventDateTimeState.recurrenceEndDate
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
            ?: eventDateTimeState.startDate
                .plusMonths(1) // По умолчанию через месяц от старта
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedDateMillis,
            selectableDates =
                object : SelectableDates {
                  override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val selectedLocalDate =
                        Instant.ofEpochMilli(utcTimeMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    // Не раньше даты начала события
                    return !selectedLocalDate.isBefore(eventDateTimeState.startDate)
                  }

                  override fun isSelectableYear(year: Int): Boolean {
                    return year >= eventDateTimeState.startDate.year
                  }
                })
    DatePickerDialog(
        onDismissRequest = { showRecurrenceEndDatePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                  val selectedDate =
                      Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                  eventDateTimeState =
                      eventDateTimeState.copy(
                          recurrenceEndDate = selectedDate,
                          recurrenceEndType = RecurrenceEndType.DATE,
                          recurrenceCount = null)
                }
                showRecurrenceEndDatePicker = false
              },
              // Можно добавить enabled = datePickerState.selectedDateMillis != null
          ) {
            Text("OK")
          }
        },
        dismissButton = {
          TextButton(onClick = { showRecurrenceEndDatePicker = false }) {
            Text(stringResource(R.string.cancel))
          }
        }
        // --- ИСПОЛЬЗУЕМ ИМЕНОВАННЫЙ ПАРАМЕТР content ---
        ) { // Начало лямбды для content
          DatePicker(state = datePickerState) // Передаем DatePicker как контент
        } // Конец лямбды для content
  }
}
