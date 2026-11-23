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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.model.Game
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
                            Icon(Icons.Default.Star, "Calificar", tint = if (game.rating != null) Color(0xFFFFC94D) else Color.White)
                        }
                        IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Cerrar", tint = Color.White) }
                    }
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tag(game.console.name, Color(0xFF5B3A1E))
                        Tag(game.source.label, when (game.source) {
                            Source.STEAM -> Color(0xFF1B2838); Source.EPIC -> Color(0xFF2A2A2A); Source.EMULATOR -> Color(0xFF3D2A5C)
                        })
                        if (game.familyShared) Tag("Familia Steam", Color(0xFF2B5A7A))
                        game.genres.forEach { Tag(it, Color(0xFF2F4A3A)) }
                    }
                    Spacer(Modifier.height(14.dp))
                    ScoresPanel(game, scale, onRate = { rating = true }, onOpen = { vm.launcher.openUrl(it) })
                    Spacer(Modifier.height(8.dp))
                    Stat("Tiempo de juego", formatPlaytime(game.totalMinutes) +
                        if (game.trackedMinutes > 0 && game.playtimeMinutes > 0) "  (${formatPlaytime(game.trackedMinutes)} medidos por Repisa)" else "")
                    Stat("Última partida", game.lastPlayed?.let { DateFormat.getDateInstance(DateFormat.LONG).format(Date(it)) } ?: "—")
                    Stat("Estudio", game.developer ?: "—")
                    game.publisher?.takeIf { it != game.developer }?.let { Stat("Editora", it) }
                    Stat("Año", game.releaseYear?.toString() ?: "—")
                    game.romName?.let { Stat("Archivo", it) }
                    game.description?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it.replace(Regex("<[^>]+>"), "").take(600), color = Color(0xFFD9C8AE), fontSize = 13.sp, lineHeight = 18.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canPlay) {
                            Button(onClick = { vm.play(game) }, modifier = Modifier.focusRequester(firstButton),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D), contentColor = Color.Black)) {
                                Icon(Icons.Default.PlayArrow, null); Text("Jugar")
                            }
                        }
                        game.storeUrl?.let { url ->
                            OutlinedButton(onClick = { vm.launcher.openUrl(url) },
                                modifier = if (canPlay) Modifier else Modifier.focusRequester(firstButton)) {
                                Text(if (game.source == Source.STEAM) "Ver en Steam" else "Ver en la tienda", color = Color.White)
                            }
                        }
                        OutlinedButton(onClick = { vm.toggleFavorite(game) },
                            modifier = if (!canPlay && game.storeUrl == null) Modifier.focusRequester(firstButton) else Modifier) {
                            Icon(Icons.Default.Star, null, tint = if (game.favorite) Color(0xFFFFD27A) else Color.Gray)
                            Text(if (game.favorite) " Favorito" else " Marcar favorito", color = Color.White)
                        }
                        OutlinedButton(onClick = { searchingCover = true }) {
                            Icon(Icons.Default.Search, null, tint = Color.White); Text(" Buscar carátula", color = Color.White)
                        }
                        OutlinedButton(onClick = { editing = true }) {
                            Icon(Icons.Default.Edit, null, tint = Color.White); Text(" Editar datos", color = Color.White)
                        }
                    }
                }
            }
        }
    }
    if (rating) RatingDialog(game, vm, onDismiss = { rating = false })
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
    var hours by remember { mutableStateOf(((game.trackedMinutes) / 60.0).let { if (it == 0.0) "" else "%.1f".format(it) }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ${game.title}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true)
                OutlinedTextField(genres, { genres = it }, label = { Text("Géneros (separados por comas)") })
                OutlinedTextField(dev, { dev = it }, label = { Text("Estudio") }, singleLine = true)
                OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("Año") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(hours, { hours = it }, label = { Text("Horas extra jugadas (a mano)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(cover, { cover = it }, label = { Text("URL de la portada") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(game.copy(
                    title = title.ifBlank { game.title },
                    genres = genres.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                    developer = dev.ifBlank { null },
                    releaseYear = year.toIntOrNull(),
                    coverUrl = cover.ifBlank { null },
                    coverAspect = if (cover.ifBlank { null } == game.coverUrl) game.coverAspect else null,
                    trackedMinutes = ((hours.replace(',', '.').toDoubleOrNull() ?: 0.0) * 60).toInt(),
                    userEdited = true,
                ))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Mi nota, Metacritic (prensa y usuarios) y duración según HowLongToBeat. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScoresPanel(game: Game, scale: com.repisa.app.model.RatingScale, onRate: () -> Unit, onOpen: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Mi nota
        Column(Modifier.background(Color(0xE6261A12), RoundedCornerShape(10.dp)).clickable(onClick = onRate)
            .padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("Mi nota", color = Color(0xFF9C8A70), fontSize = 11.sp)
            val r = game.rating
            if (r == null) Text("Calificar ★", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            else Row(verticalAlignment = Alignment.CenterVertically) {
                Text(scale.format(r.overall), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
                Text("/${scale.max}  ", color = Color(0xFF9C8A70), fontSize = 11.sp)
                Stars(r.stars, 14.dp)
            }
        }
        // Metacritic
        if (game.metascore != null || game.userScore != null) {
            Row(Modifier.background(Color(0xE6261A12), RoundedCornerShape(10.dp))
                .clickable { game.metacriticUrl?.let(onOpen) }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Metacritic", color = Color(0xFF9C8A70), fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        ScoreBadge(game.metascore?.toString() ?: "–", game.metascore?.toFloat()?.div(100f))
                        Text("prensa", color = Color(0xFFD9C8AE), fontSize = 11.sp)
                        ScoreBadge(game.userScore?.let { "%.1f".format(it).replace('.', ',') } ?: "–", game.userScore?.toFloat()?.div(10f), round = true)
                        Text("usuarios", color = Color(0xFFD9C8AE), fontSize = 11.sp)
                    }
                }
            }
        }
        // HowLongToBeat
        if (game.hltbMain != null || game.hltbExtra != null || game.hltbComplete != null) {
            Column(Modifier.background(Color(0xE6261A12), RoundedCornerShape(10.dp))
                .clickable { game.hltbUrl?.let(onOpen) }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("HowLongToBeat", color = Color(0xFF9C8A70), fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Hours("Historia", game.hltbMain)
                    Hours("+ Extras", game.hltbExtra)
                    Hours("Completista", game.hltbComplete)
                }
            }
        }
    }
}

@Composable
private fun ScoreBadge(text: String, normalized: Float?, round: Boolean = false) {
    val color = when {
        normalized == null -> Color(0xFF555555)
        normalized >= 0.75f -> Color(0xFF2E9E4F)
        normalized >= 0.5f -> Color(0xFFE0A526)
        else -> Color(0xFFD9443A)
    }
    Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp,
        modifier = Modifier.background(color, RoundedCornerShape(if (round) 50 else 6)).padding(horizontal = 8.dp, vertical = 2.dp))
}

@Composable
private fun Hours(label: String, h: Float?) {
    Column {
        Text(h?.let { if (it < 1f) "${(it * 60).toInt()} min" else "%.1f h".format(it).replace(".0 h", " h").replace('.', ',') } ?: "–",
            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFFD9C8AE), fontSize = 10.sp)
    }
}
