package com.repisa.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.R

/** En qué pantalla está lo que explica cada paso del tutorial. */
enum class TourScreen { MAIN, SETTINGS }

/** Un paso del tutorial: lo que se señala ([target], o nada para un mensaje centrado), dónde está y qué se dice. */
data class TourStep(val target: String?, val screen: TourScreen, val emoji: String, val title: Int, val body: Int)

/**
 * Tutorial dentro de la app: señala cada botón con una explicación corta, con «Anterior», «Siguiente» y
 * «Saltar tutorial». Recorre la pantalla principal y luego Ajustes. Se ve al terminar la guía de inicio y con el
 * botón «?» de Ajustes.
 */
object Tour {
    val steps = listOf(
        TourStep(null, TourScreen.MAIN, "👋", R.string.tour_hello_t, R.string.tour_hello_b),
        TourStep("achievements", TourScreen.MAIN, "🏆", R.string.tour_ach_t, R.string.tour_ach_b),
        TourStep("stats", TourScreen.MAIN, "📊", R.string.tour_stats_t, R.string.tour_stats_b),
        TourStep("search", TourScreen.MAIN, "🔍", R.string.tour_search_t, R.string.tour_search_b),
        TourStep("random", TourScreen.MAIN, "🎲", R.string.tour_random_t, R.string.tour_random_b),
        TourStep("ratings", TourScreen.MAIN, "⭐", R.string.tour_ratings_t, R.string.tour_ratings_b),
        TourStep("sync", TourScreen.MAIN, "🔄", R.string.tour_sync_t, R.string.tour_sync_b),
        TourStep("filters", TourScreen.MAIN, "🎮", R.string.tour_filters_t, R.string.tour_filters_b),
        TourStep("sort", TourScreen.MAIN, "🗂", R.string.tour_sort_t, R.string.tour_sort_b),
        TourStep("shelves", TourScreen.MAIN, "📚", R.string.tour_shelves_t, R.string.tour_shelves_b),
        TourStep("scrollbar", TourScreen.MAIN, "↕", R.string.tour_scroll_t, R.string.tour_scroll_b),
        TourStep("settings", TourScreen.MAIN, "⚙", R.string.tour_settings_t, R.string.tour_settings_b),
        TourStep("set_accounts", TourScreen.SETTINGS, "🔗", R.string.tour_accounts_t, R.string.tour_accounts_b),
        TourStep("set_EMULATORS", TourScreen.SETTINGS, "🕹", R.string.tour_emus_t, R.string.tour_emus_b),
        TourStep("set_COVERS", TourScreen.SETTINGS, "🖼", R.string.tour_covers_t, R.string.tour_covers_b),
        TourStep("set_PLAYTIME", TourScreen.SETTINGS, "⏰", R.string.tour_time_t, R.string.tour_time_b),
        TourStep("set_DISPLAY", TourScreen.SETTINGS, "📺", R.string.tour_display_t, R.string.tour_display_b),
        TourStep("set_THEMES", TourScreen.SETTINGS, "🎨", R.string.tour_themes_t, R.string.tour_themes_b),
        TourStep("set_COLLECTIONS", TourScreen.SETTINGS, "📂", R.string.tour_colls_t, R.string.tour_colls_b),
        TourStep("set_DATA", TourScreen.SETTINGS, "💾", R.string.tour_backup_t, R.string.tour_backup_b),
        TourStep("set_updates", TourScreen.SETTINGS, "⬆", R.string.tour_updates_t, R.string.tour_updates_b),
        TourStep("set_help", TourScreen.SETTINGS, "❓", R.string.tour_help_t, R.string.tour_help_b),
        TourStep(null, TourScreen.MAIN, "🎉", R.string.tour_done_t, R.string.tour_done_b),
    )

    /** Dónde está cada cosa que se puede señalar (en coordenadas de la ventana). */
    val bounds = mutableStateMapOf<String, Rect>()

    /** Lo que se está explicando ahora (para traerlo a la vista si está fuera de la pantalla). */
    var current by mutableStateOf<String?>(null)
}

