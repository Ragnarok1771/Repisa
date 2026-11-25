package com.repisa.app.model

/**
 * Unifica los géneros que llegan de Steam (etiquetas de la comunidad o géneros de la ficha, en español),
 * Wikidata/GameTDB (en inglés) y ES-DE/ScreenScraper, para que "Platform", "Plataformas" y "Platformer"
 * acaben en la misma repisa. Internamente cada género se guarda con su nombre en español (ver GENRES en
 * tools/i18n/strings.py, que da el texto en cada idioma).
 */
object Genres {
    private const val RPG = "Rol (RPG)"
    private const val STRATEGY = "Estrategia"
    private const val SHOOTER = "Disparos"
    private const val FPS = "FPS"

    /** Nombres exactos (en minúsculas) → género. */
    private val map: Map<String, String> = mapOf(
        "action" to "Acción", "acción" to "Acción", "accion" to "Acción",
        "adventure" to "Aventura", "aventura" to "Aventura", "point-and-click" to "Aventura",
        "point and click" to "Aventura", "visual novel" to "Novela visual", "novela visual" to "Novela visual",
        "rpg" to RPG, "rol" to RPG, "role-playing (rpg)" to RPG, "role playing game" to RPG, "role-playing" to RPG,
        "role playing" to RPG, "jrpg" to RPG, "action rpg" to RPG, "tactical" to STRATEGY,
        "strategy" to STRATEGY, "estrategia" to STRATEGY,
        "real time strategy (rts)" to STRATEGY, "turn-based strategy (tbs)" to STRATEGY,
        "simulation" to "Simulación", "simulator" to "Simulación", "simuladores" to "Simulación",
        "simulación" to "Simulación", "simulacion" to "Simulación",
        "sports" to "Deportes", "sport" to "Deportes", "deportes" to "Deportes",
        "racing" to "Carreras", "carreras" to "Carreras", "driving" to "Carreras", "race" to "Carreras",
        "platform" to "Plataformas", "platformer" to "Plataformas", "plataformas" to "Plataformas",
        "shooter" to SHOOTER, "shoot'em up" to SHOOTER, "shoot 'em up" to SHOOTER,
        "shmup" to SHOOTER, "lightgun" to SHOOTER, "disparos" to SHOOTER,
        "fps" to FPS, "first-person shooter" to FPS, "first person shooter" to FPS, "shooter en primera persona" to FPS,
        "fighting" to "Lucha", "fight" to "Lucha", "lucha" to "Lucha", "versus fighting" to "Lucha",
        "beat'em up" to "Beat 'em up", "beat 'em up" to "Beat 'em up", "yo contra el barrio" to "Beat 'em up",
        "hack and slash" to "Hack and slash", "hack & slash" to "Hack and slash", "hack and slash/beat 'em up" to "Hack and slash",
        "puzzle" to "Puzle", "puzle" to "Puzle", "puzzle-solving" to "Puzle", "rompecabezas" to "Puzle",
        "music" to "Música / Ritmo", "rhythm" to "Música / Ritmo", "ritmo" to "Música / Ritmo", "música" to "Música / Ritmo",
        "arcade" to "Arcade", "pinball" to "Arcade",
        "indie" to "Indie", "casual" to "Casual", "quiz/trivia" to "Casual", "card & board game" to "Mesa y cartas",
        "board game" to "Mesa y cartas", "card game" to "Mesa y cartas",
        "massively multiplayer" to "MMO", "multijugador masivo" to "MMO", "mmo" to "MMO", "mmorpg" to "MMO",
        "free to play" to "Free to play", "gratuitos" to "Free to play", "free-to-play" to "Free to play", "f2p" to "Free to play",
        "horror" to "Terror", "survival horror" to "Terror", "terror" to "Terror",
        "survival" to "Supervivencia", "supervivencia" to "Supervivencia",
        "open world" to "Mundo abierto", "mundo abierto" to "Mundo abierto", "metroidvania" to "Metroidvania",
        "roguelike" to "Roguelike", "roguelite" to "Roguelike", "moba" to "MOBA",
        "early access" to "Acceso anticipado", "acceso anticipado" to "Acceso anticipado",
        "education" to "Educativo", "educational" to "Educativo", "educación" to "Educativo", "educativo" to "Educativo",
        "gacha" to "Gacha", "soulslike" to "Soulslike", "souls-like" to "Soulslike", "souls like" to "Soulslike",
        "sigilo" to "Sigilo", "stealth" to "Sigilo",
        "fantasy" to "Fantasía", "fantasía" to "Fantasía", "fantasia" to "Fantasía",
        "sci-fi" to "Ciencia ficción", "science fiction" to "Ciencia ficción", "ciencia ficción" to "Ciencia ficción",
        "ciencia ficcion" to "Ciencia ficción", "scifi" to "Ciencia ficción",
        "battle royale" to "Battle royale", "party" to "Party", "party game" to "Party", "fiesta" to "Party",
    )

