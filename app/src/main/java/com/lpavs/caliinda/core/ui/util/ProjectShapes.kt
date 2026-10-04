package com.lpavs.caliinda.core.ui.util

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.graphics.Shape
import kotlin.math.abs

/**
 * Свои фигуры для карточек проектов — мягкие MaterialShapes, чтобы проекты отличались от звёзд
 * событий дня. Форма выбирается по id: у каждого проекта своя и не меняется.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val projectFamily by lazy {
  listOf(
      MaterialShapes.Clover4Leaf,
      MaterialShapes.Clover8Leaf,
      MaterialShapes.Flower,
      MaterialShapes.Sunny,
      MaterialShapes.VerySunny,
      MaterialShapes.Cookie6Sided,
      MaterialShapes.Cookie9Sided,
      MaterialShapes.Cookie12Sided,
      MaterialShapes.Puffy,
      MaterialShapes.PuffyDiamond,
      MaterialShapes.Bun,
      MaterialShapes.Gem,
  )
}

fun projectShape(eventId: String): Shape =
    RoundedPolygonShape(projectFamily[abs(eventId.hashCode()) % projectFamily.size])
