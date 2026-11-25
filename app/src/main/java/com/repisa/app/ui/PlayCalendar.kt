package com.repisa.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.R
import com.repisa.app.data.PlaySession
import com.repisa.app.data.dayKey
import com.repisa.app.model.Game
import com.repisa.app.model.formatPlaytime
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Gold = Color(0xFFFFD27A)
private val Ink = Color(0xFFF3E6D3)
private val Muted = Color(0xFFBFA98A)
private val Panel = Color(0xE6261A12)

/**
 * Calendario de partidas: el mes en una cuadrícula (cada día más dorado cuanto más se jugó) y,
 * al elegir un día, sus partidas con la carátula, la hora y lo que duraron.
 * [sample] = con partidas de ejemplo (vista previa desde Ajustes).
 */
@Composable
fun PlayCalendar(vm: LibraryViewModel, sample: Boolean, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val real by vm.sessions.collectAsState()
    val games by vm.displayGames.collectAsState()
    val rawGames by vm.games.collectAsState()
    val sessions = remember(sample, real, rawGames) { if (sample) vm.sampleSessions() else vm.withLastPlayed(real, rawGames) }
    val byId = remember(games) { buildMap { games.forEach { g -> put(g.id, g); g.copyIds.forEach { put(it, g) } } } }
    val byDay = remember(sessions) { sessions.groupBy { dayKey(it.start) } }

    val today = remember { Calendar.getInstance() }
    var monthOffset by remember { mutableIntStateOf(0) }
    // Se puede ir hacia atrás hasta el mes del dato más antiguo.
    val minOffset = remember(sessions) {
        val first = sessions.minOfOrNull { it.start } ?: return@remember 0
        val c = Calendar.getInstance().apply { timeInMillis = first }
        -((today.get(Calendar.YEAR) - c.get(Calendar.YEAR)) * 12 + today.get(Calendar.MONTH) - c.get(Calendar.MONTH))
    }
    val month = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, monthOffset) }
    // Hoy; o, si hoy no se jugó, el último día con partidas.
    var selected by remember(byDay) {
        val t = dayKey(today.timeInMillis)
        mutableIntStateOf(if (byDay.containsKey(t)) t else byDay.keys.filter { it <= t }.maxOrNull() ?: t)
    }

    // Botones del mando que llegan desde la actividad (también si antes se tocó la pantalla y no hay nada con foco):
    // L1 / R1 cambian de mes; la cruceta o A, sin foco, lo devuelven al día elegido.
    val start = remember { androidx.compose.ui.focus.FocusRequester() }
    val dayFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        vm.calendarKeys.collect { code ->
            when (code) {
                android.view.KeyEvent.KEYCODE_BUTTON_L1 -> if (monthOffset > minOffset) monthOffset--
                android.view.KeyEvent.KEYCODE_BUTTON_R1 -> if (monthOffset < 0) monthOffset++
                else -> if (runCatching { dayFocus.requestFocus() }.isFailure) runCatching { start.requestFocus() }
            }
        }
    }
    // Joystick derecho: desplaza la lista de partidas del día.
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val stick by vm.rightStick.collectAsState()
    androidx.compose.runtime.LaunchedEffect(stick) {
        while (stick != 0f) { listState.scrollBy(stick * 24f); kotlinx.coroutines.delay(16) }
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { vm.calendarFocused = false; vm.rightStick.value = 0f } }

    Box(Modifier.fillMaxSize().roomBackground().onFocusChanged { vm.calendarFocused = it.hasFocus }.blockUnderneath()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // El foco del mando empieza aquí (dentro del calendario, no en la pantalla de debajo).
                androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { start.requestFocus() } }
                IconButton(onClick = onClose, modifier = Modifier.focusRequester(start)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
                }
                Text("📅 " + stringResource(R.string.cal_title), color = Gold, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
                if (sample) Text("  " + stringResource(R.string.preview_sample_tag), color = Muted, fontSize = 12.sp)
            }
            Row(Modifier.fillMaxSize().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // ── Mes
                Column(Modifier.weight(1.25f).fillMaxHeight().background(Panel, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { monthOffset-- }, enabled = monthOffset > minOffset) {
                            Text("◀", color = if (monthOffset > minOffset) Ink else Muted, fontSize = 18.sp)
                        }
                        Text(SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(month.time).replaceFirstChar { it.uppercase() },
                            color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        TextButton(onClick = { monthOffset++ }, enabled = monthOffset < 0) { Text("▶", color = if (monthOffset < 0) Ink else Muted, fontSize = 18.sp) }
                    }
                    MonthGrid(month, byDay, selected, today, dayFocus) { selected = it }
                    Spacer(Modifier.height(8.dp))
                    MonthSummary(month, byDay, byId)
                }
                // ── Día elegido
                Column(Modifier.weight(1f).fillMaxHeight().background(Panel, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    val list = byDay[selected].orEmpty().sortedBy { it.start }
                    val date = list.firstOrNull()?.start ?: dayStart(selected)
                    Text(DateFormat.getDateInstance(DateFormat.FULL).format(Date(date)).replaceFirstChar { it.uppercase() },
                        color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(when {
                        list.isEmpty() -> stringResource(R.string.cal_no_games_day)
                        list.all { it.lastOnly } -> stringResource(R.string.cal_day_last_only, list.size)
                        else -> stringResource(R.string.cal_day_total, formatPlaytime(list.sumOf { it.minutes }), list.count { !it.lastOnly })
                    },
                        color = Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(list) { s -> SessionRow(s, byId[s.gameId]) }
                    }
                }
            }
        }
    }
}

