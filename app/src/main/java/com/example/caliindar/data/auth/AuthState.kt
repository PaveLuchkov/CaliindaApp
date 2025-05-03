package com.example.caliindar.data.auth

data class AuthState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val authError: String? = null,
    val isLoading: Boolean = false,
    val displayName: String? = null
)