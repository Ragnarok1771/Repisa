package com.repisa.app.sources

import com.repisa.app.data.Http
import com.repisa.app.data.arr
import com.repisa.app.data.get
import com.repisa.app.data.long
import com.repisa.app.data.str
import com.repisa.app.model.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

/**
 * Datos externos de un juego que se muestran en su ficha:
 *  - Metacritic: nota de la prensa (0-100) y de los usuarios (0-10).
 *  - HowLongToBeat: horas de la historia principal, con extras y para completarlo al 100 %.
 * Ninguna de las dos webs tiene API pública; se lee lo mismo que muestra su web.
 */
object GameInfo {
    private const val UA = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"

    data class Metacritic(val metascore: Int?, val userScore: Double?, val url: String)
    data class Hltb(val main: Float?, val extra: Float?, val complete: Float?, val url: String)

    private suspend fun get(url: String, referer: String? = null): Pair<Int, String> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).header("User-Agent", UA).apply { referer?.let { header("Referer", it) } }.build()
        Http.client.newCall(req).execute().use { it.code to (it.body?.string().orEmpty()) }
    }

    // ------------------------------------------------------------------ Metacritic

    /** "The Legend of Zelda: Tears of the Kingdom" → "the-legend-of-zelda-tears-of-the-kingdom" */
    fun slug(title: String): String = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFD)
        .replace(Regex("""\p{Mn}+"""), "")
        .lowercase()
        .replace("&", "and")
        .replace(Regex("""['’.]"""), "")
        .replace(Regex("""[^a-z0-9]+"""), "-")
        .trim('-')

    suspend fun metacritic(game: Game): Metacritic? {
        val title = EmulatorSource.cleanTitle(game.title)
        var url = "https://www.metacritic.com/game/${slug(title)}/"
        var (code, html) = get(url)
        if (code != 200 || !html.contains("Metascore")) {
            // Buscar y quedarse con el resultado cuyo nombre más se parezca.
            val (_, search) = get("https://www.metacritic.com/search/${URLEncoder.encode(title, "UTF-8").replace("+", "%20")}/?category=13")
            val wanted = CoverSearch.tokens(title)
            val best = Regex("""href="/game/([a-z0-9-]+)/"""").findAll(search).map { it.groupValues[1] }.distinct()
                .map { it to CoverSearch.similarity(wanted, CoverSearch.tokens(it.replace('-', ' '))) }
                .filter { it.second >= 0.6 }.maxByOrNull { it.second }?.first ?: return null
            url = "https://www.metacritic.com/game/$best/"
            val r = get(url); code = r.first; html = r.second
            if (code != 200) return null
        }
        // La primera nota de la página es la del juego (las siguientes son de juegos relacionados).
        val meta = Regex("""title="Metascore (\d+) out of 100"""").find(html)?.groupValues?.get(1)?.toIntOrNull()
        val user = Regex("""title="User score ([\d.]+) out of 10"""").find(html)?.groupValues?.get(1)?.toDoubleOrNull()
        if (meta == null && user == null) return null
        return Metacritic(meta, user, url)
    }

    // ------------------------------------------------------------------ HowLongToBeat

    private var hltbToken: String? = null

    private suspend fun hltbInit(): String? {
        val (code, body) = get("https://howlongtobeat.com/api/search/site/init?t=${System.currentTimeMillis()}", "https://howlongtobeat.com/")
        if (code != 200) return null
        return runCatching { Http.json.parseToJsonElement(body)["token"].str }.getOrNull().also { hltbToken = it }
    }

    suspend fun hltb(game: Game): Hltb? {
        val title = EmulatorSource.cleanTitle(game.title)
        val terms = title.replace(Regex("""[:™®©]"""), " ").split(Regex("""\s+""")).filter { it.isNotBlank() }
        val empty = JsonObject(mapOf("mode" to JsonPrimitive("include"), "values" to JsonArray(emptyList())))
        val body = JsonObject(mapOf(
            "searchType" to JsonPrimitive("games"),
            "searchTerms" to JsonArray(terms.map { JsonPrimitive(it) }),
            "searchPage" to JsonPrimitive(1),
            "size" to JsonPrimitive(20),
            "searchOptions" to JsonObject(mapOf(
                "games" to JsonObject(mapOf(
                    "userId" to JsonPrimitive(0), "platform" to empty, "sortCategory" to JsonPrimitive("popular"),
                    "rangeCategory" to JsonPrimitive("main"),
                    "rangeTime" to JsonObject(mapOf("min" to JsonNull, "max" to JsonNull)),
                    "gameplay" to JsonObject(mapOf("perspective" to empty, "flow" to empty, "genre" to empty)),
                    "year" to empty, "modifier" to JsonPrimitive(""),
                )),
                "users" to JsonObject(mapOf("sortCategory" to JsonPrimitive("postcount"))),
                "lists" to JsonObject(mapOf("sortCategory" to JsonPrimitive("follows"))),
                "filter" to JsonPrimitive(""), "sort" to JsonPrimitive(0), "randomizer" to JsonPrimitive(0),
            )),
            "useCache" to JsonPrimitive(true),
        )).toString()

        suspend fun search(token: String): Pair<Int, String> = withContext(Dispatchers.IO) {
            val req = Request.Builder().url("https://howlongtobeat.com/api/search/site")
                .header("User-Agent", UA).header("Referer", "https://howlongtobeat.com/").header("Origin", "https://howlongtobeat.com")
                .header("x-auth-token", token)
                .post(body.toRequestBody("application/json".toMediaType())).build()
            Http.client.newCall(req).execute().use { it.code to (it.body?.string().orEmpty()) }
        }

        var token = hltbToken ?: hltbInit() ?: return null
        var (code, resp) = search(token)
        if (code == 403 || code == 401) { // token caducado: pedir otro una vez
            token = hltbInit() ?: return null
            val r = search(token); code = r.first; resp = r.second
        }
        if (code != 200) return null
        val wanted = CoverSearch.tokens(title)
        val best = Http.json.parseToJsonElement(resp)["data"].arr
            .map { it to CoverSearch.similarity(wanted, CoverSearch.tokens(it["game_name"].str.orEmpty())) }
            .filter { it.second >= 0.6 }
            .maxByOrNull { it.second }?.first ?: return null
        fun hours(key: String) = best[key].long?.takeIf { it > 0 }?.let { it / 3600f }
        return Hltb(hours("comp_main"), hours("comp_plus"), hours("comp_100"),
            "https://howlongtobeat.com/game/${best["game_id"].long}")
    }
}
