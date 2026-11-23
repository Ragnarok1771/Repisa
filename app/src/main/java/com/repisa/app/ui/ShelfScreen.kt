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
import androidx.compose.material.icons.filled.Star
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
fun ShelfScreen(vm: LibraryViewModel, onOpenSettings: () -> Unit, onOpenGame: (Game) -> Unit, onOpenRated: () -> Unit = {}) {
    val shelves by vm.shelves.collectAsState()
    val options by vm.options.collectAsState()
    val status by vm.status.collectAsState()
    val focused by vm.focused.collectAsState()
    val all by vm.games.collectAsState()
    val boxHeight = vm.settings.shelfHeightDp.dp

    Column(Modifier.fillMaxSize().roomBackground()) {
        TopBar(vm, options, all, onOpenSettings, onOpenRated)
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
private fun TopBar(vm: LibraryViewModel, options: ViewOptions, all: List<Game>, onOpenSettings: () -> Unit, onOpenRated: () -> Unit) {
    var searching by remember { mutableStateOf(options.query.isNotEmpty()) }
    Column(Modifier.fillMaxWidth().background(Color(0xCC120C09)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Repisa", color = Color(0xFFFFD27A), fontWeight = FontWeight.SemiBold, fontSize = 24.sp,
                fontFamily = DisplayFont)
            Text(" v${com.repisa.app.BuildConfig.VERSION_NAME}", color = Color(0xFF9C8A70), fontSize = 10.sp,
                modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.width(14.dp))
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
            IconButton(onClick = onOpenRated) { Icon(Icons.Default.Star, "Mis calificaciones", tint = Color(0xFFFFC94D)) }
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
                        val w = boxSize(g, boxHeight * g.console.case.heightScale).first + gap
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
        CabinetSides()
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
            Text(group.title, color = Color(0xFF2B1A08), fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = DisplayFont)
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
    Column(
        Modifier.fillMaxWidth()
            .drawBehind {
                // Luz cálida que baja desde la tira LED de la balda de arriba.
                drawRect(Brush.verticalGradient(listOf(Color(0x30FFC680), Color.Transparent), endY = size.height * 0.75f))
            }
            .padding(top = 18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = sidePad),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.Bottom,
        ) {
            games.forEach { g ->
                var isFocused by remember { mutableStateOf(false) }
                // Cada caja con su altura real: Switch más baja que PS2, 3DS más pequeña, Neo Geo enorme...
                GameBox(
                    g, boxHeight * g.console.case.heightScale,
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

/** Balda de madera: superficie, canto con veta, tira de luz LED cálida debajo y su resplandor. */
@Composable
private fun WoodPlank() {
    Canvas(Modifier.fillMaxWidth().height(34.dp)) {
        val top = 9.dp.toPx()
        val front = 15.dp.toPx()
        val w = size.width
        // Superficie (vista desde arriba)
        drawRect(Brush.verticalGradient(listOf(Color(0xFF4A2C16), Color(0xFF8C5A30)), endY = top), size = Size(w, top))
        // Canto frontal
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFFC08A55), Color(0xFF9A673A), Color(0xFF6E4422)), startY = top, endY = top + front),
            topLeft = Offset(0f, top), size = Size(w, front),
        )
        // Veta: líneas finas onduladas
        for (i in 0 until 5) {
            val y0 = top + front * (0.18f + i * 0.16f)
            var x = 0f
            val step = 40f
            while (x < w) {
                val dy = kotlin.math.sin((x + i * 57) / 90f) * 1.2f
                drawLine(Color(0x1F2A1408), Offset(x, y0 + dy), Offset(x + step, y0 + kotlin.math.sin((x + step + i * 57) / 90f) * 1.2f), 1f)
                x += step
            }
        }
        drawLine(Color(0x66FFE2B8), Offset(0f, top), Offset(w, top), strokeWidth = 1.5f)
        // Tira LED bajo la balda y su luz cayendo sobre la pared
        val led = top + front
        drawRect(Color(0xFF3A2414), topLeft = Offset(0f, led), size = Size(w, 2.dp.toPx()))
        drawLine(Color(0xCCFFD9A0), Offset(w * 0.04f, led + 1.dp.toPx()), Offset(w * 0.96f, led + 1.dp.toPx()), strokeWidth = 1.2.dp.toPx())
        drawRect(
            Brush.verticalGradient(listOf(Color(0x55FFC680), Color.Transparent), startY = led + 2.dp.toPx(), endY = size.height),
            topLeft = Offset(0f, led + 2.dp.toPx()), size = Size(w, size.height - led - 2.dp.toPx()),
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
            color = Color(0xFFFFD27A), fontSize = 22.sp, fontFamily = DisplayFont, fontWeight = FontWeight.Bold)
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

/**
 * Fondo de vitrina: panel trasero de tablones verticales de nogal con veta, juntas entre tablones
 * y una luz cálida desde arriba. Se usa en todas las pantallas.
 */
fun Modifier.roomBackground(): Modifier = this
    .background(Brush.verticalGradient(listOf(Color(0xFF3B2516), Color(0xFF2A1A0F), Color(0xFF1B110A))))
    .drawBehind {
        val plank = 132.dp.toPx()
        val h = size.height
        var x = 0f
        var i = 0
        while (x < size.width) {
            // Cada tablón con un tono ligeramente distinto
            val tint = when (i % 3) { 0 -> Color(0x0FFFFFFF); 1 -> Color(0x12000000); else -> Color(0x06FFD0A0) }
            drawRect(tint, topLeft = Offset(x, 0f), size = Size(plank, h))
            // Veta vertical ondulada
            for (g in 0 until 7) {
                val gx = x + plank * (0.1f + g * 0.13f) + ((i * 31 + g * 17) % 9)
                var y = 0f
                val step = 60f
                while (y < h) {
                    val dx1 = kotlin.math.sin((y + g * 40 + i * 70) / 140f) * 3f
                    val dx2 = kotlin.math.sin((y + step + g * 40 + i * 70) / 140f) * 3f
                    drawLine(Color(if (g % 2 == 0) 0x14000000 else 0x0AFFE0B0), Offset(gx + dx1, y), Offset(gx + dx2, y + step), 1.2f)
                    y += step
                }
            }
            // Junta entre tablones
            drawLine(Color(0x66000000), Offset(x, 0f), Offset(x, h), 2.dp.toPx())
            drawLine(Color(0x14FFFFFF), Offset(x + 2.dp.toPx(), 0f), Offset(x + 2.dp.toPx(), h), 1f)
            x += plank; i++
        }
        // Luz cálida desde arriba y viñeta en los bordes
        drawRect(Brush.radialGradient(listOf(Color(0x33FFC67A), Color.Transparent),
            center = Offset(size.width / 2, 0f), radius = size.width * 0.75f))
        drawRect(Brush.horizontalGradient(
            0f to Color(0x66000000), 0.08f to Color.Transparent, 0.92f to Color.Transparent, 1f to Color(0x66000000)))
    }

/** Laterales de madera de la vitrina, fijos a los lados de la pantalla. */
@Composable
fun CabinetSides(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val w = 14.dp.toPx()
        val wood = Brush.horizontalGradient(listOf(Color(0xFF5A3720), Color(0xFF8C5A32), Color(0xFF5A3720)))
        drawRect(wood, size = Size(w, size.height))
        drawRect(Brush.horizontalGradient(listOf(Color(0x66000000), Color.Transparent), startX = w, endX = w + 10.dp.toPx()),
            topLeft = Offset(w, 0f), size = Size(10.dp.toPx(), size.height))
        val rx = size.width - w
        drawRect(Brush.horizontalGradient(listOf(Color(0xFF5A3720), Color(0xFF8C5A32), Color(0xFF5A3720)), startX = rx, endX = size.width),
            topLeft = Offset(rx, 0f), size = Size(w, size.height))
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color(0x66000000)), startX = rx - 10.dp.toPx(), endX = rx),
            topLeft = Offset(rx - 10.dp.toPx(), 0f), size = Size(10.dp.toPx(), size.height))
    }
}
