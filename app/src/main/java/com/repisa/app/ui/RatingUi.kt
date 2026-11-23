package com.repisa.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repisa.app.R
import com.repisa.app.data.Http
import com.repisa.app.i18n.L10n
import com.repisa.app.data.Settings
import com.repisa.app.model.Criterion
import com.repisa.app.model.DefaultCriteria
import com.repisa.app.model.Game
import com.repisa.app.model.Rating
import com.repisa.app.model.RatingScale
import kotlinx.serialization.builtins.ListSerializer
import kotlin.math.roundToInt

private val Gold = Color(0xFFFFC94D)

/** Cinco estrellas que se llenan en proporción: 4,25 = cuatro llenas y un cuarto de la quinta. */
@Composable
fun Stars(stars: Float, size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        for (i in 0 until 5) {
            val fill = (stars - i).coerceIn(0f, 1f)
            Box(Modifier.size(size)) {
                Icon(Icons.Default.Star, null, tint = Color(0x44FFFFFF), modifier = Modifier.fillMaxSize())
                if (fill > 0f) Icon(
                    Icons.Default.Star, null, tint = Gold,
                    modifier = Modifier.fillMaxSize().clipToBounds().drawWithContent {
                        clipRect(right = this.size.width * fill) { this@drawWithContent.drawContent() }
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------- Plantilla de criterios

fun loadTemplate(settings: Settings): List<Criterion> = settings.ratingTemplate?.let {
    runCatching { Http.json.decodeFromString(ListSerializer(Criterion.serializer()), it) }.getOrNull()
}?.takeIf { it.isNotEmpty() } ?: DefaultCriteria

fun saveTemplate(settings: Settings, list: List<Criterion>) {
    settings.ratingTemplate = Http.json.encodeToString(ListSerializer(Criterion.serializer()), list.map { it.copy(score = 0f) })
}

/**
 * Editor de criterios: nombre libre, peso en % y (si [withScores]) la nota en la escala elegida.
 * Se usa para calificar un juego y para editar la plantilla en Ajustes.
 */
@Composable
fun CriteriaEditor(
    criteria: MutableList<Criterion>,
    scale: RatingScale,
    withScores: Boolean,
) {
    val totalWeight = criteria.sumOf { it.weight.toDouble() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        criteria.forEachIndexed { i, c ->
            Column(Modifier.fillMaxWidth().background(Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                // Fila compacta: qué se califica · peso · nota · quitar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = c.name, onValueChange = { criteria[i] = c.copy(name = it) }, singleLine = true,
                        label = { Text(stringResource(R.string.what_you_rate)) }, modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    NumberField(value = c.weight, label = stringResource(R.string.weight_pct), width = 84.dp, decimals = false) {
                        criteria[i] = c.copy(weight = it.coerceIn(0f, 100f))
                    }
                    if (withScores) {
                        Spacer(Modifier.width(8.dp))
                        NumberField(value = c.score * scale.max, label = stringResource(R.string.score_of, scale.max), width = 96.dp, decimals = scale.max < 100) {
                            criteria[i] = c.copy(score = (it / scale.max).coerceIn(0f, 1f))
                        }
                    }
                    IconButton(onClick = { criteria.removeAt(i) }) { Icon(Icons.Default.Delete, stringResource(R.string.remove), tint = Color(0xFFE57373)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (withScores) Slider(
                        value = c.score * scale.max, onValueChange = { v ->
                            val step = if (scale.max >= 100) 1f else 0.5f
                            criteria[i] = c.copy(score = ((v / step).roundToInt() * step / scale.max).coerceIn(0f, 1f))
                        },
                        valueRange = 0f..scale.max.toFloat(), modifier = Modifier.weight(1f).height(28.dp),
                        colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold),
                    ) else Spacer(Modifier.weight(1f))
                    if (totalWeight > 0) Text(
                        "  " + stringResource(R.string.pct_of_score, L10n.decimal(c.weight / totalWeight * 100, 0)),
                        color = Color(0xFF9C8A70), fontSize = 11.sp,
                    )
                }
            }
        }
        OutlinedButton(onClick = { criteria += Criterion(L10n.s(R.string.new_aspect), 10f, 0f) }) {
            Icon(Icons.Default.Add, null, tint = Color.White); Text(" " + stringResource(R.string.add_aspect), color = Color.White)
        }
        if (totalWeight > 0 && Math.round(totalWeight) != 100L) Text(
            stringResource(R.string.weights_sum, L10n.decimal(totalWeight, 0)),
            color = Color(0xFF9C8A70), fontSize = 11.sp,
        )
    }
}

/** Campo numérico que acepta coma o punto. */
@Composable
private fun NumberField(value: Float, label: String, width: Dp, decimals: Boolean, onChange: (Float) -> Unit) {
    fun fmt(v: Float) = if (!decimals || v % 1f == 0f) v.roundToInt().toString() else L10n.decimal(v)
    var text by remember(value) { mutableStateOf(fmt(value)) }
    OutlinedTextField(
        value = text, singleLine = true, label = { Text(label, fontSize = 11.sp) },
        onValueChange = { t -> text = t; t.replace(',', '.').toFloatOrNull()?.let(onChange) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.width(width),
    )
}

// ---------------------------------------------------------------------------- Calificar un juego

@Composable
fun RatingDialog(game: Game, vm: LibraryViewModel, onDismiss: () -> Unit) {
    val scale by vm.ratingScale.collectAsState()
    val criteria = remember(game.id) {
        mutableStateListOf<Criterion>().apply { addAll(game.rating?.criteria ?: loadTemplate(vm.settings)) }
    }
    var note by remember(game.id) { mutableStateOf(game.rating?.note.orEmpty()) }
    val preview = Rating(criteria.toList())

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().padding(16.dp).background(Color(0xFF1E1612), RoundedCornerShape(14.dp)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.rate), color = Color(0xFF9C8A70), fontSize = 12.sp)
                    Text(game.title, color = Color(0xFFFFD27A), fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = DisplayFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${scale.format(preview.overall)} / ${scale.max}", color = Color.White, fontSize = 26.sp,
                        fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
                    Stars(preview.stars, 20.dp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                CriteriaEditor(criteria, scale, withScores = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.comment_optional)) }, modifier = Modifier.fillMaxWidth())
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (game.rating != null) TextButton(onClick = { vm.setRating(game, null); onDismiss() }) {
                    Text(stringResource(R.string.delete_rating), color = Color(0xFFE57373))
                }
                TextButton(onClick = { criteria.clear(); criteria.addAll(loadTemplate(vm.settings)) }) { Text(stringResource(R.string.use_template)) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Button(onClick = {
                    vm.setRating(game, Rating(criteria.filter { it.name.isNotBlank() }, note.ifBlank { null }))
                    onDismiss()
                }) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

// ---------------------------------------------------------------------------- Juegos calificados

@Composable
fun RatedScreen(vm: LibraryViewModel, onBack: () -> Unit, onOpen: (Game) -> Unit) {
    val games by vm.games.collectAsState()
    val scale by vm.ratingScale.collectAsState()
    val rated = games.filter { it.rating != null }.sortedByDescending { it.rating!!.overall }
    var showSettings by remember { mutableStateOf(false) }
    if (showSettings) RatingSettingsDialog(vm, onDismiss = { showSettings = false })
    Column(Modifier.fillMaxSize().roomBackground()) {
        Row(Modifier.fillMaxWidth().background(Color(0xCC120C09)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White) }
            Text(stringResource(R.string.my_ratings), color = Color(0xFFFFD27A), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, fontFamily = DisplayFont)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.rated_subtitle, L10n.games(rated.size), scale.label.lowercase()),
                color = Color(0xFFBFA98A), fontSize = 12.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Settings, stringResource(R.string.rating_settings), tint = Color.White)
            }
        }
        if (rated.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.rated_empty),
                    color = Color(0xFFBFA98A), fontSize = 15.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(rated, key = { _, g -> g.id }) { i, g ->
                    val r = g.rating!!
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xB3120C09), RoundedCornerShape(12.dp))
                            .clickable { onOpen(g) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${i + 1}", color = Color(0xFF9C8A70), fontSize = 18.sp, fontFamily = DisplayFont,
                            modifier = Modifier.width(36.dp))
                        GameBox(g, 70.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(g.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(g.console.name + (g.metascore?.let { " · Metacritic $it" } ?: ""), color = Color(0xFFBFA98A), fontSize = 12.sp)
                            Text(r.criteria.joinToString("  ·  ") { "${it.name} ${scale.format(it.score)}" },
                                color = Color(0xFF9C8A70), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(scale.format(r.overall), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                                fontFamily = DisplayFont)
                            Stars(r.stars, 16.dp)
                        }
                    }
                }
            }
        }
    }
}

