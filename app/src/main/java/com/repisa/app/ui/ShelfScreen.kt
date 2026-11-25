package com.repisa.app.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.fillMaxHeight
import kotlinx.coroutines.launch
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.R
import com.repisa.app.i18n.L10n
import com.repisa.app.model.Game
import com.repisa.app.model.label
import com.repisa.app.model.Source
import com.repisa.app.model.formatPlaytime

/** Al salir el foco de una carátula, la pantalla de abajo vuelve al resumen (ver LibraryViewModel.blurGame). */
private var LocalBlur: ((Game) -> Unit)? = null

private sealed interface ShelfItem {
    val key: String
    data class Header(val group: ShelfGroup, override val key: String) : ShelfItem
    /** [groupKey] solo en la primera balda de cada repisa: ahí baja el foco desde su placa. */
    data class Row(val games: List<Game>, override val key: String, val groupKey: String? = null) : ShelfItem
}

@Composable
fun ShelfScreen(vm: LibraryViewModel, onOpenSettings: () -> Unit, onOpenGame: (Game) -> Unit, onOpenRated: () -> Unit = {},
                onOpenRandom: () -> Unit = {}, onOpenStats: () -> Unit = {}, onOpenAchievements: () -> Unit = {}) {
    val shelves by vm.shelves.collectAsState()
    val options by vm.options.collectAsState()
    val status by vm.status.collectAsState()
    val focused by vm.focused.collectAsState()
    val all by vm.displayGames.collectAsState()
    val boxHeight = vm.settings.shelfHeightDp.dp

    Column(Modifier.fillMaxSize().roomBackground()) {
        TopBar(vm, options, all, onOpenSettings, onOpenRated, onOpenRandom, onOpenStats, onOpenAchievements)
        val selecting by vm.selecting.collectAsState()
        val selection by vm.selection.collectAsState()
        if (selecting) SelectionBar(vm, selection, shelves.flatMap { it.games }.distinctBy { it.id })
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
                val selState = SelectionState(selecting, selection, toggle = { vm.toggleSelected(it) }, start = { vm.startSelection(it) })
                androidx.compose.runtime.CompositionLocalProvider(LocalSelection provides selState) {
                val collapsed by vm.collapsed.collectAsState()
                LocalBlur = { vm.blurGame(it) }
                val prices by vm.wishPrices.collectAsState()
                // Cajas de la lista de deseos: abren la tienda de Steam en vez de la ficha.
                val openStore: (Game) -> Unit = { g -> g.storeUrl?.let { vm.launcher.openSteamStore(it) } }
                val platinums by vm.platinums.collectAsState()
                androidx.compose.runtime.CompositionLocalProvider(LocalPriceTags provides prices, LocalPlatinums provides platinums.keys) {
                Shelves(shelves, collapsed, { vm.toggleCollapsed(it) }, boxHeight, focused?.id, onFocus = { vm.focusGame(it) },
                    onOpen = { if (it.isWish) openStore(it) else onOpenGame(it) },
                    onPlay = { if (it.isWish) openStore(it) else vm.play(it) },
                    onFavorite = { if (!it.isWish) vm.toggleFavorite(it) },
                    onAspect = { g, a -> if (!g.isWish) vm.setCoverAspect(g.id, a) },
                    plaqueExtra = { gr -> if (gr.key == "wishlist") WishlistControls(vm) })
                }
                }
            }
        }
        FocusBar(focused)
    }
}

