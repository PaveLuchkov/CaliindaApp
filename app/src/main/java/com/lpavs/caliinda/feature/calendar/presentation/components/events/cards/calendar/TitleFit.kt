package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar

import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.cuid
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Как показать название в карточке заданного размера. */
data class TitleFit(
    val fontFamily: FontFamily,
    /** Сколько строк влезает в свёрнутую карточку. */
    val maxLines: Int,
    /** Строк в раскрытой карточке — полное название, но не больше лимита. */
    val expandedLines: Int,
    /** Сколько высоты добавить раскрытой карточке под эти строки. */
    val expandedExtra: Dp,
)

/**
 * Высота карточки задана длительностью (DESIGN.md), поэтому под неё подстраивается текст:
 * сначала столько строк, сколько влезает по высоте; не хватило — то же начертание, но уже
 * ([condensed]); раскрытая карточка дорастает до полного названия, но не больше
 * [MAX_EXTRA_LINES] строк.
 */
@Composable
fun rememberTitleFit(
    title: String,
    style: TextStyle,
    regular: FontFamily,
    condensed: FontFamily,
    width: Dp,
    titleHeight: Dp,
): TitleFit {
  val measurer = rememberTextMeasurer()
  val density = LocalDensity.current
  return remember(title, style, regular, condensed, width, titleHeight, density) {
    val widthPx = with(density) { width.roundToPx() }.coerceAtLeast(1)
    val constraints = Constraints(maxWidth = widthPx)
    fun measure(family: FontFamily, maxLines: Int = Int.MAX_VALUE) =
        measurer.measure(
            title, style.copy(fontFamily = family), maxLines = maxLines, constraints = constraints)

    val full = measure(regular)
    val lineHeight = with(density) { (full.size.height / full.lineCount.coerceAtLeast(1)).toDp() }
    val lines = (titleHeight / lineHeight).toInt().coerceAtLeast(1)
    val family =
        if (full.lineCount > lines && measure(condensed).lineCount < full.lineCount) condensed
        else regular
    val needed = if (family == regular) full.lineCount else measure(condensed).lineCount
    val extraLines = (needed - lines).coerceIn(0, MAX_EXTRA_LINES)
    TitleFit(family, lines, lines + extraLines, if (extraLines == 0) 0.dp else lineHeight * extraLines)
  }
}

private const val MAX_EXTRA_LINES = 3

/**
 * [rememberTitleFit] для карточки дня или проекта: ширина — карточка [cardMaxWidth] без
 * отступов (у короткой — ещё и без времени справа), высота — без строки времени под названием.
 */
@Composable
fun rememberCardTitleFit(
    title: String,
    timeText: String,
    micro: Boolean,
    baseHeight: Dp,
    cardMaxWidth: Dp,
    style: TextStyle,
    regular: FontFamily,
    condensed: FontFamily,
): TitleFit {
  val density = LocalDensity.current
  val measurer = rememberTextMeasurer()
  val timeStyle = typography.labelMedium
  val smallLine = typography.labelSmall.lineHeight
  val timeWidth =
      remember(micro, timeText, timeStyle, density) {
        if (!micro) 0.dp
        else with(density) { measurer.measure(timeText, timeStyle).size.width.toDp() } + cuid.padding
      }
  val width =
      cardMaxWidth - CalendarUiDefaults.ItemHorizontalPadding * 2 -
          cuid.ItemHorizontalPadding * 2 - timeWidth
  val height =
      if (micro) baseHeight - cuid.MicroItemContentVerticalPadding * 2
      else
          baseHeight - cuid.StandardItemContentVerticalPadding * 2 - 2.dp -
              with(density) { smallLine.toDp() }
  return rememberTitleFit(title, style, regular, condensed, width, height)
}
