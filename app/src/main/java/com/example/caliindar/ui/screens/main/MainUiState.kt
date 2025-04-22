package com.example.caliindar.ui.screens.main

// Переносим MainUiState в свой файл
data class MainUiState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val message: String? = "Требуется вход.", // Оставляем для общих статусов/начального сообщения
    val showGeneralError: String? = null,
    val showAuthError: String? = null,
)

data class CalendarEvent(
    val id: String,
    val summary: String, // Название события
    val startTime: String, // ISO 8601 строка (e.g., "2023-10-27T10:00:00+03:00")
    val endTime: String, // ISO 8601 строка
    val description: String? = null,
    val location: String? = null,
    val isAllDay: Boolean = false
    // Добавьте другие поля при необходимости (например, link)
)