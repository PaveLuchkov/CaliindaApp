package com.lpavs.caliinda.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType.Companion.PrimaryEditable
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.feature.settings.vm.SettingsViewModel
import kotlinx.coroutines.launch
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimeSettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit, title: String) {
  // Используем переданный viewModel

  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
    val currentSavedTimeZone by viewModel.timeZone.collectAsStateWithLifecycle()
    // Запоминаем ID системной таймзоны, чтобы не вызывать ZoneId.systemDefault().id постоянно
    val systemDefaultTimeZoneId = remember { ZoneId.systemDefault().id }

    // ГЛАВНОЕ ИЗМЕНЕНИЕ: Это наш единственный "источник правды" для Switch
    var useSystemTimeZone by remember { mutableStateOf(true) }

    // Состояние для отображаемой в поле таймзоны
    var selectedTimeZoneId by remember { mutableStateOf(systemDefaultTimeZoneId) }

    // Состояние для выпадающего меню
    var expanded by remember { mutableStateOf(false) }

    // Этот эффект синхронизирует состояние UI с данными из ViewModel при первом запуске
    // или когда данные в ViewModel изменятся.
    LaunchedEffect(currentSavedTimeZone, systemDefaultTimeZoneId) {
        val isSystem = currentSavedTimeZone.isEmpty() || currentSavedTimeZone == systemDefaultTimeZoneId
        useSystemTimeZone = isSystem
        selectedTimeZoneId = if (isSystem) systemDefaultTimeZoneId else currentSavedTimeZone
    }

    // Получаем список всех таймзон один раз
    val allTimeZones = remember { ZoneId.getAvailableZoneIds().sorted() }
  Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back))
              }
            })
      }) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxWidth()) {
          Box(
              modifier =
                  Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(cuid.SettingsItemCornerRadius))
                      .background(color = colorScheme.surfaceContainer)
                      .padding(16.dp)) {
              Column {
                  Row(
                      verticalAlignment = Alignment.CenterVertically
                  ) {
                      Text(text = stringResource(R.string.use_system_time_zone))
                      Spacer(modifier = Modifier.weight(1f))

                      // --- ШАГ 2: Упрощаем логику Switch ---
                      Switch(
                          checked = useSystemTimeZone, // Напрямую привязываем к нашему состоянию
                          onCheckedChange = { isChecked ->
                              useSystemTimeZone = isChecked // Обновляем состояние
                              if (isChecked) {
                                  // Если включили, то сразу сохраняем системную таймзону
                                  selectedTimeZoneId = systemDefaultTimeZoneId
                                  viewModel.updateTimeZoneSetting(systemDefaultTimeZoneId)
                              }
                              // Если выключили, ничего не делаем, ждем выбора пользователя
                          }
                      )
                  }

                  // --- ШАГ 3: Управляем доступностью меню через useSystemTimeZone ---
                  ExposedDropdownMenuBox(
                      expanded = expanded,
                      onExpandedChange = {
                          // Разрешаем открывать меню, только если Switch выключен
                          if (!useSystemTimeZone) {
                              expanded = !expanded
                          }
                      }
                  ) {
                      OutlinedTextField(
                          modifier = Modifier
                              .menuAnchor() // menuAnchor без параметров - это стандартный способ
                              .fillMaxWidth(),
                          readOnly = true,
                          // Меню заблокировано, если включен системный часовой пояс
                          enabled = !useSystemTimeZone,
                          value = selectedTimeZoneId,
                          onValueChange = {},
                          label = { Text(stringResource(R.string.time_zone)) },
                          trailingIcon = {
                              ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                          },
                          shape = RoundedCornerShape(12.dp)
                      )

                      ExposedDropdownMenu(
                          expanded = expanded,
                          onDismissRequest = { expanded = false }
                      ) {
                          allTimeZones.forEach { timeZoneId ->
                              DropdownMenuItem(
                                  text = { Text(timeZoneId) },
                                  onClick = {
                                      selectedTimeZoneId = timeZoneId // Обновляем UI
                                      expanded = false // Закрываем меню
                                      // Сохраняем выбор в ViewModel
                                      viewModel.updateTimeZoneSetting(timeZoneId)
                                      scope.launch {
                                          snackbarHostState.showSnackbar("Time zone saved: $timeZoneId")
                                      }
                                  }
                              )
                          }
                      }
                  }
              }
          }
            Spacer(modifier = Modifier.height(10.dp))
        }
  }
}