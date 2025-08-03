package com.lpavs.caliinda.feature.calendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.Typography
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalTextApi::class)
@Composable
fun CalendarAppBar(
    onNavigateToSettings: () -> Unit,
    onGoToTodayClick: () -> Unit,
    onTitleClick: () -> Unit,
    date: LocalDate
) {
  val isToday = date == LocalDate.now()
  val headerBackgroundColor =
      if (isToday) {
        colorScheme.tertiary
      } else {
        colorScheme.secondary
      }
  val headerTextColor =
      if (isToday) {
        colorScheme.onTertiary
      } else {
        colorScheme.onSecondary
      }
  val headerTextStyle =
      when {
        isToday -> Typography.titleLargeEmphasized
        else -> Typography.titleLarge
      }
  val headerFontFamily =
      when {
        isToday ->
            FontFamily(
                Font(
                    R.font.robotoflex_variable,
                    variationSettings =
                        FontVariation.Settings(
                            FontVariation.weight(750),
                        )))
        else ->
            FontFamily(
                Font(
                    R.font.robotoflex_variable,
                    variationSettings =
                        FontVariation.Settings(
                            FontVariation.weight(600),
                        )))
      }
  val currentLocale = LocalConfiguration.current.getLocales().get(0)
  val formatterWithShortDay = DateTimeFormatter.ofPattern("E, d MMMM yyyy", currentLocale)
  CenterAlignedTopAppBar(
      title = {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(color = headerBackgroundColor)
                    .clickable(onClick = onTitleClick),
        ) {
          Text(
              text = date.format(formatterWithShortDay),
              style = headerTextStyle,
              fontFamily = headerFontFamily,
              color = headerTextColor,
              modifier =
                  Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                      .fillMaxWidth(), // Больше отступы
              textAlign = TextAlign.Center,
              fontSize = 16.sp,
          )
        }
      },
      navigationIcon = {
        FilledIconButton(
            onClick = onGoToTodayClick,
            modifier =
                Modifier.minimumInteractiveComponentSize()
                    .size(
                        IconButtonDefaults.smallContainerSize(
                            IconButtonDefaults.IconButtonWidthOption.Wide)),
            shape = IconButtonDefaults.smallRoundShape) {
              Icon(
                  Icons.Filled.Today,
                  contentDescription = "Перейти к сегодня",
              )
            }
      },
      actions = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          FilledIconButton(
              onClick = onNavigateToSettings,
              modifier =
                  Modifier.minimumInteractiveComponentSize()
                      .size(
                          IconButtonDefaults.smallContainerSize(
                              IconButtonDefaults.IconButtonWidthOption.Wide)),
              shape = IconButtonDefaults.smallRoundShape) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Настройки",
                )
              }
        }
      },
      colors = topAppBarColors(containerColor = Color.Transparent))
}
