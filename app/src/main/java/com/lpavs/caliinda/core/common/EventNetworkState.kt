package com.lpavs.caliinda.core.common

sealed interface EventNetworkState {
  object Idle : EventNetworkState

  object Loading : EventNetworkState

  data class Error(val message: String) : EventNetworkState
}