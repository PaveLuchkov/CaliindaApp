package com.lpavs.caliinda.feature.live

import com.lpavs.caliinda.core.data.calendar.model.EventDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class LiveEventPlanTest {
  private fun at(hm: String) = Instant.parse("2026-10-05T${hm}:00Z")

  private fun event(id: String, from: String, to: String, allDay: Boolean = false) =
      EventDto(id = id, summary = id, startTime = at(from), endTime = at(to), isAllDay = allDay)

  @Test
  fun `ничего не идёт — без уведомления, пересчёт к началу следующего`() {
    val plan = planLiveEvent(listOf(event("a", "12:00", "13:00")), at("10:00"))
    assertNull(plan.current)
    assertEquals(at("12:00"), plan.refreshAt)
  }

  @Test
  fun `идёт событие — следующее вплотную показываем, прогресс обновляем по шагу`() {
    val a = event("a", "10:00", "11:00")
    val b = event("b", "11:10", "12:00")
    val plan = planLiveEvent(listOf(b, a), at("10:20"))
    assertEquals(a, plan.current)
    assertEquals(b, plan.next)
    assertEquals(at("10:25"), plan.refreshAt)
  }

  @Test
  fun `следующее далеко — не показываем, пересчёт к концу текущего`() {
    val a = event("a", "10:00", "11:00")
    val plan = planLiveEvent(listOf(a, event("b", "13:00", "14:00")), at("10:58"))
    assertNull(plan.next)
    assertEquals(at("11:00"), plan.refreshAt)
  }

  @Test
  fun `весь день и пересечения`() {
    val allDay = event("day", "00:00", "23:59", allDay = true)
    val long = event("long", "09:00", "12:00")
    val short = event("short", "10:00", "10:30")
    val plan = planLiveEvent(listOf(allDay, long, short), at("10:10"))
    // Из идущих — то, что закончится раньше; «весь день» не в счёт.
    assertEquals(short, plan.current)
  }
}
