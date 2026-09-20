package com.gabinkenko.tikapub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TikapubPrimary = Color(0xFF7C4DFF)
private val TikapubSecondary = Color(0xFF25F4EE)
private val TikapubBackground = Color(0xFF0F0F14)
private val TikapubSurface = Color(0xFF1A1A22)

private val DarkColors = darkColorScheme(
    primary = TikapubPrimary,
    secondary = TikapubSecondary,
    background = TikapubBackground,
    surface = TikapubSurface,
)

private val LightColors = lightColorScheme(
    primary = TikapubPrimary,
    secondary = TikapubSecondary,
)

@Composable
fun TikapubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
