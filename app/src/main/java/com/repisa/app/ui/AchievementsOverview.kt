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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val repisaCount by produceState(0, games) {
        value = withContext(Dispatchers.Default) {
            games.sumOf { g -> com.repisa.app.model.RepisaAchievement.entries.count { it.met(g) } }
        }
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
                }
            }
            if (recent.isNotEmpty()) {
                item { Header(stringResource(R.string.achov_recent)) }
                items(recent, key = { "r" + it.second.id + it.first.id }) { (a, g) ->
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
