package com.example.habitpower.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    onPrimary = TextPrimary,
    secondary = PrimaryAccentDark,
    onSecondary = TextPrimary,
    tertiary = SuccessAccent,
    onTertiary = TextPrimary,
    background = DarkGray,
    surface = LightGray,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SegmentBorder,
    onSurfaceVariant = TextSecondary,
    error = ErrorColor,
    primaryContainer = PrimaryAccentDark,
    onPrimaryContainer = TextPrimary,
    secondaryContainer = SegmentBorder,
    onSecondaryContainer = TextPrimary,
    tertiaryContainer = Color(0xFF1F3A30),
    onTertiaryContainer = Color(0xFFD4F3E6),
    outline = Color(0xFF54606D)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryAccent,
    onPrimary = Color.White,
    secondary = PrimaryAccentDark,
    onSecondary = Color.White,
    tertiary = SuccessAccent,
    onTertiary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    error = Color(0xFFB3261E),
    primaryContainer = Color(0xFFD4E9F8),
    onPrimaryContainer = Color(0xFF0A2D49),
    secondaryContainer = Color(0xFFDCEAF7),
    onSecondaryContainer = Color(0xFF12324E),
    tertiaryContainer = Color(0xFFDAF2E7),
    onTertiaryContainer = Color(0xFF113728),
    outline = Color(0xFF738190)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HabitPowerTheme(

    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
