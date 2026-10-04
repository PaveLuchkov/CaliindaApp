package com.lpavs.caliinda.core.data.calendar.model

import java.time.LocalDate

/** Данные месяца для экрана проектов. */
data class MonthEvents(
    /** Многодневные события, пересекающие месяц. */
    val projects: List<EventDto>,
    /** Сколько обычных (не многодневных) событий начинается в каждый день месяца. */
    val eventsPerDay: Map<LocalDate, Int>,
)
