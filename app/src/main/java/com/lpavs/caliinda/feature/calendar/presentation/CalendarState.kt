package com.lpavs.caliinda.feature.calendar.presentation

import com.lpavs.caliinda.feature.calendar.data.EventDetailsUiModel

data class CalendarState(
    val hasCalendarPermission: Boolean = false,
    /** Событие, открытое в диалоге подробностей; null — диалог закрыт. */
    val eventDetails: EventDetailsUiModel? = null,
)

