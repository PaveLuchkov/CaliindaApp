package com.lpavs.caliinda.core.data.repository

import java.time.YearMonth
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.lpavs.caliinda.R
import android.content.ContentValues
import android.provider.CalendarContract.Events
import android.util.Log
import com.lpavs.caliinda.app.di.IoDispatcher
import com.lpavs.caliinda.core.data.calendar.CalendarPermissionManager
import com.lpavs.caliinda.core.data.calendar.CalendarProviderDataSource
import com.lpavs.caliinda.core.data.calendar.EventRow
import com.lpavs.caliinda.core.data.calendar.InstanceRow
import com.lpavs.caliinda.core.data.calendar.model.DeviceCalendar
import com.lpavs.caliinda.core.data.calendar.model.EventDeleteMode
import com.lpavs.caliinda.core.data.calendar.model.EventDraft
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.calendar.model.EventUpdateMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CalendarRepository
@Inject
constructor(
    private val dataSource: CalendarProviderDataSource,
    private val permissionManager: CalendarPermissionManager,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

  /** Эмитит true/false (есть ли доступ) при старте и при каждом изменении данных календаря. */
  private val dataChanges: Flow<Boolean> =
      permissionManager.isGranted
          .flatMapLatest { granted ->
            if (granted) dataSource.observeChanges().map { true } else flowOf(false)
          }
          .shareIn(scope, SharingStarted.WhileSubscribed(5000), replay = 1)

  private fun <T> observeCalendar(load: (ZoneId) -> T, empty: T): Flow<T> =
      combine(dataChanges, settingsRepository.zoneFlow) { granted, zone -> granted to zone }
          .mapLatest { (granted, zone) -> if (granted) load(zone) else empty }
          .catch { e ->
            Log.e(TAG, "Error reading calendar", e)
            emit(empty)
          }
          .flowOn(ioDispatcher)

  // --- Чтение ---

  /** События дня: обычные (< 24ч) и однодневные all-day. */
  fun getEventsFlowForDate(date: LocalDate): Flow<List<EventDto>> =
      observeCalendar(
          load = { zone ->
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            loadInstances(dayStart, dayEnd, zone)
                .filter { inst ->
                  if (inst.row.isAllDay) {
                    inst.allDayStart == date && inst.allDayEndExclusive == date.plusDays(1)
                  } else {
                    inst.endMillis > dayStart &&
                        inst.startMillis < dayEnd &&
                        inst.endMillis - inst.startMillis < DAY_MILLIS
                  }
                }
                .map { it.toDto(zone) }
          },
          empty = emptyList())

  /** "Проекты": многодневные события, которые идут в указанную дату. */
  fun getEventsFlowForProjects(date: LocalDate): Flow<List<EventDto>> =
      observeCalendar(
          load = { zone ->
            // Идущие сейчас и предстоящие: иначе проект, начинающийся завтра, нигде не виден.
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val windowEnd =
                date.plusDays(PROJECTS_LOOKAHEAD_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
            loadInstances(dayStart, windowEnd, zone)
                .filter { inst -> inst.overlaps(dayStart, windowEnd) && inst.isProject() }
                .map { it.toDto(zone) }
          },
          empty = emptyList())

  /** Проекты, которые идут в указанный день, — для лент на странице дня. */
  fun getProjectsFlowForDate(date: LocalDate): Flow<List<EventDto>> =
      observeCalendar(
          load = { zone ->
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            loadInstances(dayStart, dayEnd, zone)
                .filter { inst -> inst.overlaps(dayStart, dayEnd) && inst.isProject() }
                .sortedBy { it.startMillis }
                .map { it.toDto(zone) }
          },
          empty = emptyList())

  /** Многодневные события, пересекающие месяц (в том числе уже прошедшие) — страница месяца. */
  fun getMonthProjectsFlow(month: YearMonth): Flow<List<EventDto>> =
      observeCalendar(
          load = { zone ->
            val first = month.atDay(1)
            val monthStart = first.atStartOfDay(zone).toInstant().toEpochMilli()
            val monthEnd = first.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
            loadInstances(monthStart, monthEnd, zone)
                .filter { it.overlaps(monthStart, monthEnd) && it.isProject() }
                .map { it.toDto(zone) }
          },
          empty = emptyList())

  /** Календари, в которые можно записывать события. */
  fun getWritableCalendars(): Flow<List<DeviceCalendar>> =
      dataChanges
          .mapLatest { granted -> if (granted) dataSource.queryWritableCalendars() else emptyList() }
          .catch { e ->
            Log.e(TAG, "Error reading calendars", e)
            emit(emptyList())
          }
          .flowOn(ioDispatcher)

  /** Календарь, в который сейчас будут создаваться события (с учётом автоподбора). */
  fun getDefaultCalendarId(): Flow<Long?> =
      combine(getWritableCalendars(), settingsRepository.defaultCalendarIdFlow) { calendars, saved
        ->
        (calendars.firstOrNull { it.id == saved } ?: pickDefaultCalendar(calendars))?.id
      }

  /** Просит систему подтянуть свежие данные из Google и других аккаунтов. */
  suspend fun requestSync() =
      withContext(ioDispatcher) {
        if (permissionManager.isGranted.value) {
          runCatching { dataSource.requestSync() }
              .onFailure { Log.w(TAG, "requestSync failed", it) }
        }
      }

  // --- Запись ---

  suspend fun createEvent(draft: EventDraft): Result<Unit> = write {
    val values =
        draft.contentValues().apply {
          put(Events.CALENDAR_ID, resolveTargetCalendarId())
          putTiming(timingOf(draft, draft.startDate, draft.endDate), draft.recurrenceRule)
        }
    dataSource.insertEvent(values)
  }

  suspend fun updateEvent(event: EventDto, draft: EventDraft, mode: EventUpdateMode): Result<Unit> =
      write {
        val isException = event.originalEventId != null
        val isSeriesInstance = event.recurrenceRule != null
        val timing = timingOf(draft, draft.startDate, draft.endDate)

        when {
          mode == EventUpdateMode.SINGLE_INSTANCE && isSeriesInstance -> {
            // Строки-исключения (ORIGINAL_ID) провайдер плохо переваривает для ещё не
            // синхронизированных серий, поэтому: исключаем дату из серии + отдельное событие.
            excludeInstance(event.eventId, event.instanceBegin)
            val values =
                draft.contentValues().apply {
                  put(Events.CALENDAR_ID, event.calendarId)
                  putTiming(timing, rrule = null)
                }
            dataSource.insertEvent(values)
          }
          mode == EventUpdateMode.SINGLE_INSTANCE && isException -> {
            dataSource.updateEvent(
                event.eventId, draft.contentValues().apply { putExceptionTiming(timing) })
          }
          mode == EventUpdateMode.ALL_IN_SERIES && (isSeriesInstance || isException) -> {
            val masterId = event.originalEventId ?: event.eventId
            val master = dataSource.getEvent(masterId) ?: error("Series $masterId not found")
            val zone = draft.zone
            // Исключение само не знает RRULE серии — берём его у мастер-события.
            val seriesDraft =
                if (isException) draft.copy(recurrenceRule = draft.recurrenceRule ?: master.rrule)
                else draft
            val values =
                seriesDraft.contentValues().apply {
                  putTiming(
                      seriesTiming(event, master.dtStart, master.isAllDay, seriesDraft, zone),
                      seriesDraft.recurrenceRule)
                }
            dataSource.updateEvent(masterId, values)
          }
          else -> {
            dataSource.updateEvent(
                event.eventId,
                draft.contentValues().apply { putTiming(timing, draft.recurrenceRule) })
          }
        }
      }

  suspend fun deleteEvent(event: EventDto, mode: EventDeleteMode): Result<Unit> = write {
    val masterId = event.originalEventId ?: event.eventId
    val isException = event.originalEventId != null

    when (mode) {
      EventDeleteMode.DEFAULT,
      EventDeleteMode.INSTANCE_ONLY ->
          if (isException) {
            // Перенесённый экземпляр: исключаем исходную дату из серии и убираем саму строку.
            excludeInstance(masterId, event.originalInstanceTimeOrBegin())
            dataSource.deleteEvent(event.eventId)
          } else if (mode == EventDeleteMode.INSTANCE_ONLY) {
            excludeInstance(event.eventId, event.instanceBegin)
          } else {
            dataSource.deleteEvent(event.eventId)
          }
      EventDeleteMode.ALL_IN_SERIES -> dataSource.deleteEvent(masterId)
      EventDeleteMode.THIS_AND_FOLLOWING -> {
        val master = dataSource.getEvent(masterId) ?: error("Series $masterId not found")
        val rrule = master.rrule
        val instanceTime = event.originalInstanceTimeOrBegin()
        if (rrule == null || instanceTime <= master.dtStart) {
          dataSource.deleteEvent(masterId)
        } else {
          val values =
              master.seriesValues().apply {
                put(Events.RRULE, rruleEndingBefore(rrule, instanceTime, master.isAllDay))
              }
          dataSource.updateEvent(masterId, values)
        }
      }
    }
  }

  // --- Внутреннее ---

  private suspend fun write(block: suspend () -> Any): Result<Unit> =
      withContext(ioDispatcher) {
        runCatching {
              block()
              Unit
            }
            .onFailure { Log.e(TAG, "Calendar write failed", it) }
      }

  /** Добавляет экземпляр (сырой Instances.BEGIN) в EXDATE серии. */
  private fun excludeInstance(masterId: Long, instanceBegin: Long) {
    val master = dataSource.getEvent(masterId) ?: error("Series $masterId not found")
    val date =
        if (master.isAllDay) utcDate(instanceBegin).format(DateTimeFormatter.BASIC_ISO_DATE)
        else
            Instant.ofEpochMilli(instanceBegin)
                .atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
    val exdate = listOfNotNull(master.exdate, date).joinToString(",")
    dataSource.updateEvent(masterId, master.seriesValues().apply { put(Events.EXDATE, exdate) })
  }

  /**
   * Полный набор полей времени серии. Провайдер пересобирает Instances, только если они все
   * пришли в update — правка одного RRULE/EXDATE оставляет старые экземпляры.
   */
  private fun EventRow.seriesValues() =
      ContentValues().apply {
        put(Events.DTSTART, dtStart)
        put(Events.DURATION, duration)
        put(Events.RRULE, rrule)
        put(Events.EXDATE, exdate)
        put(Events.EVENT_TIMEZONE, timeZone)
        put(Events.ALL_DAY, if (isAllDay) 1 else 0)
      }

  private suspend fun resolveTargetCalendarId(): Long {
    val calendars = dataSource.queryWritableCalendars()
    val saved = settingsRepository.defaultCalendarIdFlow.first()
    return (calendars.firstOrNull { it.id == saved } ?: pickDefaultCalendar(calendars))?.id
        ?: dataSource.createLocalCalendar()
  }

  private fun pickDefaultCalendar(calendars: List<DeviceCalendar>): DeviceCalendar? =
      calendars.firstOrNull { it.isPrimary && it.accountType == GOOGLE_ACCOUNT_TYPE }
          ?: calendars.firstOrNull { it.isPrimary }
          ?: calendars.firstOrNull()

  /** Экземпляр с временем, приведённым к локальному часовому поясу пользователя. */
  private class LocalInstance(
      val row: InstanceRow,
      val startMillis: Long,
      val endMillis: Long,
      val allDayStart: LocalDate?,
      val allDayEndExclusive: LocalDate?,
  )

  private fun LocalInstance.overlaps(fromMillis: Long, toMillis: Long): Boolean =
      endMillis > fromMillis && startMillis < toMillis

  /** «Проект» — событие на несколько дней: all-day от двух дней или обычное от суток. */
  private fun LocalInstance.isProject(): Boolean =
      if (row.isAllDay) ChronoUnit.DAYS.between(allDayStart, allDayEndExclusive) > 1
      else endMillis - startMillis >= DAY_MILLIS

  private fun loadInstances(dayStart: Long, dayEnd: Long, zone: ZoneId): List<LocalInstance> =
      // All-day события хранятся в UTC, поэтому расширяем окно на сутки в обе стороны.
      dataSource.queryInstances(dayStart - DAY_MILLIS, dayEnd + DAY_MILLIS).map { row ->
        if (row.isAllDay) {
          val start = utcDate(row.begin)
          val end = utcDate(row.end).let { if (it.isAfter(start)) it else start.plusDays(1) }
          LocalInstance(
              row,
              start.atStartOfDay(zone).toInstant().toEpochMilli(),
              end.atStartOfDay(zone).toInstant().toEpochMilli(),
              start,
              end)
        } else {
          LocalInstance(row, row.begin, maxOf(row.end, row.begin), null, null)
        }
      }

  private fun LocalInstance.toDto(zone: ZoneId): EventDto {
    val originalTime =
        row.originalInstanceTime?.let { time ->
          if (row.isAllDay) utcDate(time).atStartOfDay(zone).toInstant().toEpochMilli() else time
        } ?: startMillis.takeIf { row.rrule != null }

    return EventDto(
        id = "${row.eventId}_${row.begin}",
        summary = row.title?.takeIf { it.isNotBlank() } ?: context.getString(R.string.no_title),
        startTime = Instant.ofEpochMilli(startMillis),
        endTime = Instant.ofEpochMilli(endMillis),
        description = row.description?.takeIf { it.isNotBlank() },
        location = row.location?.takeIf { it.isNotBlank() },
        isAllDay = row.isAllDay,
        recurringEventId =
            if (row.rrule != null) row.eventId.toString() else row.originalId?.toString(),
        originalStartTime = originalTime?.let(Instant::ofEpochMilli),
        recurrenceRule = row.rrule,
        eventId = row.eventId,
        instanceBegin = row.begin,
        originalEventId = row.originalId,
        originalInstanceBegin = row.originalInstanceTime,
        calendarId = row.calendarId,
        color = row.color,
        isUntitled = row.title.isNullOrBlank())
  }

  /** Сырое время экземпляра в серии (для перенесённого — исходное, до переноса). */
  private fun EventDto.originalInstanceTimeOrBegin(): Long = originalInstanceBegin ?: instanceBegin

  private data class EventTiming(
      val dtStart: Long,
      val dtEnd: Long,
      val timeZone: String,
      val isAllDay: Boolean
  )

  private fun timingOf(draft: EventDraft, startDate: LocalDate, endDate: LocalDate): EventTiming {
    if (draft.isAllDay) {
      return EventTiming(
          dtStart = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
          dtEnd = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
          timeZone = "UTC",
          isAllDay = true)
    }
    val zone = draft.zone
    val startTime = requireNotNull(draft.startTime) { "Timed event without start time" }
    val endTime = requireNotNull(draft.endTime) { "Timed event without end time" }
    return EventTiming(
        dtStart = startDate.atTime(startTime).atZone(zone).toInstant().toEpochMilli(),
        dtEnd = endDate.atTime(endTime).atZone(zone).toInstant().toEpochMilli(),
        timeZone = zone.id,
        isAllDay = false)
  }

  /**
   * Время для правки всей серии: сдвигаем дату начала серии на столько же дней, на сколько
   * пользователь сдвинул редактируемый экземпляр, а время/длительность берём из формы.
   */
  private fun seriesTiming(
      event: EventDto,
      masterStart: Long,
      masterAllDay: Boolean,
      draft: EventDraft,
      zone: ZoneId
  ): EventTiming {
    // Повторение убрали — событие становится одиночным там, где его поставили в форме.
    if (draft.recurrenceRule == null) return timingOf(draft, draft.startDate, draft.endDate)

    val span = ChronoUnit.DAYS.between(draft.startDate, draft.endDate)
    val instanceDate = (event.originalStartTime ?: event.startTime).atZone(zone).toLocalDate()
    val masterDate =
        if (masterAllDay) utcDate(masterStart)
        else Instant.ofEpochMilli(masterStart).atZone(zone).toLocalDate()
    val newStart = masterDate.plusDays(ChronoUnit.DAYS.between(instanceDate, draft.startDate))
    return timingOf(draft, newStart, newStart.plusDays(span))
  }

  private fun EventDraft.contentValues() =
      ContentValues().apply {
        put(Events.TITLE, summary)
        put(Events.DESCRIPTION, description)
        put(Events.EVENT_LOCATION, location)
      }

  private fun ContentValues.putTiming(timing: EventTiming, rrule: String?) {
    put(Events.DTSTART, timing.dtStart)
    put(Events.EVENT_TIMEZONE, timing.timeZone)
    put(Events.ALL_DAY, if (timing.isAllDay) 1 else 0)
    if (rrule != null) {
      // Повторяющимся событиям провайдер требует DURATION вместо DTEND.
      put(Events.RRULE, rrule)
      put(Events.DURATION, durationOf(timing))
      putNull(Events.DTEND)
    } else {
      putNull(Events.RRULE)
      putNull(Events.DURATION)
      put(Events.DTEND, timing.dtEnd)
    }
  }

  private fun ContentValues.putExceptionTiming(timing: EventTiming) {
    put(Events.DTSTART, timing.dtStart)
    put(Events.DTEND, timing.dtEnd)
    put(Events.EVENT_TIMEZONE, timing.timeZone)
    put(Events.ALL_DAY, if (timing.isAllDay) 1 else 0)
  }

  private fun durationOf(timing: EventTiming): String {
    val millis = (timing.dtEnd - timing.dtStart).coerceAtLeast(0)
    return if (timing.isAllDay) "P${TimeUnit.MILLISECONDS.toDays(millis).coerceAtLeast(1)}D"
    else "P${TimeUnit.MILLISECONDS.toSeconds(millis)}S"
  }

  /** Обрезает серию так, чтобы последний экземпляр был строго до instanceTime. */
  private fun rruleEndingBefore(rrule: String, instanceTime: Long, allDay: Boolean): String {
    val until =
        if (allDay) {
          utcDate(instanceTime).minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)
        } else {
          Instant.ofEpochMilli(instanceTime - 1000)
              .atZone(ZoneOffset.UTC)
              .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
        }
    val parts =
        rrule.removePrefix("RRULE:").split(';').filterNot {
          it.startsWith("UNTIL=", ignoreCase = true) || it.startsWith("COUNT=", ignoreCase = true)
        }
    return (parts + "UNTIL=$until").joinToString(";")
  }

  private fun utcDate(millis: Long): LocalDate =
      Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

  companion object {
    private const val TAG = "CalendarRepository"
    private const val GOOGLE_ACCOUNT_TYPE = "com.google"
    private val DAY_MILLIS = TimeUnit.DAYS.toMillis(1)
    /** На сколько дней вперёд экран проектов показывает предстоящие длинные события. */
    private const val PROJECTS_LOOKAHEAD_DAYS = 180L
  }
}
