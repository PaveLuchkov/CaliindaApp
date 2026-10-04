package com.lpavs.caliinda.core.data.repository

import android.content.ContentUris
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
  private val zone = ZoneId.systemDefault().id
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
          timeZoneId = zone,
          recurrenceRule = rrule)

  private fun java.time.Instant.local() = atZone(ZoneId.of(zone)).toLocalDateTime()

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
    val d2Draft =
        draft("Standup moved", today.plusDays(2), LocalTime.of(12, 0), endTime = LocalTime.of(12, 30))
    repository.updateEvent(d2, d2Draft, EventUpdateMode.SINGLE_INSTANCE).getOrThrow()
    val moved2 = day(today.plusDays(2)).single { it.summary.startsWith("Standup") }
    assertEquals("Standup moved", moved2.summary)
    assertEquals(today.plusDays(2).atTime(12, 0), moved2.startTime.local())
    // Перенесённый экземпляр становится самостоятельным событием.
    assertEquals(null, moved2.recurringEventId)
    assertTrue(day(today.plusDays(3)).single { it.summary.startsWith("Standup") }.summary == "Standup")

    // Переименовываем всю серию из сегодняшнего экземпляра
    val d0 = day(today).single { it.summary == "Standup" }
    val renamed =
        draft("Daily sync", today, LocalTime.of(8, 0), endTime = LocalTime.of(8, 30),
            rrule = d0.recurrenceRule)
    repository.updateEvent(d0, renamed, EventUpdateMode.ALL_IN_SERIES).getOrThrow()
    assertTrue(day(today).any { it.summary == "Daily sync" })
    assertTrue(day(today.plusDays(4)).any { it.summary == "Daily sync" })

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
}
