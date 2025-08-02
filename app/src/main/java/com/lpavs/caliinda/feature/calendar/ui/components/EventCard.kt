package com.lpavs.caliinda.feature.calendar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.ui.util.DateTimeUtils.parseToInstant
import com.lpavs.caliinda.core.ui.util.RoundedPolygonShape
import com.lpavs.caliinda.feature.calendar.data.model.CalendarEvent
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.theme.Typography
import java.time.Duration
import kotlin.math.abs
import kotlin.math.exp

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun EventItem(
    event: CalendarEvent,
    timeFormatter: (CalendarEvent) -> String,
    isCurrentEvent: Boolean,
    isNextEvent: Boolean,
    proximityRatio: Float,
    isMicroEventFromList: Boolean,
    targetHeightFromList: Dp,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDetailsClickFromList: () -> Unit,
    onDeleteClickFromList: () -> Unit,
    onEditClickFromList: () -> Unit,
    modifier: Modifier = Modifier,
    currentTimeZoneId: String
) {
  val eventDurationMinutes =
      remember(event.startTime, event.endTime, currentTimeZoneId) {
        val start = parseToInstant(event.startTime, currentTimeZoneId)
        val end = parseToInstant(event.endTime, currentTimeZoneId)
        if (start != null && end != null && end.isAfter(start)) {
          Duration.between(start, end).toMinutes()
        } else {
          0L
        }
      }
  val haptic = LocalHapticFeedback.current

  val shapeParams =
      remember(event.id) {
        generateShapeParams(event.id) // Use helper
      }

  val starShape =
      remember(shapeParams.numVertices, shapeParams.radiusSeed) {
        RoundedPolygon.star(
            numVerticesPerRadius = shapeParams.numVertices,
            radius = shapeParams.radiusSeed,
            innerRadius = cuid.SHAPEINNERRADIUS,
            rounding = CornerRounding(cuid.ShapeCornerRounding))
      }
  val clipStar = remember(starShape) { RoundedPolygonShape(polygon = starShape) }
  val clip2Star = remember(starShape) { RoundedPolygonShape(polygon = starShape) }

  val starContainerSize =
      remember(eventDurationMinutes, isMicroEventFromList) {
        if (isMicroEventFromList || eventDurationMinutes <= 0L) 0.dp
        else calculateShapeContainerSize(eventDurationMinutes)
      }

  // Compute the transitionColor
    val transitionColorCard = lerpOkLab(
        start = colorScheme.primaryContainer,
        stop = colorScheme.tertiaryContainer,
        fraction = proximityRatio
    )
  val darkerShadowColor = Color.Black

  // --- Параметры текущего события (получаем isCurrentEvent) ---
  //   val fixedColors = LocalFixedAccentColors.current

  val cardElevation = if (isCurrentEvent) cuid.CurrentEventElevation else 0.dp
  val starBackground =
      when {
        isCurrentEvent -> colorScheme.tertiaryContainer // Выделяем текущее
        isNextEvent -> transitionColorCard // Слегка выделяем следующее (пример)
        else -> colorScheme.primaryContainer // Обычный фон
      }
    val cardBackground by animateColorAsState(
        if (isCurrentEvent) colorScheme.tertiaryContainer else colorScheme.primaryContainer,
        label = "card color"
    )


    val cardTextColor =
      when {
        isCurrentEvent -> colorScheme.onTertiaryContainer // Выделяем текущее
        else -> colorScheme.onPrimaryContainer // Обычный фон
      }
    val textStyle = when{
        !isMicroEventFromList -> if (isCurrentEvent) Typography.headlineSmallEmphasized else Typography.headlineSmall
        else-> if (isCurrentEvent) Typography.bodyLargeEmphasized else Typography.bodyLarge
    }
    val cardFontFamily =
        when {
            isCurrentEvent -> FontFamily(
            Font(
                R.font.robotoflex_variable,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(700),
                    FontVariation.grade(70),
                    FontVariation.width(65f),
                    FontVariation.slant(-5f),
                )
            ))
            else -> FontFamily(
                Font(
                    R.font.robotoflex_variable,
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(600),
                        FontVariation.width(100f),
                    )
                ))
        }
  // --- Композиция UI ---
  Box( // Корневой Box для тени, фона, высоты и кликабельности
      modifier =
          modifier
              .shadow(
                  elevation = cardElevation,
                  shape = RoundedCornerShape(cuid.EventItemCornerRadius),
                  clip = false,
                  ambientColor = if (cardElevation > 0.dp) darkerShadowColor else Color.Transparent,
                  spotColor = if (cardElevation > 0.dp) darkerShadowColor else Color.Transparent)
              .clip(RoundedCornerShape(cuid.EventItemCornerRadius))
              .background(cardBackground)
              .height(targetHeightFromList)
              .pointerInput(event.id) {
                detectTapGestures(
                    onTap = { onToggleExpand() },
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
                              if (isMicroEventFromList) cuid.MicroItemContentVerticalPadding
                              else cuid.StandardItemContentVerticalPadding),
              // Выравнивание контента можно оставить TopStart или изменить на Center, если нужно
              contentAlignment = Alignment.TopStart) {
                if (!isMicroEventFromList && starContainerSize > 0.dp) {
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
                if (isMicroEventFromList) {
                  Row(
                      modifier = Modifier.fillMaxSize(),
                      verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.summary,
                            color = cardTextColor,
                            style = textStyle,
                            fontFamily = cardFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                        Spacer(modifier = Modifier.width(cuid.padding))
                        Text(
                            text = timeFormatter(event),
                            color = cardTextColor,
                            style = typography.labelMedium,
                            maxLines = 1)
                      }
                } else {
                  Column(verticalArrangement = Arrangement.Top) {
                    Text(
                        text = event.summary,
                        color = cardTextColor,
                        style = textStyle,
                        fontFamily = cardFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row {
                      Text(
                          text = timeFormatter(event),
                          color = cardTextColor,
                          style = typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                          maxLines = 1)
                      Spacer(modifier = Modifier.width(8.dp))
                      event.location?.let {
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
                  fadeIn(
                      animationSpec =
                          tween(
                              durationMillis = 150,
                              delayMillis =
                                  100)) +
                          expandVertically(
                          animationSpec =
                              tween(
                                  durationMillis = 250,
                                  delayMillis = 50),
                              expandFrom =
                              Alignment
                                  .Top
                          ),
              exit =
                  shrinkVertically(
                      animationSpec = tween(durationMillis = 250),
                      shrinkTowards = Alignment.Top
                  ) + fadeOut(animationSpec = tween(durationMillis = 150))) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(
                                horizontal = cuid.ItemHorizontalPadding,
                                vertical = 4.dp),
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

@Composable
fun AllDayEventItem(
    event: CalendarEvent,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val cardBackground = colorScheme.tertiaryContainer
  val cardTextColor = colorScheme.onTertiaryContainer
  val haptic = LocalHapticFeedback.current

  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .clip(
                  RoundedCornerShape(
                      cuid
                          .EventItemCornerRadius))
              .background(cardBackground)
              .pointerInput(event.id) {
                detectTapGestures(
                    onTap = { onToggleExpand() },
                    onLongPress = {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onDetailsClick()
                    })
              }) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(
                        horizontal = CalendarUiDefaults.AllDayItemPadding,
                        vertical = CalendarUiDefaults.AllDayItemVerticalContentPadding)) {
              Text(
                  text = event.summary,
                  style = typography.bodyLarge,
                  fontWeight = FontWeight.Medium,
                  color = cardTextColor,
                  textAlign = TextAlign.Center,
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(
                              vertical = 3.dp),
              )

              AnimatedVisibility(
                  visible = isExpanded,
                  enter =
                      fadeIn(animationSpec = tween(durationMillis = 150, delayMillis = 100)) +
                          expandVertically(
                              animationSpec = tween(durationMillis = 250, delayMillis = 50),
                              expandFrom = Alignment.Top),
                  exit =
                      shrinkVertically(
                          animationSpec = tween(durationMillis = 250),
                          shrinkTowards = Alignment.Top) +
                          fadeOut(animationSpec = tween(durationMillis = 150))) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                          Button(
                              onClick = {
                                onDetailsClick()
                                onToggleExpand()
                              },
                              contentPadding = PaddingValues(horizontal = 12.dp),
                              colors =
                                  ButtonDefaults.buttonColors(
                                      containerColor = colorScheme.onTertiary,
                                      contentColor = colorScheme.tertiary)) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = "Information",
                                    modifier = Modifier.size(ButtonDefaults.IconSize))
                              }
                          Spacer(modifier = Modifier.width(8.dp))
                          Button(
                              onClick = {
                                onEditClick()
                              },
                              contentPadding = PaddingValues(horizontal = 12.dp),
                              colors =
                                  ButtonDefaults.buttonColors(
                                      containerColor = colorScheme.onTertiary,
                                      contentColor = colorScheme.tertiary)) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Редактировать",
                                    modifier = Modifier.size(ButtonDefaults.IconSize))
                              }
                          Spacer(modifier = Modifier.width(8.dp))
                          Button(
                              onClick = {
                                onDeleteClick()
                              },
                              contentPadding = PaddingValues(horizontal = 12.dp),
                              colors =
                                  ButtonDefaults.buttonColors(
                                      containerColor = colorScheme.onTertiary,
                                      contentColor = colorScheme.tertiary)) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Удалить",
                                    modifier = Modifier.size(ButtonDefaults.IconSize))
                              }
                        }
                  }
            }
      }
}

