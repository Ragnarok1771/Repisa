package com.repisa.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.R
import com.repisa.app.i18n.L10n
import com.repisa.app.model.Consoles
import com.repisa.app.model.Game
import com.repisa.app.model.PlayStatus
import com.repisa.app.model.formatPlaytime
import com.repisa.app.model.label
import com.repisa.app.model.playStatus
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * «¿A qué juego?»: una tira de carátulas que da entre 2 y 4 vueltas y se frena hasta parar en un juego (como abrir una
 * caja). Filtros: Todo · Según mis gustos · Solo pendientes · Plataforma ▾ · Duración ▾.
 */
@Composable
fun RandomPicker(vm: LibraryViewModel, onClose: () -> Unit, onOpen: (Game) -> Unit) {
    val all by vm.displayGames.collectAsState()
    val anyBacklog = remember(all) { all.any { it.playStatus == PlayStatus.BACKLOG } }
    var onlyBacklog by remember { mutableStateOf(anyBacklog) }
    var maxHours by remember { mutableStateOf<Int?>(null) }
    var console by remember { mutableStateOf<String?>(null) }
    var byTaste by remember { mutableStateOf(false) }
    var platformMenu by remember { mutableStateOf(false) }
    var durationMenu by remember { mutableStateOf(false) }
    val noFilters = !onlyBacklog && maxHours == null && console == null && !byTaste

    val consoles = remember(all) { all.groupBy { it.consoleId }.entries.sortedByDescending { it.value.size }.map { it.key } }
    val candidates = remember(all, onlyBacklog, maxHours, console) {
        all.filter { g ->
            val st = g.playStatus
            (!onlyBacklog || st == PlayStatus.BACKLOG) &&
                (console == null || g.consoleId == console) &&
                (maxHours == null || (g.hltbMain ?: Float.MAX_VALUE) <= maxHours!!) &&
                st != PlayStatus.FINISHED && st != PlayStatus.COMPLETED && st != PlayStatus.ABANDONED && st != PlayStatus.NOT_APPLICABLE
        }
    }
    // «Según mis gustos»: más probabilidad para los géneros a los que más horas dedicas.
    val genreShare = remember(all) {
        val total = all.sumOf { it.totalMinutes }.coerceAtLeast(1)
        all.flatMap { g -> g.genres.map { it to g.totalMinutes } }.groupBy({ it.first }, { it.second })
            .mapValues { it.value.sum().toDouble() / total }
    }
    fun weight(g: Game) = if (!byTaste) 1.0 else 1.0 + 3.0 * g.genres.sumOf { genreShare[it] ?: 0.0 }
    fun pick(): Game? {
        if (candidates.isEmpty()) return null
        val total = candidates.sumOf { weight(it) }
        var r = Random.nextDouble() * total
        for (g in candidates) { r -= weight(g); if (r <= 0) return g }
        return candidates.last()
    }

    var strip by remember { mutableStateOf<List<Game>>(emptyList()) }
    var winnerAt by remember { mutableStateOf(0) }
    var winner by remember { mutableStateOf<Game?>(null) }
    var rolling by remember { mutableStateOf(false) }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val slot = 132.dp
    val slotPx = with(density) { slot.toPx() }

    fun roll() {
        val w = pick() ?: return
        winner = null
        rolling = true
        // 2-4 vueltas: cada vuelta recorre la lista (barajada; si es enorme, una muestra de 36).
        val laps = Random.nextInt(2, 5)
        val lap = candidates.shuffled().take(36).let { l -> if (l.size < 8) List(8) { l[it % l.size] } else l }
        val body = List(laps) { lap.shuffled() }.flatten()
        winnerAt = body.size
        strip = body + w + List(6) { candidates.random() }
        scope.launch {
            offset.snapTo(0f)
            // Arranca rápido y se frena poco a poco; se para cerca del centro de la carátula, no siempre exacto.
            val target = winnerAt * slotPx + slotPx * (0.2f + Random.nextFloat() * 0.6f) - slotPx / 2
            offset.animateTo(target, tween(9000 + laps * 700, easing = CubicBezierEasing(0.05f, 0.85f, 0.15f, 1f)))
            offset.animateTo(winnerAt * slotPx, tween(350))
            winner = w
            rolling = false
            vm.focused.value = w // la pantalla de abajo muestra el juego elegido
        }
    }

    Box(Modifier.fillMaxSize().roomBackground().background(Color(0xCC120C08))) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, stringResource(R.string.close), tint = Color.White) }
                Text("🎲 " + stringResource(R.string.random_title), color = Color(0xFFFFD27A), fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.random_count, candidates.size), color = Color(0xFFBFA98A), fontSize = 13.sp)
            }
            // Filtros: Todo · Según mis gustos · Solo pendientes · Plataforma ▾ · Duración ▾
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                item {
                    FilterChip(noFilters, { onlyBacklog = false; maxHours = null; console = null; byTaste = false },
                        { Text(stringResource(R.string.filter_all)) })
                }
                item { FilterChip(byTaste, { byTaste = !byTaste }, { Text("♥ " + stringResource(R.string.random_by_taste)) }) }
                item { FilterChip(onlyBacklog, { onlyBacklog = !onlyBacklog }, { Text("⏳ " + stringResource(R.string.random_only_backlog)) }) }
                item {
                    Box {
                        FilterChip(console != null, { platformMenu = true }, {
                            Text((console?.let { Consoles.byId(it).label } ?: stringResource(R.string.random_platform)) + " ▾")
                        })
                        DropdownMenu(platformMenu, { platformMenu = false }) {
                            DropdownMenuItem({ Text(stringResource(R.string.random_any_platform)) }, { console = null; platformMenu = false })
                            consoles.forEach { c ->
                                DropdownMenuItem({ Text(Consoles.byId(c).label) }, { console = c; platformMenu = false })
                            }
                        }
                    }
                }
                item {
                    Box {
                        FilterChip(maxHours != null, { durationMenu = true }, {
                            Text((maxHours?.let { stringResource(R.string.random_max_hours, it) } ?: stringResource(R.string.random_duration)) + " ▾")
                        })
                        DropdownMenu(durationMenu, { durationMenu = false }) {
                            DropdownMenuItem({ Text(stringResource(R.string.random_any_duration)) }, { maxHours = null; durationMenu = false })
                            listOf(5, 10, 15, 20, 30, 40, 60).forEach { h ->
                                DropdownMenuItem({ Text(stringResource(R.string.random_max_hours, h)) }, { maxHours = h; durationMenu = false })
                            }
                        }
                    }
                }
            }
            // La tira de carátulas con la marca dorada en el centro (solo se dibujan las visibles).
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                val widthPx = with(density) { maxWidth.toPx() }
                val centerPx = widthPx / 2
                val boxH = minOf(maxHeight * 0.82f, 190.dp)
                if (strip.isEmpty()) {
                    Text(stringResource(if (candidates.isEmpty()) R.string.random_none else R.string.random_hint),
                        color = Color(0xFFBFA98A), fontSize = 16.sp, modifier = Modifier.align(Alignment.Center))
                } else {
                    val first = floor((offset.value - centerPx) / slotPx).toInt().coerceAtLeast(0)
                    val last = (first + (widthPx / slotPx).toInt() + 3).coerceAtMost(strip.lastIndex)
                    for (i in first..last) {
                        // key(i): cada carátula conserva su propio dibujo mientras avanza (sin key, al girar un hueco
                        // pasaba a mostrar otro juego y la franja de la consola parpadeaba y se desplazaba).
                        androidx.compose.runtime.key(i) {
                            val g = strip[i]
                            Box(Modifier.offset { IntOffset((centerPx - slotPx / 2 + i * slotPx - offset.value).roundToInt(), 0) }
                                .width(slot).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                val h = minOf(boxH, slot * 0.9f / frontAspect(g))
                                GameBox(g, h, lifted = !rolling && i == winnerAt && winner != null, showStatus = false)
                            }
                        }
                    }
                }
                // Marca central
                Box(Modifier.align(Alignment.Center).width(3.dp).fillMaxHeight().background(Color(0xCCFFD27A)))
            }
            // Resultado
            val w = winner
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    if (w != null) {
                        Text(w.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(w.console.label, w.genres.firstOrNull()?.let(L10n::genre),
                            w.hltbMain?.let { stringResource(R.string.random_hltb, L10n.decimal(it.toDouble(), 0)) },
                            formatPlaytime(w.totalMinutes)).joinToString(" · "), color = Color(0xFFBFA98A), fontSize = 13.sp)
                    }
                }
                if (w != null && vm.launcher.canLaunch(w)) Button(onClick = { vm.play(w) }) { Text("▶ " + stringResource(R.string.play)) }
                if (w != null) OutlinedButton(onClick = { onOpen(w) }) { Text(stringResource(R.string.random_open), color = Color.White) }
                Button(onClick = { roll() }, enabled = !rolling && candidates.isNotEmpty()) {
                    Text("🎲 " + stringResource(if (strip.isEmpty()) R.string.random_roll else R.string.random_again))
                }
            }
        }
    }
}
