package com.repisa.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.i18n.L10n
import com.repisa.app.i18n.Localized
import com.repisa.app.model.Game
import com.repisa.app.model.formatPlaytime
import com.repisa.app.model.label
import com.repisa.app.ui.DisplayFont
import com.repisa.app.ui.GameBox
import com.repisa.app.ui.LibraryViewModel
import com.repisa.app.ui.RatingDialog
import com.repisa.app.ui.RepisaTheme
import com.repisa.app.ui.ScoresRow
import com.repisa.app.ui.boxSize
import com.repisa.app.ui.frontAspect
import com.repisa.app.ui.roomBackground
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

/**
 * Panel de la partida en la pantalla de abajo: mientras el juego corre arriba, aquí se ve y se puede tocar la ficha
 * del juego (tiempo de la sesión en vivo, hora, calificar, favorito, notas). Se cierra solo al volver a Repisa.
 */
class CompanionActivity : ComponentActivity() {
    private val vm: LibraryViewModel by lazy { (application as RepisaApp).library() }

    private fun noTransition(open: Boolean) {
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(if (open) OVERRIDE_TRANSITION_OPEN else OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }

    override fun finish() {
        super.finish()
        noTransition(open = false)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(L10n.wrap(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // El panel aparece y se va sin deslizarse.
        noTransition(open = true)
        setContent {
            Localized {
                RepisaTheme {
                    val playingId by vm.playing.collectAsState()
                    val games by vm.displayGames.collectAsState()
                    LaunchedEffect(playingId) { if (playingId == null) finish() }
                    val game = games.firstOrNull { it.id == playingId || playingId in it.copyIds }
                    if (game != null) CompanionPanel(game, vm, onClose = { vm.playing.value = null })
                }
            }
        }
    }
}

@Composable
private fun CompanionPanel(game: Game, vm: LibraryViewModel, onClose: () -> Unit) {
    val scale by vm.ratingScale.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val start = vm.settings.pendingSessionStart.takeIf { it > 0 } ?: now
    var rating by remember { mutableStateOf(false) }
    var editingNotes by remember { mutableStateOf(false) }
    var showAch by remember { mutableStateOf(false) }

    val elapsed = ((now - start) / 1000).coerceAtLeast(0)

    @Composable
    fun Clock(big: Boolean) {
        Text("⏱ %d:%02d:%02d".format(elapsed / 3600, elapsed / 60 % 60, elapsed % 60),
            color = Color(0xFFFFD27A), fontSize = if (big) 26.sp else 22.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.companion_session) + "  ·  🕐 " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now)),
            color = Color(0xFFBFA98A), fontSize = 13.sp)
    }

    // Lo de debajo de la carátula: notas, estado, botones... (igual en pantalla ancha o casi cuadrada)
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    @Composable
    fun Details() {
        ScoresRow(game, scale, small = false, onRate = { rating = true })
        com.repisa.app.ui.StatusPicker(game, small = true) { vm.setStatus(game, it) }
        com.repisa.app.ui.remainingText(game)?.let { Text(it, color = Color(0xFFBFA98A), fontSize = 13.sp) }
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { rating = true }) { Text("★ " + stringResource(R.string.rate), color = Color.White) }
            OutlinedButton(onClick = { vm.toggleFavorite(game) }) {
                Text((if (game.favorite) "♥ " else "♡ ") + stringResource(R.string.favorite), color = Color.White)
            }
            OutlinedButton(onClick = { editingNotes = true }) { Text("✎ " + stringResource(R.string.notes), color = Color.White) }
            OutlinedButton(onClick = { showAch = true }) {
                Text("🏆" + (game.achTotal?.takeIf { it > 0 }?.let { " ${game.achUnlocked ?: 0}/$it" } ?: ""), color = Color.White)
            }
        }
        // Notas personales, a lo ancho y con espacio: dónde lo dejaste, pistas, lo que quieras apuntar.
        Box(Modifier.fillMaxWidth().heightIn(min = 90.dp).background(Color(0x40000000), RoundedCornerShape(8.dp))
            .clickable { editingNotes = true }.padding(12.dp)) {
            Text(game.notes?.takeIf { it.isNotBlank() } ?: stringResource(R.string.notes_empty),
                color = if (game.notes.isNullOrBlank()) Color(0xFF9C8A70) else Color(0xFFE8D9C0), fontSize = 16.sp, lineHeight = 22.sp)
        }
        TextButton(onClick = onClose) { Text("✕ " + stringResource(R.string.companion_close), color = Color(0xFF9C8A70)) }
    }

    BoxWithConstraints(Modifier.fillMaxSize().roomBackground().padding(16.dp)) {
        // La pantalla de abajo de la AYN Thor es casi cuadrada: ahí todo va en columna para que nada quede apretado.
        val narrow = maxWidth < maxHeight * 1.4f
        val narrowH = minOf(maxHeight * 0.32f, maxWidth * 0.32f / frontAspect(game))
        if (narrow) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val h = narrowH
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    GameBox(game, h, lifted = false)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(game.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = DisplayFont,
                            maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text("${game.console.label} · " + formatPlaytime(game.totalMinutes), color = Color(0xFFFFD27A), fontSize = 14.sp)
                        Clock(big = true)
                    }
                }
                Details()
            }
        } else {
            val h = minOf(maxHeight * 0.62f, maxWidth * 0.30f / frontAspect(game))
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(Modifier.width(boxSize(game, h).first), horizontalAlignment = Alignment.CenterHorizontally) {
                    GameBox(game, h, lifted = false)
                    Spacer(Modifier.height(10.dp))
                    Clock(big = false)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(game.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = DisplayFont,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${game.console.label} · " + stringResource(R.string.stat_playtime) + ": " + formatPlaytime(game.totalMinutes),
                        color = Color(0xFFFFD27A), fontSize = 13.sp)
                    Details()
                }
            }
        }
    }
    if (rating) RatingDialog(game, vm, onDismiss = { rating = false })
    if (showAch) com.repisa.app.ui.AchievementsDialog(game, vm, onDismiss = { showAch = false })
    if (editingNotes) NotesDialog(game.notes.orEmpty(), onDismiss = { editingNotes = false }) {
        vm.updateGame(game.copy(notes = it.ifBlank { null })); editingNotes = false
    }
}

@Composable
fun NotesDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notes)) },
        text = {
            OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                placeholder = { Text(stringResource(R.string.notes_hint)) })
        },
        confirmButton = { TextButton(onClick = { onSave(text.trim()) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
