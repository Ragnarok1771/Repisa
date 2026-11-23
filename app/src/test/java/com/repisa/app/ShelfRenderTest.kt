package com.repisa.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.repisa.app.data.LibraryStore
import com.repisa.app.model.Game
import com.repisa.app.model.Source
import com.repisa.app.sources.EmulatorSource
import com.repisa.app.ui.DetailScreen
import com.repisa.app.ui.GroupMode
import com.repisa.app.ui.LibraryViewModel
import com.repisa.app.ui.RepisaTheme
import com.repisa.app.ui.ShelfScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renderiza la repisa con juegos de ejemplo y guarda capturas en build/screens/. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w800dp-h450dp-xhdpi")
class ShelfRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val sample = listOf(
        Game("steam:1", Source.STEAM, "Hollow Knight", "pc", listOf("Acción", "Metroidvania"), "Team Cherry", releaseYear = 2017, playtimeMinutes = 2950),
        Game("steam:2", Source.STEAM, "Hades", "pc", listOf("Acción", "Roguelike"), "Supergiant Games", releaseYear = 2020, playtimeMinutes = 4100),
        Game("epic:3", Source.EPIC, "Alan Wake 2", "pc", listOf("Terror"), "Remedy", releaseYear = 2023, playtimeMinutes = 600),
        Game("emu:snes:a", Source.EMULATOR, "Super Metroid", "snes", listOf("Plataformas"), "Nintendo", releaseYear = 1994, playtimeMinutes = 420),
        Game("emu:ps2:b", Source.EMULATOR, "Shadow of the Colossus", "ps2", listOf("Aventura"), "Team Ico", releaseYear = 2005, trackedMinutes = 300),
        Game("emu:switch:c", Source.EMULATOR, "Metroid Dread", "switch", listOf("Acción"), "MercurySteam", releaseYear = 2021),
        Game("emu:gba:d", Source.EMULATOR, "Golden Sun", "gba", listOf("Rol (RPG)"), "Camelot", releaseYear = 2001, playtimeMinutes = 1300),
        Game("emu:psx:e", Source.EMULATOR, "Final Fantasy VII", "psx", listOf("Rol (RPG)"), "Square", releaseYear = 1997, playtimeMinutes = 3900),
        Game("emu:gc:f", Source.EMULATOR, "Metroid Prime", "gc", listOf("Acción"), "Retro Studios", releaseYear = 2002),
        Game("emu:nds:g", Source.EMULATOR, "Professor Layton", "nds", listOf("Puzle"), "Level-5", releaseYear = 2007),
        Game("emu:dreamcast:h", Source.EMULATOR, "Jet Set Radio", "dreamcast", listOf("Acción"), "Smilebit", releaseYear = 2000),
        Game("emu:wii:i", Source.EMULATOR, "Super Mario Galaxy", "wii", listOf("Plataformas"), "Nintendo", releaseYear = 2007),
    )

    private fun save(name: String) {
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun renderShelves() {
        runBlocking { LibraryStore(ApplicationProvider.getApplicationContext()).save(sample) }
        val vm = LibraryViewModel(ApplicationProvider.getApplicationContext())
        rule.setContent { RepisaTheme { ShelfScreen(vm, {}, {}) } }
        rule.waitUntil(5000) { vm.games.value.isNotEmpty() }
        rule.waitForIdle(); save("1-repisa")
        vm.setGroup(GroupMode.CONSOLE); rule.waitForIdle(); save("2-por-consola")
        vm.setGroup(GroupMode.GENRE); rule.waitForIdle(); save("3-por-genero")
    }

    @Test
    fun renderDetail() {
        val vm = LibraryViewModel(ApplicationProvider.getApplicationContext())
        rule.setContent { RepisaTheme { DetailScreen(sample[4], vm) {} } }
        rule.mainClock.advanceTimeBy(1500); rule.waitForIdle(); save("4-ficha")
    }

    @Test
    fun cleanTitles() {
        assertEquals("The Legend of Zelda - A Link to the Past",
            EmulatorSource.cleanTitle("Legend of Zelda, The - A Link to the Past (USA) [!]"))
        assertEquals("final fantasy vii (usa)", EmulatorSource.romKey("Final Fantasy VII (USA) (Disc 2)"))
    }
}
