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

    /**
     * Juego (de cualquier tienda o emulador) sin género, estudio o año al que aún no se le han buscado; o de emulador
     * sin identidad en Wikidata (para reconocerlo con su título en otro idioma). Los editados a mano solo si no
     * tienen género.
     */
    fun needsFacts(g: Game) = !g.factsFetched && (if (g.userEdited) g.genres.isEmpty()
        else g.genres.isEmpty() || g.developer == null || g.releaseYear == null || needsIdentity(g))

    private fun needsIdentity(g: Game) = g.source == Source.EMULATOR && g.wikidataId == null && !g.idChecked

    /**
     * Género, estudio, año y descripción, rellenando solo lo que falta:
     *  - Steam y Epic: etiquetas de la comunidad de Steam (géneros precisos: «JRPG», «FPS», «Souls-like»...)
     *    y la ficha de la tienda (estudio, año, descripción). Epic, con el mismo juego en Steam.
     *  - Emuladores: GameTDB (Nintendo, con sinopsis) y Wikidata, que además da su nombre en inglés y en español.
     *  - Si aún no hay género: el mismo juego en Steam, buscado por cualquiera de sus nombres.
     * Si una consulta falla por la red o por límite de peticiones, no se marca como hecho: se reintenta más tarde.
     */
    private suspend fun facts(g: Game): Game {
        var out = g
        var complete = true
        fun missing() = out.genres.isEmpty() || out.developer == null || out.releaseYear == null
        fun fill(genres: List<String>, dev: String?, pub: String?, year: Int?, desc: String?) {
            out = out.copy(
                genres = out.genres.ifEmpty { Genres.normalize(genres) },
                developer = out.developer ?: dev?.takeIf { it.isNotBlank() },
                publisher = out.publisher ?: pub?.takeIf { it.isNotBlank() },
                releaseYear = out.releaseYear ?: Years.valid(year),
                description = out.description ?: desc,
            )
        }
        /** Géneros por las etiquetas de Steam del juego con ese appid (si aún no tiene). */
        suspend fun tagsOf(appId: Long) {
            if (out.genres.isNotEmpty()) return
            val r = SteamSource.tagGenres(listOf(appId))
            if (r.failed) complete = false
            r.genres[appId]?.let { out = out.copy(genres = it) }
        }
        /** El mismo juego en Steam por su nombre (Epic, emuladores, o un juego de Steam sin ficha: betas, archivos). */
        suspend fun steamByName(names: List<String>): Long? {
            if (out.steamChecked) { out.steamMatch?.let { tagsOf(it) }; return out.steamMatch }
            for (name in names.distinct()) {
                val r = runCatching { SteamSource.searchByName(name) }
                if (r.isFailure) { complete = false; return null }
                r.getOrNull()?.let { m ->
                    out = out.copy(steamMatch = m.appId, steamChecked = true)
                    if (out.genres.isEmpty() && m.genres.isNotEmpty()) out = out.copy(genres = m.genres)
                    return m.appId
                }
            }
            out = out.copy(steamChecked = true)
            return null
        }
        val title = g.originalTitle ?: g.title
        val store = g.source == Source.STEAM || g.source == Source.EPIC
        if (store) {
            val appId = if (g.source == Source.STEAM) g.id.removePrefix("steam:").toLongOrNull() else steamByName(listOf(title))
            appId?.let { id ->
                tagsOf(id)
                if (missing() || out.description == null) {
                    val f = runCatching { SteamSource.appFacts(id.toString()) }
                    delay(1600) // límite de la tienda de Steam
                    if (f.isFailure) complete = false
                    f.getOrNull()?.let { fill(it.genres, it.developer, it.publisher, it.year, it.description) }
                }
            }
            // Un juego de Steam sin etiquetas (beta, «archivos», retirado...): el juego principal del mismo nombre.
            if (g.source == Source.STEAM && out.genres.isEmpty()) steamByName(listOf(title))?.takeIf { it != appId }?.let { tagsOf(it) }
        }
        if (g.source == Source.EMULATOR && tdb.supports(g.consoleId) && missing()) {
            val r = runCatching { tdb.facts(g.consoleId, title) }
            if (r.isFailure) complete = false
            r.getOrNull()?.let { e -> fill(listOfNotNull(e.genre), e.developer, null, e.year, e.synopsis) }
        }
        if (missing() || needsIdentity(out)) {
            val r = runCatching { Wikidata.facts(title) }
            if (r.isFailure) complete = false
            else out = out.copy(idChecked = true)
            r.getOrNull()?.let { f ->
                // Wikidata sabe su número de Steam aunque la tienda ya no lo venda (Rocket League en Epic):
                // sus etiquetas, más precisas, van primero; el género de Wikidata queda de respaldo.
                f.steamAppId?.let { id -> tagsOf(id); if (out.steamMatch == null && g.source != Source.STEAM) out = out.copy(steamMatch = id, steamChecked = true) }
                fill(f.genres, f.developer, f.publisher, f.year, null)
                out = out.copy(wikidataId = out.wikidataId ?: f.id, altTitles = out.altTitles.ifEmpty { f.titles })
            }
        }
        // Sin género todavía: el mismo juego en Steam, por su nombre o por su nombre en inglés («Dragon Quest Builders 2»).
        if (out.genres.isEmpty() && g.source == Source.EMULATOR) steamByName(listOf(title) + out.altTitles)?.let { tagsOf(it) }
        // Último recurso: el juego original de una remasterización o edición («Tomak ... Regeneration»).
        if (out.genres.isEmpty()) {
            val r = runCatching { Wikidata.originalGenres(title) }
            if (r.isFailure) complete = false
            r.getOrNull()?.let { if (it.isNotEmpty()) out = out.copy(genres = Genres.normalize(it)) }
        }
        return out.copy(factsFetched = complete)
    }

    suspend fun enrich(g: Game): Game {
        // Editado a mano: solo se le busca el género (si no tiene); nada más se toca.
        if (g.userEdited) return (if (needsFacts(g)) facts(g) else g).copy(metaFetched = true)
        if (g.metaFetched && needsFacts(g)) return facts(g) // solo faltan los datos del juego
        var out = g
        if (needsFacts(g)) out = facts(out)
        if (g.source == Source.STEAM && !g.metaFetched) {
            // Si no hay carátula vertical, que la busque el buscador (abajo) en vez de estirar la cabecera.
            if (out.coverUrl != null && Http.definitelyMissing(out.coverUrl!!)) out = out.copy(coverUrl = null)
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
