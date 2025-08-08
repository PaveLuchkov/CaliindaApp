package com.lpavs.caliinda.core.ui.util

import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

class DateTimeUtilsImplTest {

  private lateinit var dateTimeUtils: DateTimeUtilsImpl

  @Before
  fun setUp() {
    dateTimeUtils = DateTimeUtilsImpl()
  }

  @Test
  fun `parseToInstant с полной ISO строкой Z возвращает корректный Instant`() {
    val dateTimeString = "2025-08-10T15:30:00Z"
    val expectedInstant = Instant.parse(dateTimeString)

    val result = dateTimeUtils.parseToInstant(dateTimeString, "UTC")

    assertEquals(expectedInstant, result)
  }

  @Test
  fun `parseToInstant с LocalDateTime строкой и часовым поясом возвращает корректный Instant`() {
    val dateTimeString = "2025-08-10T18:30:00"
    val zoneIdString = "Europe/Moscow"
    val expectedInstant = Instant.parse("2025-08-10T15:30:00Z")

    val result = dateTimeUtils.parseToInstant(dateTimeString, zoneIdString)

    assertEquals(expectedInstant, result)
  }

  @Test
  fun `parseToInstant с LocalDate строкой возвращает начало дня в UTC`() {
    val dateString = "2025-08-10"
    val expectedInstant = Instant.parse("2025-08-10T00:00:00Z")

    val result = dateTimeUtils.parseToInstant(dateString, "AnyZone")

    assertEquals(expectedInstant, result)
  }

  @Test
  fun `parseToInstant с null строкой возвращает null`() {
    val dateTimeString: String? = null

    val result = dateTimeUtils.parseToInstant(dateTimeString, "UTC")

    assertNull(result)
  }

  @Test
  fun `parseToInstant с пустой строкой возвращает null`() {
    val dateTimeString = ""

    val result = dateTimeUtils.parseToInstant(dateTimeString, "UTC")

    assertNull(result)
  }

  @Test
  fun `parseToInstant с невалидной строкой возвращает null`() {
    val dateTimeString = "это не дата"

    val result = dateTimeUtils.parseToInstant(dateTimeString, "UTC")

    assertNull(result)
  }
}
