package com.chatora.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand Colors ─────────────────────────────────────────────────────────────
val ChatoraPrimary = Color(0xFF6C63FF)
val ChatoraPrimaryDark = Color(0xFF5A52D5)
val ChatoraSecondary = Color(0xFF03DAC5)
val ChatoraBackground = Color(0xFF121212)
val ChatoraSurface = Color(0xFF1E1E2E)
val ChatoraError = Color(0xFFCF6679)
val ChatoraOnPrimary = Color.White
val ChatoraOnSurface = Color(0xFFE1E1E6)
val ChatoraGreen = Color(0xFF4CAF50)
val ChatoraRed = Color(0xFFE53935)

private val DarkColorScheme = darkColorScheme(
    primary = ChatoraPrimary,
    onPrimary = ChatoraOnPrimary,
    secondary = ChatoraSecondary,
    background = ChatoraBackground,
    surface = ChatoraSurface,
    error = ChatoraError,
    onBackground = ChatoraOnSurface,
    onSurface = ChatoraOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = ChatoraPrimary,
    onPrimary = ChatoraOnPrimary,
    secondary = ChatoraSecondary,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    error = Color(0xFFB00020),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun ChatoraTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