    /**
     * Géneros descriptivos (Wikidata, GameTDB, ScreenScraper): "tactical role-playing game", "gacha game",
     * "first-person shooter"... se reconocen por palabras clave. El orden importa: lo más específico primero.
     */
    private val keywords: List<Pair<Regex, List<String>>> = listOf(
        "gacha" to listOf("Gacha"),
        "souls-?like" to listOf("Soulslike"),
        "battle royale" to listOf("Battle royale"),
        "first-person shooter|first person shooter|\\bfps\\b" to listOf(FPS),
        "tactical role-playing|tactical rpg|strategy rpg|strategy role-playing" to listOf(RPG, STRATEGY),
        "action-adventure|action adventure" to listOf("Acción", "Aventura"),
        "role-playing|role playing|\\brpg\\b|dungeon crawl|monster-tam|monster tam|monster collect" to listOf(RPG),
        "strateg|tactic|\\b4x\\b|tower defen|wargame" to listOf(STRATEGY),
        "metroidvania" to listOf("Metroidvania"),
        "roguelike|roguelite|rogue-like" to listOf("Roguelike"),
        "platform" to listOf("Plataformas"),
        "hack and slash|hack-and-slash|hack & slash|musou|character.action|spectacle fight" to listOf("Hack and slash"),
        "beat 'em up|beat-'em-up|beat em up|brawler" to listOf("Beat 'em up"),
        "shooter|shoot 'em up|shoot-'em-up|shmup|light gun|lightgun|bullet hell" to listOf(SHOOTER),
        "fighting|fighter" to listOf("Lucha"),
        "racing|kart|driving" to listOf("Carreras"),
        "puzzle" to listOf("Puzle"),
        "visual novel|dating sim|otome" to listOf("Novela visual"),
        "survival horror|horror" to listOf("Terror"),
        "stealth" to listOf("Sigilo"),
        "rhythm|music" to listOf("Música / Ritmo"),
        "sport|football|soccer|golf|tennis|basketball|baseball|wrestling|boxing|skateboard" to listOf("Deportes"),
        "party|minigame|mini-game" to listOf("Party"),
        "simulat|farming|life sim|management|god game|city build|colony" to listOf("Simulación"),
        "trivia|quiz|casual" to listOf("Casual"),
        "card|board game|mahjong|deckbuild|tabletop" to listOf("Mesa y cartas"),
        "open world|sandbox" to listOf("Mundo abierto"),
        "survival" to listOf("Supervivencia"),
        "adventure|point-and-click|point and click|walking sim" to listOf("Aventura"),
        "action" to listOf("Acción"),
        "educational|education" to listOf("Educativo"),
        "massively multiplayer|\\bmmo" to listOf("MMO"),
        "fantasy" to listOf("Fantasía"),
        "science fiction|sci-fi|cyberpunk|space opera" to listOf("Ciencia ficción"),
    ).map { (k, v) -> Regex(k, RegexOption.IGNORE_CASE) to v }

    /** Todos los géneros que Repisa conoce (los que tienen traducción). */
    val known: Set<String> = (map.values + keywords.flatMap { it.second }).toSet()
    private val knownLower = known.associateBy { it.lowercase() }

    private fun byKeyword(g: String): List<String>? = keywords.firstOrNull { it.first.containsMatchIn(g) }?.second

