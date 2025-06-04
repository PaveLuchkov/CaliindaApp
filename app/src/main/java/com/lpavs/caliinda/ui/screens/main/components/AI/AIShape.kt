package com.lpavs.caliinda.ui.screens.main.components.AI

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

// Define star parameters (you can make these configurable)
private const val STAR_VERTICES = 17
private val STAR_ROUNDING = CornerRounding(0.95f)
// You might need innerRadiusRatio or other parameters for androidx.graphics.shapes.star
// Example: Assuming a default or requiring it as a parameter
private const val STAR_INNER_RADIUS_RATIO = 0.4f // Example value, adjust as needed

// A shape specifically for the AI Visualizer Star
object AiStarShape : Shape {
  // Cache the polygon to avoid recreating it constantly
  private val starPolygon =
      RoundedPolygon.star(
          numVerticesPerRadius = STAR_VERTICES,
          innerRadius = STAR_INNER_RADIUS_RATIO, // Adjust if needed
          rounding = STAR_ROUNDING)
  private val matrix = Matrix()

  override fun createOutline(
      size: Size,
      layoutDirection: LayoutDirection,
      density: Density
  ): Outline {
    // The RoundedPolygon is defined in a normalized space (e.g., radius 1)
    // We need to scale and center it within the requested Size.
    matrix.reset()
    // Scale to fit the smaller dimension, maintaining aspect ratio
    val scale = minOf(size.width, size.height) / 2f
    matrix.scale(scale, scale)
    // Translate to center
    matrix.translate(size.width / (2f * scale), size.height / (2f * scale))

    // Generate the path and transform it
    val path = starPolygon.toPath().asComposePath() // Use the cached polygon
    path.transform(matrix)

    return Outline.Generic(path)
  }
}
