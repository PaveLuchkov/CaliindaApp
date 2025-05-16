package com.example.caliindar.ui.screens.main

// Переносим MainUiState в свой файл
data class MainUiState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val message: String? = "Требуется вход.",
    val showGeneralError: String? = null,
    val showAuthError: String? = null,
    val displayName: String? = null,
    val eventToDeleteId: String? = null,
    val showDeleteConfirmationDialog: Boolean = false,
    val deleteOperationError: String? = null
)

data class CalendarEvent(
    val id: String,
    val summary: String,
    val startTime: String?,
    val endTime: String?,
    val description: String? = null,
    val location: String? = null,
    val isAllDay: Boolean = false
)