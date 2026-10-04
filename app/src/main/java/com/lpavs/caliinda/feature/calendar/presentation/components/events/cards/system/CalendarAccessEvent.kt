package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.system

import androidx.compose.ui.res.stringResource
import com.lpavs.caliinda.core.ui.theme.CaliindaFonts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.CaliindaTheme
import com.lpavs.caliinda.core.ui.theme.Typography
import com.lpavs.caliinda.core.ui.theme.cuid

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun CalendarAccessEvent(
    onGrantAccessClick: () -> Unit,
) {
  val darkerShadowColor = Color.Black
  val cardElevation = cuid.CurrentEventElevation
  val cardFontFamily =
      CaliindaFonts.Card
  val cardBackground = colorScheme.tertiaryFixed
  val cardTextColor = colorScheme.onTertiaryFixedVariant
  val cardBorderColor = colorScheme.onSurface
  val textStyle = Typography.headlineSmall
  val onDialog = colorScheme.onTertiaryFixedVariant

  val shape1 = MaterialShapes.Flower.toShape()
  val shape2 = MaterialShapes.Cookie12Sided.toShape()

  Box(
      modifier =
          Modifier.padding(
                  horizontal = CalendarUiDefaults.ItemHorizontalPadding,
                  vertical = CalendarUiDefaults.ItemVerticalPadding)
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
        Box(
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .size(100.dp)
                    .offset(y = (10).dp, x = 10.dp)
                    .rotate(75f)
                    .border(width = 2.dp, color = onDialog.copy(alpha = 0.2f), shape = shape1)
                    .clip(shape1)
                    .background(onDialog.copy(alpha = 0f)))

        Box(
            modifier =
                Modifier.size(100.dp)
                    .offset(y = (-10).dp, x = (-10).dp)
                    .rotate(30f)
                    .border(width = 2.dp, color = onDialog.copy(alpha = 0.2f), shape = shape2)
                    .clip(shape2)
                    .background(onDialog.copy(alpha = 0f)))
        Column(modifier = Modifier.fillMaxWidth()) {
          Box(
              modifier =
                  Modifier
                      .fillMaxWidth()
                      .padding(
                          horizontal = cuid.ItemHorizontalPadding,
                          vertical = cuid.StandardItemContentVerticalPadding),
              contentAlignment = Alignment.TopStart) {
                Column(verticalArrangement = Arrangement.Top) {
                  Text(
                      text = stringResource(R.string.calendar_access_title),
                      color = cardTextColor,
                      style = textStyle,
                      fontFamily = cardFontFamily,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis)
                  Spacer(Modifier.height(8.dp))
                  Text(
                      text = stringResource(R.string.calendar_access_body),
                      color = cardTextColor,
                      style = Typography.bodyMedium)
                }
              }
          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(horizontal = cuid.ItemHorizontalPadding, vertical = 4.dp),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { onGrantAccessClick() },
                    contentPadding = PaddingValues(horizontal = 12.dp)) {
                      Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                      Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                      Text(stringResource(R.string.allow_access))
                    }
              }
        }
      }
}

@Preview(showBackground = true, wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE)
@Composable
fun CalendarAccessEventPreview() {
  CaliindaTheme { CalendarAccessEvent(onGrantAccessClick = {}) }
}
