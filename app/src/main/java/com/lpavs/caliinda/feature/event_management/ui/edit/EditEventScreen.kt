package com.lpavs.caliinda.feature.event_management.ui.edit

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
import com.lpavs.caliinda.core.data.calendar.model.EventUpdateMode
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.feature.event_management.ui.shared.EventFormContent
import com.lpavs.caliinda.feature.event_management.ui.shared.SaveBar
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions.SuggestionsViewModel
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditEventScreen(
    viewModel: EventManagementViewModel,
    suggestionsViewModel: SuggestionsViewModel = hiltViewModel(),
    eventToEdit: EventDto,
    selectedUpdateMode: EventUpdateMode,
) {
  // Состояние данных
  // Подпись «(Без названия)» — только для показа, в поле её подставлять нельзя: сохранится как
  // настоящее название.
  var summary by
      remember(eventToEdit.id) {
        mutableStateOf(if (eventToEdit.isUntitled) "" else eventToEdit.summary)
      }
  var description by remember(eventToEdit.id) { mutableStateOf(eventToEdit.description ?: "") }
  var location by remember(eventToEdit.id) { mutableStateOf(eventToEdit.location ?: "") }

  // Состояние ошибок и валидации
  var summaryError by remember { mutableStateOf<String?>(null) }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  // Вспомогательные переменные
  val initialEventDateTimeState =
      remember(eventToEdit.id) { viewModel.parseEventToState(eventToEdit) }
  var eventDateTimeState by remember(eventToEdit.id) { mutableStateOf(initialEventDateTimeState) }

  // Подписки на события
  val suggestedChips by suggestionsViewModel.suggestionChips.collectAsStateWithLifecycle()

  LaunchedEffect(eventDateTimeState.startTime) {
    suggestionsViewModel.updateSortContext(
        eventDateTimeState.startTime, eventDateTimeState.isAllDay)
  }

  val save = {
    viewModel.updateEvent(
        summary = summary,
        description = description,
        location = location,
        dateTimeState = eventDateTimeState,
        updateMode = selectedUpdateMode)
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
        modifier = Modifier.weight(1f, fill = false))
    SaveBar(enabled = summary.isNotBlank(), isLoading = uiState.isLoading, onSave = save)
  }
}
