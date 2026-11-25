package com.repisa.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.model.Game
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Máquina recreativa (arcade): una moneda vuela hasta la ranura de la puerta, entra con su tintineo, se ilumina el
 * cartel y la pantalla muestra el juego. Y ordenador antiguo (DOS, Amstrad, Commodore, motores de juego...): el
 * disquete sale de la caja grande y entra en la disquetera; el monitor de tubo se enciende.
 * Dibujados a partir de fotos de referencia (una recreativa vertical clásica y un PC de los 80), sin logos.
 */

@Composable
private fun Layer(u: Dp, area: SceneRect, draw: DrawScope.() -> Unit) =
    Canvas(Modifier.offset(u * area.x, u * area.y).size(u * area.w, u * area.h)) {
        val k = size.width / area.w
        withTransform({ scale(k, k, Offset.Zero); translate(-area.x, -area.y) }) { draw() }
    }

private fun mix(a: Color, b: Color, t: Float) = Color(
    a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha + (b.alpha - a.alpha) * t,
)

// ------------------------------------------------------------------ Recreativa

internal val ARCADE_SCREEN = SceneRect(1040f, 200f, 220f, 172f)

/** Mueble de recreativa (cartel, pantalla, mandos, altavoz y monedero), en unidades de la escena. */
internal fun DrawScope.drawArcadeCabinet(credit: Float, slotGlow: Float = 0.4f) {
    val slotA = Offset(1131f, 578f)
    val side = Color(0xFF1C2E66); val trim = Color(0xFF7FA6E0)
    drawPath(Path().apply {
        moveTo(990f, 80f); lineTo(1310f, 80f); lineTo(1316f, 404f); lineTo(1334f, 474f); lineTo(1322f, 700f); lineTo(978f, 700f)
        lineTo(966f, 474f); lineTo(984f, 404f); close()
    }, side)
    drawRect(trim, Offset(986f, 80f), Size(6f, 620f)); drawRect(trim, Offset(1308f, 80f), Size(6f, 620f))
    // Cartel (marquesina): se enciende con el crédito
    drawRect(mix(Color(0xFF3A2A10), Color(0xFFFFC94A), 0.25f + 0.75f * credit), Offset(1000f, 94f), Size(300f, 70f))
    // Marco de la pantalla con su dibujo
    drawRect(Color(0xFF0E0F12), Offset(1006f, 172f), Size(288f, 226f))
    drawRect(Brush.verticalGradient(listOf(Color(0xFFE0A83A), Color(0xFFB05A2A)), 172f, 398f), Offset(1006f, 172f), Size(288f, 226f), style = Stroke(16f))
    // Panel de mandos (sobresale): palanca roja y botones de colores
    drawPath(Path().apply { moveTo(984f, 404f); lineTo(1316f, 404f); lineTo(1334f, 474f); lineTo(966f, 474f); close() }, Color(0xFF26282E))
    drawRect(Color(0xFF3A3D45), Offset(966f, 470f), Size(368f, 8f))
    drawLine(Color(0xFF15161A), Offset(1060f, 440f), Offset(1060f, 420f), 7f); drawCircle(Color(0xFFD93A3A), 13f, Offset(1060f, 418f))
    listOf(Color(0xFF3A7BD9) to 1150f, Color(0xFFE8A030) to 1192f, Color(0xFFE8503A) to 1234f).forEach { (c, x) ->
        drawCircle(Color(0xFF111215), 13f, Offset(x, 440f)); drawCircle(c, 10f, Offset(x, 438f))
    }
    // Altavoz y puerta de monedas
    drawCircle(Color(0xFF111A3A), 26f, Offset(1032f, 526f))
    for (i in -2..2) drawLine(Color(0xFF26346A), Offset(1012f, 526f + i * 8f), Offset(1052f, 526f + i * 8f), 3f)
    drawRoundRect(Color(0xFF101114), Offset(1090f, 540f), Size(120f, 130f), CornerRadius(6f))
    listOf(slotA, Offset(slotA.x + 40f, slotA.y)).forEach { s ->
        val glow = if (s == slotA) slotGlow else 0.4f
        drawRoundRect(mix(Color(0xFF5A2A10), Color(0xFFFF8A30), glow), Offset(s.x - 12f, s.y - 20f), Size(24f, 40f), CornerRadius(4f))
        drawRect(Color(0xFF0A0A0B), Offset(s.x - 2f, s.y - 13f), Size(4f, 26f))
    }
    drawRect(Color(0xFF0B0C0E), Offset(990f, 680f), Size(320f, 20f)) // zócalo
    // Pantalla apagada (la escena pone el juego encima).
    drawRect(Color(0xFF0B0D12), Offset(ARCADE_SCREEN.x, ARCADE_SCREEN.y), Size(ARCADE_SCREEN.w, ARCADE_SCREEN.h))
}

