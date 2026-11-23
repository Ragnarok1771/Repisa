package com.repisa.app.ui

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.model.Game
import com.repisa.app.model.Source
import com.repisa.app.model.formatPlaytime

private sealed interface ShelfItem {
    val key: String
    data class Header(val group: ShelfGroup, override val key: String) : ShelfItem
    data class Row(val games: List<Game>, override val key: String) : ShelfItem
}

@Composable
fun ShelfScreen(vm: LibraryViewModel, onOpenSettings: () -> Unit, onOpenGame: (Game) -> Unit) {
    val shelves by vm.shelves.collectAsState()
    val options by vm.options.collectAsState()
    val status by vm.status.collectAsState()
    val focused by vm.focused.collectAsState()
    val all by vm.games.collectAsState()
    val boxHeight = vm.settings.shelfHeightDp.dp

    Column(Modifier.fillMaxSize().roomBackground()) {
        TopBar(vm, options, all, onOpenSettings)
        status?.let {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(it, color = Color(0xFFE8D5B5), fontSize = 12.sp)
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 2.dp), color = Color(0xFFFFB74D))
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (shelves.isEmpty()) {
                EmptyRoom(hasGames = all.isNotEmpty(), onOpenSettings = onOpenSettings)
            } else {
                Shelves(shelves, boxHeight, focused?.id, onFocus = { vm.focused.value = it }, onOpen = onOpenGame,
                    onPlay = { vm.play(it) }, onFavorite = { vm.toggleFavorite(it) },
                    onAspect = { g, a -> vm.setCoverAspect(g.id, a) })
            }
        }
        FocusBar(focused)
    }
}

@Composable
private fun TopBar(vm: LibraryViewModel, options: ViewOptions, all: List<Game>, onOpenSettings: () -> Unit) {
    var searching by remember { mutableStateOf(options.query.isNotEmpty()) }
    Column(Modifier.fillMaxWidth().background(Color(0xCC120C09)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Repisa", color = Color(0xFFFFD27A), fontWeight = FontWeight.Black, fontSize = 22.sp,
                fontFamily = FontFamily.Serif)
            Spacer(Modifier.width(12.dp))
            Text(
                "${all.size} juegos · ${all.sumOf { it.totalMinutes } / 60} h jugadas",
                color = Color(0xFFBFA98A), fontSize = 12.sp,
            )
            Spacer(Modifier.weight(1f))
            if (searching) {
                BasicTextField(
                    value = options.query, onValueChange = vm::setQuery, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color(0xFFFFD27A)),
                    decorationBox = { inner ->
                        Box(Modifier.width(200.dp).background(Color(0x33FFFFFF), RoundedCornerShape(8.dp)).padding(8.dp)) {
                            if (options.query.isEmpty()) Text("Buscar título, género, estudio…", color = Color(0x88FFFFFF), fontSize = 14.sp)
                            inner()
                        }
                    },
                )
                IconButton(onClick = { vm.setQuery(""); searching = false }) { Icon(Icons.Default.Clear, "Cerrar búsqueda", tint = Color.White) }
            } else {
                IconButton(onClick = { searching = true }) { Icon(Icons.Default.Search, "Buscar", tint = Color.White) }
            }
            IconButton(onClick = { vm.syncAll() }) { Icon(Icons.Default.Refresh, "Sincronizar", tint = Color.White) }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Ajustes", tint = Color.White) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            item { SourceChip("Todo", options.source == null) { vm.setSource(null) } }
            items(Source.entries) { s ->
                val count = all.count { it.source == s }
                if (count > 0) SourceChip("${s.label} ($count)", options.source == s) { vm.setSource(s) }
            }
            item { Spacer(Modifier.width(8.dp)) }
            item { Picker("Ordenar", options.sort.label, SortMode.entries.map { it.label }) { vm.setSort(SortMode.entries[it]) } }
            item { Picker("Repisas por", options.group.label, GroupMode.entries.map { it.label }) { vm.setGroup(GroupMode.entries[it]) } }
        }
    }
}

@Composable
private fun SourceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, fontSize = 12.sp) })
}

@Composable
private fun Picker(title: String, value: String, choices: List<String>, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text("$title: ", color = Color(0xFFBFA98A), fontSize = 12.sp)
            Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            choices.forEachIndexed { i, c ->
                DropdownMenuItem(text = { Text(c) }, onClick = { open = false; onPick(i) })
            }
        }
    }
}

@Composable
private fun Shelves(
    shelves: List<ShelfGroup>,
    boxHeight: Dp,
    focusedId: String?,
    onFocus: (Game) -> Unit,
    onOpen: (Game) -> Unit,
    onPlay: (Game) -> Unit,
    onFavorite: (Game) -> Unit,
    onAspect: (Game, Float) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sidePad = 28.dp
        val gap = 14.dp
        val usable = maxWidth - sidePad * 2
        // Reparte las cajas en baldas según el ancho real de cada una.
        val items = remember(shelves, usable, boxHeight) {
            buildList<ShelfItem> {
                shelves.forEachIndexed { gi, group ->
                    if (shelves.size > 1 || group.title != "Mi colección") add(ShelfItem.Header(group, "h$gi:${group.title}"))
                    var row = mutableListOf<Game>()
                    var used = 0.dp
                    group.games.forEach { g ->
                        val w = boxSize(g, boxHeight).first + gap
                        if (row.isNotEmpty() && used + w > usable) {
                            add(ShelfItem.Row(row, "r$gi:${size}")); row = mutableListOf(); used = 0.dp
                        }
                        row += g; used += w
                    }
                    if (row.isNotEmpty()) add(ShelfItem.Row(row, "r$gi:${size}"))
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(items, key = { it.key }) { item ->
                when (item) {
                    is ShelfItem.Header -> GroupPlaque(item.group)
                    is ShelfItem.Row -> ShelfRow(item.games, boxHeight, sidePad, gap, focusedId, onFocus, onOpen, onPlay, onFavorite, onAspect)
                }
            }
        }
    }
}

@Composable
private fun GroupPlaque(group: ShelfGroup) {
    Row(Modifier.fillMaxWidth().padding(start = 28.dp, top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .background(Brush.verticalGradient(listOf(Color(0xFFE9C46A), Color(0xFFB08436))), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF6E4F1E), RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 3.dp),
        ) {
            Text(group.title, color = Color(0xFF2B1A08), fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Serif)
        }
        Spacer(Modifier.width(10.dp))
        Text(group.subtitle, color = Color(0xFFBFA98A), fontSize = 11.sp)
    }
}

