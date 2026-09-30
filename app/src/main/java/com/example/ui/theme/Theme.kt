package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PaperDarkColorScheme = darkColorScheme(
    primary = PaperCyan,
    onPrimary = PaperDarkBg,
    primaryContainer = PaperCyanDim,
    onPrimaryContainer = PaperTextPrimary,
    secondary = PaperGreen,
    onSecondary = PaperDarkBg,
    secondaryContainer = PaperGreenDark,
    onSecondaryContainer = PaperTextPrimary,
    tertiary = PaperGold,
    background = PaperDarkBg,
    onBackground = PaperTextPrimary,
    surface = PaperSurface,
    onSurface = PaperTextPrimary,
    surfaceVariant = PaperSurfaceElevated,
    onSurfaceVariant = PaperTextSecondary,
    outline = PaperSurfaceBorder,
    error = PaperRed,
    onError = PaperDarkBg
)

private val PaperLightColorScheme = lightColorScheme(
    primary = PaperCyanDim,
    onPrimary = PaperTextPrimary,
    secondary = PaperGreenDark,
    background = PaperDarkBg, // Keep deep dark aesthetic even in light mode for server immersion
    surface = PaperSurface,
    onSurface = PaperTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinctive gaming obsidian branding
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PaperDarkColorScheme,
        typography = Typography,
        content = content
    )
}
