package com.lpavs.caliinda.core.data.utils

import com.lpavs.caliinda.core.data.utils.EventPlace.Address
import com.lpavs.caliinda.core.data.utils.EventPlace.Link
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeetingPlaceTest {

  @Test
  fun `ссылки на созвоны узнаются по домену, в том числе поддомену`() {
    val zoom = eventPlace("https://us02web.zoom.us/j/123456789?pwd=abc", null) as Link
    assertEquals(MeetingService.ZOOM, zoom.service)
    assertEquals("us02web.zoom.us", zoom.host)
    assertEquals(MeetingService.MEET, (eventPlace("meet.google.com/abc-defg-hij", null) as Link).service)
    assertEquals(
        "https://meet.google.com/abc-defg-hij",
        (eventPlace("meet.google.com/abc-defg-hij", null) as Link).url)
    assertEquals(
        MeetingService.TELEMOST,
        (eventPlace("Созвон: https://telemost.yandex.ru/j/1234", null) as Link).service)
  }

  @Test
  fun `обычная ссылка — с доменом, без сервиса`() {
    val link = eventPlace("https://www.example.com/event.", null) as Link
    assertEquals(null, link.service)
    assertEquals("example.com", link.host)
    assertEquals("https://www.example.com/event", link.url)
  }

  @Test
  fun `адрес остаётся адресом`() {
    assertEquals(Address("ул. Ленина, 5"), eventPlace("ул. Ленина, 5", null))
    assertEquals(Address("Кафе в 10.30"), eventPlace("Кафе в 10.30", null))
    assertEquals(Address("mail me: a@b.com"), eventPlace("mail me: a@b.com", null))
  }

  @Test
  fun `пустое место — ссылка на созвон из описания, но не любая`() {
    val desc = "Повестка https://docs.google.com/x\nJoin: https://meet.google.com/abc-defg-hij"
    assertEquals(MeetingService.MEET, (eventPlace("  ", desc) as Link).service)
    assertNull(eventPlace(null, "Почитать https://example.com/article"))
    assertNull(eventPlace(null, null))
  }
}