@Composable
private fun TopBar(vm: LibraryViewModel, options: ViewOptions, all: List<Game>, onOpenSettings: () -> Unit, onOpenRated: () -> Unit,
                   onOpenRandom: () -> Unit, onOpenStats: () -> Unit, onOpenAchievements: () -> Unit) {
    var searching by remember { mutableStateOf(options.query.isNotEmpty()) }
    Column(Modifier.fillMaxWidth().background(Color(0xCC120C09)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        // Fila 1: nombre y versión a la izquierda, juegos y horas en el centro, botones a la derecha.
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                Text("Repisa", color = Color(0xFFFFD27A), fontWeight = FontWeight.SemiBold, fontSize = 24.sp,
                    fontFamily = DisplayFont)
                Text(" v${com.repisa.app.BuildConfig.VERSION_NAME}", color = Color(0xFF9C8A70), fontSize = 10.sp,
                    modifier = Modifier.padding(top = 8.dp))
            }
            // 🏆 abre los logros; tocar los totales abre las estadísticas.
            if (!searching) Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                Text("🏆", fontSize = 16.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenAchievements)
                    .padding(horizontal = 10.dp, vertical = 6.dp))
                Text(
                    L10n.games(all.size) + " · " + stringResource(R.string.hours_played, all.sumOf { it.totalMinutes } / 60) + "  📊",
                    color = Color(0xFFBFA98A), fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenStats).padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                if (searching) {
                    BasicTextField(
                        value = options.query, onValueChange = vm::setQuery, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(Color(0xFFFFD27A)),
                        decorationBox = { inner ->
                            Box(Modifier.width(260.dp).background(Color(0x33FFFFFF), RoundedCornerShape(8.dp)).padding(8.dp)) {
                                if (options.query.isEmpty()) Text(stringResource(R.string.search_hint), color = Color(0x88FFFFFF), fontSize = 14.sp)
                                inner()
                            }
                        },
                    )
                    TopIcon(Icons.Default.Clear, stringResource(R.string.close_search)) { vm.setQuery(""); searching = false }
                } else {
                    TopIcon(Icons.Default.Search, stringResource(R.string.search)) { searching = true }
                    TopTextIcon("🎲", stringResource(R.string.random_title), onClick = onOpenRandom)
                }
                TopIcon(Icons.Default.Star, stringResource(R.string.my_ratings), Color(0xFFFFC94D), onClick = onOpenRated)
                TopIcon(Icons.Default.Refresh, stringResource(R.string.sync)) { vm.syncAll() }
                TopIcon(Icons.Default.Settings, stringResource(R.string.settings), onClick = onOpenSettings)
            }
        }
        // Fila 2: filtros a la izquierda; ordenar y repisas al fondo a la derecha.
        Row(verticalAlignment = Alignment.CenterVertically) {
            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Filtro por consola (desde «Mis consolas»): se quita tocándolo.
                options.console?.let { id ->
                    item { SourceChip("🎮 " + com.repisa.app.model.Consoles.byId(id).name + "  ✕", true) { vm.closeConsoleFilter() } }
                }
                item { SourceChip(stringResource(R.string.filter_all), options.source == null) { vm.setSource(null) } }
                items(Source.entries) { s ->
                    val count = all.count { it.source == s || s in it.groupSources }
                    if (count > 0) SourceChip("${s.label} ($count)", options.source == s) { vm.setSource(s) }
                }
            }
            Picker(stringResource(R.string.sort_label), options.sort.label, SortMode.entries.map { it.label }) { vm.setSort(SortMode.entries[it]) }
            Picker(stringResource(R.string.group_label), options.group.label, GroupMode.entries.map { it.label }) { vm.setGroup(GroupMode.entries[it]) }
        }
    }
}

/**
 * Icono de la barra superior. Con el mando, el que tiene el foco se ve claramente: círculo dorado con borde
 * y el icono en oscuro (antes el resaltado por defecto de Material era casi invisible sobre la madera).
 */
@Composable
private fun TopIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .then(if (focused) Modifier.background(Color(0xFFFFD27A), androidx.compose.foundation.shape.CircleShape)
                .border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape) else Modifier),
    ) {
        Icon(icon, label, tint = if (focused) Color(0xFF2A1C14) else tint)
    }
}

/** Como [TopIcon] pero con un emoji (el dado). */
@Composable
private fun TopTextIcon(text: String, label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label }
            .then(if (focused) Modifier.background(Color(0xFFFFD27A), androidx.compose.foundation.shape.CircleShape)
                .border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape) else Modifier),
    ) { Text(text, fontSize = 20.sp) }
}

/** Selección de varias carátulas, compartida con las cajas de las repisas. */
class SelectionState(val active: Boolean, val ids: Set<String>, val toggle: (Game) -> Unit, val start: (Game) -> Unit)

val LocalSelection = androidx.compose.runtime.staticCompositionLocalOf { SelectionState(false, emptySet(), {}, {}) }

