package com.repisa.app.sources

import com.repisa.app.data.Http
import com.repisa.app.data.Settings
import com.repisa.app.model.Game
import com.repisa.app.model.Genres
import com.repisa.app.model.Source
import kotlinx.coroutines.delay
import java.net.URLEncoder

/**
 * Completa portadas, géneros, estudio y año de los juegos a los que les falta algo.
 *  - Steam: ficha de la tienda.
 *  - Emuladores: portadas de libretro-thumbnails (gratis, sin cuenta).
 *  - Si aún falta la carátula: buscador (GameTDB, libretro por parecido, SteamGridDB, tienda de Steam).
 */
class Metadata(private val settings: Settings, private val covers: CoverSearch, private val tdb: GameTdb) {

    fun needsWork(g: Game): Boolean = (!g.metaFetched && !g.userEdited && !g.customCover &&
        (g.coverUrl == null || g.genres.isEmpty() || g.developer == null)) || needsFacts(g)

    /** Juego de emulador sin género, estudio o año al que aún no se le han buscado. */
    private fun needsFacts(g: Game) = g.source == Source.EMULATOR && !g.factsFetched && !g.userEdited &&
        (g.genres.isEmpty() || g.developer == null || g.releaseYear == null)

    /**
     * Género, estudio, año y descripción para juegos de emulador: GameTDB (Nintendo, con sinopsis) y, para lo que
     * falte o para el resto de consolas, Wikidata. Solo rellena lo que está vacío.
     */
    private suspend fun facts(g: Game): Game {
        var out = g
        // Si una consulta falla por la red, no se marca como hecho: se reintenta en la próxima pasada.
        var complete = true
        if (tdb.supports(g.consoleId)) {
            val r = runCatching { tdb.facts(g.consoleId, g.title) }
            if (r.isFailure) complete = false
            r.getOrNull()?.let { e ->
                out = out.copy(
                    genres = out.genres.ifEmpty { Genres.normalize(listOfNotNull(e.genre)) },
                    developer = out.developer ?: e.developer,
                    releaseYear = out.releaseYear ?: e.year,
                    description = out.description ?: e.synopsis,
                )
            }
        }
        if (out.genres.isEmpty() || out.developer == null || out.releaseYear == null) {
            val r = runCatching { Wikidata.facts(g.title) }
            if (r.isFailure) complete = false
            r.getOrNull()?.let { f ->
                out = out.copy(
                    genres = out.genres.ifEmpty { Genres.normalize(f.genres) },
                    developer = out.developer ?: f.developer,
                    publisher = out.publisher ?: f.publisher,
                    releaseYear = out.releaseYear ?: f.year,
                )
            }
        }
        return out.copy(factsFetched = complete)
    }

    suspend fun enrich(g: Game): Game {
        if (g.metaFetched && needsFacts(g)) return facts(g) // solo faltan los datos del juego
        var out = g
        if (needsFacts(g)) out = facts(out)
        if (g.source == Source.STEAM && (g.genres.isEmpty() || g.developer == null)) {
            runCatching { SteamSource.storeDetails(g) }.getOrNull()?.let { out = it }
            delay(1600) // límite de la tienda de Steam
            // Si no hay carátula vertical, que la busque el buscador (abajo) en vez de estirar la cabecera.
            if (out.coverUrl != null && !Http.exists(out.coverUrl!!)) out = out.copy(coverUrl = null)
        }
        if (g.source == Source.EMULATOR && out.coverUrl == null && settings.useLibretroCovers) {
            libretroCover(out)?.let { out = out.copy(coverUrl = it) }
        }
        // Último recurso: buscador de carátulas (libretro por parecido, SteamGridDB, tienda de Steam).
        if (out.coverUrl == null) {
            runCatching { covers.auto(out) }.getOrNull()?.let { out = out.copy(coverUrl = it, coverAspect = null) }
        }
        return out.copy(metaFetched = true)
    }

    // ---------------------------------------------------------------- libretro-thumbnails

    /**
     * Carátulas escaneadas de las cajas reales, del proyecto libretro-thumbnails (repositorios en GitHub,
     * uno por sistema: "Sony - PlayStation" → libretro-thumbnails/Sony_-_PlayStation).
     */
    private suspend fun libretroCover(g: Game): String? {
        val system = g.console.libretro ?: return null
        val repo = system.replace(' ', '_')
        val names = buildList {
            g.romName?.let { add(it) }
            // Nombre No-Intro con el artículo al final: "Legend of Zelda, The - ..."
            val clean = Regex("""^(The|A|An) (.*?)( - .*)?$""").find(g.title)
                ?.let { m -> "${m.groupValues[2]}, ${m.groupValues[1]}${m.groupValues[3]}" } ?: g.title
            listOf(clean, g.title).distinct().forEach { t ->
                listOf(" (USA)", " (Europe)", " (World)", " (USA, Europe)", " (Japan, USA)", " (Spain)", " (Japan)", "")
                    .forEach { add(t + it) }
            }
            g.romName?.let { add(it.replace(Regex("""\s*\((Disc|Disk|CD) \d+\)""", RegexOption.IGNORE_CASE), "") + " (Disc 1)") }
        }.distinct()
        for (n in names) {
            // libretro sustituye estos caracteres por "_" en los nombres de archivo.
            val file = n.replace(Regex("""[&*/:`<>?\\|"]"""), "_")
            val url = "https://raw.githubusercontent.com/libretro-thumbnails/$repo/master/Named_Boxarts/${enc(file)}.png"
            if (Http.exists(url)) return url
        }
        return null
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
