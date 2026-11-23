package com.repisa.app.model

/**
 * Unifica los géneros que llegan de Steam (en español), IGDB (en inglés) y ES-DE/ScreenScraper
 * para que "Platform", "Plataformas" y "Platformer" acaben en la misma repisa.
 */
object Genres {
    private val map: Map<String, String> = mapOf(
        "action" to "Acción", "acción" to "Acción", "accion" to "Acción",
        "adventure" to "Aventura", "aventura" to "Aventura", "point-and-click" to "Aventura",
        "point and click" to "Aventura", "visual novel" to "Novela visual",
        "rpg" to "Rol (RPG)", "rol" to "Rol (RPG)", "role-playing (rpg)" to "Rol (RPG)",
        "role playing game" to "Rol (RPG)", "role-playing" to "Rol (RPG)", "role playing" to "Rol (RPG)",
        "jrpg" to "Rol (RPG)", "action rpg" to "Rol (RPG)", "tactical" to "Estrategia",
        "strategy" to "Estrategia", "estrategia" to "Estrategia",
        "real time strategy (rts)" to "Estrategia", "turn-based strategy (tbs)" to "Estrategia",
        "simulation" to "Simulación", "simulator" to "Simulación", "simuladores" to "Simulación",
        "simulación" to "Simulación", "simulacion" to "Simulación",
        "sports" to "Deportes", "sport" to "Deportes", "deportes" to "Deportes",
        "racing" to "Carreras", "carreras" to "Carreras", "driving" to "Carreras", "race" to "Carreras",
        "platform" to "Plataformas", "platformer" to "Plataformas", "plataformas" to "Plataformas",
        "shooter" to "Disparos", "shoot'em up" to "Disparos", "shoot 'em up" to "Disparos",
        "shmup" to "Disparos", "fps" to "Disparos", "lightgun" to "Disparos", "disparos" to "Disparos",
        "fighting" to "Lucha", "fight" to "Lucha", "lucha" to "Lucha", "versus fighting" to "Lucha",
        "beat'em up" to "Beat 'em up", "beat 'em up" to "Beat 'em up", "hack and slash/beat 'em up" to "Beat 'em up",
        "hack and slash" to "Beat 'em up",
        "puzzle" to "Puzle", "puzle" to "Puzle", "puzzle-solving" to "Puzle",
        "music" to "Música / Ritmo", "rhythm" to "Música / Ritmo",
        "arcade" to "Arcade", "pinball" to "Arcade",
        "indie" to "Indie", "casual" to "Casual", "quiz/trivia" to "Casual", "card & board game" to "Mesa y cartas",
        "board game" to "Mesa y cartas", "card game" to "Mesa y cartas",
        "massively multiplayer" to "MMO", "multijugador masivo" to "MMO", "mmo" to "MMO",
        "free to play" to "Free to play", "gratuitos" to "Free to play", "free-to-play" to "Free to play",
        "horror" to "Terror", "survival horror" to "Terror", "terror" to "Terror",
        "survival" to "Supervivencia", "open world" to "Mundo abierto", "metroidvania" to "Metroidvania",
        "roguelike" to "Roguelike", "roguelite" to "Roguelike", "moba" to "MOBA",
        "early access" to "Acceso anticipado", "acceso anticipado" to "Acceso anticipado",
        "education" to "Educativo", "educational" to "Educativo",
    )

    /** Separa ("Action / Platform", "Acción, Aventura") y normaliza; elimina duplicados. */
    fun normalize(raw: Iterable<String>): List<String> =
        raw.flatMap { it.split('/', ',', ';', '|') }
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.lowercase() !in IGNORED }
            .map { g -> map[g.lowercase()] ?: g.replaceFirstChar { it.uppercase() } }
            .distinct()

    private val IGNORED = setOf("game", "games", "misc", "other", "others", "otros", "utilities", "software training")
}
