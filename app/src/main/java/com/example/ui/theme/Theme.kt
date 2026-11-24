package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemePreference

private val SoniqDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = CrimsonMuted,
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = CrimsonGlow,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF272732),
    onSecondaryContainer = TextPrimaryDark,
    tertiary = LiveEmerald,
    onTertiary = Color.Black,
    background = ObsidianBlack,
    onBackground = TextPrimaryDark,
    surface = CarbonSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CarbonSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = CarbonBorder,
    outlineVariant = Color(0xFF1F1F28),
    error = Color(0xFFFB7185),
    onError = Color.Black
)

private val SoniqLightColorScheme = lightColorScheme(
    primary = CrimsonLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4E8),
    onPrimaryContainer = Color(0xFF690012),
    secondary = Color(0xFF2B2B36),
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    outlineVariant = Color(0xFFE5E5EE),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val SoniqShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun SoniqProTheme(
    themePreference: ThemePreference = ThemePreference.DARK_OLED,
    content: @Composable () -> Unit
) {
    val isDark = when (themePreference) {
        ThemePreference.DARK_OLED -> true
        ThemePreference.LIGHT_CRIMSON -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) SoniqDarkColorScheme else SoniqLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SoniqTypography,
        shapes = SoniqShapes,
        content = content
    )
}
