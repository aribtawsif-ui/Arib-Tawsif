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

private val LightColorScheme = lightColorScheme(
    primary = PoreyForest,
    onPrimary = PoreyCreamLight,
    primaryContainer = PoreySageSoft,
    onPrimaryContainer = PoreyForestDeep,
    secondary = PoreyCoral,
    onSecondary = Color.White,
    secondaryContainer = PoreyCoralSoft,
    onSecondaryContainer = PoreyCoral,
    tertiary = PoreySageDark,
    onTertiary = Color.White,
    tertiaryContainer = PoreySand,
    onTertiaryContainer = PoreyForest,
    background = PoreyCream,
    onBackground = PoreyForestDeep,
    surface = PoreyCreamLight,
    onSurface = PoreyForestDeep,
    surfaceVariant = PoreySand,
    onSurfaceVariant = PoreySlate,
    outline = PoreyOutline,
    outlineVariant = PoreySand
)

private val DarkColorScheme = darkColorScheme(
    primary = PoreyDarkSage,
    onPrimary = PoreyForestDeep,
    primaryContainer = PoreyForest,
    onPrimaryContainer = PoreyDarkCream,
    secondary = PoreyDarkCoral,
    onSecondary = PoreyForestDeep,
    secondaryContainer = Color(0xFF4B211A),
    onSecondaryContainer = PoreyCoralSoft,
    tertiary = PoreySage,
    onTertiary = PoreyForestDeep,
    background = PoreyDarkBg,
    onBackground = PoreyDarkCream,
    surface = PoreyDarkSurface,
    onSurface = PoreyDarkCream,
    surfaceVariant = PoreyDarkSurfaceElevated,
    onSurfaceVariant = Color(0xFFB3BEB6),
    outline = Color(0xFF3E4A42)
)

val PoreyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PoreyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PoreyTypography,
        shapes = PoreyShapes,
        content = content
    )
}
