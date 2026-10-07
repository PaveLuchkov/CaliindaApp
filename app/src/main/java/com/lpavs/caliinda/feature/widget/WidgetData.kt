package com.lpavs.caliinda.feature.widget

import com.lpavs.caliinda.core.data.calendar.CalendarPermissionManager
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import com.lpavs.caliinda.core.data.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.lpavs.caliinda.feature.calendar.presentation.model.dateRange
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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
    /** Идущий сегодня проект — тот, что закончится раньше других. */
    val project: WidgetProject? = null,
    /** Событие после [focus] — для строки «потом». */
    val afterFocus: EventDto? = null,
)

/** Проект в виджете: «день [day] из [totalDays]». */
data class WidgetProject(val event: EventDto, val day: Int, val totalDays: Int) {
  val progress: Float
    get() = day.toFloat() / totalDays
}

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
  val zone = settingsRepository().zoneFlow.first()
  val today = now.atZone(zone).toLocalDate()
  val midnight = today.plusDays(1).atStartOfDay(zone).toInstant()

  if (!permissionManager().isGranted.value) {
    return WidgetData(false, today, zone, null, false, midnight)
  }

  val dayEvents = calendarRepository().getDayFlow(today).first()
  val timed =
      dayEvents.events
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
      nextChange = nextChange,
      afterFocus = timed.getOrNull(1),
      project =
          dayEvents.projects
              .minByOrNull { it.endTime }
              ?.let {
                val range = it.dateRange(zone)
                WidgetProject(
                    event = it,
                    day = ChronoUnit.DAYS.between(range.start, today).toInt() + 1,
                    totalDays = ChronoUnit.DAYS.between(range.start, range.endInclusive).toInt() + 1)
              })
}
