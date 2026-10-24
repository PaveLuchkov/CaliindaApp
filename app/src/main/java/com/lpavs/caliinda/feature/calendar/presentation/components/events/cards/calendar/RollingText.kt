package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
 *
 * [order] — порядок надписи (например, день): выросла — барабан крутится в другую сторону,
 * новая въезжает снизу, как следующий день при листании вверх.
 *
 * [screen] — индекс экрана в горизонтальном пейджере: сменился — надпись едет вбок вслед за
 * листанием, а не барабаном.
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
    order: Long? = null,
    screen: Int? = null,
) {
  AnimatedContent(
      targetState = RollingLabel(text, order, screen),
      transitionSpec = {
        val fromScreen = initialState.screen
        val toScreen = targetState.screen
        if (fromScreen != null && toScreen != null && fromScreen != toScreen) {
          // Вправо на следующий экран — новая въезжает справа, старая уходит влево.
          val right = if (toScreen > fromScreen) 1 else -1
          return@AnimatedContent (slideInHorizontally(AppMotion.defaultSpatialSpec()) { it * right } +
                  fadeIn(AppMotion.fastEffectsSpec()))
              .togetherWith(
                  slideOutHorizontally(AppMotion.defaultSpatialSpec()) { -it * right } +
                      fadeOut(AppMotion.fastEffectsSpec()))
              .using(SizeTransform(clip = false))
        }
        val from = initialState.order
        val to = targetState.order
        // 1 — новая сверху, старая вниз; -1 — наоборот.
        val down = if (from != null && to != null && to > from) -1 else 1
        (slideInVertically(AppMotion.defaultSpatialSpec()) { -it * down } + fadeIn(AppMotion.fastEffectsSpec()))
            .togetherWith(
                slideOutVertically(AppMotion.defaultSpatialSpec()) { it * down } +
                    fadeOut(AppMotion.fastEffectsSpec()))
            .using(SizeTransform(clip = false))
      },
      contentKey = { it.text },
      modifier = modifier.clipToBounds(),
      label = "rollingText") { (shown) ->
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

private data class RollingLabel(val text: String, val order: Long?, val screen: Int?)
