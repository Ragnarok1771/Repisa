package com.repisa.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import java.util.concurrent.ConcurrentHashMap

/**
 * Los dibujos de las consolas en reposo (sin cartucho ni disco, tapa y bandeja cerradas, pantallas apagadas; DS y
 * 3DS abiertas), los mismos que usan las animaciones al jugar. Están en unidades de la escena (1600 × 900).
 */
internal object ConsoleArt {
    private val SWITCH_SCREEN = LaunchKind.SWITCH.screen
    private val PC_SCREEN = LaunchKind.PC.screen
    private val dark = Color(0xFF0B0D12)

    /** El dibujo de la consola [consoleId], o null si no tiene. */
    fun of(consoleId: String): (DrawScope.() -> Unit)? = when (consoleId) {
        "switch", "switch2" -> {
            {
                withTransform({ translate(870f, 436f) }) { drawSwitchBody(consoleId == "switch2") }
                drawRoundRect(dark, Offset(SWITCH_SCREEN.x, SWITCH_SCREEN.y), Size(SWITCH_SCREEN.w, SWITCH_SCREEN.h), CornerRadius(3f))
            }
        }
        "pc", "other" -> {
            {
                withTransform({ translate(760f, 140f) }) { drawMonitorBody(0f) }
                drawRoundRect(dark, Offset(PC_SCREEN.x, PC_SCREEN.y), Size(PC_SCREEN.w, PC_SCREEN.h), CornerRadius(3f))
            }
        }
        "ps2" -> { { drawTopLoaderBody(TopLoader.PS2_SLIM, false); drawTopLoaderLid(TopLoader.PS2_SLIM, 0f) } }
        "gc" -> { { drawTopLoaderBody(TopLoader.GAMECUBE, false); drawTopLoaderLid(TopLoader.GAMECUBE, 0f) } }
        "psx", "dreamcast", "saturn", "pcfx" -> TopLoader.of(consoleId).let { m -> { drawTopLoaderBody(m, false); drawTopLoaderLid(m, 0f) } }
        "wii" -> { { drawStandingConsole(false, 0f) } }
        "ps5" -> { { drawStandingConsole(true, 0f) } }
        "arcade" -> { { drawArcadeCabinet(0f) } }
        in setOf("dos", "amstradcpc", "apple2", "atarist", "c64", "amiga", "msx", "zxspectrum", "x68000", "scummvm", "engines") ->
            { { drawComputer(0f); drawRoundRect(dark, Offset(COMPUTER_SCREEN.x, COMPUTER_SCREEN.y), Size(COMPUTER_SCREEN.w, COMPUTER_SCREEN.h), CornerRadius(14f)) } }
        in Handheld.consoles -> Handheld.of(consoleId).let { m -> { drawHandheldIdle(m) } }
        "wiiu" -> SlotConsole.of(consoleId).let { m -> { m.draw(this); drawCircle(m.ledOn.copy(alpha = 0.35f), 5f, m.led); drawWiiUGamePad() } }
        in SlotConsole.consoles -> SlotConsole.of(consoleId).let { m -> { m.draw(this); drawCircle(m.ledOn.copy(alpha = 0.35f), 5f, m.led) } }
        in TrayConsole.consoles -> TrayConsole.of(consoleId).let { m -> { m.draw(this, 0f) } }
        in CartConsole.consoles -> CartConsole.of(consoleId).let { m -> { m.draw(this); drawCircle(m.ledOn.copy(alpha = 0.35f), 5f, m.led) } }
        else -> null
    }

    private val boundsCache = ConcurrentHashMap<String, SceneRect>()

