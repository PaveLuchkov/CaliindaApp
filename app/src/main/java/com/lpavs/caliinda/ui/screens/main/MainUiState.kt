package com.lpavs.caliinda.ui.screens.main

import com.lpavs.caliinda.data.calendar.ClientEventUpdateMode

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
    val eventPendingDeletion: CalendarEvent? = null,
    val showDeleteConfirmationDialog: Boolean = false,
    val showRecurringDeleteOptionsDialog: Boolean = false,
    val deleteOperationError: String? = null,
    val eventBeingEdited: CalendarEvent? = null, // Событие, которое сейчас редактируется (оригинальные данные)
    val showRecurringEditOptionsDialog: Boolean = false, // Показать диалог выбора режима редактирования для повторяющихся
    val showEditEventDialog: Boolean = false, // Показать основной диалог/экран редактирования
    val selectedUpdateMode: ClientEventUpdateMode? = null,
    val editOperationError: String? = null, // Ошибка, специфичная для операции обновления
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
    val originalStartTime: String? = null, // Для измененных экземпляров, их оригинальное время начала
    val recurrenceRule: String? = null
)