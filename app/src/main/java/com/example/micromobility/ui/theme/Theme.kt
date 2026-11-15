package com.example.micromobility.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class SemanticPalette(
    val emerald: Color,
    val emeraldContainer: Color,
    val edgeCyan: Color,
    val edgeCyanContainer: Color,
    val pauseAmber: Color,
    val pauseAmberContainer: Color,
    val violationCoral: Color,
    val violationCoralContainer: Color,
    val mapBackground: Color,
    val mapRoadPrimary: Color,
    val mapRoadSecondary: Color,
    val mapPark: Color,
    val mapRiver: Color
)

private val LightPalette = SemanticPalette(
    emerald = Emerald,
    emeraldContainer = EmeraldContainerLight,
    edgeCyan = EdgeCyan,
    edgeCyanContainer = EdgeCyanContainerLight,
    pauseAmber = PauseAmber,
    pauseAmberContainer = PauseAmberContainerLight,
    violationCoral = ViolationCoral,
    violationCoralContainer = ViolationCoralContainerLight,
    mapBackground = Color(0xFFEDF3F5),
    mapRoadPrimary = Color(0xFFFFFFFF),
    mapRoadSecondary = Color(0xFFDCE7EB),
    mapPark = Color(0xFFD5EFE2),
    mapRiver = Color(0xFFBEE3F8)
)

private val DarkPalette = SemanticPalette(
    emerald = EmeraldBright,
    emeraldContainer = EmeraldContainerDark,
    edgeCyan = EdgeCyanBright,
    edgeCyanContainer = EdgeCyanContainerDark,
    pauseAmber = PauseAmberBright,
    pauseAmberContainer = PauseAmberContainerDark,
    violationCoral = ViolationCoralBright,
    violationCoralContainer = ViolationCoralContainerDark,
    mapBackground = Color(0xFF0A141A),
    mapRoadPrimary = Color(0xFF1A2D38),
    mapRoadSecondary = Color(0xFF13222B),
    mapPark = Color(0xFF0E2B24),
    mapRiver = Color(0xFF0C2A3E)
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldBright,
    onPrimary = MidnightSlate,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldContainerLight,
    secondary = EdgeCyanBright,
    onSecondary = MidnightSlate,
    secondaryContainer = EdgeCyanContainerDark,
    onSecondaryContainer = EdgeCyanContainerLight,
    background = SlateSurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SlateSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateCardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateBorderDark,
    error = ViolationCoralBright,
    errorContainer = ViolationCoralContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = MidnightSlate,
    secondary = EdgeCyan,
    onSecondary = Color.White,
    secondaryContainer = EdgeCyanContainerLight,
    onSecondaryContainer = MidnightSlate,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceContainerLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = ViolationCoral,
    errorContainer = ViolationCoralContainerLight
)

private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)
@Composable
fun Theme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val palette = if (darkTheme) DarkPalette else LightPalette

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}