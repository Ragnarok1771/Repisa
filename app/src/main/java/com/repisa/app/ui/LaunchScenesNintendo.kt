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
import androidx.compose.ui.graphics.TransformOrigin
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
import kotlin.math.sin

/**
 * Escenas de la primera tanda de Nintendo: portátiles de cartucho (Game Boy, Game Boy Color, Game Boy Advance,
 * DS y 3DS), GameCube y Wii. Todo con formas propias (sin logos): se reconocen por su silueta y sus colores.
 */

// ------------------------------------------------------------------ Portátiles

/** Cómo es cada portátil en la escena (unidades de 1600 × 900). */
internal data class Handheld(
    val body: Color,
    val edge: Color,
    /** DS y 3DS: se abren como un libro; [body] es la mitad de abajo y [lid] la de arriba. */
    val bodyRect: SceneRect,
    val lid: SceneRect? = null,
    val screen: SceneRect,
    val bottomScreen: SceneRect? = null,
    val bezel: SceneRect? = null,
    val bezelColor: Color = Color(0xFF15161A),
    val cartW: Float, val cartH: Float, val cartColor: Color,
    /** Ancho que ocupa el cartucho al entrar en la ranura, centro de la ranura y borde por donde entra. */
    val slotW: Float, val slotX: Float, val slotY: Float,
    val caseInside: Color,
    /** Game Boy original: pantalla verdosa. */
    val greenScreen: Boolean = false,
    /** Color que tiñe la pantalla (Virtual Boy: rojo). */
    val tint: Color? = null,
    /** Luz de encendido (si no, junto al marco de la pantalla). */
    val led: Offset? = null,
    /** Esquinas muy redondas (PSP, Vita) y juego en UMD (PSP) en vez de cartucho. */
    val corner: Float = 22f,
    val umd: Boolean = false,
    val draw: DrawScope.() -> Unit = {},
) {
    val zoomScreen: SceneRect get() = screen

    companion object {
        val consoles = setOf("gb", "gbc", "gba", "nds", "3ds", "psp", "psvita", "gamegear", "atarilynx", "ngp", "wonderswan",
            "pokemini", "virtualboy", "gameandwatch", "supervision", "ngage", "vectrex")

        private val GB = Handheld(
            body = Color(0xFFC9C7BF), edge = Color(0xFFA9A79F),
            bodyRect = SceneRect(1060f, 270f, 260f, 430f),
            bezel = SceneRect(1082f, 296f, 216f, 178f), bezelColor = Color(0xFF5A5E70),
            screen = SceneRect(1120f, 318f, 140f, 126f),
            cartW = 110f, cartH = 126f, cartColor = Color(0xFF9EA0A6),
            slotW = 170f, slotX = 1190f, slotY = 270f, caseInside = Color(0xFF8C7A60), greenScreen = true,
            draw = {
                dpad(Offset(1108f, 548f), Color(0xFF2B2B30))
                drawCircle(Color(0xFF8E1F4E), 17f, Offset(1288f, 528f)); drawCircle(Color(0xFF8E1F4E), 17f, Offset(1250f, 552f))
                listOf(1168f, 1208f).forEach { x -> drawRoundRect(Color(0xFF7D7C78), Offset(x - 16f, 610f), Size(32f, 9f), CornerRadius(5f)) }
                for (i in 0 until 6) drawLine(Color(0xFF9D9B93), Offset(1250f + i * 10f, 640f), Offset(1270f + i * 10f, 680f), 3f)
            },
        )
        private val GBC = Handheld(
            body = Color(0xFF5E2D8C), edge = Color(0xFF45206A),
            bodyRect = SceneRect(1070f, 300f, 240f, 400f),
            bezel = SceneRect(1088f, 322f, 204f, 168f), bezelColor = Color(0xFF17171B),
            screen = SceneRect(1120f, 338f, 140f, 126f),
            cartW = 110f, cartH = 126f, cartColor = Color(0xFF2F3036),
            slotW = 160f, slotX = 1190f, slotY = 300f, caseInside = Color(0xFF8C7A60),
            draw = {
                dpad(Offset(1112f, 560f), Color(0xFF26262B))
                drawCircle(Color(0xFF3A3A40), 16f, Offset(1280f, 540f)); drawCircle(Color(0xFF3A3A40), 16f, Offset(1244f, 564f))
                listOf(1170f, 1210f).forEach { x -> drawRoundRect(Color(0xFF3A3A40), Offset(x - 15f, 618f), Size(30f, 8f), CornerRadius(4f)) }
            },
        )
        // Game Boy Advance (la original, horizontal), medida sobre una foto de frente: arriba plana en el centro y
        // bajando suave hacia los lados, lados que se abomban hacia abajo, marco de pantalla muy grande, cruceta y
        // botones gris claro, B y A a la derecha (A más alta), luz de encendido y altavoz de rayas inclinadas.
        private val GBA = Handheld(
            body = Color(0xFF4B44A3), edge = Color(0xFF39338A),
            bodyRect = SceneRect(874f, 420f, 572f, 282f),
            bezel = SceneRect(1014f, 444f, 303f, 224f), bezelColor = Color(0xFF34353A),
            screen = SceneRect(1054f, 458f, 228f, 152f),
            cartW = 130f, cartH = 80f, cartColor = Color(0xFF8E9198),
            slotW = 170f, slotX = 1160f, slotY = 420f, caseInside = Color(0xFF8C7A60),
            led = Offset(1322f, 457f),
            draw = {
                val gray = Color(0xFFBDBFC6)
                // Cruceta gris claro arriba a la izquierda
                drawRoundRect(gray, Offset(974f - 36f, 511f - 12f), Size(72f, 24f), CornerRadius(5f))
                drawRoundRect(gray, Offset(974f - 12f, 511f - 36f), Size(24f, 72f), CornerRadius(5f))
                // Start y Select: botoncitos redondos con su hueco alargado a la izquierda
                listOf(Offset(999f, 586f), Offset(992f, 616f)).forEach { c ->
                    drawRoundRect(Color(0xFF423B95), Offset(c.x - 46f, c.y - 9f), Size(42f, 18f), CornerRadius(9f))
                    drawCircle(gray, 9f, c)
                }
                // B (más baja) y A
                listOf(Offset(1346f, 530f), Offset(1388f, 515f)).forEach { c ->
                    drawCircle(Color(0xFF9EA1A9), 16f, c); drawCircle(gray, 14.5f, c)
                }
                // Altavoz: rayas inclinadas abajo a la derecha
                for (i in 0 until 6) drawLine(Color(0xFF2E2878), Offset(1324f, 578f + i * 11f), Offset(1392f, 570f + i * 11f), 4f)
            },
        )
        private val DS = Handheld(
            body = Color(0xFFEEF0F3), edge = Color(0xFFC3C8D0),
            bodyRect = SceneRect(930f, 444f, 460f, 256f), lid = SceneRect(930f, 188f, 460f, 256f),
            screen = SceneRect(1045f, 230f, 230f, 172f), bottomScreen = SceneRect(1045f, 486f, 230f, 172f),
            cartW = 90f, cartH = 84f, cartColor = Color(0xFF6C7078),
            slotW = 110f, slotX = 1160f, slotY = 444f, caseInside = Color(0xFFE3E6EA),
            draw = { dsButtons(Color(0xFF9AA0AA)) },
        )
        private val N3DS = Handheld(
            body = Color(0xFF1F6A98), edge = Color(0xFF154B6E),
            bodyRect = SceneRect(930f, 444f, 460f, 256f), lid = SceneRect(930f, 188f, 460f, 256f),
            screen = SceneRect(1010f, 238f, 300f, 180f), bottomScreen = SceneRect(1050f, 480f, 220f, 165f),
            cartW = 90f, cartH = 84f, cartColor = Color(0xFFD9DCE1),
            slotW = 110f, slotX = 1160f, slotY = 444f, caseInside = Color(0xFFE3E6EA),
            draw = { dsButtons(Color(0xFF15161A)) ; drawCircle(Color(0xFF15161A), 22f, Offset(985f, 500f)) },
        )

        // PSP (1000): negra y muy alargada, pantalla ancha, cruceta y palanca a la izquierda, cuatro botones a la
        // derecha y la franja plateada de abajo. El UMD entra por detrás (aquí, por el borde de arriba).
        private val PSP = Handheld(
            body = Color(0xFF16171B), edge = Color(0xFF34363D),
            bodyRect = SceneRect(850f, 450f, 620f, 250f),
            screen = SceneRect(1000f, 486f, 320f, 180f),
            cartW = 92f, cartH = 98f, cartColor = Color(0xFF1E1F24),
            slotW = 120f, slotX = 1160f, slotY = 450f, caseInside = Color(0xFF1C1D22),
            led = Offset(1410f, 664f), corner = 118f, umd = true,
            draw = {
                val k = Color(0xFF2A2C33); val sym = Color(0xFF8D9099)
                // Cruceta de cuatro flechas sueltas y palanca pequeña
                listOf(Offset(922f, 512f), Offset(922f, 560f), Offset(898f, 536f), Offset(946f, 536f)).forEach { drawCircle(k, 13f, it) }
                drawCircle(Color(0xFF0E0F12), 20f, Offset(920f, 616f)); drawCircle(Color(0xFF3A3C44), 15f, Offset(920f, 616f))
                // Cuatro botones de la derecha
                listOf(Offset(1398f, 510f), Offset(1398f, 562f), Offset(1372f, 536f), Offset(1424f, 536f)).forEach {
                    drawCircle(k, 15f, it); drawCircle(sym, 6f, it, style = Stroke(2f))
                }
                // Franja plateada de abajo con sus botoncitos
                drawRoundRect(Color(0xFFB9BCC3), Offset(960f, 676f), Size(400f, 8f), CornerRadius(4f))
                for (i in 0 until 6) drawRoundRect(Color(0xFF3A3C44), Offset(1000f + i * 52f, 682f), Size(34f, 9f), CornerRadius(4f))
            },
        )
        // PS Vita: cápsula negra más redondeada, pantalla ancha, una palanca bajo cada lado. Tarjeta pequeña arriba.
        private val VITA = Handheld(
            body = Color(0xFF111215), edge = Color(0xFF2E3036),
            bodyRect = SceneRect(866f, 436f, 588f, 264f),
            screen = SceneRect(992f, 478f, 336f, 189f),
            cartW = 70f, cartH = 80f, cartColor = Color(0xFF26272D),
            slotW = 74f, slotX = 1270f, slotY = 436f, caseInside = Color(0xFF1C1D22),
            led = Offset(1310f, 684f), corner = 128f,
            draw = {
                val k = Color(0xFF26282E); val sym = Color(0xFF8D9099)
                listOf(Offset(930f, 500f), Offset(930f, 540f), Offset(910f, 520f), Offset(950f, 520f)).forEach { drawCircle(k, 11f, it) }
                listOf(Offset(1390f, 498f), Offset(1390f, 542f), Offset(1368f, 520f), Offset(1412f, 520f)).forEach {
                    drawCircle(k, 12f, it); drawCircle(sym, 5f, it, style = Stroke(2f))
                }
                // Palancas
                listOf(Offset(934f, 612f), Offset(1386f, 612f)).forEach { drawCircle(Color(0xFF07080A), 26f, it); drawCircle(Color(0xFF2E3037), 20f, it) }
                drawCircle(Color(0xFF26282E), 9f, Offset(918f, 668f))
            },
        )

        // Game Gear: negra y apaisada, pantalla en el centro con marco grande, cruceta a la izquierda y dos botones.
        private val GAMEGEAR = Handheld(
            body = Color(0xFF1E1F23), edge = Color(0xFF34353B),
            bodyRect = SceneRect(866f, 450f, 588f, 250f),
            bezel = SceneRect(1012f, 464f, 296f, 216f), bezelColor = Color(0xFF111215),
            screen = SceneRect(1056f, 486f, 208f, 166f),
            cartW = 110f, cartH = 76f, cartColor = Color(0xFF1C1C1F),
            slotW = 150f, slotX = 1160f, slotY = 450f, caseInside = Color(0xFF8C7A60),
            led = Offset(1032f, 572f), corner = 70f,
            draw = {
                drawCircle(Color(0xFF2A2B30), 42f, Offset(940f, 560f)); dpad(Offset(940f, 560f), Color(0xFF111215))
                listOf(Offset(1370f, 578f), Offset(1416f, 550f)).forEach { drawCircle(Color(0xFF2E2F35), 20f, it); drawCircle(Color(0xFF3B3C43), 16f, it) }
                drawCircle(Color(0xFF2A5BB0), 9f, Offset(1392f, 500f)) // start
                for (i in 0 until 3) for (j in 0 until 4) drawCircle(Color(0xFF111215), 3f, Offset(920f + i * 12f, 630f + j * 12f)) // altavoz
            },
        )

        // Atari Lynx: negra y muy ancha, pantalla en el centro, cruceta a la izquierda y botones a la derecha.
        private val LYNX = Handheld(
            body = Color(0xFF26272B), edge = Color(0xFF3A3B41),
            bodyRect = SceneRect(840f, 470f, 640f, 230f),
            bezel = SceneRect(1040f, 488f, 240f, 190f), bezelColor = Color(0xFF15161A),
            screen = SceneRect(1072f, 512f, 176f, 112f),
            cartW = 90f, cartH = 70f, cartColor = Color(0xFF2E2F35),
            slotW = 100f, slotX = 1380f, slotY = 470f, caseInside = Color(0xFF8C7A60), corner = 90f,
            draw = {
                drawCircle(Color(0xFF1C1D21), 44f, Offset(932f, 580f)); dpad(Offset(932f, 580f), Color(0xFF0E0F11))
                listOf(Offset(1390f, 600f), Offset(1430f, 640f)).forEach { drawCircle(Color(0xFF1C1D21), 18f, it); drawCircle(Color(0xFF3A3B41), 14f, it) }
                for (i in 0 until 3) drawRoundRect(Color(0xFF3A3B41), Offset(1300f, 520f + i * 22f), Size(30f, 10f), CornerRadius(3f))
            },
        )
        // Neo Geo Pocket Color: pequeña y apaisada, gris oscuro, palanca a la izquierda y A/B a la derecha.
        private val NGP = Handheld(
            body = Color(0xFF2E3036), edge = Color(0xFF43464E),
            bodyRect = SceneRect(940f, 470f, 440f, 230f),
            bezel = SceneRect(1080f, 484f, 170f, 168f), bezelColor = Color(0xFF16171A),
            screen = SceneRect(1098f, 500f, 134f, 124f),
            cartW = 70f, cartH = 70f, cartColor = Color(0xFF1C1D21),
            slotW = 76f, slotX = 1165f, slotY = 470f, caseInside = Color(0xFF8C7A60), corner = 60f,
            draw = {
                drawCircle(Color(0xFF16171A), 30f, Offset(1010f, 560f)); drawCircle(Color(0xFF55585F), 18f, Offset(1010f, 560f))
                listOf(Offset(1300f, 590f), Offset(1334f, 560f)).forEach { drawCircle(Color(0xFF16171A), 16f, it) }
            },
        )
        // WonderSwan: negra translúcida, dos cruces a la izquierda, pantalla grande y A/B a la derecha.
        private val WONDERSWAN = Handheld(
            body = Color(0xFF2A2C31), edge = Color(0xFF474A52),
            bodyRect = SceneRect(900f, 466f, 520f, 234f),
            bezel = SceneRect(1056f, 482f, 230f, 196f), bezelColor = Color(0xFFDADCE0),
            screen = SceneRect(1080f, 500f, 182f, 150f),
            cartW = 76f, cartH = 70f, cartColor = Color(0xFF1C1D21),
            slotW = 84f, slotX = 1170f, slotY = 466f, caseInside = Color(0xFF8C7A60), corner = 70f,
            draw = {
                listOf(Offset(960f, 540f), Offset(990f, 620f)).forEach { dpad(it, Color(0xFF16171A)) }
                listOf(Offset(1340f, 610f), Offset(1376f, 580f)).forEach { drawCircle(Color(0xFF16171A), 16f, it) }
            },
        )
        // Pokémon Mini: diminuta y vertical, transparente azulada, pantalla arriba y cruceta abajo.
        private val POKEMINI = Handheld(
            body = Color(0xFFBFD6EC), edge = Color(0xFF8DAFD0),
            bodyRect = SceneRect(1080f, 380f, 200f, 320f),
            bezel = SceneRect(1100f, 406f, 160f, 140f), bezelColor = Color(0xFF27292E),
            screen = SceneRect(1118f, 424f, 124f, 100f),
            cartW = 60f, cartH = 50f, cartColor = Color(0xFF2E2F35),
            slotW = 64f, slotX = 1180f, slotY = 380f, caseInside = Color(0xFF8C7A60), corner = 60f,
            draw = { dpad(Offset(1140f, 610f), Color(0xFF2A6FD0)); drawCircle(Color(0xFF2A6FD0), 14f, Offset(1226f, 620f)) },
        )
        // Virtual Boy: el visor rojo y negro sobre su soporte; la pantalla se ve roja.
        private val VIRTUALBOY = Handheld(
            body = Color(0xFFC62828), edge = Color(0xFF8E1C1C),
            bodyRect = SceneRect(960f, 300f, 400f, 150f),
            bezel = SceneRect(1040f, 330f, 240f, 90f), bezelColor = Color(0xFF111215),
            screen = SceneRect(1060f, 340f, 200f, 70f),
            cartW = 80f, cartH = 70f, cartColor = Color(0xFF1C1D21),
            slotW = 86f, slotX = 1160f, slotY = 300f, caseInside = Color(0xFF8C7A60), corner = 40f,
            tint = Color(0xAAE01A1A),
            draw = {
                drawRect(Color(0xFF111215), Offset(960f, 430f), Size(400f, 20f))
                // Soporte de dos patas
                drawLine(Color(0xFF26272B), Offset(1160f, 450f), Offset(1160f, 560f), 14f)
                drawLine(Color(0xFF26272B), Offset(1160f, 560f), Offset(1080f, 700f), 10f)
                drawLine(Color(0xFF26272B), Offset(1160f, 560f), Offset(1240f, 700f), 10f)
            },
        )
        // Game & Watch: plana y apaisada, plateada con marco rojo, pantalla de cristal líquido y dos botones. Sin cartucho.
        private val GAMEWATCH = Handheld(
            body = Color(0xFFC9CCD2), edge = Color(0xFFB22222),
            bodyRect = SceneRect(930f, 480f, 460f, 220f),
            bezel = SceneRect(1060f, 498f, 200f, 150f), bezelColor = Color(0xFF8E9198),
            screen = SceneRect(1078f, 512f, 164f, 120f),
            cartW = 0f, cartH = 0f, cartColor = Color.Transparent,
            slotW = 0f, slotX = 1160f, slotY = 480f, caseInside = Color(0xFF8C7A60), corner = 16f, greenScreen = false,
            tint = Color(0x33A8B49A),
            draw = {
                listOf(Offset(990f, 640f), Offset(1330f, 640f)).forEach { drawCircle(Color(0xFFB22222), 24f, it); drawCircle(Color(0xFFD93A3A), 20f, it) }
                listOf(1110f, 1160f, 1210f).forEach { x -> drawRoundRect(Color(0xFFB22222), Offset(x - 14f, 668f), Size(28f, 10f), CornerRadius(4f)) }
            },
        )
        // Watara Supervision: gris, con la parte de la pantalla inclinada arriba y los mandos abajo.
        private val SUPERVISION = Handheld(
            body = Color(0xFF8E9198), edge = Color(0xFF6E7178),
            bodyRect = SceneRect(1050f, 330f, 260f, 370f),
            bezel = SceneRect(1068f, 346f, 224f, 196f), bezelColor = Color(0xFF3A3C42),
            screen = SceneRect(1092f, 368f, 176f, 150f),
            cartW = 90f, cartH = 70f, cartColor = Color(0xFF2E2F35),
            slotW = 96f, slotX = 1180f, slotY = 330f, caseInside = Color(0xFF8C7A60), greenScreen = true, corner = 30f,
            draw = {
                drawRect(Color(0xFF6E7178), Offset(1050f, 556f), Size(260f, 6f))
                dpad(Offset(1110f, 620f), Color(0xFF2E3036))
                listOf(Offset(1236f, 630f), Offset(1270f, 610f)).forEach { drawCircle(Color(0xFF3FB59A), 14f, it) }
            },
        )
        // N-Gage: con forma de taco, gris oscura, pantalla vertical en el centro, cruceta a la izquierda y teclado a la derecha.
        private val NGAGE = Handheld(
            body = Color(0xFF3A3D45), edge = Color(0xFF55585F),
            bodyRect = SceneRect(880f, 470f, 560f, 230f),
            bezel = SceneRect(1094f, 478f, 132f, 168f), bezelColor = Color(0xFF16171A),
            screen = SceneRect(1108f, 494f, 104f, 130f),
            cartW = 50f, cartH = 40f, cartColor = Color(0xFF2E2F35),
            slotW = 54f, slotX = 1160f, slotY = 470f, caseInside = Color(0xFF8C7A60), corner = 110f,
            draw = {
                drawCircle(Color(0xFFBFC2C9), 34f, Offset(980f, 570f)); drawCircle(Color(0xFF55585F), 24f, Offset(980f, 570f))
                for (i in 0 until 3) for (j in 0 until 4) drawRoundRect(Color(0xFFBFC2C9), Offset(1270f + i * 44f, 520f + j * 36f), Size(36f, 24f), CornerRadius(8f))
            },
        )
        // Vectrex: tiene su propia pantalla vertical (de vectores); el cartucho entra por el lado de arriba.
        private val VECTREX = Handheld(
            body = Color(0xFF1C1D20), edge = Color(0xFF34353A),
            bodyRect = SceneRect(1040f, 250f, 260f, 450f),
            bezel = SceneRect(1060f, 280f, 220f, 270f), bezelColor = Color(0xFF0B0B0C),
            screen = SceneRect(1080f, 296f, 180f, 236f),
            cartW = 100f, cartH = 80f, cartColor = Color(0xFF1C1D21),
            slotW = 110f, slotX = 1170f, slotY = 250f, caseInside = Color(0xFF8C7A60), corner = 18f,
            tint = Color(0x44FFFFFF),
            draw = {
                for (i in 0 until 6) drawRect(Color(0xFF26272B), Offset(1070f, 580f + i * 16f), Size(200f, 6f))
                drawRoundRect(Color(0xFF2A2B30), Offset(1200f, 670f), Size(70f, 18f), CornerRadius(4f))
            },
        )

        fun of(consoleId: String): Handheld = when (consoleId) {
            "atarilynx" -> LYNX; "ngp" -> NGP; "wonderswan" -> WONDERSWAN; "pokemini" -> POKEMINI; "virtualboy" -> VIRTUALBOY
            "gameandwatch" -> GAMEWATCH; "supervision" -> SUPERVISION; "ngage" -> NGAGE; "vectrex" -> VECTREX
            "gb" -> GB; "gbc" -> GBC; "gba" -> GBA; "3ds" -> N3DS; "psp" -> PSP; "psvita" -> VITA; "gamegear" -> GAMEGEAR; else -> DS
        }
    }
}

