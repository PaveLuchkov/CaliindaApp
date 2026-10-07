package com.lpavs.caliinda.feature.widget.countdown

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.RoundedPolygon
import com.lpavs.caliinda.core.data.calendar.model.EventDto

// Только симметричные «круглые» фигуры: число в них сидит ровно, а у листовых и вытянутых
// середина узкая — цифры упираются в края.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val countdownShapes by lazy {
  listOf(
      MaterialShapes.Cookie9Sided,
      MaterialShapes.Cookie12Sided,
      MaterialShapes.Sunny,
      MaterialShapes.Flower,
  )
}

/** Ключ события для отсчёта: у серии — мастер-событие, чтобы фигура не менялась от раза к разу. */
internal val EventDto.countdownKey: Long
  get() = originalEventId ?: eventId

/** Фигура события в виджете и в пикере — одна и та же. */
internal fun countdownPolygon(eventKey: Long): RoundedPolygon =
    countdownShapes[Math.floorMod(eventKey, countdownShapes.size.toLong()).toInt()]
