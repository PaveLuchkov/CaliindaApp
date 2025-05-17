package com.example.caliinda.data.calendar

sealed interface CreateEventResult {
    object Idle : CreateEventResult
    object Loading : CreateEventResult
    object Success : CreateEventResult
    data class Error(val message: String) : CreateEventResult
}