    /** Un nombre de género suelto → géneros de Repisa (vacío si no se reconoce). */
    private fun recognize(g: String): List<String> {
        val low = g.lowercase().trim()
        knownLower[low]?.let { return listOf(it) }
        map[low]?.let { return listOf(it) }
        // «Fantasy video game», «Action game» → sin la coletilla.
        val core = low.replace(Regex("""\s*(video\s*)?games?$"""), "").trim()
        map[core]?.let { return listOf(it) }
        return byKeyword(g).orEmpty()
    }

    /**
     * Separa ("Action / Platform", "Acción, Aventura") y normaliza; elimina duplicados y redundancias.
     * Lo que no se reconoce se descarta si hay algo reconocido; si no hay nada, se queda limpio («Interactive film»).
     * Con [keepUnknown] (lo que escribe el usuario a mano) se conserva siempre, tal cual lo escribió.
     */
    fun normalize(raw: Iterable<String>, keepUnknown: Boolean = false): List<String> {
        val parts = raw.flatMap { it.split('/', ',', ';', '|') }.map { it.trim() }
            .filter { it.isNotEmpty() && it.lowercase() !in IGNORED }
        val known = mutableListOf<String>()
        val unknown = mutableListOf<String>()
        parts.forEach { p ->
            val r = recognize(p)
            if (r.isNotEmpty()) known += r
            else unknown += p.replace(Regex("""\s*(video\s*)?games?$""", RegexOption.IGNORE_CASE), "").trim()
                .replaceFirstChar { it.uppercase() }.ifEmpty { return@forEach }
        }
        val out = tidy(known)
        return when {
            keepUnknown -> (out + unknown).distinct()
            out.isNotEmpty() -> out
            else -> unknown.distinct()
        }
    }

    /** Quita redundancias: «FPS» ya dice «Disparos». */
    private fun tidy(list: List<String>): List<String> {
        val d = list.distinct()
        return if (FPS in d) d - SHOOTER else d
    }

    /**
     * Etiquetas de la comunidad de Steam (en inglés, de más a menos votada, con su peso) → hasta 4 géneros.
     * Solo cuentan las etiquetas con al menos la mitad de votos que la principal, para no colar géneros
     * secundarios («Persona» tiene «Turn-Based Strategy», pero es un RPG).
     */
    fun fromSteamTags(tags: List<Pair<String, Int>>): List<String> {
        val top = tags.maxOfOrNull { it.second } ?: return emptyList()
        val out = mutableListOf<String>()
        tags.sortedByDescending { it.second }.filter { it.second * 2 >= top }.forEach { (name, _) ->
            val low = name.lowercase()
            val g = STEAM_TAGS[low] ?: return@forEach
            // «Turn-Based Strategy» en un JRPG es por los combates, no un juego de estrategia (los RPG tácticos
            // suelen tener «Tactical RPG» como etiqueta principal, y esa da los dos géneros a la vez).
            if (STRATEGY in g && RPG in out) return@forEach
            g.forEach { if (it !in out) out += it }
        }
        return tidy(out).take(4)
    }

