package com.lpavs.caliinda.data.calendar

sealed interface EventNetworkState {
  object Idle : EventNetworkState

  object Loading : EventNetworkState

  data class Error(val message: String) : EventNetworkState
}
