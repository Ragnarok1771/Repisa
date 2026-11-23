package com.repisa.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colors = darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFF2B1A08),
    secondary = Color(0xFFE9C46A),
    secondaryContainer = Color(0xFF5B3A1E),
    onSecondaryContainer = Color(0xFFFFE7C2),
    background = Color(0xFF1E1612),
    surface = Color(0xFF2A1E17),
    surfaceVariant = Color(0xFF3A2A20),
    onSurface = Color(0xFFF3E6D3),
    onSurfaceVariant = Color(0xFFD9C8AE),
    outline = Color(0xFF8A7358),
)

@Composable
fun RepisaTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = colors, content = content)
