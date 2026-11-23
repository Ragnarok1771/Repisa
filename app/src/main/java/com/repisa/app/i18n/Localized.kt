package com.repisa.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Aplica el idioma elegido a todo lo que hay dentro, y lo vuelve a aplicar al cambiarlo,
 * sin reiniciar la actividad (así la segunda pantalla no se apaga ni parpadea).
 */
@Composable
fun Localized(content: @Composable () -> Unit) {
    val v by L10n.version.collectAsState()
    val base = LocalContext.current
    val ctx = remember(v, base) { L10n.localize(base) }
    CompositionLocalProvider(LocalContext provides ctx, LocalConfiguration provides ctx.resources.configuration) {
        key(v) { content() }
    }
}
