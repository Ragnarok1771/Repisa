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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.repisa.app.R
import com.repisa.app.i18n.L10n
import com.repisa.app.model.Game
import com.repisa.app.sources.CoverResult
import com.repisa.app.sources.EmulatorSource
import kotlinx.coroutines.launch

/**
 * Buscador de carátulas: escribe el nombre (viene relleno con el del juego, ya limpio),
 * y elige una de las carátulas encontradas en GameTDB, libretro, SteamGridDB o la tienda de Steam.
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
            failed = out?.failed ?: listOf(L10n.s(R.string.all_sources))
            loading = false
        }
    }
    LaunchedEffect(game.id) { search() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().padding(16.dp).background(Color(0xFF1E1612), RoundedCornerShape(14.dp)).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cover_search_title, game.console.name), color = Color(0xFFFFD27A), fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    label = { Text(stringResource(R.string.game_name)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { search() }),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { search() }) { Icon(Icons.Default.Search, null); Text(" " + stringResource(R.string.search)) }
            }
            if (!vm.coverSearch.hasSteamGridDb) {
                Text(
                    stringResource(R.string.sgdb_tip),
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
                        if (failed.isNotEmpty()) stringResource(R.string.cover_conn_failed, failed.joinToString(", "))
                        else stringResource(R.string.cover_none),
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
