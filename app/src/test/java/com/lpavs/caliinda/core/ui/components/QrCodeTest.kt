package com.lpavs.caliinda.core.ui.components

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlin.math.floor
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Test

/** Рисуем QR тем же стилем, что и QrCode (точки, скруглённые «глазки»), и читаем его ZXing. */
class QrCodeTest {

  @Test
  fun `фигурный QR со ссылкой на событие читается`() {
    val link =
        "https://calendar.google.com/calendar/render?action=TEMPLATE" +
            "&text=%D0%9E%D0%B1%D0%B5%D0%B4%20%D1%81%20%D0%BA%D0%BE%D0%BC%D0%B0%D0%BD%D0%B4%D0%BE%D0%B9" +
            "&dates=20261024T130000Z/20261024T143000Z"
    assertEquals(link, decode(render(qrLayout(link))))
  }

  /** Растр в пикселях (true — модуль): каждый пиксель проверяем на попадание в точку или «глазок». */
  private fun render(layout: QrLayout, cell: Int = 10, quiet: Int = 4): Pair<Int, IntArray> {
    val dots = layout.dots.toHashSet()
    val side = (layout.size + quiet * 2) * cell
    val pixels = IntArray(side * side) { WHITE }
    for (py in 0 until side) for (px in 0 until side) {
      // Центр пикселя — в единицах модуля, от угла кода.
      val x = (px + 0.5f) / cell - quiet
      val y = (py + 0.5f) / cell - quiet
      val mx = floor(x).toInt()
      val my = floor(y).toInt()
      val dot =
          (mx to my) in dots &&
              (x - mx - 0.5f).let { it * it } + (y - my - 0.5f).let { it * it } <= QR_DOT_RADIUS * QR_DOT_RADIUS
      val finder =
          layout.finders.any { (fx, fy) ->
            // Рамка — как обводка в QrCode: между прямоугольниками 7×7 (r=2,5) и 5×5 (r=1,5).
            (inRound(x, y, fx.toFloat(), fy.toFloat(), 7f, 2.5f) &&
                !inRound(x, y, fx + 1f, fy + 1f, 5f, 1.5f)) || inRound(x, y, fx + 2f, fy + 2f, 3f, 1f)
          }
      if (dot || finder) pixels[py * side + px] = BLACK
    }
    return side to pixels
  }

  private fun inRound(x: Float, y: Float, left: Float, top: Float, size: Float, r: Float): Boolean {
    if (x < left || y < top || x > left + size || y > top + size) return false
    val dx = max(max(left + r - x, x - (left + size - r)), 0f)
    val dy = max(max(top + r - y, y - (top + size - r)), 0f)
    return dx * dx + dy * dy <= r * r
  }

  private fun decode(image: Pair<Int, IntArray>): String {
    val (side, pixels) = image
    val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(side, side, pixels)))
    return QRCodeReader().decode(bitmap, mapOf(DecodeHintType.CHARACTER_SET to "UTF-8")).text
  }

  private companion object {
    const val WHITE = 0xFFFFFFFF.toInt()
    const val BLACK = 0xFF000000.toInt()
  }
}
