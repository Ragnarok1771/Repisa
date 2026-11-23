package com.repisa.app

import com.repisa.app.model.Consoles
import com.repisa.app.model.Genres
import com.repisa.app.sources.EmulatorSource
import org.junit.Assert.assertEquals
import org.junit.Test

class RomNamesTest {
    @Test
    fun cleanTitleMovesArticleAndDropsTags() {
        assertEquals("The Legend of Zelda - A Link to the Past",
            EmulatorSource.cleanTitle("Legend of Zelda, The - A Link to the Past (USA) [!]"))
        assertEquals("Super Mario World", EmulatorSource.cleanTitle("Super Mario World (USA)"))
    }

    @Test
    fun romKeyIgnoresDiscNumber() {
        assertEquals(EmulatorSource.romKey("Final Fantasy VII (USA) (Disc 1)"),
            EmulatorSource.romKey("Final Fantasy VII (USA) (Disc 3)"))
    }

    @Test
    fun consolesAreRecognised() {
        assertEquals("snes", Consoles.byFolder("SNES")?.id)
        assertEquals("psx", Consoles.byLibretroName("Sony - PlayStation.lpl")?.id)
        assertEquals("gba", Consoles.guess("mgba_libretro_android.so")?.id)
        assertEquals("ps2", Consoles.guess("NetherSX2")?.id)
    }

    @Test
    fun genresAreUnified() {
        assertEquals(listOf("Acción", "Plataformas"), Genres.normalize(listOf("Action / Platform", "Plataformas")))
        assertEquals(listOf("Rol (RPG)"), Genres.normalize(listOf("Role-playing (RPG)", "Rol")))
    }

    @Test
    fun playtimeFormats() {
        assertEquals(630, EmulatorSource.parsePlaytime("37800"))      // segundos (Batocera)
        assertEquals(630, EmulatorSource.parsePlaytime("10:30:00"))
        assertEquals(630, EmulatorSource.parsePlaytime("10h 30m"))
        assertEquals(null, EmulatorSource.parsePlaytime("nunca"))
    }
}
