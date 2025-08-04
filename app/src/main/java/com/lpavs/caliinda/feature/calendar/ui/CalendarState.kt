package com.lpavs.caliinda.feature.calendar.ui

import android.app.PendingIntent
import android.net.Uri
import com.lpavs.caliinda.core.data.remote.EventUpdateMode
import com.lpavs.caliinda.core.data.remote.dto.EventDto

data class CalendarState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val displayName: String? = null,
    val photo: Uri? = null,
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val message: String? = "Требуется вход.",
    val showSignInRequiredDialog: Boolean = false,
    val authorizationIntent: PendingIntent? = null,
)