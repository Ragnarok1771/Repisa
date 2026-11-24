package com.repisa.app

import com.repisa.app.sources.CoverSearch
import com.repisa.app.sources.FrontendEntry
import com.repisa.app.sources.FrontendIndex
import com.repisa.app.sources.FrontendMeta
import com.repisa.app.sources.FrontendReader
import com.repisa.app.sources.FrontendReport
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrontendTest {
    private fun meta(name: String, minutes: Int? = null, genre: String? = null) =
        FrontendMeta(name, genre, null, null, null, null, null, null, minutes)

    private fun entry(console: String?, path: String?, name: String, minutes: Int? = null, genre: String? = null) =
        FrontendEntry(console, path?.let { FrontendReader.fileKeyOf(it) }, CoverSearch.tokens(name), meta(name, minutes, genre))

    @Test fun fileKeysFromAnyPath() {
        assertEquals("atelier ryza", FrontendReader.fileKeyOf("./Atelier Ryza.nsp"))
        assertEquals("atelier ryza", FrontendReader.fileKeyOf("content://com.android.externalstorage.documents/tree/primary%3AROMs%2Fswitch/document/primary%3AROMs%2Fswitch%2FAtelier%20Ryza.nsp"))
        assertEquals("mario + rabbids", FrontendReader.fileKeyOf("/storage/emulated/0/ROMs/switch/Mario + Rabbids.xci"))
        assertEquals("fire emblem three houses [010055d009f78000][v0]",
            FrontendReader.fileKeyOf("Fire Emblem Three Houses [010055D009F78000][v0].nsp"))
    }

    @Test fun playtimeUnits() {
        assertEquals(1072, FrontendReader.toMinutes("playtime", JsonPrimitive(64_320)))          // segundos
        assertEquals(1072, FrontendReader.toMinutes("playtimems", JsonPrimitive(64_320_000)))    // ms
        assertEquals(90, FrontendReader.toMinutes("playtimeminutes", JsonPrimitive(90)))
        assertEquals(1072, FrontendReader.toMinutes("totalplaytime", JsonPrimitive(64_320_000_000L / 1000))) // ms sin indicarlo
        assertTrue(FrontendReader.isPlaytimeKey("totalPlayTime"))
        assertTrue(FrontendReader.isPlaytimeKey("play_time_seconds"))
        assertTrue(!FrontendReader.isPlaytimeKey("lastplayed"))
    }

    @Test fun matchesByFileNameAndByTitle() {
        val index = FrontendIndex(listOf(
            entry("switch", "./Fire Emblem Three Houses [010055D009F78000][v0].nsp", "Fire Emblem: Three Houses", 1072, "Strategy"),
            entry("switch", "content://x/Atelier%20Ryza%20Ever%20Darkness.nsp", "Atelier Ryza: Ever Darkness & the Secret Hideout", 600),
            entry(null, null, "Bravely Default II", 300),           // de una copia de seguridad sin sistema
            entry("3ds", null, "Fire Emblem Echoes: Shadows of Valentia", 50),
        ))
        // Mismo archivo
        val fe = index.find("switch", "fire emblem three houses [010055d009f78000][v0]", "Fire Emblem Three Houses")
        assertEquals(1072, fe?.playMinutes)
        assertEquals("Strategy", fe?.genre)
        // Distinto archivo, título casi igual
        val ryza = index.find("switch", "atelier ryza ever darkness and the secret hideout [0100d1900ec80000][v0]",
            "Atelier Ryza Ever Darkness and the Secret Hideout")
        assertEquals(600, ryza?.playMinutes)
        // Sin sistema: solo por nombre
        assertEquals(300, index.find("switch", "bravely_default_ii_[0100b6801137e000]", "Bravely Default II")?.playMinutes)
        // No mezcla sistemas ni juegos distintos
        assertNull(index.find("switch", "fire emblem engage", "Fire Emblem Engage"))
        assertNotNull(index.find("3ds", "fe echoes", "Fire Emblem Echoes: Shadows of Valentia"))
        assertTrue(index.unused().isEmpty())
    }

    @Test fun reportRoundTrip() {
        val r = FrontendReport(3, setOf("switch", "3ds"), 120, 100, 80, 1, 75, 90, 70, 123L)
        assertEquals(r, FrontendReport.decode(r.encode()))
        assertNull(FrontendReport.decode(null))
    }

    @Test fun looseParserSurvivesBrokenXml() {
        val xml = "\uFEFF<?xml version=\"1.0\"?>\n<gameList>\n" +
            "<game id=\"7\"><path>./Atelier Ryza.nsp</path><name>Atelier Ryza: Ever Darkness & the Secret Hideout</name>" +
            "<genre>Role-playing</genre><playtime>26940</playtime><desc><![CDATA[Ryza <3]]></desc></game>\n" +
            "<Game path=\"./Kirby Mass Attack.nds\" name=\"Kirby Mass Attack\"/>\n</gameList>"
        val games = FrontendReader.parseLoose(xml)
        assertEquals(2, games.size)
        assertEquals("Atelier Ryza: Ever Darkness & the Secret Hideout", games[0]["name"])
        assertEquals("26940", games[0]["playtime"])
        assertEquals("Ryza <3", games[0]["desc"])
        assertEquals("7", games[0]["id"])
        assertEquals("Kirby Mass Attack", games[1]["name"])
    }
}