@Composable
private fun SourceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    // Con el mando, el chip enfocado se aclara (dorado) y lleva borde: antes solo se oscurecía un poco.
    var focused by remember { mutableStateOf(false) }
    FilterChip(
        selected = selected, onClick = onClick,
        label = { Text(label, fontSize = 12.sp, color = if (focused) Color(0xFF2A1C14) else Color.Unspecified) },
        modifier = Modifier.onFocusChanged { focused = it.isFocused },
        colors = if (focused) androidx.compose.material3.FilterChipDefaults.filterChipColors(
            containerColor = Color(0xFFFFD27A), selectedContainerColor = Color(0xFFFFE3A8),
        ) else androidx.compose.material3.FilterChipDefaults.filterChipColors(),
        border = if (focused) androidx.compose.foundation.BorderStroke(2.dp, Color.White)
            else androidx.compose.material3.FilterChipDefaults.filterChipBorder(enabled = true, selected = selected),
    )
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
internal fun Shelves(
    shelves: List<ShelfGroup>,
    collapsed: Set<String>,
    onToggle: (String) -> Unit,
    boxHeight: Dp,
    focusedId: String?,
    onFocus: (Game) -> Unit,
    onOpen: (Game) -> Unit,
    onPlay: (Game) -> Unit,
    onFavorite: (Game) -> Unit,
    onAspect: (Game, Float) -> Unit,
    plaqueExtra: @Composable (ShelfGroup) -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sidePad = 28.dp
        val gap = 14.dp
        val usable = maxWidth - sidePad * 2
        // Reparte las cajas en baldas según el ancho real de cada una.
        val items = remember(shelves, collapsed, usable, boxHeight) {
            buildList<ShelfItem> {
                shelves.forEachIndexed { gi, group ->
                    val alone = shelves.size == 1 && group.title == L10n.s(R.string.shelf_my_collection)
                    if (!alone) add(ShelfItem.Header(group, "h$gi:${group.title}"))
                    // Plegada: solo la placa (una repisa sola no se puede plegar, se quedaría la sala vacía).
                    if (!alone && group.key in collapsed) return@forEachIndexed
                    var row = mutableListOf<Game>()
                    var used = 0.dp
                    var firstRow = true
                    group.games.forEach { g ->
                        val w = boxSize(g, boxHeight * g.console.case.heightScale).first + gap
                        if (row.isNotEmpty() && used + w > usable) {
                            add(ShelfItem.Row(row, "r$gi:${size}", group.key.takeIf { firstRow })); row = mutableListOf(); used = 0.dp
                            firstRow = false
                        }
                        row += g; used += w
                    }
                    if (row.isNotEmpty()) add(ShelfItem.Row(row, "r$gi:${size}", group.key.takeIf { firstRow }))
                }
            }
        }
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        // Primer juego de cada repisa: desde la placa, abajo lleva a él (no al que quede más centrado bajo la placa).
        val firstGame = remember { mutableMapOf<String, FocusRequester>() }
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
            items(items, key = { it.key }) { item ->
                when (item) {
                    is ShelfItem.Header -> GroupPlaque(item.group, item.group.key in collapsed, { plaqueExtra(item.group) },
                        downTo = if (item.group.key in collapsed) null else firstGame.getOrPut(item.group.key) { FocusRequester() }) { onToggle(item.group.key) }
                    is ShelfItem.Row -> ShelfRow(item.games, boxHeight, sidePad, gap, focusedId, onFocus, onOpen, onPlay, onFavorite, onAspect,
                        firstFocus = item.groupKey?.let { firstGame.getOrPut(it) { FocusRequester() } })
                }
            }
        }
        CabinetSides()
        // Barra para subir y bajar rápido: con el nombre de la repisa por la que vas mientras se arrastra.
        FastScrollbar(listState, Modifier.align(Alignment.CenterEnd)) { index ->
            items.subList(0, (index + 1).coerceIn(1, items.size)).lastOrNull { it is ShelfItem.Header }
                ?.let { (it as ShelfItem.Header).group.title }
        }
    }
}

/**
 * Barra de desplazamiento que se puede arrastrar (o tocar para saltar), en el lateral derecho del mueble.
 * [label] da el texto de la burbuja para el elemento al que se llega (el nombre de la repisa).
 */
