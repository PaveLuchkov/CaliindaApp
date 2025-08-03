package com.lpavs.caliinda.feature.calendar.data.onEventResults

sealed interface DeleteEventResult {
  object Idle : DeleteEventResult

  object Loading : DeleteEventResult

  object Success : DeleteEventResult

  data class Error(val message: String) : DeleteEventResult
}
