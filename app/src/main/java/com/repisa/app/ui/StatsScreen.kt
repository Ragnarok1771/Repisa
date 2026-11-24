package com.repisa.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.R
import com.repisa.app.i18n.L10n
import com.repisa.app.model.Game
import com.repisa.app.model.PlayStatus
import com.repisa.app.model.formatPlaytime
import com.repisa.app.model.label
import com.repisa.app.model.playStatus

private val BarColor = Color(0xFFE0A84A)   // un solo tono: las barras indican cantidad, no identidad
private val Track = Color(0x22FFFFFF)
private val Ink = Color(0xFFF3E6D3)
private val Muted = Color(0xFFBFA98A)

/** Estadísticas de la biblioteca: totales, estados, horas por sistema/género, décadas y los más jugados. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(vm: LibraryViewModel, onBack: () -> Unit) {
    val all by vm.displayGames.collectAsState()
    val scale by vm.ratingScale.collectAsState()
    val s = remember(all) { Stats.of(all) }

    Box(Modifier.fillMaxSize().roomBackground()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White) }
                Text(stringResource(R.string.stats_title), color = Color(0xFFFFD27A), fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
            }
            // Cifras principales
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile(L10n.games(all.size), stringResource(R.string.stats_in_library))
                Tile(formatPlaytime(s.totalMinutes), stringResource(R.string.stats_played))
                Tile(s.done.toString(), stringResource(R.string.stats_done))
                Tile(s.backlog.toString(), stringResource(R.string.stats_backlog_left,
                    if (s.backlogHours > 0) L10n.decimal(s.backlogHours.toDouble(), 0) else "—"))
                s.avgRating?.let { Tile(scale.format(it), stringResource(R.string.stats_avg_rating, s.rated)) }
            }
            Card(stringResource(R.string.stats_by_status)) {
                PlayStatus.entries.forEach { st ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        Box(Modifier.size(12.dp).background(Color(st.color), CircleShape))
                        Text("  ${st.icon} ${st.label}", color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text((s.byStatus[st] ?: 0).toString(), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(stringResource(R.string.stats_no_status, s.byStatus[null] ?: 0), color = Muted, fontSize = 12.sp)
            }
            Card(stringResource(R.string.stats_hours_system)) { Bars(s.hoursBySystem) { formatPlaytime(it) } }
            Card(stringResource(R.string.stats_hours_genre)) { Bars(s.hoursByGenre) { formatPlaytime(it) } }
            Card(stringResource(R.string.stats_by_decade)) { Bars(s.gamesByDecade) { L10n.games(it) } }
            Card(stringResource(R.string.stats_most_played)) {
                s.top.forEachIndexed { i, g ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text("${i + 1}. ${g.title}", color = Ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(formatPlaytime(g.totalMinutes), color = Muted, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(value: String, label: String) {
    Column(Modifier.widthIn(min = 150.dp).background(Color(0xE6261A12), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(value, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xE6261A12), RoundedCornerShape(10.dp)).padding(14.dp)) {
        Text(title, color = Color(0xFFFFD27A), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}

/** Barras horizontales finas, ordenadas de mayor a menor, con el valor escrito al lado. */
@Composable
private fun Bars(rows: List<Pair<String, Int>>, format: (Int) -> String) {
    if (rows.isEmpty()) { Text("—", color = Muted); return }
    val max = rows.maxOf { it.second }.coerceAtLeast(1)
    rows.forEach { (label, v) ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
            Text(label, color = Ink, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(170.dp))
            Box(Modifier.weight(1f).height(12.dp).background(Track, RoundedCornerShape(4.dp))) {
                Box(Modifier.fillMaxWidth(v.toFloat() / max).height(12.dp).background(BarColor, RoundedCornerShape(4.dp)))
            }
            Text(format(v), color = Muted, fontSize = 13.sp, modifier = Modifier.width(110.dp).padding(start = 8.dp))
        }
    }
}

/** Cálculos de las estadísticas (sin Compose, para poder probarlos). */
data class Stats(
    val totalMinutes: Int,
    val done: Int,
    val backlog: Int,
    val backlogHours: Float,
    val avgRating: Float?,
    val rated: Int,
    val byStatus: Map<PlayStatus?, Int>,
    val hoursBySystem: List<Pair<String, Int>>,
    val hoursByGenre: List<Pair<String, Int>>,
    val gamesByDecade: List<Pair<String, Int>>,
    val top: List<Game>,
) {
    companion object {
        /** Las 9 categorías mayores y el resto sumado en «Otros». */
        private fun topWithOther(map: Map<String, Int>, other: String): List<Pair<String, Int>> {
            val sorted = map.entries.filter { it.value > 0 }.sortedByDescending { it.value }.map { it.key to it.value }
            if (sorted.size <= 10) return sorted
            return sorted.take(9) + (other to sorted.drop(9).sumOf { it.second })
        }

        fun of(all: List<Game>): Stats {
            val other = L10n.s(R.string.stats_other)
            val rated = all.mapNotNull { it.rating?.overall }
            val backlog = all.filter { it.playStatus == PlayStatus.BACKLOG }
            return Stats(
                totalMinutes = all.sumOf { it.totalMinutes },
                done = all.count { it.playStatus == PlayStatus.FINISHED || it.playStatus == PlayStatus.COMPLETED },
                backlog = backlog.size,
                backlogHours = backlog.sumOf { (it.hltbMain ?: 0f).toDouble() }.toFloat(),
                avgRating = rated.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
                rated = rated.size,
                byStatus = all.groupingBy { it.playStatus }.eachCount(),
                hoursBySystem = topWithOther(all.groupBy { it.console.label }.mapValues { e -> e.value.sumOf { it.totalMinutes } }, other),
                hoursByGenre = topWithOther(all.flatMap { g -> g.genres.map { L10n.genre(it) to g.totalMinutes } }
                    .groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }, other),
                gamesByDecade = all.mapNotNull { it.releaseYear }.groupingBy { it / 10 * 10 }.eachCount().entries
                    .sortedBy { it.key }.map { L10n.s(R.string.shelf_decade, it.key) to it.value },
                top = all.filter { it.totalMinutes > 0 }.sortedByDescending { it.totalMinutes }.take(10),
            )
        }
    }
}
