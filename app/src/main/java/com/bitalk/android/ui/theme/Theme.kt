package com.bitalk.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Bitalk brand colors
val BitalkAccent = Color(0xFFE32A6D)
val BitalkAccentDark = Color(0xFFC2185B)
val BitalkAccentLight = Color(0xFFF48FB1)

// Neutral colors
val GrayLight = Color(0xFFF5F5F5)
val GrayMedium = Color(0xFF9E9E9E)
val GrayDark = Color(0xFF424242)

private val DarkColorScheme = darkColorScheme(
    primary = BitalkAccent,
    secondary = BitalkAccent,
    tertiary = BitalkAccentLight,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = BitalkAccent,
    secondary = BitalkAccent,
    tertiary = BitalkAccentLight,
    background = Color.White,
    surface = GrayLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = GrayDark,
    onSurface = GrayDark,
)

@Composable
fun BitalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}