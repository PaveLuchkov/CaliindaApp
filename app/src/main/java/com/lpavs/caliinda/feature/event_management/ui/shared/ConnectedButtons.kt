@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.lpavs.caliinda.feature.event_management.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Связанная группа кнопок M3 Expressive с одним выбранным вариантом. */
@Composable
fun <T> ConnectedChoice(
    items: List<T>,
    isSelected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  ConnectedRow(modifier) {
    items.forEachIndexed { index, item ->
      ConnectedButton(
          checked = isSelected(item),
          onCheckedChange = { onSelect(item) },
          text = label(item),
          shapes = connectedShapes(index, items.lastIndex),
          role = Role.RadioButton,
          enabled = enabled,
          modifier = Modifier.weight(1f))
    }
  }
}

/** Вариант для независимых переключателей: каждую кнопку можно включить отдельно. */
class ConnectedToggle(val text: String, val checked: Boolean, val onToggle: () -> Unit)

/** Связанная группа независимых переключателей — как [ConnectedChoice], но без «одного из». */
@Composable
fun ConnectedToggles(
    toggles: List<ConnectedToggle>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  ConnectedRow(modifier) {
    toggles.forEachIndexed { index, toggle ->
      ConnectedButton(
          checked = toggle.checked,
          onCheckedChange = { toggle.onToggle() },
          text = toggle.text,
          shapes = connectedShapes(index, toggles.lastIndex),
          role = Role.Checkbox,
          enabled = enabled,
          modifier = Modifier.weight(1f))
    }
  }
}

@Composable
private fun ConnectedRow(
    modifier: Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
  Row(
      modifier = modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
      content = content)
}

@Composable
private fun ConnectedButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    shapes: ToggleButtonShapes,
    role: Role,
    enabled: Boolean,
    modifier: Modifier,
) {
  ToggleButton(
      checked = checked,
      onCheckedChange = onCheckedChange,
      shapes = shapes,
      // Невыбранная кнопка по умолчанию цвета surfaceContainer — как фон секции формы, и
      // группа читалась бы просто текстом. Highest — на ступень выше полей ввода.
      colors = ToggleButtonDefaults.toggleButtonColors(containerColor = colorScheme.surfaceContainerHighest),
      enabled = enabled,
      // Кнопки делят ширину поровну — внутренние поля поменьше, чтобы длинным подписям
      // («Toute la journée») хватало места.
      contentPadding = PaddingValues(horizontal = 8.dp),
      modifier = modifier.semantics { this.role = role }) {
        // Подпись ужимается под кнопку, а не обрезается многоточием.
        Text(
            text,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp))
      }
}

@Composable
private fun connectedShapes(index: Int, lastIndex: Int): ToggleButtonShapes =
    when (index) {
      0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
      lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
      else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    }
