package com.lpavs.caliinda.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Круглые кнопки Пн…Вс — для повторения и для дней чипа. Последний день снять нельзя: набор
 * без дней бессмыслен.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeekdayToggles(
    selected: Set<DayOfWeek>,
    onChange: (Set<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier,
) {
  Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    DayOfWeek.entries.forEach { day ->
      val checked = day in selected
      ToggleButton(
          checked = checked,
          onCheckedChange = {
            val next = if (checked) selected - day else selected + day
            if (next.isNotEmpty()) onChange(next)
          },
          shapes = ToggleButtonDefaults.shapes(),
          contentPadding = ButtonDefaults.ExtraSmallContentPadding,
          modifier = Modifier.weight(1f).aspectRatio(1f)) {
            Text(
                day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = typography.labelMedium,
                maxLines = 1)
          }
    }
  }
}
