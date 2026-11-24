package com.repisa.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.repisa.app.R
import com.repisa.app.model.Game
import com.repisa.app.model.Source
import com.repisa.app.sources.Achievement
import com.repisa.app.sources.Achievements
import java.text.DateFormat
import java.util.Date

private val Ink = Color(0xFFF3E6D3)
private val Muted = Color(0xFFBFA98A)

private fun whenText(t: Long): String =
    if (t <= 1L) "✓" else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(t))

/**
 * Logros del juego: los conseguidos primero, por fecha (cuándo desbloqueaste cada uno), y luego los que faltan con
 * el % de jugadores que los tienen. En juegos de Epic (lista de Steam) se marcan a mano. Al final, los logros de Repisa.
 */
@Composable
fun AchievementsDialog(game: Game, vm: LibraryViewModel, onDismiss: () -> Unit) {
    LaunchedEffect(game.id) { vm.loadAchievements(game) }
    val all by vm.achievements.collectAsState()
    val loading by vm.achievementsLoading.collectAsState()
    val errors by vm.achievementErrors.collectAsState()
    val set = all[game.id]
    val known = game.id in all
    val repisa = remember(game) { vm.repisaAchievements(game) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().padding(16.dp).background(Color(0xFF1E1612), RoundedCornerShape(14.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🏆 " + stringResource(R.string.achievements) + " · " + game.title, color = Color(0xFFFFD27A),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    set?.let { s ->
                        Text(
                            stringResource(when (s.kind) { Achievements.RETRO -> R.string.ach_source_ra; Achievements.STEAM -> R.string.ach_source_steam
                                else -> R.string.ach_source_steam_list }) +
                                " · " + stringResource(R.string.ach_progress, s.unlocked, s.items.size, if (s.items.isEmpty()) 0 else s.unlocked * 100 / s.items.size),
                            color = Muted, fontSize = 13.sp,
                        )
                    }
                }
                if (game.id in loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else TextButton(onClick = { vm.loadAchievements(game, force = true) }) { Text("↻ " + stringResource(R.string.ach_retry)) }
                set?.url?.let { u -> TextButton(onClick = { vm.launcher.openUrl(u) }) { Text(stringResource(R.string.open_web)) } }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            }
            set?.let { s -> if (s.items.isNotEmpty()) LinearProgressIndicator(progress = { s.unlocked / s.items.size.toFloat() }, modifier = Modifier.fillMaxWidth()) }
            when (set?.note) {
                "epic" -> Note(stringResource(R.string.ach_note_epic))
                "private" -> Note(stringResource(R.string.ach_note_private))
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (set == null) item {
                    Text(
                        when {
                            game.id in loading || (!known && game.id !in errors) -> stringResource(R.string.ach_loading)
                            game.id in errors -> stringResource(R.string.ach_error)
                            game.source == Source.EMULATOR && game.consoleId !in Achievements.raConsoles ->
                                stringResource(R.string.ach_no_ra_system, game.console.name)
                            game.source == Source.EMULATOR && vm.settings.raUser == null -> stringResource(R.string.ach_link_ra)
                            else -> stringResource(R.string.ach_none)
                        },
                        color = Muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    val manualMode = set.kind == Achievements.STEAM_LIST
                    val done = set.items.filter { it.unlockedAt != null }.sortedByDescending { it.unlockedAt }
                    val left = set.items.filter { it.unlockedAt == null }.sortedByDescending { it.percent ?: -1f }
                    if (done.isNotEmpty()) item { Header(stringResource(R.string.ach_unlocked, done.size)) }
                    items(done, key = { "d" + it.id }) { a -> AchRow(a, manualMode) { vm.toggleManualAchievement(game, a.id) } }
                    if (left.isNotEmpty()) item { Header(stringResource(R.string.ach_locked, left.size)) }
                    items(left, key = { "l" + it.id }) { a -> AchRow(a, manualMode) { vm.toggleManualAchievement(game, a.id) } }
                }
                item { Header(stringResource(R.string.ach_repisa)) }
                items(repisa, key = { "r" + it.first.name }) { (a, at) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().alpha(if (at != null) 1f else 0.5f).padding(vertical = 3.dp)) {
                        Text(a.icon, fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(a.description, color = Muted, fontSize = 12.sp)
                        }
                        Text(at?.let(::whenText) ?: "—", color = if (at != null) Color(0xFF7BD389) else Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, color = Color(0xFFE8D9C0), fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().background(Color(0x33FFD27A), RoundedCornerShape(8.dp)).padding(10.dp))
}

@Composable
private fun Header(text: String) {
    Text(text, color = Color(0xFFFFD27A), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
}

@Composable
private fun AchRow(a: Achievement, manualMode: Boolean, onToggle: () -> Unit) {
    val got = a.unlockedAt != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .then(if (manualMode) Modifier.clickable(onClick = onToggle) else Modifier).padding(vertical = 4.dp, horizontal = 4.dp),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A1E17))) {
            a.icon?.let {
                AsyncImage(it, null, Modifier.fillMaxSize().alpha(if (got) 1f else 0.45f),
                    colorFilter = if (got) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }))
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(a.name + (a.points?.let { "  ($it pts)" } ?: ""), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (a.description.isNotBlank() && (got || !a.hidden)) Text(a.description, color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!got && a.hidden) Text(stringResource(R.string.ach_hidden), color = Muted, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            a.unlockedAt?.let { Text(whenText(it), color = Color(0xFF7BD389), fontSize = 12.sp) }
            if (a.manual) Text(stringResource(R.string.ach_manual), color = Muted, fontSize = 11.sp)
            a.percent?.let { Text("%.1f %%".format(it), color = Muted, fontSize = 12.sp) }
        }
        if (manualMode) Checkbox(checked = got, onCheckedChange = { onToggle() })
    }
}
