package org.ardour.android.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal object ArdourPalette {
    val Background = Color(0xFF0D0E10)
    val Surface = Color(0xFF17181B)
    val SurfaceRaised = Color(0xFF202226)
    val SurfaceSelected = Color(0xFF2B2E33)
    val Outline = Color(0xFF34373D)
    val Primary = Color(0xFF8AB4FF)
    val OnPrimary = Color(0xFF07111F)
    val TextPrimary = Color(0xFFF3F4F6)
    val TextSecondary = Color(0xFFB8BBC2)
    val Record = Color(0xFFFF6B6B)
    val Region = Color(0xFF6F91C8)
    val RegionSecondary = Color(0xFF7B8A72)
}

private val ArdourDarkColors = darkColorScheme(
    primary = ArdourPalette.Primary,
    onPrimary = ArdourPalette.OnPrimary,
    background = ArdourPalette.Background,
    onBackground = ArdourPalette.TextPrimary,
    surface = ArdourPalette.Surface,
    onSurface = ArdourPalette.TextPrimary,
    surfaceVariant = ArdourPalette.SurfaceRaised,
    onSurfaceVariant = ArdourPalette.TextSecondary,
    outline = ArdourPalette.Outline,
    error = ArdourPalette.Record,
)

private val ArdourShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ArdourTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ArdourDarkColors,
        shapes = ArdourShapes,
        content = content,
    )
}
