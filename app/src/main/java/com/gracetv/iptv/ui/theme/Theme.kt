package com.gracetv.iptv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gracetv.iptv.domain.model.ThemeMode

private val GraceDarkScheme = darkColorScheme(
    primary = GraceRed,
    onPrimary = Color.White,
    primaryContainer = GraceRedContainer,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = GraceYellow,
    onSecondary = Color(0xFF1A1400),
    secondaryContainer = GraceYellowContainer,
    onSecondaryContainer = Color(0xFFFFE082),
    tertiary = GraceGreen,
    onTertiary = Color(0xFF00210B),
    tertiaryContainer = GraceGreenContainer,
    onTertiaryContainer = Color(0xFFA7F5BA),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Color(0xFF0A0C0E),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceElevated,
    outline = DarkOutline,
    outlineVariant = DarkOutlineSubtle,
    error = Color(0xFFFF5252),
    onError = Color.White
)

private val GraceAmoledScheme = GraceDarkScheme.copy(
    background = AmoledBackground,
    surface = Color(0xFF090A0C),
    surfaceVariant = Color(0xFF14171D),
    surfaceContainerLow = Color(0xFF090A0C),
    surfaceContainer = Color(0xFF14171D),
    surfaceContainerHigh = Color(0xFF1C2028)
)

private val GraceHighContrastScheme = GraceDarkScheme.copy(
    background = Color(0xFF050607),
    surface = Color(0xFF101216),
    onBackground = Color.White,
    onSurface = Color.White,
    outline = Color(0xFF6B7280)
)

val GraceShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GraceTvTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.DARK -> GraceDarkScheme
        ThemeMode.AMOLED -> GraceAmoledScheme
        ThemeMode.HIGH_CONTRAST -> GraceHighContrastScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GraceTvTypography,
        shapes = GraceShapes,
        content = content
    )
}
