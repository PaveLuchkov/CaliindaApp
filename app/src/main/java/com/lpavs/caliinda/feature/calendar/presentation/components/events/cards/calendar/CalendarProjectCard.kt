package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar

import com.lpavs.caliinda.core.ui.theme.AppMotion
import com.lpavs.caliinda.core.ui.theme.CaliindaFonts
import java.time.OffsetDateTime

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.calendar.model.EventDto
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.CaliindaTheme
import com.lpavs.caliinda.core.ui.theme.Typography
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.util.projectShape
import com.lpavs.caliinda.core.ui.util.calculateProjectShapeContainerSize
import com.lpavs.caliinda.core.ui.util.lerpOkLab
import com.lpavs.caliinda.feature.calendar.data.EventUiModel
import com.lpavs.caliinda.feature.calendar.data.GeneratedShapeParams

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun CalendarProjectItem(
    uiModel: EventUiModel,
    isExpanded: Boolean,
    //    highlightAction: PreviewAction?,
    onToggleExpand: () -> Unit,
    onDetailsClickFromList: () -> Unit,
    onDeleteClickFromList: () -> Unit,
    onEditClickFromList: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val darkTheme = isSystemInDarkTheme()
  val haptic = LocalHapticFeedback.current
  val current = uiModel.isCurrent
  val micro = uiModel.isMicroEvent
  val shapeParams = uiModel.shapeParams
  val targetElevatioion = if (isExpanded) cuid.CurrentEventElevation else 0.dp
  val targetHeight = if (isExpanded) uiModel.expandedHeight else uiModel.baseHeight
  val animatedHeight by
      animateDpAsState(
          targetValue = targetHeight,
          animationSpec = AppMotion.defaultSpatialSpec(),
          label = "eventItemHeightAnimation")
  val animatedElevation by
      animateDpAsState(
          targetValue = targetElevatioion,
          animationSpec = AppMotion.defaultEffectsSpec(),
          label = "eventItemHeightAnimation")
  val shadowColor = if (!darkTheme) Color.Black else Color.White
  val cardElevation =
      Shadow(
          radius = 0.dp,
          spread = 0.dp,
          color = shadowColor.copy(0.5f),
          offset = DpOffset(x = 0.dp, 0.dp))
  // У проектов свои фигуры, отличные от звёзд событий дня (см. ProjectShapes.kt).
  val clipStar = remember(uiModel.id) { projectShape(uiModel.id) }
  val starContainerSize =
      remember(uiModel.durationMinutes, micro) {
        if (micro || uiModel.durationMinutes <= 0L) 0.dp
        else calculateProjectShapeContainerSize(uiModel.durationMinutes)
      }

  val transitionColorCard =
      lerpOkLab(
          start = colorScheme.primaryContainer,
          stop = colorScheme.tertiaryContainer,
          fraction = uiModel.proximityRatio)
  val starShadow =
      Shadow(
          radius = 0.dp,
          spread = 0.dp,
          color = Color.Black.copy(0.22f),
          offset = DpOffset(x = shapeParams.shadowOffsetXSeed, y = shapeParams.shadowOffsetYSeed))

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
  val cardShape = RoundedCornerShape(cuid.EventItemCornerRadius)

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
            CaliindaFonts.ProjectCurrent
        else ->
            CaliindaFonts.Card
      }
  // --- Композиция UI ---
  Box( // Корневой Box для тени, фона, высоты и кликабельности
      modifier =
          Modifier.padding(
                  horizontal = CalendarUiDefaults.ItemHorizontalPadding,
                  vertical = CalendarUiDefaults.ItemVerticalPadding)
              .dropShadow(shape = cardShape, shadow = cardElevation)
              .shadow(elevation = animatedElevation, shape = cardShape, clip = false)
              .clip(cardShape)
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
                  Box( // Основная фигура
                      modifier =
                          Modifier.align(Alignment.CenterEnd) // Позиционирование звезды
                              .graphicsLayer(
                                  translationX = with(density) { starOffsetX.toPx() },
                                  translationY = with(density) { starOffsetY.toPx() },
                                  rotationZ = rotationAngle)
                              .requiredSize(starContainerSize)
                              .clip(clipStar)
                              .background(starBackground)
                              // Тень поверх заливки, как у событий дня: иначе заливка её закрывает.
                              .innerShadow(shape = clipStar, shadow = starShadow))
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
                      uiModel.daysLeft?.let {
                        Text(
                            text =
                                if (it == 1L) stringResource(R.string.project_last_day)
                                else
                                    pluralStringResource(
                                        R.plurals.project_days_left, it.toInt(), it.toInt()),
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
                  fadeIn(animationSpec = AppMotion.fastEffectsSpec()) +
                      expandVertically(
                          animationSpec = AppMotion.defaultSpatialSpec(),
                          expandFrom = Alignment.Top),
              exit =
                  shrinkVertically(
                      animationSpec = AppMotion.defaultSpatialSpec(), shrinkTowards = Alignment.Top) +
                      fadeOut(animationSpec = AppMotion.fastEffectsSpec())) {
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
                                contentDescription = stringResource(R.string.details),
                            )
                          }
                      Spacer(modifier = Modifier.width(4.dp))
                      Button(
                          onClick = { onEditClickFromList() },
                          contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.edit))
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
                                contentDescription = stringResource(R.string.delete),
                            )
                          }
                    }
              } // Конец AnimatedVisibility
        }
      } // Конец Column (контент + кнопки)
} // Конец корневого Box

val normalProjectEvent =
    EventUiModel(
        id = "1",
        summary = "SQL Practice: Query Building",
        isAllDay = false,
        formattedTimeString = "30 сентября - 11 октября",
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
                startTime = OffsetDateTime.parse("2025-09-18T00:00:00+03:00").toInstant(),
                endTime = OffsetDateTime.parse("2025-10-19T00:00:00+03:00").toInstant(),
                description = null,
                location = null,
                isAllDay = false),
    )

@Preview(showBackground = true, wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE)
@Composable
fun CalendarProjectItemPreview() {
  CaliindaTheme {
    CalendarProjectItem(
        onToggleExpand = {},
        onDetailsClickFromList = {},
        isExpanded = false,
        onEditClickFromList = {},
        onDeleteClickFromList = {},
        uiModel = normalProjectEvent)
  }
}
