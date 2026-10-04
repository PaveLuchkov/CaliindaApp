package com.lpavs.caliinda.feature.calendar.presentation.components.bars

import com.lpavs.caliinda.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import com.lpavs.caliinda.core.ui.theme.CaliindaTheme
import com.lpavs.caliinda.feature.calendar.presentation.AppMode
import java.time.LocalDate

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalTextApi::class)
@Composable
fun CalendarAppBar(
    onNavigateToSettings: () -> Unit,
    onGoToTodayClick: () -> Unit,
    onTitleClick: () -> Unit,
    onTitleHold: () -> Unit,
    date: LocalDate,
    today: LocalDate,
    hasCalendarAccess: Boolean,
    currentCalendarScreenMode: AppMode = AppMode.CALENDAR
) {
  CenterAlignedTopAppBar(
      title = {
        when (currentCalendarScreenMode) {
          AppMode.CALENDAR ->
              CalendarDateTitle(
                  date = date,
                  today = today,
                  hasCalendarAccess = hasCalendarAccess,
                  onTitleHold = onTitleHold,
                  onTitleClick = onTitleClick)
          AppMode.MANAGEMENT -> ManagementTitle()
        }
      },
      navigationIcon = {
          val haptic = LocalHapticFeedback.current
        FilledIconButton(
            onClick = { onGoToTodayClick()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      },
            modifier =
                Modifier
                    .minimumInteractiveComponentSize()
                    .size(
                        IconButtonDefaults.smallContainerSize(
                            IconButtonDefaults.IconButtonWidthOption.Wide
                        )
                    ),
            shape = IconButtonDefaults.smallRoundShape,
            enabled = hasCalendarAccess) {
              Icon(
                  Icons.Filled.Today,
                  contentDescription = stringResource(R.string.go_to_today),
              )
            }
      },
      actions = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          FilledIconButton(
              onClick = onNavigateToSettings,
              modifier =
                  Modifier
                      .minimumInteractiveComponentSize()
                      .size(
                          IconButtonDefaults.smallContainerSize(
                              IconButtonDefaults.IconButtonWidthOption.Wide
                          )
                      ),
              shape = IconButtonDefaults.smallRoundShape) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings),
                )
              }
        }
      },
      colors = topAppBarColors(containerColor = Color.Transparent))
}

@Preview(showBackground = true, wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE)
@Composable
fun CalendarEventPreview() {
  CaliindaTheme {
    CalendarAppBar(
        onTitleClick = {},
        onGoToTodayClick = {},
        onNavigateToSettings = {},
        onTitleHold = {},
        date = LocalDate.now(),
        today = LocalDate.now(),
        hasCalendarAccess = true)
  }
}
