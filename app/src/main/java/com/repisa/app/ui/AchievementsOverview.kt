package com.repisa.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.repisa.app.R
import com.repisa.app.model.Game
import com.repisa.app.sources.Achievement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private val Ink = Color(0xFFF3E6D3)
private val Muted = Color(0xFFBFA98A)

/**
 * Todos tus logros: totales, los últimos que conseguiste (con fecha) y el progreso de cada juego.
 * Tocar un juego abre su ficha.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AchievementsOverview(vm: LibraryViewModel, onBack: () -> Unit, onOpen: (Game) -> Unit) {
    val games by vm.displayGames.collectAsState()
    val withAch = games.filter { (it.achTotal ?: 0) > 0 }.sortedWith(
        compareByDescending<Game> { (it.achUnlocked ?: 0).toFloat() / it.achTotal!! }.thenByDescending { it.achUnlocked ?: 0 })
    // Últimos logros conseguidos (de los que ya se han cargado), con su juego.
    val recent by produceState(emptyList<Pair<Achievement, Game>>(), games) {
        value = withContext(Dispatchers.IO) {
            val byFile = games.flatMap { g -> (listOf(g.id) + g.copyIds).map { vm.achievementStore.fileKey(it) to g } }.toMap()
            vm.achievementStore.all().flatMap { (key, set) ->
                val g = byFile[key] ?: return@flatMap emptyList()
                set.items.filter { (it.unlockedAt ?: 0) > 1 }.map { it to g }
            }.sortedByDescending { it.first.unlockedAt }.take(40)
        }
    }
    val sessions by vm.sessions.collectAsState()
    val repisaCount by produceState(0, games, sessions) {
        value = withContext(Dispatchers.Default) {
            games.sumOf { g -> val ctx = com.repisa.app.model.GameAchContext(g, sessions); com.repisa.app.model.RepisaAchievement.entries.count { it.met(ctx) } }
        }
    }
    val platinums by vm.platinums.collectAsState()
    // Secciones plegables («Últimos logros» y «Logros de Repisa»): plegadas al principio; se recuerda cómo las dejas.
    val opened by vm.collapsed.collectAsState()
    val recentOpen = "achopen:recent" in opened
    val appOpen = "achopen:app" in opened
    val appAch by vm.appAchievements.collectAsState()
    val platGames = remember(platinums, games) {
        games.filter { it.id in platinums }.sortedByDescending { platinums[it.id] }
    }
    val unlocked = withAch.sumOf { it.achUnlocked ?: 0 }
    val total = withAch.sumOf { it.achTotal ?: 0 }

    Box(Modifier.fillMaxSize().roomBackground()) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White) }
                    Text("🏆 " + stringResource(R.string.achievements), color = Color(0xFFFFD27A), fontSize = 26.sp,
                        fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile("$unlocked / $total", stringResource(R.string.achov_unlocked))
                    Tile(withAch.size.toString(), stringResource(R.string.achov_games))
                    Tile(withAch.count { it.achUnlocked == it.achTotal }.toString(), stringResource(R.string.achov_perfect))
                    Tile(repisaCount.toString(), stringResource(R.string.achov_repisa))
                    Tile("💎 " + platGames.size, stringResource(R.string.plat_count))
                    Tile("${appAch.count { it.second != null }} / ${appAch.size}", stringResource(R.string.aach_count))
                }
            }
            // Colección de platinos: las cajas en su propia balda, con la fecha.
            item { Header("💎 " + stringResource(R.string.plat_collection)) }
            item {
                if (platGames.isEmpty()) Text(stringResource(R.string.plat_none), color = Muted, fontSize = 13.sp)
                else Column {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Bottom, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
                        items(platGames, key = { "p" + it.id }) { g ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onOpen(g) }) {
                                Box {
                                    GameBox(g, 120.dp, showStatus = false)
                                    Text("💎", fontSize = 20.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 10.dp))
                                }
                                Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(platinums[g.id] ?: 0)), color = Color(0xFFBFEAFF), fontSize = 11.sp)
                            }
                        }
                    }
                    WoodPlank(modifier = Modifier.fillMaxWidth().height(24.dp))
                }
            }
            // Logros de la app: conseguidos (con fecha) y los que faltan con su progreso.
            item {
                FoldHeader("🏆 " + stringResource(R.string.aach_title) + " (${appAch.count { it.second != null }} / ${appAch.size})", appOpen) {
                    vm.toggleCollapsed("achopen:app")
                }
            }
            val orderedApp = appAch.filter { it.second != null }.sortedByDescending { it.second } +
                appAch.filter { it.second == null }.sortedByDescending { it.third.toFloat() / it.first.target }
            if (appOpen) items(orderedApp, key = { "a" + it.first.name }) { (a, at, value) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    .then(if (at == null) Modifier.alpha(0.7f) else Modifier)) {
                    Text(a.icon, fontSize = 22.sp, modifier = Modifier.width(40.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.tier.medal + " " + a.title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(a.description, color = Muted, fontSize = 12.sp)
                        if (at == null && a.target > 1) Box(Modifier.padding(top = 3.dp).fillMaxWidth(0.6f).height(6.dp).background(Color(0x22FFFFFF), RoundedCornerShape(3.dp))) {
                            Box(Modifier.fillMaxWidth((value.toFloat() / a.target).coerceIn(0f, 1f)).height(6.dp).background(Color(0xFFE0A84A), RoundedCornerShape(3.dp)))
                        }
                    }
                    Text(at?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) } ?: "${minOf(value, a.target)} / ${a.target}",
                        color = if (at != null) Color(0xFF7BD389) else Muted, fontSize = 12.sp)
                }
            }
            if (recent.isNotEmpty()) {
                item { FoldHeader(stringResource(R.string.achov_recent) + " (${recent.size})", recentOpen) { vm.toggleCollapsed("achopen:recent") } }
                if (recentOpen) items(recent, key = { "r" + it.second.id + it.first.id }) { (a, g) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .clickable { onOpen(g) }.padding(4.dp)) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A1E17))) {
                            a.icon?.let { AsyncImage(it, null, Modifier.fillMaxSize()) }
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(a.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(g.title, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(a.unlockedAt!!)),
                            color = Color(0xFF7BD389), fontSize = 12.sp)
                    }
                }
            }
            item { Header(stringResource(R.string.achov_by_game)) }
            if (withAch.isEmpty()) item { Text(stringResource(R.string.achov_none), color = Muted, fontSize = 14.sp) }
            items(withAch, key = { "g" + it.id }) { g ->
                val frac = (g.achUnlocked ?: 0).toFloat() / g.achTotal!!
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .clickable { onOpen(g) }.padding(vertical = 4.dp, horizontal = 4.dp)) {
                    Text(g.title, color = Ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 380.dp).width(320.dp))
                    Box(Modifier.weight(1f).height(10.dp).background(Color(0x22FFFFFF), RoundedCornerShape(4.dp))) {
                        Box(Modifier.fillMaxWidth(frac).height(10.dp).background(Color(0xFFE0A84A), RoundedCornerShape(4.dp)))
                    }
                    Text("${g.achUnlocked ?: 0}/${g.achTotal} (${(frac * 100).toInt()} %)", color = Muted, fontSize = 13.sp,
                        modifier = Modifier.padding(start = 10.dp).width(120.dp))
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
private fun Header(text: String) =
    Text(text, color = Color(0xFFFFD27A), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))

/** Título de sección que se pliega y despliega al tocarlo (o con A en el mando). */
@Composable
private fun FoldHeader(text: String, open: Boolean, onToggle: () -> Unit) {
    var focused by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Text((if (open) "▾ " else "▸ ") + text, color = Color(0xFFFFD27A), fontSize = 16.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(6.dp))
            .then(if (focused) Modifier.background(Color(0x33FFD27A)) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onToggle).padding(horizontal = 6.dp, vertical = 4.dp))
}
