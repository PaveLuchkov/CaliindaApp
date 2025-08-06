package com.lpavs.caliinda.feature.calendar.ui

import android.app.PendingIntent
import android.net.Uri

data class CalendarState(
    val isSignedIn: Boolean = false,
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val message: String? = "Требуется вход.",
    val showSignInRequiredDialog: Boolean = false,
    val authorizationIntent: PendingIntent? = null,
)
