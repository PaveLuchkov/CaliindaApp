package com.lpavs.caliinda.core.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.lpavs.caliinda.core.ui.theme.cold.ColdDarkScheme
import com.lpavs.caliinda.core.ui.theme.cold.ColdLightScheme
import com.lpavs.caliinda.core.ui.theme.green.GreenDarkScheme
import com.lpavs.caliinda.core.ui.theme.green.GreenLightScheme
import com.lpavs.caliinda.core.ui.theme.pinky.PinkyDarkScheme
import com.lpavs.caliinda.core.ui.theme.pinky.PinkyLightScheme
import com.lpavs.caliinda.core.ui.theme.sunny.SunnyDarkScheme
import com.lpavs.caliinda.core.ui.theme.sunny.SunnyLightScheme
import com.lpavs.caliinda.core.ui.theme.warm.WarmDarkScheme
import com.lpavs.caliinda.core.ui.theme.warm.WarmLightScheme
import com.lpavs.caliinda.core.data.model.ThemeMode

/**
 * Цветовая схема для режима темы. SYSTEM — динамические цвета от обоев (minSdk 32, доступны
 * всегда), остальные — собственные M3-схемы.
 */
fun colorSchemeFor(
    themeMode: ThemeMode,
    darkTheme: Boolean,
    context: Context,
    /** Насыщенный вариант (Tonal Spot сочнее) — от того же базового цвета, что и тема. */
    vibrant: Boolean = false,
): ColorScheme {
  val muted = mutedColorSchemeFor(themeMode, darkTheme, context)
  if (!vibrant) return muted
  // База — главный цвет той же темы (светлой: в тёмной он уже осветлён): схема поднимает
  // насыщенность, а оттенок остаётся. Цвет обоев напрямую сдвигал оттенок — система выбирает
  // его из обоев по-своему.
  val seed = mutedColorSchemeFor(themeMode, darkTheme = false, context).primary
  return saturatedColorScheme(seed, darkTheme)
}

private fun mutedColorSchemeFor(themeMode: ThemeMode, darkTheme: Boolean, context: Context): ColorScheme =
    when (themeMode) {
      ThemeMode.SYSTEM ->
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      ThemeMode.COLD -> if (darkTheme) ColdDarkScheme else ColdLightScheme
      ThemeMode.WARM -> if (darkTheme) WarmDarkScheme else WarmLightScheme
      ThemeMode.PINKY -> if (darkTheme) PinkyDarkScheme else PinkyLightScheme
      ThemeMode.GREEN -> if (darkTheme) GreenDarkScheme else GreenLightScheme
      ThemeMode.SUNNY -> if (darkTheme) SunnyDarkScheme else SunnyLightScheme
    }

/**
 * Движение приложения. Та же схема отдаётся в MaterialExpressiveTheme, а здесь доступна и вне
 * composable — в transitionSpec у AnimatedContent и переходах навигации.
 * Spatial — позиция и размер (пружина с отскоком), effects — цвет и прозрачность (без отскока).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val AppMotion: MotionScheme = MotionScheme.expressive()

/** Тема приложения: M3 Expressive — пружинная motion scheme и expressive-формы по умолчанию. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaliindaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    vibrant: Boolean = false,
    content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val colorScheme =
      remember(themeMode, darkTheme, vibrant, context) {
        colorSchemeFor(themeMode, darkTheme, context, vibrant)
      }

  MaterialExpressiveTheme(
      colorScheme = colorScheme,
      motionScheme = AppMotion,
      typography = Typography,
      content = content)
}

/** Схема произвольной темы — для превью выбора темы в настройках. */
@Composable
fun themeColorScheme(
    themeMode: ThemeMode,
    darkTheme: Boolean = isSystemInDarkTheme(),
    vibrant: Boolean = false,
): ColorScheme = colorSchemeFor(themeMode, darkTheme, LocalContext.current, vibrant)
