package com.example.caliindar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CalendarEventEntity::class],
    version = 2, // Увеличивай версию при изменении схемы
    exportSchema = false // Установи true и укажи путь для схем в production
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}

// ВАЖНО: Так как мы используем fallbackToDestructiveMigration,
// старая база данных будет удалена при обновлении. Для продакшена
// нужно будет написать реальную миграцию (ALTER TABLE ... ADD COLUMN ...).