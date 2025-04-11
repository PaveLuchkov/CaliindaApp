package com.example.caliindar.ui.screens.main

import com.example.caliindar.data.model.ChatMessage

// Переносим MainUiState в свой файл
data class MainUiState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val isLoading: Boolean = false,
    val isRecording: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val message: String = "Требуется вход.", // Оставляем для общих статусов/начального сообщения
    val showGeneralError: String? = null,
    val showAuthError: String? = null,
)
