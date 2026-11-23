package com.repisa.app.sources

import android.content.Context
import com.repisa.app.data.Http
import com.repisa.app.data.Settings
import com.repisa.app.data.arr
import com.repisa.app.data.get
import com.repisa.app.data.long
import com.repisa.app.data.str
import com.repisa.app.model.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder

/** Una carátula encontrada en algún servidor. */
data class CoverResult(val url: String, val thumb: String, val source: String, val label: String)

/**
 * Buscador de carátulas, como el de Cocoon. Consulta varios servidores:
 *  - libretro-thumbnails: escaneos de cajas reales de consolas (sin cuenta). Se descarga la lista de
 *    nombres de cada sistema y se busca por parecido, así no hace falta que el ROM se llame exactamente igual.
 *  - Tienda de Steam: carátula vertical oficial de la versión de PC, si existe (sin cuenta).
 *  - SteamGridDB: miles de carátulas de cualquier plataforma (necesita una clave gratuita).
 *  - IGDB: carátula oficial (necesita credenciales de Twitch).
 */
class CoverSearch(private val context: Context, private val settings: Settings) {

    // ------------------------------------------------------------------ Búsqueda manual

    /** Resultado de una búsqueda: carátulas y servidores que no respondieron. */
    data class Outcome(val results: List<CoverResult>, val failed: List<String>)

