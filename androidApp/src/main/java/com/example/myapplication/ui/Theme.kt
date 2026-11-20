package com.example.myapplication.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object BwColors {
    // cores do gráfico circular de composição corporal
    val Fat = Color(0xFFFF8A65)
    val Muscle = Color(0xFF3FD0AE)
    val Bone = Color(0xFFB9C2F5)
    val Other = Color(0xFFB0BEC5)
}

// Tema claro: azul-água + menta + coral
private val LightColors = lightColorScheme(
    primary = Color(0xFF0C8DC6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EEF9),
    onPrimaryContainer = Color(0xFF06344B),
    secondary = Color(0xFF3FD0AE),
    onSecondary = Color(0xFF101828),
    secondaryContainer = Color(0xFFD8F5EE),
    onSecondaryContainer = Color(0xFF101828),
    tertiary = Color(0xFFFF8A65),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF101828),
    surface = Color.White,
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFE4E7F0),
    onSurfaceVariant = Color(0xFF667085)
)

// Tema escuro: meia-noite
private val DarkColors = darkColorScheme(
    primary = Color(0xFF3DB9EA),
    onPrimary = Color(0xFF0F1420),
    primaryContainer = Color(0xFF26304A),
    onPrimaryContainer = Color(0xFFE8ECF5),
    secondary = Color(0xFF6EE7B7),
    onSecondary = Color(0xFF0F1420),
    secondaryContainer = Color(0xFF26304A),
    onSecondaryContainer = Color(0xFFE8ECF5),
    tertiary = Color(0xFFFFB4A2),
    background = Color(0xFF0F1420),
    onBackground = Color(0xFFE8ECF5),
    surface = Color(0xFF181F2E),
    onSurface = Color(0xFFE8ECF5),
    surfaceVariant = Color(0xFF26304A),
    onSurfaceVariant = Color(0xFF9AA6BF)
)

@Composable
fun MyScaleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
