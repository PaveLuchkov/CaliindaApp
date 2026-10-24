package com.lpavs.caliinda.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

/**
 * Модули QR-кода в стиле приложения: данные — точками, три «глазка» — скруглёнными квадратами.
 * Отдельно от отрисовки — чтобы тест рисовал тот же стиль и проверял, что код читается.
 */
class QrLayout(
    /** Сторона кода в модулях. */
    val size: Int,
    /** Модули данных (x, y) — без «глазков», их рисуем целиком. */
    val dots: List<Pair<Int, Int>>,
    /** Левые верхние углы трёх «глазков» 7×7. */
    val finders: List<Pair<Int, Int>>,
)

fun qrLayout(text: String): QrLayout {
  // Уровень M (15%): Q надёжнее, но код плотнее — на экране, вблизи камеры, хватает M.
  val matrix =
      Encoder.encode(text, ErrorCorrectionLevel.M, mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")).matrix
  val n = matrix.width
  val finders = listOf(0 to 0, n - FINDER to 0, 0 to n - FINDER)
  fun inFinder(x: Int, y: Int) =
      finders.any { (fx, fy) -> x in fx until fx + FINDER && y in fy until fy + FINDER }
  val dots = buildList {
    for (y in 0 until n) for (x in 0 until n) {
      if (matrix.get(x, y).toInt() == 1 && !inFinder(x, y)) add(x to y)
    }
  }
  return QrLayout(n, dots, finders)
}

const val FINDER = 7
/** Радиус точки — доля модуля: чуть меньше половины, чтобы точки не слипались. */
const val QR_DOT_RADIUS = 0.42f

/** QR без своего фона и полей: тихая зона — фон, на котором он лежит. */
@Composable
fun QrCode(text: String, color: Color, modifier: Modifier = Modifier) {
  val layout = remember(text) { qrLayout(text) }
  Canvas(modifier) {
    val cell = size.minDimension / layout.size
    layout.dots.forEach { (x, y) ->
      drawCircle(color, radius = cell * QR_DOT_RADIUS, center = Offset((x + 0.5f) * cell, (y + 0.5f) * cell))
    }
    layout.finders.forEach { (fx, fy) ->
      // Рамка 7×7 толщиной в модуль: обводка по середине крайних модулей.
      drawRoundRect(
          color,
          topLeft = Offset((fx + 0.5f) * cell, (fy + 0.5f) * cell),
          size = Size(6 * cell, 6 * cell),
          cornerRadius = CornerRadius(2 * cell),
          style = Stroke(width = cell))
      drawRoundRect(
          color,
          topLeft = Offset((fx + 2) * cell, (fy + 2) * cell),
          size = Size(3 * cell, 3 * cell),
          cornerRadius = CornerRadius(cell))
    }
  }
}
