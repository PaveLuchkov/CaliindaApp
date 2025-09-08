package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.remote.agent.PreviewAction
import com.lpavs.caliinda.core.data.remote.calendar.dto.EventDto
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.Typography
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.util.RoundedPolygonShape
import com.lpavs.caliinda.feature.calendar.data.EventUiModel
import com.lpavs.caliinda.feature.calendar.data.GeneratedShapeParams
import kotlin.math.exp

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun CalendarEventItem(
    uiModel: EventUiModel,
    isExpanded: Boolean,
    highlightAction: PreviewAction?,
    onToggleExpand: () -> Unit,
    onDetailsClickFromList: () -> Unit,
    onDeleteClickFromList: () -> Unit,
    onEditClickFromList: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val current = uiModel.isCurrent
  val micro = uiModel.isMicroEvent
  val shapeParams = uiModel.shapeParams
  val targetHeight = if (isExpanded) uiModel.expandedHeight else uiModel.baseHeight
  val animatedHeight by
      animateDpAsState(
          targetValue = targetHeight,
          animationSpec = tween(durationMillis = 250),
          label = "eventItemHeightAnimation")
  val starShape =
      remember(shapeParams.numVertices, shapeParams.radiusSeed) {
        RoundedPolygon.star(
            numVerticesPerRadius = shapeParams.numVertices,
            radius = shapeParams.radiusSeed,
            innerRadius = cuid.SHAPEINNERRADIUS,
            rounding = CornerRounding(cuid.ShapeCornerRounding))
      }
  val borderColor =
      when (highlightAction) {
        PreviewAction.SEARCH -> colorScheme.tertiary
        PreviewAction.DELETE -> colorScheme.error
        PreviewAction.UPDATE -> colorScheme.primaryContainer
        else -> Color.Transparent
      }

  val clipStar = remember(starShape) { RoundedPolygonShape(polygon = starShape) }
  val clip2Star = remember(starShape) { RoundedPolygonShape(polygon = starShape) }

  val starContainerSize =
      remember(uiModel.durationMinutes, micro) {
        if (micro || uiModel.durationMinutes <= 0L) 0.dp
        else calculateShapeContainerSize(uiModel.durationMinutes)
      }

  val transitionColorCard =
      lerpOkLab(
          start = colorScheme.primaryContainer,
          stop = colorScheme.tertiaryContainer,
          fraction = uiModel.proximityRatio)
  val darkerShadowColor = Color.Black

  val cardElevation = if (current) cuid.CurrentEventElevation else 0.dp
  val starBackground =
      when {
        current -> colorScheme.tertiaryContainer
        uiModel.isNext -> transitionColorCard
        else -> colorScheme.primaryContainer
      }
  val cardBackground by
      animateColorAsState(
          if (current) colorScheme.tertiaryContainer else colorScheme.primaryContainer,
          label = "card color")

  val cardTextColor =
      when {
        current -> colorScheme.onTertiaryContainer // Выделяем текущее
        else -> colorScheme.onPrimaryContainer // Обычный фон
      }
  val textStyle =
      when {
        !micro -> if (current) Typography.headlineSmallEmphasized else Typography.headlineSmall
        else -> if (current) Typography.bodyLargeEmphasized else Typography.bodyLarge
      }
  val cardFontFamily =
      when {
        current ->
            FontFamily(
                Font(
                    R.font.robotoflex_variable,
                    variationSettings =
                        FontVariation.Settings(
                            FontVariation.weight(700),
                            FontVariation.grade(70),
                            FontVariation.width(65f),
                            //                            FontVariation.opticalSizing(0.sp),
                            FontVariation.slant(-5f),
                        )))
        else ->
            FontFamily(
                Font(
                    R.font.robotoflex_variable,
                    variationSettings =
                        FontVariation.Settings(
                            FontVariation.weight(600),
                            FontVariation.width(100f),
                        )))
      }
  // --- Композиция UI ---
  Box( // Корневой Box для тени, фона, высоты и кликабельности
      modifier =
          modifier
              .padding(
                  horizontal = CalendarUiDefaults.ItemHorizontalPadding,
                  vertical = CalendarUiDefaults.ItemVerticalPadding)
              .shadow(
                  elevation = cardElevation,
                  shape = RoundedCornerShape(cuid.EventItemCornerRadius),
                  clip = false,
                  ambientColor = if (cardElevation > 0.dp) darkerShadowColor else Color.Transparent,
                  spotColor = if (cardElevation > 0.dp) darkerShadowColor else Color.Transparent)
              .clip(RoundedCornerShape(cuid.EventItemCornerRadius))
              .border(
                  BorderStroke(2.dp, borderColor),
                  shape = RoundedCornerShape(cuid.EventItemCornerRadius))
              .background(cardBackground)
              .height(animatedHeight)
              .pointerInput(uiModel.id) {
                detectTapGestures(
                    onTap = {
                      haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                      onToggleExpand()
                    },
                    onLongPress = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onDetailsClickFromList()
                    })
              }) {
        Column(modifier = Modifier.fillMaxSize()) {
          Box(
              modifier =
                  Modifier.weight(1f) // Занимает все место, ОСТАВЛЯЯ место для кнопок снизу
                      .fillMaxWidth()
                      // Внутренние отступы для текста и звезды
                      .padding(
                          horizontal = cuid.ItemHorizontalPadding,
                          vertical =
                              if (micro) cuid.MicroItemContentVerticalPadding
                              else cuid.StandardItemContentVerticalPadding),
              // Выравнивание контента можно оставить TopStart или изменить на Center, если нужно
              contentAlignment = Alignment.TopStart) {
                if (!micro && starContainerSize > 0.dp) {
                  val density = LocalDensity.current
                  val starOffsetY = starContainerSize * shapeParams.offestParam
                  val starOffsetX = starContainerSize * -shapeParams.offestParam
                  val rotationAngle = shapeParams.rotationAngle
                  val shadowColor = Color.Black.copy(alpha = 0.3f) // Переместил

                  Box( // Тень
                      modifier =
                          Modifier.align(Alignment.CenterEnd) // Позиционирование звезды
                              .graphicsLayer(
                                  translationX =
                                      with(density) {
                                        (starOffsetX + shapeParams.shadowOffsetXSeed).toPx()
                                      },
                                  translationY =
                                      with(density) {
                                        (starOffsetY - shapeParams.shadowOffsetYSeed).toPx()
                                      },
                                  rotationZ = rotationAngle)
                              .requiredSize(starContainerSize)
                              .clip(clip2Star)
                              .background(shadowColor))
                  Box( // Основная фигура
                      modifier =
                          Modifier.align(Alignment.CenterEnd) // Позиционирование звезды
                              .graphicsLayer(
                                  translationX = with(density) { starOffsetX.toPx() },
                                  translationY = with(density) { starOffsetY.toPx() },
                                  rotationZ = rotationAngle)
                              .requiredSize(starContainerSize)
                              .clip(clipStar)
                              .background(starBackground.copy(alpha = cuid.ShapeMainAlpha)))
                }
                if (micro) {
                  Row(
                      modifier = Modifier.fillMaxSize(),
                      verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = uiModel.summary,
                            color = cardTextColor,
                            style = textStyle,
                            fontFamily = cardFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                        Spacer(modifier = Modifier.width(cuid.padding))
                        Text(
                            text = uiModel.formattedTimeString,
                            color = cardTextColor,
                            style = typography.labelMedium,
                            maxLines = 1)
                      }
                } else {
                  Column(verticalArrangement = Arrangement.Top) {
                    Text(
                        text = uiModel.summary,
                        color = cardTextColor,
                        style = textStyle,
                        fontFamily = cardFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row {
                      Text(
                          text = uiModel.formattedTimeString,
                          color = cardTextColor,
                          style = typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                          maxLines = 1)
                      Spacer(modifier = Modifier.width(8.dp))
                      uiModel.location?.let {
                        Text(
                            text = it,
                            color = cardTextColor,
                            style = typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                            maxLines = 1)
                      }
                    }
                  }
                }
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
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = cuid.ItemHorizontalPadding, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                      FilledIconButton(
                          onClick = {
                            onDetailsClickFromList()
                            onToggleExpand()
                          },
                          modifier =
                              Modifier.minimumInteractiveComponentSize()
                                  .size(
                                      IconButtonDefaults.smallContainerSize(
                                          IconButtonDefaults.IconButtonWidthOption.Uniform)),
                          shape = IconButtonDefaults.smallRoundShape) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "info",
                            )
                          }
                      Spacer(modifier = Modifier.width(4.dp))
                      Button(
                          onClick = { onEditClickFromList() },
                          contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Edit")
                          }
                      FilledIconButton(
                          onClick = { onDeleteClickFromList() },
                          modifier =
                              Modifier.minimumInteractiveComponentSize()
                                  .size(
                                      IconButtonDefaults.smallContainerSize(
                                          IconButtonDefaults.IconButtonWidthOption.Narrow)),
                          shape = IconButtonDefaults.smallRoundShape) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                            )
                          }
                    }
              } // Конец AnimatedVisibility
        }
      } // Конец Column (контент + кнопки)
} // Конец корневого Box

