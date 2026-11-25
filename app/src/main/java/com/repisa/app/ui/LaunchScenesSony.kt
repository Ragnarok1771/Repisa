package com.repisa.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.repisa.app.model.Game
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tanda 2 (Sony y Microsoft) — consolas de sobremesa con ranura o bandeja delantera:
 * PS3 (la negra brillante original: el disco entra de canto por la ranura del frente) y
 * Xbox (la caja negra grande con la «X» en relieve arriba: bandeja que sale hacia delante).
 * Formas medidas sobre fotos de referencia, sin logos.
 */

/** Caja del juego a la izquierda con su interior; devuelve dónde empieza el disco. */
@Composable
private fun DiscCase(game: Game, p: Float, u: Dp, inside: Color, fadeAt: Float): Triple<Float, Float, Float> {
    val caseH = 440f
    val boxHu = boxSize(game, u * caseH).second / u
    val caseX = 140f; val caseY = TABLE_Y - boxHu - 6f
    val frontW = caseH * frontAspect(game)
    val open = seg(p, 0.12f, 0.27f)
    val caseAlpha = ease(seg(p, 0f, 0.1f)) * (1f - seg(p, fadeAt, fadeAt + 0.1f))
    val d0 = minOf(frontW * 0.78f, caseH * 0.62f)
    ClosedCase(game, u, caseX, caseY, caseH, caseAlpha, open) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(4)).background(inside)) {
            Box(Modifier.align(Alignment.Center).size(u * (d0 * 0.3f)).clip(CircleShape).background(Color(0xFF2A2E36)))
        }
    }
    return Triple(caseX + frontW / 2, caseY + (boxHu - caseH) + caseH / 2, d0)
}

@Composable
private fun SpinningDisc(game: Game, u: Dp, cx: Float, cy: Float, d: Float, spin: Float, scaleX: Float = 1f, scaleY: Float = 1f, alpha: Float = 1f) {
    Box(Modifier.offset(u * (cx - d / 2), u * (cy - d / 2)).size(u * d, u * d).alpha(alpha).graphicsLayer { this.scaleX = scaleX; this.scaleY = scaleY }) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin }) { Disc(game) }
    }
}

/** Dibuja en coordenadas de la escena dentro de un rectángulo. */
@Composable
private fun SceneCanvas(u: Dp, area: SceneRect, draw: DrawScope.() -> Unit) =
    Canvas(Modifier.offset(u * area.x, u * area.y).size(u * area.w, u * area.h)) {
        val k = size.width / area.w
        withTransform({ scale(k, k, Offset.Zero); translate(-area.x, -area.y) }) { draw() }
    }

// ------------------------------------------------------------------ Ranura delantera (PS3, Wii U, PS4)

/** Consola de sobremesa con ranura en el frente: el disco se pone de canto delante y entra. */
internal data class SlotConsole(val slot: SceneRect, val led: Offset, val ledOn: Color, val draw: DrawScope.() -> Unit) {
    companion object {
        val consoles = setOf("ps3", "wiiu", "ps4")

        /** PS3 original: negra brillante, arriba abombada, franja cromada. */
        private val PS3 = SlotConsole(SceneRect(990f, 636f, 260f, 6f), Offset(1392f, 640f), Color(0xFF59E07A)) {
            val top = Path().apply { moveTo(860f, 614f); cubicTo(900f, 556f, 1380f, 556f, 1420f, 614f); close() }
            drawPath(top, Brush.verticalGradient(listOf(Color(0xFF3A3D45), Color(0xFF0E0F12)), 560f, 614f))
            drawRect(Color(0xFF0C0D10), Offset(860f, 614f), Size(560f, 86f))
            drawRect(Color(0xFFBFC3CB), Offset(860f, 612f), Size(560f, 3f))
            drawRect(Brush.verticalGradient(listOf(Color(0x30FFFFFF), Color.Transparent), 616f, 660f), Offset(860f, 616f), Size(560f, 40f))
            drawCircle(Color(0xFF2A2C33), 6f, Offset(1330f, 640f)); drawCircle(Color(0xFF2A2C33), 6f, Offset(1356f, 640f))
        }

        /** Wii U: caja blanca brillante y alargada, ranura en el frente y luz azul. */
        private val WIIU = SlotConsole(SceneRect(1000f, 640f, 240f, 6f), Offset(1376f, 648f), Color(0xFF4FC3F7)) {
            drawPath(Path().apply { moveTo(892f, 590f); lineTo(1388f, 590f); lineTo(1410f, 618f); lineTo(870f, 618f); close() }, Color(0xFFF4F5F7))
            drawRoundRect(Color(0xFFE9EBEF), Offset(870f, 616f), Size(540f, 84f), CornerRadius(10f))
            drawRoundRect(Color(0xFFCCD0D6), Offset(870f, 616f), Size(540f, 84f), CornerRadius(10f), style = Stroke(2f))
            drawRect(Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color.Transparent), 620f, 660f), Offset(874f, 620f), Size(532f, 36f))
            drawRoundRect(Color(0xFFD5D8DD), Offset(1300f, 640f), Size(40f, 14f), CornerRadius(7f))
        }

        /** PS4: negra y plana, con el corte en el medio (mitad brillante y mitad mate) donde está la ranura. */
        private val PS4 = SlotConsole(SceneRect(1000f, 652f, 250f, 6f), Offset(1200f, 590f), Color(0xFF3FA0FF)) {
            drawPath(Path().apply { moveTo(900f, 574f); lineTo(1380f, 574f); lineTo(1410f, 640f); lineTo(870f, 640f); close() },
                Brush.verticalGradient(listOf(Color(0xFF2A2C32), Color(0xFF101114)), 574f, 640f))
            drawRect(Color(0xFF16171A), Offset(870f, 640f), Size(540f, 30f))
            drawRect(Color(0xFF050506), Offset(870f, 648f), Size(540f, 14f)) // el corte del medio
            drawRect(Color(0xFF1C1D21), Offset(880f, 664f), Size(520f, 36f))
            drawRect(Color(0xFF2A4C7A), Offset(1150f, 586f), Size(100f, 4f)) // franja de luz de arriba
        }

        fun of(consoleId: String): SlotConsole = when (consoleId) { "wiiu" -> WIIU; "ps4" -> PS4; else -> PS3 }
    }
}