private fun DrawScope.dpad(c: Offset, color: Color) {
    drawRoundRect(color, Offset(c.x - 30f, c.y - 10f), Size(60f, 20f), CornerRadius(4f))
    drawRoundRect(color, Offset(c.x - 10f, c.y - 30f), Size(20f, 60f), CornerRadius(4f))
}

/** Botones de la mitad de abajo de DS / 3DS: cruceta a la izquierda y cuatro botones a la derecha. */
private fun DrawScope.dsButtons(color: Color) {
    dpad(Offset(985f, 572f), color)
    listOf(Offset(1335f, 548f), Offset(1335f, 596f), Offset(1311f, 572f), Offset(1359f, 572f)).forEach { drawCircle(color, 11f, it) }
}

@Composable
internal fun HandheldScene(game: Game, p: Float, u: Dp) {
    val m = Handheld.of(game.consoleId)
    val clam = m.lid != null
    // Caja de la izquierda, apoyada en la mesa.
    val caseH = if (clam) 380f else 420f
    val boxHu = boxSize(game, u * caseH).second / u
    val caseX = 170f; val caseY = TABLE_Y - boxHu - 6f
    val frontW = caseH * frontAspect(game)
    val rise = boxHu - caseH
    val open = seg(p, 0.13f, 0.28f)
    val caseAlpha = ease(seg(p, 0f, 0.1f)) * (1f - seg(p, 0.55f, 0.65f))

    // Cartucho: en la caja → encima de la ranura → entra.
    val startCx = caseX + frontW / 2; val startCy = caseY + rise + caseH * 0.42f
    // Sin cartucho (Game & Watch: el juego va dentro): solo se enciende.
    val hasCart = m.cartW > 0f
    val sEnd = if (hasCart) m.slotW / m.cartW else 1f
    val fly = ease(seg(p, 0.30f, 0.52f))
    val down = ease(seg(p, 0.52f, 0.57f))
    val scale = lerp(1f, sEnd, fly)
    val hEnd = m.cartH * sEnd
    val cx = lerp(startCx, m.slotX, fly)
    val cy = lerp(startCy, m.slotY - hEnd / 2 - 24f, fly) - 120f * sin(PI.toFloat() * fly) + down * (hEnd + 26f)
    val cart = @Composable {
        Box(Modifier.offset(u * (cx - m.cartW / 2), u * (cy - m.cartH / 2)).size(u * m.cartW, u * m.cartH).graphicsLayer {
            scaleX = scale; scaleY = scale; rotationZ = 12f * sin(PI.toFloat() * fly)
        }) { if (m.umd) Umd(game) else Cartridge(game, m.cartColor) }
    }

    ClosedCase(game, u, caseX, caseY, caseH, caseAlpha, open) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(6)).background(m.caseInside)) {
            Box(Modifier.offset(u * (frontW / 2 - m.cartW / 2 - 8), u * (caseH * 0.42f - m.cartH / 2 - 8)).size(u * (m.cartW + 16), u * (m.cartH + 16))
                .clip(RoundedCornerShape(10)).background(m.caseInside.copy(alpha = 1f).let { lerpColor(it, Color.Black, 0.25f) }))
        }
    }

    val lidOpen = if (clam) ease(seg(p, 0.57f, 0.67f)) else 1f
    val on = seg(p, if (clam) 0.63f else 0.59f, if (clam) 0.71f else 0.67f)

    if (hasCart && p >= 0.52f) cart() // entrando: queda detrás de la consola
    // Cuerpo (o mitad de abajo)
    val b = m.bodyRect
    Canvas(Modifier.offset(u * b.x, u * b.y).size(u * b.w, u * b.h)) {
        val k = size.width / b.w
        withTransform({ scale(k, k, Offset.Zero); translate(-b.x, -b.y) }) {
            if (m == Handheld.of("gba")) {
                // Botones L y R grises asomando por las esquinas de arriba
                val gray = Color(0xFFC5C7CC)
                drawRoundRect(gray, Offset(898f, 428f), Size(112f, 44f), CornerRadius(22f))
                drawRoundRect(gray, Offset(1310f, 428f), Size(112f, 44f), CornerRadius(22f))
                // Silueta medida sobre la foto de frente
                val shape = Path().apply {
                    moveTo(997f, 420f); lineTo(1323f, 420f)
                    cubicTo(1362f, 420f, 1384f, 430f, 1398f, 440f)
                    cubicTo(1424f, 456f, 1438f, 470f, 1442f, 502f)
                    cubicTo(1450f, 560f, 1450f, 640f, 1428f, 676f)
                    cubicTo(1398f, 692f, 1300f, 702f, 1160f, 702f)
                    cubicTo(1020f, 702f, 922f, 692f, 892f, 676f)
                    cubicTo(870f, 640f, 870f, 560f, 878f, 502f)
                    cubicTo(882f, 470f, 896f, 456f, 922f, 440f)
                    cubicTo(936f, 430f, 958f, 420f, 997f, 420f)
                    close()
                }
                drawPath(shape, m.body)
                drawPath(shape, m.edge, style = Stroke(3f))
            } else {
                drawRoundRect(m.body, Offset(b.x, b.y), Size(b.w, b.h), CornerRadius(m.corner))
                drawRoundRect(m.edge, Offset(b.x, b.y), Size(b.w, b.h), CornerRadius(m.corner), style = Stroke(4f))
                // Brillo del plástico negro (PSP, Vita)
                if (m.corner > 60f) drawRoundRect(Brush.verticalGradient(listOf(Color(0x22FFFFFF), Color.Transparent), b.y, b.y + b.h * 0.5f),
                    Offset(b.x + 20f, b.y + 6f), Size(b.w - 40f, b.h * 0.45f), CornerRadius(m.corner * 0.8f))
            }
            // Ranura por donde entra el cartucho (en el borde de arriba)
            drawRoundRect(Color(0xFF101114), Offset(m.slotX - m.slotW / 2 - 6f, m.slotY - 3f), Size(m.slotW + 12f, 7f), CornerRadius(3f))
            m.bezel?.let { z ->
                if (m.led != null) {
                    // GBA: marco con las esquinas de abajo más redondeadas (deja sitio al nombre de la consola)
                    drawPath(Path().apply {
                        addRoundRect(androidx.compose.ui.geometry.RoundRect(z.x, z.y, z.x + z.w, z.y + z.h,
                            CornerRadius(26f), CornerRadius(26f), CornerRadius(42f), CornerRadius(42f)))
                    }, m.bezelColor)
                } else drawRoundRect(m.bezelColor, Offset(z.x, z.y), Size(z.w, z.h), CornerRadius(12f))
            }
            m.draw(this)
            // Luz de encendido
            drawCircle(if (on > 0f) Color(0xFFFF3B3B) else Color(0xFF5A2020), 5f,
                m.led ?: Offset(m.bezel?.let { it.x + 14f } ?: (b.x + 40f), m.bezel?.let { it.y + it.h / 2 } ?: (b.y + 40f)))
        }
    }
    // Pantallas
    if (!clam) {
        Box(Modifier.offset(u * m.screen.x, u * m.screen.y).size(u * m.screen.w, u * m.screen.h).clip(RoundedCornerShape(2.dp))) {
            ScreenContent(game, on)
            if (m.greenScreen) Box(Modifier.fillMaxSize().background(Color(0x668BAC0F)))
            m.tint?.let { Box(Modifier.fillMaxSize().background(it)) }
        }
    } else {
        m.bottomScreen?.let { s ->
            Box(Modifier.offset(u * s.x, u * s.y).size(u * s.w, u * s.h).clip(RoundedCornerShape(2.dp)).background(Color(0xFF07090D))) {
                if (on > 0f) Box(Modifier.fillMaxSize().alpha(ease(on)).background(Brush.verticalGradient(listOf(Color(0xFF26324A), Color(0xFF0B0F18))))) {
                    Text(game.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 3,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(8.dp))
                }
            }
        }
        // Tapa: doblada sobre la mitad de abajo (se ve su espalda) y se levanta sobre la bisagra.
        val l = m.lid!!
        val angle = lerp(-180f, 0f, lidOpen)
        Box(Modifier.offset(u * l.x, u * l.y).size(u * l.w, u * l.h).graphicsLayer {
            rotationX = angle
            transformOrigin = TransformOrigin(0.5f, 1f)
            cameraDistance = 18f * density
        }) {
            if (angle < -90f) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRoundRect(m.body, Offset.Zero, size, CornerRadius(22f * size.width / l.w))
                    drawRoundRect(m.edge, Offset.Zero, size, CornerRadius(22f * size.width / l.w), style = Stroke(4f))
                    drawOval(lerpColor(m.body, Color.White, 0.18f), Offset(size.width * 0.3f, size.height * 0.35f), Size(size.width * 0.4f, size.height * 0.3f))
                }
            } else {
                Canvas(Modifier.fillMaxSize()) {
                    drawRoundRect(m.body, Offset.Zero, size, CornerRadius(22f * size.width / l.w))
                    drawRoundRect(m.edge, Offset.Zero, size, CornerRadius(22f * size.width / l.w), style = Stroke(4f))
                    // Marco negro de la pantalla de arriba
                    val k = size.width / l.w
                    val s = m.screen
                    drawRoundRect(Color(0xFF15161A), Offset((s.x - l.x - 14f) * k, (s.y - l.y - 14f) * k), Size((s.w + 28f) * k, (s.h + 28f) * k), CornerRadius(10f * k))
                }
                val s = m.screen
                Box(Modifier.offset(u * (s.x - l.x), u * (s.y - l.y)).size(u * s.w, u * s.h).clip(RoundedCornerShape(2.dp))) { ScreenContent(game, on) }
            }
        }
    }
    if (hasCart && p in 0.30f..0.52f) cart()
    if (hasCart && p < 0.30f && open > 0.5f) cart()
}

