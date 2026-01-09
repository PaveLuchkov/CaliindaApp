package com.lpavs.caliinda.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.lpavs.caliinda.feature.settings.vm.ThemeMode
import com.lpavs.caliinda.core.ui.theme.cold.ColdLightScheme
import com.lpavs.caliinda.core.ui.theme.cold.ColdDarkScheme
import com.lpavs.caliinda.core.ui.theme.green.GreenDarkScheme
import com.lpavs.caliinda.core.ui.theme.green.GreenLightScheme
import com.lpavs.caliinda.core.ui.theme.pinky.PinkyDarkScheme
import com.lpavs.caliinda.core.ui.theme.pinky.PinkyLightScheme
import com.lpavs.caliinda.core.ui.theme.sunny.SunnyDarkScheme
import com.lpavs.caliinda.core.ui.theme.sunny.SunnyLightScheme
import com.lpavs.caliinda.core.ui.theme.warm.WarmDarkScheme
import com.lpavs.caliinda.core.ui.theme.warm.WarmLightScheme

data class FixedAccentColors(
    val primaryFixed: Color,
    val onPrimaryFixed: Color,
    val secondaryFixed: Color,
    val onSecondaryFixed: Color,
    val tertiaryFixed: Color,
    val onTertiaryFixed: Color,
    val primaryFixedDim: Color,
    val secondaryFixedDim: Color,
    val tertiaryFixedDim: Color,
)

val LocalFixedAccentColors =
    compositionLocalOf<FixedAccentColors> { error("No FixedAccentColors provided") }

@Composable
fun rememberFixedAccentColors(
    colorSchemeLight: ColorScheme,
    colorSchemeDark: ColorScheme
): FixedAccentColors {
  return remember(colorSchemeLight, colorSchemeDark) {
    FixedAccentColors(
        primaryFixed = colorSchemeLight.primaryContainer,
        onPrimaryFixed = colorSchemeLight.onPrimaryContainer,
        secondaryFixed = colorSchemeLight.secondaryContainer,
        onSecondaryFixed = colorSchemeLight.onSecondaryContainer,
        tertiaryFixed = colorSchemeLight.tertiaryContainer,
        onTertiaryFixed = colorSchemeLight.onTertiaryContainer,
        primaryFixedDim = colorSchemeDark.primary,
        secondaryFixedDim = colorSchemeDark.secondary,
        tertiaryFixedDim = colorSchemeDark.tertiary)
  }
}

// -------------------- Calendar Theme --------------------

@Composable
fun CaliindaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
  val context = LocalContext.current

  val lightColors =
      when {
        dynamicColor && true ->
            dynamicLightColorScheme(context)
        else -> lightColorScheme()
      }

  val darkColors =
      when {
        dynamicColor && true ->
            dynamicDarkColorScheme(context)
        else -> darkColorScheme()
      }

    val colorScheme: ColorScheme = when (themeMode) {
        ThemeMode.SYSTEM -> {
            // Логика для системной темы (Dynamic Colors или стандартные)
            val dynamicColorsAvailable = true
            if (darkTheme) {
                if (dynamicColorsAvailable) dynamicDarkColorScheme(context) else darkColorScheme() // стандартный dark fallback
            } else {
                if (dynamicColorsAvailable) dynamicLightColorScheme(context) else lightColorScheme() // стандартный light fallback
            }
        }
        ThemeMode.COLD -> if (darkTheme) ColdDarkScheme else ColdLightScheme
        ThemeMode.WARM -> if (darkTheme) WarmDarkScheme else WarmLightScheme
        ThemeMode.PINKY -> if (darkTheme) PinkyDarkScheme else PinkyLightScheme // пример
        ThemeMode.GREEN -> if (darkTheme) GreenDarkScheme else GreenLightScheme // пример
        ThemeMode.SUNNY -> if (darkTheme) SunnyDarkScheme else SunnyLightScheme // пример
    }

  val fixedAccentColors = rememberFixedAccentColors(lightColors, darkColors)

  CompositionLocalProvider(LocalFixedAccentColors provides fixedAccentColors) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}

@Composable
fun getThemePrimaryColor(themeMode: ThemeMode, darkTheme: Boolean = isSystemInDarkTheme()): Color {
    val context = LocalContext.current

    return when (themeMode) {
        ThemeMode.SYSTEM -> {
            if (darkTheme) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
        }
        ThemeMode.COLD -> if (darkTheme) ColdDarkScheme.primary else ColdLightScheme.primary
        ThemeMode.WARM -> if (darkTheme) WarmDarkScheme.primary else WarmLightScheme.primary
        ThemeMode.PINKY -> if (darkTheme) PinkyDarkScheme.primary else PinkyLightScheme.primary
        ThemeMode.GREEN -> if (darkTheme) GreenDarkScheme.primary else GreenLightScheme.primary
        ThemeMode.SUNNY -> if (darkTheme) SunnyDarkScheme.primary else SunnyLightScheme.primary
    }
}