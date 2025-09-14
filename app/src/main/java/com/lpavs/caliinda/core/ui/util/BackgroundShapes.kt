package com.lpavs.caliinda.core.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlin.math.max

enum class BackgroundShapeContext {
  Main,
  EventCreation
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackgroundShapes(context: BackgroundShapeContext = BackgroundShapeContext.Main) {
  Box(
      modifier = Modifier.fillMaxSize()
  ) {
        when (context) {
          BackgroundShapeContext.Main -> {

            val clover4Leaf = MaterialShapes.Clover4Leaf.toShape()
            val cookie4Sided = MaterialShapes.Flower.toShape()
            val starContainerSize = 300.dp
            val star2ContainerSize = 200.dp

              Box(
                modifier =
                    Modifier.size(starContainerSize)
                        .align(Alignment.TopEnd)
                        .offset(x = starContainerSize * 0.2f, y = -starContainerSize * 0.1f)
                        .rotate(30f)
                        .clip(clover4Leaf)
                        .background(color = Color.Transparent)
                        .border(
                            width = 2.dp, color = colorScheme.surfaceVariant, shape = clover4Leaf))

              Box(
                modifier =
                    Modifier.size(star2ContainerSize)
                        .align(Alignment.TopStart)
                        .offset(
                            x = -star2ContainerSize * 0.4f,
                            y = star2ContainerSize * 1.5f
                        )
                        .rotate(80f)
                        .clip(cookie4Sided)
                        .background(colorScheme.surfaceVariant))
          }

          BackgroundShapeContext.EventCreation -> {
            val starShape = remember { RoundedPolygon.star(3, rounding = CornerRounding(0.2f)) }
            val clipStar = remember(starShape) { RoundedPolygonShape(polygon = starShape) }
            val star2Shape = remember {
              RoundedPolygon.star(5, rounding = CornerRounding(0.4f), radius = 2f)
            }
            val clip2Star = remember(star2Shape) { RoundedPolygonShape(polygon = star2Shape) }
            val starContainerSize = 200.dp
            val star2ContainerSize = 300.dp

              Box(
                modifier =
                    Modifier.size(starContainerSize)
                        .align(Alignment.TopEnd)
                        .offset(x = starContainerSize * 0.2f, y = -starContainerSize * 0.1f)
                        .graphicsLayer {
                          shape = clipStar
                          alpha = 0.99f
                        }
                        .clip(clipStar)
                        .background(colorScheme.surfaceVariant))

              Box(
                modifier =
                    Modifier.size(star2ContainerSize)
                        .align(Alignment.TopStart)
                        .offset(
                            x = -star2ContainerSize * 0.4f,
                            y = star2ContainerSize * 2f
                        )
                        .graphicsLayer { shape = clip2Star }
                        .clip(clip2Star)
                        .background(colorScheme.surfaceVariant))
          }
        }
      }
}

fun RoundedPolygon.getBounds() = calculateBounds().let { Rect(it[0], it[1], it[2], it[3]) }

class RoundedPolygonShape(
    private val polygon: RoundedPolygon,
    private var matrix: Matrix = Matrix()
) : Shape {
  private var path = Path()

  override fun createOutline(
      size: Size,
      layoutDirection: LayoutDirection,
      density: Density
  ): Outline {
    path.rewind()
    path = polygon.toPath().asComposePath()
    matrix.reset()
    val bounds = polygon.getBounds()
    val maxDimension = max(bounds.width, bounds.height)
    matrix.scale(size.width / maxDimension, size.height / maxDimension)
    matrix.translate(-bounds.left, -bounds.top)

    path.transform(matrix)
    return Outline.Generic(path)
  }
}