@Composable
internal fun SlotScene(game: Game, p: Float, u: Dp) {
    val m = SlotConsole.of(game.consoleId)
    Television(u, on = seg(p, 0.61f, 0.68f), game = game, s = LaunchKind.PS3.screen)
    val (sx, sy, d0) = DiscCase(game, p, u, Color(0xFF101114), fadeAt = 0.46f)
    val slot = m.slot
    SceneCanvas(u, SceneRect(850f, 540f, 580f, 165f)) {
        m.draw(this)
        drawRoundRect(Color(0xFF020203), Offset(slot.x, slot.y), Size(slot.w, slot.h), CornerRadius(3f))
        drawCircle(if (p > 0.52f) m.ledOn else Color(0xFF7A2A2A), 5f, m.led)
    }
    // Disco: de la caja → de canto delante de la ranura → entra.
    val fly = ease(seg(p, 0.29f, 0.45f))
    val into = ease(seg(p, 0.45f, 0.52f))
    val spin = if (p > 0.29f) (p - 0.29f) * 900f else 0f
    if (p < 0.29f && seg(p, 0.12f, 0.27f) > 0.4f) SpinningDisc(game, u, sx, sy, d0, 0f)
    if (p in 0.29f..0.52f) {
        val d = lerp(d0, slot.w * 0.92f, fly)
        val cx = lerp(sx, slot.x + slot.w / 2, fly)
        val cy = lerp(sy, slot.y + slot.h / 2, fly) - 150f * sin(PI.toFloat() * fly)
        SpinningDisc(game, u, cx, cy, d, spin, scaleX = lerp(1f, 0.92f, into), scaleY = lerp(1f, 0.07f, fly), alpha = 1f - into)
    }
}

// ------------------------------------------------------------------ Bandeja (Xbox, Xbox 360)

