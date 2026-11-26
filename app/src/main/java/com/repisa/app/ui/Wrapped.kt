package com.repisa.app.ui

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.R
import com.repisa.app.data.PlaySession
import com.repisa.app.data.dayKey
import com.repisa.app.i18n.L10n
import com.repisa.app.model.Game
import com.repisa.app.model.PlayStatus
import com.repisa.app.model.formatPlaytime
import com.repisa.app.model.label
import com.repisa.app.model.playStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar

/**
 * El «año» de Repisa va del 8 de octubre al 8 de octubre. El resumen de cada año sale el 8 de octubre y se puede ver
 * hasta el siguiente 8 de octubre, cuando lo sustituye el nuevo. El primero solo sale si Repisa ya se usaba al menos
 * medio año antes de que terminara (si no, apenas tendría datos).
 */
object WrappedPeriod {
    /** Día y mes en que termina cada año de Repisa. */
    private const val END_MONTH = Calendar.OCTOBER
    private const val END_DAY = 8
    private const val MIN_USE_MS = 182L * 24 * 3600 * 1000

    /** Inicio, fin (sin incluir) y año del periodo. */
    data class Period(val start: Long, val end: Long, val year: Int)

    private fun endOf(year: Int): Long = Calendar.getInstance().apply {
        clear(); set(year, END_MONTH, END_DAY, 0, 0, 0)
    }.timeInMillis

    /** El último año de Repisa ya terminado (aunque aún no esté disponible). */
    fun latest(now: Long = System.currentTimeMillis()): Period {
        val y = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)
        val endYear = if (now >= endOf(y)) y else y - 1
        return Period(endOf(endYear - 1), endOf(endYear), endYear)
    }

    /** El resumen que se puede ver ahora, o null si todavía no hay ninguno. */
    fun current(firstUse: Long, now: Long = System.currentTimeMillis()): Period? =
        latest(now).takeIf { firstUse <= it.end - MIN_USE_MS }

    /** Cuándo saldrá el próximo resumen (para avisar mientras no hay ninguno). */
    fun nextDate(firstUse: Long, now: Long = System.currentTimeMillis()): Long {
        var y = latest(now).year + 1
        while (firstUse > endOf(y) - MIN_USE_MS) y++
        return endOf(y)
    }
}

/** Los datos del resumen del año. */
data class WrappedData(
    val year: Int,
    val minutes: Int,
    val sessions: Int,
    val days: Int,
    val top: List<Pair<Game, Int>>,
    /** Minutos de cada mes, empezando por el mes en que empieza el periodo ([firstMonth]). */
    val months: IntArray,
    val firstMonth: Int,
    val topGenre: String?,
    val topConsole: String?,
    val finished: Int,
    val streak: Int,
    val games: Int,
) {
    companion object {
        fun of(period: WrappedPeriod.Period, sessions: List<PlaySession>, games: List<Game>, sample: Boolean): WrappedData {
            val byId = buildMap { games.forEach { g -> put(g.id, g); g.copyIds.forEach { put(it, g) } } }
            val cal = Calendar.getInstance()
            fun inPeriod(t: Long) = t >= period.start && t < period.end
            val mine = sessions.filter { inPeriod(it.start) }
            cal.timeInMillis = period.start
            val firstMonth = cal.get(Calendar.MONTH)
            val perGame = mine.groupBy { byId[it.gameId] }.filterKeys { it != null }
                .map { (g, l) -> g!! to l.sumOf { it.minutes } }.sortedByDescending { it.second }
            val months = IntArray(12)
            mine.forEach { cal.timeInMillis = it.start; months[(cal.get(Calendar.MONTH) - firstMonth + 12) % 12] += it.minutes }
            val genre = perGame.flatMap { (g, m) -> g.genres.map { it to m } }.groupBy({ it.first }, { it.second })
                .maxByOrNull { it.value.sum() }?.key?.let { L10n.genre(it) }
            val console = perGame.groupBy({ it.first.console.label }, { it.second }).maxByOrNull { it.value.sum() }?.key
            // Racha: más días seguidos con alguna partida.
            val days = mine.map { dayKey(it.start) }.distinct().sorted()
            var best = 0; var run = 0; var prev = -10
            days.forEach { d -> run = if (d == prev + 1) run + 1 else 1; best = maxOf(best, run); prev = d }
            val finished = if (sample) games.count { it.playStatus == PlayStatus.FINISHED || it.playStatus == PlayStatus.COMPLETED }.coerceAtLeast(7)
            else games.count { g ->
                (g.playStatus == PlayStatus.FINISHED || g.playStatus == PlayStatus.COMPLETED) &&
                    g.lastPlayedAny?.let { inPeriod(it) } == true
            }
            return WrappedData(period.year, mine.sumOf { it.minutes }, mine.size, days.size, perGame.take(5), months, firstMonth, genre, console,
                finished, best, perGame.size)
        }
    }
}