@Composable
private fun ShelfRow(
    games: List<Game>,
    boxHeight: Dp,
    sidePad: Dp,
    gap: Dp,
    focusedId: String?,
    onFocus: (Game) -> Unit,
    onOpen: (Game) -> Unit,
    onPlay: (Game) -> Unit,
    onFavorite: (Game) -> Unit,
    onAspect: (Game, Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = sidePad),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.Bottom,
        ) {
            games.forEach { g ->
                var isFocused by remember { mutableStateOf(false) }
                GameBox(
                    g, boxHeight,
                    lifted = isFocused,
                    onCoverAspect = { onAspect(g, it) },
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.isFocused; if (it.isFocused) onFocus(g) }
                        .onKeyEvent { e ->
                            if (e.type != KeyEventType.KeyUp) return@onKeyEvent false
                            when (e.key.nativeKeyCode) {
                                KeyEvent.KEYCODE_BUTTON_A -> { onOpen(g); true }
                                KeyEvent.KEYCODE_BUTTON_X -> { onPlay(g); true }
                                KeyEvent.KEYCODE_BUTTON_Y -> { onFavorite(g); true }
                                else -> false
                            }
                        }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            onFocus(g); onOpen(g)
                        },
                )
            }
        }
        WoodPlank()
    }
}

/** Balda de madera con canto, veta y sombra. */
@Composable
private fun WoodPlank() {
    Canvas(Modifier.fillMaxWidth().height(30.dp)) {
        val top = 10.dp.toPx()
        val front = 14.dp.toPx()
        // superficie (vista desde arriba, más clara)
        drawRect(Brush.verticalGradient(listOf(Color(0xFF6B4423), Color(0xFF9A6A3C)), endY = top), size = Size(size.width, top))
        // canto frontal
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFFB98552), Color(0xFF7A4E2A), Color(0xFF5A3618)), startY = top, endY = top + front),
            topLeft = Offset(0f, top), size = Size(size.width, front),
        )
        // vetas
        val grain = Color(0x22000000)
        for (i in 0 until 6) {
            val y = top + front * (0.2f + i * 0.13f)
            drawLine(grain, Offset(0f, y), Offset(size.width, y + (if (i % 2 == 0) 1.5f else -1.5f)), strokeWidth = 1f)
        }
        drawLine(Color(0x55FFFFFF), Offset(0f, top), Offset(size.width, top), strokeWidth = 1.5f)
        // sombra bajo la balda
        drawRect(
            Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent), startY = top + front, endY = size.height),
            topLeft = Offset(0f, top + front), size = Size(size.width, size.height - top - front),
        )
    }
}

@Composable
private fun FocusBar(game: Game?) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xE6120C09)).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (game == null) {
            Text("Toca una caja o muévete con el mando · A: ver · X: jugar · Y: favorito · Select: cambiar repisas",
                color = Color(0xFF9C8A70), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else {
            Text((if (game.favorite) "★ " else "") + game.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Text(
                "   ${game.console.name} · ${formatPlaytime(game.totalMinutes)}" +
                    (game.genres.firstOrNull()?.let { " · $it" } ?: "") + (game.developer?.let { " · $it" } ?: ""),
                color = Color(0xFFBFA98A), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyRoom(hasGames: Boolean, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (hasGames) "Ninguna caja coincide con el filtro" else "Tu repisa está vacía",
            color = Color(0xFFFFD27A), fontSize = 22.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (!hasGames) {
            Text("Conecta Steam, Epic Games o elige tus carpetas de ROMs, ES-DE o RetroArch para llenarla.",
                color = Color(0xFFBFA98A), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onOpenSettings) { Text("Abrir ajustes", color = Color.White) }
        }
        WoodPlank()
    }
}

/** Pared de la habitación: papel pintado a rayas con luz cálida desde arriba. */
fun Modifier.roomBackground(): Modifier = this
    .background(Brush.verticalGradient(listOf(Color(0xFF3A2A20), Color(0xFF241812), Color(0xFF16100C))))
    .drawBehind {
        val stripe = 22.dp.toPx()
        var x = 0f
        var i = 0
        while (x < size.width) {
            if (i % 2 == 0) drawRect(Color(0x0DFFFFFF), topLeft = Offset(x, 0f), size = Size(stripe, size.height))
            x += stripe; i++
        }
        drawRect(
            Brush.radialGradient(listOf(Color(0x30FFC67A), Color.Transparent), center = Offset(size.width / 2, 0f),
                radius = size.width * 0.7f),
        )
    }
