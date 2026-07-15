package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TelegramLightBlue,
    onPrimary = DarkTextPrimary,
    primaryContainer = DarkBubbleMe,
    onPrimaryContainer = DarkTextPrimary,
    secondaryContainer = DarkBubbleOther,
    onSecondaryContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkAccent,
    onSurfaceVariant = DarkTextSecondary,
    secondary = TelegramLightBlue,
    onSecondary = DarkTextPrimary,
    tertiary = OnlineGreen,
    onTertiary = DarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = TelegramBlue,
    onPrimary = LightSurface,
    primaryContainer = LightBubbleMe,
    onPrimaryContainer = LightTextPrimary,
    secondaryContainer = LightBubbleOther,
    onSecondaryContainer = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightAccent,
    onSurfaceVariant = LightTextSecondary,
    secondary = TelegramBlue,
    onSecondary = LightSurface,
    tertiary = OnlineGreen,
    onTertiary = LightSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
