package com.goldmeltcheck.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class VerdictColors(val good: Color, val warn: Color, val bad: Color, val goldSoft: Color)

val LocalVerdictColors = staticCompositionLocalOf {
    VerdictColors(Color(0xFF2F7A45), Color(0xFFA8650B), Color(0xFFB0322A), Color(0xFFF3E7C4))
}

private val Light = lightColorScheme(
    primary = Color(0xFFA87A12),
    onPrimary = Color.White,
    background = Color(0xFFF6F4EE),
    onBackground = Color(0xFF1D1A14),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1A14),
    surfaceVariant = Color(0xFFF6F4EE),
    onSurfaceVariant = Color(0xFF6D6656),
    outline = Color(0xFFE3DDCD),
    outlineVariant = Color(0xFFE3DDCD),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFE2B543),
    onPrimary = Color(0xFF241B05),
    background = Color(0xFF15130F),
    onBackground = Color(0xFFF1EBDC),
    surface = Color(0xFF1F1C16),
    onSurface = Color(0xFFF1EBDC),
    surfaceVariant = Color(0xFF15130F),
    onSurfaceVariant = Color(0xFFA59D89),
    outline = Color(0xFF36312A),
    outlineVariant = Color(0xFF36312A),
)

@Composable
fun GoldTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val verdict = if (dark) {
        VerdictColors(Color(0xFF6CC489), Color(0xFFE6A64A), Color(0xFFEF7B70), Color(0xFF3A3020))
    } else {
        VerdictColors(Color(0xFF2F7A45), Color(0xFFA8650B), Color(0xFFB0322A), Color(0xFFF3E7C4))
    }
    CompositionLocalProvider(LocalVerdictColors provides verdict) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
    }
}
