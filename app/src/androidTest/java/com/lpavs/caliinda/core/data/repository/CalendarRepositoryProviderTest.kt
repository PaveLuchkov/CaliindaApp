package com.lpavs.caliinda.core.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lpavs.caliinda.core.data.calendar.CalendarPermissionManager
import com.lpavs.caliinda.core.data.calendar.CalendarProviderDataSource
import com.lpavs.caliinda.core.data.calendar.model.EventDeleteMode
import com.lpavs.caliinda.core.data.calendar.model.EventDraft
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.data.calendar.model.EventUpdateMode
import com.lpavs.caliinda.core.data.di.settingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Гоняет CalendarRepository против настоящего системного CalendarProvider на устройстве. */
@RunWith(AndroidJUnit4::class)
class CalendarRepositoryProviderTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val zone = ZoneId.systemDefault()
  private val today = LocalDate.now()

  private lateinit var repository: CalendarRepository
  private var calendarId: Long = -1

  @Before
  fun setUp() = runBlocking {
    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    CalendarPermissionManager.PERMISSIONS.forEach {
      automation.grantRuntimePermission(context.packageName, it)
    }
    val dataSource = CalendarProviderDataSource(context)
    val settings = SettingsRepository(context.settingsDataStore)
    calendarId = dataSource.createLocalCalendar()
    settings.saveDefaultCalendarId(calendarId)
    repository =
        CalendarRepository(dataSource, CalendarPermissionManager(context), settings, Dispatchers.IO)
  }

  @After
  fun tearDown() {
    val uri =
        ContentUris.withAppendedId(Calendars.CONTENT_URI, calendarId)
            .buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, CalendarProviderDataSource.LOCAL_ACCOUNT_NAME)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
    context.contentResolver.delete(uri, null, null)
  }

  private fun draft(
      title: String,
      start: LocalDate,
      startTime: LocalTime? = LocalTime.of(10, 0),
      end: LocalDate = start,
      endTime: LocalTime? = LocalTime.of(11, 0),
      allDay: Boolean = false,
      rrule: String? = null
  ) =
      EventDraft(
          summary = title,
          description = null,
          location = null,
          isAllDay = allDay,
          startDate = start,
          startTime = if (allDay) null else startTime,
          endDate = end,
          endTime = if (allDay) null else endTime,
          zone = zone,
          recurrenceRule = rrule)

  /** Как после синхронизации с Google: у серии появляется _SYNC_ID (пишет только sync adapter). */
  private fun markSynced(eventId: Long) {
    val uri =
        ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            .buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, CalendarProviderDataSource.LOCAL_ACCOUNT_NAME)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
    val values = ContentValues().apply { put(CalendarContract.Events._SYNC_ID, "synced-$eventId") }
    assertEquals(1, context.contentResolver.update(uri, values, null, null))
  }

  private fun java.time.Instant.local() = atZone(zone).toLocalDateTime()

  private suspend fun day(date: LocalDate): List<EventDto> =
      repository.getEventsFlowForDate(date).first().filter { it.calendarId == calendarId }

  private suspend fun projects(date: LocalDate): List<EventDto> =
      repository.getEventsFlowForProjects(date).first().filter { it.calendarId == calendarId }

  @Test
  fun timedEvent_create_update_delete() = runBlocking {
    assertTrue(repository.createEvent(draft("Timed", today)).isSuccess)
    val created = day(today).single { it.summary == "Timed" }
    assertEquals(today.atTime(10, 0), created.startTime.local())
    assertEquals(today.atTime(11, 0), created.endTime.local())
    assertEquals(null, created.recurringEventId)

    val moved = draft("Timed moved", today, LocalTime.of(14, 0), endTime = LocalTime.of(15, 30))
    assertTrue(repository.updateEvent(created, moved, EventUpdateMode.ALL_IN_SERIES).isSuccess)
    val updated = day(today).single { it.eventId == created.eventId }
    assertEquals("Timed moved", updated.summary)
    assertEquals(today.atTime(14, 0), updated.startTime.local())

    assertTrue(repository.deleteEvent(updated, EventDeleteMode.DEFAULT).isSuccess)
    assertTrue(day(today).none { it.eventId == created.eventId })
  }

  @Test
  fun allDayEvents_goToDayOrProjects() = runBlocking {
    val tomorrow = today.plusDays(1)
    repository.createEvent(draft("One day", tomorrow, allDay = true)).getOrThrow()
    repository
        .createEvent(draft("Trip", tomorrow, end = tomorrow.plusDays(2), allDay = true))
        .getOrThrow()

    val oneDay = day(tomorrow).single { it.summary == "One day" }
    assertTrue(oneDay.isAllDay)
    assertEquals(tomorrow.atTime(0, 0), oneDay.startTime.local())
    assertTrue(day(today).none { it.summary == "One day" })
    assertTrue(day(tomorrow).none { it.summary == "Trip" })

    val trip = projects(tomorrow.plusDays(1)).single { it.summary == "Trip" }
    assertEquals(tomorrow.plusDays(3).atTime(0, 0), trip.endTime.local())
    // Предстоящий проект виден уже сегодня, закончившийся — пропадает.
    assertTrue(projects(today).any { it.summary == "Trip" })
    assertTrue(projects(tomorrow.plusDays(3)).none { it.summary == "Trip" })
  }

  @Test
  fun recurringSeries_instanceAndSeriesOperations() = runBlocking {
    repository
        .createEvent(
            draft("Standup", today, LocalTime.of(8, 0), endTime = LocalTime.of(8, 30),
                rrule = "FREQ=DAILY;COUNT=6"))
        .getOrThrow()
    (0L..5L).forEach { assertTrue("day $it", day(today.plusDays(it)).any { e -> e.summary == "Standup" }) }
    assertTrue(day(today.plusDays(6)).none { it.summary == "Standup" })

    // Удаляем только экземпляр +1
    val d1 = day(today.plusDays(1)).single { it.summary == "Standup" }
    assertEquals(d1.eventId.toString(), d1.recurringEventId)
    repository.deleteEvent(d1, EventDeleteMode.INSTANCE_ONLY).getOrThrow()
    assertTrue(day(today.plusDays(1)).none { it.summary == "Standup" })
    assertTrue(day(today.plusDays(2)).any { it.summary == "Standup" })

    // Переносим только экземпляр +2
    val d2 = day(today.plusDays(2)).single { it.summary == "Standup" }
    markSynced(d2.eventId)
    val d2Draft =
        draft("Standup moved", today.plusDays(2), LocalTime.of(12, 0), endTime = LocalTime.of(12, 30))
    repository.updateEvent(d2, d2Draft, EventUpdateMode.SINGLE_INSTANCE).getOrThrow()
    val moved2 = day(today.plusDays(2)).single { it.summary.startsWith("Standup") }
    assertEquals("Standup moved", moved2.summary)
    assertEquals(today.plusDays(2).atTime(12, 0), moved2.startTime.local())
    // Перенесённый экземпляр остаётся в серии — исключение, а не отдельное событие.
    assertEquals(d2.eventId, moved2.originalEventId)
    assertEquals(d2.eventId.toString(), moved2.recurringEventId)
    assertEquals("FREQ=DAILY;COUNT=6", repository.seriesRule(moved2))
    assertTrue(day(today.plusDays(3)).single { it.summary.startsWith("Standup") }.summary == "Standup")

    // Правка самого исключения ещё раз — только оно
    val again = draft("Standup moved again", today.plusDays(2), LocalTime.of(13, 0),
        endTime = LocalTime.of(13, 30))
    repository.updateEvent(moved2, again, EventUpdateMode.SINGLE_INSTANCE).getOrThrow()
    val moved2Again = day(today.plusDays(2)).single { it.summary.startsWith("Standup") }
    assertEquals("Standup moved again", moved2Again.summary)
    assertEquals(today.plusDays(2).atTime(13, 0), moved2Again.startTime.local())

    // Переименовываем всю серию из сегодняшнего экземпляра
    val d0 = day(today).single { it.summary == "Standup" }
    val renamed =
        draft("Daily sync", today, LocalTime.of(8, 0), endTime = LocalTime.of(8, 30),
            rrule = d0.recurrenceRule)
    repository.updateEvent(d0, renamed, EventUpdateMode.ALL_IN_SERIES).getOrThrow()
    assertTrue(day(today).any { it.summary == "Daily sync" })
    assertTrue(day(today.plusDays(4)).any { it.summary == "Daily sync" })
    // Исключение живёт своей жизнью и не дублируется исходным экземпляром.
    assertEquals(
        listOf("Standup moved again"),
        day(today.plusDays(2)).map { it.summary })

    // Удаляем "этот и следующие" начиная с +4
    val d4 = day(today.plusDays(4)).single { it.summary == "Daily sync" }
    repository.deleteEvent(d4, EventDeleteMode.THIS_AND_FOLLOWING).getOrThrow()
    assertTrue(day(today.plusDays(3)).any { it.summary == "Daily sync" })
    assertTrue(day(today.plusDays(4)).none { it.summary == "Daily sync" })
    assertTrue(day(today.plusDays(5)).none { it.summary == "Daily sync" })

    // И, наконец, всю серию
    repository.deleteEvent(day(today).single { it.summary == "Daily sync" }, EventDeleteMode.ALL_IN_SERIES)
        .getOrThrow()
    assertTrue(day(today).none { it.summary == "Daily sync" })
  }

  @Test
  fun recurringSeries_editThisAndFollowing() = runBlocking {
    repository
        .createEvent(
            draft("Gym", today, LocalTime.of(18, 0), endTime = LocalTime.of(19, 0),
                rrule = "FREQ=DAILY;COUNT=6"))
        .getOrThrow()
    markSynced(day(today).single { it.summary == "Gym" }.eventId)
    // Перенесённый экземпляр после точки разреза — не должен остаться дублем.
    val d4 = day(today.plusDays(4)).single { it.summary == "Gym" }
    repository
        .updateEvent(
            d4, draft("Gym late", today.plusDays(4), LocalTime.of(21, 0), endTime = LocalTime.of(22, 0)),
            EventUpdateMode.SINGLE_INSTANCE)
        .getOrThrow()

    val d3 = day(today.plusDays(3)).single { it.summary == "Gym" }
    val evening =
        draft("Gym evening", today.plusDays(3), LocalTime.of(20, 0), endTime = LocalTime.of(21, 0),
            rrule = "FREQ=DAILY;COUNT=6")
    repository.updateEvent(d3, evening, EventUpdateMode.THIS_AND_FOLLOWING).getOrThrow()

    (0L..2L).forEach { assertEquals("day $it", listOf("Gym"), day(today.plusDays(it)).map { e -> e.summary }) }
    (3L..5L).forEach {
      val e = day(today.plusDays(it)).single()
      assertEquals("day $it", "Gym evening", e.summary)
      assertEquals(today.plusDays(it).atTime(20, 0), e.startTime.local())
    }
    // COUNT=6 делится: 3 у старой серии и 3 у новой — седьмого дня нет.
    assertTrue(day(today.plusDays(6)).isEmpty())
    val newSeries = day(today.plusDays(3)).single()
    assertEquals("FREQ=DAILY;COUNT=3", newSeries.recurrenceRule)

    // С первого экземпляра «это и следующие» = вся серия, без новой строки.
    val first = day(today).single()
    repository
        .updateEvent(
            first, draft("Gym morning", today, LocalTime.of(7, 0), endTime = LocalTime.of(8, 0),
                rrule = first.recurrenceRule),
            EventUpdateMode.THIS_AND_FOLLOWING)
        .getOrThrow()
    assertEquals(first.eventId, day(today).single().eventId)
    assertEquals("Gym morning", day(today.plusDays(2)).single().summary)
  }

  @Test
  fun unsyncedSeries_singleEditDetaches() = runBlocking {
    // Без _SYNC_ID провайдер теряет серию с исключением — правка уходит в отдельное событие.
    repository
        .createEvent(
            draft("Walk", today, LocalTime.of(9, 0), endTime = LocalTime.of(9, 30),
                rrule = "FREQ=DAILY;COUNT=3"))
        .getOrThrow()
    val d1 = day(today.plusDays(1)).single()
    repository
        .updateEvent(
            d1, draft("Walk later", today.plusDays(1), LocalTime.of(10, 0), endTime = LocalTime.of(10, 30)),
            EventUpdateMode.SINGLE_INSTANCE)
        .getOrThrow()
    assertEquals(listOf("Walk"), day(today).map { it.summary })
    val moved = day(today.plusDays(1)).single()
    assertEquals("Walk later", moved.summary)
    assertEquals(null, moved.recurringEventId)
    assertEquals(listOf("Walk"), day(today.plusDays(2)).map { it.summary })
  }
}