@Composable
private fun FastScrollbar(state: androidx.compose.foundation.lazy.LazyListState, modifier: Modifier, label: (Int) -> String?) {
    val total = state.layoutInfo.totalItemsCount
    val visible = state.layoutInfo.visibleItemsInfo.size
    if (total == 0 || (!state.canScrollForward && !state.canScrollBackward)) return
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }
    // Posición actual (0..1): elemento de arriba + lo que se ha desplazado dentro de él.
    val first = state.layoutInfo.visibleItemsInfo.firstOrNull()
    val within = if (first != null && first.size > 0) state.firstVisibleItemScrollOffset.toFloat() / first.size else 0f
    val scrollable = (total - visible).coerceAtLeast(1)
    val fraction = if (dragging) dragFraction
        else if (!state.canScrollForward) 1f else ((state.firstVisibleItemIndex + within) / scrollable).coerceIn(0f, 1f)
    val active = dragging || state.isScrollInProgress
    val alpha by androidx.compose.animation.core.animateFloatAsState(if (active) 1f else 0.8f, label = "barra")
    fun jump(y: Float, height: Float, thumb: Float) {
        dragFraction = ((y - thumb / 2) / (height - thumb).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val target = (dragFraction * scrollable).toInt().coerceIn(0, total - 1)
        scope.launch { state.scrollToItem(target) }
    }
    BoxWithConstraints(modifier.fillMaxHeight().width(34.dp).padding(vertical = 6.dp)) {
        val height = constraints.maxHeight.toFloat()
        val density = androidx.compose.ui.platform.LocalDensity.current
        // Tirador proporcional a lo que se ve, pero nunca más pequeño que un dedo.
        val thumbPx = maxOf(with(density) { 52.dp.toPx() }, height * visible / total).coerceAtMost(height * 0.5f)
        val top = (height - thumbPx) * fraction
        Box(
            Modifier.fillMaxSize()
                .pointerInput(total) {
                    detectTapGestures { jump(it.y, height, thumbPx) }
                }
                .pointerInput(total) {
                    detectVerticalDragGestures(
                        onDragStart = { dragging = true; jump(it.y, height, thumbPx) },
                        onDragEnd = { dragging = false }, onDragCancel = { dragging = false },
                    ) { change, _ -> change.consume(); jump(change.position.y, height, thumbPx) }
                },
        ) {
            // Carril oscuro sobre el lateral del mueble, para que se vea que la barra está ahí.
            Box(Modifier.align(Alignment.TopEnd).padding(end = 4.dp).width(8.dp).fillMaxHeight()
                .background(Color(0x55000000), RoundedCornerShape(4.dp)))
            Box(
                Modifier.align(Alignment.TopEnd).offset { androidx.compose.ui.unit.IntOffset(0, top.toInt()) }
                    .padding(end = if (active) 1.dp else 2.dp).width(if (active) 14.dp else 12.dp).height(with(density) { thumbPx.toDp() })
                    .graphicsLayer { this.alpha = alpha }
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFE2A0), Color(0xFFC99A4A))), RoundedCornerShape(7.dp))
                    .border(1.dp, Color(0x88000000), RoundedCornerShape(7.dp)),
            )
        }
        // Burbuja con la repisa a la que se llega, a la izquierda del tirador.
        if (dragging) label((dragFraction * scrollable).toInt().coerceIn(0, total - 1))?.let { text ->
            Box(
                Modifier.align(Alignment.TopEnd)
                    .offset { androidx.compose.ui.unit.IntOffset(-with(density) { 38.dp.roundToPx() }, (top + thumbPx / 2 - with(density) { 20.dp.toPx() }).toInt()) }
                    .wrapContentWidth(unbounded = true, align = Alignment.End)
                    .background(Color(0xF02A1C14), RoundedCornerShape(10.dp)).border(1.dp, Color(0xFFFFD27A), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) { Text(text, color = Color(0xFFFFD27A), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, fontFamily = DisplayFont) }
        }
    }
}

