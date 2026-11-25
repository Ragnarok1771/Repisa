package com.repisa.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GenresAndCopiesTest {
    @Test fun normalizaGenerosDeWikidataYSteam() {
        assertEquals(listOf("Gacha", "Rol (RPG)"), Genres.normalize(listOf("gacha game", "role-playing video game")))
        assertEquals(listOf("FPS", "Metroidvania"), Genres.normalize(listOf("first-person shooter", "Metroidvania")))
        assertEquals(listOf("Rol (RPG)", "Acción"), Genres.normalize(listOf("Rol", "Acción", "Violento", "Gore")))
        // Desconocido con otros reconocidos: fuera. Solo desconocidos: se queda limpio.
        assertEquals(listOf("Plataformas"), Genres.normalize(listOf("platform game", "Nintendo franchise thing")))
        assertEquals(listOf("Interactive film"), Genres.normalize(listOf("interactive film video game")))
        // Lo escrito a mano: se reconoce lo conocido y lo demás se respeta.
        assertEquals(listOf("FPS", "Mis favoritos"), Genres.normalize(listOf("fps", "Mis favoritos"), keepUnknown = true))
        assertEquals(listOf("Hack and slash"), Genres.normalize(listOf("character-action video game")))
        assertEquals(listOf("Novela visual"), Genres.normalize(listOf("dating sim")))
        // Títulos entre corchetes y con marcas registradas: se buscan por su nombre.
        assertEquals("REDACTED", com.repisa.app.sources.EmulatorSource.cleanTitle("[REDACTED]"))
        assertEquals("Rocket League", com.repisa.app.sources.EmulatorSource.cleanTitle("Rocket League®"))
    }

    @Test fun etiquetasDeSteam() {
        val p3r = listOf("JRPG" to 757, "Anime" to 600, "RPG" to 592, "Party-Based RPG" to 564, "Story Rich" to 531,
            "Turn-Based Strategy" to 529, "Strategy RPG" to 500, "Dating Sim" to 467, "Adventure" to 464, "Strategy" to 452)
        assertEquals(listOf("Rol (RPG)", "Aventura"), Genres.fromSteamTags(p3r))
        val doom = listOf("FPS" to 1064, "Gore" to 987, "Action" to 850, "Shooter" to 737, "Sci-fi" to 453)
        assertEquals(listOf("FPS", "Acción"), Genres.fromSteamTags(doom))
        val fe = listOf("Tactical RPG" to 900, "Strategy" to 800, "Anime" to 700)
        assertEquals(listOf("Rol (RPG)", "Estrategia"), Genres.fromSteamTags(fe))
    }

    @Test fun uneCopiasEnEspanolEIngles() {
        val es = Game("emu:3ds:a", Source.EMULATOR, "Pokémon Mundo Megamisterioso", "3ds",
            wikidataId = "Q19962722", altTitles = listOf("Pokémon Super Mystery Dungeon", "Pokémon Mundo Megamisterioso"), playtimeMinutes = 50)
        val en = Game("emu:3ds:b", Source.EMULATOR, "Pokemon Super Mystery Dungeon (Europe)", "3ds", playtimeMinutes = 10)
        val otra = Game("emu:3ds:c", Source.EMULATOR, "Pokémon Mystery Dungeon: Gates to Infinity", "3ds")
        val wii = Game("emu:wiiu:d", Source.EMULATOR, "Pokemon Super Mystery Dungeon", "wiiu")
        val merged = Copies.merge(listOf(es, en, otra, wii))
        assertEquals(3, merged.size)
        val box = merged.first { it.copyIds.isNotEmpty() }
        assertEquals("Pokémon Mundo Megamisterioso", box.title) // el idioma de la copia preferente (la más jugada)
        assertEquals(listOf("emu:3ds:b"), box.copyIds)
        // Separadas por el usuario: no se unen.
        assertEquals(4, Copies.merge(listOf(es, en.copy(noMerge = true), otra, wii)).size)
    }
}