/** Consola con bandeja delantera: la bandeja sale hacia delante, recibe el disco y entra. */
internal data class TrayConsole(
    val slotY: Float, val trayX: Float, val trayW: Float,
    val trayColor: Color, val lipColor: Color,
    val draw: DrawScope.(on: Float) -> Unit,
) {
    companion object {
        val consoles = setOf("xbox", "xbox360", "3do", "cdi")

        /** Xbox original: caja negra grande con la «X» en relieve arriba y el círculo verde en el centro. */
        private val XBOX = TrayConsole(630f, 900f, 290f, Color(0xFF26282E), Color(0xFF16171B)) { on ->
            val top = Path().apply { moveTo(896f, 536f); lineTo(1384f, 536f); lineTo(1410f, 612f); lineTo(870f, 612f); close() }
            drawPath(top, Color(0xFF1C1D21))
            drawLine(Color(0xFF2B2D33), Offset(900f, 540f), Offset(1400f, 608f), 26f)
            drawLine(Color(0xFF2B2D33), Offset(1380f, 540f), Offset(880f, 608f), 26f)
            drawOval(Color(0xFF0E0F12), Offset(1100f, 556f), Size(80f, 36f))
            drawOval(lerpColor2(Color(0xFF3E7A1E), Color(0xFF8CE04A), on), Offset(1106f, 559f), Size(68f, 30f))
            drawRect(Color(0xFF131417), Offset(870f, 612f), Size(540f, 88f))
            drawRect(Color(0xFF2A2C32), Offset(870f, 612f), Size(540f, 3f))
            listOf(900f, 990f, 1230f, 1320f).forEach { x -> drawRoundRect(Color(0xFF07080A), Offset(x, 668f), Size(60f, 22f), CornerRadius(4f)) }
            drawCircle(Color(0xFF2E3036), 10f, Offset(1240f, 640f)); drawCircle(Color(0xFF2E3036), 10f, Offset(1240f, 662f))
            drawCircle(if (on > 0f) Color(0xFF8CE04A) else Color(0xFF2E3036), 4f, Offset(1262f, 640f))
        }

        /** Xbox 360: blanca, con los costados grises hundidos y el aro de luz verde del botón de encendido. */
        private val XBOX360 = TrayConsole(626f, 900f, 280f, Color(0xFFD9DBDF), Color(0xFFC3C6CC)) { on ->
            drawPath(Path().apply { moveTo(896f, 552f); lineTo(1384f, 552f); lineTo(1410f, 612f); lineTo(870f, 612f); close() }, Color(0xFFF2F3F5))
            drawRoundRect(Color(0xFFB8BCC3), Offset(1180f, 562f), Size(170f, 40f), CornerRadius(14f)) // rejilla gris de arriba
            for (i in 0 until 6) drawLine(Color(0xFFA2A6AE), Offset(1190f + i * 28f, 566f), Offset(1200f + i * 28f, 598f), 3f)
            drawRoundRect(Color(0xFFE7E8EB), Offset(870f, 612f), Size(540f, 88f), CornerRadius(6f))
            drawRect(Color(0xFFCDD0D5), Offset(870f, 612f), Size(540f, 3f))
            // Aro de luz del botón de encendido y puertos
            drawCircle(Color(0xFFB8BCC3), 22f, Offset(1330f, 656f))
            drawCircle(lerpColor2(Color(0xFF3C5A2A), Color(0xFF7CFF4E), on), 22f, Offset(1330f, 656f), style = Stroke(5f))
            drawCircle(Color(0xFFDADCE0), 15f, Offset(1330f, 656f))
            listOf(1230f, 1268f).forEach { x -> drawRoundRect(Color(0xFF9EA2AA), Offset(x, 680f), Size(26f, 10f), CornerRadius(3f)) }
        }

        /** 3DO (Panasonic FZ-1): negra, ancha y baja, con la parte de arriba con forma de cruz y la bandeja delante. */
        private val TDO = TrayConsole(640f, 930f, 270f, Color(0xFF24252A), Color(0xFF15161A)) { on ->
            drawPath(Path().apply { moveTo(900f, 560f); lineTo(1380f, 560f); lineTo(1410f, 620f); lineTo(870f, 620f); close() }, Color(0xFF1F2024))
            drawPath(Path().apply { moveTo(1000f, 566f); lineTo(1280f, 566f); lineTo(1300f, 614f); lineTo(980f, 614f); close() }, Color(0xFF2A2B30))
            drawRect(Color(0xFF16171A), Offset(870f, 620f), Size(540f, 80f))
            drawRect(Color(0xFF2E2F35), Offset(870f, 620f), Size(540f, 3f))
            for (i in 0 until 6) drawRect(Color(0xFF0E0F11), Offset(1230f + i * 22f, 634f), Size(12f, 50f))
            drawCircle(Color(0xFF2E3036), 9f, Offset(1210f, 646f))
            drawCircle(if (on > 0f) Color(0xFF7CFF8A) else Color(0xFF3A4A30), 4f, Offset(1210f, 672f))
        }

        /** Philips CD-i: gris oscura como un reproductor de vídeo, con la bandeja a la izquierda y los botones a la derecha. */
        private val CDI = TrayConsole(640f, 900f, 260f, Color(0xFF3A3C42), Color(0xFF2A2C31)) { on ->
            drawPath(Path().apply { moveTo(896f, 590f); lineTo(1384f, 590f); lineTo(1408f, 622f); lineTo(872f, 622f); close() }, Color(0xFF55575E))
            drawRect(Color(0xFF45474D), Offset(872f, 622f), Size(536f, 78f))
            drawRect(Color(0xFF606268), Offset(872f, 622f), Size(536f, 3f))
            drawRoundRect(Color(0xFF2A2B30), Offset(1190f, 640f), Size(120f, 22f), CornerRadius(3f)) // ranura de tarjeta
            listOf(1330f, 1360f, 1390f).forEach { x -> drawRoundRect(Color(0xFF2E3036), Offset(x - 10f, 668f), Size(20f, 12f), CornerRadius(2f)) }
            drawCircle(if (on > 0f) Color(0xFF7CFF8A) else Color(0xFF3A4A30), 4f, Offset(1180f, 682f))
        }

        fun of(consoleId: String): TrayConsole = when (consoleId) { "xbox360" -> XBOX360; "3do" -> TDO; "cdi" -> CDI; else -> XBOX }
    }
}