@Composable
private fun GroupPlaque(group: ShelfGroup, isCollapsed: Boolean, extra: @Composable () -> Unit = {}, downTo: FocusRequester? = null, onToggle: () -> Unit) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val focusedPlaque by interaction.collectIsFocusedAsState()
    Row(Modifier.fillMaxWidth().padding(start = 28.dp, top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        // Repisas por consola: la consola dibujada en pequeño junto a su placa.
        group.consoleId?.let { id -> if (hasConsoleArt(id)) ConsoleIconBox(id, 46.dp, 26.dp, Modifier.padding(end = 8.dp)) }
        // La placa se toca (o se pulsa con el mando) para plegar o desplegar la repisa.
        Box(
            Modifier
                .clip(RoundedCornerShape(4.dp))
                // Abajo con el mando: al primer juego de la repisa. Si no está en pantalla, el movimiento normal.
                .onPreviewKeyEvent { e ->
                    downTo != null && e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown &&
                        runCatching { downTo.requestFocus() }.isSuccess
                }
                .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
                .background(Brush.verticalGradient(ShelfTheme.current.plaque), RoundedCornerShape(4.dp))
                .border(if (focusedPlaque) 3.dp else 1.dp, if (focusedPlaque) Color.White else ShelfTheme.current.plaqueBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 3.dp),
        ) {
            Text((if (isCollapsed) "▸ " else "▾ ") + group.title, color = ShelfTheme.current.plaqueText, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = DisplayFont)
        }
        Spacer(Modifier.width(10.dp))
        Text(group.subtitle, color = Color(0xFFBFA98A), fontSize = 11.sp)
        extra()
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
    firstFocus: FocusRequester? = null,
) {
    Column(
        Modifier.fillMaxWidth()
            .drawBehind {
                // Luz cálida que baja desde la tira LED de la balda de arriba.
                drawRect(Brush.verticalGradient(listOf(ShelfTheme.current.ledGlow.copy(alpha = ShelfTheme.current.ledGlow.alpha * 0.55f), Color.Transparent), endY = size.height * 0.75f))
            }
            .padding(top = 18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = sidePad),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.Bottom,
        ) {
            val sel = LocalSelection.current
            games.forEach { g ->
                var isFocused by remember { mutableStateOf(false) }
                val selected = sel.active && g.id in sel.ids
                // Cada caja con su altura real: Switch más baja que PS2, 3DS más pequeña, Neo Geo enorme...
                Box {
                    GameBox(
                        g, boxHeight * g.console.case.heightScale,
                        showStatus = !g.isWish && LocalShowStatus.current,
                        overlay = run {
                            val price = LocalPriceTags.current[g.id]
                            val platinum = g.id in LocalPlatinums.current
                            if (price == null && !platinum) null else {
                                {
                                    price?.let { PriceBand(it, Modifier.align(Alignment.BottomCenter)) }
                                    // Platino: diamante en la esquina de arriba a la derecha de la carátula.
                                    if (platinum) Text("💎", fontSize = 16.sp, modifier = Modifier.align(Alignment.TopEnd).padding(3.dp)
                                        .background(Color(0x99000000), androidx.compose.foundation.shape.CircleShape).padding(3.dp))
                                }
                            }
                        },
                        lifted = isFocused || selected,
                        onCoverAspect = { onAspect(g, it) },
                        modifier = Modifier
                            .then(if (firstFocus != null && g === games.first()) Modifier.focusRequester(firstFocus) else Modifier)
                            .onFocusChanged { f ->
                                if (f.isFocused) onFocus(g) else if (isFocused) LocalBlur?.invoke(g)
                                isFocused = f.isFocused
                            }
                            .onKeyEvent { e ->
                                if (e.type != KeyEventType.KeyUp) return@onKeyEvent false
                                when (e.key.nativeKeyCode) {
                                    KeyEvent.KEYCODE_BUTTON_A -> { if (sel.active) sel.toggle(g) else onOpen(g); true }
                                    KeyEvent.KEYCODE_BUTTON_X -> { if (!sel.active) onPlay(g); true }
                                    KeyEvent.KEYCODE_BUTTON_Y -> { if (sel.active) sel.toggle(g) else onFavorite(g); true }
                                    else -> false
                                }
                            }
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() }, indication = null,
                                // Mantener pulsada una caja empieza la selección de varias.
                                onLongClick = { if (g.isWish) Unit else if (!sel.active) sel.start(g) else sel.toggle(g) },
                            ) { onFocus(g); if (sel.active) sel.toggle(g) else onOpen(g) },
                    )
                    if (selected) Text(
                        "✓", color = Color(0xFF2A1C14), fontSize = 16.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 4.dp)
                            .background(Color(0xFFFFD27A), androidx.compose.foundation.shape.CircleShape).padding(horizontal = 7.dp, vertical = 1.dp),
                    )
                }
            }
        }
        WoodPlank()
    }
}

