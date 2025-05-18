package com.lpavs.caliinda.ui.common

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.compose.ui.graphics.asComposePath
import androidx.graphics.shapes.toPath

class CustomRotatingMorphShape(
    private val morph: Morph,
    private val percentage: Float,
    private val rotation: Float
) : Shape {

    private val matrix = Matrix()
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // Растягиваем на размер контейнера
        matrix.reset() // Сбрасываем матрицу перед использованием
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f) // Центрируем (предполагается радиус 1f в Morph)
        matrix.rotateZ(rotation) // Вращаем

        // Получаем путь из Morph и трансформируем
        val path = morph.toPath(progress = percentage).asComposePath()
        path.transform(matrix)

        return Outline.Generic(path)
    }
}