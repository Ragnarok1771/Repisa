package com.repisa.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.R
import com.repisa.app.model.Game
import com.repisa.app.model.Source

/**
 * Animación al pulsar «Jugar»: la caja se abre, el cartucho o el disco vuela a su consola, la pantalla se enciende
 * y la cámara se acerca a ella hasta que se abre el juego. Todo dibujado con código (sin logos ni sonidos de
 * Nintendo o Sony: las consolas se reconocen por su forma y los sonidos son propios).
 *
 * Se juega en una escena fija de 1600 × 900 unidades que se escala a la pantalla.
 */
enum class LaunchKind(val cues: List<Pair<Float, Int>>, val screen: SceneRect, val durationMs: Int = 3600, val zoomStart: Float = 0.68f) {
    /** Juegos de PC (Steam, Epic): la caja entra en el monitor. */
    PC(listOf(0.15f to R.raw.anim_whoosh, 0.48f to R.raw.anim_pc), SceneRect(780f, 160f, 680f, 382f), zoomStart = 0.64f),

    /** Cartucho pequeño en una consola con pantalla (Switch y Switch 2). */
    SWITCH(listOf(0.13f to R.raw.anim_click, 0.30f to R.raw.anim_whoosh, 0.57f to R.raw.anim_click, 0.61f to R.raw.anim_chime),
        SceneRect(990f, 446f, 400f, 228f)),

    /** PS2 Slim: se abre la tapa de arriba, entra el disco y se cierra; luego la tele. */
    PS2(listOf(0.12f to R.raw.anim_click, 0.21f to R.raw.anim_click, 0.29f to R.raw.anim_whoosh, 0.50f to R.raw.anim_click,
        0.58f to R.raw.anim_spin, 0.68f to R.raw.anim_chime), SceneRect(800f, 90f, 680f, 360f), durationMs = 4800, zoomStart = 0.76f),

    /** Portátiles de cartucho de Nintendo: Game Boy, Game Boy Color, Game Boy Advance, DS y 3DS (estas dos se abren). */
    HANDHELD(listOf(0.13f to R.raw.anim_click, 0.30f to R.raw.anim_whoosh, 0.56f to R.raw.anim_click, 0.63f to R.raw.anim_chime),
        SceneRect(0f, 0f, 1f, 1f), durationMs = 4000, zoomStart = 0.72f),

    /** GameCube: se levanta la tapa, entra el minidisco y se cierra. */
    GAMECUBE(listOf(0.12f to R.raw.anim_click, 0.21f to R.raw.anim_click, 0.29f to R.raw.anim_whoosh, 0.50f to R.raw.anim_click,
        0.56f to R.raw.anim_spin, 0.66f to R.raw.anim_chime), SceneRect(760f, 70f, 620f, 340f), durationMs = 4600, zoomStart = 0.74f),

    /** PlayStation original: tapa redonda arriba, como la GameCube. */
    PS1(listOf(0.12f to R.raw.anim_click, 0.21f to R.raw.anim_click, 0.29f to R.raw.anim_whoosh, 0.50f to R.raw.anim_click,
        0.56f to R.raw.anim_spin, 0.66f to R.raw.anim_chime), SceneRect(800f, 90f, 680f, 360f), durationMs = 4600, zoomStart = 0.74f),

    /** PS3: el disco entra de canto por la ranura del frente. */
    PS3(listOf(0.12f to R.raw.anim_click, 0.29f to R.raw.anim_whoosh, 0.46f to R.raw.anim_spin, 0.62f to R.raw.anim_chime),
        SceneRect(800f, 90f, 680f, 360f), durationMs = 4400, zoomStart = 0.72f),

    /** Xbox: bandeja que sale hacia delante. */
    XBOX(listOf(0.12f to R.raw.anim_click, 0.19f to R.raw.anim_tray, 0.29f to R.raw.anim_whoosh, 0.50f to R.raw.anim_tray,
        0.61f to R.raw.anim_spin, 0.69f to R.raw.anim_chime), SceneRect(800f, 90f, 680f, 360f), durationMs = 4800, zoomStart = 0.76f),

