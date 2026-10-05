package com.lpavs.caliinda.feature.event_management.ui.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.feature.event_management.ui.shared.EventFormContent
import com.lpavs.caliinda.feature.event_management.ui.shared.SaveBar
import androidx.compose.ui.focus.FocusRequester
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimeState
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions.SuggestionsViewModel
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateEventScreen(
    viewModel: EventManagementViewModel = hiltViewModel(),
    suggestionsViewModel: SuggestionsViewModel = hiltViewModel(),
    userTimeZone: ZoneId,
    initialDate: LocalDate,
    initialProject: Boolean = false,
    /** Шторка встала на место — можно звать клавиатуру, не сбивая анимацию появления. */
    sheetSettled: Boolean = true,
) {
  // Состояние полей
  var summary by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }

  // Ошибки
  var summaryError by remember { mutableStateOf<String?>(null) }

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  // Начальное состояние даты и времени
  var eventDateTimeState by remember {
    if (initialProject) {
      // Проект: весь день, сразу промежуток из нескольких дней.
      return@remember mutableStateOf(
          EventDateTimeState(
              startDate = initialDate,
              startTime = null,
              endDate = initialDate.plusDays(1),
              endTime = null,
              isAllDay = true,
              isRecurring = false))
    }
    // Начало — следующий ровный час. Для сегодняшнего дня считаем с датой: в 23:30 это уже
    // 00:00 завтрашнего дня, а не прошедшая полночь сегодняшнего.
    val now = LocalDateTime.now(userTimeZone)
    val nextHour = now.plusHours(1).truncatedTo(ChronoUnit.HOURS)
    val defaultStart =
        if (initialDate == now.toLocalDate()) nextHour
        else initialDate.atTime(nextHour.toLocalTime())
    val defaultEnd = defaultStart.plusHours(1)

    mutableStateOf(
        EventDateTimeState(
            startDate = defaultStart.toLocalDate(),
            startTime = defaultStart.toLocalTime(),
            endDate = defaultEnd.toLocalDate(),
            endTime = defaultEnd.toLocalTime(),
            isAllDay = false,
            isRecurring = false))
  }

  // Обновление контекста для чипсов-подсказок
  LaunchedEffect(eventDateTimeState.startTime, eventDateTimeState.isAllDay) {
    suggestionsViewModel.updateSortContext(
        eventDateTimeState.startTime, eventDateTimeState.isAllDay)
  }
  val suggestedChips by suggestionsViewModel.suggestionChips.collectAsStateWithLifecycle()

  val save = {
    viewModel.createEvent(
        summary = summary,
        description = description,
        location = location,
        dateTimeState = eventDateTimeState)
  }
  // Сразу клавиатура в названии: типичное событие — это название и время.
  val nameFocusRequester = remember { FocusRequester() }
  LaunchedEffect(sheetSettled) {
    if (sheetSettled) runCatching { nameFocusRequester.requestFocus() }
  }

  // Шторка по содержимому: кнопка сразу под формой, без пустоты до низа экрана. Если форма
  // не влезает (клавиатура), она прокручивается, а кнопка остаётся видна.
  Column(modifier = Modifier.fillMaxWidth()) {
    EventFormContent(
        summary = summary,
        onSummaryChange = { summary = it },
        summaryError = summaryError,
        onSummaryErrorChange = { summaryError = it },
        description = description,
        onDescriptionChange = { description = it },
        location = location,
        onLocationChange = { location = it },
        dateTimeState = eventDateTimeState,
        onDateTimeStateChange = { eventDateTimeState = it },
        isLoading = uiState.isLoading,
        suggestedChips = suggestedChips,
        onSave = save,
        nameFocusRequester = nameFocusRequester,
        modifier = Modifier.weight(1f, fill = false))
    SaveBar(enabled = summary.isNotBlank(), isLoading = uiState.isLoading, onSave = save)
  }
}
