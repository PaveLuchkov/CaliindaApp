package com.example.caliindar.ui.screens.settings


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.MenuAnchorType.Companion.PrimaryEditable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.caliindar.ui.screens.main.MainViewModel
import com.example.caliindar.ui.screens.main.components.calendarui.cuid
import kotlinx.coroutines.launch
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    title: String
) {
    // Используем переданный viewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentSavedTimeZone by viewModel.timeZone.collectAsStateWithLifecycle()
    var selectedTimeZoneId by remember(currentSavedTimeZone) {
        mutableStateOf(currentSavedTimeZone.takeIf { it.isNotEmpty() } ?: ZoneId.systemDefault().id)
    }
    val currentTimeZone by viewModel.timeZone.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    val allTimeZones = remember { ZoneId.getAvailableZoneIds().sorted() }
    val use12Hour by viewModel.use12HourFormat.collectAsStateWithLifecycle()


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cuid.SettingsItemCornerRadius))
                    .background(color = colorScheme.surfaceContainer)
                    .padding(16.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        readOnly = true, // Меню только для чтения
                        value = selectedTimeZoneId, // Показываем выбранный ID
                        onValueChange = {}, // Пустой обработчик, т.к. readOnly
                        label = { Text("Часовой пояс") }, // Используй Text() для label
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(type = PrimaryEditable, enabled = true) // Исправлено
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(cuid.SettingsItemCornerRadius)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        allTimeZones.forEach { timeZoneId ->
                            DropdownMenuItem(
                                text = { Text(timeZoneId) },
                                onClick = {
                                    selectedTimeZoneId =
                                        timeZoneId // Обновляем локальное состояние UI
                                    expanded = false // Закрываем меню
                                    scope.launch {
                                        viewModel.updateTimeZoneSetting(timeZoneId) // Сохраняем выбор
                                        snackbarHostState.showSnackbar("Часовой пояс сохранён: $timeZoneId")
                                    }
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp)) // Используй константу

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cuid.SettingsItemCornerRadius))
                    .background(color = colorScheme.surfaceContainer)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween // Разносим текст и свитч
                ) {
                    Text(
                        text = "12-часовой формат (AM/PM)",
                        style = typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                            .padding(end = 8.dp) // Занимает место и добавляет отступ
                    )
                    Switch(
                        checked = use12Hour, // Текущее состояние
                        onCheckedChange = { newValue ->
                            viewModel.updateUse12HourFormat(newValue) // Сохраняем новое значение
                            // Можно показать Snackbar об успехе, если нужно
                            // scope.launch { snackbarHostState.showSnackbar("Формат времени изменен") }
                        },
                        colors = SwitchDefaults.colors( // Опционально: настройка цветов
                            // checkedThumbColor = ...,
                            // checkedTrackColor = ...,
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp)) // Используй константу
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cuid.SettingsItemCornerRadius)) // Константа
                    .background(color = colorScheme.surfaceContainer) // Тема
                    .padding(16.dp)
            ) {
                Column {
                    Text("Другие настройки будут здесь.")
                }
            }
        }
    }
}
