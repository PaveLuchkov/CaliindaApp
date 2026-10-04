package com.lpavs.caliinda.feature.event_management.ui.edit

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
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.SheetValue
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
import com.lpavs.caliinda.core.data.calendar.model.EventUpdateMode
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.feature.event_management.ui.shared.EventFormContent
import com.lpavs.caliinda.feature.event_management.ui.shared.sections.suggestions.SuggestionsViewModel
import com.lpavs.caliinda.feature.event_management.vm.EventManagementUiEvent
import com.lpavs.caliinda.feature.event_management.vm.EventManagementViewModel
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditEventScreen(
    viewModel: EventManagementViewModel,
    suggestionsViewModel: SuggestionsViewModel = hiltViewModel(),
    userTimeZone: String,
    eventToEdit: EventDto,
    selectedUpdateMode: EventUpdateMode,
    onDismiss: () -> Unit,
    currentSheetValue: SheetValue,
) {
  // Состояние данных
  var summary by remember(eventToEdit.id) { mutableStateOf(eventToEdit.summary) }
  var description by remember(eventToEdit.id) { mutableStateOf(eventToEdit.description ?: "") }
  var location by remember(eventToEdit.id) { mutableStateOf(eventToEdit.location ?: "") }

  // Состояние ошибок и валидации
  var summaryError by remember { mutableStateOf<String?>(null) }
  var validationError by remember { mutableStateOf<String?>(null) }
  val uiState by viewModel.uiState.collectAsState()

  // Вспомогательные переменные
  val userTimeZoneId = remember { ZoneId.of(userTimeZone) }
  val initialEventDateTimeState =
      remember(eventToEdit.id) { viewModel.parseEventToState(eventToEdit) }
  var eventDateTimeState by remember(eventToEdit.id) { mutableStateOf(initialEventDateTimeState) }

  // Подписки на события
  val suggestedChips by suggestionsViewModel.suggestionChips.collectAsStateWithLifecycle()

  LaunchedEffect(key1 = true) {
    viewModel.eventFlow.collect { event ->
      if (event is EventManagementUiEvent.OperationSuccess) onDismiss()
    }
  }

  LaunchedEffect(eventDateTimeState.startTime) {
    suggestionsViewModel.updateSortContext(
        eventDateTimeState.startTime, eventDateTimeState.isAllDay)
  }

  // UI
  Column(modifier = Modifier.fillMaxSize()) {
    // Кнопка сохранения в верхней части (как в вашем коде)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center) {
          Button(
              onClick = {
                viewModel.updateEvent(
                    summary = summary,
                    description = description,
                    location = location,
                    dateTimeState = eventDateTimeState,
                    updateMode = selectedUpdateMode)
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

    // Основная форма
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
        userTimeZoneId = userTimeZoneId,
        isLoading = uiState.isLoading,
        suggestedChips = suggestedChips)

    validationError?.let {
      Text(
          text = it,
          color = colorScheme.error,
          style = typography.bodySmall,
          modifier = Modifier.padding(horizontal = 16.dp))
    }
  }
}
