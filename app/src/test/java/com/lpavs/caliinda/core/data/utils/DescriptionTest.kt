package com.lpavs.caliinda.core.data.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DescriptionTest {

  @Test
  fun `html из Outlook — в читаемый текст`() {
    val html =
        """<html><head><style>p { margin: 0 } .x { color: red }</style></head>
        <body><div class="x">Привет,&nbsp;команда!</div>
        <p>Повестка:</p><ul><li>Бюджет</li><li>Найм &amp; онбординг</li></ul>
        <!-- служебное --><br>Ссылка: &lt;https://example.com&gt;</body></html>"""
    assertEquals(
        "Привет, команда!\nПовестка:\n• Бюджет\n• Найм & онбординг\n\nСсылка: <https://example.com>",
        readableDescription(html))
  }

  @Test
  fun `обычный текст не трогаем, кроме разделителей и лишних пустых строк`() {
    val text = "Обсудить план\n\n\n\nВзять ноутбук\n________________________________\nJoin Teams"
    assertEquals("Обсудить план\n\nВзять ноутбук\nJoin Teams", readableDescription(text))
    assertEquals("a < b и c > d", readableDescription("a < b и c > d"))
  }

  @Test
  fun `сущности — включая числовые, двойное экранирование не раскрываем дважды`() {
    assertEquals("Café — 5 €", readableDescription("<p>Caf&#233; &#x2014; 5 &#8364;</p>"))
    assertEquals("&lt;", readableDescription("<p>&amp;lt;</p>"))
  }
}