    /** Consolas de cartucho de sobremesa (NES, Super Nintendo, N64, Mega Drive, Master System, Atari, Neo Geo, PC Engine). */
    CART(listOf(0.12f to R.raw.anim_click, 0.30f to R.raw.anim_whoosh, 0.55f to R.raw.anim_click, 0.63f to R.raw.anim_chime),
        SceneRect(800f, 90f, 680f, 360f), durationMs = 4200, zoomStart = 0.72f),

    /** Recreativa: la moneda entra por la ranura y se enciende la pantalla. */
    ARCADE(listOf(0.40f to R.raw.anim_coin, 0.53f to R.raw.anim_chime), SceneRect(1040f, 200f, 220f, 172f), durationMs = 3800, zoomStart = 0.64f),

    /** Ordenador antiguo y motores de juego: el disquete entra en la disquetera y se enciende el monitor de tubo. */
    COMPUTER(listOf(0.12f to R.raw.anim_click, 0.30f to R.raw.anim_whoosh, 0.50f to R.raw.anim_click, 0.53f to R.raw.anim_tray,
        0.61f to R.raw.anim_pc), SceneRect(1004f, 238f, 216f, 164f), durationMs = 4200, zoomStart = 0.70f),

    /** Wii: el disco entra de canto por la ranura y la ranura se ilumina. */
    WII(listOf(0.12f to R.raw.anim_click, 0.29f to R.raw.anim_whoosh, 0.48f to R.raw.anim_spin, 0.62f to R.raw.anim_chime),
        SceneRect(700f, 90f, 620f, 340f), durationMs = 4400, zoomStart = 0.72f);

    /** La pantalla a la que se acerca la cámara al final. */
    fun screenFor(game: Game): SceneRect = if (this == HANDHELD) Handheld.of(game.consoleId).zoomScreen else screen

    companion object {
        /** Qué animación lleva un juego (null = de momento ninguna: se abre directamente). */
        fun of(game: Game): LaunchKind? = when {
            game.source != Source.EMULATOR || game.consoleId == "pc" -> PC
            game.consoleId == "switch" || game.consoleId == "switch2" -> SWITCH
            game.consoleId == "ps2" -> PS2
            game.consoleId in Handheld.consoles -> HANDHELD
            game.consoleId == "gc" -> GAMECUBE
            game.consoleId == "wii" -> WII
            game.consoleId in setOf("psx", "dreamcast", "saturn", "pcfx") -> PS1
            game.consoleId in SlotConsole.consoles -> PS3
            game.consoleId in TrayConsole.consoles -> XBOX
            game.consoleId == "ps5" -> WII
            game.consoleId in CartConsole.consoles -> CART
            game.consoleId == "arcade" -> ARCADE
            game.consoleId in setOf("dos", "amstradcpc", "apple2", "atarist", "c64", "amiga", "msx", "zxspectrum", "x68000", "scummvm", "engines") -> COMPUTER
            else -> null
        }
    }
}

/** Rectángulo en unidades de la escena (1600 × 900). */
data class SceneRect(val x: Float, val y: Float, val w: Float, val h: Float) {
    val cx get() = x + w / 2
    val cy get() = y + h / 2
}

const val LAUNCH_ANIM_MS = 3600

/** Solo para revisar fotogramas en la versión de pruebas. */
object LaunchDebug { var freezeAt: Float? by androidx.compose.runtime.mutableStateOf(null) }
internal const val SCENE_W = 1600f
internal const val SCENE_H = 900f
internal const val TABLE_Y = 700f

internal fun seg(p: Float, a: Float, b: Float) = ((p - a) / (b - a)).coerceIn(0f, 1f)
internal fun ease(t: Float) = FastOutSlowInEasing.transform(t)
internal fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

