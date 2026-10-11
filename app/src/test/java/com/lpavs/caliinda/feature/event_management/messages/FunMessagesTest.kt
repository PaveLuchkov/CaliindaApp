package com.lpavs.caliinda.feature.event_management.messages

import com.lpavs.caliinda.core.data.utils.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FunMessagesTest {
  private val messages = FunMessagesImpl()

  @Test
  fun `название подставляется в фразу`() {
    val text = messages.getEventCreatedMessage("Йога") as UiText.StringResource
    assertEquals(listOf("Йога"), text.formatArgs)
  }

  @Test
  fun `без названия — фраза без аргументов`() {
    for (name in listOf(null, "", "  ")) {
      val text = messages.getEventDeletedMessage(name) as UiText.StringResource
      assertTrue(text.formatArgs.isEmpty())
    }
  }
}
