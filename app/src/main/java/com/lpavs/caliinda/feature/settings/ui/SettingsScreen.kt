package com.lpavs.caliinda.feature.settings.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.BuildConfig
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.DeviceCalendar
import com.lpavs.caliinda.core.data.model.ThemeMode
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.theme.themeColorScheme
import com.lpavs.caliinda.feature.settings.vm.SettingsViewModel
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

private const val PRIVACY_POLICY_URL = "https://www.lpavs.com/caliinda/privacy-policy"

private fun Context.startActivitySafely(intent: Intent) {
  try {
    startActivity(intent)
  } catch (_: ActivityNotFoundException) {
    // Нет браузера или экрана настроек — просто ничего не открываем
  }
}

/**
 * Все настройки одним экраном, как системные в Android: секции с заголовками и сгруппированные
 * строки (SegmentedListItem) вместо отдельных полупустых экранов.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChips: () -> Unit,
) {
  val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
  val calendars by settingsViewModel.calendars.collectAsStateWithLifecycle()
  val defaultCalendarId by settingsViewModel.defaultCalendarId.collectAsStateWithLifecycle()
  val savedZone by settingsViewModel.timeZone.collectAsStateWithLifecycle()
  val systemZone = remember { ZoneId.systemDefault().id }
  val usesSystemZone = savedZone.isEmpty() || savedZone == systemZone
  val currentZone = if (usesSystemZone) systemZone else savedZone
  var showZonePicker by rememberSaveable { mutableStateOf(false) }
  val context = LocalContext.current

  // Live-уведомление: включаем только с разрешением; отказали — дальше ведём в системные настройки.
  val liveEnabled by settingsViewModel.liveNotification.collectAsStateWithLifecycle()
  var notificationsAllowed by remember {
    mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
  }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
  }
  var permissionDenied by rememberSaveable { mutableStateOf(false) }
  val permissionLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted
        permissionDenied = !granted
        if (granted) settingsViewModel.setLiveNotification(true)
      }
  val onLiveToggle: (Boolean) -> Unit = { checked ->
    when {
      !checked -> settingsViewModel.setLiveNotification(false)
      notificationsAllowed -> settingsViewModel.setLiveNotification(true)
      permissionDenied || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
          context.startActivitySafely(
              Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                  .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
      else -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      topBar = {
        LargeFlexibleTopAppBar(
            title = { Text(stringResource(R.string.settings)) },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back))
              }
            },
            scrollBehavior = scrollBehavior)
      }) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
              section(R.string.settings_appearance) {
                item {
                  ThemePickerItem(
                      currentMode = themeMode,
                      onModeSelected = settingsViewModel::updateThemeMode)
                }
              }

              section(R.string.calendars) {
                item {
                  Text(
                      text = stringResource(R.string.new_events_saved_to),
                      style = typography.bodyMedium,
                      color = colorScheme.onSurfaceVariant,
                      modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                if (calendars.isEmpty()) {
                  item {
                    Text(
                        text = stringResource(R.string.no_calendars_found),
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp))
                  }
                }
                itemsIndexed(calendars, key = { _, it -> it.id }) { index, calendar ->
                  CalendarRow(
                      calendar = calendar,
                      selected = calendar.id == defaultCalendarId,
                      index = index,
                      count = calendars.size,
                      onClick = { settingsViewModel.selectDefaultCalendar(calendar.id) })
                }
              }

              section(R.string.settings_suggestions) {
                item {
                  SegmentedListItem(
                      onClick = onNavigateToChips,
                      shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                      colors = settingsItemColors(),
                      leadingContent = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                      trailingContent = {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                      },
                      supportingContent = { Text(stringResource(R.string.chips_settings_summary)) }) {
                        Text(stringResource(R.string.chips_settings_title))
                      }
                }
              }

              section(R.string.settings_notifications) {
                item {
                  val checked = liveEnabled && notificationsAllowed
                  SegmentedListItem(
                      checked = checked,
                      onCheckedChange = onLiveToggle,
                      shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                      colors = settingsItemColors(),
                      leadingContent = {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                      },
                      trailingContent = { Switch(checked = checked, onCheckedChange = null) },
                      supportingContent = {
                        Text(
                            stringResource(
                                if (permissionDenied && !notificationsAllowed) {
                                  R.string.live_permission_denied
                                } else R.string.live_setting_summary))
                      }) {
                        Text(stringResource(R.string.live_setting_title))
                      }
                }
              }

              section(R.string.time) {
                item {
                  SegmentedListItem(
                      checked = usesSystemZone,
                      onCheckedChange = { checked ->
                        // Выключили — сразу выбираем пояс вручную; включили — возвращаем системный.
                        if (checked) settingsViewModel.updateTimeZoneSetting(systemZone)
                        else showZonePicker = true
                      },
                      shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                      colors = settingsItemColors(),
                      leadingContent = { Icon(Icons.Rounded.Language, contentDescription = null) },
                      trailingContent = { Switch(checked = usesSystemZone, onCheckedChange = null) },
                      supportingContent = { Text(systemZone) }) {
                        Text(stringResource(R.string.use_system_time_zone))
                      }
                }
                item {
                  SegmentedListItem(
                      onClick = { showZonePicker = true },
                      enabled = !usesSystemZone,
                      shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                      colors = settingsItemColors(),
                      leadingContent = { Icon(Icons.Rounded.Public, contentDescription = null) },
                      supportingContent = { Text(zoneLabel(currentZone)) }) {
                        Text(stringResource(R.string.time_zone))
                      }
                }
              }

              section(R.string.about) {
                item {
                  SegmentedListItem(
                      onClick = {
                        context.startActivitySafely(
                            Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri()))
                      },
                      shapes = ListItemDefaults.segmentedShapes(index = 0, count = 3),
                      colors = settingsItemColors(),
                      leadingContent = {
                        Icon(painterResource(R.drawable.doc), contentDescription = null)
                      },
                      trailingContent = {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                      }) {
                        Text(stringResource(R.string.privacy_policy))
                      }
                }
                item {
                  // Все данные приложения хранятся только на устройстве: в системной карточке
                  // приложения их можно стереть и отозвать доступ к календарю.
                  SegmentedListItem(
                      onClick = {
                        context.startActivitySafely(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)))
                      },
                      shapes = ListItemDefaults.segmentedShapes(index = 1, count = 3),
                      colors = settingsItemColors(),
                      leadingContent = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
                      supportingContent = { Text(stringResource(R.string.delete_data_summary)) }) {
                        Text(stringResource(R.string.delete_data))
                      }
                }
                item {
                  // Версия только для чтения — обычная строка без отклика на нажатие.
                  ListItem(
                      headlineContent = { Text(stringResource(R.string.app_version)) },
                      supportingContent = { Text(BuildConfig.VERSION_NAME) },
                      leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
                      colors = ListItemDefaults.colors(containerColor = colorScheme.surfaceContainer),
                      modifier =
                          Modifier.clip(ListItemDefaults.segmentedShapes(index = 2, count = 3).shape))
                }
              }
              item { Spacer(Modifier.height(24.dp)) }
            }
      }

  if (showZonePicker) {
    TimeZonePickerSheet(
        current = currentZone,
        onSelect = {
          settingsViewModel.updateTimeZoneSetting(it)
          showZonePicker = false
        },
        onDismiss = { showZonePicker = false })
  }
}

/** Подложка строк — как у выбора темы, чтобы группы читались на фоне экрана. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun settingsItemColors() =
    ListItemDefaults.segmentedColors(
        containerColor = colorScheme.surfaceContainer,
        disabledContainerColor = colorScheme.surfaceContainer)

/** Заголовок секции и её строки. */
private fun LazyListScope.section(titleRes: Int, content: LazyListScope.() -> Unit) {
  item(key = "header_$titleRes") {
    Text(
        text = stringResource(titleRes),
        style = typography.titleSmall,
        color = colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp))
  }
  content()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CalendarRow(
    calendar: DeviceCalendar,
    selected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
  SegmentedListItem(
      selected = selected,
      onClick = onClick,
      shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
                      colors = settingsItemColors(),
      // Цвет календаря — только точкой: карточки красим ролями темы (DESIGN.md).
      leadingContent = {
        Box(Modifier.size(16.dp).clip(CircleShape).background(Color(calendar.color)))
      },
      trailingContent = { RadioButton(selected = selected, onClick = null) },
      supportingContent = { Text(calendar.accountName, maxLines = 1) }) {
        Text(calendar.displayName, maxLines = 1)
      }
}

