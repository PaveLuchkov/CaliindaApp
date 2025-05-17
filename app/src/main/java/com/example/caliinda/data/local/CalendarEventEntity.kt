package com.example.caliinda.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String, // Google Calendar Event ID (естественный ключ)
    val summary: String,
    val startTimeMillis: Long, // Время начала в миллисекундах UTC
    val endTimeMillis: Long,   // Время конца в миллисекундах UTC
    val description: String?,
    val location: String?,
    val isAllDay: Boolean = false,
    val recurringEventId: String? = null, // Новое поле
    val originalStartTimeString: String? = null, // Новое поле (храним как строку)
    // Опционально: Можно добавить userGoogleId, если планируешь мультиюзерность в одной БД
    // val userGoogleId: String,
    // Опционально: Можно добавить время последнего обновления из сети
    val lastFetchedMillis: Long = System.currentTimeMillis()
)