/** UMD de PSP: carcasa negra casi cuadrada con una ventana redonda por la que se ve el disco (con la carátula). */
@Composable
private fun Umd(game: Game) {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(topStartPercent = 30, topEndPercent = 30, bottomStartPercent = 12, bottomEndPercent = 12)).background(Color(0xFF1E1F24))) {
        Box(Modifier.align(Alignment.Center).fillMaxSize(0.82f).clip(CircleShape)) { Disc(game) }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha + (b.alpha - a.alpha) * t,
)

// ------------------------------------------------------------------ Consolas de tapa (GameCube, PS2 Slim)

/**
 * Consola que se carga por arriba: se ve un poco desde arriba (cara superior y frente), la tapa se levanta sobre su
 * bisagra de atrás, el disco cae tumbado en su hueco y la tapa se cierra.
 */
internal data class TopLoader(
    val front: SceneRect,
    /** Altura del borde de atrás de la cara de arriba y cuánto más estrecho es (perspectiva). */
    val backY: Float, val inset: Float,
    /** Parte de la cara de arriba que es tapa (x en el borde delantero) y hasta dónde sube abierta. */
    val lidL: Float, val lidR: Float, val lidOpenY: Float,
    val well: Offset, val wellW: Float, val wellH: Float, val discD: Float,
    val body: Color, val top: Color, val under: Color, val edge: Color,
    val led: Offset, val ledOn: Color,
    val tv: SceneRect,
    /** Tapa redonda (PS1): radios de la elipse que forma vista desde arriba. */
    val roundLid: Boolean = false, val lidRx: Float = 0f, val lidRy: Float = 0f,
    val details: DrawScope.() -> Unit = {},
) {
    fun backX(x: Float) = front.x + inset + (x - front.x) * (front.w - 2 * inset) / front.w

    companion object {
        /** PlayStation (la gris original): plana, con la tapa redonda grande en el centro de arriba. */
        val PS1 = TopLoader(
            front = SceneRect(880f, 632f, 520f, 68f), backY = 528f, inset = 24f,
            lidL = 0f, lidR = 0f, lidOpenY = 290f,
            well = Offset(1140f, 580f), wellW = 210f, wellH = 58f, discD = 210f,
            body = Color(0xFFBFBFC2), top = Color(0xFFCDCDD0), under = Color(0xFF9E9EA3), edge = Color(0xFF9A9AA0),
            led = Offset(930f, 652f), ledOn = Color(0xFF6CFF7E), tv = LaunchKind.PS1.screen,
            roundLid = true, lidRx = 140f, lidRy = 42f,
            details = {
                // Botones redondos a los lados de la tapa (encendido a la izquierda, abrir a la derecha) y reset
                listOf(Offset(945f, 588f), Offset(1335f, 588f)).forEach { drawCircle(Color(0xFFB0B0B5), 24f, it); drawCircle(Color(0xFFC9CAD0), 19f, it) }
                drawCircle(Color(0xFFB0B0B5), 11f, Offset(995f, 610f))
                // Frente: dos puertos de mando y dos ranuras de tarjeta, rejilla a la derecha
                listOf(1060f, 1170f).forEach { x -> drawRoundRect(Color(0xFF8E8E94), Offset(x, 650f), Size(70f, 30f), CornerRadius(6f)) }
                listOf(1060f, 1170f).forEach { x -> drawRect(Color(0xFF55555B), Offset(x + 6f, 640f), Size(58f, 5f)) }
                for (i in 0 until 8) drawRect(Color(0xFFA6A6AB), Offset(1300f + i * 11f, 646f), Size(5f, 44f))
            },
        )

        /** Dreamcast: blanca y casi cuadrada vista desde arriba; la tapa redonda grande ocupa casi toda la parte de
         *  arriba, con los botones de abrir y encender a la derecha y cuatro puertos de mando delante. */
        val DREAMCAST = TopLoader(
            front = SceneRect(970f, 628f, 340f, 72f), backY = 492f, inset = 22f,
            lidL = 0f, lidR = 0f, lidOpenY = 300f,
            well = Offset(1124f, 560f), wellW = 176f, wellH = 58f, discD = 176f,
            body = Color(0xFFE4E5E8), top = Color(0xFFF1F2F4), under = Color(0xFFC6C8CD), edge = Color(0xFFBFC1C6),
            led = Offset(1286f, 650f), ledOn = Color(0xFFFFA24A), tv = LaunchKind.PS1.screen,
            roundLid = true, lidRx = 112f, lidRy = 50f,
            details = {
                // Frente redondeado: sombra en las esquinas
                drawRect(Color(0xFFD3D5D9), Offset(970f, 628f), Size(10f, 72f)); drawRect(Color(0xFFD3D5D9), Offset(1300f, 628f), Size(10f, 72f))
                listOf(990f, 1050f, 1110f, 1170f).forEach { x -> drawRoundRect(Color(0xFF9EA1A8), Offset(x, 648f), Size(46f, 34f), CornerRadius(5f)) }
                drawCircle(Color(0xFFD5D7DB), 13f, Offset(1268f, 596f)); drawCircle(Color(0xFFD5D7DB), 10f, Offset(1262f, 534f))
                for (i in 0 until 4) drawRect(Color(0xFFD0D2D6), Offset(1240f + i * 9f, 652f), Size(4f, 34f))
            },
        )

        /** Saturn (primera): gris muy oscuro, tapa redonda grande en el centro de arriba. */
        val SATURN = TopLoader(
            front = SceneRect(880f, 636f, 520f, 64f), backY = 536f, inset = 26f,
            lidL = 0f, lidR = 0f, lidOpenY = 310f,
            well = Offset(1140f, 586f), wellW = 210f, wellH = 56f, discD = 200f,
            body = Color(0xFF26272B), top = Color(0xFF323338), under = Color(0xFF1A1B1E), edge = Color(0xFF44454B),
            led = Offset(1360f, 656f), ledOn = Color(0xFF6CFF7E), tv = LaunchKind.PS1.screen,
            roundLid = true, lidRx = 134f, lidRy = 40f,
            details = {
                drawRoundRect(Color(0xFF16171A), Offset(1060f, 542f), Size(160f, 10f), CornerRadius(3f)) // ranura de cartucho atrás
                listOf(1300f, 1335f).forEach { x -> drawCircle(Color(0xFF3C3D43), 12f, Offset(x, 600f)) }
                listOf(920f, 980f).forEach { x -> drawRoundRect(Color(0xFF0E0F11), Offset(x, 656f), Size(48f, 24f), CornerRadius(4f)) }
                for (i in 0 until 7) drawRect(Color(0xFF1C1D20), Offset(1240f + i * 9f, 650f), Size(4f, 40f))
            },
        )

        /** PC-FX: torre blanca de pie, con la tapa redonda en la parte de arriba. */
        val PCFX = TopLoader(
            front = SceneRect(1150f, 460f, 180f, 240f), backY = 410f, inset = 14f,
            lidL = 0f, lidR = 0f, lidOpenY = 260f,
            well = Offset(1240f, 436f), wellW = 140f, wellH = 34f, discD = 140f,
            body = Color(0xFFE6E7EA), top = Color(0xFFF1F2F4), under = Color(0xFFC6C8CD), edge = Color(0xFFBFC1C6),
            led = Offset(1180f, 600f), ledOn = Color(0xFF6CFF7E), tv = LaunchKind.PS1.screen,
            roundLid = true, lidRx = 80f, lidRy = 22f,
            details = {
                for (i in 0 until 10) drawRect(Color(0xFFD0D2D6), Offset(1260f, 490f + i * 18f), Size(56f, 6f))
                drawRoundRect(Color(0xFFC0C2C7), Offset(1166f, 620f), Size(70f, 40f), CornerRadius(4f))
            },
        )

        fun of(consoleId: String): TopLoader = when (consoleId) {
            "dreamcast" -> DREAMCAST; "saturn" -> SATURN; "gc" -> GAMECUBE; "ps2" -> PS2_SLIM; "pcfx" -> PCFX; else -> PS1
        }

        /** GameCube: cubo morado casi cuadrado, con asa atrás y tapa en toda la parte de arriba (minidisco). */
        val GAMECUBE = TopLoader(
            front = SceneRect(1040f, 530f, 220f, 170f), backY = 450f, inset = 16f,
            lidL = 1040f, lidR = 1260f, lidOpenY = 330f,
            well = Offset(1150f, 490f), wellW = 150f, wellH = 48f, discD = 130f,
            body = Color(0xFF4A3C8C), top = Color(0xFF5A4AA4), under = Color(0xFF2E2559), edge = Color(0xFF3A2F70),
            led = Offset(1238f, 560f), ledOn = Color(0xFF7CFF8A), tv = LaunchKind.GAMECUBE.screen,
            details = {
                drawRoundRect(Color(0xFF2B2252), Offset(1066f, 440f), Size(168f, 12f), CornerRadius(6f)) // asa
                listOf(1056f, 1100f, 1144f, 1188f).forEach { x -> drawRoundRect(Color(0xFF2A2250), Offset(x, 640f), Size(30f, 28f), CornerRadius(4f)) }
                drawCircle(Color(0xFF3A2F70), 9f, Offset(1072f, 562f))
            },
        )

        /** PS2 Slim: negra y plana, con la tapa redonda del disco en la parte izquierda de arriba. */
        val PS2_SLIM = TopLoader(
            front = SceneRect(870f, 646f, 540f, 54f), backY = 556f, inset = 26f,
            lidL = 892f, lidR = 1200f, lidOpenY = 400f,
            well = Offset(1046f, 601f), wellW = 236f, wellH = 64f, discD = 220f,
            body = Color(0xFF1B1C21), top = Color(0xFF26282F), under = Color(0xFF15161A), edge = Color(0xFF34363E),
            led = Offset(1386f, 672f), ledOn = Color(0xFF59E07A), tv = LaunchKind.PS2.screen,
            details = {
                // Estrías en la parte derecha de arriba, puertos de mando y botones del frente.
                for (i in 0 until 7) drawLine(Color(0xFF30333B), Offset(1236f + i * 4f, 562f + i * 12f), Offset(1390f + i * 3f, 562f + i * 12f), 3f)
                listOf(896f, 948f).forEach { x -> drawRoundRect(Color(0xFF0E0F12), Offset(x, 662f), Size(40f, 22f), CornerRadius(4f)) }
                listOf(1330f, 1356f).forEach { x -> drawCircle(Color(0xFF2E3038), 7f, Offset(x, 672f)) }
            },
        )
    }
}

