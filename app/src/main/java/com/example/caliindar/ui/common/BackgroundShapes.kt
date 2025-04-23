package com.example.caliindar.ui.common

import RoundedPolygonShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme // <-- Импорт
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.compose.ui.platform.LocalDensity

@Composable
fun BackgroundShapes(colorScheme: ColorScheme, modifier: Modifier = Modifier) { // Принимаем ColorScheme
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            //    .graphicsLayer(alpha = 0.5f) // Можно сделать полупрозрачным, если нужно
    ) {
        val starShape = remember {
            RoundedPolygon.star(17, rounding = CornerRounding(0.95f))
        }
        val clipStar = remember(starShape) {
            RoundedPolygonShape(polygon = starShape)
        }
        val star2Shape = remember {
            RoundedPolygon.star(4, rounding = CornerRounding(0.4f), radius = 2f)
        }
        val clip2Star = remember(star2Shape) { // Исправлено: использовать star2Shape
            RoundedPolygonShape(polygon = star2Shape)
        }
        val starContainerSize = 300.dp
        val star2ContainerSize = 200.dp

        // Фигура 1
        Box(
            modifier = Modifier
                .size(starContainerSize)
                .align(Alignment.TopEnd)
                .offset(
                    x = starContainerSize * 0.2f,
                    y = -starContainerSize * 0.1f
                )
                // --- ЗАМЕНЯЕМ .shadow() НА .graphicsLayer ---
                .graphicsLayer {
                    val elevationPx = 16.dp.toPx() // Конвертируем dp в пиксели
                    shadowElevation = elevationPx
                    shape = clipStar // Используем ту же фигуру
                    // clip = false // clip по умолчанию false в graphicsLayer
                    alpha = 0.99f // Иногда помогает "форсировать" создание слоя, если тень не появляется
                }
                // ------------------------------------------
                .clip(clipStar) // Обрезка остается после graphicsLayer
                .background(colorScheme.surfaceVariant) // Фон остается
        )

        // Фигура 2
        Box(
            modifier = Modifier
                .size(star2ContainerSize)
                .align(Alignment.TopStart)
                .offset(
                    x = -star2ContainerSize * 0.4f,
                    y = star2ContainerSize * 1.5f
                )
                // --- ЗАМЕНЯЕМ .shadow() НА .graphicsLayer ---
                .graphicsLayer {
                    val elevationPx = 6.dp.toPx() // Другое значение, тоже в пиксели
                    shadowElevation = elevationPx
                    shape = clip2Star
                }
                // ------------------------------------------
                .clip(clip2Star)
                .background(colorScheme.surfaceVariant)
        )

    }
}