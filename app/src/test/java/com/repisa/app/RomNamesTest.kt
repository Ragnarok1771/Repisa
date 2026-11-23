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

    @Test
    fun messyFileNames() {
        assertEquals("Kingdom Hearts 358-2 Days", EmulatorSource.cleanTitle("4262 - Kingdom Hearts 358-2 Days (Europe) (En,Fr,De,Es,It)"))
        assertEquals("Bravely Default II", EmulatorSource.cleanTitle("Bravely_Default_II_[0100B6801137E000][v0]"))
        assertEquals("Bravely Default 2", EmulatorSource.cleanTitle("Bravely Default 2 NSP eShop Update v1.0.2"))
        assertEquals("1942", EmulatorSource.cleanTitle("1942 (USA)"))
    }

    @Test
    fun keywordMatching() {
        val file = com.repisa.app.sources.CoverSearch.tokens("Bravely Default 2 NSP eShop")
        val right = com.repisa.app.sources.CoverSearch.tokens("Bravely Default II (World)")
        val wrong = com.repisa.app.sources.CoverSearch.tokens("Bravely Default (Europe)")
        val s1 = com.repisa.app.sources.CoverSearch.similarity(file, right)
        val s2 = com.repisa.app.sources.CoverSearch.similarity(file, wrong)
        assert(s1 >= 0.72) { "s1=$s1" }
        assert(s2 < s1) { "s2=$s2 s1=$s1" }
        val kh = com.repisa.app.sources.CoverSearch.tokens("4262 - Kingdom Hearts 358-2 Days (Europe)")
        val khBox = com.repisa.app.sources.CoverSearch.tokens("Kingdom Hearts 358-2 Days (USA)")
        val kh2 = com.repisa.app.sources.CoverSearch.tokens("Kingdom Hearts II (USA)")
        assert(com.repisa.app.sources.CoverSearch.similarity(kh, khBox) >= 0.9)
        assert(com.repisa.app.sources.CoverSearch.similarity(kh, kh2) < 0.72)
    }
}
