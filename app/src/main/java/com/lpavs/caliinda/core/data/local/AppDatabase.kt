package com.lpavs.caliinda.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lpavs.caliinda.data.local.CalendarEventEntity

@Database(
    entities = [CalendarEventEntity::class],
    version = 4, // Увеличивай версию при изменении схемы
    exportSchema = false // Установи true и укажи путь для схем в production
    )
abstract class AppDatabase : RoomDatabase() {
  abstract fun eventDao(): CalendarLocalDataSource
}
