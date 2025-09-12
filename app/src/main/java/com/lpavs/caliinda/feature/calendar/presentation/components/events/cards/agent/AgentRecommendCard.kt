package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.lpavs.caliinda.core.data.remote.agent.Slot
import com.lpavs.caliinda.core.data.remote.agent.Suggestion
import com.lpavs.caliinda.core.data.remote.agent.Weekday
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.CaliindaTheme
import com.lpavs.caliinda.core.ui.theme.cuid

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun AgentRecommendItem(
    suggestion: Suggestion,
    onToggleExpand: () -> Unit,
    onConfirm: (String) -> Unit,
    isExpanded: Boolean,
) {
  val darkerShadowColor = Color.Black
  val cardElevation = cuid.CurrentEventElevation
  val cardFontFamily =
      FontFamily(
          Font(
              R.font.robotoflex_variable,
              variationSettings =
                  FontVariation.Settings(
                      FontVariation.weight(600),
                      FontVariation.width(100f),
                      FontVariation.opticalSizing(150.sp),
                  )))
  val slotNameFontFamily =
      FontFamily(
          Font(
              R.font.robotoflex_variable,
              variationSettings =
                  FontVariation.Settings(
                      FontVariation.weight(700),
                      FontVariation.width(100f),
                      FontVariation.opticalSizing(50.sp),
                  )))
  val isRecommened = suggestion.isRecommended
  val cardBackground =
      if (!isRecommened) colorScheme.secondaryContainer else colorScheme.primaryContainer
  val cardTextColor =
      if (!isRecommened) colorScheme.onSecondaryContainer else colorScheme.onPrimaryContainer
  val cardBorderColor = colorScheme.onTertiaryFixed
  val styleTitle = typography.headlineSmall
  val textTitleAlign = TextAlign.Start
  val recommentTitle = suggestion.title
  Box(
      modifier =
          Modifier.padding(
                  horizontal = CalendarUiDefaults.ItemHorizontalPadding,
                  vertical = CalendarUiDefaults.ItemVerticalPadding)
              .shadow(
                  elevation = cardElevation,
                  shape = RoundedCornerShape(cuid.EventItemCornerRadius),
                  clip = false,
                  ambientColor = darkerShadowColor,
                  spotColor = darkerShadowColor)
              .clip(RoundedCornerShape(cuid.EventItemCornerRadius))
              .border(
                  width = 2.dp,
                  color = cardBorderColor,
                  shape = RoundedCornerShape(cuid.EventItemCornerRadius))
              .background(cardBackground)
              .clickable(onClick = { onToggleExpand() })) {
        Column {
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(
                          horizontal = cuid.ItemHorizontalPadding,
                          vertical = cuid.AgentCardVerticalPadding),
              contentAlignment = Alignment.CenterStart) {
                Text(
                    text = suggestion.title,
                    color = cardTextColor,
                    textAlign = textTitleAlign,
                    style = styleTitle,
                    fontFamily = cardFontFamily,
                )
              }

          AnimatedVisibility(
              visible = isExpanded,
              enter =
                  fadeIn(animationSpec = tween(durationMillis = 150, delayMillis = 100)) +
                      expandVertically(
                          animationSpec = tween(durationMillis = 250, delayMillis = 50),
                          expandFrom = Alignment.Top),
              exit =
                  shrinkVertically(
                      animationSpec = tween(durationMillis = 250), shrinkTowards = Alignment.Top) +
                      fadeOut(animationSpec = tween(durationMillis = 150))) {
                Column {
                  Box(
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(
                                  horizontal = cuid.ItemHorizontalPadding,
                                  vertical = cuid.AgentCardVerticalPadding),
                      contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = suggestion.description,
                            style = typography.bodySmall,
                            color = cardTextColor)
                      }
                  Column(
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(
                                  horizontal = cuid.ItemHorizontalPadding,
                                  vertical = cuid.AgentCardVerticalPadding),
                      horizontalAlignment = Alignment.CenterHorizontally) {
                        suggestion.slots.forEach { slot ->
                          Column(
                              horizontalAlignment = Alignment.Start,
                              modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = slot.name,
                                    style = typography.bodyLarge,
                                    color = cardTextColor,
                                    fontFamily = slotNameFontFamily)
                                Text(
                                    text = "${slot.day} • ${slot.startTime} - ${slot.endTime}",
                                    style = typography.bodySmall,
                                    color = cardTextColor)
                              }
                        }
                        Button(
                            onClick = {
                              onConfirm(
                                  "Я соглашаюсь на план $recommentTitle и подтверждаю его создание. class: plan_update.")
                            }) {
                              Text("Confirm plan", fontFamily = slotNameFontFamily)
                            }
                      }
                }
              }
        }
      }
}

@Preview(showBackground = true, wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE)
@Composable
fun AgentEventRecommendPreview() {
  val debugVsCodeSuggestion =
      Suggestion(
          title = "Debug VS code",
          description =
              "When attempting to debug my app in Visual Studio Code using any bedugging platform, the process gets stuck in an infinite loading loop.",
          slots =
              listOf(
                  Slot(Weekday.Monday, "Debugging tests", "07:00", "08:00"),
                  Slot(Weekday.Wednesday, "Attemping to create outline", "17:30", "18:00"),
                  Slot(Weekday.Friday, "Generate AI crap", "13:30", "14:30")),
          isRecommended = true)
  CaliindaTheme {
    AgentRecommendItem(
        suggestion = debugVsCodeSuggestion,
        isExpanded = true,
        onToggleExpand = {},
        onConfirm = {})
  }
}