/** Ordenador antiguo beis (monitor de tubo, caja con disqueteras y teclado), en unidades de la escena. */
internal fun DrawScope.drawComputer(on: Float, driveLed: Boolean = false) {
    val beige = Color(0xFFDCD4BE); val beigeDark = Color(0xFFBFB59C)
    // Monitor de tubo
    drawRoundRect(beige, Offset(960f, 200f), Size(304f, 330f), CornerRadius(22f))
    drawRoundRect(beigeDark, Offset(960f, 200f), Size(304f, 330f), CornerRadius(22f), style = Stroke(3f))
    drawRoundRect(Color(0xFF2B2A27), Offset(984f, 222f), Size(256f, 200f), CornerRadius(18f))
    listOf(486f, 506f).forEach { y -> drawCircle(Color(0xFFA79E86), 7f, Offset(1236f, y)) }
    drawCircle(if (on > 0f) Color(0xFF7BD389) else Color(0xFF6A5F48), 4f, Offset(1236f, 466f))
    drawRect(beigeDark, Offset(1070f, 530f), Size(84f, 22f)) // pie
    // Caja del ordenador con dos disqueteras
    drawRoundRect(beige, Offset(880f, 552f), Size(460f, 104f), CornerRadius(6f))
    drawRoundRect(beigeDark, Offset(880f, 552f), Size(460f, 104f), CornerRadius(6f), style = Stroke(3f))
    for (i in 0 until 9) drawRect(Color(0xFF9E957C), Offset(900f + i * 9f, 600f), Size(4f, 34f)) // rejilla
    listOf(1070f, 1206f).forEach { x ->
        drawRoundRect(Color(0xFF26262A), Offset(x, 574f), Size(120f, 62f), CornerRadius(4f))
        drawRect(Color(0xFF070708), Offset(x + 6f, 602f), Size(108f, 5f))
        drawRect(Color(0xFF3E3E44), Offset(x + 52f, 584f), Size(16f, 40f))
    }
    drawCircle(if (driveLed) Color(0xFF7BFF6A) else Color(0xFF3A4A30), 4f, Offset(1180f, 628f))
    // Teclado
    drawPath(Path().apply { moveTo(890f, 664f); lineTo(1330f, 664f); lineTo(1352f, 700f); lineTo(868f, 700f); close() }, beige)
    for (row in 0 until 3) drawLine(beigeDark, Offset(896f - row * 7f, 672f + row * 9f), Offset(1324f + row * 7f, 672f + row * 9f), 3f)
}

@Composable
internal fun ArcadeScene(game: Game, p: Float, u: Dp) {
    val credit = seg(p, 0.46f, 0.54f)          // la moneda ya entró: se ilumina el cartel
    val on = seg(p, 0.52f, 0.60f)
    val slotA = Offset(1131f, 578f)
    // Mueble: costados oscuros, cartel arriba, pantalla, panel de mandos, altavoz y puerta de monedas.
    Layer(u, SceneRect(960f, 70f, 380f, 632f)) {
        drawArcadeCabinet(credit, slotGlow = maxOf(0.35f, 1f - seg(p, 0.42f, 0.6f) * 0.6f + credit * 0.2f))
    }
    // Nombre del juego en el cartel
    Box(Modifier.offset(u * 1004, u * 96).size(u * 292, u * 66), contentAlignment = Alignment.Center) {
        Text(game.title, color = mix(Color(0xFF6B4A1A), Color(0xFF2A1404), credit), fontSize = 13.sp, fontWeight = FontWeight.Black,
            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 6.dp))
    }
    Box(Modifier.offset(u * ARCADE_SCREEN.x, u * ARCADE_SCREEN.y).size(u * ARCADE_SCREEN.w, u * ARCADE_SCREEN.h).clip(RoundedCornerShape(6.dp))) {
        ScreenContent(game, on)
    }
    // Moneda: aparece girando, vuela en arco hasta la ranura y entra.
    val appear = ease(seg(p, 0.04f, 0.14f))
    val fly = ease(seg(p, 0.16f, 0.40f))
    val into = ease(seg(p, 0.40f, 0.46f))
    if (p < 0.46f) {
        val cx = lerp(420f, slotA.x, fly)
        val cy = lerp(480f, slotA.y, fly) - 260f * sin(PI.toFloat() * fly)
        val turn = abs(cos(p * 40f)) * (1f - fly) + 0.08f * fly
        val r = lerp(46f, 18f, fly)
        Box(Modifier.offset(u * (cx - r), u * (cy - r)).size(u * (2 * r), u * (2 * r)).alpha(appear * (1f - into)).graphicsLayer {
            scaleX = lerp(turn.coerceIn(0.08f, 1f), 0.08f, into); scaleY = lerp(1f, 0.7f, into)
        }) { Coin() }
    }
}