    /** Etiquetas de Steam que son un género (las de estilo, ambiente o modo de juego no cuentan). */
    private val STEAM_TAGS: Map<String, List<String>> = mapOf(
        "action" to "Acción", "adventure" to "Aventura", "action-adventure" to "Acción",
        "rpg" to RPG, "jrpg" to RPG, "action rpg" to RPG, "party-based rpg" to RPG, "crpg" to RPG,
        "dungeon crawler" to RPG, "creature collector" to RPG, "mystery dungeon" to RPG,
        "strategy" to STRATEGY, "turn-based strategy" to STRATEGY, "turn-based tactics" to STRATEGY,
        "rts" to STRATEGY, "grand strategy" to STRATEGY, "4x" to STRATEGY, "tower defense" to STRATEGY,
        "tactical" to STRATEGY, "real-time tactics" to STRATEGY, "wargame" to STRATEGY, "auto battler" to STRATEGY,
        "simulation" to "Simulación", "life sim" to "Simulación", "farming sim" to "Simulación", "city builder" to "Simulación",
        "colony sim" to "Simulación", "management" to "Simulación", "god game" to "Simulación", "space sim" to "Simulación",
        "automobile sim" to "Simulación", "job simulator" to "Simulación", "flight" to "Simulación",
        "sports" to "Deportes", "football (soccer)" to "Deportes", "football (american)" to "Deportes", "basketball" to "Deportes",
        "golf" to "Deportes", "tennis" to "Deportes", "baseball" to "Deportes", "wrestling" to "Deportes", "boxing" to "Deportes",
        "skateboarding" to "Deportes", "hockey" to "Deportes",
        "racing" to "Carreras", "driving" to "Carreras", "combat racing" to "Carreras",
        "platformer" to "Plataformas", "2d platformer" to "Plataformas", "3d platformer" to "Plataformas",
        "precision platformer" to "Plataformas", "puzzle platformer" to "Plataformas", "collectathon" to "Plataformas",
        "fps" to FPS, "boomer shooter" to FPS, "arena shooter" to FPS,
        "shooter" to SHOOTER, "third-person shooter" to SHOOTER, "shoot 'em up" to SHOOTER, "bullet hell" to SHOOTER,
        "twin stick shooter" to SHOOTER, "top-down shooter" to SHOOTER, "looter shooter" to SHOOTER, "hero shooter" to SHOOTER,
        "extraction shooter" to SHOOTER, "on-rails shooter" to SHOOTER, "rail shooter" to SHOOTER,
        "fighting" to "Lucha", "2d fighter" to "Lucha", "3d fighter" to "Lucha", "martial arts" to "Lucha",
        "beat 'em up" to "Beat 'em up", "hack and slash" to "Hack and slash", "character action game" to "Hack and slash",
        "spectacle fighter" to "Hack and slash", "musou" to "Hack and slash",
        "puzzle" to "Puzle", "match 3" to "Puzle", "sokoban" to "Puzle", "hidden object" to "Puzle", "logic" to "Puzle",
        "rhythm" to "Música / Ritmo",
        "arcade" to "Arcade", "pinball" to "Arcade",
        "indie" to "Indie", "casual" to "Casual",
        "card game" to "Mesa y cartas", "card battler" to "Mesa y cartas", "trading card game" to "Mesa y cartas",
        "board game" to "Mesa y cartas", "deckbuilding" to "Mesa y cartas", "roguelike deckbuilder" to "Mesa y cartas",
        "chess" to "Mesa y cartas", "mahjong" to "Mesa y cartas", "tabletop" to "Mesa y cartas", "solitaire" to "Mesa y cartas",
        "massively multiplayer" to "MMO", "mmorpg" to "MMO", "free to play" to "Free to play",
        "horror" to "Terror", "survival horror" to "Terror", "psychological horror" to "Terror",
        "survival" to "Supervivencia", "open world survival craft" to "Supervivencia",
        "open world" to "Mundo abierto", "sandbox" to "Mundo abierto", "metroidvania" to "Metroidvania",
        "roguelike" to "Roguelike", "roguelite" to "Roguelike", "action roguelike" to "Roguelike",
        "traditional roguelike" to "Roguelike", "moba" to "MOBA", "early access" to "Acceso anticipado",
        "education" to "Educativo", "visual novel" to "Novela visual", "otome" to "Novela visual",
        "interactive fiction" to "Novela visual", "point & click" to "Aventura", "walking simulator" to "Aventura",
        "souls-like" to "Soulslike", "stealth" to "Sigilo", "battle royale" to "Battle royale",
        "fantasy" to "Fantasía", "dark fantasy" to "Fantasía", "sci-fi" to "Ciencia ficción", "cyberpunk" to "Ciencia ficción",
        "space" to "Ciencia ficción", "party game" to "Party", "party" to "Party", "minigames" to "Party",
    ).mapValues { listOf(it.value) } + mapOf(
        "tactical rpg" to listOf(RPG, STRATEGY), "strategy rpg" to listOf(RPG, STRATEGY),
    )

    private val IGNORED = setOf(
        "video game", "videogame", "game", "games", "misc", "other", "others", "otros", "utilities", "utilidades",
        "software training", "formación de software", "violento", "violent", "gore", "desnudos", "nudity",
        "contenido sexual", "sexual content", "animación y modelado", "diseño e ilustración", "edición de fotos",
        "producción de vídeo", "producción de audio", "desarrollo web", "contabilidad", "animation & modeling",
        "design & illustration", "photo editing", "video production", "audio production", "web publishing",
    )
}
