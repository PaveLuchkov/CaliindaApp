package com.lpavs.caliinda.feature.calendar.data.onEventResults

sealed interface CreateEventResult {
  object Idle : CreateEventResult

  object Loading : CreateEventResult

  object Success : CreateEventResult

  data class Error(val message: String) : CreateEventResult
}
