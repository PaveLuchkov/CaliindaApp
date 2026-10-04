package com.lpavs.caliinda.feature.event_management.ui.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.feature.event_management.ui.shared.EventFormContent
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.EventDateTimeState
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions.SuggestionsViewModel
import com.lpavs.caliinda.feature.event_management.vm.EventManagementUiEvent
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
    userTimeZone: String,
    initialDate: LocalDate,
    initialProject: Boolean = false,
    onDismiss: () -> Unit,
) {
  // Состояние полей
  var summary by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }

  // Ошибки
  var summaryError by remember { mutableStateOf<String?>(null) }
  var validationError by remember { mutableStateOf<String?>(null) }
  var generalError by remember { mutableStateOf<String?>(null) }

  val userTimeZoneId = remember { ZoneId.of(userTimeZone) }
  val uiState by viewModel.uiState.collectAsState()

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
    val now = LocalDateTime.now(userTimeZoneId)
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

  // Обработка событий ViewModel
  LaunchedEffect(Unit) {
    viewModel.eventFlow.collect { event ->
      if (event is EventManagementUiEvent.OperationSuccess) onDismiss()
    }
  }

  // Обновление контекста для чипсов-подсказок
  LaunchedEffect(eventDateTimeState.startTime, eventDateTimeState.isAllDay) {
    suggestionsViewModel.updateSortContext(
        eventDateTimeState.startTime, eventDateTimeState.isAllDay)
  }
  val suggestedChips by suggestionsViewModel.suggestionChips.collectAsStateWithLifecycle()

  Column(modifier = Modifier.fillMaxSize()) {
    // Кнопка "Сохранить" (показывается, когда введено название)
    AnimatedContent(targetState = summary.isNotEmpty()) { isNotEmpty ->
      if (isNotEmpty) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
              Button(
                  onClick = {
                    viewModel.createEvent(
                        summary = summary,
                        description = description,
                        location = location,
                        dateTimeState = eventDateTimeState)
                  },
                  enabled = !uiState.isLoading,
                  modifier = Modifier.fillMaxWidth().padding(cuid.ContainerPadding),
              ) {
                if (uiState.isLoading) {
                  LoadingIndicator(
                      color = colorScheme.onPrimary,
                      modifier = Modifier.size(ButtonDefaults.iconSizeFor(30.dp)))
                } else {
                  Text(text = stringResource(R.string.save))
                }
              }
            }
      }
    }

    // Вся форма вынесена в отдельный компонент
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
        onDateTimeStateChange = {
          eventDateTimeState = it
          validationError = null
        },
        isLoading = uiState.isLoading,
        suggestedChips = suggestedChips)

    // Вывод ошибок валидации
    validationError?.let {
      Text(it, color = colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
    }
    generalError?.let {
      Text(it, color = colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
    }
  }
}
