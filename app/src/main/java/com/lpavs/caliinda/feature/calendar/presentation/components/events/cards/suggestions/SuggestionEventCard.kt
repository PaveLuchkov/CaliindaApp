package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.suggestions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.lpavs.caliinda.R
import com.lpavs.caliinda.core.data.remote.calendar.dto.EventDto
import com.lpavs.caliinda.core.ui.theme.CalendarUiDefaults
import com.lpavs.caliinda.core.ui.theme.CaliindaTheme
import com.lpavs.caliinda.core.ui.theme.Typography
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.util.RoundedPolygonShape
import com.lpavs.caliinda.core.ui.util.calculateShapeContainerSize
import com.lpavs.caliinda.core.ui.util.lerpOkLab
import com.lpavs.caliinda.feature.calendar.data.EventUiModel
import com.lpavs.caliinda.feature.calendar.data.GeneratedShapeParams
import com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar.normalEvent

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun SuggestionEventCard(
    uiModel: EventUiModel,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val current = uiModel.isCurrent
    val micro = uiModel.isMicroEvent
    val shapeParams = uiModel.shapeParams
    val targetHeight = if (isExpanded) uiModel.expandedHeight else uiModel.baseHeight
    val targetElevatioion = if (isExpanded) cuid.CurrentEventElevation else 0.dp
    val animatedHeight by
    animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(durationMillis = 250),
        label = "eventItemHeightAnimation")
    val animatedElevation by
    animateDpAsState(
        targetValue = targetElevatioion,
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
    val borderColor = colorScheme.onSecondaryContainer

    val clipStar = remember(starShape) { RoundedPolygonShape(polygon = starShape) }
    val darkTheme = isSystemInDarkTheme()
    val shadowColor = if (!darkTheme) Color.Black else Color.White
    val cardShape = RoundedCornerShape(cuid.EventItemCornerRadius)
    val cardElevation =
        if (current)
            Shadow(
                radius = 5.dp,
                spread = 2.dp,
                color = shadowColor.copy(0.5f),
                offset = DpOffset(x = 0.dp, 4.dp))
        else
            Shadow(
                radius = 0.dp,
                spread = 0.dp,
                color = Color.Black.copy(0.5f),
                offset = DpOffset(x = 0.dp, 0.dp))
    val starShadow =
        Shadow(
            radius = 0.dp,
            spread = 0.dp,
            color = Color.Black.copy(0.22f),
            offset = DpOffset(x = shapeParams.shadowOffsetXSeed, y = shapeParams.shadowOffsetYSeed))

    val starContainerSize =
        remember(uiModel.durationMinutes, micro) {
            if (micro || uiModel.durationMinutes <= 0L) 0.dp
            else calculateShapeContainerSize(uiModel.durationMinutes)
        }

    val mainColor = colorScheme.secondaryContainer.copy(0.7f)
    val highlightColor = colorScheme.tertiaryContainer
    val onMainColor = colorScheme.onSecondaryContainer
    val onHighlightColor = colorScheme.onTertiaryContainer
    val transitionColorCard =
        lerpOkLab(
            start = mainColor,
            stop = highlightColor,
            fraction = uiModel.proximityRatio)
    //  val darkerShadowColor = Color.Black

    val starBackground =
        when {
            current -> highlightColor
            uiModel.isNext -> transitionColorCard
            else -> mainColor
        }
    val cardBackground by
    animateColorAsState(
        if (current) highlightColor else mainColor,
        label = "card color")
    val cardTextColor =
        when {
            current -> onMainColor // Выделяем текущее
            else -> onHighlightColor // Обычный фон
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
                                FontVariation.weight(600),
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
            Modifier
                .padding(
                horizontal = CalendarUiDefaults.ItemHorizontalPadding,
                vertical = CalendarUiDefaults.ItemVerticalPadding)
                .dropShadow(shape = cardShape, shadow = cardElevation)
                .shadow(elevation = animatedElevation, shape = cardShape, clip = false)
                .clip(cardShape)
                .background(cardBackground)
                .drawBehind {
                    val outline = cardShape.createOutline(size, layoutDirection, this)
                    drawOutline(
                        outline = outline,
                        color = borderColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f))
                        )
                    )
                }
                .height(animatedHeight)
                .pointerInput(uiModel.id) {
                    detectTapGestures(
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onToggleExpand()
                        },
                        )
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
                                .innerShadow(shape = clipStar, shadow = starShadow)

                    )
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
                    Button(
                        onClick = { onApprove },
                        contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "Approve Suggestion")
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Approve")
                    }
                    FilledIconButton(
                        onClick = { onDeny() },
                        modifier =
                            Modifier.minimumInteractiveComponentSize()
                                .size(
                                    IconButtonDefaults.smallContainerSize(
                                        IconButtonDefaults.IconButtonWidthOption.Narrow)),
                        shape = IconButtonDefaults.smallRoundShape) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Remove Suggestion",
                        )
                    }
                }
            } // Конец AnimatedVisibility
        }
    } // Конец Column (контент + кнопки)
} // Конец корневого Box

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
        isCurrent = false,
        isNext = true,
        proximityRatio = 0.2f,
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
                isAllDay = false,
                isPhantom = true),
        isPhantom = true)

@Preview(showBackground = true, wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE, apiLevel = 29)
@Composable
fun CalendarEventPreview() {
    CaliindaTheme {
        SuggestionEventCard(
            onToggleExpand = {},
            isExpanded = false,
            uiModel = normalEvent,
            onDeny = {},
            onApprove = {}
        )

    }
}