/** Solo en la versión de pruebas: abrir en una pantalla concreta y sin pasar sola (para capturas). */
object WrappedDebug { var slide = -1 }

private val Gold = Color(0xFFFFD27A)
private val Ink = Color(0xFFF3E6D3)
private val Muted = Color(0xFFBFA98A)
private const val SLIDES = 6
private val CARD_W = 640.dp
private val CARD_H = 360.dp
private const val SLIDE_MS = 6500L

/**
 * Repisa Wrapped: el año en juegos en seis pantallas animadas que pasan solas (o tocando / con el mando),
 * y una tarjeta final que se puede guardar o compartir como imagen.
 */
@Composable
fun WrappedScreen(vm: LibraryViewModel, sample: Boolean, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val real by vm.sessions.collectAsState()
    val games by vm.displayGames.collectAsState()
    // El año de Repisa que toca (o, de ejemplo, el último terminado).
    val period = remember { WrappedPeriod.current(vm.firstUse) ?: WrappedPeriod.latest() }
    val data = remember(sample, real, games) { WrappedData.of(period, if (sample) vm.sampleSessions() else real, games, sample) }
    var slide by remember { mutableIntStateOf(WrappedDebug.slide.coerceAtLeast(0)) }
    // Música de fondo en bucle: entra subiendo el volumen y se apaga al salir. Y (o 🔊) la silencia.
    val context = LocalContext.current
    var muted by remember { androidx.compose.runtime.mutableStateOf(false) }
    val player = remember {
        runCatching { android.media.MediaPlayer.create(context, R.raw.wrapped_music)?.apply { isLooping = true; setVolume(0f, 0f); start() } }.getOrNull()
    }
    androidx.compose.runtime.DisposableEffect(player) { onDispose { runCatching { player?.stop(); player?.release() } } }
    LaunchedEffect(muted) {
        val target = if (muted) 0f else 0.6f
        var v = if (muted) 0.6f else 0f
        repeat(20) { v += (target - v) * 0.25f; runCatching { player?.setVolume(v, v) }; delay(60) }
        runCatching { player?.setVolume(target, target) }
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    // Pasa sola, salvo la última (la tarjeta para compartir).
    LaunchedEffect(slide) { if (slide < SLIDES - 1 && WrappedDebug.slide < 0) { delay(SLIDE_MS); slide++ } }

    Box(
        Modifier.fillMaxSize().roomBackground().blockUnderneath()
            .focusRequester(focus).focusable()
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyUp) return@onKeyEvent false
                when (e.key.nativeKeyCode) {
                    // En la tarjeta final, las flechas y A son para sus botones.
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_BUTTON_A -> if (slide < SLIDES - 1) { slide++; true } else false
                    KeyEvent.KEYCODE_BUTTON_R1 -> { if (slide < SLIDES - 1) slide++; true }
                    KeyEvent.KEYCODE_DPAD_LEFT -> if (slide in 1 until SLIDES - 1) { slide--; true } else false
                    KeyEvent.KEYCODE_BUTTON_L1 -> { if (slide > 0) slide--; true }
                    KeyEvent.KEYCODE_BUTTON_B -> { onClose(); true }
                    KeyEvent.KEYCODE_BUTTON_Y -> { muted = !muted; true }
                    else -> false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { o -> if (o.x < size.width * 0.3f) { if (slide > 0) slide-- } else if (slide < SLIDES - 1) slide++ }
            },
    ) {
        // Oscurece un poco la vitrina para que se lea bien.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x88000000), Color(0x33000000), Color(0xAA000000)))))
        AnimatedContent(
            slide,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(500)) { it * dir / 3 } + fadeIn(tween(500))) togetherWith
                    (slideOutHorizontally(tween(400)) { -it * dir / 3 } + fadeOut(tween(300)))
            },
            label = "wrapped",
            modifier = Modifier.fillMaxSize().padding(top = 36.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
        ) { s ->
            when (s) {
                0 -> IntroSlide(data)
                1 -> HoursSlide(data)
                2 -> TopSlide(data)
                3 -> MonthsSlide(data)
                4 -> FactsSlide(data)
                else -> FinalSlide(data, sample, onClose)
            }
        }
        // Barritas de progreso arriba, como las historias.
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 60.dp, top = 14.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until SLIDES) {
                val fill = remember(slide) { Animatable(if (i < slide) 1f else 0f) }
                LaunchedEffect(slide) { if (i == slide) fill.animateTo(1f, tween(if (i == SLIDES - 1) 300 else SLIDE_MS.toInt(), easing = androidx.compose.animation.core.LinearEasing)) }
                Box(Modifier.weight(1f).height(4.dp).background(Color(0x44FFFFFF), RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth(fill.value).height(4.dp).background(Gold, RoundedCornerShape(2.dp)))
                }
            }
        }
        if (sample) Text(stringResource(R.string.preview_sample_tag), color = Muted, fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = 64.dp))
        Text(if (muted) "🔇" else "🔊", fontSize = 18.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 14.dp)
            .clip(RoundedCornerShape(8.dp)).clickable { muted = !muted }.padding(6.dp))
    }
}

