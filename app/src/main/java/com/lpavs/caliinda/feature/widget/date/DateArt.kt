package com.lpavs.caliinda.feature.widget.date

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.core.content.res.ResourcesCompat
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.lpavs.caliinda.R
import java.time.LocalDate
import kotlin.math.max

// Картинки виджета даты. Фигуру и цифру рисуем белым — цвет темы накладывается в виджете через
// tint, так виджет сам перекрашивается при смене темы. Эмодзи — цветной, без tint.

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val dayShapes by lazy {
  // Только «круглые» фигуры: в них хорошо сидит цифра.
  listOf(
      MaterialShapes.Cookie9Sided,
      MaterialShapes.Sunny,
      MaterialShapes.Flower,
      MaterialShapes.Cookie12Sided,
      MaterialShapes.VerySunny,
      MaterialShapes.Clover8Leaf,
      MaterialShapes.Cookie7Sided,
      MaterialShapes.SoftBurst,
  )
}

/** Фигура дня: своя на каждый день, по кругу. Квадрат [sidePx]. */
internal fun dayShapeBitmap(date: LocalDate, sidePx: Int): Bitmap =
    polygonBitmap(
        dayShapes[Math.floorMod(date.toEpochDay(), dayShapes.size.toLong()).toInt()],
        sidePx,
        // Лёгкий поворот от дня к дню — фигура «живая», даже если повторилась.
        rotation = (date.dayOfYear * 37 % 60 - 30).toFloat())

/** Фигура [polygon] на весь квадрат [sidePx], белым — под tint. */
internal fun polygonBitmap(polygon: RoundedPolygon, sidePx: Int, rotation: Float = 0f): Bitmap {
  val path = polygon.toPath()
  // Сначала поворачиваем, потом вписываем: иначе углы повёрнутой фигуры вылезут за квадрат.
  path.transform(Matrix().apply { postRotate(rotation) })
  val bounds = RectF().also { path.computeBounds(it, true) }
  val scale = sidePx / max(bounds.width(), bounds.height())
  path.transform(
      Matrix().apply {
        postTranslate(-bounds.centerX(), -bounds.centerY())
        postScale(scale, scale)
        postTranslate(sidePx / 2f, sidePx / 2f)
      })
  return Bitmap.createBitmap(sidePx, sidePx, Bitmap.Config.ARGB_8888).also {
    Canvas(it).drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
  }
}

/** Эмодзи по центру квадрата [sidePx] — вместо числа, когда считать уже нечего. */
internal fun emojiInShapeBitmap(emoji: String, sidePx: Int): Bitmap {
  val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = sidePx * 0.45f }
  val bounds = Rect().also { paint.getTextBounds(emoji, 0, emoji.length, it) }
  return Bitmap.createBitmap(sidePx, sidePx, Bitmap.Config.ARGB_8888).also {
    Canvas(it)
        .drawText(
            emoji, sidePx / 2f - bounds.exactCenterX(), sidePx / 2f - bounds.exactCenterY(), paint)
  }
}

/**
 * Число месяца, по центру квадрата [sidePx] того же размера, что и фигура. Центруем по
 * настоящим границам цифр, а не по строке: иначе цифра съезжает вверх.
 */
internal fun dayNumberBitmap(
    context: Context,
    day: Int,
    sidePx: Int,
    /** Доля фигуры под цифры: у «листовых» фигур середина уже, число должно быть мельче. */
    fill: Float = 1f,
): Bitmap {
  val text = day.toString()
  // Три цифры (отсчёт на сотни дней) — узко, иначе выйдут мелкими.
  val paint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = ResourcesCompat.getFont(context, R.font.robotoflex_variable)
        // Одна цифра — широкая и тяжёлая, две — поуже, чтобы влезли в фигуру крупно.
        fontVariationSettings =
            when (text.length) {
              1 -> "'wght' 1000, 'wdth' 151"
              2 -> "'wght' 1000, 'wdth' 100"
              else -> "'wght' 1000, 'wdth' 75"
            }
        textSize = 100f
      }
  val bounds = Rect().also { paint.getTextBounds(text, 0, text.length, it) }
  // Цифра занимает ~половину фигуры по высоте и не шире 64% — фигура вокруг читается как рамка.
  val scale = minOf(sidePx * 0.48f * fill / bounds.height(), sidePx * 0.64f * fill / bounds.width())
  paint.textSize *= scale
  paint.getTextBounds(text, 0, text.length, bounds)
  return Bitmap.createBitmap(sidePx, sidePx, Bitmap.Config.ARGB_8888).also {
    Canvas(it)
        .drawText(
            text,
            sidePx / 2f - bounds.exactCenterX(),
            sidePx / 2f - bounds.exactCenterY(),
            paint)
  }
}

/**
 * Эмодзи дня на весь виджет: крупный, повёрнутый, полупрозрачный и наполовину за краем — как
 * фигура, выглядывающая из карточки события.
 */
internal fun dayEmojiBitmap(emoji: String, widthPx: Int, heightPx: Int): Bitmap {
  val side = minOf(widthPx, heightPx)
  val paint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = side * 0.5f
        alpha = 80
      }
  val bounds = Rect().also { paint.getTextBounds(emoji, 0, emoji.length, it) }
  return Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888).also {
    Canvas(it).apply {
      translate(widthPx - side * 0.1f, side * 0.12f)
      rotate(18f)
      drawText(emoji, -bounds.exactCenterX(), -bounds.exactCenterY(), paint)
    }
  }
}
