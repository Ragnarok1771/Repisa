package com.repisa.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.repisa.app.R

/** Cómo se dibuja la pared del fondo de la vitrina. */
enum class PanelStyle { PLANKS, BRUSHED, GLASS, PEGBOARD }

/**
 * Tema de vitrina: colores de la pared, las baldas, la luz LED, los laterales y las placas.
 * Se elige en Ajustes → Temas y se aplica a todas las pantallas (también a la de abajo).
 */
class ShelfTheme(
    /** Identificador guardado en Ajustes («WALNUT», «METAL»... o «CUSTOM»). */
    val name: String,
    val nameRes: Int,
    val emoji: String,
    val panel: PanelStyle,
    /** Degradado de la pared, de arriba abajo. */
    val wall: List<Color>,
    /** Veta (o rayado) de la pared: oscura y clara. */
    val grainDark: Color,
    val grainLight: Color,
    /** Junta entre tablones / paneles. */
    val joint: Color,
    /** Luz que baja desde arriba. */
    val topLight: Color,
    /** Balda: superficie (de atrás a delante) y canto frontal (de arriba abajo). */
    val plankTop: List<Color>,
    val plankFront: List<Color>,
    val plankGrain: Color,
    val plankEdge: Color,
    /** Tira LED, su soporte y el resplandor sobre la pared. */
    val ledBase: Color,
    val led: Color,
    val ledGlow: Color,
    /** Laterales del mueble (borde, centro). */
    val sideOuter: Color,
    val sideInner: Color,
    /** Placa con el nombre de cada repisa. */
    val plaque: List<Color>,
    val plaqueText: Color,
    val plaqueBorder: Color,
) {

    val label: String get() = com.repisa.app.i18n.L10n.s(nameRes)

    companion object {
        val WALNUT = ShelfTheme(
            "WALNUT", R.string.theme_walnut, "🪵", PanelStyle.PLANKS,
            wall = listOf(Color(0xFF3B2516), Color(0xFF2A1A0F), Color(0xFF1B110A)),
            grainDark = Color(0x14000000), grainLight = Color(0x0AFFE0B0), joint = Color(0x66000000), topLight = Color(0x33FFC67A),
            plankTop = listOf(Color(0xFF4A2C16), Color(0xFF8C5A30)),
            plankFront = listOf(Color(0xFFC08A55), Color(0xFF9A673A), Color(0xFF6E4422)),
            plankGrain = Color(0x1F2A1408), plankEdge = Color(0x66FFE2B8),
            ledBase = Color(0xFF3A2414), led = Color(0xCCFFD9A0), ledGlow = Color(0x55FFC680),
            sideOuter = Color(0xFF5A3720), sideInner = Color(0xFF8C5A32),
            plaque = listOf(Color(0xFFE9C46A), Color(0xFFB08436)), plaqueText = Color(0xFF2B1A08), plaqueBorder = Color(0xFF6E4F1E),
        )
        val OAK = ShelfTheme(
            "OAK", R.string.theme_oak, "🌾", PanelStyle.PLANKS,
            wall = listOf(Color(0xFF7A5530), Color(0xFF5E4024), Color(0xFF402A16)),
            grainDark = Color(0x18000000), grainLight = Color(0x10FFF0D0), joint = Color(0x55301C0A), topLight = Color(0x40FFE2A8),
            plankTop = listOf(Color(0xFFA77B4A), Color(0xFFD9B07A)),
            plankFront = listOf(Color(0xFFF2D29C), Color(0xFFD6AC70), Color(0xFFA67A45)),
            plankGrain = Color(0x22603A15), plankEdge = Color(0x88FFF3D6),
            ledBase = Color(0xFF7A5530), led = Color(0xDDFFF1D0), ledGlow = Color(0x55FFE6B0),
            sideOuter = Color(0xFF9C7344), sideInner = Color(0xFFD2A870),
            plaque = listOf(Color(0xFF5B3A1E), Color(0xFF3E2512)), plaqueText = Color(0xFFFFE8B8), plaqueBorder = Color(0xFF2A170A),
        )
        val EBONY = ShelfTheme(
            "EBONY", R.string.theme_ebony, "🌑", PanelStyle.PLANKS,
            wall = listOf(Color(0xFF221712), Color(0xFF17100C), Color(0xFF0B0706)),
            grainDark = Color(0x22000000), grainLight = Color(0x08FFD6B0), joint = Color(0x88000000), topLight = Color(0x26FFB070),
            plankTop = listOf(Color(0xFF1E140F), Color(0xFF3E2C22)),
            plankFront = listOf(Color(0xFF5A4031), Color(0xFF3E2B21), Color(0xFF231812)),
            plankGrain = Color(0x26000000), plankEdge = Color(0x44FFD2A8),
            ledBase = Color(0xFF140D0A), led = Color(0xCCFFC488), ledGlow = Color(0x4DFFA860),
            sideOuter = Color(0xFF1C130E), sideInner = Color(0xFF3A2A20),
            plaque = listOf(Color(0xFFD8B26A), Color(0xFF8E6A2E)), plaqueText = Color(0xFF1A1007), plaqueBorder = Color(0xFF4A3412),
        )
        val METAL = ShelfTheme(
            "METAL", R.string.theme_metal, "🔩", PanelStyle.BRUSHED,
            wall = listOf(Color(0xFF454B52), Color(0xFF2F343A), Color(0xFF1B1E22)),
            grainDark = Color(0x16000000), grainLight = Color(0x0EFFFFFF), joint = Color(0x77000000), topLight = Color(0x2AD8ECFF),
            plankTop = listOf(Color(0xFF6E767E), Color(0xFFB9C0C7)),
            plankFront = listOf(Color(0xFFE6EAEE), Color(0xFFAEB5BC), Color(0xFF6A7279)),
            plankGrain = Color(0x14000000), plankEdge = Color(0xAAFFFFFF),
            ledBase = Color(0xFF2A2F34), led = Color(0xDDE4F2FF), ledGlow = Color(0x50B8DCFF),
            sideOuter = Color(0xFF50575E), sideInner = Color(0xFFA2AAB2),
            plaque = listOf(Color(0xFFE8ECEF), Color(0xFFA9B1B8)), plaqueText = Color(0xFF1E2328), plaqueBorder = Color(0xFF555D64),
        )
        val GLASS = ShelfTheme(
            "GLASS", R.string.theme_glass, "🧊", PanelStyle.GLASS,
            wall = listOf(Color(0xFF16222E), Color(0xFF0E1720), Color(0xFF070B10)),
            grainDark = Color(0x10000000), grainLight = Color(0x0CBFE6FF), joint = Color(0x33A8D8FF), topLight = Color(0x2A9FD8FF),
            plankTop = listOf(Color(0x5582B8D8), Color(0x88C8ECFF)),
            plankFront = listOf(Color(0xAAE0F6FF), Color(0x6699CCEE), Color(0x44557799)),
            plankGrain = Color(0x00000000), plankEdge = Color(0xDDFFFFFF),
            ledBase = Color(0x6630465A), led = Color(0xDD9EEBFF), ledGlow = Color(0x4D5FD0FF),
            sideOuter = Color(0xFF1E2A35), sideInner = Color(0xFF4A6070),
            plaque = listOf(Color(0xCCDFF4FF), Color(0xAA8EC4E0)), plaqueText = Color(0xFF0D2233), plaqueBorder = Color(0xFFBFE9FF),
        )
        val RETRO_SHOP = ShelfTheme(
            "RETRO_SHOP", R.string.theme_retro, "🕹", PanelStyle.PEGBOARD,
            wall = listOf(Color(0xFF3A1A55), Color(0xFF26103D), Color(0xFF140824)),
            grainDark = Color(0x55000000), grainLight = Color(0x14FF9CF0), joint = Color(0x44000000), topLight = Color(0x30FF5AD8),
            plankTop = listOf(Color(0xFF8E1A2A), Color(0xFFD8363F)),
            plankFront = listOf(Color(0xFFFF6A5A), Color(0xFFD8302F), Color(0xFF8A1620)),
            plankGrain = Color(0x00000000), plankEdge = Color(0xAAFFE07A),
            ledBase = Color(0xFF2A0F2E), led = Color(0xEEFF66E0), ledGlow = Color(0x5AFF3FD0),
            sideOuter = Color(0xFF1F2A80), sideInner = Color(0xFF3F55E0),
            plaque = listOf(Color(0xFFFFE45A), Color(0xFFFFA62B)), plaqueText = Color(0xFF2A0F2E), plaqueBorder = Color(0xFFFF3FD0),
        )

        /** Los temas de fábrica, en el orden en que se muestran. */
        val entries: List<ShelfTheme> = listOf(WALNUT, OAK, EBONY, METAL, GLASS, RETRO_SHOP)

        /** Tema en uso (estado de Compose: al cambiarlo se redibuja todo). */
        var current by mutableStateOf(WALNUT)

        /** El tema personalizado guardado (textura y colores elegidos por el usuario). */
        var custom by mutableStateOf(CustomTheme().build())

        fun of(name: String?) = if (name == "CUSTOM") custom else entries.firstOrNull { it.name == name } ?: WALNUT
    }
}