/** Entra con un pequeño rebote, con [delayMs] de retraso. */
@Composable
private fun pop(delayMs: Int): Float {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); a.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
    return a.value
}

@Composable
private fun Title(text: String, delayMs: Int = 0, size: Int = 34) {
    val p = pop(delayMs)
    Text(text, color = Gold, fontSize = size.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont, textAlign = TextAlign.Center,
        modifier = Modifier.graphicsLayer { alpha = p.coerceIn(0f, 1f); translationY = (1f - p) * 40f })
}

@Composable
private fun Sub(text: String, delayMs: Int = 0) {
    val p = pop(delayMs)
    Text(text, color = Ink, fontSize = 17.sp, textAlign = TextAlign.Center,
        modifier = Modifier.graphicsLayer { alpha = p.coerceIn(0f, 1f) })
}

// ── 1. Portada: cajas que caen sobre la balda
@Composable
private fun IntroSlide(d: WrappedData) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Title(stringResource(R.string.wrapped_intro_title, d.year), size = 44)
        Spacer(Modifier.height(6.dp))
        Sub(stringResource(R.string.wrapped_intro_sub), 300)
        Spacer(Modifier.height(24.dp))
        Column(Modifier.widthIn(max = 620.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
                d.top.forEachIndexed { i, (g, _) ->
                    val drop = remember { Animatable(-500f) }
                    LaunchedEffect(Unit) { delay(500L + i * 220L); drop.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
                    GameBox(g, 120.dp, showStatus = false, modifier = Modifier.graphicsLayer { translationY = drop.value })
                }
            }
            WoodPlank(modifier = Modifier.fillMaxWidth().height(30.dp))
        }
    }
}

// ── 2. Horas: el número cuenta hacia arriba
@Composable
private fun HoursSlide(d: WrappedData) {
    val count = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(300); count.animateTo(d.minutes / 60f, tween(2200, easing = FastOutSlowInEasing)) }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Sub(stringResource(R.string.wrapped_hours_pre, d.year))
        Text(L10n.decimal(count.value.toDouble(), 0), color = Gold, fontSize = 110.sp, fontWeight = FontWeight.Black, fontFamily = DisplayFont)
        Title(stringResource(R.string.wrapped_hours_unit), 200, 30)
        Spacer(Modifier.height(10.dp))
        Sub(stringResource(R.string.wrapped_hours_detail, d.sessions, d.days, d.games), 1400)
        if (d.minutes >= 24 * 60) Sub(stringResource(R.string.wrapped_hours_days, L10n.decimal(d.minutes / 1440.0, 1)), 2200)
    }
}

