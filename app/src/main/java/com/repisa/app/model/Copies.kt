package com.repisa.app.model

import com.repisa.app.sources.CoverSearch
import com.repisa.app.sources.EmulatorSource

/**
 * Agrupa las copias del mismo juego (GTA V normal, Enhanced y Legacy; el mismo juego en Steam y en Epic...)
 * para mostrarlas como una sola caja. Solo se agrupan copias del mismo sistema.
 */
object Copies {
    /** Palabras de edición que no cambian el juego. */
    private val editionWords = setOf(
        "enhanced", "legacy", "edition", "definitive", "complete", "goty", "deluxe", "ultimate", "standard",
        "remastered", "hd", "anniversary", "premium", "gold", "special", "collectors", "collector", "digital",
        "classic", "directors", "cut", "prepare", "die", "year", "game", "bundle", "pack", "version",
    )

    /** Clave de grupo, o null si el usuario pidió no agrupar este juego. */
    private val keyCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** Clave de grupo (con caché: limpiar el título con expresiones regulares cuesta). */
    fun key(g: Game): String? {
        if (g.noMerge) return null
        val cacheKey = g.consoleId + "|" + g.title
        keyCache[cacheKey]?.let { return it.ifEmpty { null } }
        if (keyCache.size > 20_000) keyCache.clear()
        return computeKey(g).also { keyCache[cacheKey] = it ?: "" }
    }

    private fun computeKey(g: Game): String? {
        val words = CoverSearch.norm(EmulatorSource.cleanTitle(g.title)).split(' ')
            .filter { it.isNotBlank() && it !in editionWords && it !in setOf("the", "of", "to") }
            .map { if (it == "v") "5" else it } // "Grand Theft Auto V" = "... 5"
        if (words.isEmpty()) return null
        return g.consoleId + ":" + words.joinToString(" ")
    }

    /**
     * Devuelve una caja por grupo: la copia preferente (o la más jugada) con el tiempo sumado de todas,
     * la última partida más reciente y la lista de las demás copias.
     */
    fun merge(games: List<Game>): List<Game> {
        val out = mutableListOf<Game>()
        games.groupBy { key(it) ?: "solo:${it.id}" }.values.forEach { group ->
            if (group.size == 1) { out += group[0]; return@forEach }
            // Preferente: la elegida por el usuario; si no, la de Steam (la más completa); si no, la más jugada.
            val main = group.firstOrNull { it.preferredCopy }
                ?: group.maxWith(compareBy<Game> { it.source == Source.STEAM }.thenBy { it.totalMinutes })
            // Nombre de la caja: el más corto del grupo ("Grand Theft Auto V" en vez de "... Legacy"),
            // salvo que el usuario haya puesto uno a mano.
            val name = if (main.userEdited) main.title else group.minBy { it.title.length }.title
            out += main.copy(
                status = main.status ?: group.firstNotNullOfOrNull { it.status },
                collections = group.flatMap { it.collections }.distinct(),
                notes = main.notes ?: group.firstNotNullOfOrNull { it.notes },
                title = name,
                originalTitle = main.title,
                copyIds = group.map { it.id }.filter { it != main.id },
                groupMinutes = group.sumOf { it.playtimeMinutes + it.trackedMinutes + it.manualMinutes },
                groupLastPlayed = group.mapNotNull { it.lastPlayed }.maxOrNull(),
                groupSources = group.map { it.source }.distinct(),
            )
        }
        return out
    }
}

/** Herramientas y programas que no son juegos (editores, kits de mods, servidores...). */
object Tools {
    private val patterns = listOf(
        "dedicated server", "mod kit", "modkit", "dev kit", "devkit", "editor", "sdk", "soundtrack", "artbook",
        "art book", "benchmark", "resource archiver", "toolkit", "creation kit", "map maker",
        "wallpaper", "unreal engine", "playtest", "test server", "pre-game editor",
    ).map { Regex("""(^|[^a-z])${Regex.escape(it)}([^a-z]|$)""") } +
        // Paquetes de contenido que no son el juego: "Death Stranding Content", "... Bonus Content".
        Regex("""\s(bonus |digital |extra )?content\s*$""")

    fun looksLikeTool(title: String): Boolean {
        val t = title.lowercase()
        return patterns.any { it.containsMatchIn(t) }
    }
}
