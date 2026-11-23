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
            val boxH = minOf(maxHeight * 0.85f, maxWidth * 0.55f / frontAspect(game))
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
                    game.romName?.let { Stat(stringResource(R.string.stat_file), it) }
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
                        OutlinedButton(onClick = { editing = true }) {
                            Icon(Icons.Default.Edit, null, tint = Color.White); Text(" " + stringResource(R.string.edit_data), color = Color.White)
                        }
                    }
                }
            }
        }
    }
    if (rating) RatingDialog(game, vm, onDismiss = { rating = false })
    if (showCopies) CopiesDialog(game, vm, onDismiss = { showCopies = false })
    if (searchingCover) CoverSearchDialog(game, vm, onDismiss = { searchingCover = false })
    if (editing) EditDialog(game, onDismiss = { editing = false }) { vm.updateGame(it); editing = false }
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
private fun EditDialog(game: Game, onDismiss: () -> Unit, onSave: (Game) -> Unit) {
    var title by remember { mutableStateOf(game.title) }
    var genres by remember { mutableStateOf(game.genres.joinToString(", ")) }
    var dev by remember { mutableStateOf(game.developer.orEmpty()) }
    var year by remember { mutableStateOf(game.releaseYear?.toString().orEmpty()) }
    var cover by remember { mutableStateOf(game.coverUrl.orEmpty()) }
    var hours by remember { mutableStateOf(((game.trackedMinutes) / 60.0).let { if (it == 0.0) "" else L10n.decimal(it) }) }
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
                OutlinedTextField(hours, { hours = it }, label = { Text(stringResource(R.string.field_extra_hours)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(cover, { cover = it }, label = { Text(stringResource(R.string.field_cover_url)) }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(game.copy(
                    title = title.ifBlank { game.title },
                    // Si el usuario cambió el nombre, se guarda el suyo y no el de la copia.
                    originalTitle = if (title.isNotBlank() && title != game.title) null else game.originalTitle,
                    genres = genres.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                    developer = dev.ifBlank { null },
                    releaseYear = year.toIntOrNull(),
                    coverUrl = cover.ifBlank { null },
                    coverAspect = if (cover.ifBlank { null } == game.coverUrl) game.coverAspect else null,
                    trackedMinutes = ((hours.replace(',', '.').toDoubleOrNull() ?: 0.0) * 60).toInt(),
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
    val pad = Modifier.padding(horizontal = if (small) 8.dp else 10.dp, vertical = if (small) 4.dp else 6.dp)
    fun box(onClick: (() -> Unit)?) = Modifier.fillMaxHeight().background(Color(0xE6261A12), RoundedCornerShape(8.dp))
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }.then(pad)
    // Los tres recuadros con la misma altura.
    Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Mi nota
        Column(box(onRate)) {
            Text(stringResource(R.string.my_score), color = Color(0xFF9C8A70), fontSize = label)
            val r = game.rating
            if (r == null) Text(stringResource(R.string.rate_action), color = Color.White, fontSize = value, fontWeight = FontWeight.Bold)
            else Row(verticalAlignment = Alignment.CenterVertically) {
                Text(scale.format(r.overall), color = Color.White, fontSize = value, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
                Spacer(Modifier.width(4.dp))
                Stars(r.stars, if (small) 10.dp else 12.dp)
            }
        }
        // Metacritic
        if (game.metascore != null || game.userScore != null) {
            Column(box(game.metacriticUrl?.let { url -> onOpen?.let { { it(url) } } })) {
                Text("Metacritic", color = Color(0xFF9C8A70), fontSize = label)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ScoreBadge(game.metascore?.toString() ?: "–", game.metascore?.toFloat()?.div(100f), value)
                    if (!small) Text(stringResource(R.string.press), color = Color(0xFFD9C8AE), fontSize = label)
                    ScoreBadge(game.userScore?.let { L10n.decimal(it) } ?: "–", game.userScore?.toFloat()?.div(10f), value, round = true)
                    if (!small) Text(stringResource(R.string.users), color = Color(0xFFD9C8AE), fontSize = label)
                }
            }
        }
        // HowLongToBeat
        if (game.hltbMain != null || game.hltbExtra != null || game.hltbComplete != null) {
            Column(box(game.hltbUrl?.let { url -> onOpen?.let { { it(url) } } })) {
                Text("HowLongToBeat", color = Color(0xFF9C8A70), fontSize = label)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Hours(stringResource(R.string.hltb_main), game.hltbMain, value, label)
                    Hours(stringResource(R.string.hltb_extra), game.hltbExtra, value, label)
                    Hours(stringResource(R.string.hltb_complete), game.hltbComplete, value, label)
                }
            }
        }
    }
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
        Text(h?.let(::hoursText) ?: "–", color = Color.White, fontSize = value, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = Color(0xFFD9C8AE), fontSize = small, maxLines = 1)
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
