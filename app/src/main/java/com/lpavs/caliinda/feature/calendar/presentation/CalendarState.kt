package com.lpavs.caliinda.feature.calendar.presentation

import com.lpavs.caliinda.core.data.remote.EventUpdateMode
import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.feature.calendar.data.EventDetailsUiModel
import java.time.LocalDate

data class CalendarState(
    val isSignedIn: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = "Требуется вход.",
    val showSignInRequiredDialog: Boolean = false,
    val eventForDetailedView: EventDetailsUiModel? = null,
    val showEventDetailedView: Boolean = false,
)

sealed interface SheetState {
  data object Hidden : SheetState

  data object DatePicker : SheetState

  data class CreateEvent(val initialDate: LocalDate) : SheetState

  data class EditEvent(val event: EventDto, val mode: EventUpdateMode) : SheetState // Пример

  data class EventDetails(val event: EventDto) : SheetState

  data class ConfirmDelete(val event: EventDto) : SheetState

  data class ConfirmRecurringEdit(val event: EventDto) : SheetState

  data class ConfirmRecurringDelete(val event: EventDto) : SheetState

  data object LoginRequired : SheetState
}

// События, которые UI отправляет в ViewModel
sealed interface CalendarScreenEvent {
  // Управление датой
  data class PageChanged(val newDate: LocalDate) : CalendarScreenEvent

  data object GoToTodayClicked : CalendarScreenEvent

  data object AppBarTitleClicked : CalendarScreenEvent

  data class DateFromPickerSelected(val date: LocalDate) : CalendarScreenEvent

  // Управление окнами
  data object DismissSheet : CalendarScreenEvent

  data class CreateEventClicked(val onDate: LocalDate) : CalendarScreenEvent

  // ... и так далее для каждого действия пользователя
}
