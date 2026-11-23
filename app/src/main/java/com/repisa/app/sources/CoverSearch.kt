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
 */
class CoverSearch(private val context: Context, private val settings: Settings) {

    private val gameTdb = GameTdb(context)

    // ------------------------------------------------------------------ Búsqueda manual

    /** Resultado de una búsqueda: carátulas y servidores que no respondieron. */
    data class Outcome(val results: List<CoverResult>, val failed: List<String>)

    suspend fun search(game: Game, query: String): Outcome = coroutineScope {
        val q = query.trim().ifEmpty { game.title }
        val failed = java.util.Collections.synchronizedList(mutableListOf<String>())
        suspend fun safe(name: String, block: suspend () -> List<CoverResult>) =
            try { block() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { failed += name; emptyList() }
        val jobs = listOf(
            async { safe("GameTDB") { gameTdbResults(game, q, 8) } + safe("libretro") { libretro(game, q, 12) } },
            async { safe("SteamGridDB") { steamGridDb(q, 12) } },
            async { safe("Steam") { steamStore(q, 6) } },
        )
        // libretro primero si es un juego de consola; en PC, Steam/SteamGridDB primero.
        val (lr, sg, st) = jobs.awaitAll()
        Outcome(if (game.consoleId == "pc") sg + st + lr else lr + sg + st, failed.toList())
    }

    val hasSteamGridDb get() = settings.steamGridDbKey != null

    // ------------------------------------------------------------------ Búsqueda automática

    /** Mejor carátula para un juego sin intervención del usuario, o null. Solo acepta coincidencias claras. */
    suspend fun auto(game: Game): String? {
        // Nintendo (Switch, 3DS, DS, Wii, GameCube, Wii U): GameTDB, sin clave y por id exacto.
        if (gameTdb.supports(game.consoleId)) {
            for (name in listOfNotNull(game.title, game.romName).distinct()) {
                val best = runCatching { gameTdb.find(game.consoleId, name, 1, minScore = 0.72) }.getOrNull()?.firstOrNull()
                if (best != null) runCatching { gameTdb.coverUrl(game.consoleId, best.first) }.getOrNull()?.let { return it }
            }
        }
        if (game.consoleId != "pc") {
            runCatching { libretro(game, game.romName ?: game.title, 1, minScore = 0.72) }.getOrNull()
                ?.firstOrNull()?.let { return it.url }
        }
        val title = EmulatorSource.cleanTitle(game.title)
        runCatching { steamGridDb(title, 1, minSim = 0.8) }.getOrNull()?.firstOrNull()?.let { return it.url }
        runCatching { steamStore(title, 1, minSim = 0.85) }.getOrNull()?.firstOrNull()?.let { return it.url }
        return null
    }

    // ------------------------------------------------------------------ GameTDB

    private suspend fun gameTdbResults(game: Game, query: String, limit: Int): List<CoverResult> {
        if (!gameTdb.supports(game.consoleId)) return emptyList()
        return coroutineScope {
            gameTdb.find(game.consoleId, query, limit, minScore = 0.35).map { (e, _) ->
                async {
                    gameTdb.coverUrl(game.consoleId, e)?.let { CoverResult(it, it, "GameTDB", "${e.title} (${e.region})") }
                }
            }.awaitAll().filterNotNull()
        }
    }

    // ------------------------------------------------------------------ libretro-thumbnails

    /** Si la API de GitHub falla (p. ej. límite de 60 peticiones/hora), no reintentar durante un rato. */
    @Volatile private var githubBlockedUntil = 0L

    private val listMutex = Mutex()

    /** Nombres de todas las carátulas de un sistema (lista cacheada 14 días). */
    private suspend fun libretroNames(repo: String): List<String> = listMutex.withLock {
        withContext(Dispatchers.IO) {
            val cache = File(context.cacheDir, "libretro/$repo.txt")
            if (cache.exists() && System.currentTimeMillis() - cache.lastModified() < 14L * 86_400_000) {
                return@withContext cache.readLines()
            }
            if (System.currentTimeMillis() < githubBlockedUntil) {
                return@withContext if (cache.exists()) cache.readLines() else emptyList()
            }
            try {
                runCatching { fetchLibretroNames(repo, cache) }.getOrNull()?.takeIf { it.isNotEmpty() }
                    ?: fetchLibretroListing(repo.replace('_', ' '), cache)
            } catch (e: Exception) {
                githubBlockedUntil = System.currentTimeMillis() + 20 * 60_000L
                if (cache.exists()) cache.readLines() else throw e
            }
        }
    }

    /** Respaldo: listado HTML del servidor de libretro (no tiene límite de peticiones). */
    private suspend fun fetchLibretroListing(system: String, cache: File): List<String> = withContext(Dispatchers.IO) {
        val url = "https://thumbnails.libretro.com/${enc(system)}/Named_Boxarts/"
        val html = Http.client.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) error("libretro ${r.code}"); r.body?.string().orEmpty()
        }
        val names = Regex("""href="([^"?/][^"]*?)\.png"""").findAll(html)
            .map { java.net.URLDecoder.decode(it.groupValues[1], "UTF-8") }.toList()
        if (names.isEmpty()) error("listado vacío")
        cache.parentFile?.mkdirs()
        cache.writeText(names.joinToString("\n"))
        names
    }