@Composable
private fun Coin() {
    Box(Modifier.fillMaxSize().clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFFE89A), Color(0xFFE0A62A), Color(0xFFA9701A))))) {
        Box(Modifier.align(Alignment.Center).fillMaxSize(0.62f).clip(CircleShape).background(Color(0x33FFFFFF)))
    }
}

// ------------------------------------------------------------------ Ordenador antiguo

internal val COMPUTER_SCREEN = SceneRect(1004f, 238f, 216f, 164f)

@Composable
internal fun ComputerScene(game: Game, p: Float, u: Dp) {
    val on = seg(p, 0.60f, 0.68f)
    // Caja grande del juego (cartón) a la izquierda
    val caseH = 400f
    val boxHu = boxSize(game, u * caseH).second / u
    val caseX = 160f; val caseY = TABLE_Y - boxHu - 6f
    val frontW = caseH * frontAspect(game)
    val open = seg(p, 0.12f, 0.27f)
    val caseAlpha = ease(seg(p, 0f, 0.1f)) * (1f - seg(p, 0.5f, 0.6f))
    ClosedCase(game, u, caseX, caseY, caseH, caseAlpha, open) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(4)).background(Color(0xFF8C7A60)))
    }
    val beige = Color(0xFFDCD4BE); val beigeDark = Color(0xFFBFB59C)
    val drive = Offset(1130f, 604f)
    Layer(u, SceneRect(850f, 190f, 520f, 512f)) { drawComputer(on, driveLed = p > 0.52f && p < 0.66f) }
    Box(Modifier.offset(u * COMPUTER_SCREEN.x, u * COMPUTER_SCREEN.y).size(u * COMPUTER_SCREEN.w, u * COMPUTER_SCREEN.h).clip(RoundedCornerShape(14.dp))) {
        ScreenContent(game, on)
        // Líneas del tubo
        if (on > 0f) Canvas(Modifier.fillMaxSize()) { var y = 0f; while (y < size.height) { drawLine(Color(0x1A000000), Offset(0f, y), Offset(size.width, y), 1.5f); y += 4f } }
    }
    // Disquete: de la caja → delante de la disquetera → entra (se ve de canto).
    val startCx = caseX + frontW / 2; val startCy = caseY + (boxHu - caseH) + caseH * 0.45f
    val fly = ease(seg(p, 0.30f, 0.48f))
    val into = ease(seg(p, 0.48f, 0.54f))
    if ((p < 0.30f && open > 0.5f) || p in 0.30f..0.54f) {
        val d = lerp(150f, 100f, fly)
        val cx = lerp(startCx, drive.x, fly)
        val cy = lerp(startCy, drive.y, fly) - 140f * sin(PI.toFloat() * fly)
        Box(Modifier.offset(u * (cx - d / 2), u * (cy - d / 2)).size(u * d, u * d).alpha(1f - into).graphicsLayer {
            scaleY = lerp(1f, 0.06f, fly) * lerp(1f, 0.6f, into); scaleX = lerp(1f, 0.9f, into)
            rotationZ = 12f * sin(PI.toFloat() * fly)
        }) { Floppy(game) }
    }
}

/** Disquete negro con su etiqueta (la carátula), ventana de lectura y agujero central. */
@Composable
private fun Floppy(game: Game) {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(4)).background(Color(0xFF17171A))) {
        Box(Modifier.align(Alignment.TopCenter).padding(top = 4.dp).fillMaxSize(0.8f).clip(RoundedCornerShape(3))) {
            Box(Modifier.fillMaxSize().padding(bottom = 1.dp)) { CoverArt(game) }
        }
        Box(Modifier.align(Alignment.Center).fillMaxSize(0.22f).clip(CircleShape).background(Color(0xFF0A0A0B)))
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).fillMaxSize(0.16f).clip(RoundedCornerShape(2)).background(Color(0xFF2A2A2E)))
    }
}
