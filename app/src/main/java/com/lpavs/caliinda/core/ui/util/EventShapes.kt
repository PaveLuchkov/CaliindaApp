package com.lpavs.caliinda.core.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.Dp
import com.lpavs.caliinda.core.ui.theme.cuid
import kotlin.math.exp


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

/**
 * Размер фигуры для карточки проекта. Минутная шкала событий дня тут не годится: у проекта
 * десятки тысяч минут, и фигура раздувается так, что уходит за край невысокой карточки. Считаем
 * по дням — от двух дней до месяца.
 */
fun calculateProjectShapeContainerSize(durationMinutes: Long): Dp {
    val days = durationMinutes / (24f * 60f)
    val ratio = ((days - 2f) / 28f).coerceIn(0f, 1f)
    return cuid.MinStarContainerSize +
        (cuid.MaxProjectStarContainerSize - cuid.MinStarContainerSize) * ratio
}

/**
 * Доля высоты карточки проекта (0 — минимум, 1 — максимум) по его длительности. Сигмоида
 * нормирована на [ProjectHeightMinDays..ProjectHeightMaxDays]: рост плавный на всём месяце —
 * неделя ≈ четверть, две недели ≈ 70 %, без скачка в одной точке.
 */
fun projectHeightFraction(durationMinutes: Long): Float {
  fun sigmoid(days: Double) =
      1.0 / (1.0 + exp(-(days - cuid.ProjectHeightMidpointDays) / cuid.ProjectHeightScaleDays))
  val days = durationMinutes / (24.0 * 60.0)
  val low = sigmoid(cuid.ProjectHeightMinDays)
  val high = sigmoid(cuid.ProjectHeightMaxDays)
  return ((sigmoid(days) - low) / (high - low)).toFloat().coerceIn(0f, 1f)
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