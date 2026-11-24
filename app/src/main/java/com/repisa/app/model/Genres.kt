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

    /**
     * Géneros descriptivos (Wikidata, GameTDB): "tactical role-playing game", "action-adventure game",
     * "platformer"... se reconocen por palabras clave. El orden importa: lo más específico primero.
     */
    private val keywords: List<Pair<Regex, List<String>>> = listOf(
        "action-adventure|action adventure" to listOf("Acción", "Aventura"),
        "role-playing|role playing|\\brpg\\b|dungeon crawl|monster-tam|monster tam|monster collect" to listOf("Rol (RPG)"),
        "strateg|tactic|4x|tower defen" to listOf("Estrategia"),
        "metroidvania" to listOf("Metroidvania"),
        "roguelike|roguelite|rogue-like" to listOf("Roguelike"),
        "platform" to listOf("Plataformas"),
        "beat 'em up|beat-'em-up|beat em up|hack and slash|hack-and-slash|musou|brawler" to listOf("Beat 'em up"),
        "shooter|shoot 'em up|shoot-'em-up|shmup|light gun|lightgun" to listOf("Disparos"),
        "fighting" to listOf("Lucha"),
        "racing|kart|driving" to listOf("Carreras"),
        "puzzle" to listOf("Puzle"),
        "visual novel" to listOf("Novela visual"),
        "survival horror|horror" to listOf("Terror"),
        "rhythm|music" to listOf("Música / Ritmo"),
        "sport|football|soccer|golf|tennis|basketball|baseball|wrestling|boxing" to listOf("Deportes"),
        "simulat|farming|life sim|management|god game" to listOf("Simulación"),
        "party|minigame|mini-game|trivia|quiz" to listOf("Casual"),
        "card|board game|mahjong" to listOf("Mesa y cartas"),
        "open world|sandbox" to listOf("Mundo abierto"),
        "survival" to listOf("Supervivencia"),
        "adventure|point-and-click|point and click" to listOf("Aventura"),
        "action|stealth" to listOf("Acción"),
        "educational|education" to listOf("Educativo"),
        "massively multiplayer|mmo" to listOf("MMO"),
    ).map { (k, v) -> Regex(k, RegexOption.IGNORE_CASE) to v }

    private fun byKeyword(g: String): List<String>? = keywords.firstOrNull { it.first.containsMatchIn(g) }?.second

    /** Separa ("Action / Platform", "Acción, Aventura") y normaliza; elimina duplicados. */
    fun normalize(raw: Iterable<String>): List<String> =
        raw.flatMap { it.split('/', ',', ';', '|') }
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.lowercase() !in IGNORED }
            .flatMap { g -> map[g.lowercase()]?.let { listOf(it) } ?: byKeyword(g) ?: listOf(g.replaceFirstChar { it.uppercase() }) }
            .distinct()

    private val IGNORED = setOf("video game", "videogame", "game", "games", "misc", "other", "others", "otros", "utilities", "software training")
}
