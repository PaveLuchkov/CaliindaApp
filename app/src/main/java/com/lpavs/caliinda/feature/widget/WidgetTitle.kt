package com.lpavs.caliinda.feature.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.icu.text.BreakIterator
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.CharacterStyle
import androidx.core.content.res.ResourcesCompat
import com.lpavs.caliinda.R
import kotlin.math.ceil

/**
 * Заголовок, нарисованный картинкой: размеры в dp, чтобы виджет показал её без масштабирования.
 * [glyphs] — буквы белым, под tint цветом темы; [emoji] — цветные эмодзи того же размера, их
 * кладут поверх без tint (иначе tint превращает эмодзи в одноцветные силуэты).
 */
class WidgetTitle(
    val glyphs: Bitmap,
    val emoji: Bitmap?,
    val widthDp: Float,
    val heightDp: Float,
    /** Каким размером в итоге нарисовано: по нему можно решить, не взять ли текст короче. */
    val textSizeSp: Float,
    val ellipsized: Boolean,
)

/**
 * В RemoteViews нельзя подключить свой шрифт, а системные варианты Roboto либо обычные, либо
 * сплющенные. Поэтому название рисуем картинкой нашим RobotoFlex с осями, как у заголовков в
 * приложении: жирно и шире обычного. Если не влезает — сначала сужаем, потом уменьшаем, потом
 * переносим на вторую строку (если хватает [maxHeightDp]), и только в конце многоточие.
 */
fun widgetTitle(
    context: Context,
    text: String,
    maxWidthDp: Float,
    maxHeightDp: Float,
    textSizeSp: Float,
    minTextSizeSp: Float = 16f,
): WidgetTitle {
  val metrics = context.resources.displayMetrics
  val typeface = ResourcesCompat.getFont(context, R.font.robotoflex_variable)
  val maxWidthPx = (maxWidthDp * metrics.density).toInt().coerceAtLeast(1)
  val maxHeightPx = maxHeightDp * metrics.density

  fun paint(sizeSp: Float, width: Int) =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sizeSp * metrics.scaledDensity
        this.typeface = typeface
        fontVariationSettings = "'wght' 780, 'wdth' $width"
      }

  // Варианты от самого выразительного к самому скромному.
  val variants = buildList {
    add(textSizeSp to 125)
    var size = textSizeSp
    while (size >= minTextSizeSp) {
      add(size to 100)
      size -= 2f
    }
  }
  val layout =
      // Сначала пробуем уместить в одну строку, потом в две.
      (1..2).firstNotNullOfOrNull { lines ->
        variants.firstNotNullOfOrNull { (size, width) ->
          titleLayout(text, paint(size, width), maxWidthPx, lines).takeIf {
            !it.isEllipsized() && it.height <= maxHeightPx
          }
        }
      }
          ?: titleLayout(text, paint(minTextSizeSp, 100), maxWidthPx, maxLines = 1)

  val ranges = emojiRanges(text)
  val width =
      ceil((0 until layout.lineCount).maxOf { layout.getLineWidth(it) }).toInt().coerceAtLeast(1)
  val height = layout.height.coerceAtLeast(1)
  val glyphs = drawLayer(layout, text, width, height, hide = ranges, invert = false)
  val emoji =
      if (ranges.isEmpty()) null else drawLayer(layout, text, width, height, ranges, invert = true)
  return WidgetTitle(
      glyphs,
      emoji,
      width / metrics.density,
      height / metrics.density,
      layout.paint.textSize / metrics.scaledDensity,
      layout.isEllipsized())
}

private fun titleLayout(text: CharSequence, paint: TextPaint, widthPx: Int, maxLines: Int) =
    StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()

private fun StaticLayout.isEllipsized() = getEllipsisCount(lineCount - 1) > 0

/**
 * Рисует ту же раскладку, но прячет часть символов прозрачностью: метрики от неё не меняются,
 * так что оба слоя совпадают пиксель в пиксель. [invert] — прятать всё, кроме [hide].
 */
private fun drawLayer(
    layout: StaticLayout,
    text: String,
    width: Int,
    height: Int,
    hide: List<IntRange>,
    invert: Boolean,
): Bitmap {
  val spanned = SpannableString(text)
  val hidden =
      if (!invert) hide
      else
          buildList {
            var start = 0
            hide.forEach {
              if (it.first > start) add(start until it.first)
              start = it.last + 1
            }
            if (start < text.length) add(start until text.length)
          }
  hidden.forEach {
    spanned.setSpan(Invisible(), it.first, it.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
  }
  val paint = TextPaint(layout.paint)
  val layer =
      StaticLayout.Builder.obtain(spanned, 0, spanned.length, paint, layout.width)
          .setAlignment(Layout.Alignment.ALIGN_NORMAL)
          .setIncludePad(false)
          .setMaxLines(layout.lineCount)
          .setEllipsize(TextUtils.TruncateAt.END)
          .build()
  return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
    layer.draw(Canvas(it))
  }
}

private class Invisible : CharacterStyle() {
  override fun updateDrawState(tp: TextPaint) {
    tp.alpha = 0
  }
}

/** Диапазоны графем, которые система нарисует цветным эмодзи. */
internal fun emojiRanges(text: String): List<IntRange> {
  val iterator = BreakIterator.getCharacterInstance().apply { setText(text) }
  val result = mutableListOf<IntRange>()
  var start = iterator.first()
  var end = iterator.next()
  while (end != BreakIterator.DONE) {
    val cluster = text.substring(start, end)
    if (cluster.codePoints().anyMatch(::isEmojiCodePoint)) {
      // Соседние эмодзи склеиваем в один диапазон.
      val last = result.lastOrNull()
      if (last != null && last.last + 1 == start) result[result.lastIndex] = last.first until end
      else result += start until end
    }
    start = end
    end = iterator.next()
  }
  return result
}

// Только то, что по умолчанию рисуется цветным: «☺» или «©» без FE0F остаются буквами.
private fun isEmojiCodePoint(cp: Int) =
    UCharacter.hasBinaryProperty(cp, UProperty.EMOJI_PRESENTATION) ||
        UCharacter.hasBinaryProperty(cp, UProperty.REGIONAL_INDICATOR) ||
        cp == 0xFE0F ||
        cp == 0x20E3
