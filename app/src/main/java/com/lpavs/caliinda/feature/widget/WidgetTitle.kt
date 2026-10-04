package com.lpavs.caliinda.feature.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.lpavs.caliinda.R
import kotlin.math.ceil

/** Заголовок, нарисованный картинкой: размеры в dp, чтобы виджет показал её без масштабирования. */
class WidgetTitle(val bitmap: Bitmap, val widthDp: Float, val heightDp: Float)

/**
 * В RemoteViews нельзя подключить свой шрифт, а системные варианты Roboto либо обычные, либо
 * сплющенные. Поэтому название рисуем картинкой нашим RobotoFlex с осями, как у заголовков в
 * приложении: жирно и шире обычного. Белым — цвет темы накладывается в виджете через tint.
 */
fun widgetTitle(context: Context, text: String, maxWidthDp: Float, textSizeSp: Float): WidgetTitle {
  val metrics = context.resources.displayMetrics
  val paint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = textSizeSp * metrics.scaledDensity
        typeface = ResourcesCompat.getFont(context, R.font.robotoflex_variable)
        fontVariationSettings = "'wght' 780, 'wdth' 125"
      }
  val maxWidthPx = maxWidthDp * metrics.density
  val line = TextUtils.ellipsize(text, paint, maxWidthPx, TextUtils.TruncateAt.END).toString()
  val fm = paint.fontMetrics
  val width = ceil(paint.measureText(line)).toInt().coerceAtLeast(1)
  val height = ceil(fm.descent - fm.ascent).toInt().coerceAtLeast(1)
  val bitmap =
      Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
        Canvas(it).drawText(line, 0f, -fm.ascent, paint)
      }
  return WidgetTitle(bitmap, width / metrics.density, height / metrics.density)
}
