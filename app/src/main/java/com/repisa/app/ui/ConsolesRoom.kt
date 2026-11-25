package com.repisa.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.R
import com.repisa.app.i18n.L10n
import com.repisa.app.model.Consoles
import com.repisa.app.model.formatPlaytime

/** Una consola de la biblioteca en la sala: cuántos juegos tienes y cuántas horas le has echado. */
private data class OwnedConsole(val id: String, val name: String, val year: Int, val games: Int, val minutes: Int)

/**
 * «Mis consolas»: todas las consolas de las que tienes juegos, dibujadas en baldas como una colección de hardware,
 * con sus juegos y horas. Tocar una (o pulsar A) vuelve a la repisa con solo los juegos de esa consola.
 */
@Composable
fun ConsolesRoom(vm: LibraryViewModel, onClose: () -> Unit, onPick: (String) -> Unit) {
    BackHandler(onBack = onClose)
    val games by vm.displayGames.collectAsState()
    val owned = remember(games) {
        games.groupBy { it.consoleId }.map { (id, list) ->
            val c = Consoles.byId(id)
            OwnedConsole(id, if (id == "pc") "PC" else c.name, c.year, list.size, list.sumOf { it.totalMinutes })
        }.sortedWith(compareBy({ it.year }, { it.name }))
    }
    val first = remember { FocusRequester() }
    LaunchedEffect(owned.isNotEmpty()) { if (owned.isNotEmpty()) runCatching { first.requestFocus() } }

    Box(Modifier.fillMaxSize().roomBackground().blockUnderneath()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(start = 8.dp, top = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White) }
                Column {
                    Text("🎮 " + stringResource(R.string.consoles_title), color = Color(0xFFFFD27A), fontSize = 26.sp,
                        fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
                    Text(stringResource(R.string.consoles_sub, owned.size, L10n.games(games.size)), color = Color(0xFFBFA98A), fontSize = 13.sp)
                }
            }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // Cuántas consolas caben por balda según el ancho (unos 190 dp cada una).
                val perRow = (maxWidth.value / 190f).toInt().coerceIn(3, 7)
                val rows = owned.chunked(perRow)
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                    items(rows.size) { r ->
                        Column {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 0.dp).padding(top = 14.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                                rows[r].forEachIndexed { i, c ->
                                    ConsoleSpot(c, Modifier.weight(1f).then(if (r == 0 && i == 0) Modifier.focusRequester(first) else Modifier)) { onPick(c.id) }
                                }
                                // Huecos vacíos para que la última balda no estire sus consolas.
                                repeat(perRow - rows[r].size) { Box(Modifier.weight(1f)) }
                            }
                            WoodPlank()
                        }
                    }
                }
                CabinetSides()
            }
        }
    }
}

@Composable
private fun ConsoleSpot(c: OwnedConsole, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier.clip(RoundedCornerShape(10.dp))
            .then(if (focused) Modifier.background(Color(0x33FFD27A)).border(2.dp, Color(0xFFFFD27A), RoundedCornerShape(10.dp)) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.height(110.dp), contentAlignment = Alignment.BottomCenter) {
            if (hasConsoleArt(c.id)) ConsoleIcon(c.id, Modifier.height(if (focused) 108.dp else 100.dp).padding(horizontal = 4.dp))
            else Text("🎮", fontSize = 48.sp)
        }
        Text(c.name, color = Color(0xFFFFD27A), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        Text(L10n.games(c.games) + " · " + formatPlaytime(c.minutes), color = Color(0xFFE8D9C0), fontSize = 11.sp, maxLines = 1,
            textAlign = TextAlign.Center)
    }
}