@Composable
internal fun TopLoaderScene(game: Game, p: Float, u: Dp, m: TopLoader, tvOnAt: Float) {
    Television(u, on = seg(p, tvOnAt, tvOnAt + 0.07f), game = game, s = m.tv)
    val caseH = 440f
    val boxHu = boxSize(game, u * caseH).second / u
    val caseX = 150f; val caseY = TABLE_Y - boxHu - 6f
    val frontW = caseH * frontAspect(game)
    val open = seg(p, 0.12f, 0.27f)
    val caseAlpha = ease(seg(p, 0f, 0.1f)) * (1f - seg(p, 0.48f, 0.58f))
    // Tapa: se levanta (0.20–0.29) y se cierra (0.48–0.55).
    val lid = ease(seg(p, 0.20f, 0.29f)) * (1f - ease(seg(p, 0.48f, 0.55f)))
    // Disco: en la caja → cae tumbado en el hueco de arriba.
    val d0 = minOf(frontW * 0.78f, caseH * 0.62f) * (m.discD / 220f).coerceAtMost(1f)
    val flatEnd = m.wellH * 0.9f / m.discD
    val startCx = caseX + frontW / 2; val startCy = caseY + (boxHu - caseH) + caseH / 2
    val fly = ease(seg(p, 0.29f, 0.47f))
    val d = lerp(d0, m.discD, fly); val flat = lerp(1f, flatEnd, fly)
    val dcx = lerp(startCx, m.well.x, fly)
    val dcy = lerp(startCy, m.well.y, fly) - 170f * sin(PI.toFloat() * fly)
    val spin = if (p > 0.29f) (p - 0.29f) * 1400f else 0f
    val disc = @Composable { x: Float, y: Float ->
        Box(Modifier.offset(u * (x - d / 2), u * (y - d / 2)).size(u * d, u * d).graphicsLayer { scaleY = flat }) {
            Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin }) { Disc(game) }
        }
    }
    ClosedCase(game, u, caseX, caseY, caseH, caseAlpha, open) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(4)).background(Color(0xFF15171C))) {
            Box(Modifier.align(Alignment.Center).size(u * (d0 * 0.3f)).clip(CircleShape).background(Color(0xFF2A2E36)))
        }
    }
    if (p < 0.29f && open > 0.4f) disc(startCx, startCy)

    val f = m.front
    val area = SceneRect(f.x - 20f, minOf(m.lidOpenY, m.backY) - 30f, f.w + 40f, f.y + f.h - minOf(m.lidOpenY, m.backY) + 30f)
    @Composable
    fun layer(draw: DrawScope.() -> Unit) = Canvas(Modifier.offset(u * area.x, u * area.y).size(u * area.w, u * area.h)) {
        val k = size.width / area.w
        withTransform({ scale(k, k, Offset.Zero); translate(-area.x, -area.y) }) { draw() }
    }
    // Cuerpo: cara de arriba, hueco del disco, frente y detalles.
    layer {
        val topFace = Path().apply { moveTo(m.backX(f.x), m.backY); lineTo(m.backX(f.x + f.w), m.backY); lineTo(f.x + f.w, f.y); lineTo(f.x, f.y); close() }
        drawPath(topFace, m.top)
        drawOval(Color(0xFF0D0E12), Offset(m.well.x - m.wellW / 2, m.well.y - m.wellH / 2), Size(m.wellW, m.wellH))
        drawCircle(lerpColor(m.top, Color.White, 0.15f), m.wellH * 0.12f, m.well)
        drawRect(m.body, Offset(f.x, f.y), Size(f.w, f.h))
        drawRect(m.edge, Offset(f.x, f.y), Size(f.w, 3f))
        m.details(this)
        drawCircle(if (p > 0.57f) m.ledOn else lerpColor(m.ledOn, Color.Black, 0.7f), 6f, m.led)
    }
    if (p >= 0.47f) disc(m.well.x, m.well.y)
    // Tapa: gira sobre la bisagra de atrás (al abrirse se ve su cara de abajo).
    if (m.roundLid) layer {
        // Tapa redonda: vista desde arriba es una elipse; al levantarse sobre su bisagra de atrás se ve cada vez
        // más alta (de frente, casi un círculo) y por su cara de abajo.
        val hinge = m.well.y - m.lidRy
        val topY = lerp(hinge, hinge - m.lidRx * 1.05f, lid)
        val bottomY = lerp(m.well.y + m.lidRy, hinge, lid)
        val y0 = minOf(topY, bottomY); val y1 = maxOf(topY, bottomY).coerceAtLeast(y0 + 4f)
        if (lid > 0.5f) drawOval(Brush.verticalGradient(listOf(lerpColor(m.under, Color.Black, 0.25f), m.under), y0, y1),
            Offset(m.well.x - m.lidRx, y0), Size(m.lidRx * 2, y1 - y0))
        else drawOval(m.top, Offset(m.well.x - m.lidRx, y0), Size(m.lidRx * 2, y1 - y0))
        drawOval(m.edge, Offset(m.well.x - m.lidRx, y0), Size(m.lidRx * 2, y1 - y0), style = Stroke(3f))
        if (lid < 0.5f) for (i in 1..3) {
            val f = 1f - i * 0.22f
            drawOval(lerpColor(m.top, Color.Black, 0.06f), Offset(m.well.x - m.lidRx * f, (y0 + y1) / 2 - (y1 - y0) / 2 * f),
                Size(m.lidRx * 2 * f, (y1 - y0) * f), style = Stroke(2f))
        }
    } else layer {
        val fy = lerp(f.y, m.lidOpenY, lid)
        val lx = lerp(m.lidL, m.backX(m.lidL), lid); val rx = lerp(m.lidR, m.backX(m.lidR), lid)
        val path = Path().apply { moveTo(m.backX(m.lidL), m.backY); lineTo(m.backX(m.lidR), m.backY); lineTo(rx, fy); lineTo(lx, fy); close() }
        drawPath(path, if (lid > 0.5f) m.under else m.top)
        drawPath(path, m.edge, style = Stroke(3f))
        if (lid < 0.5f) drawCircle(lerpColor(m.top, Color.White, 0.1f), 24f * (1f - lid), Offset((lx + rx) / 2, (m.backY + fy) / 2))
    }
    if (p in 0.29f..0.47f) disc(dcx, dcy)
}

