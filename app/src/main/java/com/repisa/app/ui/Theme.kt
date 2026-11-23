package com.repisa.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.repisa.app.R
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

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variable(res: Int, vararg weights: Int) = weights.map { w ->
    Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
}

/** Títulos: Fredoka, redondeada y con carácter (fuente libre OFL). */
val DisplayFont = FontFamily(variable(R.font.fredoka, 400, 500, 600, 700))

/** Texto: Nunito, muy legible en pantallas pequeñas (fuente libre OFL). */
val BodyFont = FontFamily(variable(R.font.nunito, 400, 500, 600, 700, 800, 900))

private val typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = DisplayFont), displayMedium = displayMedium.copy(fontFamily = DisplayFont),
        displaySmall = displaySmall.copy(fontFamily = DisplayFont), headlineLarge = headlineLarge.copy(fontFamily = DisplayFont),
        headlineMedium = headlineMedium.copy(fontFamily = DisplayFont), headlineSmall = headlineSmall.copy(fontFamily = DisplayFont),
        titleLarge = titleLarge.copy(fontFamily = DisplayFont), titleMedium = titleMedium.copy(fontFamily = BodyFont),
        titleSmall = titleSmall.copy(fontFamily = BodyFont), bodyLarge = bodyLarge.copy(fontFamily = BodyFont),
        bodyMedium = bodyMedium.copy(fontFamily = BodyFont), bodySmall = bodySmall.copy(fontFamily = BodyFont),
        labelLarge = labelLarge.copy(fontFamily = BodyFont), labelMedium = labelMedium.copy(fontFamily = BodyFont),
        labelSmall = labelSmall.copy(fontFamily = BodyFont),
    )
}

@Composable
fun RepisaTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