    /**
     * Lo que ocupa el dibujo, medido una vez dibujándolo en pequeño y buscando los píxeles pintados
     * (así el icono queda bien recortado aunque se retoque el dibujo).
     */
    fun bounds(consoleId: String): SceneRect? {
        boundsCache[consoleId]?.let { return it }
        val draw = of(consoleId) ?: return null
        val k = 0.25f
        val w = (SCENE_W * k).toInt(); val h = (SCENE_H * k).toInt()
        val img = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(img), Size(w.toFloat(), h.toFloat())) {
            withTransform({ scale(k, k, Offset.Zero) }) { draw() }
        }
        val px = IntArray(w * h)
        img.readPixels(px)
        var x0 = w; var y0 = h; var x1 = -1; var y1 = -1
        for (y in 0 until h) for (x in 0 until w) if ((px[y * w + x] ushr 24) > 16) {
            if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y
        }
        if (x1 < 0) return null
        val pad = 6f
        val r = SceneRect(x0 / k - pad, y0 / k - pad, (x1 - x0 + 1) / k + 2 * pad, (y1 - y0 + 1) / k + 2 * pad)
        boundsCache[consoleId] = r
        return r
    }
}

/** Icono de la consola de un juego, recortado a su forma (la altura o el ancho los da [modifier]). */
@Composable
fun ConsoleIcon(consoleId: String, modifier: Modifier = Modifier) {
    val draw = remember(consoleId) { ConsoleArt.of(consoleId) } ?: return
    val b = remember(consoleId) { ConsoleArt.bounds(consoleId) } ?: return
    Canvas(modifier.aspectRatio(b.w / b.h)) {
        val k = minOf(size.width / b.w, size.height / b.h)
        withTransform({ scale(k, k, Offset.Zero); translate(-b.x, -b.y) }) { draw() }
    }
}

/**
 * Icono de consola dentro de un recuadro fijo de [width] × [height]: se ajusta al recuadro y se centra abajo, así
 * todas ocupan lo mismo (una NES ancha no sale enorme ni una GameCube o una DS enanas).
 */
@Composable
fun ConsoleIconBox(consoleId: String, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val draw = remember(consoleId) { ConsoleArt.of(consoleId) } ?: return
    val b = remember(consoleId) { ConsoleArt.bounds(consoleId) } ?: return
    Canvas(modifier.then(Modifier.androidxSize(width, height))) {
        val k = minOf(size.width / b.w, size.height / b.h)
        val dx = (size.width - b.w * k) / 2
        val dy = size.height - b.h * k
        withTransform({ translate(dx, dy); scale(k, k, Offset.Zero); translate(-b.x, -b.y) }) { draw() }
    }
}

private fun Modifier.androidxSize(w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp) =
    this.then(Modifier.size(w, h))

/** ¿Tiene dibujo esta consola? */
fun hasConsoleArt(consoleId: String) = ConsoleArt.of(consoleId) != null

/** Wii U: el GamePad (la tableta con pantalla) delante de la consola, que es lo que más la identifica. */
private fun DrawScope.drawWiiUGamePad() {
    val x = 700f; val y = 520f; val w = 340f; val h = 190f
    val body = Color(0xFFF2F3F5); val edge = Color(0xFFC9CDD4); val btn = Color(0xFFBFC4CB)
    drawRoundRect(body, Offset(x, y), Size(w, h), CornerRadius(60f))
    drawRoundRect(edge, Offset(x, y), Size(w, h), CornerRadius(60f), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
    // Pantalla con su marco negro
    drawRoundRect(Color(0xFF141519), Offset(x + 92f, y + 26f), Size(156f, 112f), CornerRadius(8f))
    drawRoundRect(Color(0xFF0B0D12), Offset(x + 100f, y + 33f), Size(140f, 98f), CornerRadius(4f))
    // Palancas arriba, cruceta y botones debajo
    listOf(Offset(x + 50f, y + 52f), Offset(x + w - 50f, y + 52f)).forEach { drawCircle(Color(0xFF9EA4AC), 20f, it); drawCircle(btn, 14f, it) }
    drawRoundRect(btn, Offset(x + 34f, y + 108f), Size(32f, 10f), CornerRadius(3f))
    drawRoundRect(btn, Offset(x + 45f, y + 97f), Size(10f, 32f), CornerRadius(3f))
    val c = Offset(x + w - 50f, y + 113f)
    listOf(Offset(0f, -13f), Offset(0f, 13f), Offset(-13f, 0f), Offset(13f, 0f)).forEach { drawCircle(btn, 6.5f, c + it) }
    drawCircle(btn, 7f, Offset(x + w / 2, y + 162f))
}
