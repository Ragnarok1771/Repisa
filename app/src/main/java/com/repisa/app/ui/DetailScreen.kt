package com.repisa.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.repisa.app.R
import com.repisa.app.i18n.L10n
import com.repisa.app.model.Game
import com.repisa.app.model.label
import com.repisa.app.model.Source
import com.repisa.app.model.formatPlaytime
import java.text.DateFormat
import java.util.Date

/** Ficha del juego: la caja sale de la repisa y gira un poco para enseñarse. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(game: Game, vm: LibraryViewModel, onClose: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var hiding by remember { mutableStateOf(false) }
    var editingNotes by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }
    var searchingCover by remember { mutableStateOf(false) }
    var rating by remember { mutableStateOf(false) }
    var showCopies by remember { mutableStateOf(false) }
    val scale by vm.ratingScale.collectAsState()
    LaunchedEffect(game.id) { vm.loadExternalInfo(game) }
    val spin = remember(game.id) { Animatable(-35f) }
    val rise = remember(game.id) { Animatable(0.6f) }
    LaunchedEffect(game.id) { rise.animateTo(1f, tween(350)) }
    LaunchedEffect(game.id) { spin.animateTo(-10f, tween(700)) }
    val firstButton = remember { FocusRequester() }
    LaunchedEffect(game.id) { runCatching { firstButton.requestFocus() } }
    val canPlay = vm.launcher.canLaunch(game)
    val density = LocalDensity.current

    Box(
        Modifier.fillMaxSize().background(Color(0xE6100A07))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(20.dp)) {
            // La caja ocupa como mucho ~40 % del ancho (las carátulas anchas ya no dejan sin sitio a la ficha).
            val boxH = minOf(maxHeight * 0.85f, maxWidth * 0.40f / frontAspect(game))
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                GameBox(
                    game, boxH,
                    modifier = Modifier.graphicsLayer {
                        rotationY = spin.value
                        scaleX = rise.value; scaleY = rise.value
                        cameraDistance = 14f * density.density
                    },
                )
                Spacer(Modifier.width(28.dp))
                Column(
                    Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState())
                        .clickable(remember { MutableInteractionSource() }, indication = null) { },
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(game.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black,
                            fontFamily = DisplayFont, lineHeight = 30.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { rating = true }) {
                            Icon(Icons.Default.Star, stringResource(R.string.rate), tint = if (game.rating != null) Color(0xFFFFC94D) else Color.White)
                        }
                        IconButton(onClick = onClose) { Icon(Icons.Default.Close, stringResource(R.string.close), tint = Color.White) }
                    }
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tag(game.console.label, Color(0xFF5B3A1E))
                        Tag(game.source.label, when (game.source) {
                            Source.STEAM -> Color(0xFF1B2838); Source.EPIC -> Color(0xFF2A2A2A); Source.EMULATOR -> Color(0xFF3D2A5C)
                        })
                        if (game.familyShared) Tag(stringResource(R.string.tag_family), Color(0xFF2B5A7A))
                        game.genres.forEach { Tag(L10n.genre(it), Color(0xFF2F4A3A)) }
                    }
                    Spacer(Modifier.height(14.dp))
                    ScoresRow(game, scale, onRate = { rating = true }, onOpen = { vm.launcher.openUrl(it) })
                    Spacer(Modifier.height(8.dp))
                    Stat(stringResource(R.string.stat_playtime), formatPlaytime(game.totalMinutes) +
                        if (game.trackedMinutes > 0 && game.playtimeMinutes > 0)
                            "  " + stringResource(R.string.stat_tracked, formatPlaytime(game.trackedMinutes)) else "")
                    Stat(stringResource(R.string.stat_last_played),
                        game.lastPlayed?.let { DateFormat.getDateInstance(DateFormat.LONG).format(Date(it)) } ?: "—")
                    Stat(stringResource(R.string.stat_studio), game.developer ?: "—")
                    game.publisher?.takeIf { it != game.developer }?.let { Stat(stringResource(R.string.stat_publisher), it) }
                    Stat(stringResource(R.string.stat_year), game.releaseYear?.toString() ?: "—")
                    game.notes?.takeIf { it.isNotBlank() }?.let { Stat(stringResource(R.string.notes), it) }
                    game.romName?.let { Stat(stringResource(R.string.stat_file), it) }
                    if (game.source == com.repisa.app.model.Source.EMULATOR) Stat(stringResource(R.string.stat_origin), stringResource(when {
                        game.romUri != null -> R.string.origin_rom_folder
                        game.corePath != null || game.romPath != null -> R.string.origin_retroarch
                        else -> R.string.origin_frontend
                    }))
                    game.description?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it.replace(Regex("<[^>]+>"), "").take(600), color = Color(0xFFD9C8AE), fontSize = 13.sp, lineHeight = 18.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canPlay) {
                            Button(onClick = { vm.play(game) }, modifier = Modifier.focusRequester(firstButton),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D), contentColor = Color.Black)) {
                                Icon(Icons.Default.PlayArrow, null); Text(stringResource(R.string.play))
                            }
                        }
                        game.storeUrl?.let { url ->
                            OutlinedButton(onClick = { vm.launcher.openUrl(url) },
                                modifier = if (canPlay) Modifier else Modifier.focusRequester(firstButton)) {
                                Text(stringResource(if (game.source == Source.STEAM) R.string.view_steam else R.string.view_store), color = Color.White)
                            }
                        }
                        OutlinedButton(onClick = { vm.toggleFavorite(game) },
                            modifier = if (!canPlay && game.storeUrl == null) Modifier.focusRequester(firstButton) else Modifier) {
                            Icon(Icons.Default.Star, null, tint = if (game.favorite) Color(0xFFFFD27A) else Color.Gray)
                            Text(" " + stringResource(if (game.favorite) R.string.favorite else R.string.mark_favorite), color = Color.White)
                        }
                        OutlinedButton(onClick = { searchingCover = true }) {
                            Icon(Icons.Default.Search, null, tint = Color.White); Text(" " + stringResource(R.string.search_cover), color = Color.White)
                        }
                        if (game.copyIds.isNotEmpty()) OutlinedButton(onClick = { showCopies = true }) {
                            Text("⧉ " + stringResource(R.string.copies_button, game.copyIds.size + 1), color = Color.White)
                        }
                        OutlinedButton(onClick = { editingNotes = true }) {
                            Text("✎ " + stringResource(R.string.notes), color = Color.White)
                        }
                        OutlinedButton(onClick = { editing = true }) {
                            Icon(Icons.Default.Edit, null, tint = Color.White); Text(" " + stringResource(R.string.edit_data), color = Color.White)
                        }
                        if (game.totalMinutes > 0) OutlinedButton(onClick = { resetting = true }) {
                            Text("⟲ " + stringResource(R.string.reset_time), color = Color.White)
                        }
                        OutlinedButton(onClick = { hiding = true }) {
                            Text("✕ " + stringResource(R.string.hide_game), color = Color(0xFFFFB4A8))
                        }
                    }
                }
            }
        }
    }
    if (rating) RatingDialog(game, vm, onDismiss = { rating = false })
    if (showCopies) CopiesDialog(game, vm, onDismiss = { showCopies = false })
    if (searchingCover) CoverSearchDialog(game, vm, onDismiss = { searchingCover = false })
    if (editing) EditDialog(game, vm.settings.manualTimeIsTotal, onDismiss = { editing = false }) { vm.updateGame(it); editing = false }
    if (editingNotes) com.repisa.app.NotesDialog(game.notes.orEmpty(), onDismiss = { editingNotes = false }) {
        vm.updateGame(game.copy(notes = it.ifBlank { null })); editingNotes = false
    }
    if (resetting) androidx.compose.material3.AlertDialog(
        onDismissRequest = { resetting = false },
        title = { Text(stringResource(R.string.reset_time)) },
        text = { Text(stringResource(R.string.reset_time_confirm, game.title)) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = {
            resetting = false
            // El total queda en 0: lo que registren las fuentes desde ahora se vuelve a sumar.
            vm.updateGame(game.copy(manualMinutes = game.manualMinutes - game.totalMinutes))
        }) { Text(stringResource(R.string.reset_time)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { resetting = false }) { Text(stringResource(R.string.cancel)) } },
    )
    if (hiding) androidx.compose.material3.AlertDialog(
        onDismissRequest = { hiding = false },
        title = { Text(stringResource(R.string.hide_game)) },
        text = { Text(stringResource(R.string.hide_game_confirm, game.title)) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { hiding = false; vm.hideGame(game) }) { Text(stringResource(R.string.hide_game)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { hiding = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun Tag(text: String, color: Color) {
    Text(text, color = Color.White, fontSize = 12.sp,
        modifier = Modifier.background(color, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp))
}

@Composable
private fun Stat(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", color = Color(0xFF9C8A70), fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EditDialog(game: Game, totalMode: Boolean, onDismiss: () -> Unit, onSave: (Game) -> Unit) {
    var title by remember { mutableStateOf(game.title) }
    var genres by remember { mutableStateOf(game.genres.joinToString(", ")) }
    var dev by remember { mutableStateOf(game.developer.orEmpty()) }
    var year by remember { mutableStateOf(game.releaseYear?.toString().orEmpty()) }
    var cover by remember { mutableStateOf(game.coverUrl.orEmpty()) }
    // Modo «sumar»: se edita solo el tiempo puesto a mano. Modo «total»: se edita el tiempo total del juego.
    val shown = if (totalMode) game.totalMinutes else maxOf(0, game.manualMinutes)
    var hours by remember { mutableStateOf(if (shown > 0) (shown / 60).toString() else "") }
    var minutes by remember { mutableStateOf(if (shown > 0) (shown % 60).toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_title, game.title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.field_title)) }, singleLine = true)
                OutlinedTextField(genres, { genres = it }, label = { Text(stringResource(R.string.field_genres)) })
                OutlinedTextField(dev, { dev = it }, label = { Text(stringResource(R.string.stat_studio)) }, singleLine = true)
                OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text(stringResource(R.string.stat_year)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                // Tiempo jugado fuera de Repisa en horas y minutos (17 h 52 min), no en decimales.
                Text(stringResource(if (totalMode) R.string.field_total_time else R.string.field_extra_time), fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(hours, { hours = it.filter(Char::isDigit).take(5) }, label = { Text(stringResource(R.string.field_hours)) },
                        singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(2) }, label = { Text(stringResource(R.string.field_minutes)) },
                        singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                OutlinedTextField(cover, { cover = it }, label = { Text(stringResource(R.string.field_cover_url)) }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val typed = (hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0).coerceAtMost(59)
                // En modo total, el tiempo a mano es lo que falta (o sobra) para llegar al total escrito.
                val manual = if (totalMode) game.manualMinutes + (typed - game.totalMinutes) else typed
                // Solo cambiar el tiempo no «congela» el resto de datos del juego.
                val onlyTime = title == game.title && genres == game.genres.joinToString(", ") &&
                    dev == game.developer.orEmpty() && year == game.releaseYear?.toString().orEmpty() && cover == game.coverUrl.orEmpty()
                if (onlyTime) { onSave(game.copy(manualMinutes = manual)); return@TextButton }
                onSave(game.copy(
                    title = title.ifBlank { game.title },
                    // Si el usuario cambió el nombre, se guarda el suyo y no el de la copia.
                    originalTitle = if (title.isNotBlank() && title != game.title) null else game.originalTitle,
                    genres = genres.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                    developer = dev.ifBlank { null },
                    releaseYear = year.toIntOrNull(),
                    coverUrl = cover.ifBlank { null },
                    coverAspect = if (cover.ifBlank { null } == game.coverUrl) game.coverAspect else null,
                    manualMinutes = manual,
                    userEdited = true,
                ))
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * Mi nota, Metacritic (prensa y usuarios) y HowLongToBeat en una sola fila compacta.
 * [small] es la versión reducida para la segunda pantalla.
 */