/** Balda: superficie, canto con veta, tira de luz LED debajo y su resplandor. Colores del tema de vitrina. */
@Composable
fun WoodPlank(theme: ShelfTheme = ShelfTheme.current, modifier: Modifier = Modifier.fillMaxWidth().height(34.dp)) {
    Canvas(modifier) {
        val top = size.height * (9f / 34f)
        val front = size.height * (15f / 34f)
        val w = size.width
        // Superficie (vista desde arriba)
        drawRect(Brush.verticalGradient(theme.plankTop, endY = top), size = Size(w, top))
        // Canto frontal
        drawRect(
            Brush.verticalGradient(theme.plankFront, startY = top, endY = top + front),
            topLeft = Offset(0f, top), size = Size(w, front),
        )
        // Veta: líneas finas onduladas (el metal lleva rayado recto; el cristal, nada)
        if (theme.plankGrain.alpha > 0f) for (i in 0 until 5) {
            val y0 = top + front * (0.18f + i * 0.16f)
            var x = 0f
            val step = 40f
            val wave = if (theme.panel == PanelStyle.BRUSHED) 0f else 1.2f
            while (x < w) {
                val dy = kotlin.math.sin((x + i * 57) / 90f) * wave
                drawLine(theme.plankGrain, Offset(x, y0 + dy), Offset(x + step, y0 + kotlin.math.sin((x + step + i * 57) / 90f) * wave), 1f)
                x += step
            }
        }
        // Cristal: un reflejo en diagonal sobre el canto.
        if (theme.panel == PanelStyle.GLASS) {
            var x = w * 0.05f
            while (x < w) {
                drawLine(Color(0x55FFFFFF), Offset(x, top + front), Offset(x + front * 0.8f, top), 2f)
                x += w * 0.23f
            }
        }
        drawLine(theme.plankEdge, Offset(0f, top), Offset(w, top), strokeWidth = 1.5f)
        // Tira LED bajo la balda y su luz cayendo sobre la pared
        val led = top + front
        val ledH = size.height * (2f / 34f)
        drawRect(theme.ledBase, topLeft = Offset(0f, led), size = Size(w, ledH))
        drawLine(theme.led, Offset(w * 0.04f, led + ledH / 2), Offset(w * 0.96f, led + ledH / 2), strokeWidth = ledH * 0.6f)
        drawRect(
            Brush.verticalGradient(listOf(theme.ledGlow, Color.Transparent), startY = led + ledH, endY = size.height),
            topLeft = Offset(0f, led + ledH), size = Size(w, size.height - led - ledH),
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
            Text(stringResource(R.string.hint_controls),
                color = Color(0xFF9C8A70), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else {
            Text((if (game.favorite) "★ " else "") + game.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Text(
                "   ${game.console.label} · ${formatPlaytime(game.totalMinutes)}" +
                    (game.genres.firstOrNull()?.let { " · " + L10n.genre(it) } ?: "") + (game.developer?.let { " · $it" } ?: ""),
                color = Color(0xFFBFA98A), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyRoom(hasGames: Boolean, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(if (hasGames) R.string.empty_filtered else R.string.empty_title),
            color = Color(0xFFFFD27A), fontSize = 22.sp, fontFamily = DisplayFont, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (!hasGames) {
            Text(stringResource(R.string.empty_body),
                color = Color(0xFFBFA98A), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.open_settings), color = Color.White) }
        }
        WoodPlank()
    }
}

/**
 * Fondo de vitrina con el tema elegido: tablones de madera con veta, chapa de metal cepillado,
 * cristal con reflejos o el panel perforado de una tienda retro; con luz desde arriba y viñeta.
 * Se usa en todas las pantallas.
 */
fun Modifier.roomBackground(theme: ShelfTheme = ShelfTheme.current): Modifier = this
    .background(Brush.verticalGradient(theme.wall))
    .drawBehind { drawRoom(theme) }

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoom(theme: ShelfTheme) {
    val h = size.height
    val unit = 1.dp.toPx()
    when (theme.panel) {
        PanelStyle.PLANKS -> {
            val plank = 132 * unit
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
                        drawLine(if (g % 2 == 0) theme.grainDark else theme.grainLight, Offset(gx + dx1, y), Offset(gx + dx2, y + step), 1.2f)
                        y += step
                    }
                }
                // Junta entre tablones
                drawLine(theme.joint, Offset(x, 0f), Offset(x, h), 2 * unit)
                drawLine(Color(0x14FFFFFF), Offset(x + 2 * unit, 0f), Offset(x + 2 * unit, h), 1f)
                x += plank; i++
            }
        }
        PanelStyle.BRUSHED -> {
            // Chapa cepillada: rayas horizontales finas y paneles con remaches.
            var y = 0f
            var k = 0
            while (y < h) {
                drawLine(if (k % 3 == 0) theme.grainLight else theme.grainDark, Offset(0f, y), Offset(size.width, y), 1f)
                y += 3 * unit + (k * 7 % 5); k++
            }
            val panel = 220 * unit
            var x = 0f
            while (x < size.width) {
                drawLine(theme.joint, Offset(x, 0f), Offset(x, h), 2 * unit)
                drawLine(Color(0x22FFFFFF), Offset(x + 2 * unit, 0f), Offset(x + 2 * unit, h), 1f)
                var ry = 40 * unit
                while (ry < h) {
                    drawCircle(Color(0x55000000), 3 * unit, Offset(x + 12 * unit, ry + unit))
                    drawCircle(Color(0x66D8DEE4), 2.2f * unit, Offset(x + 12 * unit, ry))
                    ry += 160 * unit
                }
                x += panel
            }
        }
        PanelStyle.GLASS -> {
            // Cristal oscuro con reflejos en diagonal.
            var x = -h
            var k = 0
            while (x < size.width) {
                val wdt = if (k % 3 == 0) 26 * unit else 8 * unit
                drawLine(Color(if (k % 3 == 0) 0x0CFFFFFF else 0x08BFE6FF), Offset(x, h), Offset(x + h * 0.6f, 0f), wdt)
                x += 170 * unit; k++
            }
            val panel = 300 * unit
            var px = 0f
            while (px < size.width) { drawLine(theme.joint, Offset(px, 0f), Offset(px, h), 1.5f * unit); px += panel }
        }
        PanelStyle.PEGBOARD -> {
            // Panel perforado de tienda de videojuegos de los 90.
            val step = 22 * unit
            var y = step / 2
            while (y < h) {
                var x = step / 2
                while (x < size.width) {
                    drawCircle(theme.grainDark, 2.6f * unit, Offset(x, y))
                    drawCircle(theme.grainLight, 2.6f * unit, Offset(x + 0.8f * unit, y + 0.8f * unit))
                    x += step
                }
                y += step
            }
        }
    }
    // Luz desde arriba y viñeta en los bordes
    drawRect(Brush.radialGradient(listOf(theme.topLight, Color.Transparent),
        center = Offset(size.width / 2, 0f), radius = size.width * 0.75f))
    drawRect(Brush.horizontalGradient(
        0f to Color(0x66000000), 0.08f to Color.Transparent, 0.92f to Color.Transparent, 1f to Color(0x66000000)))
}

/** Laterales del mueble (con los colores del tema), fijos a los lados de la pantalla. */
@Composable
fun CabinetSides(modifier: Modifier = Modifier, theme: ShelfTheme = ShelfTheme.current) {
    Canvas(modifier.fillMaxSize()) {
        val w = 14.dp.toPx()
        val wood = Brush.horizontalGradient(listOf(theme.sideOuter, theme.sideInner, theme.sideOuter), endX = w)
        drawRect(wood, size = Size(w, size.height))
        drawRect(Brush.horizontalGradient(listOf(Color(0x66000000), Color.Transparent), startX = w, endX = w + 10.dp.toPx()),
            topLeft = Offset(w, 0f), size = Size(10.dp.toPx(), size.height))
        val rx = size.width - w
        drawRect(Brush.horizontalGradient(listOf(theme.sideOuter, theme.sideInner, theme.sideOuter), startX = rx, endX = size.width),
            topLeft = Offset(rx, 0f), size = Size(w, size.height))
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color(0x66000000)), startX = rx - 10.dp.toPx(), endX = rx),
            topLeft = Offset(rx - 10.dp.toPx(), 0f), size = Size(10.dp.toPx(), size.height))
    }
}

