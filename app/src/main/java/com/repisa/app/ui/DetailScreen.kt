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
            val boxH = minOf(maxHeight * 0.85f, maxWidth * 0.55f / game.console.case.aspect)
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
                            fontFamily = FontFamily.Serif, lineHeight = 30.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Cerrar", tint = Color.White) }
                    }
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tag(game.console.name, Color(0xFF5B3A1E))
                        Tag(game.source.label, when (game.source) {
                            Source.STEAM -> Color(0xFF1B2838); Source.EPIC -> Color(0xFF2A2A2A); Source.EMULATOR -> Color(0xFF3D2A5C)
                        })
                        game.genres.forEach { Tag(it, Color(0xFF2F4A3A)) }
                    }
                    Spacer(Modifier.height(14.dp))
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
                        OutlinedButton(onClick = { editing = true }) {
                            Icon(Icons.Default.Edit, null, tint = Color.White); Text(" Editar datos", color = Color.White)
                        }
                    }
                }
            }
        }
    }
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
                    trackedMinutes = ((hours.replace(',', '.').toDoubleOrNull() ?: 0.0) * 60).toInt(),
                    userEdited = true,
                ))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
