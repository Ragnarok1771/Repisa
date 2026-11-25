package com.repisa.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.repisa.app.R
import com.repisa.app.model.Game
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Una carátula flotando: posición (0..1), dirección, velocidad, giro, tamaño y profundidad. */
private class Floater(val game: Game, var x: Float, var y: Float, val angle: Float, val speed: Float,
                      val spin: Float, val size: Float, val depth: Float)

/**
 * Salvapantallas: las carátulas de la biblioteca flotan despacio por la vitrina a oscuras, rebotando en
 * los bordes, con el reloj en grande. Cualquier botón o toque lo cierra.
 */
@Composable
fun Screensaver(vm: LibraryViewModel, onClose: () -> Unit, main: Boolean = true) {
    // [main] = pantalla de arriba: lleva la hora y la batería, y recibe Atrás y el foco del mando.
    // En la de abajo solo flotan las carátulas.
    if (main) BackHandler(onBack = onClose)
    val games by vm.displayGames.collectAsState()
    val focus = remember { FocusRequester() }
    if (main) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val fadeIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fadeIn.animateTo(1f, tween(1500)) }

    val floaters = remember(games.isEmpty()) {
        val rnd = Random(System.currentTimeMillis())
        val pool = games.filter { it.coverUrl != null }.ifEmpty { games }.shuffled(rnd).take(16)
        pool.mapIndexed { i, g ->
            val depth = 0.45f + rnd.nextFloat() * 0.55f
            Floater(g, rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat() * 6.283f,
                (0.012f + rnd.nextFloat() * 0.014f) * depth, (rnd.nextFloat() - 0.5f) * 6f, 70f + 90f * depth, depth)
        }.sortedBy { it.depth }
    }
    // Reloj de fotogramas: cada fotograma mueve las carátulas un poquito.
    var frame by remember { mutableLongStateOf(0L) }
    var time by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.1f)
                last = now
                time += dt
                floaters.forEach { f ->
                    f.x += cos(f.angle) * f.speed * dt
                    f.y += sin(f.angle) * f.speed * dt * 1.6f
                    // Al salir por un lado, vuelve a entrar por el otro.
                    if (f.x < -0.15f) f.x = 1.1f; if (f.x > 1.15f) f.x = -0.1f
                    if (f.y < -0.25f) f.y = 1.15f; if (f.y > 1.25f) f.y = -0.2f
                }
                frame = now
            }
        }
    }
    var clock by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) { while (true) { clock = Calendar.getInstance(); delay(60_000L - System.currentTimeMillis() % 60_000L + 50) } }

    Box(
        Modifier.fillMaxSize().zIndex(10f)
            .graphicsLayer { alpha = fadeIn.value }
            .roomBackground()
            .then(if (main) Modifier.focusRequester(focus).focusable() else Modifier)
            .onKeyEvent { onClose(); true }
            .pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent(); onClose() } } },
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xAA000000)))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            floaters.forEach { f ->
                // La posición se lee al colocar y dibujar (no al componer): moverlas no rehace las cajas.
                GameBox(
                    f.game, f.size.dp, showStatus = false,
                    modifier = Modifier
                        .offset {
                            @Suppress("UNUSED_VARIABLE") val tick = frame
                            androidx.compose.ui.unit.IntOffset((w * f.x - f.size * 0.35f * density).toInt(), (h * f.y - f.size / 2 * density).toInt())
                        }
                        .graphicsLayer {
                            rotationZ = sin(time * 0.4f + f.spin) * f.spin
                            alpha = 0.35f + 0.65f * f.depth
                        },
                )
            }
        }
        // Abajo: tu semana (horas por día de los últimos 7 días) sobre las carátulas.
        if (!main) WeekChart(vm, Modifier.align(Alignment.Center))
        if (main) Column(Modifier.align(Alignment.BottomEnd).padding(28.dp), horizontalAlignment = Alignment.End) {
            Text("%02d:%02d".format(clock.get(Calendar.HOUR_OF_DAY), clock.get(Calendar.MINUTE)), color = Color(0xEEF3E6D3),
                fontSize = 64.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
            // La batería, debajo de la hora (los dos datos que importan con la consola en reposo).
            BatteryIndicator(Modifier.padding(bottom = 6.dp).graphicsLayer { scaleX = 1.35f; scaleY = 1.35f; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f) })
            Text(stringResource(R.string.screensaver_hint), color = Color(0x99F3E6D3), fontSize = 13.sp)
        }
    }
}

/** Gráfico pequeño de la semana: horas de cada uno de los últimos 7 días, el total y los días jugados. */
@Composable
private fun WeekChart(vm: LibraryViewModel, modifier: Modifier = Modifier) {
    val sessions by vm.sessions.collectAsState()
    val week = androidx.compose.runtime.remember(sessions) {
        val zone = java.time.ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zone)
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val perDay = sessions.filter { !it.lastOnly }
            .groupBy { java.time.Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate() }
            .mapValues { e -> e.value.sumOf { it.minutes } }
        days.map { it to (perDay[it] ?: 0) }
    }
    val max = (week.maxOfOrNull { it.second } ?: 0).coerceAtLeast(60)
    val total = week.sumOf { it.second }
    Column(
        modifier.background(Color(0xCC120C09), androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.week_title), color = Color(0xFFFFD27A), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
        androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
        androidx.compose.foundation.layout.Row(Modifier.height(120.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom) {
            week.forEach { (day, mins) ->
                Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom) {
                    if (mins > 0) Text(if (mins >= 60) "${mins / 60}h" else "${mins}′", color = Color(0xFFBFA98A), fontSize = 10.sp)
                    Box(Modifier.width(22.dp).fillMaxHeight((mins.toFloat() / max * 0.8f).coerceAtLeast(0.02f))
                        .background(if (mins > 0) Color(0xFFE0A84A) else Color(0x33FFFFFF),
                            androidx.compose.foundation.shape.RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                    Text(day.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()).uppercase(),
                        color = Color(0xFFF3E6D3), fontSize = 11.sp)
                }
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.week_summary, com.repisa.app.model.formatPlaytime(total), week.count { it.second > 0 }),
            color = Color(0xFFF3E6D3), fontSize = 13.sp)
    }
}
