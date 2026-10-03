package com.tnhc.community.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF205C45), onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EBDD), onPrimaryContainer = Color(0xFF143C2C),
    secondary = Color(0xFF566458), secondaryContainer = Color(0xFFE2EADF),
    background = Color(0xFFFAF9F6), onBackground = Color(0xFF1D2520),
    surface = Color(0xFFFAF9F6), onSurface = Color(0xFF1D2520),
    surfaceVariant = Color(0xFFE8EBE4), onSurfaceVariant = Color(0xFF4B554D),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFA1D3B4), onPrimary = Color(0xFF083821),
    primaryContainer = Color(0xFF284F39), onPrimaryContainer = Color(0xFFD7EBDD),
    secondary = Color(0xFFB6C8B6), secondaryContainer = Color(0xFF344B3B),
    background = Color(0xFF111713), onBackground = Color(0xFFE1E9DF),
    surface = Color(0xFF111713), onSurface = Color(0xFFE1E9DF),
    surfaceVariant = Color(0xFF354039), onSurfaceVariant = Color(0xFFC0CBC1),
)

@Composable
fun TnhcTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
