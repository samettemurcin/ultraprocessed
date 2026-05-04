package com.b2.ultraprocessed.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    secondary = Emerald400,
    tertiary = Emerald600,
    background = DarkBg,
    surface = DarkBg,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF111318),
    onSurfaceVariant = Color(0xFFBDC5CB),
    outline = Color(0xFF1F2937),
    error = Red400,
    onError = Color.Black,
)

@Composable
fun UltraProcessedTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
