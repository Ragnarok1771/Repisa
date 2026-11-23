package com.repisa.app.sources

import com.repisa.app.data.Http
import com.repisa.app.data.Settings
import com.repisa.app.model.Game
import com.repisa.app.model.Source
import kotlinx.coroutines.delay
import java.net.URLEncoder

/**
 * Completa portadas, géneros, estudio y año de los juegos a los que les falta algo.
 *  - Steam: ficha de la tienda.
 *  - Emuladores: portadas de libretro-thumbnails (gratis, sin cuenta).
 *  - Si aún falta la carátula: buscador (GameTDB, libretro por parecido, SteamGridDB, tienda de Steam).
 */
class Metadata(private val settings: Settings, private val covers: CoverSearch) {

    fun needsWork(g: Game): Boolean = !g.metaFetched && !g.userEdited && !g.customCover &&
        (g.coverUrl == null || g.genres.isEmpty() || g.developer == null)

    suspend fun enrich(g: Game): Game {
        var out = g
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
