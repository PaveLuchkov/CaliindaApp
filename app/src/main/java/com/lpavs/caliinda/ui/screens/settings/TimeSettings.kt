package com.lpavs.caliinda.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.ui.screens.main.MainViewModel
import com.lpavs.caliinda.ui.screens.main.components.UIDefaults.cuid
import kotlinx.coroutines.launch
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSettingsScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit, title: String) {
  // Используем переданный viewModel

  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val currentSavedTimeZone by viewModel.timeZone.collectAsStateWithLifecycle()
  var selectedTimeZoneId by
      remember(currentSavedTimeZone) {
        mutableStateOf(currentSavedTimeZone.takeIf { it.isNotEmpty() } ?: ZoneId.systemDefault().id)
      }
  var expanded by remember { mutableStateOf(false) }
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
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp).fillMaxWidth()) {
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(cuid.SettingsItemCornerRadius))
                      .background(color = colorScheme.surfaceContainer)
                      .padding(16.dp)) {
                // TODO: Сделать настройку системного или нет таймзоны
                ExposedDropdownMenuBox(
                    expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                      OutlinedTextField(
                          readOnly = true, // Меню только для чтения
                          value = selectedTimeZoneId, // Показываем выбранный ID
                          onValueChange = {}, // Пустой обработчик, т.к. readOnly
                          label = {
                            Text(stringResource(R.string.time_zone))
                          }, // Используй Text() для label
                          trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                          },
                          modifier =
                              Modifier.menuAnchor(
                                      type = PrimaryEditable, enabled = true) // Исправлено
                                  .fillMaxWidth(),
                          shape = RoundedCornerShape(cuid.SettingsItemCornerRadius))

                      ExposedDropdownMenu(
                          expanded = expanded, onDismissRequest = { expanded = false }) {
                            allTimeZones.forEach { timeZoneId ->
                              DropdownMenuItem(
                                  text = { Text(timeZoneId) },
                                  onClick = {
                                    selectedTimeZoneId =
                                        timeZoneId // Обновляем локальное состояние UI
                                    expanded = false // Закрываем меню
                                    scope.launch {
                                      viewModel.updateTimeZoneSetting(timeZoneId) // Сохраняем выбор
                                      snackbarHostState.showSnackbar("Time zone saved")
                                    }
                                  })
                            }
                          }
                    }
              }
          Spacer(modifier = Modifier.height(10.dp)) // Используй константу
          //            Box(
          //                modifier = Modifier
          //                    .fillMaxWidth()
          //                    .clip(RoundedCornerShape(cuid.SettingsItemCornerRadius)) //
          // Константа
          //                    .background(color = colorScheme.surfaceContainer) // Тема
          //                    .padding(16.dp)
          //            ) {
          //                Column {
          //                    Text("Другие настройки будут здесь.")
          //                }
          //            }
        }
      }
}
