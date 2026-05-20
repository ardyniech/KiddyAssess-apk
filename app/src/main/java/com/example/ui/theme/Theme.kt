package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val M3LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = PureWhite,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = AmberContrast,
    onSecondary = PureWhite,
    tertiary = SuccessGreen,
    onTertiary = PureWhite,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    outline = OutlineBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = M3LightColorScheme,
        typography = Typography,
        content = content
    )
}