private fun dayStart(key: Int): Long = Calendar.getInstance().run {
    clear(); set(Calendar.YEAR, key / 1000); set(Calendar.DAY_OF_YEAR, key % 1000); timeInMillis
}

@Composable
private fun MonthGrid(month: Calendar, byDay: Map<Int, List<PlaySession>>, selected: Int, today: Calendar,
                      selectedFocus: androidx.compose.ui.focus.FocusRequester, onPick: (Int) -> Unit) {
    val days = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Lunes primero.
    val firstCol = (month.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val maxMinutes = (1..days).maxOfOrNull { d ->
        byDay[dayKey((month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, d) }.timeInMillis)].orEmpty().sumOf { it.minutes }
    }?.coerceAtLeast(60) ?: 60
    val weekdays = remember {
        val c = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) }
        (0 until 7).map { SimpleDateFormat("EEEEE", Locale.getDefault()).format(c.time).also { c.add(Calendar.DAY_OF_YEAR, 1) }.uppercase() }
    }
    Row(Modifier.fillMaxWidth()) {
        weekdays.forEach { Text(it, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
    }
    val cells = firstCol + days
    for (week in 0 until (cells + 6) / 7) {
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (col in 0 until 7) {
                val d = week * 7 + col - firstCol + 1
                if (d < 1 || d > days) { Spacer(Modifier.weight(1f)); continue }
                val c = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, d) }
                val key = dayKey(c.timeInMillis)
                val mins = byDay[key].orEmpty().sumOf { it.minutes }
                val lastOnly = byDay[key].orEmpty().count { it.lastOnly }
                val isToday = key == dayKey(today.timeInMillis)
                val isFuture = c.after(today) && !isToday
                var focused by remember { mutableStateOf(false) }
                val heat = if (mins == 0) 0f else 0.25f + 0.75f * (mins.toFloat() / maxMinutes).coerceAtMost(1f)
                Box(
                    Modifier.weight(1f).aspectRatio(1.5f)
                        .then(if (key == selected) Modifier.focusRequester(selectedFocus) else Modifier)
                        .clip(RoundedCornerShape(6.dp))
                        .background(when {
                            mins > 0 -> Color(0xFFE0A84A).copy(alpha = heat)
                            lastOnly > 0 -> Color(0x33E0A84A) // solo «último día jugado»: tono suave
                            else -> Color(0x14FFFFFF)
                        })
                        .border(
                            when { focused || key == selected -> 2.dp; isToday -> 1.dp; else -> 0.dp },
                            when { focused -> Color.White; key == selected -> Gold; else -> Color(0x88FFD27A) },
                            RoundedCornerShape(6.dp),
                        )
                        .onFocusChanged { focused = it.isFocused; if (it.isFocused) onPick(key) }
                        .clickable(enabled = !isFuture) { onPick(key) },
                ) {
                    Text(d.toString(), color = if (isFuture) Color(0x55F3E6D3) else if (heat > 0.6f) Color(0xFF2A1C14) else Ink,
                        fontSize = 13.sp, fontWeight = if (isToday) FontWeight.Black else FontWeight.Normal,
                        modifier = Modifier.align(Alignment.TopStart).padding(start = 5.dp, top = 2.dp))
                    // Un puntito por juego (hasta 3) y, si hay más, cuántos más: «••• +1».
                    if (mins == 0 && lastOnly > 0) Text("•".repeat(lastOnly.coerceAtMost(3)) + if (lastOnly > 3) " +${lastOnly - 3}" else "", color = Gold, fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp))
                    if (mins > 0) Text(if (mins >= 60) "${mins / 60} h" else "$mins′",
                        color = if (heat > 0.6f) Color(0xFF2A1C14) else Ink, fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 1.dp))
                }
            }
        }
    }
}