@Composable
fun LaunchAnimation(game: Game, kind: LaunchKind, onFinish: () -> Unit) {
    val finish by rememberUpdatedState(onFinish)
    val progress = remember(game.id, kind) { Animatable(0f) }
    val context = LocalContext.current
    val sounds = remember { LaunchSounds(context) }
    DisposableEffect(Unit) { onDispose { sounds.release() } }
    val frozen = LaunchDebug.freezeAt
    LaunchedEffect(game.id, kind, frozen) {
        frozen?.let { progress.snapTo(it); return@LaunchedEffect }
        var next = 0
        progress.animateTo(1f, tween(kind.durationMs, easing = LinearEasing)) {
            while (next < kind.cues.size && value >= kind.cues[next].first) { sounds.play(kind.cues[next].second); next++ }
            // El juego se abre ya casi en negro; mientras carga, la pantalla se queda en negro.
            if (value >= 0.97f) finish()
        }
        finish()
    }
    val p = progress.value

    // Tocar la pantalla (o cualquier botón del mando, ver MainActivity) salta la animación y abre el juego.
    Box(Modifier.fillMaxSize().roomBackground().background(Color(0xB3000000)).pointerInput(Unit) { detectTapGestures { finish() } }) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val u: Dp = minOf(maxWidth / SCENE_W, maxHeight / SCENE_H)
            val unitPx = with(LocalDensity.current) { u.toPx() }
            // Acercamiento final a la pantalla de la consola: se agranda y se lleva al centro.
            val screen = kind.screenFor(game)
            val z = ease(seg(p, kind.zoomStart, 0.97f))
            val zoomEnd = SCENE_W / screen.w * 1.08f
            Box(
                Modifier.size(u * SCENE_W, u * SCENE_H).graphicsLayer {
                    val s = lerp(1f, zoomEnd, z * z)
                    scaleX = s; scaleY = s
                    transformOrigin = TransformOrigin(screen.cx / SCENE_W, screen.cy / SCENE_H)
                    translationX = (SCENE_W / 2 - screen.cx) * unitPx * z
                    translationY = (SCENE_H / 2 - screen.cy) * unitPx * z
                },
            ) {
                Table(u)
                when (kind) {
                    LaunchKind.PC -> PcScene(game, p, u)
                    LaunchKind.SWITCH -> SwitchScene(game, p, u)
                    LaunchKind.PS2 -> TopLoaderScene(game, p, u, TopLoader.PS2_SLIM, tvOnAt = 0.68f)
                    LaunchKind.HANDHELD -> HandheldScene(game, p, u)
                    LaunchKind.GAMECUBE -> TopLoaderScene(game, p, u, TopLoader.GAMECUBE, tvOnAt = 0.66f)
                    LaunchKind.WII -> WiiScene(game, p, u)
                    LaunchKind.PS1 -> TopLoaderScene(game, p, u, TopLoader.of(game.consoleId), tvOnAt = 0.66f)
                    LaunchKind.PS3 -> SlotScene(game, p, u)
                    LaunchKind.CART -> CartScene(game, p, u)
                    LaunchKind.ARCADE -> ArcadeScene(game, p, u)
                    LaunchKind.COMPUTER -> ComputerScene(game, p, u)
                    LaunchKind.XBOX -> XboxScene(game, p, u)
                }
            }
        }
        // Fundido a negro justo antes de abrir el juego.
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = seg(p, 0.9f, 1f))))
        Text("▶ " + game.title, color = Color.White.copy(alpha = 0.85f * (1f - seg(p, 0.5f, 0.65f))), fontSize = 18.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp))
    }
}

// ------------------------------------------------------------------ Escenas

/** Mesa de madera sobre la que están las consolas. */
@Composable
private fun Table(u: Dp) {
    Box(
        Modifier.offset(0.dp, u * TABLE_Y).size(u * SCENE_W, u * (SCENE_H - TABLE_Y))
            .background(Brush.verticalGradient(listOf(Color(0xFF6B4226), Color(0xFF3E2414)))),
    )
    Box(Modifier.offset(0.dp, u * TABLE_Y).size(u * SCENE_W, u * 6).background(Color(0xFF8A5A36)))
}

