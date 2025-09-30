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

fun lerpOkLab(start: Color, stop: Color, fraction: Float): Color {
    val startOklab = start.convert(ColorSpaces.Oklab)
    val stopOklab = stop.convert(ColorSpaces.Oklab)

    val l = startOklab.component1() + (stopOklab.component1() - startOklab.component1()) * fraction
    val a = startOklab.component2() + (stopOklab.component2() - startOklab.component2()) * fraction
    val b = startOklab.component3() + (stopOklab.component3() - startOklab.component3()) * fraction
    val alpha = startOklab.alpha + (stopOklab.alpha - startOklab.alpha) * fraction

    return Color(l, a, b, alpha, ColorSpaces.Oklab).convert(ColorSpaces.Srgb)
}