package com.repisa.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.repisa.app.model.Game
import com.repisa.app.sources.CoverResult
import com.repisa.app.sources.EmulatorSource
import kotlinx.coroutines.launch

/**
 * Buscador de carátulas: escribe el nombre (viene relleno con el del juego, ya limpio),
 * y elige una de las carátulas encontradas en libretro, SteamGridDB, IGDB o la tienda de Steam.
 */
@Composable
fun CoverSearchDialog(game: Game, vm: LibraryViewModel, onDismiss: () -> Unit) {
    val initial = remember(game.id) { EmulatorSource.cleanTitle(game.title) }
    var query by remember { mutableStateOf(initial) }
    var results by remember { mutableStateOf<List<CoverResult>?>(null) }
    var failed by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun search() {
        loading = true
        scope.launch {
            val out = runCatching { vm.coverSearch.search(game, query) }.getOrNull()
            results = out?.results.orEmpty()
            failed = out?.failed ?: listOf("todos")
            loading = false
        }
    }
    LaunchedEffect(game.id) { search() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().padding(16.dp).background(Color(0xFF1E1612), RoundedCornerShape(14.dp)).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Buscar carátula · ${game.console.name}", color = Color(0xFFFFD27A), fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    label = { Text("Nombre del juego") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { search() }),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { search() }) { Icon(Icons.Default.Search, null); Text(" Buscar") }
            }
            if (!vm.coverSearch.hasSteamGridDb) {
                Text(
                    "Consejo: añade tu clave gratuita de SteamGridDB en Ajustes → Portadas y datos para tener muchísimas más carátulas (Switch, 3DS, PC…).",
                    color = Color(0xFFBFA98A), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val r = results
                when {
                    loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color(0xFFFFB74D))
                    r == null -> Unit
                    r.isEmpty() -> Text(
                        if (failed.isNotEmpty()) "No se pudo conectar con: ${failed.joinToString(", ")}. Revisa la conexión a internet."
                        else "No se encontró nada. Prueba con menos palabras (p. ej. solo \"Bravely Default\") o con el nombre en inglés.",
                        color = Color(0xFFBFA98A), modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(r, key = { it.url }) { c ->
                            Column(
                                Modifier.clip(RoundedCornerShape(6.dp))
                                    .clickable { vm.setCustomCover(game, c.url); onDismiss() }
                                    .padding(4.dp),
                            ) {
                                AsyncImage(
                                    model = c.thumb, contentDescription = c.label, contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(0.7f)
                                        .background(Color(0xFF2A1E17), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(4.dp)),
                                )
                                Text(c.source, color = Color(0xFFFFD27A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(c.label, color = Color(0xFFD9C8AE), fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
