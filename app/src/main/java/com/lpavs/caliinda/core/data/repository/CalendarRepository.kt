package com.lpavs.caliinda.core.data.repository

import java.time.YearMonth
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
import com.lpavs.caliinda.core.data.calendar.recurrence.Rrule
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

  /** Что нужно чтению: пояс из настроек и календари, скрытые в Caliinda. */
  private class ReadScope(val zone: ZoneId, val hidden: Set<Long>)

  private fun <T> observeCalendar(load: ReadScope.() -> T, empty: T): Flow<T> =
      combine(
              dataChanges,
              settingsRepository.zoneFlow,
              settingsRepository.hiddenCalendarIdsFlow) { granted, zone, hidden ->
                if (granted) ReadScope(zone, hidden) else null
              }
          .mapLatest { read -> read?.load() ?: empty }
          .catch { e ->
            Log.e(TAG, "Error reading calendar", e)
            emit(empty)
          }
          .flowOn(ioDispatcher)

  // --- Чтение ---

  /**
   * Страница дня одним запросом к провайдеру: события дня (обычные < 24ч и однодневные all-day)
   * и проекты, которые идут в этот день, — для лент под шапкой.
   */
  fun getDayFlow(date: LocalDate): Flow<DayEvents> =
      observeCalendar(
          load = {
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val instances = loadInstances(dayStart, dayEnd)
            DayEvents(
                events =
                    instances
                        .filter { inst ->
                          if (inst.row.isAllDay) {
                            inst.allDayStart == date && inst.allDayEndExclusive == date.plusDays(1)
                          } else {
                            inst.overlaps(dayStart, dayEnd) &&
                                inst.endMillis - inst.startMillis < DAY_MILLIS
                          }
                        }
                        .map { it.toDto(zone) },
                projects =
                    instances
                        .filter { inst -> inst.overlaps(dayStart, dayEnd) && inst.isProject() }
                        .sortedBy { it.startMillis }
                        .map { it.toDto(zone) })
          },
          empty = DayEvents(emptyList(), emptyList()))

  /** События дня без проектов — для виджета. */
  fun getEventsFlowForDate(date: LocalDate): Flow<List<EventDto>> =
      getDayFlow(date).map { it.events }

  /** "Проекты": многодневные события, которые идут в указанную дату. */
  fun getEventsFlowForProjects(date: LocalDate): Flow<List<EventDto>> =
      observeCalendar(
          load = {
            // Идущие сейчас и предстоящие: иначе проект, начинающийся завтра, нигде не виден.
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val windowEnd =
                date.plusDays(PROJECTS_LOOKAHEAD_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
            loadInstances(dayStart, windowEnd)
                .filter { inst -> inst.overlaps(dayStart, windowEnd) && inst.isProject() }
                .map { it.toDto(zone) }
          },
          empty = emptyList())

  /** Многодневные события, пересекающие месяц (в том числе уже прошедшие) — страница месяца. */
  fun getMonthProjectsFlow(month: YearMonth): Flow<List<EventDto>> =
      observeCalendar(
          load = {
            val first = month.atDay(1)
            val monthStart = first.atStartOfDay(zone).toInstant().toEpochMilli()
            val monthEnd = first.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
            loadInstances(monthStart, monthEnd)
                .filter { it.overlaps(monthStart, monthEnd) && it.isProject() }
                .map { it.toDto(zone) }
          },
          empty = emptyList())

  /**
   * Идущие и предстоящие события на [days] дней вперёд, по одному — ближайшему — экземпляру на
   * событие: у ежегодного дня рождения важен только следующий. Для обратного отсчёта.
   */
  fun getUpcomingFlow(from: LocalDate, days: Long): Flow<List<EventDto>> =
      observeCalendar(
          load = {
            val start = from.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = from.plusDays(days).atStartOfDay(zone).toInstant().toEpochMilli()
            loadInstances(start, end)
                .filter { it.overlaps(start, end) }
                .sortedBy { it.startMillis }
                .distinctBy { it.row.eventId }
                .map { it.toDto(zone) }
          },
          empty = emptyList())

  /** Календари, в которые можно записывать события, — кроме скрытых фильтром. */
  fun getWritableCalendars(): Flow<List<DeviceCalendar>> =
      combine(dataChanges, settingsRepository.hiddenCalendarIdsFlow) { granted, hidden ->
            granted to hidden
          }
          .mapLatest { (granted, hidden) ->
            if (granted) shownCalendars(dataSource.queryWritableCalendars(), hidden) else emptyList()
          }
          .catch { e ->
            Log.e(TAG, "Error reading calendars", e)
            emit(emptyList())
          }
          .flowOn(ioDispatcher)

  /** Все видимые календари устройства — для фильтра в настройках. */
  fun getVisibleCalendars(): Flow<List<DeviceCalendar>> =
      dataChanges
          .mapLatest { granted -> if (granted) dataSource.queryVisibleCalendars() else emptyList() }
          .catch { e ->
            Log.e(TAG, "Error reading calendars", e)
            emit(emptyList())
          }
          .flowOn(ioDispatcher)

  /** Календарь, в который сейчас будут создаваться события (с учётом автоподбора). */
  fun getDefaultCalendarId(): Flow<Long?> =
      combine(getWritableCalendars(), settingsRepository.defaultCalendarIdFlow) { calendars, saved
        ->
        targetCalendar(calendars, saved)?.id
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
          put(Events.CALENDAR_ID, resolveTargetCalendarId(draft.calendarId))
          putTiming(timingOf(draft), draft.recurrenceRule)
        }
    val id = dataSource.insertEvent(values)
    draft.reminders?.let { dataSource.replaceReminders(id, it) }
  }

  /** Напоминания события (минуты до начала) — для формы правки. */
  suspend fun remindersOf(event: EventDto): List<Int> =
      withContext(ioDispatcher) {
        runCatching { dataSource.queryReminders(event.eventId) }.getOrDefault(emptyList())
      }

  suspend fun updateEvent(event: EventDto, draft: EventDraft, mode: EventUpdateMode): Result<Unit> =
      write {
        val isException = event.originalEventId != null
        val isSeriesInstance = event.recurrenceRule != null
        val inSeries = isSeriesInstance || isException
        val timing = timingOf(draft)

        when {
          mode == EventUpdateMode.SINGLE_INSTANCE && isSeriesInstance ->
              editSingleInstance(event, draft, timing)
          mode == EventUpdateMode.SINGLE_INSTANCE && isException -> {
            dataSource.updateEvent(
                event.eventId, draft.contentValues().apply { putExceptionTiming(timing) })
            draft.reminders?.let { dataSource.replaceReminders(event.eventId, it) }
          }
          mode == EventUpdateMode.THIS_AND_FOLLOWING && inSeries -> splitSeries(event, draft)
          mode != EventUpdateMode.SINGLE_INSTANCE && inSeries -> updateWholeSeries(event, draft)
          else -> {
            dataSource.updateEvent(
                event.eventId,
                draft.contentValues().apply { putTiming(timing, draft.recurrenceRule) })
            draft.reminders?.let { dataSource.replaceReminders(event.eventId, it) }
          }
        }
      }

  /**
   * Один экземпляр серии. У синхронизированной — настоящее исключение: правка остаётся частью
   * серии, и Google Calendar видит её как вхождение. У несинхронизированной провайдер связывает
   * исключения с серией по _SYNC_ID и без него теряет все экземпляры — тогда по-старому:
   * исключаем дату из серии + отдельное событие.
   */
  private fun editSingleInstance(event: EventDto, draft: EventDraft, timing: EventTiming) {
    val master = dataSource.getEvent(event.eventId) ?: error("Series ${event.eventId} not found")
    if (master.syncId != null) {
      val values =
          draft.contentValues().apply {
            put(Events.ORIGINAL_INSTANCE_TIME, event.instanceBegin)
            // DTEND провайдер у новых исключений не принимает — считает из DURATION.
            put(Events.DTSTART, timing.dtStart)
            put(Events.DURATION, timing.duration)
            put(Events.EVENT_TIMEZONE, timing.timeZone)
            put(Events.ALL_DAY, if (timing.isAllDay) 1 else 0)
          }
      val id = dataSource.insertException(master.id, values)
      dataSource.replaceReminders(id, draft.reminders ?: dataSource.queryReminders(master.id))
    } else {
      excludeInstance(master.id, event.instanceBegin)
      val values =
          draft.contentValues().apply {
            put(Events.CALENDAR_ID, event.calendarId)
            putTiming(timing, rrule = null)
          }
      val id = dataSource.insertEvent(values)
      // Отдельное событие вместо экземпляра — напоминания как у серии, если их не меняли.
      dataSource.replaceReminders(id, draft.reminders ?: dataSource.queryReminders(master.id))
    }
  }

  /** Правило серии, к которой относится экземпляр (у исключений своего RRULE нет). */
  suspend fun seriesRule(event: EventDto): String? =
      event.recurrenceRule
          ?: withContext(ioDispatcher) {
            event.originalEventId?.let { runCatching { dataSource.getEvent(it)?.rrule }.getOrNull() }
          }

  private fun updateWholeSeries(event: EventDto, draft: EventDraft) {
    val masterId = event.originalEventId ?: event.eventId
    val master = dataSource.getEvent(masterId) ?: error("Series $masterId not found")
    // Исключение само не знает RRULE серии — берём его у мастер-события.
    val seriesDraft =
        if (event.originalEventId != null) {
          draft.copy(recurrenceRule = draft.recurrenceRule ?: master.rrule)
        } else draft
    val values =
        seriesDraft.contentValues().apply {
          putTiming(
              seriesTiming(event, master.dtStart, master.isAllDay, seriesDraft),
              seriesDraft.recurrenceRule)
        }
    dataSource.updateEvent(masterId, values)
    draft.reminders?.let { dataSource.replaceReminders(masterId, it) }
  }

  /**
   * «Это и следующие»: старая серия заканчивается до экземпляра, с него начинается новая — с
   * правками из формы. С первого экземпляра это то же, что править всю серию.
   */
  private fun splitSeries(event: EventDto, draft: EventDraft) {
    val masterId = event.originalEventId ?: event.eventId
    val master = dataSource.getEvent(masterId) ?: error("Series $masterId not found")
    val rrule = master.rrule
    val splitTime = event.originalInstanceTimeOrBegin()
    if (rrule == null || splitTime <= master.dtStart) {
      updateWholeSeries(event, draft)
      return
    }
    val oldRule = Rrule.parse(rrule)
    // COUNT считал экземпляры с начала серии — новой достаётся только остаток.
    val newRule =
        draft.recurrenceRule?.let(Rrule::parse)?.let { rule ->
          val count = rule.count
          if (count != null && count == oldRule.count) {
            rule.copy(count = (count - occurrencesBefore(master, splitTime)).coerceAtLeast(1))
          } else rule
        }
    truncateSeries(master, splitTime)
    val values =
        draft.contentValues().apply {
          put(Events.CALENDAR_ID, event.calendarId)
          putTiming(timingOf(draft), newRule?.format())
        }
    val id = dataSource.insertEvent(values)
    // Новая серия продолжает старую — и напоминания у неё те же, если их не меняли.
    dataSource.replaceReminders(id, draft.reminders ?: dataSource.queryReminders(master.id))
  }

  /** Серия заканчивается строго до [splitTime]; её исключения с этого момента удаляются. */
  private fun truncateSeries(master: EventRow, splitTime: Long) {
    val rrule = master.rrule ?: return
    // Иначе перенесённые экземпляры остались бы висеть после конца серии.
    dataSource.deleteExceptionsFrom(master.id, splitTime)
    val values =
        master.seriesValues().apply {
          put(Events.RRULE, Rrule.parse(rrule).endingBefore(splitTime, master.isAllDay).format())
        }
    dataSource.updateEvent(master.id, values)
  }

  /**
   * Сколько экземпляров серии (в смысле COUNT) было до [splitTime]: видимые, перенесённые и
   * удалённые через EXDATE — COUNT считает и их.
   */
  private fun occurrencesBefore(master: EventRow, splitTime: Long): Int {
    val shown =
        dataSource.queryInstances(master.dtStart, splitTime).count { row ->
          (row.eventId == master.id && row.begin < splitTime) ||
              (row.originalId == master.id && (row.originalInstanceTime ?: Long.MAX_VALUE) < splitTime)
        }
    val excluded = master.exdate?.split(',')?.count { Rrule.exdateMillis(it) < splitTime } ?: 0
    return shown + excluded
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
          truncateSeries(master, instanceTime)
        }
      }
    }
  }

  // --- Внутреннее ---

  private suspend fun write(block: suspend () -> Any?): Result<Unit> =
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
    val date = Rrule.exdateValue(instanceBegin, master.isAllDay)
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

  private suspend fun resolveTargetCalendarId(chosen: Long?): Long {
    val writable = dataSource.queryWritableCalendars()
    // Выбранный в форме — если в него по-прежнему можно писать (аккаунт могли удалить).
    if (chosen != null && writable.any { it.id == chosen }) return chosen
    val calendars = shownCalendars(writable, settingsRepository.hiddenCalendarIdsFlow.first())
    val saved = settingsRepository.defaultCalendarIdFlow.first()
    return targetCalendar(calendars, saved)?.id ?: dataSource.createLocalCalendar()
  }

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

  private fun ReadScope.loadInstances(dayStart: Long, dayEnd: Long): List<LocalInstance> =
      // All-day события хранятся в UTC, поэтому расширяем окно на сутки в обе стороны.
      dataSource
          .queryInstances(dayStart - DAY_MILLIS, dayEnd + DAY_MILLIS)
          .filter { it.calendarId !in hidden }
          .map { row ->
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
        summary = row.title.orEmpty(),
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
        color = row.color)
  }

  /** Сырое время экземпляра в серии (для перенесённого — исходное, до переноса). */
  private fun EventDto.originalInstanceTimeOrBegin(): Long = originalInstanceBegin ?: instanceBegin

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
      put(Events.DURATION, timing.duration)
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

  private fun utcDate(millis: Long): LocalDate =
      Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

  companion object {
    private const val TAG = "CalendarRepository"
    private val DAY_MILLIS = TimeUnit.DAYS.toMillis(1)
    /** На сколько дней вперёд экран проектов показывает предстоящие длинные события. */
    private const val PROJECTS_LOOKAHEAD_DAYS = 180L
  }
}

/** Страница дня: события самого дня и проекты, которые его захватывают. */
data class DayEvents(val events: List<EventDto>, val projects: List<EventDto>)

private const val GOOGLE_ACCOUNT_TYPE = "com.google"

/**
 * Куда сохранять можно только в показанные календари: иначе новое событие сразу пропадёт с
 * экрана. Если скрыты все — не прячем ничего, чтобы не плодить локальный календарь.
 */
internal fun shownCalendars(writable: List<DeviceCalendar>, hidden: Set<Long>): List<DeviceCalendar> =
    writable.filter { it.id !in hidden }.ifEmpty { writable }

/**
 * Сохранённый выбор, а если он скрыт или исчез — автоподбор. Сам выбор не перезаписываем: покажут
 * календарь снова — он вернётся календарём по умолчанию.
 */
internal fun targetCalendar(calendars: List<DeviceCalendar>, saved: Long?): DeviceCalendar? =
    calendars.firstOrNull { it.id == saved }
        ?: calendars.firstOrNull { it.isPrimary && it.accountType == GOOGLE_ACCOUNT_TYPE }
        ?: calendars.firstOrNull { it.isPrimary }
        ?: calendars.firstOrNull()
