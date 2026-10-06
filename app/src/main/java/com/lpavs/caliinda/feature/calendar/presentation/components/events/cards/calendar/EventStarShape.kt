package com.lpavs.caliinda.feature.calendar.presentation.components.events.cards.calendar

import androidx.graphics.shapes.star
import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import com.lpavs.caliinda.core.ui.theme.cuid
import com.lpavs.caliinda.core.ui.util.RoundedPolygonShape
import com.lpavs.caliinda.feature.calendar.presentation.model.GeneratedShapeParams

/** Звезда события — одна функция для карточки в ленте и для подробностей. */
fun eventStarShape(params: GeneratedShapeParams): Shape =
    RoundedPolygonShape(
        polygon =
            RoundedPolygon.star(
                numVerticesPerRadius = params.numVertices,
                radius = params.radiusSeed,
                innerRadius = cuid.SHAPEINNERRADIUS,
                rounding = CornerRounding(cuid.ShapeCornerRounding)))