/** Barra de selección: cuántos hay, poner estado, añadir a colección, seleccionar todo y cancelar. */
@Composable
private fun SelectionBar(vm: LibraryViewModel, selection: Set<String>, visible: List<Game>) {
    val collections by vm.collections.collectAsState()
    var statusMenu by remember { mutableStateOf(false) }
    var collMenu by remember { mutableStateOf(false) }
    var newColl by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().background(Color(0xF02A1C14)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.selected_count, selection.size), color = Color(0xFFFFD27A), fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f))
        Box {
            androidx.compose.material3.OutlinedButton(onClick = { statusMenu = true }, enabled = selection.isNotEmpty()) {
                Text(stringResource(R.string.group_status) + " ▾", color = Color.White)
            }
            androidx.compose.material3.DropdownMenu(statusMenu, { statusMenu = false }) {
                com.repisa.app.model.StatusRules.order.forEach { st ->
                    androidx.compose.material3.DropdownMenuItem(text = { Text("${st.icon} ${st.label}") },
                        onClick = { statusMenu = false; vm.setStatusFor(selection, st); vm.endSelection() })
                }
                androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.status_clear)) },
                    onClick = { statusMenu = false; vm.setStatusFor(selection, null); vm.endSelection() })
            }
        }
        Box {
            androidx.compose.material3.OutlinedButton(onClick = { collMenu = true }, enabled = selection.isNotEmpty()) {
                Text("📂 " + stringResource(R.string.collection) + " ▾", color = Color.White)
            }
            androidx.compose.material3.DropdownMenu(collMenu, { collMenu = false }) {
                collections.forEach { c ->
                    androidx.compose.material3.DropdownMenuItem(text = { Text(c.name) },
                        onClick = { collMenu = false; vm.addToCollection(selection, c.id); vm.endSelection() })
                }
                androidx.compose.material3.DropdownMenuItem(text = { Text("＋ " + stringResource(R.string.collection_new)) },
                    onClick = { collMenu = false; newColl = true })
                // Quitar: solo las colecciones en las que está alguno de los seleccionados.
                val inUse = visible.filter { it.id in selection }.flatMap { it.collections }.toSet()
                val removable = collections.filter { it.id in inUse }
                if (removable.isNotEmpty()) {
                    androidx.compose.material3.HorizontalDivider()
                    Text("  " + stringResource(R.string.collection_remove_from), color = Color.Gray, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    removable.forEach { c ->
                        androidx.compose.material3.DropdownMenuItem(text = { Text("➖ " + c.name) },
                            onClick = { collMenu = false; vm.removeFromCollection(selection, c.id); vm.endSelection() })
                    }
                }
            }
        }
        androidx.compose.material3.TextButton(onClick = { vm.selectAll(visible) }) { Text(stringResource(R.string.select_all)) }
        androidx.compose.material3.TextButton(onClick = { vm.endSelection() }) { Text(stringResource(R.string.cancel)) }
    }
    if (newColl) CollectionNameDialog(onDismiss = { newColl = false }) { name ->
        val c = vm.createCollection(name); vm.addToCollection(selection, c.id); vm.endSelection(); newColl = false
    }
}

