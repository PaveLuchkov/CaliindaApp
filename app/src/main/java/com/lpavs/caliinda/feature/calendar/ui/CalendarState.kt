package com.lpavs.caliinda.feature.calendar.ui

import android.app.PendingIntent
import android.net.Uri
import com.lpavs.caliinda.core.data.remote.EventUpdateMode
import com.lpavs.caliinda.feature.calendar.data.model.CalendarEvent

data class CalendarState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val displayName: String? = null,
    val photo: Uri? = null,
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val message: String? = "Требуется вход.",
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
    val selectedUpdateMode: EventUpdateMode? = null,
    val editOperationError: String? = null,
    val eventForDetailedView: CalendarEvent? = null,
    val showEventDetailedView: Boolean = false,
    val showSignInRequiredDialog: Boolean = false,
    val authorizationIntent: PendingIntent? = null,
)