/**
 * Tema personalizado: una de las texturas de los temas de fábrica y tres colores (pared, baldas y luz);
 * el resto (degradados, veta, laterales, placas) se calcula a partir de ellos.
 */
data class CustomTheme(
    val panel: PanelStyle = PanelStyle.PLANKS,
    val wall: Long = 0xFF2E3A2A,
    val shelf: Long = 0xFF8C5A30,
    val light: Long = 0xFFFFD9A0,
) {
    fun encode() = "${panel.name}|$wall|$shelf|$light"

    fun build(): ShelfTheme {
        val w = Color(wall); val sh = Color(shelf); val l = Color(light)
        val glass = panel == PanelStyle.GLASS
        val plaqueDark = l.luminance() > 0.45f
        return ShelfTheme(
            "CUSTOM", R.string.theme_custom, "🖌", panel,
            wall = listOf(w.mix(Color.White, 0.10f), w, w.mix(Color.Black, 0.55f)),
            grainDark = Color(0x18000000), grainLight = l.copy(alpha = 0.06f), joint = Color(0x66000000), topLight = l.copy(alpha = 0.22f),
            plankTop = listOf(sh.mix(Color.Black, 0.45f), sh).map { if (glass) it.copy(alpha = 0.55f) else it },
            plankFront = listOf(sh.mix(Color.White, 0.30f), sh, sh.mix(Color.Black, 0.35f)).map { if (glass) it.copy(alpha = 0.6f) else it },
            plankGrain = if (panel == PanelStyle.PLANKS) Color(0x1F000000) else Color(0x00000000), plankEdge = Color(0x88FFFFFF),
            ledBase = sh.mix(Color.Black, 0.6f), led = l.copy(alpha = 0.85f), ledGlow = l.copy(alpha = 0.33f),
            sideOuter = sh.mix(Color.Black, 0.35f), sideInner = sh,
            plaque = listOf(l.mix(Color.White, 0.2f), l.mix(Color.Black, 0.2f)),
            plaqueText = if (plaqueDark) Color(0xFF1E140C) else Color.White,
            plaqueBorder = l.mix(Color.Black, 0.5f),
        )
    }

    companion object {
        fun decode(text: String?): CustomTheme = runCatching {
            val p = text!!.split("|")
            CustomTheme(PanelStyle.valueOf(p[0]), p[1].toLong(), p[2].toLong(), p[3].toLong())
        }.getOrDefault(CustomTheme())
    }
}

private fun Color.mix(other: Color, t: Float) = androidx.compose.ui.graphics.lerp(this, other, t)
private fun Color.luminance() = 0.299f * red + 0.587f * green + 0.114f * blue