@Composable
internal fun XboxScene(game: Game, p: Float, u: Dp) {
    val m = TrayConsole.of(game.consoleId)
    Television(u, on = seg(p, 0.69f, 0.76f), game = game, s = LaunchKind.XBOX.screen)
    val (sx, sy, d0) = DiscCase(game, p, u, Color(0xFF101114), fadeAt = 0.48f)
    val slotY = m.slotY; val trayX = m.trayX; val trayW = m.trayW; val trayD = 66f; val lip = 14f
    val on = seg(p, 0.6f, 0.68f)
    SceneCanvas(u, SceneRect(850f, 520f, 580f, 185f)) {
        m.draw(this, on)
        drawRect(Color(0xFF050506), Offset(trayX, slotY - 3f), Size(trayW, 6f)) // ranura de la bandeja
    }
    // Bandeja: sale (0.19–0.30) y entra (0.50–0.61), con el disco encima.
    val trayOut = ease(seg(p, 0.19f, 0.30f)) * (1f - ease(seg(p, 0.50f, 0.61f)))
    val dd = trayD * trayOut
    val fly = ease(seg(p, 0.29f, 0.48f))
    val dEnd = 200f; val flatEnd = trayD * 0.86f / dEnd
    val discD = lerp(d0, dEnd, fly); val flat = lerp(1f, flatEnd, fly)
    val half = dEnd * flatEnd / 2
    val landedCy = slotY - half + trayOut * (trayD * 0.5f + half)
    val cxEnd = trayX + trayW / 2
    val spin = if (p > 0.29f) (p - 0.29f) * 1400f else 0f
    if (p < 0.29f && seg(p, 0.12f, 0.27f) > 0.4f) SpinningDisc(game, u, sx, sy, d0, 0f)
    val clipX = trayX - 40f
    Box(Modifier.offset(u * clipX, u * slotY).size(u * (trayW + 80), u * (trayD + lip + 20)).clipToBounds()) {
        if (dd > 0.5f) Canvas(Modifier.size(u * (trayW + 80), u * (trayD + lip + 20))) {
            val k = size.width / (trayW + 80)
            withTransform({ scale(k, k, Offset.Zero) }) {
                val l = 40f; val r = 40f + trayW
                val grow = 14f * trayOut
                drawRect(Color(0x55000000), Offset(l - grow, dd + lip), Size(trayW + grow * 2, 10f))
                drawPath(Path().apply { moveTo(l, 0f); lineTo(r, 0f); lineTo(r + grow, dd); lineTo(l - grow, dd); close() }, m.trayColor)
                drawOval(lerpColor2(m.trayColor, Color.Black, 0.35f), Offset(l + trayW / 2 - trayW * 0.37f, dd * 0.08f), Size(trayW * 0.74f, dd * 0.84f))
                drawRect(m.lipColor, Offset(l - grow, dd), Size(trayW + grow * 2, lip))
                drawRect(lerpColor2(m.lipColor, Color.White, 0.2f), Offset(l - grow, dd), Size(trayW + grow * 2, 2f))
            }
        }
        if (p >= 0.48f) Box(Modifier.offset(u * -clipX, u * -slotY)) { SpinningDisc(game, u, cxEnd, landedCy, discD, spin, scaleY = flat) }
    }
    if (p in 0.29f..0.48f) {
        val cx = lerp(sx, cxEnd, fly)
        val cy = lerp(sy, slotY + trayD * 0.5f, fly) - 150f * sin(PI.toFloat() * fly)
        SpinningDisc(game, u, cx, cy, discD, spin, scaleY = flat)
    }
}

private fun lerpColor2(a: Color, b: Color, t: Float) = Color(
    a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha + (b.alpha - a.alpha) * t,
)
