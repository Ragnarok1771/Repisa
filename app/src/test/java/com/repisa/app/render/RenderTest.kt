package com.repisa.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.repisa.app.model.Consoles
import com.repisa.app.model.Game
import com.repisa.app.model.Source
import com.repisa.app.ui.ConsoleIcon
import com.repisa.app.ui.LaunchAnimation
import com.repisa.app.ui.LaunchDebug
import com.repisa.app.ui.LaunchKind
import com.repisa.app.ui.hasConsoleArt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Dibuja pantallas en el ordenador (sin emulador) para revisar los dibujos de las consolas.
 * Solo hace algo si se pasa -Drender.out=carpeta (y opcionalmente -Drender.ids=switch,gba y -Drender.at=0.5).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1200dp-h900dp-mdpi")
class RenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val out get() = System.getProperty("render.out")

    private fun save(name: String) {
        val dir = out ?: return
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { view.draw(Canvas(bmp)) }
        File(dir).mkdirs()
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val ids: List<String>
        get() = System.getProperty("render.ids")?.split(",")?.filter { it.isNotBlank() }
            ?: Consoles.all.map { it.id }.filter { hasConsoleArt(it) }

    /** Hoja con el icono de cada consola, sobre madera, a dos tamaños (el de la placa y el de «Mis consolas»). */
    @OptIn(ExperimentalLayoutApi::class)
    @Test fun consoleIcons() {
        if (out == null) return
        compose.setContent {
            FlowRow(Modifier.fillMaxSize().background(Color(0xFF2A1A0F)).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ids.forEach { id ->
                    Column(Modifier.width(140.dp).background(Color(0xFF3B2516)).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.height(90.dp), contentAlignment = Alignment.BottomCenter) { ConsoleIcon(id, Modifier.height(86.dp)) }
                        Box(Modifier.height(56.dp).background(Color(0xFF4A2C16)), contentAlignment = Alignment.Center) {
                            com.repisa.app.ui.ConsoleIconBox(id, 92.dp, 52.dp)
                        }
                        Text(id, color = Color(0xFFF3E6D3), fontSize = 11.sp)
                    }
                }
            }
        }
        save("iconos")
    }

    /** Cada consola de [ids] en grande (para compararla con su foto), de dos en dos por imagen. */
    @Test fun bigIcons() {
        if (out == null || System.getProperty("render.big") == null) return
        val pairs = ids.chunked(2)
        var current by androidx.compose.runtime.mutableStateOf(pairs.first())
        compose.setContent {
            androidx.compose.foundation.layout.Row(Modifier.fillMaxSize().background(Color(0xFF2A1A0F)).padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                current.forEach { id ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.runtime.key(id) { ConsoleIcon(id, Modifier.width(540.dp).height(620.dp)) }
                        Text(id, color = Color(0xFFF3E6D3), fontSize = 18.sp)
                    }
                }
            }
        }
        pairs.forEach { pair ->
            compose.runOnUiThread { current = pair }
            save("grande_${pair.joinToString("_")}")
        }
    }

    /** Un fotograma de la animación de cada consola de [ids] en el instante -Drender.at (0..1). */
    @Test fun frames() {
        if (out == null || System.getProperty("render.frames") == null) return
        val at = System.getProperty("render.at")?.toFloatOrNull() ?: 0.5f
        LaunchDebug.freezeAt = at
        val games = ids.map { Game("emu:$it:x", Source.EMULATOR, "Prueba", it) }.filter { LaunchKind.of(it) != null }
        var current by androidx.compose.runtime.mutableStateOf(games.first())
        compose.setContent { androidx.compose.runtime.key(current.id) { LaunchAnimation(current, LaunchKind.of(current)!!, onFinish = {}) } }
        games.forEach { g ->
            compose.runOnUiThread { current = g }
            save("f_${g.consoleId}_$at")
        }
    }

    /** Repisas con muchas cajas para ver la barra de desplazamiento rápido (en reposo y arrastrando). */
    @Test fun scrollbar() {
        if (out == null || System.getProperty("render.scroll") == null) return
        com.repisa.app.i18n.L10n.init(compose.activity.applicationContext)
        val names = listOf("Acción", "Aventura", "FPS", "Gacha", "Metroidvania", "Rol (RPG)", "Soulslike", "Terror")
        val shelves = names.map { n ->
            com.repisa.app.ui.ShelfGroup(com.repisa.app.i18n.L10n.genre(n), "", (1..14).map { i -> Game("x:$n$i", Source.STEAM, "$n $i", "pc") })
        }
        compose.setContent {
            com.repisa.app.ui.Shelves(shelves, emptySet(), {}, 150.dp, null, {}, {}, {}, {}, { _, _ -> })
        }
        save("barra_reposo")
        // Arrastrar el tirador hasta la mitad.
        val root = compose.onRoot()
        root.performTouchInput { down(androidx.compose.ui.geometry.Offset(width - 8f, 40f)); moveTo(androidx.compose.ui.geometry.Offset(width - 8f, height * 0.55f)) }
        save("barra_arrastrando")
        root.performTouchInput { up() }
    }

    /** Desde la placa de una repisa, abajo con el mando lleva al primer juego (no al que quede más centrado). */
    @OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
    @Test fun plaqueDownGoesToFirstGame() {
        com.repisa.app.i18n.L10n.init(compose.activity.applicationContext)
        // El primero en caja pequeña (3DS) y el resto grandes: la placa queda más centrada sobre el segundo.
        val games = (1..6).map { i -> Game("x:s$i", Source.EMULATOR, "Juego $i", if (i == 1) "3ds" else "ps2") }
        val shelves = listOf(
            com.repisa.app.ui.ShelfGroup("Seguir jugando", "Jugados hace poco, con un subtítulo largo", games),
            com.repisa.app.ui.ShelfGroup("Otra", "", (1..4).map { i -> Game("x:o$i", Source.STEAM, "Otro $i", "pc") }),
        )
        var focused by androidx.compose.runtime.mutableStateOf<String?>(null)
        var inputMode: androidx.compose.ui.input.InputModeManager? = null
        compose.setContent {
            inputMode = androidx.compose.ui.platform.LocalInputModeManager.current
            com.repisa.app.ui.Shelves(shelves, emptySet(), {}, 150.dp, null, { focused = it.id }, {}, {}, {}, { _, _ -> })
        }
        // Como en la consola con el mando: modo teclado (en modo táctil la placa no recibe el foco).
        compose.runOnUiThread { inputMode?.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        compose.waitForIdle()
        compose.onNode(androidx.compose.ui.test.hasText("Seguir jugando", substring = true) and androidx.compose.ui.test.hasClickAction())
            .requestFocus()
        compose.waitForIdle()
        compose.onNode(androidx.compose.ui.test.isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionDown) }
        compose.waitForIdle()
        org.junit.Assert.assertEquals("x:s1", focused)
    }
}