@Composable
fun ScoresRow(game: Game, scale: com.repisa.app.model.RatingScale, small: Boolean = false,
              onRate: (() -> Unit)? = null, onOpen: ((String) -> Unit)? = null) {
    val label = if (small) 9.sp else 10.sp
    val value = if (small) 13.sp else 15.sp
    // Altura fija y texto en una línea: los recuadros no se estiran hacia abajo aunque falte espacio.
    val boxHeight = if (small) 44.dp else 68.dp
    fun box(onClick: (() -> Unit)?) = Modifier.height(boxHeight)
        .background(Color(0xE6261A12), RoundedCornerShape(8.dp))
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(horizontal = if (small) 8.dp else 10.dp, vertical = if (small) 3.dp else 5.dp)
    val loading = game.extFetchedAt == null
    // Interlineado ajustado: Nunito trae mucho espacio y recortaba la segunda línea.
    val tight = androidx.compose.material3.LocalTextStyle.current.copy(
        lineHeight = 1.15.em,
        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
            androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
            androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
        ),
    )
    // Si no caben en una fila (carátulas anchas como DS, 3DS, N64 o GBA), HowLongToBeat baja a la siguiente.
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalTextStyle provides tight) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Mi nota
        Column(box(onRate), verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.my_score), color = Color(0xFF9C8A70), fontSize = label, maxLines = 1, softWrap = false)
            val r = game.rating
            if (r == null) Text(stringResource(R.string.rate_action), color = Color.White, fontSize = value,
                fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            else Row(verticalAlignment = Alignment.CenterVertically) {
                Text(scale.format(r.overall), color = Color.White, fontSize = value, fontWeight = FontWeight.Bold,
                    fontFamily = DisplayFont, maxLines = 1, softWrap = false)
                Spacer(Modifier.width(4.dp))
                Stars(r.stars, if (small) 10.dp else 12.dp)
            }
        }
        // Metacritic (siempre visible: con sus notas o "No disponible")
        val hasMc = game.metascore != null || game.userScore != null
        Column(box(if (hasMc) game.metacriticUrl?.let { url -> onOpen?.let { { it(url) } } } else null),
            verticalArrangement = Arrangement.Center) {
            Text("Metacritic", color = Color(0xFF9C8A70), fontSize = label, maxLines = 1, softWrap = false)
            if (hasMc) Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                ScoreBadge(game.metascore?.toString() ?: "–", game.metascore?.toFloat()?.div(100f), value)
                if (!small) Text(stringResource(R.string.press), color = Color(0xFFD9C8AE), fontSize = label, maxLines = 1, softWrap = false)
                ScoreBadge(game.userScore?.let { L10n.decimal(it) } ?: "–", game.userScore?.toFloat()?.div(10f), value, round = true)
                if (!small) Text(stringResource(R.string.users), color = Color(0xFFD9C8AE), fontSize = label, maxLines = 1, softWrap = false)
            } else Missing(loading, value)
        }
        // HowLongToBeat (siempre visible)
        val hasHltb = game.hltbMain != null || game.hltbExtra != null || game.hltbComplete != null
        Column(box(if (hasHltb) game.hltbUrl?.let { url -> onOpen?.let { { it(url) } } } else null),
            verticalArrangement = Arrangement.Center) {
            Text("HowLongToBeat", color = Color(0xFF9C8A70), fontSize = label, maxLines = 1, softWrap = false)
            if (hasHltb) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 2.dp)) {
                Hours(stringResource(R.string.hltb_main), game.hltbMain, value, label)
                Hours(stringResource(R.string.hltb_extra), game.hltbExtra, value, label)
                Hours(stringResource(R.string.hltb_complete), game.hltbComplete, value, label)
            } else Missing(loading, value)
        }
    }
    }
}

