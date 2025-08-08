package com.lpavs.caliinda.core.data.repository.mapper

import com.lpavs.caliinda.core.data.remote.dto.EventDto
import com.lpavs.caliinda.core.data.repository.CalendarEventEntity
import com.lpavs.caliinda.core.ui.util.IDateTimeUtils
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EventMapperTest {

  private lateinit var dateTimeUtils: IDateTimeUtils
  private lateinit var eventMapper: EventMapper

  @Before
  fun setUp() {
    dateTimeUtils = mock(IDateTimeUtils::class.java)
    eventMapper = EventMapper(dateTimeUtils)
  }

  @Test
  fun mapToEntity_returnsEntity_whenDatesAreValid() {
    val event = EventDto(
      id = "1",
      summary = "Test Event",
      startTime = "2025-08-08T10:00:00Z",
      endTime = "2025-08-08T12:00:00Z",
      description = "desc",
      location = "location",
      isAllDay = false,
      recurringEventId = null,
      originalStartTime = null,
      recurrenceRule = null
    )

    val startInstant = Instant.parse("2025-08-08T10:00:00Z")
    val endInstant = Instant.parse("2025-08-08T12:00:00Z")

    `when`(dateTimeUtils.parseToInstant(event.startTime, "UTC")).thenReturn(startInstant)
    `when`(dateTimeUtils.parseToInstant(event.endTime, "UTC")).thenReturn(endInstant)

    val entity = eventMapper.mapToEntity(event, "UTC")

    assertNotNull(entity)
    assertEquals(startInstant.toEpochMilli(), entity.startTimeMillis)
    assertEquals(endInstant.toEpochMilli(), entity.endTimeMillis)
  }

  @Test
  fun mapToEntity_returnsNull_whenStartTimeInvalid() {
    val event = EventDto(
      id = "1",
      summary = "Invalid Start",
      startTime = "bad-date",
      endTime = null,
      description = null,
      location = null,
      isAllDay = false,
      recurringEventId = null,
      originalStartTime = null,
      recurrenceRule = null
    )

    `when`(dateTimeUtils.parseToInstant(event.startTime, "UTC")).thenReturn(null)

    val entity = eventMapper.mapToEntity(event, "UTC")

    assertNull(entity)
  }

  @Test
  fun mapToEntity_setsEndTimeNextDay_forAllDayEventWithoutEndTime() {
    val event = EventDto(
      id = "1",
      summary = "All Day",
      startTime = "2025-08-08T00:00:00Z",
      endTime = null,
      description = null,
      location = null,
      isAllDay = true,
      recurringEventId = null,
      originalStartTime = null,
      recurrenceRule = null
    )

    val startInstant = Instant.parse("2025-08-08T00:00:00Z")

    `when`(dateTimeUtils.parseToInstant(event.startTime, "UTC")).thenReturn(startInstant)
    `when`(dateTimeUtils.parseToInstant(null, "UTC")).thenReturn(null)

    val entity = eventMapper.mapToEntity(event, "UTC")

    assertNotNull(entity)
    assertEquals(
      startInstant.plus(1, ChronoUnit.DAYS).toEpochMilli(),
      entity.endTimeMillis
    )
  }
  @Test
  fun mapToEntity_usesStartTime_whenEndTimeBeforeStartTime() {
    val event = EventDto(
      id = "1",
      summary = "Invalid End Time",
      startTime = "2025-08-08T12:00:00Z",
      endTime = "2025-08-08T10:00:00Z", // раньше startTime
      description = null,
      location = null,
      isAllDay = false,
      recurringEventId = null,
      originalStartTime = null,
      recurrenceRule = null
    )

    val startInstant = Instant.parse("2025-08-08T12:00:00Z")
    val endInstant = Instant.parse("2025-08-08T10:00:00Z")

    `when`(dateTimeUtils.parseToInstant(event.startTime, "UTC")).thenReturn(startInstant)
    `when`(dateTimeUtils.parseToInstant(event.endTime, "UTC")).thenReturn(endInstant)

    val entity = eventMapper.mapToEntity(event, "UTC")

    assertNotNull(entity)
    assertEquals(startInstant.toEpochMilli(), entity.startTimeMillis)
    assertEquals(startInstant.toEpochMilli(), entity.endTimeMillis) // endTime = startTime
  }

  @Test
  fun mapToEntity_usesStartTime_whenEndTimeEqualToStartTime() {
    val event = EventDto(
      id = "1",
      summary = "Same Times",
      startTime = "2025-08-08T12:00:00Z",
      endTime = "2025-08-08T12:00:00Z", // равно startTime
      description = null,
      location = null,
      isAllDay = false,
      recurringEventId = null,
      originalStartTime = null,
      recurrenceRule = null
    )

    val startInstant = Instant.parse("2025-08-08T12:00:00Z")
    val endInstant = Instant.parse("2025-08-08T12:00:00Z")

    `when`(dateTimeUtils.parseToInstant(event.startTime, "UTC")).thenReturn(startInstant)
    `when`(dateTimeUtils.parseToInstant(event.endTime, "UTC")).thenReturn(endInstant)

    val entity = eventMapper.mapToEntity(event, "UTC")

    assertNotNull(entity)
    assertEquals(startInstant.toEpochMilli(), entity.startTimeMillis)
    assertEquals(startInstant.toEpochMilli(), entity.endTimeMillis) // endTime = startTime
  }

  @Test
  fun mapToEntity_usesStartTimeAsEndTime_whenNotAllDayAndNoEndTime() {
    val event = EventDto(
      id = "1",
      summary = "No End Time",
      startTime = "2025-08-08T10:00:00Z",
      endTime = null,
      description = null,
      location = null,
      isAllDay = false, // НЕ all-day событие
      recurringEventId = null,
      originalStartTime = null,
      recurrenceRule = null
    )

    val startInstant = Instant.parse("2025-08-08T10:00:00Z")

    `when`(dateTimeUtils.parseToInstant(event.startTime, "UTC")).thenReturn(startInstant)
    `when`(dateTimeUtils.parseToInstant(null, "UTC")).thenReturn(null)

    val entity = eventMapper.mapToEntity(event, "UTC")

    assertNotNull(entity)
    assertEquals(startInstant.toEpochMilli(), entity.startTimeMillis)
    assertEquals(startInstant.toEpochMilli(), entity.endTimeMillis) // endTime = startTime
  }

  @Test
  fun mapToEntity_usesStartTimeAsEndTime_whenNotAllDayAndInvalidEndTime() {
    val event = EventDto(
      id = "1",
      summary = "Invalid End Time",
      startTime = "2025-08-08T10:00:00Z",
      endTime = "invalid-date",
      description = null,
      location = null,
      isAllDay = false,
      recurringEventId = null,
      originalStartTime = null,
      recurrenceRule = null
    )

    val startInstant = Instant.parse("2025-08-08T10:00:00Z")

    `when`(dateTimeUtils.parseToInstant(event.startTime, "UTC")).thenReturn(startInstant)
    `when`(dateTimeUtils.parseToInstant(event.endTime, "UTC")).thenReturn(null)

    val entity = eventMapper.mapToEntity(event, "UTC")

    assertNotNull(entity)
    assertEquals(startInstant.toEpochMilli(), entity.startTimeMillis)
    assertEquals(startInstant.toEpochMilli(), entity.endTimeMillis) // endTime = startTime
  }

  @Test
  fun mapToEntity_returnsNull_whenExceptionThrown() {
    val event = EventDto(
      id = "1",
      summary = "Exception Test",
      startTime = "2025-08-08T10:00:00Z",
      endTime = "2025-08-08T12:00:00Z",
      description = null,
      location = null,
      isAllDay = false,
      recurringEventId = null,
      originalStartTime = null,
      recurrenceRule = null
    )

    // Имитируем исключение при парсинге
    `when`(dateTimeUtils.parseToInstant(event.startTime, "UTC"))
      .thenThrow(RuntimeException("Parse error"))

    val entity = eventMapper.mapToEntity(event, "UTC")

    assertNull(entity)
  }

  @Test
  fun mapToEntity_handlesNullAndEmptyValues() {
    val event = EventDto(
      id = "1",
      summary = "Test Event",
      startTime = "2025-08-08T10:00:00Z",
      endTime = "2025-08-08T12:00:00Z",
      description = null, // null значения
      location = "",      // пустые строки
      isAllDay = false,
      recurringEventId = null,
      originalStartTime = null,
      recurrenceRule = null
    )

    val startInstant = Instant.parse("2025-08-08T10:00:00Z")
    val endInstant = Instant.parse("2025-08-08T12:00:00Z")

    `when`(dateTimeUtils.parseToInstant(event.startTime, "UTC")).thenReturn(startInstant)
    `when`(dateTimeUtils.parseToInstant(event.endTime, "UTC")).thenReturn(endInstant)

    val entity = eventMapper.mapToEntity(event, "UTC")

    assertNotNull(entity)
    assertNull(entity.description)
    assertEquals("", entity.location)
    assertNull(entity.recurringEventId)
    assertNull(entity.originalStartTimeString)
    assertNull(entity.recurrenceRuleString)
  }

// ТЕСТЫ ДЛЯ mapToDomain

  @Test
  fun mapToDomain_returnsDtoWithFormattedDates() {
    val entity = CalendarEventEntity(
      id = "1",
      summary = "Test Event",
      startTimeMillis = 1691496000000L, // 2025-08-08T10:00:00Z
      endTimeMillis = 1691503200000L,   // 2025-08-08T12:00:00Z
      description = "Test description",
      location = "Test location",
      isAllDay = false,
      recurringEventId = "recurring-1",
      originalStartTimeString = "original-start",
      recurrenceRuleString = "RRULE:FREQ=DAILY"
    )

    `when`(dateTimeUtils.formatMillisToIsoString(entity.startTimeMillis, "UTC"))
      .thenReturn("2025-08-08T10:00:00Z")
    `when`(dateTimeUtils.formatMillisToIsoString(entity.endTimeMillis, "UTC"))
      .thenReturn("2025-08-08T12:00:00Z")

    val dto = eventMapper.mapToDomain(entity, "UTC")

    assertEquals("1", dto.id)
    assertEquals("Test Event", dto.summary)
    assertEquals("2025-08-08T10:00:00Z", dto.startTime)
    assertEquals("2025-08-08T12:00:00Z", dto.endTime)
    assertEquals("Test description", dto.description)
    assertEquals("Test location", dto.location)
    assertEquals(false, dto.isAllDay)
    assertEquals("recurring-1", dto.recurringEventId)
    assertEquals("original-start", dto.originalStartTime)
    assertEquals("RRULE:FREQ=DAILY", dto.recurrenceRule)
  }

  @Test
  fun mapToDomain_handlesNullFormattedDates() {
    val entity = CalendarEventEntity(
      id = "1",
      summary = "Test Event",
      startTimeMillis = 1691496000000L,
      endTimeMillis = 1691503200000L,
      description = null,
      location = null,
      isAllDay = true,
      recurringEventId = null,
      originalStartTimeString = null,
      recurrenceRuleString = null
    )

    // Имитируем, что форматирование вернуло null
    `when`(dateTimeUtils.formatMillisToIsoString(entity.startTimeMillis, "UTC"))
      .thenReturn(null)
    `when`(dateTimeUtils.formatMillisToIsoString(entity.endTimeMillis, "UTC"))
      .thenReturn(null)

    val dto = eventMapper.mapToDomain(entity, "UTC")

    assertEquals("", dto.startTime) // пустая строка вместо null
    assertEquals("", dto.endTime)   // пустая строка вместо null
    assertNull(dto.description)
    assertNull(dto.location)
    assertTrue(dto.isAllDay)
    assertNull(dto.recurringEventId)
    assertNull(dto.originalStartTime)
    assertNull(dto.recurrenceRule)
  }

  @Test
  fun mapToDomain_handlesAllDayEvent() {
    val entity = CalendarEventEntity(
      id = "1",
      summary = "All Day Event",
      startTimeMillis = 1691452800000L, // начало дня
      endTimeMillis = 1691539200000L,   // следующий день
      description = null,
      location = null,
      isAllDay = true,
      recurringEventId = null,
      originalStartTimeString = null,
      recurrenceRuleString = null
    )

    `when`(dateTimeUtils.formatMillisToIsoString(entity.startTimeMillis, "UTC"))
      .thenReturn("2025-08-08T00:00:00Z")
    `when`(dateTimeUtils.formatMillisToIsoString(entity.endTimeMillis, "UTC"))
      .thenReturn("2025-08-09T00:00:00Z")

    val dto = eventMapper.mapToDomain(entity, "UTC")

    assertEquals("All Day Event", dto.summary)
    assertEquals("2025-08-08T00:00:00Z", dto.startTime)
    assertEquals("2025-08-09T00:00:00Z", dto.endTime)
    assertTrue(dto.isAllDay)
  }
}

