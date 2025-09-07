package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.remote.agent.ChatMessage
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.cuid

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun AgentItem(
    message: String
) {
  val darkerShadowColor = Color.Black
  val cardElevation = cuid.CurrentEventElevation
  val cardFontFamily =
      FontFamily(
          Font(
              R.font.robotoflex_variable,
              variationSettings =
                  FontVariation.Settings(
                      FontVariation.weight(700),
                      FontVariation.width(100f),
                      FontVariation.opticalSizing(100.sp)
                  )))
  val cardBackground = colorScheme.tertiaryFixed
  val cardTextColor = colorScheme.onTertiaryFixedVariant
  val cardBorderColor = colorScheme.onTertiaryFixed

  Box(
      modifier =
          Modifier.padding(
                  horizontal = CalendarUiDefaults.ItemHorizontalPadding,
                  vertical = CalendarUiDefaults.ItemVerticalPadding + 5.dp)
              .clip(RoundedCornerShape(cuid.EventItemCornerRadius))
              .shadow(
                  elevation = cardElevation,
                  shape = RoundedCornerShape(cuid.EventItemCornerRadius),
                  clip = false,
                  ambientColor = darkerShadowColor,
                  spotColor = darkerShadowColor)
              .border(
                  width = 2.dp,
                  color = cardBorderColor,
                  shape = RoundedCornerShape(cuid.EventItemCornerRadius))
              .background(cardBackground)) {
        val currentText = message
        var lineCount by remember { mutableIntStateOf(1) }
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(
                        horizontal = cuid.ItemHorizontalPadding,
                        vertical = cuid.StandardItemContentVerticalPadding),
            contentAlignment = Alignment.CenterStart) {
              Text(
                  text = currentText,
                  color = cardTextColor,
                  textAlign = if (lineCount == 1) TextAlign.Center else TextAlign.Start,
                  style =
                      when {
                        lineCount == 1 -> typography.headlineSmall
                        lineCount < 3 -> typography.bodyLarge
                        else -> typography.bodyMedium
                      },
                  fontFamily = cardFontFamily,
                  onTextLayout = { layoutResult -> lineCount = layoutResult.lineCount }
              )
            }
      }
}

@Preview(showBackground = true, wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE)
@Composable
fun AgentEventPreview() {
    AgentItem(
        message = "Привет! Как дела? "
    )
}