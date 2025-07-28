package com.lpavs.caliinda.ui.screens.main

import android.app.PendingIntent
import android.net.Uri
import com.lpavs.caliinda.data.calendar.ClientEventUpdateMode

// Переносим MainUiState в свой файл
data class MainUiState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val displayName: String? = null,
    val photo: Uri? = null,
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val message: String? = "Требуется вход.", // ?? TODO исправить
    val showGeneralError: String? = null,
    val showAuthError: String? = null,
    val eventToDeleteId: String? = null,
    val eventPendingDeletion: CalendarEvent? = null,
    val showDeleteConfirmationDialog: Boolean = false,
    val showRecurringDeleteOptionsDialog: Boolean = false,
    val deleteOperationError: String? = null,
    val eventBeingEdited: CalendarEvent? = null,
    val showRecurringEditOptionsDialog: Boolean = false,
    val showEditEventDialog: Boolean = false,
    val selectedUpdateMode: ClientEventUpdateMode? = null,
    val editOperationError: String? = null,
    val eventForDetailedView: CalendarEvent? = null,
    val showEventDetailedView: Boolean = false,
    val showSignInRequiredDialog: Boolean = false,
    val authorizationIntent: PendingIntent? = null,
)

data class CalendarEvent(
    val id: String,
    val summary: String,
    val startTime: String?,
    val endTime: String?,
    val description: String? = null,
    val location: String? = null,
    val isAllDay: Boolean = false,
    val recurringEventId: String? = null, // ID "мастер-события", если это экземпляр
    val originalStartTime: String? =
        null, // Для измененных экземпляров, их оригинальное время начала
    val recurrenceRule: String? = null
)
