package com.lpavs.caliinda.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.DeviceCalendar
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.feature.settings.vm.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit) {
  val calendars by viewModel.calendars.collectAsStateWithLifecycle()
  val defaultCalendarId by viewModel.defaultCalendarId.collectAsStateWithLifecycle()

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Calendars") },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back))
              }
            })
      }) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(cuid.ItemVerticalPadding)) {
              item {
                Text(
                    text = "New events are saved to",
                    style = typography.titleSmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp))
              }
              if (calendars.isEmpty()) {
                item {
                  Text(
                      text =
                          "No calendars found. Allow calendar access or add an account in " +
                              "system settings. Otherwise Caliinda will create a local calendar " +
                              "on the first event.",
                      style = typography.bodyMedium,
                      color = colorScheme.onSurfaceVariant)
                }
              }
              items(calendars, key = { it.id }) { calendar ->
                CalendarItem(
                    calendar = calendar,
                    selected = calendar.id == defaultCalendarId,
                    onClick = { viewModel.selectDefaultCalendar(calendar.id) })
              }
            }
      }
}

@Composable
private fun CalendarItem(calendar: DeviceCalendar, selected: Boolean, onClick: () -> Unit) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(cuid.SettingsItemCornerRadius))
              .background(colorScheme.surfaceContainer)
              .clickable(onClick = onClick)
              .padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(calendar.color)))
          Spacer(Modifier.width(16.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(text = calendar.displayName, style = typography.bodyLarge, maxLines = 1)
            Text(
                text = calendar.accountName,
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1)
          }
          RadioButton(selected = selected, onClick = onClick)
        }
      }
}
