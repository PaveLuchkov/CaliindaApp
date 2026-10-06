@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.core.ui.theme.AppMotion

/** Ячейка блока даты и времени. [highlighted] — сегодняшняя дата (роль tertiary). */
class DateTimeCell(
    val text: String,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
    val highlighted: Boolean = false,
)

/**
 * Время и дата одной плиткой на всю ширину: строка времени (если не «весь день») и строка
 * дат — одна ячейка или пара «начало | конец». Ширина не зависит от режима, меняется только
 * число строк и ячеек. Скругления — как у групп кнопок: крупные снаружи, мелкие на стыках.
 */
@Composable
fun DateTimeBlock(
    times: List<DateTimeCell>?,
    dates: List<DateTimeCell>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
  // Пока строка времени уезжает, times уже null — держим прежние ячейки, чтобы не мигала пустота.
  var shownTimes by remember { mutableStateOf(times) }
  if (times != null) shownTimes = times
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GAP)) {
    AnimatedVisibility(
        visible = times != null,
        enter = fadeIn(AppMotion.fastEffectsSpec()) + expandVertically(AppMotion.defaultSpatialSpec()),
        exit = fadeOut(AppMotion.fastEffectsSpec()) + shrinkVertically(AppMotion.defaultSpatialSpec())) {
          shownTimes?.let {
            CellRow(it, top = true, bottom = false, height = 48.dp, typography.titleMedium, enabled)
          }
        }
    // Размер текста — в тон подписям кнопок формы, время лишь чуть заметнее дат.
    CellRow(dates, top = times == null, bottom = true, height = 40.dp, typography.labelLarge, enabled)
  }
}

@Composable
private fun CellRow(
    cells: List<DateTimeCell>,
    top: Boolean,
    bottom: Boolean,
    height: Dp,
    style: TextStyle,
    enabled: Boolean,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GAP)) {
    cells.forEachIndexed { index, cell ->
      Cell(
          cell = cell,
          shape =
              cellShape(
                  topStart = top && index == 0,
                  topEnd = top && index == cells.lastIndex,
                  bottomStart = bottom && index == 0,
                  bottomEnd = bottom && index == cells.lastIndex),
          style = style,
          enabled = enabled,
          modifier = Modifier.weight(1f).height(height))
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Cell(
    cell: DateTimeCell,
    shape: CornerBasedShape,
    style: TextStyle,
    enabled: Boolean,
    modifier: Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val container by
      animateColorAsState(
          if (cell.highlighted) colorScheme.tertiaryContainer else colorScheme.secondaryContainer,
          AppMotion.defaultEffectsSpec(),
          label = "cellContainer")
  val content by
      animateColorAsState(
          if (cell.highlighted) colorScheme.onTertiaryContainer else colorScheme.onSecondaryContainer,
          AppMotion.defaultEffectsSpec(),
          label = "cellContent")
  Box(
      modifier =
          modifier
              .clip(shape)
              .background(container)
              .combinedClickable(
                  enabled = enabled,
                  onClick = cell.onClick,
                  onLongClick =
                      cell.onLongClick?.let { long ->
                        {
                          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                          long()
                        }
                      }),
      contentAlignment = Alignment.Center) {
        Text(text = cell.text, style = style, color = content, maxLines = 1)
      }
}

/**
 * Скругление ячейки: снаружи плитки — extraLarge (в тон капсулам групп кнопок над и под
 * блоком), на стыках с соседями — extraSmall.
 */
@Composable
private fun cellShape(
    topStart: Boolean,
    topEnd: Boolean,
    bottomStart: Boolean,
    bottomEnd: Boolean,
): CornerBasedShape {
  val outer = shapes.extraLarge.topStart
  val inner = shapes.extraSmall.topStart
  return shapes.extraLarge.copy(
      topStart = if (topStart) outer else inner,
      topEnd = if (topEnd) outer else inner,
      bottomStart = if (bottomStart) outer else inner,
      bottomEnd = if (bottomEnd) outer else inner)
}

private val GAP = 2.dp
