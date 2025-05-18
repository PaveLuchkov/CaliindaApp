package com.lpavs.caliinda.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

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

val LocalFixedAccentColors = compositionLocalOf<FixedAccentColors> {
    error("No FixedAccentColors provided")
}

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
            tertiaryFixedDim = colorSchemeDark.tertiary
        )
    }
}

// -------------------- Calendar Theme --------------------

@Composable
fun CaliindaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val lightColors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        else -> lightColorScheme()
    }

    val darkColors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicDarkColorScheme(context)
        else -> darkColorScheme()
    }

    val colorScheme = if (darkTheme) darkColors else lightColors

    val fixedAccentColors = rememberFixedAccentColors(lightColors, darkColors)

    CompositionLocalProvider(LocalFixedAccentColors provides fixedAccentColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}