    suspend fun search(game: Game, query: String): Outcome = coroutineScope {
        val q = query.trim().ifEmpty { game.title }
        val failed = java.util.Collections.synchronizedList(mutableListOf<String>())
        suspend fun safe(name: String, block: suspend () -> List<CoverResult>) =
            try { block() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { failed += name; emptyList() }
        val jobs = listOf(
            async { safe("libretro") { libretro(game, q, 12) } },
            async { safe("SteamGridDB") { steamGridDb(q, 12) } },
            async { safe("Steam") { steamStore(q, 6) } },
            async { safe("IGDB") { igdb(q, 6) } },
        )
        // libretro primero si es un juego de consola; en PC, Steam/SteamGridDB primero.
        val (lr, sg, st, ig) = jobs.awaitAll()
        Outcome(if (game.consoleId == "pc") sg + st + ig + lr else lr + sg + ig + st, failed.toList())
    }

    val hasSteamGridDb get() = settings.steamGridDbKey != null

    // ------------------------------------------------------------------ Búsqueda automática

    /** Mejor carátula para un juego sin intervención del usuario, o null. Solo acepta coincidencias claras. */
    suspend fun auto(game: Game): String? {
        if (game.consoleId != "pc") {
            runCatching { libretro(game, game.romName ?: game.title, 1, minScore = 0.72) }.getOrNull()
                ?.firstOrNull()?.let { return it.url }
        }
        runCatching { steamGridDb(game.title, 1, exactOnly = true) }.getOrNull()?.firstOrNull()?.let { return it.url }
        runCatching { igdb(game.title, 1, exactOnly = true) }.getOrNull()?.firstOrNull()?.let { return it.url }
        runCatching { steamStore(game.title, 1, exactOnly = true) }.getOrNull()?.firstOrNull()?.let { return it.url }
        return null
    }

    // ------------------------------------------------------------------ libretro-thumbnails

    private val listMutex = Mutex()

    /** Nombres de todas las carátulas de un sistema (lista cacheada 14 días). */
    private suspend fun libretroNames(repo: String): List<String> = listMutex.withLock {
        withContext(Dispatchers.IO) {
            val cache = File(context.cacheDir, "libretro/$repo.txt")
            if (cache.exists() && System.currentTimeMillis() - cache.lastModified() < 14L * 86_400_000) {
                return@withContext cache.readLines()
            }
            // 1) árbol raíz para encontrar la carpeta Named_Boxarts; 2) su contenido (sin recursión, cabe entero).
            val api = "https://api.github.com/repos/libretro-thumbnails/$repo/git/trees"
            val root = Http.getJson(Request.Builder().url("$api/master")
                .header("Accept", "application/vnd.github+json").build())
            val sha = root["tree"].arr.firstOrNull { it["path"].str == "Named_Boxarts" }?.get("sha").str
                ?: return@withContext emptyList()
            val names = Http.getJson(Request.Builder().url("$api/$sha").header("Accept", "application/vnd.github+json").build())["tree"]
                .arr.mapNotNull { it["path"].str?.takeIf { p -> p.endsWith(".png") }?.removeSuffix(".png") }
            cache.parentFile?.mkdirs()
            cache.writeText(names.joinToString("\n"))
            names
        }
    }

    private suspend fun libretro(game: Game, query: String, limit: Int, minScore: Double = 0.35): List<CoverResult> {
        val system = game.console.libretro ?: return emptyList()
        val repo = system.replace(' ', '_')
        val names = libretroNames(repo)
        val wanted = tokens(query)
        if (wanted.isEmpty()) return emptyList()
        return names.asSequence()
            .map { it to similarity(wanted, tokens(it)) + regionBonus(it) }
            .filter { it.second >= minScore }
            .sortedByDescending { it.second }
            .take(limit)
            .map { (name, _) ->
                val url = "https://raw.githubusercontent.com/libretro-thumbnails/$repo/master/Named_Boxarts/${enc(name)}.png"
                CoverResult(url, url, "libretro", name)
            }.toList()
    }

    // ------------------------------------------------------------------ SteamGridDB

    private suspend fun steamGridDb(query: String, limit: Int, exactOnly: Boolean = false): List<CoverResult> {
        val key = settings.steamGridDbKey ?: return emptyList()
        fun req(url: String) = Request.Builder().url(url).header("Authorization", "Bearer $key").build()
        val base = "https://www.steamgriddb.com/api/v2"
        val games = Http.getJson(req("$base/search/autocomplete/${enc(query)}"))["data"].arr
            .filter { !exactOnly || norm(it["name"].str.orEmpty()) == norm(query) }
            .take(3)
        return games.flatMap { g ->
            val id = g["id"].long ?: return@flatMap emptyList()
            Http.getJson(req("$base/grids/game/$id?dimensions=600x900,342x482,660x930&types=static&nsfw=false&humor=false"))["data"]
                .arr.mapNotNull { grid ->
                    val url = grid["url"].str ?: return@mapNotNull null
                    CoverResult(url, grid["thumb"].str ?: url, "SteamGridDB", g["name"].str.orEmpty())
                }
        }.take(limit)
    }

    // ------------------------------------------------------------------ Tienda de Steam

    private suspend fun steamStore(query: String, limit: Int, exactOnly: Boolean = false): List<CoverResult> {
        val url = "https://store.steampowered.com/api/storesearch/".toHttpUrl().newBuilder()
            .addQueryParameter("term", query).addQueryParameter("l", "spanish").addQueryParameter("cc", "ES").build()
        val items = Http.getJson(url.toString())["items"].arr
            .filter { !exactOnly || norm(it["name"].str.orEmpty()) == norm(query) }
            .take(limit)
        if (items.isEmpty()) return emptyList()
        val ids = items.mapNotNull { it["id"].long }
        val covers = SteamSource.libraryCovers(ids)
        return items.mapNotNull { item ->
            val id = item["id"].long ?: return@mapNotNull null
            val cover = covers[id] ?: return@mapNotNull null
            CoverResult(cover, cover, "Steam", item["name"].str.orEmpty())
        }
    }

    // ------------------------------------------------------------------ IGDB

    private var igdbToken: String? = null
    private var igdbExpiry = 0L

    private suspend fun igdb(query: String, limit: Int, exactOnly: Boolean = false): List<CoverResult> {
        val id = settings.igdbClientId ?: return emptyList()
        val secret = settings.igdbClientSecret ?: return emptyList()
        if (igdbToken == null || System.currentTimeMillis() > igdbExpiry) {
            val r = Http.getJson(Request.Builder()
                .url("https://id.twitch.tv/oauth2/token?client_id=$id&client_secret=$secret&grant_type=client_credentials")
                .post(ByteArray(0).toRequestBody()).build())
            igdbToken = r["access_token"].str
            igdbExpiry = System.currentTimeMillis() + ((r["expires_in"].long ?: 3600) - 60) * 1000
        }
        val body = """search "${query.replace("\"", "")}"; fields name,cover.image_id,platforms.abbreviation; limit 10;"""
        val results = Http.getJson(Request.Builder().url("https://api.igdb.com/v4/games")
            .header("Client-ID", id).header("Authorization", "Bearer $igdbToken")
            .post(body.toRequestBody("text/plain".toMediaType())).build()).arr
        return results
            .filter { !exactOnly || norm(it["name"].str.orEmpty()) == norm(query) }
            .mapNotNull { r ->
                val img = r["cover"]["image_id"].str ?: return@mapNotNull null
                val platforms = r["platforms"].arr.mapNotNull { it["abbreviation"].str }.take(3).joinToString(", ")
                CoverResult(
                    "https://images.igdb.com/igdb/image/upload/t_cover_big_2x/$img.jpg",
                    "https://images.igdb.com/igdb/image/upload/t_cover_big/$img.jpg",
                    "IGDB", r["name"].str.orEmpty() + if (platforms.isNotEmpty()) " ($platforms)" else "",
                )
            }.take(limit)
    }

    // ------------------------------------------------------------------ Parecido de nombres

    companion object {
        private val tagRegex = Regex("""[(\[][^)\]]*[)\]]""")
        private val stop = setOf("the", "a", "an", "of", "el", "la", "los", "las", "de", "del")

        fun norm(s: String): String = s.lowercase()
            .replace(tagRegex, " ")
            .replace("&", " and ")
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()

        /** Palabras clave del título (sin números de lista, versiones, regiones ni relleno). */
        fun tokens(s: String): Set<String> = norm(EmulatorSource.cleanTitle(s)).split(' ')
            .filter { it.isNotEmpty() && it !in stop }
            .map { romanToArabic(it) }.toSet()

        private fun romanToArabic(t: String): String = when (t) {
            "ii" -> "2"; "iii" -> "3"; "iv" -> "4"; "v" -> "5"; "vi" -> "6"; "vii" -> "7"; "viii" -> "8"
            "ix" -> "9"; "x" -> "10"; "xi" -> "11"; "xii" -> "12"; else -> t
        }

        /**
         * Parecido entre el nombre buscado [a] y un candidato [b], de 0 a 1. Mezcla:
         *  - cuántas palabras comparten (Dice), y
         *  - si todas las palabras del candidato aparecen en el nombre buscado (así "Bravely Default 2 eShop Update"
         *    encuentra "Bravely Default II").
         * Penaliza números distintos (Final Fantasy VII ≠ VIII, Kingdom Hearts 358-2 ≠ Kingdom Hearts 2).
         */
        fun similarity(a: Set<String>, b: Set<String>): Double {
            if (a.isEmpty() || b.isEmpty()) return 0.0
            val inter = a.intersect(b).size
            val dice = 2.0 * inter / (a.size + b.size)
            val coverage = inter.toDouble() / b.size
            var score = 0.5 * dice + 0.5 * coverage
            val numsA = a.filter { it.all(Char::isDigit) }.toSet()
            val numsB = b.filter { it.all(Char::isDigit) }.toSet()
            if (numsA != numsB) score -= 0.3
            return score
        }

        /** Preferir la versión occidental de la caja. */
        fun regionBonus(name: String): Double = when {
            "(USA" in name || "(Europe" in name || "(World" in name -> 0.02
            "(Spain" in name -> 0.03
            else -> 0.0
        }

        private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
    }
}
