package com.lpavs.caliinda.feature.settings.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.theme.themeColorScheme
import com.lpavs.caliinda.feature.settings.vm.SettingsViewModel
import com.lpavs.caliinda.feature.settings.vm.ThemeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    settignsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCalendarSettings: () -> Unit,
    onNavigateToTimeSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val themeMode by settignsViewModel.themeMode.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.settings)) },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back))
              }
            })
      }) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxWidth()) {
            ThemePickerItem(
                modifier = Modifier.padding(vertical = cuid.ItemVerticalPadding),
                currentMode =  themeMode,
                onModeSelected = { newMode ->
                    settignsViewModel.updateThemeMode(newMode)
                }
            )
              SettingsItem(
                    modifier = Modifier.padding(vertical = cuid.ItemVerticalPadding),
                  icon = {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        tint = colorScheme.onPrimaryContainer,
                        contentDescription = null)
                  },
                  title = "Calendars",
                  onClick = onNavigateToCalendarSettings,
                  shape = MaterialShapes.Clover4Leaf.toShape())
              SettingsItem(
                  modifier = Modifier.padding(vertical = cuid.ItemVerticalPadding),
                  icon = {
                    Icon(
                        Icons.Rounded.AccessTimeFilled,
                        tint = colorScheme.onPrimaryContainer,
                        contentDescription = stringResource(R.string.time))
                  },
                  title = stringResource(R.string.time_format),
                  onClick = onNavigateToTimeSettings,
                  shape = MaterialShapes.Pill.toShape())
            SettingsItem(
                modifier = Modifier.padding(vertical = cuid.ItemVerticalPadding),
                icon = {
                    Icon(
                        Icons.Rounded.Info,
                        tint = colorScheme.onPrimaryContainer,
                        contentDescription = ("About"))
                },
                title = "About",
                onClick = onNavigateToAbout,
                shape = MaterialShapes.Gem.toShape())
            }

      }
}

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
    shape: Shape
) {
  val cornerRadius = cuid.SettingsItemCornerRadius
  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(cornerRadius))
              .background(color = colorScheme.surfaceContainer)
              .height(60.dp)
              .clickable(onClick = onClick)) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .clip(shape)
                    .size(40.dp)
                    .background(color = colorScheme.primaryContainer),
            contentAlignment = Alignment.Center) {
              icon()
            }
        Text(text = title, modifier = Modifier
            .padding(16.dp)
            .align(Alignment.Center))
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