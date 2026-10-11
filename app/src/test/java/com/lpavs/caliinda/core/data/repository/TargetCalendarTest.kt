package com.lpavs.caliinda.core.data.repository

import com.lpavs.caliinda.core.data.calendar.model.DeviceCalendar
import org.junit.Assert.assertEquals
import org.junit.Test

class TargetCalendarTest {
  private fun calendar(id: Long, primary: Boolean = false, type: String = "com.google") =
      DeviceCalendar(id, "c$id", "me", type, color = 0, isPrimary = primary)

  private val main = calendar(1, primary = true)
  private val work = calendar(2)
  private val local = calendar(3, type = "LOCAL")

  @Test
  fun `сохранённый показанный календарь остаётся`() {
    val shown = shownCalendars(listOf(main, work, local), hidden = emptySet())
    assertEquals(work, targetCalendar(shown, saved = 2))
  }

  @Test
  fun `скрытый сохранённый — автоподбор среди оставшихся`() {
    val shown = shownCalendars(listOf(main, work, local), hidden = setOf(2))
    assertEquals(main, targetCalendar(shown, saved = 2))
  }

  @Test
  fun `скрыт основной — первый из оставшихся`() {
    val shown = shownCalendars(listOf(main, work, local), hidden = setOf(1, 2))
    assertEquals(local, targetCalendar(shown, saved = 1))
  }

  @Test
  fun `скрыты все — выбираем из всех, а не создаём локальный`() {
    val all = listOf(main, work)
    assertEquals(all, shownCalendars(all, hidden = setOf(1, 2)))
    assertEquals(work, targetCalendar(shownCalendars(all, setOf(1, 2)), saved = 2))
  }
}
