package com.repisa.app.sources

import com.repisa.app.data.Http
import com.repisa.app.data.Settings
import com.repisa.app.data.arr
import com.repisa.app.data.get
import com.repisa.app.data.long
import com.repisa.app.data.str
import com.repisa.app.model.Game
import com.repisa.app.model.Genres
import com.repisa.app.model.Source
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneOffset

/**
 * Completa portadas, géneros, estudio y año de los juegos a los que les falta algo.
 *  - Steam: ficha de la tienda.
 *  - Emuladores: portadas de libretro-thumbnails (gratis, sin cuenta).
 *  - Todos (opcional): IGDB, si el usuario añadió sus credenciales de Twitch.
 */
class Metadata(private val settings: Settings) {

    private var igdbToken: String? = null
    private var igdbTokenExpiry = 0L

    private val igdbEnabled get() = settings.igdbClientId != null && settings.igdbClientSecret != null

    fun needsWork(g: Game): Boolean = !g.metaFetched && !g.userEdited &&
        (g.coverUrl == null || g.genres.isEmpty() || g.developer == null)

    suspend fun enrich(g: Game): Game {
        var out = g
        if (g.source == Source.STEAM && (g.genres.isEmpty() || g.developer == null)) {
            runCatching { SteamSource.storeDetails(g) }.getOrNull()?.let { out = it }
            delay(1600) // límite de la tienda de Steam
            // Si no hay carátula vertical, mejor dejar que IGDB la busque que estirar la cabecera horizontal.
            if (out.coverUrl != null && !Http.exists(out.coverUrl!!)) {
                out = out.copy(coverUrl = if (igdbEnabled) null else out.altCoverUrl)
            }
        }
        if (g.source == Source.EMULATOR && out.coverUrl == null && settings.useLibretroCovers) {
            libretroCover(out)?.let { out = out.copy(coverUrl = it) }
        }
        if (igdbEnabled && (out.coverUrl == null || out.genres.isEmpty() || out.developer == null)) {
            runCatching { igdb(out) }.getOrNull()?.let { out = it }
            delay(300) // IGDB permite 4 peticiones por segundo
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

    // ---------------------------------------------------------------- IGDB

    private suspend fun igdbAuth(): String {
        igdbToken?.takeIf { System.currentTimeMillis() < igdbTokenExpiry }?.let { return it }
        val url = "https://id.twitch.tv/oauth2/token?client_id=${settings.igdbClientId}" +
            "&client_secret=${settings.igdbClientSecret}&grant_type=client_credentials"
        val r = Http.getJson(Request.Builder().url(url).post(ByteArray(0).toRequestBody()).build())
        igdbToken = r["access_token"].str ?: error("Credenciales de IGDB no válidas")
        igdbTokenExpiry = System.currentTimeMillis() + ((r["expires_in"].long ?: 3600) - 60) * 1000
        return igdbToken!!
    }

    private suspend fun igdb(g: Game): Game? {
        val token = igdbAuth()
        val q = g.title.replace("\"", "").replace(Regex("""[™®©]"""), "")
        val body = """search "$q"; fields name,genres.name,themes.name,involved_companies.company.name,""" +
            """involved_companies.developer,involved_companies.publisher,cover.image_id,first_release_date,""" +
            """platforms.name,summary; limit 8;"""
        val req = Request.Builder().url("https://api.igdb.com/v4/games")
            .header("Client-ID", settings.igdbClientId!!)
            .header("Authorization", "Bearer $token")
            .post(body.toRequestBody("text/plain".toMediaType()))
            .build()
        val results = Http.getJson(req).arr
        if (results.isEmpty()) return null
        // Preferir el que coincide en nombre y en plataforma.
        val wantedPlatform = g.console.name.lowercase().split(" / ").first()
        val best = results.maxBy { r ->
            var score = 0
            if (r["name"].str.equals(g.title, ignoreCase = true)) score += 10
            if (r["platforms"].arr.any { (it["name"].str ?: "").lowercase().contains(wantedPlatform) }) score += 5
            if (r["cover"] != null) score += 1
            score
        }
        val companies = best["involved_companies"].arr
        val dev = companies.firstOrNull { it["developer"].str == "true" }?.get("company")?.get("name").str
        val pub = companies.firstOrNull { it["publisher"].str == "true" }?.get("company")?.get("name").str
        val genres = Genres.normalize(
            best["genres"].arr.mapNotNull { it["name"].str } + best["themes"].arr.mapNotNull { it["name"].str }
                .filter { it in setOf("Horror", "Survival", "Open world") }
        )
        val year = best["first_release_date"].long?.let { Instant.ofEpochSecond(it).atZone(ZoneOffset.UTC).year }
        val cover = best["cover"]["image_id"].str?.let { "https://images.igdb.com/igdb/image/upload/t_cover_big_2x/$it.jpg" }
        return g.copy(
            genres = g.genres.ifEmpty { genres },
            developer = g.developer ?: dev,
            publisher = g.publisher ?: pub,
            releaseYear = g.releaseYear ?: year,
            description = g.description ?: best["summary"].str,
            coverUrl = g.coverUrl ?: cover,
        )
    }
}