    /** 1) árbol raíz del repositorio para encontrar Named_Boxarts; 2) su contenido (sin recursión, cabe entero). */
    private suspend fun fetchLibretroNames(repo: String, cache: File): List<String> {
        val api = "https://api.github.com/repos/libretro-thumbnails/$repo/git/trees"
        fun req(url: String) = Request.Builder().url(url).header("Accept", "application/vnd.github+json").build()
        val sha = Http.getJson(req("$api/master"))["tree"].arr
            .firstOrNull { it["path"].str == "Named_Boxarts" }?.get("sha").str ?: return emptyList()
        val names = Http.getJson(req("$api/$sha"))["tree"].arr
            .mapNotNull { it["path"].str?.takeIf { p -> p.endsWith(".png") }?.removeSuffix(".png") }
        cache.parentFile?.mkdirs()
        cache.writeText(names.joinToString("\n"))
        return names
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

    /** Parecido entre lo buscado y un nombre devuelto por un servidor (para filtrar en la búsqueda automática). */
    private fun close(query: String, name: String, minSim: Double) =
        minSim <= 0.0 || similarity(tokens(query), tokens(name)) >= minSim

    private suspend fun steamGridDb(query: String, limit: Int, minSim: Double = 0.0): List<CoverResult> {
        val key = settings.steamGridDbKey ?: return emptyList()
        fun req(url: String) = Request.Builder().url(url).header("Authorization", "Bearer $key").build()
        val base = "https://www.steamgriddb.com/api/v2"
        val games = Http.getJson(req("$base/search/autocomplete/${enc(query)}"))["data"].arr
            .filter { close(query, it["name"].str.orEmpty(), minSim) }
            .sortedByDescending { if (minSim > 0) similarity(tokens(query), tokens(it["name"].str.orEmpty())) else 0.0 }
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

    private suspend fun steamStore(query: String, limit: Int, minSim: Double = 0.0): List<CoverResult> {
        val url = "https://store.steampowered.com/api/storesearch/".toHttpUrl().newBuilder()
            .addQueryParameter("term", query).addQueryParameter("l", "spanish").addQueryParameter("cc", "ES").build()
        val items = Http.getJson(url.toString())["items"].arr
            .filter { close(query, it["name"].str.orEmpty(), minSim) }
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

    // ------------------------------------------------------------------ Parecido de nombres

    companion object {
        private val tagRegex = Regex("""[(\[][^)\]]*[)\]]""")
        private val stop = setOf("the", "a", "an", "of", "el", "la", "los", "las", "de", "del")

        fun norm(s: String): String = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "") // sin acentos: Pokémon = Pokemon
            .lowercase()
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
            val queryCoverage = inter.toDouble() / a.size
            // También vale que todo lo buscado esté en el candidato aunque este tenga palabras de más
            // ("Dragon Ball Z Kakarot" → "Dragon Ball Z: Kakarot + A New Power Awakens Set").
            var score = maxOf(0.5 * dice + 0.5 * coverage, 0.5 * dice + 0.5 * queryCoverage - 0.1)
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
