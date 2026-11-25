package com.repisa.app

import com.repisa.app.model.Genres
import org.junit.Assert.assertEquals
import org.junit.Test

class GenresTest {
    @Test fun wikidataLabels() {
        assertEquals(listOf("Rol (RPG)", "Estrategia"), Genres.normalize(listOf("tactical role-playing game")))
        assertEquals(listOf("Rol (RPG)"), Genres.normalize(listOf("role-playing video game")))
        assertEquals(listOf("Acción", "Aventura"), Genres.normalize(listOf("action-adventure game")))
        assertEquals(listOf("Plataformas"), Genres.normalize(listOf("platform game")))
        assertEquals(listOf("Simulación"), Genres.normalize(listOf("life simulation game")))
        assertEquals(listOf("Lucha"), Genres.normalize(listOf("fighting game")))
        assertEquals(listOf("FPS"), Genres.normalize(listOf("first-person shooter")))
        assertEquals(emptyList<String>(), Genres.normalize(listOf("video game")))
    }

    @Test fun gameTdbGenres() {
        assertEquals(listOf("Acción", "Estrategia"), Genres.normalize(listOf("action,strategy")))
        assertEquals(listOf("Rol (RPG)", "Estrategia"), Genres.normalize(listOf("role-playing,tactics")))
        assertEquals(listOf("Plataformas"), Genres.normalize(listOf("platformer")))
    }

    @Test fun steamGenresUnchanged() {
        assertEquals(listOf("Acción", "Aventura", "Indie"), Genres.normalize(listOf("Acción", "Aventura", "Indie")))
        assertEquals(listOf("Rol (RPG)", "Simulación"), Genres.normalize(listOf("Rol", "Simuladores")))
    }
}
