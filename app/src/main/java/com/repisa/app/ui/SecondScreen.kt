package com.repisa.app.ui

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.repisa.app.R
import com.repisa.app.i18n.L10n
import com.repisa.app.model.formatPlaytime
import com.repisa.app.model.label
import com.repisa.app.model.playStatus
import java.text.DateFormat
import java.util.Date

/**
 * Muestra la ficha del juego seleccionado en la otra pantalla (la inferior de la AYN Thor).
 * Usa la API Presentation de Android, que funciona con cualquier pantalla secundaria.
 */
class SecondScreen(private val activity: ComponentActivity, private val vm: LibraryViewModel) {
    private val displayManager = activity.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var presentation: Presentation? = null

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = update()
        override fun onDisplayRemoved(displayId: Int) = update()
        override fun onDisplayChanged(displayId: Int) = Unit
    }

    fun start() {
        displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        update()
    }

    fun stop() {
        displayManager.unregisterDisplayListener(listener)
        presentation?.dismiss()
        presentation = null
    }

    fun update() {
        val target = if (vm.settings.secondScreen) findSecondDisplay() else null
        if (target == null) {
            presentation?.dismiss(); presentation = null; return
        }
        if (presentation?.display?.displayId == target.displayId) return
        presentation?.dismiss()
        presentation = runCatching {
            Presentation(activity, target).apply {
                val view = ComposeView(context).apply {
                    setViewTreeLifecycleOwner(activity)
                    setViewTreeViewModelStoreOwner(activity)
                    setViewTreeSavedStateRegistryOwner(activity)
                    setContent { com.repisa.app.i18n.Localized { RepisaTheme { FocusPanel(vm) } } }
                }
                setContentView(view)
                show()
            }
        }.getOrNull()
    }

    private fun findSecondDisplay(): Display? {
        val current = activity.display?.displayId ?: Display.DEFAULT_DISPLAY
        val candidates = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).toList() +
            displayManager.displays.toList()
        return candidates.firstOrNull { it.displayId != current && it.state != Display.STATE_OFF }
    }
}

@Composable
private fun FocusPanel(vm: LibraryViewModel) {
    val game by vm.focused.collectAsState()
    val all by vm.displayGames.collectAsState()
    Box(Modifier.fillMaxSize().roomBackground().padding(20.dp)) {
        val g = game
        if (g == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Repisa", color = Color(0xFFFFD27A), fontSize = 40.sp, fontWeight = FontWeight.Black, fontFamily = DisplayFont)
                Text(L10n.games(all.size) + " · " + stringResource(R.string.hours_played, all.sumOf { it.totalMinutes } / 60),
                    color = Color(0xFFBFA98A))
                // Resumen por estado: ⏳ 12 · ▶ 3 · ✓ 40 ...
                val byStatus = androidx.compose.runtime.remember(all) { all.mapNotNull { it.playStatus }.groupingBy { it }.eachCount() }
                if (byStatus.isNotEmpty()) Text(
                    com.repisa.app.model.PlayStatus.entries.filter { it in byStatus }.joinToString("  ·  ") { "${it.icon} ${it.label} ${byStatus[it]}" },
                    color = Color(0xFFE8D9C0), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.height(12.dp))
                val top = androidx.compose.runtime.remember(all) {
                    all.filter { it.playStatus != com.repisa.app.model.PlayStatus.NOT_APPLICABLE }.sortedByDescending { it.totalMinutes }.take(3)
                }
                if (top.isNotEmpty()) {
                    Text(stringResource(R.string.most_played), color = Color.White, fontWeight = FontWeight.Bold)
                    top.forEach { Text("${it.title} — ${formatPlaytime(it.totalMinutes)}", color = Color(0xFFD9C8AE), fontSize = 13.sp) }
                }
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val scale by vm.ratingScale.collectAsState()
                val shown = all.firstOrNull { it.id == g.id } ?: g // con las copias unidas
                // Globo con Mi nota y tiempo jugado justo encima de la carátula: la caja se reduce lo
                // necesario para que el globo quepa sin taparla ni salirse de su ancho.
                val bubble = 30.dp
                val gap = 8.dp
                val h = minOf((maxHeight - bubble - gap) * 0.98f, maxWidth * 0.42f / frontAspect(shown))
                val boxW = boxSize(shown, h).first
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.width(boxW), horizontalAlignment = Alignment.Start) {
                        StatsBubble(shown, scale, Modifier.height(bubble))
                        Spacer(Modifier.height(gap))
                        GameBox(shown, h, lifted = false)
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(g.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black,
                            fontFamily = DisplayFont, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text("${g.console.label} · ${g.source.label}", color = Color(0xFFFFD27A), fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Line(stringResource(R.string.stat_playtime), formatPlaytime(shown.totalMinutes))
                        Line(stringResource(R.string.stat_last_played), shown.lastPlayedAny?.let { DateFormat.getDateInstance().format(Date(it)) } ?: "—")
                        Line(stringResource(R.string.stat_genre), g.genres.joinToString(", ") { L10n.genre(it) }.ifEmpty { "—" })
                        Line(stringResource(R.string.stat_studio), g.developer ?: "—")
                        Line(stringResource(R.string.stat_year), g.releaseYear?.toString() ?: "—")
                        g.description?.let {
                            Text(it.replace(Regex("<[^>]+>"), ""), color = Color(0xFFD9C8AE), fontSize = 12.sp,
                                maxLines = 5, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp)
                                    .background(Color(0x33000000)).padding(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row {
        Text("$label: ", color = Color(0xFF9C8A70), fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Globo pequeño: mi nota (con estrellas) y tiempo jugado. */
@Composable
private fun StatsBubble(g: com.repisa.app.model.Game, scale: com.repisa.app.model.RatingScale, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(Color(0xF02A1C14), androidx.compose.foundation.shape.RoundedCornerShape(50))
            .border(1.dp, Color(0x66FFD27A), androidx.compose.foundation.shape.RoundedCornerShape(50))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        g.rating?.let { r ->
            Text(scale.format(r.overall), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Stars(r.stars, 10.dp)
            Text("·", color = Color(0xFF9C8A70), fontSize = 13.sp)
        }
        Text("⏱ " + formatPlaytime(g.totalMinutes), color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
