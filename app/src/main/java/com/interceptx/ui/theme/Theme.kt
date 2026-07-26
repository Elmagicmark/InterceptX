package com.interceptx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val InterceptXDarkScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = VoidBlack,
    primaryContainer = SurfaceSlateElevated,
    onPrimaryContainer = ElectricCyan,

    secondary = NeonGreen,
    onSecondary = VoidBlack,
    secondaryContainer = SurfaceSlateElevated,
    onSecondaryContainer = NeonGreen,

    tertiary = AmberAlert,
    onTertiary = VoidBlack,

    error = WarningCrimson,
    onError = Color.White,
    errorContainer = WarningCrimsonDim,
    onErrorContainer = Color.White,

    background = VoidBlack,
    onBackground = TextPrimary,

    surface = DarkSlate,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSlate,
    onSurfaceVariant = TextSecondary,

    outline = StrokeSlate,
    outlineVariant = StrokeSlate
)

val InterceptXShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun InterceptXTheme(
    // The app is intentionally always-dark (cyber-security aesthetic);
    // the parameter is kept for API compatibility if a light mode is added later.
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = InterceptXDarkScheme,
        typography = InterceptXTypography,
        shapes = InterceptXShapes,
        content = content
    )
}