@Composable
private fun MonthSummary(month: Calendar, byDay: Map<Int, List<PlaySession>>, byId: Map<String, Game>) {
    val y = month.get(Calendar.YEAR); val m = month.get(Calendar.MONTH)
    val inMonth = byDay.values.flatten().filter { s -> Calendar.getInstance().apply { timeInMillis = s.start }.let { it.get(Calendar.YEAR) == y && it.get(Calendar.MONTH) == m } }
    val days = inMonth.map { dayKey(it.start) }.distinct().size
    val top = inMonth.filter { !it.lastOnly }.groupBy { it.gameId }.maxByOrNull { e -> e.value.sumOf { it.minutes } }
    Text(stringResource(R.string.cal_month_total, formatPlaytime(inMonth.sumOf { it.minutes }), days), color = Ink, fontSize = 13.sp)
    top?.let { (id, l) ->
        Text(stringResource(R.string.cal_month_top, byId[id]?.title ?: l.first().title, formatPlaytime(l.sumOf { it.minutes })),
            color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SessionRow(s: PlaySession, game: Game?) {
    // Se puede enfocar con la cruceta: así se recorre la lista con el mando (la lista se desplaza sola).
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().background(Color(0x22000000), RoundedCornerShape(8.dp))
        .border(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
        .onFocusChanged { focused = it.isFocused }.focusable()
        .padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (game != null) GameBox(game, 56.dp, showStatus = false) else Box(Modifier.size(40.dp, 56.dp).background(Color(0x33FFFFFF)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(game?.title ?: s.title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sessionLine(s), color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun sessionLine(s: PlaySession): String {
    val t = DateFormat.getTimeInstance(DateFormat.SHORT)
    if (s.lastOnly) return stringResource(R.string.cal_last_played)
    return if (s.measured) "${t.format(Date(s.start))} – ${t.format(Date(s.end))} · ${formatPlaytime(s.minutes)}"
    else stringResource(R.string.cal_session_synced, formatPlaytime(s.minutes), t.format(Date(s.end)))
}

/** Partidas de un juego (ficha del juego → «📅 Partidas»). */
@Composable
fun GameSessionsDialog(vm: LibraryViewModel, game: Game, onDismiss: () -> Unit) {
    val real by vm.sessions.collectAsState()
    val rawGames by vm.games.collectAsState()
    val ids = remember(game) { (listOf(game.id) + game.copyIds).toSet() }
    val list = remember(real, rawGames, ids) { vm.withLastPlayed(real, rawGames).filter { it.gameId in ids }.sortedByDescending { it.start } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📅 " + stringResource(R.string.cal_game_sessions, game.title)) },
        text = {
            if (list.isEmpty()) Text(stringResource(R.string.cal_game_none))
            else Column {
                Text(stringResource(R.string.cal_day_total, formatPlaytime(list.sumOf { it.minutes }), list.count { !it.lastOnly }), fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn(Modifier.height(320.dp)) {
                    items(list) { s ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(when { s.lastOnly -> Color(0xFF9C8A70); s.measured -> Color(0xFF7BD389); else -> Color(0xFFE0A84A) }, CircleShape))
                            Text("  " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(s.start)), modifier = Modifier.weight(1f))
                            Text(sessionLine(s), fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
