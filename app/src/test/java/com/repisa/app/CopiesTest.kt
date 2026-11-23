package com.repisa.app

import com.repisa.app.model.Copies
import com.repisa.app.model.Game
import com.repisa.app.model.Source
import com.repisa.app.model.Tools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CopiesTest {
    private fun g(id: String, title: String, source: Source = Source.STEAM, minutes: Int = 0) =
        Game(id, source, title, "pc", playtimeMinutes = minutes)

    @Test
    fun gtaEditionsAreOneGame() {
        val games = listOf(
            g("steam:1", "Grand Theft Auto V Legacy", minutes = 600),
            g("steam:2", "Grand Theft Auto V Enhanced", minutes = 120),
            g("epic:a", "Grand Theft Auto V", Source.EPIC),
            g("epic:b", "Grand Theft Auto V Enhanced", Source.EPIC, 30),
            g("steam:3", "Borderlands 2", minutes = 50),
            g("epic:c", "Borderlands 2", Source.EPIC, 10),
            g("steam:4", "Borderlands 3"),
        )
        val merged = Copies.merge(games)
        assertEquals(3, merged.size)
        val gta = merged.first { it.title.startsWith("Grand") }
        assertEquals("steam:1", gta.id)          // la más jugada representa al grupo
        assertEquals(750, gta.totalMinutes)      // tiempo sumado de las cuatro copias
        assertEquals(3, gta.copyIds.size)
        assertEquals(60, merged.first { it.title == "Borderlands 2" }.totalMinutes)
    }

    @Test
    fun differentNumbersStaySeparate() {
        assertNotEquals(Copies.key(g("1", "Football Manager 2024")), Copies.key(g("2", "Football Manager 2026")))
        assertNotEquals(Copies.key(g("1", "Borderlands 2")), Copies.key(g("2", "Borderlands 3")))
    }

    @Test
    fun preferredCopyWins() {
        val merged = Copies.merge(listOf(
            g("steam:1", "Dead by Daylight", minutes = 900),
            g("epic:1", "Dead by Daylight", Source.EPIC).copy(preferredCopy = true),
        ))
        assertEquals("epic:1", merged.single().id)
    }

    @Test
    fun toolsAreDetected() {
        assertTrue(Tools.looksLikeTool("ARK Mod Kit (UE4)"))
        assertTrue(Tools.looksLikeTool("Football Manager 2024 Pre-Game Editor"))
        assertTrue(Tools.looksLikeTool("Football Manager 2024 Resource Archiver"))
        assertTrue(Tools.looksLikeTool("ARK Dedicated Server"))
        assertFalse(Tools.looksLikeTool("ARK: Survival Evolved"))
        assertFalse(Tools.looksLikeTool("ARK: Survival Of The Fittest"))
        assertFalse(Tools.looksLikeTool("Football Manager 2024"))
    }
}
