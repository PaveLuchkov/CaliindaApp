package com.lpavs.caliinda.feature.agent.presentation.indicators

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star // Make sure this import works
import androidx.graphics.shapes.toPath

private const val STAR_VERTICES = 17
private val STAR_ROUNDING = CornerRounding(0.95f)
private const val STAR_INNER_RADIUS_RATIO = 0.4f

object AiStarShape : Shape {
  private val starPolygon =
      RoundedPolygon.star(
          numVerticesPerRadius = STAR_VERTICES,
          innerRadius = STAR_INNER_RADIUS_RATIO,
          rounding = STAR_ROUNDING)
  private val matrix = Matrix()

  override fun createOutline(
      size: Size,
      layoutDirection: LayoutDirection,
      density: Density
  ): Outline {
      matrix.reset()
      val scale = minOf(size.width, size.height) / 2f
    matrix.scale(scale, scale)
      matrix.translate(size.width / (2f * scale), size.height / (2f * scale))

      val path = starPolygon.toPath().asComposePath()
      path.transform(matrix)

    return Outline.Generic(path)
  }
}
