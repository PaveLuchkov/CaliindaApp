package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.lpavs.caliinda.core.ui.theme.AppMotion

/**
 * Надпись, которая сменяется «барабаном»: старая уезжает вниз, новая въезжает сверху — для
 * времени ↔ весёлой фразы при «рождении» карточки. Обрезана по своей строке, чтобы прокрутка
 * шла внутри неё и не наезжала на соседние.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RollingText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
) {
  AnimatedContent(
      targetState = text,
      transitionSpec = {
        (slideInVertically(AppMotion.defaultSpatialSpec()) { -it } + fadeIn(AppMotion.fastEffectsSpec()))
            .togetherWith(
                slideOutVertically(AppMotion.defaultSpatialSpec()) { it } + fadeOut(AppMotion.fastEffectsSpec()))
            .using(SizeTransform(clip = false))
      },
      modifier = modifier.clipToBounds(),
      label = "rollingText") { shown ->
        Text(
            text = shown,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            textAlign = textAlign)
      }
}
