package com.lpavs.caliinda.core.ui.util

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import kotlin.math.abs
import kotlin.math.max

/** Как выглядят фигуры на карточках проектов. Временный переключатель — сравниваем варианты. */
enum class ProjectShapeStyle {
  /** Фигура перетекает из острой в круг по мере того, как проект идёт. */
  PROGRESS,
  /** Своё семейство мягких MaterialShapes, форма выбирается по id. */
  FAMILY,
}

val PROJECT_SHAPE_STYLE = ProjectShapeStyle.FAMILY

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val projectMorph by lazy { Morph(MaterialShapes.SoftBurst, MaterialShapes.Circle) }

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

/** Фигура для карточки проекта: [progress] — доля прошедшего времени проекта (0…1). */
fun projectShape(eventId: String, progress: Float): Shape =
    when (PROJECT_SHAPE_STYLE) {
      ProjectShapeStyle.PROGRESS -> MorphShape(projectMorph, progress.coerceIn(0f, 1f))
      ProjectShapeStyle.FAMILY ->
          RoundedPolygonShape(projectFamily[abs(eventId.hashCode()) % projectFamily.size])
    }

/** Промежуточная форма [morph] на шаге [progress], вписанная в размер элемента. */
class MorphShape(morph: Morph, progress: Float) : Shape {
  private val basePath = morph.toPath(progress).asComposePath()
  private val bounds = basePath.getBounds()

  override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
    val maxDimension = max(bounds.width, bounds.height)
    val matrix =
        Matrix().apply {
          scale(size.width / maxDimension, size.height / maxDimension)
          translate(-bounds.left, -bounds.top)
        }
    return Outline.Generic(Path().apply { addPath(basePath) }.also { it.transform(matrix) })
  }
}

