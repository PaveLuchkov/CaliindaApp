package com.lpavs.caliinda.data.calendar

sealed interface UpdateEventResult {
    object Idle : UpdateEventResult
    object Loading : UpdateEventResult
    data class Success(val updatedEventId: String) : UpdateEventResult // Возвращаем ID, т.к. он может измениться
    data class Error(val message: String) : UpdateEventResult
}