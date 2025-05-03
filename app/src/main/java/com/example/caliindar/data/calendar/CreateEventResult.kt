package com.example.caliindar.data.calendar

sealed interface CreateEventResult {
    object Idle : CreateEventResult
    object Loading : CreateEventResult
    object Success : CreateEventResult
    data class Error(val message: String) : CreateEventResult
}