/** La caja cerrada: aparece y (si [openAt] no es null) se abre como un libro. */
@Composable
internal fun ClosedCase(game: Game, u: Dp, x: Float, y: Float, h: Float, appear: Float, open: Float, inside: @Composable () -> Unit) {
    val boxWu = boxSize(game, u * h).first / u
    val boxHu = boxSize(game, u * h).second / u
    val frontW = h * frontAspect(game)
    val rise = boxHu - h
    Box(Modifier.offset(u * x, u * y).size(u * boxWu, u * boxHu).alpha(appear).graphicsLayer {
        scaleX = lerp(0.85f, 1f, appear); scaleY = scaleX
    }) {
        // Interior de la caja (se ve al abrirla), detrás de la tapa.
        if (open > 0f) Box(Modifier.offset(0.dp, u * rise).size(u * frontW, u * h)) { inside() }
        // Tapa: gira sobre el borde izquierdo como un libro hasta quedar de canto y desaparece.
        val angle = -105f * ease(open)
        Box(Modifier.size(u * boxWu, u * boxHu).alpha(1f - seg(open, 0.75f, 1f)).graphicsLayer {
            rotationY = angle
            transformOrigin = TransformOrigin(0f, 0.5f)
            cameraDistance = 14f * density
        }) { GameBox(game, u * h, showStatus = false) }
    }
}

@Composable
private fun PcScene(game: Game, p: Float, u: Dp) {
    val screen = LaunchKind.PC.screen
    Monitor(u, on = seg(p, 0.48f, 0.6f), game = game)
    // La caja aparece a la izquierda y vuela hasta meterse en la pantalla.
    val h = 380f
    val boxWu = boxSize(game, u * h).first / u
    val fly = ease(seg(p, 0.15f, 0.48f))
    val x = lerp(220f, screen.cx - boxWu / 2, fly)
    val y = lerp(TABLE_Y - h - 10f, screen.cy - h / 2, fly)
    val appear = ease(seg(p, 0f, 0.12f))
    Box(Modifier.offset(u * x, u * y).size(u * boxWu, u * h).alpha(appear * (1f - seg(p, 0.40f, 0.50f))).graphicsLayer {
        val s = lerp(0.85f, 1f, appear) * lerp(1f, 0.2f, fly)
        scaleX = s; scaleY = s
        rotationZ = lerp(0f, -8f, fly) * (1f - fly)
    }) { GameBox(game, u * h, showStatus = false) }
}

@Composable
private fun SwitchScene(game: Game, p: Float, u: Dp) {
    val screen = LaunchKind.SWITCH.screen
    val caseX = 170f; val caseY = 230f; val caseH = 440f
    val frontW = caseH * frontAspect(game)
    val boxWu = boxSize(game, u * caseH).first / u
    val open = seg(p, 0.13f, 0.30f)
    val caseAlpha = ease(seg(p, 0f, 0.1f)) * (1f - seg(p, 0.55f, 0.65f))
    // Cartucho: dentro de la caja → vuela encima de la ranura → baja y entra.
    val cw = 80f; val ch = 112f
    val rise = boxSize(game, u * caseH).second / u - caseH
    val startX = caseX + frontW / 2 - cw / 2
    val startY = caseY + rise + caseH * 0.28f
    val slotX = 1040f; val consoleTop = 436f
    val fly = ease(seg(p, 0.30f, 0.52f))
    val down = ease(seg(p, 0.52f, 0.58f))
    val scale = lerp(1f, 0.5f, fly)
    val cx = lerp(startX + cw / 2, slotX, fly)
    val cy = lerp(startY + ch / 2, consoleTop - ch * 0.5f * 0.5f - 30f, fly) - 120f * kotlin.math.sin(Math.PI.toFloat() * fly) + down * (ch * 0.5f + 30f)
    val cartridge = @Composable {
        Box(Modifier.offset(u * (cx - cw / 2), u * (cy - ch / 2)).size(u * cw, u * ch).graphicsLayer {
            scaleX = scale; scaleY = scale
            rotationZ = 14f * kotlin.math.sin(Math.PI.toFloat() * fly)
        }) { Cartridge(game, if (game.consoleId == "switch2") Color(0xFFC8102E) else Color(0xFF34363B)) }
    }

    ClosedCase(game, u, caseX, caseY, caseH, caseAlpha, open) {
        // Interior rojo de la caja con el hueco del cartucho (vacío cuando sale volando).
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(6)).background(Color(0xFFC62828))) {
            Box(Modifier.offset(u * (frontW / 2 - cw / 2 - 8), u * (caseH * 0.28f - 8)).size(u * (cw + 16), u * (ch + 16))
                .clip(RoundedCornerShape(10)).background(Color(0xFF8E1B1B)))
            Box(Modifier.offset(u * 20, u * (caseH * 0.62f)).size(u * (frontW - 40), u * (caseH * 0.3f))
                .clip(RoundedCornerShape(8)).background(Color(0xFFB71C1C)))
        }
    }
    // Al bajar, el cartucho queda detrás de la consola (entra por la ranura de arriba).
    if (p >= 0.52f) cartridge()
    SwitchConsole(u, on = seg(p, 0.60f, 0.68f), game = game, v2 = game.consoleId == "switch2")
    if (p in 0.30f..0.52f) cartridge()
    if (p < 0.30f && open > 0.5f) cartridge()
}