/** Pide el nombre de una colección nueva (o el nuevo nombre de una existente). */
@Composable
fun CollectionNameDialog(initial: String = "", onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.isEmpty()) R.string.collection_new else R.string.collection_rename)) },
        text = {
            androidx.compose.material3.OutlinedTextField(name, { name = it }, singleLine = true,
                placeholder = { Text(stringResource(R.string.collection_hint)) })
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Juegos con platino (llevan un 💎 en la caja). */
val LocalPlatinums = androidx.compose.runtime.staticCompositionLocalOf<Set<String>> { emptySet() }

/** Precios de la lista de deseos por id de caja (ver LibraryViewModel.wishPrices). */
val LocalPriceTags = androidx.compose.runtime.staticCompositionLocalOf<Map<String, com.repisa.app.sources.WishItem>> { emptyMap() }

/**
 * Franja de precio sobre la carátula de la lista de deseos: negra, de lado a lado, con el precio en blanco.
 * De oferta: el precio de antes tachado, el descuento en verde y el precio nuevo.
 */
@Composable
private fun PriceBand(w: com.repisa.app.sources.WishItem, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().background(Color(0xE6000000)).padding(horizontal = 3.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (w.discountPct > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("−${w.discountPct}%", color = Color(0xFF0E2A12), fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1,
                    modifier = Modifier.background(Color(0xFFA4E86A), RoundedCornerShape(3.dp)).padding(horizontal = 3.dp))
                w.originalPrice?.let {
                    Text(it, color = Color(0xFFAAAAAA), fontSize = 10.sp, maxLines = 1,
                        style = TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                }
            }
            Text(w.price.orEmpty(), color = Color(0xFFBEEE62), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        } else {
            Text(w.price ?: L10n.s(R.string.wish_no_price), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

/** Junto al nombre de la repisa de la lista de deseos: ordenar y volver a consultar los precios. */
@Composable
private fun WishlistControls(vm: LibraryViewModel) {
    val sort by vm.wishSort.collectAsState()
    val refreshing by vm.wishRefreshing.collectAsState()
    val options = listOf(
        "STEAM" to R.string.wish_sort_steam, "NAME" to R.string.wish_sort_name, "PRICE_DESC" to R.string.wish_sort_price_desc,
        "PRICE_ASC" to R.string.wish_sort_price_asc, "DISCOUNT" to R.string.wish_sort_discount,
    )
    Spacer(Modifier.width(8.dp))
    Picker(stringResource(R.string.sort_label), stringResource(options.firstOrNull { it.first == sort }?.second ?: R.string.wish_sort_steam),
        options.map { stringResource(it.second) }) { vm.setWishSort(options[it].first) }
    TextButton(onClick = { vm.refreshWishlistNow() }, enabled = !refreshing) {
        Text(if (refreshing) "⏳ " + stringResource(R.string.wish_refreshing) else "🔄 " + stringResource(R.string.wish_refresh_prices),
            color = Color(0xFFE8D5B5), fontSize = 12.sp)
    }
}
