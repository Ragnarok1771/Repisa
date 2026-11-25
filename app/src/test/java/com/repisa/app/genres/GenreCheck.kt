package com.repisa.app.genres

import com.repisa.app.model.Game
import com.repisa.app.model.Genres
import com.repisa.app.model.Source
import com.repisa.app.sources.Achievements
import com.repisa.app.sources.SteamSource
import com.repisa.app.sources.Wikidata
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Revisión de géneros con juegos reales (usa la red). Solo se ejecuta con -Dgenres.check=1.
 * Para cada juego: appid de Steam por el nombre → etiquetas → géneros; si no, Wikidata.
 */
class GenreCheck {
    private val games = listOf(
        Source.EPIC to "Trackmania", Source.EPIC to "The Textorcist: The Story of Ray Bibbia", Source.EPIC to "Batman - The Telltale Series",
        Source.EPIC to "Batman: The Enemy Within - The Telltale Series", Source.EPIC to "Chivalry 2", Source.STEAM to "steam:0|Chivalry 2 Public Testing",
        Source.EPIC to "Dragon Age™: Inquisition – Game of the Year Edition", Source.EMULATOR to "Dragon Quest Builders 2",
        Source.STEAM to "steam:0|G-MODE Archives+ Persona 3 Aigis The First Mission", Source.EPIC to "Nightingale Legacy Mode",
        Source.EPIC to "Rustler Grand Theft Horse", Source.EPIC to "Rocket League®", Source.EPIC to "[REDACTED]",
        Source.EPIC to "Tomak: Save the Earth Regeneration 25th Anniversary",
    )

    /** La búsqueda por nombre de Steam que usa la app (sugerencias de la tienda), juego a juego. */
    @Test fun byName() = runBlocking {
        if (System.getProperty("genres.check") == null) return@runBlocking
        games.forEach { (_, t) ->
            val title = t.substringAfter('|')
            val m = runCatching { SteamSource.searchByName(title) }.getOrElse { println("  error: ${it.message}"); null }
            println("%-55s → %s".format(title, m?.let { "${it.genres} (Steam ${it.appId})" } ?: "no encontrado"))
        }
    }

    @Test fun check() = runBlocking {
        if (System.getProperty("genres.check") == null) return@runBlocking
        val only = System.getProperty("genres.only")
        val titleOnly = System.getProperty("genres.title")
        games.filter { (only == null || it.first.name == only) && (titleOnly == null || it.second.contains(titleOnly)) }.forEach { (src, t) ->
            val (id, title) = if (t.contains('|')) t.split('|').let { it[0] to it[1] } else "x:$t" to t
            val g = Game(id, src, title, if (src == Source.EMULATOR) "switch" else "pc")
            var how = ""
            var genres: List<String>? = null
            var titles: List<String> = emptyList()
            if (src != Source.EMULATOR) {
                val app = runCatching { Achievements.steamAppId(g, loose = true) }.getOrNull()
                app?.let { genres = SteamSource.tagGenres(listOf(it.toLong())).genres[it.toLong()]; how = "Steam $it" }
            }
            if (genres.isNullOrEmpty()) {
                kotlinx.coroutines.delay(1500)
                val w = runCatching { Wikidata.facts(title) }.getOrElse { println("  wikidata: ${it.message}"); null }
                titles = w?.titles.orEmpty()
                w?.let { genres = Genres.normalize(it.genres); how = "Wikidata ${it.id} ${it.genres} steam=${it.steamAppId}" }
                if (genres.isNullOrEmpty()) w?.steamAppId?.let { genres = SteamSource.tagGenres(listOf(it)).genres[it]; how += " → etiquetas" }
            }
            if (genres.isNullOrEmpty()) for (n in (listOf(title) + titles).distinct()) {
                val app = runCatching { Achievements.steamAppId(g.copy(title = n), loose = true) }.getOrNull() ?: continue
                genres = SteamSource.tagGenres(listOf(app.toLong())).genres[app.toLong()]; how = "Steam por nombre «$n» $app"; break
            }
            if (genres.isNullOrEmpty()) runCatching { Wikidata.originalGenres(title) }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { genres = Genres.normalize(it); how = "juego original en Wikidata $it" }
            println("%-55s → %s   [%s] títulos=%s".format(title, genres ?: "SIN GÉNERO", how, titles))
        }
    }
}