/** Marca un elemento para que el tutorial lo pueda señalar (y desplazarlo a la vista cuando le toca). */
fun Modifier.tourTarget(id: String): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val active = Tour.current == id
    LaunchedEffect(active) { if (active) runCatching { requester.bringIntoView() } }
    this.bringIntoViewRequester(requester)
        .onGloballyPositioned { Tour.bounds[id] = it.boundsInRoot() }
}

/**
 * La capa del tutorial: oscurece todo salvo lo que se explica (con un marco dorado) y muestra la explicación al lado,
 * con «Anterior», «Siguiente» y «Saltar». No deja tocar lo de debajo. Atrás (o B) cierra el tutorial.
 */
@Composable
fun TourOverlay(step: Int, onStep: (Int) -> Unit, onClose: () -> Unit) {
    val s = Tour.steps[step]
    BackHandler(onBack = onClose)
    LaunchedEffect(s.target) { Tour.current = s.target }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val rect = s.target?.let { Tour.bounds[it] }?.translate(-origin)
    val next = remember { FocusRequester() }
    LaunchedEffect(step) { kotlinx.coroutines.delay(120); runCatching { next.requestFocus() } }
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent().changes.forEach { it.consume() } } } },
    ) {
        val pad = with(density) { 8.dp.toPx() }
        // Oscuro con un hueco donde está lo que se explica.
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color(0xC8000000))
            rect?.let { r ->
                val hole = Rect(r.left - pad, r.top - pad, r.right + pad, r.bottom + pad)
                drawRoundRect(Color.Black, hole.topLeft, hole.size, CornerRadius(pad * 1.5f), blendMode = BlendMode.Clear)
                drawRoundRect(Color(0xFFFFD27A), hole.topLeft, hole.size, CornerRadius(pad * 1.5f), style = Stroke(width = pad / 2.5f))
            }
        }
        val w = maxWidth
        val h = maxHeight
        val cardW = minOf(w - 32.dp, 520.dp)
        // La explicación: debajo de lo señalado si cabe; si no, encima; sin nada que señalar, en el centro.
        val (x, y) = with(density) {
            if (rect == null) ((w - cardW) / 2) to (h / 2 - 130.dp)
            else {
                val cx = (rect.center.x.toDp() - cardW / 2).coerceIn(16.dp, w - cardW - 16.dp)
                val below = rect.bottom.toDp() + 20.dp
                val cy = if (below + 230.dp < h) below else (rect.top.toDp() - 250.dp).coerceAtLeast(16.dp)
                cx to cy
            }
        }
        Column(
            Modifier.offset { IntOffset(x.roundToPx(), y.roundToPx()) }.width(cardW)
                .background(Color(0xF52A1C14), RoundedCornerShape(16.dp)).border(2.dp, Color(0xFFFFD27A), RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.emoji, fontSize = 24.sp)
                Spacer(Modifier.size(10.dp))
                Text(stringResource(s.title), color = Color(0xFFFFD27A), fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    fontFamily = DisplayFont, modifier = Modifier.weight(1f))
                Text("${step + 1}/${Tour.steps.size}", color = Color(0xFFBFA98A), fontSize = 12.sp)
            }
            Text(stringResource(s.body), color = Color(0xFFF3E6D3), fontSize = 15.sp, lineHeight = 21.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val last = step == Tour.steps.lastIndex
                if (!last) TextButton(onClick = onClose) { Text(stringResource(R.string.tour_skip), color = Color(0xFFBFA98A)) }
                Spacer(Modifier.weight(1f))
                if (step > 0) OutlinedButton(onClick = { onStep(step - 1) }) { Text(stringResource(R.string.tour_prev), color = Color.White) }
                Button(onClick = { if (last) onClose() else onStep(step + 1) }, modifier = Modifier.focusRequester(next)) {
                    Text(stringResource(if (last) R.string.tour_finish else R.string.tour_next))
                }
            }
        }
    }
}
