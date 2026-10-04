package com.lpavs.caliinda.feature.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.lpavs.caliinda.core.ui.util.projectFamilyPolygon
import kotlin.math.max

private const val SHAPE_PX = 256

/**
 * Glance не умеет обрезать по фигурам и рисовать внутреннюю тень, поэтому фигуру карточки рисуем
 * в картинку так же, как она выглядит в приложении: крупная, частично за краем, видна только
 * «тень» по кромке — фигура минус её же копия со сдвигом. Цвет темы накладывается через tint.
 */
fun widgetShapeBitmap(eventId: String, rotation: Float = -18f): Bitmap {
  val polygon: RoundedPolygon = projectFamilyPolygon(eventId)
  val path = polygon.toPath()
  val bounds = RectF().also { path.computeBounds(it, true) }
  val scale = SHAPE_PX / max(bounds.width(), bounds.height()) * 1.35f
  path.transform(
      Matrix().apply {
        postTranslate(-bounds.centerX(), -bounds.centerY())
        postScale(scale, scale)
        postRotate(rotation)
        // Сдвигаем вправо-вниз: край фигуры уходит за кадр, как на карточках событий.
        postTranslate(SHAPE_PX * 0.72f, SHAPE_PX * 0.62f)
      })
  return Bitmap.createBitmap(SHAPE_PX, SHAPE_PX, Bitmap.Config.ARGB_8888).also { bitmap ->
    val canvas = Canvas(bitmap)
    canvas.drawPath(
        path,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.WHITE
          alpha = 90
        })
    canvas.save()
    canvas.translate(SHAPE_PX * 0.035f, SHAPE_PX * 0.05f)
    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) })
    canvas.restore()
  }
}
