package com.repisa.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.repisa.app.model.Game
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tanda 3 — consolas de cartucho de sobremesa (NES, Super Nintendo, N64, Mega Drive, Master System, Atari 2600,
 * Neo Geo y PC Engine), dibujadas a partir de fotos de referencia y sin logos. La consola se ve un poco desde arriba;
 * el cartucho entra por la ranura de arriba y se queda asomando, como en las de verdad. La NES carga por la puerta
 * del frente y la PC Engine con su tarjeta por la ranura del frente.
 */
internal data class CartConsole(
    /** Centro de la ranura y línea por la que entra el cartucho (arriba) o centro de la ranura del frente. */
    val slotX: Float, val slotY: Float, val slotW: Float,
    val cartW: Float, val cartH: Float, val cartColor: Color,
    /** Qué parte del cartucho queda asomando una vez metido (arriba). */
    val stick: Float = 0.45f,
    /** true: entra por el frente (NES, PC Engine) en vez de por arriba. */
    val front: Boolean = false,
    val led: Offset, val ledOn: Color = Color(0xFFFF3B3B),
    val draw: DrawScope.() -> Unit,
) {
    companion object {
        val consoles = setOf("nes", "snes", "n64", "megadrive", "sega32x", "segacd", "mastersystem", "atari2600", "neogeo", "pcengine",
            "atari5200", "atari7800", "atarijaguar", "colecovision", "intellivision", "odyssey2", "channelf", "sg1000")

        private fun DrawScope.box(x: Float, w: Float, backY: Float, frontY: Float, bottom: Float, inset: Float, top: Color, front: Color, edge: Color) {
            drawPath(Path().apply { moveTo(x + inset, backY); lineTo(x + w - inset, backY); lineTo(x + w, frontY); lineTo(x, frontY); close() }, top)
            drawRect(front, Offset(x, frontY), Size(w, bottom - frontY))
            drawRect(edge, Offset(x, frontY), Size(w, 3f))
        }

        /** NES: caja gris clara con la puerta del cartucho en el frente; parte derecha negra con estrías. */
        private val NES = CartConsole(
            slotX = 1070f, slotY = 652f, slotW = 250f, cartW = 150f, cartH = 160f, cartColor = Color(0xFF8E8E92), front = true,
            led = Offset(948f, 681f),
            draw = {
                box(900f, 480f, 590f, 622f, 700f, 20f, Color(0xFFD0D0CC), Color(0xFFC4C4C0), Color(0xFFAFAFAB))
                drawPath(Path().apply { moveTo(1270f, 590f); lineTo(1360f, 590f); lineTo(1380f, 622f); lineTo(1262f, 622f); close() }, Color(0xFF1E1E21))
                drawRect(Color(0xFF1E1E21), Offset(1262f, 622f), Size(118f, 78f))
                for (i in 0 until 5) drawRect(Color(0xFF2E2E33), Offset(1272f, 632f + i * 12f), Size(100f, 4f))
                // Franja gris oscura de abajo en todo el frente (con los botones y los puertos de mando)
                drawRect(Color(0xFF4B4B50), Offset(900f, 664f), Size(362f, 36f))
                drawRect(Color(0xFF3A3A3E), Offset(900f, 664f), Size(362f, 3f))
                // Puerta del cartucho con estrías, en la parte gris clara de arriba
                drawRect(Color(0xFFB9B9B6), Offset(940f, 628f), Size(260f, 32f))
                for (i in 0 until 5) drawRect(Color(0xFFA2A29F), Offset(946f, 632f + i * 5.6f), Size(248f, 2.4f))
                // Encendido y reset (gris claro) y los dos puertos de mando
                listOf(966f, 1018f).forEach { x -> drawRoundRect(Color(0xFFBDBDBA), Offset(x, 674f), Size(40f, 14f), CornerRadius(2f)) }
                listOf(1150f, 1204f).forEach { x ->
                    drawRoundRect(Color(0xFF2A2A2D), Offset(x, 672f), Size(40f, 20f), CornerRadius(3f))
                    drawRoundRect(Color(0xFF6A6A70), Offset(x + 8f, 678f), Size(24f, 8f), CornerRadius(2f))
                }
            },
        )

        /** Super Nintendo (americana): gris claro, deslizadores morados y ranura en el centro de arriba. */
        private val SNES = CartConsole(
            slotX = 1140f, slotY = 586f, slotW = 180f, cartW = 170f, cartH = 120f, cartColor = Color(0xFF9C9CA2),
            led = Offset(1350f, 660f),
            draw = {
                box(880f, 520f, 548f, 624f, 700f, 26f, Color(0xFFD7D7DA), Color(0xFFC9C9CD), Color(0xFFB2B2B7))
                drawRoundRect(Color(0xFFBDBDC2), Offset(1030f, 560f), Size(220f, 56f), CornerRadius(10f))
                drawRect(Color(0xFF101012), Offset(1050f, 583f), Size(180f, 7f))
                drawRoundRect(Color(0xFF6A4FA8), Offset(940f, 578f), Size(64f, 24f), CornerRadius(5f))
                drawRoundRect(Color(0xFF6A4FA8), Offset(1276f, 578f), Size(64f, 24f), CornerRadius(5f))
                listOf(1080f, 1150f).forEach { x -> drawRoundRect(Color(0xFF9A9AA0), Offset(x, 652f), Size(50f, 28f), CornerRadius(5f)) }
            },
        )

        /** Nintendo 64: gris oscuro, curvada, ranura en el centro de arriba y cuatro puertos de mando delante. */
        private val N64 = CartConsole(
            slotX = 1140f, slotY = 578f, slotW = 150f, cartW = 140f, cartH = 104f, cartColor = Color(0xFF808186),
            led = Offset(1290f, 650f),
            draw = {
                drawPath(Path().apply {
                    moveTo(900f, 630f); cubicTo(920f, 560f, 1000f, 548f, 1140f, 548f); cubicTo(1280f, 548f, 1360f, 560f, 1380f, 630f); close()
                }, Color(0xFF34353A))
                drawRoundRect(Color(0xFF2A2B2F), Offset(1060f, 562f), Size(160f, 34f), CornerRadius(14f))
                drawRect(Color(0xFF0C0C0E), Offset(1065f, 575f), Size(150f, 7f))
                drawRect(Color(0xFF26272B), Offset(900f, 628f), Size(480f, 72f))
                drawRect(Color(0xFF3E3F45), Offset(900f, 628f), Size(480f, 3f))
                listOf(1010f, 1080f, 1150f, 1220f).forEach { x -> drawRoundRect(Color(0xFF15161A), Offset(x, 658f), Size(46f, 26f), CornerRadius(8f)) }
                listOf(990f, 1260f).forEach { x -> drawRoundRect(Color(0xFF4A4B51), Offset(x, 600f), Size(40f, 16f), CornerRadius(6f)) }
            },
        )

        /** Mega Drive (primera): negra, círculo en relieve a la derecha de arriba y ranura a la izquierda. */
        private val MEGADRIVE = CartConsole(
            slotX = 1030f, slotY = 584f, slotW = 150f, cartW = 140f, cartH = 120f, cartColor = Color(0xFF1C1C1F),
            led = Offset(915f, 660f),
            draw = {
                box(880f, 520f, 548f, 624f, 700f, 26f, Color(0xFF1E1F22), Color(0xFF151517), Color(0xFF2E2F34))
                drawOval(Color(0xFF2A2B30), Offset(1180f, 560f), Size(190f, 58f))
                drawOval(Color(0xFF121315), Offset(1200f, 568f), Size(150f, 42f))
                drawOval(Color(0xFF2E2F35), Offset(1236f, 580f), Size(78f, 18f))
                drawRoundRect(Color(0xFF26272B), Offset(950f, 566f), Size(160f, 34f), CornerRadius(6f))
                drawRect(Color(0xFF060607), Offset(955f, 581f), Size(150f, 7f))
                drawRoundRect(Color(0xFFB83030), Offset(1120f, 600f), Size(30f, 12f), CornerRadius(3f))
                listOf(1270f, 1330f).forEach { x -> drawRoundRect(Color(0xFF0A0A0C), Offset(x, 656f), Size(44f, 26f), CornerRadius(5f)) }
            },
        )

        /** Master System: negra y en cuña, con el panel rojo arriba donde está la ranura. */
        private val MASTERSYSTEM = CartConsole(
            slotX = 1120f, slotY = 582f, slotW = 150f, cartW = 140f, cartH = 104f, cartColor = Color(0xFF1C1C1F),
            led = Offset(1340f, 660f),
            draw = {
                box(880f, 520f, 552f, 630f, 700f, 18f, Color(0xFF1C1D20), Color(0xFF131416), Color(0xFF2C2D32))
                drawPath(Path().apply { moveTo(1000f, 560f); lineTo(1250f, 560f); lineTo(1262f, 614f); lineTo(988f, 614f); close() }, Color(0xFFC0272D))
                drawRect(Color(0xFF080809), Offset(1045f, 579f), Size(150f, 7f))
                for (i in 0 until 4) drawRect(Color(0xFF23242A), Offset(880f, 646f + i * 12f), Size(520f, 3f))
                drawRoundRect(Color(0xFFC0272D), Offset(900f, 640f), Size(60f, 10f), CornerRadius(3f))
            },
        )

        /** Atari 2600: negra con estrías arriba, frente de madera e interruptores. */
        private val ATARI = CartConsole(
            slotX = 1140f, slotY = 576f, slotW = 120f, cartW = 112f, cartH = 96f, cartColor = Color(0xFF1C1C1F),
            led = Offset(1370f, 676f),
            draw = {
                box(880f, 520f, 548f, 628f, 700f, 22f, Color(0xFF1B1B1D), Color(0xFF151517), Color(0xFF2A2A2E))
                for (i in 0 until 6) drawLine(Color(0xFF2A2A2F), Offset(910f + i * 3f, 556f + i * 12f), Offset(1370f - i * 3f, 556f + i * 12f), 3f)
                drawRoundRect(Color(0xFF0E0E10), Offset(1072f, 562f), Size(136f, 26f), CornerRadius(4f))
                drawRect(Color(0xFF050506), Offset(1080f, 573f), Size(120f, 6f))
                listOf(960f, 1010f, 1270f, 1320f).forEach { x -> drawRoundRect(Color(0xFF0B0B0C), Offset(x, 596f), Size(16f, 28f), CornerRadius(4f)) }
                drawRect(Brush.horizontalGradient(listOf(Color(0xFF6B3E1F), Color(0xFF8A5530), Color(0xFF6B3E1F), Color(0xFF7E4A27))), Offset(880f, 636f), Size(520f, 52f))
                for (i in 0 until 5) drawLine(Color(0x33000000), Offset(880f, 646f + i * 9f), Offset(1400f, 642f + i * 9f), 2f)
            },
        )

        /** Neo Geo (AES): negra y alargada, con la ranura grande arriba para sus cartuchos enormes. */
        private val NEOGEO = CartConsole(
            slotX = 1140f, slotY = 580f, slotW = 210f, cartW = 200f, cartH = 150f, cartColor = Color(0xFF17171A), stick = 0.5f,
            led = Offset(1360f, 664f),
            draw = {
                box(870f, 540f, 552f, 626f, 700f, 22f, Color(0xFF1D1E21), Color(0xFF141517), Color(0xFF2C2D32))
                drawRoundRect(Color(0xFF26272B), Offset(1020f, 562f), Size(240f, 40f), CornerRadius(6f))
                drawRect(Color(0xFF060607), Offset(1035f, 577f), Size(210f, 8f))
                listOf(920f, 980f).forEach { x -> drawRoundRect(Color(0xFF0A0A0C), Offset(x, 658f), Size(46f, 22f), CornerRadius(4f)) }
                drawRoundRect(Color(0xFF2E2F35), Offset(1290f, 650f), Size(46f, 12f), CornerRadius(3f))
            },
        )

        /** PC Engine: blanca y pequeña; la tarjeta (HuCard) entra por la ranura del frente. */
        private val PCENGINE = CartConsole(
            slotX = 1140f, slotY = 662f, slotW = 130f, cartW = 140f, cartH = 96f, cartColor = Color(0xFFDADCE0), front = true,
            led = Offset(1240f, 682f),
            draw = {
                box(1020f, 240f, 594f, 632f, 700f, 14f, Color(0xFFEDEEF0), Color(0xFFE2E3E6), Color(0xFFC9CBCF))
                drawRect(Color(0xFF15161A), Offset(1075f, 658f), Size(130f, 8f))
                drawRoundRect(Color(0xFFD13A3A), Offset(1040f, 600f), Size(40f, 10f), CornerRadius(3f))
                drawRect(Color(0xFF2E8B57), Offset(1236f, 640f), Size(20f, 26f))
            },
        )

        /** Atari 5200: grande y negra, con la franja plateada delante y la ranura arriba. */
        private val A5200 = CartConsole(
            slotX = 1140f, slotY = 578f, slotW = 170f, cartW = 160f, cartH = 130f, cartColor = Color(0xFF1E1E21),
            led = Offset(1360f, 680f),
            draw = {
                box(870f, 540f, 540f, 630f, 700f, 26f, Color(0xFF1C1C1F), Color(0xFF141416), Color(0xFF2C2C30))
                drawRoundRect(Color(0xFF0E0E10), Offset(1040f, 562f), Size(200f, 34f), CornerRadius(4f))
                drawRect(Color(0xFF040405), Offset(1055f, 575f), Size(170f, 7f))
                drawRect(Brush.verticalGradient(listOf(Color(0xFFD8D8DC), Color(0xFF9A9AA0)), 608f, 628f), Offset(878f, 608f), Size(524f, 20f))
                listOf(940f, 1020f, 1260f, 1340f).forEach { x -> drawRoundRect(Color(0xFF0A0A0B), Offset(x, 652f), Size(50f, 22f), CornerRadius(4f)) }
            },
        )

        /** Atari 7800: negra y en cuña, con la franja plateada con raya roja y la ranura arriba. */
        private val A7800 = CartConsole(
            slotX = 1140f, slotY = 580f, slotW = 140f, cartW = 130f, cartH = 110f, cartColor = Color(0xFF1E1E21),
            led = Offset(1360f, 680f),
            draw = {
                box(880f, 520f, 548f, 630f, 700f, 22f, Color(0xFF1E1E21), Color(0xFF141416), Color(0xFF2C2C30))
                drawRoundRect(Color(0xFF0E0E10), Offset(1060f, 566f), Size(160f, 30f), CornerRadius(4f))
                drawRect(Color(0xFF040405), Offset(1070f, 577f), Size(140f, 7f))
                drawRect(Color(0xFFC9C9CE), Offset(886f, 604f), Size(508f, 22f))
                drawRect(Color(0xFFC0392B), Offset(886f, 612f), Size(508f, 4f))
                listOf(940f, 1010f).forEach { x -> drawRoundRect(Color(0xFF0A0A0B), Offset(x, 656f), Size(46f, 20f), CornerRadius(4f)) }
            },
        )

        /** Atari Jaguar: negra y redondeada, con la ranura en el centro de arriba. */
        private val JAGUAR = CartConsole(
            slotX = 1140f, slotY = 578f, slotW = 150f, cartW = 140f, cartH = 116f, cartColor = Color(0xFF1C1C1F),
            led = Offset(1330f, 650f), ledOn = Color(0xFFE03030),
            draw = {
                drawPath(Path().apply {
                    moveTo(900f, 640f); cubicTo(900f, 568f, 980f, 548f, 1140f, 548f); cubicTo(1300f, 548f, 1380f, 568f, 1380f, 640f); close()
                }, Color(0xFF1F2023))
                drawRoundRect(Color(0xFF141517), Offset(1050f, 556f), Size(180f, 44f), CornerRadius(16f))
                drawRect(Color(0xFF040405), Offset(1065f, 575f), Size(150f, 7f))
                drawRect(Color(0xFF17181B), Offset(900f, 638f), Size(480f, 62f))
                drawRect(Color(0xFFB02020), Offset(980f, 612f), Size(70f, 6f))
            },
        )

        /** ColecoVision: negra con detalles plateados, los mandos encajados a la izquierda y la ranura a la derecha. */
        private val COLECO = CartConsole(
            slotX = 1250f, slotY = 582f, slotW = 140f, cartW = 130f, cartH = 100f, cartColor = Color(0xFF1C1C1F),
            led = Offset(1370f, 676f),
            draw = {
                box(870f, 540f, 550f, 628f, 700f, 20f, Color(0xFF1C1C1F), Color(0xFF131315), Color(0xFF2C2C30))
                // Mandos encajados (teclado numérico)
                listOf(910f, 1010f).forEach { x ->
                    drawRoundRect(Color(0xFF0D0D0E), Offset(x, 560f), Size(84f, 56f), CornerRadius(6f))
                    for (i in 0 until 3) for (j in 0 until 3) drawRect(Color(0xFFC9C9CE), Offset(x + 14f + i * 20f, 568f + j * 14f), Size(14f, 9f))
                }
                drawRoundRect(Color(0xFF0E0E10), Offset(1172f, 566f), Size(156f, 30f), CornerRadius(4f))
                drawRect(Color(0xFF040405), Offset(1180f, 579f), Size(140f, 7f))
                drawRect(Color(0xFF9E9EA4), Offset(880f, 640f), Size(520f, 6f))
            },
        )

        /** Intellivision: marrón con paneles dorados, los mandos encajados y la ranura a la derecha. */
        private val INTELLIVISION = CartConsole(
            slotX = 1290f, slotY = 584f, slotW = 120f, cartW = 112f, cartH = 96f, cartColor = Color(0xFF2B211A),
            led = Offset(1370f, 676f),
            draw = {
                box(870f, 540f, 552f, 628f, 700f, 22f, Color(0xFF3A2C22), Color(0xFF2A1F18), Color(0xFF4A3A2E))
                drawPath(Path().apply { moveTo(900f, 556f); lineTo(1220f, 556f); lineTo(1226f, 618f); lineTo(890f, 618f); close() }, Color(0xFFD9BE8C))
                listOf(920f, 1040f).forEach { x ->
                    drawRoundRect(Color(0xFF2A2420), Offset(x, 566f), Size(100f, 42f), CornerRadius(6f))
                    for (i in 0 until 3) for (j in 0 until 2) drawRect(Color(0xFFE6D3A8), Offset(x + 18f + i * 24f, 572f + j * 16f), Size(16f, 10f))
                }
                drawRoundRect(Color(0xFF1A1410), Offset(1222f, 568f), Size(136f, 30f), CornerRadius(4f))
                drawRect(Color(0xFF050404), Offset(1230f, 581f), Size(120f, 7f))
                drawRect(Brush.horizontalGradient(listOf(Color(0xFF5A3A22), Color(0xFF7A5230), Color(0xFF5A3A22))), Offset(870f, 640f), Size(540f, 40f))
            },
        )

        /** Odyssey 2: plateada con el teclado integrado y la ranura atrás. */
        private val ODYSSEY2 = CartConsole(
            slotX = 1060f, slotY = 572f, slotW = 120f, cartW = 112f, cartH = 96f, cartColor = Color(0xFF1C1C1F),
            led = Offset(1360f, 676f),
            draw = {
                box(880f, 520f, 548f, 640f, 700f, 22f, Color(0xFFBFC1C6), Color(0xFFA9ABB1), Color(0xFF8E9096))
                drawRoundRect(Color(0xFF1A1A1C), Offset(990f, 556f), Size(140f, 28f), CornerRadius(3f))
                drawRect(Color(0xFF040405), Offset(1000f, 568f), Size(120f, 6f))
                drawRect(Color(0xFFB02020), Offset(1240f, 560f), Size(60f, 18f))
                for (row in 0 until 3) for (k in 0 until 12) drawRoundRect(Color(0xFF2A2A2E), Offset(920f + row * 6f + k * 36f, 592f + row * 15f), Size(28f, 11f), CornerRadius(2f))
            },
        )

        /** SG-1000: blanca con franjas azul y roja, ranura arriba. */
        private val SG1000 = CartConsole(
            slotX = 1140f, slotY = 576f, slotW = 140f, cartW = 130f, cartH = 100f, cartColor = Color(0xFF1C1C1F),
            led = Offset(1350f, 676f),
            draw = {
                box(880f, 520f, 550f, 630f, 700f, 24f, Color(0xFFEDEDEF), Color(0xFFE0E0E4), Color(0xFFC8C8CD))
                drawRoundRect(Color(0xFFD6D6DA), Offset(1050f, 558f), Size(180f, 36f), CornerRadius(5f))
                drawRect(Color(0xFF060607), Offset(1070f, 573f), Size(140f, 7f))
                drawRect(Color(0xFF1E4FA8), Offset(888f, 604f), Size(504f, 14f))
                drawRect(Color(0xFFC62828), Offset(888f, 620f), Size(504f, 6f))
                drawRoundRect(Color(0xFFC62828), Offset(1300f, 650f), Size(50f, 16f), CornerRadius(3f))
            },
        )

        /** Channel F: negra con frente imitando madera; el cartucho entra por el frente. */
        private val CHANNELF = CartConsole(
            slotX = 1140f, slotY = 660f, slotW = 120f, cartW = 120f, cartH = 120f, cartColor = Color(0xFFE6C34A), front = true,
            led = Offset(1340f, 680f),
            draw = {
                box(880f, 520f, 552f, 628f, 700f, 22f, Color(0xFF1C1C1F), Color(0xFF151517), Color(0xFF2C2C30))
                drawRect(Brush.horizontalGradient(listOf(Color(0xFF6B3E1F), Color(0xFF8A5530), Color(0xFF6B3E1F))), Offset(880f, 632f), Size(520f, 60f))
                drawRect(Color(0xFF0A0A0B), Offset(1080f, 655f), Size(120f, 10f))
                listOf(950f, 1000f, 1280f, 1330f).forEach { x -> drawRoundRect(Color(0xFFD9D9DD), Offset(x, 572f), Size(36f, 20f), CornerRadius(3f)) }
            },
        )

        fun of(consoleId: String): CartConsole = when (consoleId) {
            "nes" -> NES; "snes" -> SNES; "n64" -> N64; "mastersystem" -> MASTERSYSTEM; "atari2600" -> ATARI
            "neogeo" -> NEOGEO; "pcengine" -> PCENGINE; "atari5200" -> A5200; "atari7800" -> A7800; "atarijaguar" -> JAGUAR
            "colecovision" -> COLECO; "intellivision" -> INTELLIVISION; "odyssey2" -> ODYSSEY2; "sg1000" -> SG1000; "channelf" -> CHANNELF
            else -> MEGADRIVE
        }
    }
}

