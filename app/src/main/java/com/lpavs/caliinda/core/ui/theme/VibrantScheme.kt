package com.lpavs.caliinda.core.ui.theme

import android.annotation.SuppressLint
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.DynamicColor
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeVibrant

/**
 * Насыщенный вариант схемы: стиль палитры Vibrant вместо стандартного Tonal Spot — системные
 * цвета от обоев иначе не умеют, приглушённость там заложена в сам стиль. Строим своими силами
 * от базового цвета тем же алгоритмом Material (утилиты цвета из библиотеки material — новой
 * зависимости не нужно; API помечено restricted, но стабильно и используется самой библиотекой).
 */
@SuppressLint("RestrictedApi")
fun vibrantColorScheme(seed: Color, dark: Boolean): ColorScheme {
  val scheme = SchemeVibrant(Hct.fromInt(seed.toArgb()), dark, CONTRAST)
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

/**
 * Контраст схемы. У Vibrant при 0 контейнеры того же светлого тона, что и у приглушённой, —
 * насыщенность в них не видна; 0.2 чуть опускает их тон, и цвет проявляется (0.5 — уже тяжело).
 */
private const val CONTRAST = 0.2
