package com.example.caliinda.ui.common

import RoundedPolygonShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star

enum class BackgroundShapeContext {
    Main,
    EventCreation
}

@Composable
fun BackgroundShapes(context: BackgroundShapeContext = BackgroundShapeContext.Main) {
    Box(
        modifier = Modifier
            .fillMaxSize()
        //    .graphicsLayer(alpha = 0.5f) // Можно сделать полупрозрачным, если нужно
    ) {
        // Используем when для выбора фигур в зависимости от контекста
        when (context) {
            BackgroundShapeContext.Main -> {

                val starShape = remember {
                    RoundedPolygon.star(17, rounding = CornerRounding(0.95f))
                }
                val clipStar = remember(starShape) {
                    RoundedPolygonShape(polygon = starShape)
                }
                val star2Shape = remember {
                    RoundedPolygon.star(4, rounding = CornerRounding(0.4f), radius = 2f)
                }
                val clip2Star = remember(star2Shape) {
                    RoundedPolygonShape(polygon = star2Shape)
                }
                val starContainerSize = 300.dp
                val star2ContainerSize = 200.dp

                // Фигура 1 (звезда 17)
                Box(
                    modifier = Modifier
                        .size(starContainerSize)
                        .align(Alignment.TopEnd)
                        .offset(
                            x = starContainerSize * 0.2f,
                            y = -starContainerSize * 0.1f
                        )
                        .graphicsLayer {
                            shadowElevation = 16.dp.toPx()
                            shape = clipStar
                            alpha = 0.99f // Оставляем для надежности
                        }
                        .clip(clipStar)
                        .background(colorScheme.surfaceVariant)
                )

                // Фигура 2 (звезда 4)
                Box(
                    modifier = Modifier
                        .size(star2ContainerSize)
                        .align(Alignment.TopStart)
                        .offset(
                            x = -star2ContainerSize * 0.4f,
                            y = star2ContainerSize * 1.5f // Позиция относительно TopStart
                        )
                        .graphicsLayer {
                            shadowElevation = 6.dp.toPx()
                            shape = clip2Star
                        }
                        .clip(clip2Star)
                        .background(colorScheme.surfaceVariant)
                )
            }

            BackgroundShapeContext.EventCreation -> {
                val starShape = remember {
                    RoundedPolygon.star(3, rounding = CornerRounding(0.2f))
                }
                val clipStar = remember(starShape) {
                    RoundedPolygonShape(polygon = starShape)
                }
                val star2Shape = remember {
                    RoundedPolygon.star(5, rounding = CornerRounding(0.4f), radius = 2f)
                }
                val clip2Star = remember(star2Shape) {
                    RoundedPolygonShape(polygon = star2Shape)
                }
                val starContainerSize = 200.dp
                val star2ContainerSize = 300.dp

                // Фигура 1 (звезда 17)
                Box(
                    modifier = Modifier
                        .size(starContainerSize)
                        .align(Alignment.TopEnd)
                        .offset(
                            x = starContainerSize * 0.2f,
                            y = -starContainerSize * 0.1f
                        )
                        .graphicsLayer {
                            shape = clipStar
                            alpha = 0.99f // Оставляем для надежности
                        }
                        .clip(clipStar)
                        .background(colorScheme.surfaceVariant)
                )

                // Фигура 2 (звезда 4)
                Box(
                    modifier = Modifier
                        .size(star2ContainerSize)
                        .align(Alignment.TopStart)
                        .offset(
                            x = -star2ContainerSize * 0.4f,
                            y = star2ContainerSize * 2f // Позиция относительно TopStart
                        )
                        .graphicsLayer {
                            shape = clip2Star
                        }
                        .clip(clip2Star)
                        .background(colorScheme.surfaceVariant)
                )
            }
        }
    }
}