/** Ajustes de calificación (escala y plantilla), desde Mis calificaciones. */
@Composable
private fun RatingSettingsDialog(vm: LibraryViewModel, onDismiss: () -> Unit) {
    val scale by vm.ratingScale.collectAsState()
    val template = remember { mutableStateListOf<Criterion>().apply { addAll(loadTemplate(vm.settings)) } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().padding(16.dp).background(Color(0xFF1E1612), RoundedCornerShape(14.dp)).padding(16.dp)) {
            Text(stringResource(R.string.rating_settings), color = Color(0xFFFFD27A), fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, fontFamily = DisplayFont)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.scale_title), color = Color.White, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.scale_hint), color = Color(0xFFBFA98A), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RatingScale.entries.forEach { sc ->
                        FilterChip(selected = scale == sc, onClick = { vm.setRatingScale(sc) }, label = { Text(sc.label) })
                    }
                }
                Text(stringResource(R.string.template_title), color = Color.White, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.template_hint), color = Color(0xFFBFA98A), fontSize = 12.sp)
                CriteriaEditor(template, scale, withScores = false)
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { template.clear(); template.addAll(com.repisa.app.model.DefaultCriteria) }) {
                    Text(stringResource(R.string.restore_template))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                Button(onClick = { saveTemplate(vm.settings, template.filter { it.name.isNotBlank() }); onDismiss() }) {
                    Text(stringResource(R.string.save_template))
                }
            }
        }
    }
}
