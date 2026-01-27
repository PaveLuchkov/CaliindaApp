package com.lpavs.caliinda.feature.event_management.ui.shared

sealed class ActivePicker {
    object None : ActivePicker()
    object StartDate : ActivePicker()
    object StartTime : ActivePicker()
    object EndDate : ActivePicker()
    object EndTime : ActivePicker()
    object RecurrenceEnd : ActivePicker()
}
