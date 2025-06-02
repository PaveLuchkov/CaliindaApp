package com.lpavs.caliinda.ui.screens.main.components.UIDefaults

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Константы для UI и логики ---
object CalendarUiDefaults {
  // Размеры и отступы
  val ItemHorizontalPadding = 16.dp
  val ItemVerticalPadding = 3.dp // Базовый вертикальный отступ между элементами
  val MicroItemContentVerticalPadding = 1.dp
  val StandardItemContentVerticalPadding = 3.dp
  val AllDayItemPadding = 16.dp
  val AllDayItemVerticalContentPadding = 6.dp
  val HeaderBottomSpacing = 8.dp
  val EventItemCornerRadius = 20.dp
  val MinEventHeight = 65.dp
  val MaxEventHeight = 200.dp
  val MicroEventHeight = 30.dp
  const val MicroEventMaxDurationMinutes = 25L
  val MinStarContainerSize = 120.dp // Размер контейнера для декоративной звезды
  val MaxStarContainerSize = 360.dp // Размер контейнера для декоративной звезды

  // Для настроек
  val SettingsItemCornerRadius = 25.dp

  // Шрифты
  val HeaderFontSize = 16.sp

  // Тени и Z-index
  val CurrentEventElevation = 8.dp

  const val HeightSigmoidMidpointMinutes = 180.0
  const val HeightSigmoidScaleFactor = 30.0
  const val HeightSigmoidSteepness = 7.0
  const val EVENT_TRANSITION_WINDOW_MINUTES = 60L

  const val ShapeMinVertices = 3
  const val ShapeMaxVerticesDelta = 5 // Vertices = Min + (hash % Delta) -> Range [3, 7] now
  const val ShapeShadowOffsetXMaxModulo = 11 // Range [0, 10]
  const val ShapeShadowOffsetYMin = 3
  const val ShapeShadowOffsetYMaxModulo = 6 // Range [3, 8]
  const val ShapeOffsetParamMultiplier = 0.1f
  const val ShapeRadiusSeedMin = 0.4f
  const val ShapeRadiusSeedRange = 0.4f // Range [0.4f, 0.8f]
  const val ShapeRadiusSeedRangeModulo = 11
  const val ShapeMaxRadius = 3f // Coerce limit
  const val ShapeRotationMaxDegrees = 91 // Range [0, 90]
  const val ShapeRotationOffsetDegrees = -45f // Center range around 0
  const val ShapeCornerRounding = 0.95f
  const val ShapeMainAlpha = 0.95f // Alpha for the main star shape
  const val SHAPEINNERRADIUS = 0.3f // Alpha for the main star shape

  // CreationScreen
  val ContainerPadding = 10.dp
  val ContainerCornerRadius = 25.dp
}

val cuid = CalendarUiDefaults