/** "No disponible" (o "…" mientras se consulta). */
@Composable
private fun Missing(loading: Boolean, size: androidx.compose.ui.unit.TextUnit) {
    Text(if (loading) "…" else stringResource(R.string.not_available), color = Color(0xFF9C8A70),
        fontSize = size * 0.85f, maxLines = 1, softWrap = false)
}

@Composable
private fun ScoreBadge(text: String, normalized: Float?, size: androidx.compose.ui.unit.TextUnit, round: Boolean = false) {
    val color = when {
        normalized == null -> Color(0xFF555555)
        normalized >= 0.75f -> Color(0xFF2E9E4F)
        normalized >= 0.5f -> Color(0xFFE0A526)
        else -> Color(0xFFD9443A)
    }
    Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = size,
        modifier = Modifier.background(color, RoundedCornerShape(if (round) 50 else 5)).padding(horizontal = 6.dp, vertical = 1.dp))
}

/** "2,5 h", "45 h", "120 h": sin decimales a partir de 10 h para que quepa todo en una fila. */
private fun hoursText(h: Float): String = when {
    h < 1f -> L10n.s(R.string.time_min, (h * 60).toInt())
    h >= 10f || h % 1f == 0f -> L10n.s(R.string.time_h, Math.round(h))
    else -> L10n.decimal(h) + " h"
}