/** «Europe/Moscow · GMT+03:00» — у пояса сразу виден сдвиг. */
private fun zoneLabel(zoneId: String): String {
  val zone = runCatching { ZoneId.of(zoneId) }.getOrNull() ?: return zoneId
  val offset = ZonedDateTime.now(zone).offset.id.let { if (it == "Z") "+00:00" else it }
  return "$zoneId · GMT$offset"
}

/** Шторка выбора пояса: поиск по сотням вариантов вместо выпадающего списка. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeZonePickerSheet(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
  val zones = remember { ZoneId.getAvailableZoneIds().filter { "/" in it }.sorted() }
  var query by rememberSaveable { mutableStateOf("") }
  val filtered =
      remember(query) {
        val q = query.trim().replace(' ', '_')
        if (q.isEmpty()) zones
        else
            zones.filter {
              it.contains(q, ignoreCase = true) ||
                  ZoneId.of(it).getDisplayName(TextStyle.FULL, Locale.getDefault())
                      .contains(query.trim(), ignoreCase = true)
            }
      }
  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_time_zone)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(cuid.ContainerCornerRadius),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
          itemsIndexed(filtered, key = { _, it -> it }) { _, zone ->
            ListItem(
                headlineContent = { Text(zone) },
                supportingContent = { Text(zoneLabel(zone).substringAfter(" · ")) },
                trailingContent = {
                  if (zone == current) Icon(Icons.Default.Check, contentDescription = null)
                },
                // Фон шторки, а не свой: иначе строки ложатся полосами.
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onSelect(zone) })
          }
        }
      }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class) // Если используешь Expressive формы
@Composable
fun ThemeItem(
    mode: ThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 1. Определяем форму в зависимости от темы
    val modeShape: Shape = when(mode) {
        ThemeMode.SYSTEM -> MaterialShapes.Flower.toShape()
        ThemeMode.SUNNY -> MaterialShapes.Sunny.toShape()
        ThemeMode.WARM -> MaterialShapes.VerySunny.toShape() // Проверь названия в своих шейпах
        ThemeMode.GREEN -> MaterialShapes.Arrow.toShape()
        ThemeMode.COLD -> MaterialShapes.Burst.toShape()
        ThemeMode.PINKY -> MaterialShapes.Heart.toShape()
    }

    // Цвет превью и контрастная к нему галочка — из схемы самой темы.
    val previewScheme = themeColorScheme(themeMode = mode)
    val previewColor = previewScheme.primary
    val checkMarkColor = previewScheme.onPrimary

    Box(
        modifier = Modifier
            // Делаем элемент кликабельным и добавляем немного паддинга для визуального разделителя
            .clip(modeShape) // Обрезаем клик по форме
            .clickable(onClick = onClick)
            .size(48.dp) // Общий размер области клика (можно 40.dp)
        ,
        contentAlignment = Alignment.Center
    ) {
        // Рисуем саму фигуру
        Box(
            modifier = Modifier
                .matchParentSize() // Заполняем 48dp
                .background(previewColor)
                .border(
                    // Если выбрано - жирная рамка, если нет - тонкая или отсутствует
                    width = if (isSelected) 3.dp else 0.dp,
                    color = MaterialTheme.colorScheme.onSurface, // Цвет рамки
                    shape = modeShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Опционально: галочка внутри, если выбрано
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = checkMarkColor, // Или MaterialTheme.colorScheme.onPrimary
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ThemePickerItem(
    modifier: Modifier = Modifier,
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    val cornerRadius = cuid.SettingsItemCornerRadius // замени на  если есть доступ

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(color = colorScheme.surfaceContainer) // Фон контейнера
            .padding(vertical = 12.dp, horizontal = 8.dp) // Отступы внутри контейнера
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly, // Равномерное распределение
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeMode.entries.forEach { mode ->
                ThemeItem(
                    mode = mode,
                    isSelected = (mode == currentMode),
                    onClick = { onModeSelected(mode) }
                )
            }
        }
    }
}
