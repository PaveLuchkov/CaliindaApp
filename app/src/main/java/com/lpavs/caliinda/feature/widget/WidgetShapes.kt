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

private const val SHAPE_HEIGHT_PX = 192

/**
 * Glance не умеет обрезать по фигурам и рисовать внутреннюю тень, поэтому фигуру карточки рисуем
 * в картинку под размер виджета ([aspect] = ширина / высота) так же, как она выглядит в
 * приложении: крупная, у правого края, выше карточки и обрезана ею, видна только «тень» по
 * кромке — фигура минус её же копия со сдвигом. Цвет темы накладывается через tint.
 */
fun widgetShapeBitmap(eventId: String, aspect: Float, rotation: Float = -18f): Bitmap {
  val height = SHAPE_HEIGHT_PX
  val width = (height * aspect.coerceIn(1f, 8f)).toInt()
  val polygon: RoundedPolygon = projectFamilyPolygon(eventId)
  val path = polygon.toPath()
  val bounds = RectF().also { path.computeBounds(it, true) }
  val scale = height * 1.7f / max(bounds.width(), bounds.height())
  path.transform(
      Matrix().apply {
        postTranslate(-bounds.centerX(), -bounds.centerY())
        postScale(scale, scale)
        postRotate(rotation)
        postTranslate(width - height * 0.55f, height * 0.65f)
      })
  return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
    val canvas = Canvas(bitmap)
    canvas.drawPath(
        path,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.WHITE
          alpha = 90
        })
    canvas.save()
    canvas.translate(height * 0.045f, height * 0.065f)
    canvas.drawPath(
        path, Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) })
    canvas.restore()
  }
}