// ------------------------------------------------------------------ Consolas y objetos

/** Lo que muestra una pantalla al encenderse: la carátula sobre fondo oscuro, tras un destello. */
@Composable
internal fun ScreenContent(game: Game, on: Float) {
    Box(Modifier.fillMaxSize().background(Color(0xFF07090D))) {
        if (on > 0f) {
            Box(Modifier.fillMaxSize().alpha(ease(on)).background(Brush.radialGradient(listOf(Color(0xFF26324A), Color(0xFF0B0F18)))))
            Box(Modifier.align(Alignment.Center).fillMaxSize(0.86f).alpha(ease(seg(on, 0.2f, 1f)))) {
                BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val a = frontAspect(game)
                    val h = minOf(maxHeight, maxWidth / a)
                    Box(Modifier.size(h * a, h).clip(RoundedCornerShape(4))) { CoverArt(game) }
                }
            }
            // Destello blanco al encender.
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.8f * (1f - seg(on, 0f, 0.5f)) * if (on > 0f) 1f else 0f)))
        }
    }
}

@Composable
private fun Monitor(u: Dp, on: Float, game: Game) {
    val s = LaunchKind.PC.screen
    Canvas(Modifier.offset(u * 760, u * 140).size(u * 720, u * 560)) {
        val k = size.width / 720f
        withTransform({ scale(k, k, Offset.Zero) }) { drawMonitorBody(on) }
    }
    Box(Modifier.offset(u * s.x, u * s.y).size(u * s.w, u * s.h).clip(RoundedCornerShape(4.dp))) { ScreenContent(game, on) }
}

/** Monitor de PC (720 × 560: marco, cuello y pie; la pantalla va aparte). */
internal fun DrawScope.drawMonitorBody(on: Float) {
    drawRoundRect(Color(0xFF2A2D33), Offset(250f, 520f), Size(220f, 30f), CornerRadius(14f))
    drawRect(Color(0xFF34383F), Offset(330f, 420f), Size(60f, 105f))
    drawRoundRect(Color(0xFF1B1D22), Offset(0f, 0f), Size(720f, 422f), CornerRadius(18f))
    drawRoundRect(Color(0xFF3A3F48), Offset(0f, 0f), Size(720f, 422f), CornerRadius(18f), style = Stroke(3f))
    drawCircle(if (on > 0f) Color(0xFF7BD389) else Color(0xFF555555), 4f, Offset(360f, 412f))
}

@Composable
private fun SwitchConsole(u: Dp, on: Float, game: Game, v2: Boolean = false) {
    val s = LaunchKind.SWITCH.screen
    // 640 × 264 unidades, apoyada en la mesa.
    Canvas(Modifier.offset(u * 870, u * 436).size(u * 640, u * 264)) {
        val k = size.width / 640f
        withTransform({ scale(k, k, Offset.Zero) }) { drawSwitchBody(v2) }
    }
    Box(Modifier.offset(u * s.x, u * s.y).size(u * s.w, u * s.h).clip(RoundedCornerShape(3.dp))) { ScreenContent(game, on) }
}