fun calculateShapeContainerSize(durationMinutes: Long): Dp {
  val minStarContainerSize = cuid.MinStarContainerSize
  val maxStarContainerSize = cuid.MaxStarContainerSize
  val durationDouble = durationMinutes.toDouble()
  val heightRange = maxStarContainerSize - minStarContainerSize

  val x = (durationDouble - cuid.HeightSigmoidMidpointMinutes) / cuid.HeightSigmoidScaleFactor
  val k = cuid.HeightSigmoidSteepness
  val sigmoidOutput = 1.0 / (1.0 + exp(-k * x))

  val calculatedHeight = minStarContainerSize + (heightRange * sigmoidOutput.toFloat())
  return calculatedHeight.coerceIn(minStarContainerSize, maxStarContainerSize)
}

fun lerpOkLab(start: Color, stop: Color, fraction: Float): Color {
  val startOklab = start.convert(ColorSpaces.Oklab)
  val stopOklab = stop.convert(ColorSpaces.Oklab)

  val l = startOklab.component1() + (stopOklab.component1() - startOklab.component1()) * fraction
  val a = startOklab.component2() + (stopOklab.component2() - startOklab.component2()) * fraction
  val b = startOklab.component3() + (stopOklab.component3() - startOklab.component3()) * fraction
  val alpha = startOklab.alpha + (stopOklab.alpha - startOklab.alpha) * fraction

  return Color(l, a, b, alpha, ColorSpaces.Oklab).convert(ColorSpaces.Srgb)
}

