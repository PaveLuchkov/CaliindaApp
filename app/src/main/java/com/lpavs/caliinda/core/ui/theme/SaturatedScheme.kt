package com.lpavs.caliinda.core.ui.theme

import android.annotation.SuppressLint
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.DynamicColor
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.TonalPalette
import com.google.android.material.color.utilities.Variant

/**
 * Насыщенный вариант схемы — тот же стиль Tonal Spot, что у тем и системных цветов, только
 * насыщенность палитр выше в [CHROMA_BOOST] раз: оттенки и логика тонов прежние, просто сочнее.
 * Vibrant пробовали — акценты на пределе насыщенности и сдвинутые оттенки второстепенных цветов
 * выглядели резко. Строим тем же алгоритмом Material (утилиты цвета из библиотеки material —
 * новой зависимости не нужно; API помечено restricted, но стабильно и используется самой библиотекой).
 */
@SuppressLint("RestrictedApi")
fun saturatedColorScheme(seed: Color, dark: Boolean): ColorScheme {
  val hct = Hct.fromInt(seed.toArgb())
  // Хрома стандартного Tonal Spot: 36 / 16 / 24 (третичный — со сдвигом оттенка на 60°),
  // нейтральные 6 / 8 не трогаем — фон и поверхности остаются спокойными.
  val scheme =
      DynamicScheme(
          hct,
          Variant.TONAL_SPOT,
          dark,
          0.0,
          TonalPalette.fromHueAndChroma(hct.hue, 36.0 * CHROMA_BOOST),
          TonalPalette.fromHueAndChroma(hct.hue, 16.0 * CHROMA_BOOST),
          TonalPalette.fromHueAndChroma((hct.hue + 60.0) % 360.0, 24.0 * CHROMA_BOOST),
          TonalPalette.fromHueAndChroma(hct.hue, 6.0),
          TonalPalette.fromHueAndChroma(hct.hue, 8.0))
  val roles = MaterialDynamicColors()
  fun DynamicColor.color() = Color(getArgb(scheme))
  val base = if (dark) darkColorScheme() else lightColorScheme()
  return base.copy(
      primary = roles.primary().color(),
      onPrimary = roles.onPrimary().color(),
      primaryContainer = roles.primaryContainer().color(),
      onPrimaryContainer = roles.onPrimaryContainer().color(),
      inversePrimary = roles.inversePrimary().color(),
      secondary = roles.secondary().color(),
      onSecondary = roles.onSecondary().color(),
      secondaryContainer = roles.secondaryContainer().color(),
      onSecondaryContainer = roles.onSecondaryContainer().color(),
      tertiary = roles.tertiary().color(),
      onTertiary = roles.onTertiary().color(),
      tertiaryContainer = roles.tertiaryContainer().color(),
      onTertiaryContainer = roles.onTertiaryContainer().color(),
      background = roles.background().color(),
      onBackground = roles.onBackground().color(),
      surface = roles.surface().color(),
      onSurface = roles.onSurface().color(),
      surfaceVariant = roles.surfaceVariant().color(),
      onSurfaceVariant = roles.onSurfaceVariant().color(),
      surfaceTint = roles.surfaceTint().color(),
      inverseSurface = roles.inverseSurface().color(),
      inverseOnSurface = roles.inverseOnSurface().color(),
      error = roles.error().color(),
      onError = roles.onError().color(),
      errorContainer = roles.errorContainer().color(),
      onErrorContainer = roles.onErrorContainer().color(),
      outline = roles.outline().color(),
      outlineVariant = roles.outlineVariant().color(),
      scrim = roles.scrim().color(),
      surfaceBright = roles.surfaceBright().color(),
      surfaceDim = roles.surfaceDim().color(),
      surfaceContainer = roles.surfaceContainer().color(),
      surfaceContainerHigh = roles.surfaceContainerHigh().color(),
      surfaceContainerHighest = roles.surfaceContainerHighest().color(),
      surfaceContainerLow = roles.surfaceContainerLow().color(),
      surfaceContainerLowest = roles.surfaceContainerLowest().color(),
  )
}

/** Во сколько раз насыщеннее стандартного Tonal Spot; подобрано на странице сравнения стилей. */
private const val CHROMA_BOOST = 1.5