/**
 * Switch / Switch 2 de frente (640 × 264, la pantalla va aparte), a partir de fotos: Joy-Con azul a la izquierda y
 * rojo a la derecha; el izquierdo con «−», palanca arriba, cuatro botones de dirección y captura; el derecho con «+»,
 * X/Y/A/B arriba, palanca debajo y el botón Home. Switch 2: mandos oscuros con aros de color bajo las palancas y botón C.
 */
internal fun DrawScope.drawSwitchBody(v2: Boolean) {
    val left = if (v2) Color(0xFF2A2C31) else Color(0xFF0AB9E6)
    val right = if (v2) Color(0xFF2A2C31) else Color(0xFFFF3C28)
    val btn = Color(0xFF1E1F23)
    val btnTop = Color(0xFF34363C)
    // Mandos, redondeados por fuera.
    drawRoundRect(left, Offset(0f, 0f), Size(124f, 264f), CornerRadius(58f))
    drawRect(left, Offset(62f, 0f), Size(62f, 264f))
    drawRoundRect(right, Offset(516f, 0f), Size(124f, 264f), CornerRadius(58f))
    drawRect(right, Offset(516f, 0f), Size(62f, 264f))
    // Brillo suave del plástico de los mandos.
    drawRoundRect(Brush.verticalGradient(listOf(Color(0x30FFFFFF), Color.Transparent), 0f, 120f), Offset(6f, 4f), Size(112f, 110f), CornerRadius(52f))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0x30FFFFFF), Color.Transparent), 0f, 120f), Offset(522f, 4f), Size(112f, 110f), CornerRadius(52f))
    // Cuerpo central: cristal negro con el marco de la pantalla.
    drawRoundRect(Color(0xFF1B1C20), Offset(104f, 0f), Size(432f, 264f), CornerRadius(8f))
    drawRoundRect(Color(0xFF2C2E33), Offset(104f, 0f), Size(432f, 264f), CornerRadius(8f), style = Stroke(2f))
    // Raíles donde encajan los mandos.
    drawRect(Color(0xFF111215), Offset(102f, 0f), Size(4f, 264f))
    drawRect(Color(0xFF111215), Offset(534f, 0f), Size(4f, 264f))
    // Ranura del cartucho, arriba a la izquierda.
    drawRoundRect(Color(0xFF0E0F12), Offset(140f, -2f), Size(60f, 8f), CornerRadius(3f))
    fun stick(c: Offset) {
        drawCircle(Color(0xFF15161A), 27f, c)
        drawCircle(Color(0xFF34363C), 20f, c)
        drawCircle(Color(0xFF2A2C31), 13f, c)
    }
    fun round(c: Offset, r: Float = 12f) { drawCircle(btn, r, c); drawCircle(btnTop, r - 3f, c.copy(y = c.y - 1f)) }
    // ── Mando izquierdo
    drawRoundRect(btn, Offset(74f, 16f), Size(22f, 6f), CornerRadius(3f))                  // −
    if (v2) drawCircle(Color(0xFF2FB8E8), 33f, Offset(58f, 70f))                            // aro azul (Switch 2)
    stick(Offset(58f, 70f))
    listOf(Offset(58f, 126f), Offset(58f, 178f), Offset(32f, 152f), Offset(84f, 152f)).forEach { round(it) }
    drawRoundRect(btn, Offset(49f, 214f), Size(18f, 18f), CornerRadius(4f))                // captura
    drawCircle(btnTop, 5f, Offset(58f, 223f))
    // ── Mando derecho
    drawRect(btn, Offset(544f, 16f), Size(22f, 6f)); drawRect(btn, Offset(552f, 8f), Size(6f, 22f)) // +
    listOf(Offset(582f, 44f), Offset(582f, 96f), Offset(556f, 70f), Offset(608f, 70f)).forEach { round(it) } // X B Y A
    if (v2) drawCircle(Color(0xFFFF4B3E), 33f, Offset(582f, 150f))                          // aro rojo (Switch 2)
    stick(Offset(582f, 150f))
    drawCircle(Color(0xFF4A4C52), 13f, Offset(570f, 214f))                                  // Home (con su aro)
    drawCircle(btn, 10f, Offset(570f, 214f))
    drawCircle(Color(0xFF55575D), 4f, Offset(570f, 214f))
    if (v2) { drawCircle(btn, 9f, Offset(604f, 236f)); drawCircle(btnTop, 4f, Offset(604f, 236f)) } // botón C
}

