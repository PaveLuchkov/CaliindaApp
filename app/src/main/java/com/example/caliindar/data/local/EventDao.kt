package com.example.caliindar.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow


@Dao
interface EventDao {

    /**
     * Получает поток списка событий, время начала которых попадает
     * в указанный диапазон миллисекунд UTC.
     * Возвращает Flow для автоматического обновления UI.
     * @param startRangeMillis Начало диапазона (включительно)
     * @param endRangeMillis Конец диапазона (не включительно)
     */
    @Query("SELECT * FROM calendar_events WHERE startTimeMillis >= :startRangeMillis AND startTimeMillis < :endRangeMillis ORDER BY startTimeMillis ASC")
    fun getEventsForDateRangeFlow(startRangeMillis: Long, endRangeMillis: Long): Flow<List<CalendarEventEntity>>

    /**
     * Вставляет список событий. Если событие с таким же ID уже существует,
     * оно будет заменено.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceEvents(events: List<CalendarEventEntity>)

    /**
     * Удаляет все события, время начала которых попадает в указанный диапазон.
     * Используется перед вставкой свежих данных для этого диапазона,
     * чтобы удалить события, которых больше нет на сервере.
     * @param startRangeMillis Начало диапазона (включительно)
     * @param endRangeMillis Конец диапазона (не включительно)
     */
    @Query("DELETE FROM calendar_events WHERE startTimeMillis >= :startRangeMillis AND startTimeMillis < :endRangeMillis")
    suspend fun deleteEventsForDateRange(startRangeMillis: Long, endRangeMillis: Long)

    /**
     * Атомарно удаляет события в диапазоне и вставляет новые.
     * Гарантирует, что операции выполнятся вместе.
     */
    @Transaction
    suspend fun clearAndInsertEventsForRange(
        startRangeMillis: Long,
        endRangeMillis: Long,
        newEvents: List<CalendarEventEntity>
    ) {
        // Удаляем только те события, которые начинаются в этом диапазоне
        deleteEventsForDateRange(startRangeMillis, endRangeMillis)
        // Вставляем новые (или обновляем существующие, если их ID совпадут, благодаря REPLACE)
        insertOrReplaceEvents(newEvents)
    }

    // Дополнительно (если нужно):
    // @Query("DELETE FROM calendar_events")
    // suspend fun deleteAllEvents()
}