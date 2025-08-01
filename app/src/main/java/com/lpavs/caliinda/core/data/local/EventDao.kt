package com.lpavs.caliinda.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lpavs.caliinda.data.local.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

  /**
   * Получает поток списка событий, время начала которых попадает в указанный диапазон миллисекунд
   * UTC. Возвращает Flow для автоматического обновления UI.
   *
   * @param startRangeMillis Начало диапазона (включительно)
   * @param endRangeMillis Конец диапазона (не включительно)
   */
  @Query(
      "SELECT * FROM calendar_events WHERE startTimeMillis >= :startRangeMillis AND startTimeMillis < :endRangeMillis ORDER BY startTimeMillis ASC")
  fun getEventsForDateRangeFlow(
      startRangeMillis: Long,
      endRangeMillis: Long
  ): Flow<List<CalendarEventEntity>>

  /** Вставляет список событий. Если событие с таким же ID уже существует, оно будет заменено. */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrReplaceEvents(events: List<CalendarEventEntity>)

  /**
   * Удаляет все события, время начала которых попадает в указанный диапазон. Используется перед
   * вставкой свежих данных для этого диапазона, чтобы удалить события, которых больше нет на
   * сервере.
   *
   * @param startRangeMillis Начало диапазона (включительно)
   * @param endRangeMillis Конец диапазона (не включительно)
   */
  @Query(
      "DELETE FROM calendar_events WHERE startTimeMillis >= :startRangeMillis AND startTimeMillis < :endRangeMillis")
  suspend fun deleteEventsForDateRange(startRangeMillis: Long, endRangeMillis: Long)

  /**
   * Атомарно удаляет события в диапазоне и вставляет новые. Гарантирует, что операции выполнятся
   * вместе.
   */
  @Transaction
  suspend fun clearAndInsertEventsForRange(
      startRangeMillis: Long,
      endRangeMillis: Long,
      newEvents: List<CalendarEventEntity>
  ) {
      deleteEventsForDateRange(startRangeMillis, endRangeMillis)
      insertOrReplaceEvents(newEvents)
  }

  /**
   * Удаляет событие из локальной базы данных по его уникальному ID.
   *
   * @param eventId Уникальный идентификатор события (обычно строка, совпадающая с Google Calendar
   *   event ID).
   */
  @Query(
      "DELETE FROM calendar_events WHERE id = :eventId")
  suspend fun deleteEventById(eventId: String)

    /**
     * Удаляет все данные календаря из локальной базы данных.
     */
    @Query("DELETE FROM calendar_events")
    suspend fun deleteAllEvents()
}