val normalEvent =
    EventUiModel(
        id = "1",
        summary = "SQL Practice: Query Building",
        isAllDay = false,
        formattedTimeString = "17 - 17:45",
        durationMinutes = 45,
        isMicroEvent = false,
        baseHeight = 65.dp,
        expandedHeight = 121.dp,
        isCurrent = true,
        isNext = true,
        proximityRatio = 1f,
        shapeParams =
            GeneratedShapeParams(
                numVertices = 6,
                radiusSeed = 0.4f,
                rotationAngle = -41.0f,
                shadowOffsetYSeed = 6.0.dp,
                shadowOffsetXSeed = 6.0.dp,
                offestParam = 0.2f),
        location = null,
        originalEvent =
            EventDto(
                id = "qp919hj747psg010hiua4qvmho_20250818T140000Z",
                summary = "SQL Practice: Query Building",
                startTime = "2025-08-18T17:00:00+03:00",
                endTime = "2025-08-18T17:45:00+03:00",
                description = null,
                location = null,
                isAllDay = false))

@Preview(showBackground = true, wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE)
@Composable
fun CalendarEventPreview() {
  CalendarEventItem(
      onToggleExpand = {},
      onDetailsClickFromList = {},
      isExpanded = false,
      onEditClickFromList = {},
      onDeleteClickFromList = {},
      uiModel = normalEvent,
      highlightAction = PreviewAction.SEARCH)
}
