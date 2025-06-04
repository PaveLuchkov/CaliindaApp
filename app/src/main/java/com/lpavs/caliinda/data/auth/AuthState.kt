package com.lpavs.caliinda.data.auth

import android.net.Uri

data class AuthState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val authError: String? = null,
    val isLoading: Boolean = false,
    val displayName: String? = null,
    val photoUrl: Uri? = null
)
