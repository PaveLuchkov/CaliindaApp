package com.lpavs.caliinda.data.calendar

sealed interface DeleteEventResult {
  object Idle : DeleteEventResult

  object Loading : DeleteEventResult

  object Success : DeleteEventResult

  data class Error(val message: String) : DeleteEventResult
}
