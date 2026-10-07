package com.lpavs.caliinda.feature.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.PI
import kotlin.math.sin

/**
 * Волнистый прогресс как `LinearWavyProgressIndicator` в приложении: пройденное — волной,
 * остаток — прямой дорожкой. В RemoteViews его нет, поэтому рисуем картинкой: белым, дорожка
 * полупрозрачной — tint виджета красит обе части, сохраняя прозрачность.
 */
fun wavyProgressBitmap(widthPx: Int, heightPx: Int, progress: Float): Bitmap {
  val stroke = heightPx * 0.32f
  val amplitude = (heightPx - stroke) / 2f
  val wavelength = heightPx * 2.6f
  val mid = heightPx / 2f
  val start = stroke / 2f
  val end = widthPx - stroke / 2f
  val split = start + (end - start) * progress.coerceIn(0f, 1f)
  // Зазор между волной и дорожкой, как в M3.
  val gap = stroke * 1.5f

  val paint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
      }
  return Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888).also { bitmap ->
    val canvas = Canvas(bitmap)
    if (split + gap < end) {
      canvas.drawLine(
          if (split > start) split + gap else start,
          mid,
          end,
          mid,
          Paint(paint).apply { alpha = 70 })
    }
    if (split > start) {
      val wave = Path().apply { moveTo(start, mid) }
      var x = start
      while (x < split) {
        x = minOf(x + 1f, split)
        wave.lineTo(x, mid + amplitude * sin(2 * PI * (x - start) / wavelength).toFloat())
      }
      canvas.drawPath(wave, paint)
    }
  }
}
