package com.example.caliindar.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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

@Composable
fun CaliindarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


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
val material3LightColors = lightColorScheme()
val material3DarkColors = darkColorScheme()
fun getFixedAccentColors() =
    FixedAccentColors(
        primaryFixed = material3LightColors.primaryContainer,
        onPrimaryFixed = material3LightColors.onPrimaryContainer,
        secondaryFixed = material3LightColors.secondaryContainer,
        onSecondaryFixed = material3LightColors.onSecondaryContainer,
        tertiaryFixed = material3LightColors.tertiaryContainer,
        onTertiaryFixed = material3LightColors.onTertiaryContainer,
        primaryFixedDim = material3DarkColors.primary,
        secondaryFixedDim = material3DarkColors.secondary,
        tertiaryFixedDim = material3DarkColors.tertiary
    )
val LocalFixedAccentColors = compositionLocalOf { getFixedAccentColors() }

@Composable
fun MyMaterialTheme(
    fixedAccentColors: FixedAccentColors = LocalFixedAccentColors.current,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) material3DarkColors else material3LightColors
    ) {
        CompositionLocalProvider(LocalFixedAccentColors provides fixedAccentColors) {
            // Content has access to fixedAccentColors in both light and dark theme.
            content()
        }
    }
}