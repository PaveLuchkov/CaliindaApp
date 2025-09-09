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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.cuid

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun AgentRecommendItem(
    title: String,
    description: String,
    onToggleExpand: () -> Unit,
    onSessionDelete: () -> Unit,
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
                      FontVariation.weight(700),
                      FontVariation.width(100f),
                      FontVariation.opticalSizing(100.sp))))
  val cardBackground = colorScheme.tertiaryFixed
  val cardTextColor = colorScheme.onTertiaryFixedVariant
  val cardBorderColor = colorScheme.onTertiaryFixed
  val styleTitle = typography.headlineSmall
  val textTitleAlign = TextAlign.Start
  val shape1 = MaterialShapes.SoftBurst.toShape()
  val onDialog = colorScheme.onTertiaryFixedVariant

  Box(
      modifier =
          Modifier.padding(
                  horizontal = CalendarUiDefaults.ItemHorizontalPadding,
                  vertical = CalendarUiDefaults.ItemVerticalPadding + 5.dp)
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
        val currentText = title
//        Box(
//            modifier =
//                Modifier.align(Alignment.TopEnd)
//                    .offset(y = (-10).dp, x = 10.dp)
//                    .size(80.dp)
//                    .rotate(75f)
//                    .border(width = 2.dp, color = onDialog.copy(alpha = 0.2f), shape = shape1)
//                    .clip(shape1)
//                    .background(onDialog.copy(alpha = 0f)))
        Column {
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(
                          horizontal = cuid.ItemHorizontalPadding,
                          vertical = cuid.AgentCardVerticalPadding),
              contentAlignment = Alignment.CenterStart) {
                Text(
                    text = currentText,
                    color = cardTextColor,
                    textAlign = textTitleAlign,
                    style = styleTitle,
                    fontFamily = cardFontFamily,)
              }

          AnimatedVisibility(
              visible = isExpanded,
              enter =
                  fadeIn(animationSpec = tween(durationMillis = 150, delayMillis = 100)) +
                      expandVertically(
                          animationSpec = tween(durationMillis = 250, delayMillis = 50),
                          expandFrom = Alignment.Top // Убрал .Companion для чистоты кода
                          ),
              exit =
                  shrinkVertically(
                      animationSpec = tween(durationMillis = 250),
                      shrinkTowards = Alignment.Top // Убрал .Companion
                      ) + fadeOut(animationSpec = tween(durationMillis = 150))) {
                // Внутри AnimatedVisibility размещаем контент, который должен анимироваться
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(
                                horizontal = cuid.ItemHorizontalPadding,
                                vertical = cuid.AgentCardVerticalPadding),
                    contentAlignment = Alignment.CenterEnd) {
                      val size = ButtonDefaults.ExtraSmallContainerHeight
                      Button(
                          onClick = { onSessionDelete() },
                          modifier = Modifier.heightIn(size),
                          contentPadding = ButtonDefaults.contentPaddingFor(size),
                      ) {
                        Icon(
                            Icons.Filled.DeleteForever,
                            contentDescription = "Localized description",
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
                        )
                      }
                    }
              }
        }
      }
}

@Preview(showBackground = true, wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE)
@Composable
fun AgentEventRecommendPreview() {
    AgentRecommendItem(
      title = "Неподдерживаемый формат.",
        description = "Описание",
      isExpanded = false,
      onToggleExpand = {},
      onSessionDelete = {})
}