fun calculateEventHeight(durationMinutes: Long, isMicroEvent: Boolean): Dp {
  return if (isMicroEvent) {
    cuid.MicroEventHeight
  } else {
    val minHeight = cuid.MinEventHeight
    val maxHeight = cuid.MaxEventHeight
    val durationDouble = durationMinutes.toDouble()
    val heightRange = maxHeight - minHeight

    val x = (durationDouble - cuid.HeightSigmoidMidpointMinutes) / cuid.HeightSigmoidScaleFactor
    val k = cuid.HeightSigmoidSteepness
    val sigmoidOutput = 1.0 / (1.0 + exp(-k * x))

    val calculatedHeight = minHeight + (heightRange * sigmoidOutput.toFloat())
    calculatedHeight.coerceIn(minHeight, maxHeight)
  }
}

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

fun generateShapeParams(eventId: String): GeneratedShapeParams {
  val hashCode = eventId.hashCode()
  val absHashCode = abs(hashCode)

  val numVertices = (absHashCode % cuid.ShapeMaxVerticesDelta) + cuid.ShapeMinVertices

  val shadowOffsetXSeed = absHashCode % cuid.ShapeShadowOffsetXMaxModulo
  val shadowOffsetYSeed =
      absHashCode % cuid.ShapeShadowOffsetYMaxModulo + cuid.ShapeShadowOffsetYMin

  val offsetParam = (absHashCode % 4 + 1) * cuid.ShapeOffsetParamMultiplier

  val radiusBaseHash = absHashCode / 3 + 42
  val radiusSeed =
      ((radiusBaseHash % cuid.ShapeRadiusSeedRangeModulo) * cuid.ShapeRadiusSeedRange) +
          cuid.ShapeRadiusSeedMin
  val coercedRadiusSeed = radiusSeed.coerceIn(cuid.ShapeRadiusSeedMin, cuid.ShapeMaxRadius)

  val angleSeed = (abs(hashCode) / 5 - 99).mod(cuid.ShapeRotationMaxDegrees)
  val rotationAngle = (angleSeed + cuid.ShapeRotationOffsetDegrees)

  return GeneratedShapeParams(
      numVertices = numVertices,
      radiusSeed = coercedRadiusSeed,
      rotationAngle = rotationAngle,
      shadowOffsetXSeed = shadowOffsetXSeed.dp,
      shadowOffsetYSeed = shadowOffsetYSeed.dp,
      offestParam = offsetParam)
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

