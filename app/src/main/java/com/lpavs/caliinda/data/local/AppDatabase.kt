package com.lpavs.caliinda.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CalendarEventEntity::class],
    version = 3, // Увеличивай версию при изменении схемы
    exportSchema = false // Установи true и укажи путь для схем в production
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}