/** Cartucho grande de sobremesa con la carátula en la etiqueta. */
@Composable
private fun BigCartridge(game: Game, body: Color) {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(topStartPercent = 14, topEndPercent = 14, bottomStartPercent = 4, bottomEndPercent = 4)).background(body)) {
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0 until 4) drawRect(Color.Black.copy(alpha = 0.18f), Offset(size.width * 0.08f, size.height * (0.04f + i * 0.035f)), Size(size.width * 0.84f, size.height * 0.012f))
        }
        Box(Modifier.offset(0.dp, 0.dp).fillMaxSize().padding(0.12f, 0.22f, 0.12f, 0.1f).clip(RoundedCornerShape(6))) { CoverArt(game) }
    }
}

/** Relleno proporcional (en fracciones del tamaño) para la etiqueta del cartucho. */
private fun Modifier.padding(l: Float, t: Float, r: Float, b: Float) = this.then(
    Modifier.graphicsLayer {
        scaleX = 1f - l - r; scaleY = 1f - t - b
        translationX = size.width * (l - r) / 2; translationY = size.height * (t - b) / 2
    },
)

@Composable
internal fun CartScene(game: Game, p: Float, u: Dp) {
    val m = CartConsole.of(game.consoleId)
    Television(u, on = seg(p, 0.62f, 0.69f), game = game, s = LaunchKind.CART.screen)
    val caseH = 420f
    val boxHu = boxSize(game, u * caseH).second / u
    val caseX = 150f; val caseY = TABLE_Y - boxHu - 6f
    val frontW = caseH * frontAspect(game)
    val open = seg(p, 0.12f, 0.27f)
    val caseAlpha = ease(seg(p, 0f, 0.1f)) * (1f - seg(p, 0.5f, 0.6f))
    ClosedCase(game, u, caseX, caseY, caseH, caseAlpha, open) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(4)).background(Color(0xFF8C7A60)))
    }
    val startCx = caseX + frontW / 2; val startCy = caseY + (boxHu - caseH) + caseH * 0.45f
    val s0 = minOf(1f, frontW * 0.8f / m.cartW)
    val fly = ease(seg(p, 0.30f, 0.50f))
    val down = ease(seg(p, 0.50f, 0.56f))
    val sEnd = if (m.front) m.slotW / m.cartW else 1f
    val scale = lerp(s0, sEnd, fly) * (if (m.front) lerp(1f, 0.55f, down) else 1f)
    val targetY = if (m.front) m.slotY else m.slotY - m.cartH / 2 - 26f
    val cx = lerp(startCx, m.slotX, fly)
    var cy = lerp(startCy, targetY, fly) - 130f * sin(PI.toFloat() * fly)
    if (!m.front) cy += down * (m.cartH * (1f - m.stick) + 26f)
    val cartAlpha = if (m.front) 1f - down else 1f

    // Consola
    Canvas(Modifier.offset(u * 860, u * 520).size(u * 560, u * 185)) {
        val k = size.width / 560f
        withTransform({ scale(k, k, Offset.Zero); translate(-860f, -520f) }) {
            m.draw(this)
            drawCircle(if (p > 0.57f) m.ledOn else lerpC(m.ledOn, Color.Black, 0.7f), 5f, m.led)
        }
    }
    val cart = @Composable {
        Box(Modifier.offset(u * (cx - m.cartW / 2), u * (cy - m.cartH / 2)).size(u * m.cartW, u * m.cartH).alpha(cartAlpha).graphicsLayer {
            scaleX = scale; scaleY = scale * (if (m.front && down > 0f) lerp(1f, 0.35f, down) else 1f)
            rotationZ = 10f * sin(PI.toFloat() * fly)
        }) { BigCartridge(game, m.cartColor) }
    }
    when {
        p < 0.30f -> if (open > 0.5f) cart()
        p < 0.50f || m.front -> cart()
        // Metido por arriba: lo que entra en la ranura queda oculto; el resto asoma.
        else -> Box(Modifier.size(u * SCENE_W, u * m.slotY).clipToBounds()) { cart() }
    }
}

private fun lerpC(a: Color, b: Color, t: Float) = Color(
    a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha + (b.alpha - a.alpha) * t,
)