// ------------------------------------------------------------------ Wii

@Composable
internal fun WiiScene(game: Game, p: Float, u: Dp) {
    val tv = LaunchKind.WII.screen
    Television(u, on = seg(p, 0.61f, 0.68f), game = game, s = tv)
    val caseH = 440f
    val boxHu = boxSize(game, u * caseH).second / u
    val caseX = 140f; val caseY = TABLE_Y - boxHu - 6f
    val frontW = caseH * frontAspect(game)
    val open = seg(p, 0.12f, 0.27f)
    val caseAlpha = ease(seg(p, 0f, 0.1f)) * (1f - seg(p, 0.46f, 0.56f))
    // Consola de pie con la ranura vertical en el frente: Wii (blanca y estrecha) o PS5 (paneles blancos y centro negro).
    val ps5 = game.consoleId == "ps5"
    val wx = if (ps5) 1376f else 1410f; val wy = if (ps5) 296f else 340f
    val ww = if (ps5) 130f else 90f; val wh = if (ps5) 396f else 350f
    val slotX = if (ps5) wx + ww - 26f else wx + ww / 2
    val slotTop = if (ps5) wy + 150f else wy + 50f; val slotH = if (ps5) 200f else 240f
    val glow = seg(p, 0.50f, 0.58f)
    // Disco: en la caja → se pone de canto delante de la ranura → entra.
    val d0 = minOf(frontW * 0.78f, caseH * 0.62f)
    val startCx = caseX + frontW / 2; val startCy = caseY + (boxHu - caseH) + caseH / 2
    val fly = ease(seg(p, 0.29f, 0.45f))
    val into = ease(seg(p, 0.45f, 0.52f))
    val d = lerp(d0, slotH, fly)
    val edge = lerp(1f, 0.07f, fly)
    val dcx = lerp(startCx, slotX, fly)
    val dcy = lerp(startCy, slotTop + slotH / 2, fly) - 150f * sin(PI.toFloat() * fly)
    val spin = if (p > 0.29f) (p - 0.29f) * 900f else 0f
    ClosedCase(game, u, caseX, caseY, caseH, caseAlpha, open) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(4)).background(if (ps5) Color(0xFF15171C) else Color(0xFFE9EBEF))) {
            Box(Modifier.align(Alignment.Center).size(u * (d0 * 0.3f)).clip(CircleShape).background(Color(0xFFC9CDD4)))
        }
    }
    Canvas(Modifier.offset(u * (wx - 20), u * wy).size(u * (ww + 40), u * (wh + 12))) {
        val k = size.width / (ww + 40)
        withTransform({ scale(k, k, Offset.Zero); translate(-(wx - 20), -wy) }) {
            drawRoundRect(Color(0xFFDADDE2), Offset(wx - 20f, wy + wh - 6f), Size(ww + 40f, 14f), CornerRadius(6f)) // peana
            if (ps5) {
                // Centro negro y dos paneles blancos que se abren hacia arriba (más anchos arriba que abajo)
                drawRoundRect(Color(0xFF111215), Offset(wx + 34f, wy + 20f), Size(ww - 68f, wh - 24f), CornerRadius(8f))
                drawPath(Path().apply { moveTo(wx, wy); cubicTo(wx + 20f, wy + 10f, wx + 36f, wy + 40f, wx + 38f, wy + 90f); lineTo(wx + 38f, wy + wh - 4f); lineTo(wx + 14f, wy + wh - 4f); cubicTo(wx + 8f, wy + 200f, wx - 6f, wy + 60f, wx, wy); close() }, Color(0xFFF4F5F7))
                drawPath(Path().apply { moveTo(wx + ww, wy); cubicTo(wx + ww - 20f, wy + 10f, wx + ww - 36f, wy + 40f, wx + ww - 38f, wy + 90f); lineTo(wx + ww - 38f, wy + wh - 4f); lineTo(wx + ww - 14f, wy + wh - 4f); cubicTo(wx + ww - 8f, wy + 200f, wx + ww + 6f, wy + 60f, wx + ww, wy); close() }, Color(0xFFF4F5F7))
                drawLine(Color(0xFF3FA0FF).copy(alpha = 0.3f + 0.7f * glow), Offset(wx + 36f, wy + 90f), Offset(wx + 36f, wy + wh - 10f), 3f)
                drawLine(Color(0xFF3FA0FF).copy(alpha = 0.3f + 0.7f * glow), Offset(wx + ww - 36f, wy + 90f), Offset(wx + ww - 36f, wy + wh - 10f), 3f)
            } else {
                drawRoundRect(Color(0xFFF4F5F7), Offset(wx, wy), Size(ww, wh), CornerRadius(10f))
                drawRoundRect(Color(0xFFCBD0D7), Offset(wx, wy), Size(ww, wh), CornerRadius(10f), style = Stroke(3f))
            }
            // Ranura (se ilumina en azul al entrar el disco)
            if (glow > 0f) drawRoundRect(Color(0xFF4FC3F7).copy(alpha = 0.45f * glow), Offset(slotX - 14f, slotTop - 10f), Size(28f, slotH + 20f), CornerRadius(14f))
            drawRoundRect(lerpColor(Color(0xFF2A2E36), Color(0xFF7FD8FF), glow), Offset(slotX - 4f, slotTop), Size(8f, slotH), CornerRadius(4f))
            if (!ps5) drawCircle(Color(0xFFBFC4CC), 9f, Offset(slotX, wy + wh - 40f))
        }
    }
    if (p < 0.29f && open > 0.4f || p in 0.29f..0.52f) {
        val x = if (p < 0.29f) startCx else dcx
        val y = if (p < 0.29f) startCy else dcy
        Box(Modifier.offset(u * (x - d / 2), u * (y - d / 2)).size(u * d, u * d).alpha(1f - into).graphicsLayer {
            scaleX = edge * lerp(1f, 0.6f, into); scaleY = lerp(1f, 0.92f, into)
        }) {
            Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin }) { Disc(game) }
        }
    }
}