@Composable
private fun Hours(label: String, h: Float?, value: androidx.compose.ui.unit.TextUnit, small: androidx.compose.ui.unit.TextUnit) {
    Column {
        Text(h?.let(::hoursText) ?: "–", color = Color.White, fontSize = value, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Text(label, color = Color(0xFFD9C8AE), fontSize = small, maxLines = 1, softWrap = false)
    }
}

/** Copias del mismo juego unidas en esta caja: elegir la preferente o separar alguna. */
@Composable
private fun CopiesDialog(game: Game, vm: LibraryViewModel, onDismiss: () -> Unit) {
    val copies = remember(game.id, game.copyIds) { vm.copiesOf(game) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.copies_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.copies_hint), fontSize = 13.sp)
                copies.forEach { c ->
                    val preferred = c.id == game.id
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (preferred) Color(0xFF5B3A1E) else Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                            .clickable { vm.setPreferredCopy(copies, c); onDismiss() }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = preferred, onClick = { vm.setPreferredCopy(copies, c); onDismiss() })
                        Column(Modifier.weight(1f)) {
                            Text(c.title, fontWeight = FontWeight.Bold, maxLines = 2)
                            Text("${c.source.label} · ${formatPlaytime(c.playtimeMinutes + c.trackedMinutes)}" +
                                if (preferred) " · " + stringResource(R.string.copy_preferred) else "", fontSize = 12.sp)
                        }
                        TextButton(onClick = { vm.splitCopy(c); onDismiss() }) { Text(stringResource(R.string.copy_split)) }
                    }
                }
                Text(stringResource(R.string.copy_split_hint), fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
