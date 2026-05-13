package com.flowrite.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FloWritePrimary,
    secondary = FloWriteSecondary,
    tertiary = FloWriteAccent,
    background = FloWriteBackground,
    surface = FloWriteSurface,
    surfaceVariant = FloWriteSurfaceVariant,
    onPrimary = FloWriteBackground,
    onSecondary = FloWriteBackground,
    onTertiary = FloWriteOnSurface,
    onBackground = FloWriteOnSurface,
    onSurface = FloWriteOnSurface,
    onSurfaceVariant = FloWriteOnSurfaceVariant,
    error = FloWriteError,
    onError = FloWriteOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = FloWritePrimaryLight,
    secondary = FloWriteSecondary,
    tertiary = FloWriteAccent,
    background = FloWriteBackgroundLight,
    surface = FloWriteSurfaceLight,
    surfaceVariant = FloWriteSurfaceVariantLight,
    onPrimary = FloWriteSurfaceLight,
    onSecondary = FloWriteBackgroundLight,
    onTertiary = FloWriteSurfaceLight,
    onBackground = FloWriteOnSurfaceLight,
    onSurface = FloWriteOnSurfaceLight,
    onSurfaceVariant = FloWriteOnSurfaceVariantLight,
    error = FloWriteError,
    onError = FloWriteSurfaceLight
)

@Composable
fun FloWriteTheme(
    darkTheme: Boolean = true, // Professional Pitch Black theme enabled by default
    dynamicColor: Boolean = false,
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
        typography = FloWriteTypography,
        content = content
    )
}