// ── 3. Top 5: las carátulas suben a la balda una a una, de la quinta a la primera
@Composable
private fun TopSlide(d: WrappedData) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Title(stringResource(R.string.wrapped_top_title))
        Spacer(Modifier.height(18.dp))
        if (d.top.isEmpty()) { Sub(stringResource(R.string.wrapped_nothing)); return@Column }
        BoxWithConstraints(Modifier.fillMaxWidth().widthIn(max = 900.dp)) {
            val h: Dp = minOf(170.dp, maxWidth / 6.2f * 1.45f)
            val colW = maxWidth / 5.4f
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                    d.top.forEachIndexed { i, (g, m) ->
                        val p = pop(400 + (d.top.size - 1 - i) * 450)
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.graphicsLayer { alpha = p.coerceIn(0f, 1f); translationY = (1f - p) * 160f }) {
                            Text("#${i + 1}", color = if (i == 0) Gold else Ink, fontSize = if (i == 0) 26.sp else 18.sp, fontWeight = FontWeight.Black)
                            GameBox(g, if (i == 0) h else h * 0.82f, showStatus = false)
                        }
                    }
                }
                WoodPlank(modifier = Modifier.fillMaxWidth().height(30.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    d.top.forEachIndexed { i, (g, m) ->
                        val p = pop(400 + (d.top.size - 1 - i) * 450 + 200)
                        Column(Modifier.width(colW).graphicsLayer { alpha = p.coerceIn(0f, 1f) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(g.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            Text(formatPlaytime(m), color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── 4. Mes a mes: barras que crecen
@Composable
private fun MonthsSlide(d: WrappedData) {
    val short = remember { DateFormatSymbols.getInstance().shortMonths.take(12).map { it.trimEnd('.').replaceFirstChar { c -> c.uppercase() } } }
    // En el orden del año de Repisa: de octubre a septiembre.
    val names = remember(d.firstMonth) { List(12) { short[(d.firstMonth + it) % 12] } }
    val best = d.months.indices.maxByOrNull { d.months[it] } ?: 0
    val max = (d.months.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Title(stringResource(R.string.wrapped_months_title))
        if (d.months[best] > 0) Sub(stringResource(R.string.wrapped_months_best,
            DateFormatSymbols.getInstance().months[(d.firstMonth + best) % 12], formatPlaytime(d.months[best])), 300)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().widthIn(max = 900.dp).height(260.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            d.months.forEachIndexed { i, m ->
                val grow = remember { Animatable(0f) }
                LaunchedEffect(Unit) { delay(500L + i * 90L); grow.animateTo(m.toFloat() / max, tween(900, easing = FastOutSlowInEasing)) }
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    if (m > 0 && grow.value > 0.05f) Text("${m / 60}h", color = if (i == best) Gold else Muted, fontSize = 11.sp)
                    Box(Modifier.fillMaxWidth().fillMaxHeight(grow.value * 0.85f + 0.005f)
                        .background(if (i == best) Brush.verticalGradient(listOf(Color(0xFFFFE3A0), Color(0xFFE0A84A)))
                            else Brush.verticalGradient(listOf(Color(0xFFC08A55), Color(0xFF6E4422))), RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)))
                    Text(names[i], color = if (i == best) Gold else Ink, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

// ── 5. Datos curiosos: fichas que aparecen una a una
@Composable
private fun FactsSlide(d: WrappedData) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Title(stringResource(R.string.wrapped_facts_title))
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FactTile("🎭", d.topGenre ?: "—", stringResource(R.string.wrapped_fact_genre), 300)
            FactTile("🎮", d.topConsole ?: "—", stringResource(R.string.wrapped_fact_console), 700)
            FactTile("🏁", d.finished.toString(), stringResource(R.string.wrapped_fact_finished), 1100)
            FactTile("🔥", stringResource(R.string.wrapped_fact_streak_value, d.streak), stringResource(R.string.wrapped_fact_streak), 1500)
        }
    }
}

@Composable
private fun FactTile(emoji: String, value: String, label: String, delayMs: Int) {
    val p = pop(delayMs)
    Column(
        Modifier.width(190.dp).graphicsLayer { scaleX = 0.6f + 0.4f * p; scaleY = 0.6f + 0.4f * p; alpha = p.coerceIn(0f, 1f) }
            .background(Color(0xE6261A12), RoundedCornerShape(14.dp)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 34.sp)
        Text(value, color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont, maxLines = 2,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(label, color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

// ── 6. Tarjeta final para guardar o compartir
@Composable
private fun FinalSlide(d: WrappedData, sample: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberCaptureLayer()
    val savedMsg = stringResource(R.string.share_saved)
    val failMsg = stringResource(R.string.share_failed)
    val shareText = stringResource(R.string.wrapped_share_text, d.year)
    var note by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val shareFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(400); runCatching { shareFocus.requestFocus() } }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        val p = pop(0)
        // La tarjeta se dibuja siempre a 640×360 (así sale la imagen) y se encoge para caber en la pantalla
        // (la de la AYN Thor es pequeña aunque sea 1080p).
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f, fill = false), contentAlignment = Alignment.Center) {
            val fit = minOf(1f, maxWidth / CARD_W, if (constraints.hasBoundedHeight) maxHeight / CARD_H else 1f)
            Box(Modifier.size(CARD_W * fit, CARD_H * fit), contentAlignment = Alignment.Center) {
                Box(Modifier.requiredSize(CARD_W, CARD_H).graphicsLayer {
                    val k = fit * (0.9f + 0.1f * p); scaleX = k; scaleY = k; alpha = p.coerceIn(0f, 1f)
                }) {
                    Capturable(layer) { WrappedCard(d) }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                scope.launch {
                    val ok = ImageExport.saveToGallery(context, captureBitmap(layer), "Repisa-Wrapped-${d.year}-${System.currentTimeMillis() / 1000}")
                    note = if (ok) savedMsg else failMsg
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0A84A), contentColor = Color(0xFF2A1C14))) {
                Text("💾 " + stringResource(R.string.share_save))
            }
            Button(onClick = { scope.launch { ImageExport.share(context, captureBitmap(layer), "Repisa-Wrapped-${d.year}", shareText) } },
                modifier = Modifier.focusRequester(shareFocus),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0A84A), contentColor = Color(0xFF2A1C14))) {
                Text("📤 " + stringResource(R.string.share_share))
            }
            OutlinedButton(onClick = onClose) { Text(stringResource(R.string.close), color = Color.White) }
        }
        note?.let { Text(it, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
    }
}

/** La tarjeta del año (la imagen que se guarda): portada, cifras, top 5 en la balda y mes a mes. */
@Composable
private fun WrappedCard(d: WrappedData) {
    Column(
        Modifier.size(CARD_W, CARD_H).roomBackground().padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.wrapped_card_title, d.year), color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
                Text(stringResource(R.string.wrapped_card_line, formatPlaytime(d.minutes), d.games, d.days), color = Ink, fontSize = 13.sp)
            }
            Text("Repisa", color = Gold.copy(alpha = 0.8f), fontSize = 18.sp, fontFamily = DisplayFont, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.weight(1f)) {
            Column(Modifier.weight(1.4f)) {
                // Las cinco caben siempre: el alto de las cajas se calcula con el ancho disponible.
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 6.dp), contentAlignment = Alignment.BottomStart) {
                    val gap = 6.dp
                    val n = d.top.size.coerceAtLeast(1)
                    val unit = d.top.sumOf { (g, _) -> boxSize(g, 100.dp).first.value.toDouble() }.toFloat().coerceAtLeast(1f)
                    val h = minOf(110.dp, 100.dp * ((maxWidth.value - gap.value * (n - 1)) / unit), maxHeight - 4.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Bottom) {
                        d.top.forEach { (g, _) -> GameBox(g, h, showStatus = false) }
                    }
                }
                WoodPlank(modifier = Modifier.fillMaxWidth().height(22.dp))
                d.top.firstOrNull()?.let { (g, m) ->
                    Text("🥇 ${g.title} · ${formatPlaytime(m)}", color = Ink, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).fillMaxHeight().background(Color(0x55000000), RoundedCornerShape(10.dp)).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CardFact("🎭", d.topGenre ?: "—")
                CardFact("🎮", d.topConsole ?: "—")
                CardFact("🏁", stringResource(R.string.wrapped_card_finished, d.finished))
                CardFact("🔥", stringResource(R.string.wrapped_card_streak, d.streak))
                val max = (d.months.maxOrNull() ?: 0).coerceAtLeast(1)
                Row(Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                    d.months.forEach { m ->
                        Box(Modifier.weight(1f).fillMaxHeight(m.toFloat() / max * 0.95f + 0.03f).background(Color(0xFFE0A84A), RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
    }
}

@Composable
private fun CardFact(emoji: String, text: String) {
    Text("$emoji  $text", color = Ink, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
