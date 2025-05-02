package com.example.caliindar.ui.screens.main.components.calendarui

import RoundedPolygonShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
// import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.example.caliindar.data.local.DateTimeUtils.parseToInstant
import com.example.caliindar.ui.screens.main.CalendarEvent
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.exp

val cuid =  CalendarUiDefaults

@Composable
fun EventListItem(
    event: CalendarEvent,
    timeFormatter: (CalendarEvent) -> String,
    isCurrentEvent: Boolean, // Получаем снаружи
    isNextEvent: Boolean,
    proximityRatio: Float,
    modifier: Modifier = Modifier, // Позволяем контейнеру управлять внешними отступами/размером
    currentTimeZoneId: String
) {
    // --- Расчет высоты  ---
    val eventDurationMinutes = remember(event.startTime, event.endTime) {
        val start = parseToInstant(event.startTime, currentTimeZoneId)
        val end = parseToInstant(event.endTime, currentTimeZoneId)
        if (start != null && end != null && end.isAfter(start)) {
            Duration.between(start, end).toMinutes()
        } else {
            0L
        }
    }

    val isMicroEvent = remember(eventDurationMinutes) {
        eventDurationMinutes in 1..cuid.MicroEventMaxDurationMinutes
    }

    val targetHeight = remember(isMicroEvent, eventDurationMinutes) {
        calculateEventHeight(eventDurationMinutes, isMicroEvent)
    }

    // --- Генерация формы ---
    val shapeParams = remember(event.id) {
        generateShapeParams(event.id) // Use helper
    }

    val starShape = remember(shapeParams.numVertices, shapeParams.radiusSeed) {
        RoundedPolygon.star(
            numVerticesPerRadius = shapeParams.numVertices,
            radius = shapeParams.radiusSeed,
            innerRadius = cuid.SHAPEINNERRADIUS,
            rounding = CornerRounding(cuid.ShapeCornerRounding)
        )
    }
    val clipStar = remember(starShape) { RoundedPolygonShape(polygon = starShape) }
    val clip2Star = remember(starShape) { RoundedPolygonShape(polygon = starShape) }

    val starContainerSize = remember(eventDurationMinutes) {
        calculateShapeContainerSize(eventDurationMinutes)
    }


    // Compute the transitionColor
    val transitionColorCard = colorScheme.primaryContainer.transitionTo(colorScheme.tertiaryContainer, proximityRatio)
    val transitionColorText = colorScheme.onPrimaryContainer.transitionTo(colorScheme.onTertiaryContainer, proximityRatio)

    val starOffsetY = starContainerSize * shapeParams.offestParam
    val starOffsetX = starContainerSize * -shapeParams.offestParam
    val rotationAngle = shapeParams.rotationAngle
    val shadowColor = Color.Black.copy(alpha = 0.3f)
    val density = LocalDensity.current
    val darkerShadowColor = Color.Black

    // --- Параметры текущего события (получаем isCurrentEvent) ---
 //   val fixedColors = LocalFixedAccentColors.current

    val cardElevation = if (isCurrentEvent) cuid.CurrentEventElevation else 0.dp
    val cardBackground = when {
        isCurrentEvent -> colorScheme.tertiaryContainer // Выделяем текущее
        isNextEvent -> transitionColorCard// Слегка выделяем следующее (пример)
        else -> colorScheme.primaryContainer // Обычный фон
    }

    val cardTextColor = when {
        isCurrentEvent -> colorScheme.onTertiaryContainer // Выделяем текущее
        isNextEvent -> transitionColorText// Слегка выделяем следующее (пример)
        else -> colorScheme.onPrimaryContainer // Обычный фон
    }
    // --- Композиция UI ---
    Box(
        modifier = modifier // Применяем модификатор от родителя
            // Убрали внешний padding отсюда
            .shadow(
                elevation = cardElevation,
                shape = RoundedCornerShape(cuid.EventItemCornerRadius),
                clip = false,
                ambientColor = if (cardElevation > 0.dp) darkerShadowColor else Color.Transparent,
                spotColor = if (cardElevation > 0.dp) darkerShadowColor else Color.Transparent
            )
            .clip(RoundedCornerShape(cuid.EventItemCornerRadius))
            .background(cardBackground)
            .height(targetHeight) // Высота применяется здесь
    ) {
        // --- Декорация (оставлена без изменений по запросу) ---
        if (!isMicroEvent) {
            // Тень
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer(
                        translationX = with(density) { (starOffsetX + shapeParams.shadowOffsetXSeed).toPx() },
                        translationY = with(density) { (starOffsetY - shapeParams.shadowOffsetYSeed).toPx() },
                        rotationZ = rotationAngle,
                        /*
                        renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            BlurEffect(radiusX = cuid.StarShadowBlurRadius, radiusY = cuid.StarShadowBlurRadius, edgeTreatment = TileMode.Decal)
                        } else null

                         */
                    )
                    .requiredSize(starContainerSize)
                    .clip(clip2Star)
                    .background(shadowColor)
            )
            // Основная фигура
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer(
                        translationX = with(density) { starOffsetX.toPx() },
                        translationY = with(density) { starOffsetY.toPx() },
                        rotationZ = rotationAngle
                    )
                    .requiredSize(starContainerSize)
                    .clip(clipStar)
                    .background(cardBackground.copy(alpha = cuid.ShapeMainAlpha)) // Используем фон карточки
            )
        }

        // --- Текстовый контент ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Используем константы для внутренних отступов
                .padding(
                    horizontal = cuid.ItemHorizontalPadding, // Внутренний горизонтальный
                    vertical = if (isMicroEvent) cuid.MicroItemContentVerticalPadding else cuid.StandardItemContentVerticalPadding
                ),
            contentAlignment = Alignment.TopStart
        ) {
            if (isMicroEvent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.summary,
                        color = cardTextColor,
                        style = typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(cuid.HeaderBottomSpacing))
                    Text(
                        text = timeFormatter(event),
                        color = cardTextColor,
                        style = typography.labelMedium,
                        maxLines = 1
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = event.summary,
                        color = cardTextColor,
                        style = typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeFormatter(event),
                        color = cardTextColor,
                        style = typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                        maxLines = 1
                    )
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

        // Sigmoid calculation
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

    // Use different simple transformations of the hash for variety
    val shadowOffsetXSeed = absHashCode % cuid.ShapeShadowOffsetXMaxModulo
    val shadowOffsetYSeed = absHashCode % cuid.ShapeShadowOffsetYMaxModulo + cuid.ShapeShadowOffsetYMin

    val offsetParam = (absHashCode % 4 + 1) * cuid.ShapeOffsetParamMultiplier

    val radiusBaseHash = absHashCode / 3 + 42
    val radiusSeed = ((radiusBaseHash % cuid.ShapeRadiusSeedRangeModulo) * cuid.ShapeRadiusSeedRange) + cuid.ShapeRadiusSeedMin
    val coercedRadiusSeed = radiusSeed.coerceIn(cuid.ShapeRadiusSeedMin, cuid.ShapeMaxRadius)

    val angleSeed = (abs(hashCode) / 5 - 99).mod(cuid.ShapeRotationMaxDegrees)
    val rotationAngle = (angleSeed + cuid.ShapeRotationOffsetDegrees).toFloat()

    return GeneratedShapeParams(
        numVertices = numVertices,
        radiusSeed = coercedRadiusSeed,
        rotationAngle = rotationAngle,
        shadowOffsetXSeed = shadowOffsetXSeed.dp, // Convert to Dp here
        shadowOffsetYSeed = shadowOffsetYSeed.dp, // Convert to Dp here
        offestParam = offsetParam
    )
}

fun Color.transitionTo(target: Color, factor: Float): Color {
    val startHsl = FloatArray(3)
    val targetHsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), startHsl)
    ColorUtils.colorToHSL(target.toArgb(), targetHsl)
    val h = startHsl[0] + (targetHsl[0] - startHsl[0]) * factor
    val s = startHsl[1] + (targetHsl[1] - startHsl[1]) * factor
    val l = startHsl[2] + (targetHsl[2] - startHsl[2]) * factor
    val newHsl = floatArrayOf(h,s,l)
    return Color(ColorUtils.HSLToColor(newHsl))
}