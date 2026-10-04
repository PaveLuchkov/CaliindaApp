package com.lpavs.caliinda.feature.widget

import com.lpavs.caliinda.core.data.calendar.CalendarPermissionManager
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Что показывает виджет в данный момент. */
data class WidgetData(
    val hasAccess: Boolean,
    val today: LocalDate,
    val zone: ZoneId,
    /** Идущее сейчас событие, а если его нет — ближайшее следующее. */
    val focus: EventDto?,
    val focusIsNow: Boolean,
    /** Когда виджету стоит обновиться самому: начало или конец ближайшего события, полночь. */
    val nextChange: Instant,
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
  fun calendarRepository(): CalendarRepository

  fun settingsRepository(): SettingsRepository

  fun permissionManager(): CalendarPermissionManager
}

suspend fun WidgetEntryPoint.loadWidgetData(now: Instant = Instant.now()): WidgetData {
  // Виджет живёт и без открытого приложения — разрешение могли выдать или забрать с тех пор.
  permissionManager().refresh()
  val zone =
      settingsRepository().timeZoneFlow.first().let {
        runCatching { ZoneId.of(it.ifEmpty { ZoneId.systemDefault().id }) }
            .getOrDefault(ZoneId.systemDefault())
      }
  val today = now.atZone(zone).toLocalDate()
  val midnight = today.plusDays(1).atStartOfDay(zone).toInstant()

  if (!permissionManager().isGranted.value) {
    return WidgetData(false, today, zone, null, false, midnight)
  }

  val timed =
      calendarRepository()
          .getEventsFlowForDate(today)
          .first()
          .filter { !it.isAllDay && it.endTime.isAfter(now) }
          .sortedBy { it.startTime }
  val focus = timed.firstOrNull()
  val nextChange =
      timed
          .flatMap { listOf(it.startTime, it.endTime) }
          .filter { it.isAfter(now) }
          .minOrNull()
          ?.coerceAtMost(midnight) ?: midnight

  return WidgetData(
      hasAccess = true,
      today = today,
      zone = zone,
      focus = focus,
      focusIsNow = focus != null && !focus.startTime.isAfter(now),
      nextChange = nextChange)
}