@Composable
internal fun Television(u: Dp, on: Float, game: Game, s: SceneRect = LaunchKind.PS2.screen) {
    // Marco alrededor de la pantalla [s] (20 de borde) y pie debajo.
    Canvas(Modifier.offset(u * (s.x - 30), u * (s.y - 30)).size(u * (s.w + 60), u * (s.h + 120))) {
        val k = size.width / (s.w + 60)
        withTransform({ scale(k, k, Offset.Zero) }) {
            val w = s.w + 60; val h = s.h + 60
            drawRect(Color(0xFF26282D), Offset(w / 2 - 40f, h), Size(80f, 50f))
            drawRoundRect(Color(0xFF17181C), Offset(0f, 0f), Size(w, h), CornerRadius(14f))
            drawRoundRect(Color(0xFF34373E), Offset(0f, 0f), Size(w, h), CornerRadius(14f), style = Stroke(3f))
            drawCircle(if (on > 0f) Color(0xFF6FB7FF) else Color(0xFFB33A3A), 4f, Offset(w - 40f, h - 16f))
        }
    }
    Box(Modifier.offset(u * s.x, u * s.y).size(u * s.w, u * s.h).clip(RoundedCornerShape(3.dp))) { ScreenContent(game, on) }
}

/** Cartucho pequeño con la carátula en la etiqueta. */
@Composable
internal fun Cartridge(game: Game, body: Color = Color(0xFF34363B)) {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12)).background(body)) {
        Canvas(Modifier.fillMaxSize()) {
            // Muesca arriba a la derecha y contactos abajo
            drawRect(Color(0xFF1E1F23), Offset(size.width * 0.78f, 0f), Size(size.width * 0.22f, size.height * 0.07f))
            for (i in 0 until 8) drawRect(Color(0xFFC9A24A), Offset(size.width * (0.12f + i * 0.1f), size.height * 0.93f),
                Size(size.width * 0.06f, size.height * 0.05f))
        }
        Box(Modifier.fillMaxSize().padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 10.dp).clip(RoundedCornerShape(6))) { CoverArt(game) }
    }
}

/** Disco plateado con brillo de arcoíris y la carátula en la etiqueta. */
@Composable
internal fun Disc(game: Game) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) { drawDisc(this) }
        Box(Modifier.align(Alignment.Center).fillMaxSize(0.62f).clip(CircleShape)) { CoverArt(game) }
        Box(Modifier.align(Alignment.Center).fillMaxSize(0.16f).clip(CircleShape).background(Color(0xFFDADDE2)))
        Box(Modifier.align(Alignment.Center).fillMaxSize(0.07f).clip(CircleShape).background(Color(0xFF15171C)))
    }
}

private fun drawDisc(d: DrawScope) = with(d) {
    val r = size.minDimension / 2
    drawCircle(Brush.radialGradient(listOf(Color(0xFFF2F4F7), Color(0xFFB9BEC7), Color(0xFFE4E7EB)), center, r), r)
    drawCircle(Brush.sweepGradient(listOf(Color(0x40FF6B6B), Color(0x40FFE66B), Color(0x406BFF95), Color(0x406BC8FF),
        Color(0x40B36BFF), Color(0x40FF6B6B)), center), r)
    drawCircle(Color(0xFF9EA4AE), r, style = Stroke(r * 0.02f))
}

// ------------------------------------------------------------------ Sonidos

/** Sonidos cortos de la animación (propios, generados para Repisa). */
private class LaunchSounds(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
    ).build()
    private val ids = listOf(R.raw.anim_click, R.raw.anim_whoosh, R.raw.anim_tray, R.raw.anim_spin, R.raw.anim_chime, R.raw.anim_pc)
        .associateWith { runCatching { pool.load(context, it, 1) }.getOrDefault(0) }

    fun play(res: Int) { ids[res]?.takeIf { it != 0 }?.let { pool.play(it, 0.8f, 0.8f, 1, 0, 1f) } }
    fun release() = pool.release()
}
