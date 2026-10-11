package com.lpavs.caliinda.feature.event_management.ui.shared.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.DeviceCalendar
import com.lpavs.caliinda.core.ui.components.ChoiceDialog
import com.lpavs.caliinda.core.ui.components.ChoiceOption

/**
 * Календарь нового события: цветная точка, название и аккаунт; по тапу — выбор из показанных.
 * Цвет календаря — только точкой, строку красим ролями темы (DESIGN.md).
 */
@Composable
fun CalendarField(
    calendars: List<DeviceCalendar>,
    selected: DeviceCalendar,
    onSelect: (Long) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
  var showPicker by rememberSaveable { mutableStateOf(false) }
  val canChoose = enabled && calendars.size > 1
  ListItem(
      headlineContent = { Text(selected.displayName, maxLines = 1) },
      supportingContent = { Text(selected.accountName, maxLines = 1) },
      leadingContent = {
        Box(Modifier.size(16.dp).clip(CircleShape).background(Color(selected.color)))
      },
      trailingContent = {
        if (calendars.size > 1) Icon(Icons.Rounded.ExpandMore, contentDescription = null)
      },
      colors = ListItemDefaults.colors(containerColor = colorScheme.surfaceContainerHigh),
      modifier =
          modifier
              .fillMaxWidth()
              .clip(shapes.extraLarge)
              .clickable(enabled = canChoose) { showPicker = true })

  if (showPicker) {
    // Одинаковые названия («Календарь» у двух аккаунтов) различаем аккаунтом.
    val duplicates = calendars.groupBy { it.displayName }.filterValues { it.size > 1 }.keys
    ChoiceDialog(
        icon = Icons.Rounded.CalendarMonth,
        title = stringResource(R.string.event_calendar),
        message = null,
        options =
            calendars.map { calendar ->
              val label =
                  if (calendar.displayName in duplicates)
                      "${calendar.displayName} · ${calendar.accountName}"
                  else calendar.displayName
              ChoiceOption(label, selected = calendar.id == selected.id) {
                onSelect(calendar.id)
                showPicker = false
              }
            },
        cancelText = stringResource(R.string.cancel),
        onDismiss = { showPicker = false })
  }
}
