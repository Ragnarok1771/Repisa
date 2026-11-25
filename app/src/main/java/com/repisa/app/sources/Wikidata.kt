package com.repisa.app.sources

import com.repisa.app.data.Http
import com.repisa.app.data.arr
import com.repisa.app.data.get
import com.repisa.app.data.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

/**
 * Wikidata: base de datos libre (la de Wikipedia) con género, estudio y fecha de casi cualquier videojuego,
 * de cualquier consola. Sin cuenta ni clave. Se usa para los juegos de emulador que no traen esos datos.
 *
 * Por juego: 1) buscar el título, 2) leer los candidatos y quedarse con el videojuego de nombre más parecido,
 * 3) traducir los identificadores de género y estudio a texto (con caché).
 */
object Wikidata {
    data class Facts(val genres: List<String>, val developer: String?, val publisher: String?, val year: Int?,
                     /** Identificador («Q...») y nombre del juego en inglés y en español. */
                     val id: String? = null, val titles: List<String> = emptyList(),
                     /** Número del juego en la tienda de Steam (P1733), si lo tiene: sirve aunque ya no se venda. */
                     val steamAppId: Long? = null)

    private const val API = "https://www.wikidata.org/w/api.php"
    private const val UA = "Repisa/1.0 (Android game library; contact via GitHub)"
    /** Clases de Wikidata que son un videojuego (videojuego, remake, expansión, edición...). */
    private val GAME_CLASSES = setOf("Q7889", "Q65963104", "Q209163", "Q21125433", "Q1066707", "Q64170203", "Q111223304")

    private val labels = HashMap<String, String>()
    /** Si Wikidata sigue pidiendo esperar tras los reintentos, se deja en paz un rato (no se insiste juego tras juego). */
    @Volatile private var pausedUntil = 0L
    private var lastCall = 0L

    private suspend fun call(params: Map<String, String>): JsonElement = withContext(Dispatchers.IO) {
        val url = API.toHttpUrl().newBuilder().apply {
            params.forEach { (k, v) -> addQueryParameter(k, v) }
            addQueryParameter("format", "json")
        }.build()
        if (System.currentTimeMillis() < pausedUntil) error("Wikidata en pausa")
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        // Si Wikidata pide esperar (429 con «Retry-After»), se espera lo que diga y se reintenta (dos veces como mucho).
        // Otros errores de red o de límite se lanzan para que el juego se vuelva a intentar más tarde.
        repeat(2) { attempt ->
            // Peticiones espaciadas: Wikidata pide no saturar su API.
            val wait = 700 - (System.currentTimeMillis() - lastCall)
            if (wait > 0) delay(wait)
            lastCall = System.currentTimeMillis()
            val (code, retry, body) = Http.client.newCall(req).execute().use { r ->
                Triple(r.code, r.header("Retry-After")?.toLongOrNull(), if (r.isSuccessful) r.body?.string().orEmpty() else null)
            }
            if (body != null) return@withContext Http.json.parseToJsonElement(body)
            if (code == 429 && attempt == 1) pausedUntil = System.currentTimeMillis() + 10 * 60_000L
            if (code != 429 || attempt == 1) error("Wikidata $code")
            delay(((retry ?: 5L).coerceIn(2L, 15L)) * 1000)
        }
        error("Wikidata")
    }

    private fun claimIds(entity: JsonElement?, prop: String): List<String> =
        entity?.get("claims")?.get(prop).arr.mapNotNull { it["mainsnak"]["datavalue"]["value"]["id"].str }

    private fun years(entity: JsonElement?): List<Int> =
        entity?.get("claims")?.get("P577").arr.mapNotNull {
            it["mainsnak"]["datavalue"]["value"]["time"].str?.let { t -> Regex("""[+-](\d{4})""").find(t)?.groupValues?.get(1)?.toIntOrNull() }
        }

    suspend fun facts(title: String): Facts? {
        val clean = EmulatorSource.cleanTitle(title).trim().ifEmpty { return null }
        // Los nombres de archivo usan " - " donde el título lleva ":" ("Persona 2 - Eternal Punishment").
        val queries = listOf(clean, clean.replace(" - ", ": ")).distinct()
        // Primero en inglés; luego en español (Cocoon y las ROMs traducidas usan «Pokémon Mundo Misterioso»...);
        // con letras japonesas, coreanas o chinas, también en ese idioma.
        for (lang in listOf("en", "es") + asianLanguages(clean)) for (query in queries) {
            factsFor(query, lang)?.let { return it }
        }
        return null
    }

    private fun compact(t: String) = CoverSearch.norm(EmulatorSource.cleanTitle(t)).filter { it.isLetterOrDigit() }

    private fun asianLanguages(t: String): List<String> = buildList {
        if (t.any { Character.UnicodeScript.of(it.code).let { s -> s == Character.UnicodeScript.HIRAGANA || s == Character.UnicodeScript.KATAKANA } }) add("ja")
        if (t.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HANGUL }) add("ko")
        if (t.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }) { add("ja"); add("zh") }
    }.distinct()

    /**
     * Género del juego original de una remasterización o edición que Wikidata no tiene
     * («Tomak: Save the Earth Regeneration 25th Anniversary» → «Tomak: Save the Earth - Love Story»).
     * Se acorta el nombre palabra a palabra (mínimo 3) y vale un videojuego cuyo nombre contenga todo lo buscado.
     * Solo da géneros: no es el mismo juego, así que no se une como copia.
     */
    suspend fun originalGenres(title: String): List<String> {
        // Se respetan los dos puntos: el buscador de Wikidata compara el principio del nombre («Tomak: Save the Earth»).
        val words = EmulatorSource.cleanTitle(title).split(Regex("\\s+")).filter { it.isNotBlank() }
        var tries = 0
        for (n in words.size - 1 downTo 3) {
            val query = words.take(n).joinToString(" ")
            if (CoverSearch.tokens(query).size < 3) break
            if (++tries > 4) break
            factsFor(query, "en", containing = true)?.genres?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return emptyList()
    }

    private suspend fun factsFor(query: String, lang: String, containing: Boolean = false): Facts? {
        val found = call(mapOf("action" to "wbsearchentities", "search" to query, "language" to lang,
            "uselang" to lang, "type" to "item", "limit" to "7"))["search"].arr
        if (found.isEmpty()) return null
        val wanted = CoverSearch.tokens(query)
        // Parecido con el nombre principal o con el nombre alternativo por el que lo encontró la búsqueda.
        val scoreById = found.mapNotNull { r ->
            val id = r["id"].str ?: return@mapNotNull null
            val names = listOfNotNull(r["label"].str, r["match"]["text"].str)
            id to (names.maxOfOrNull { n ->
                // Igual sin espacios (los títulos japoneses no separan palabras): «ペルソナ3 アイギス» = «ペルソナ3アイギス».
                if (compact(n) == compact(query)) 1.0
                else if (containing) (if (CoverSearch.tokens(n).containsAll(wanted)) 1.0 else 0.0)
                else CoverSearch.similarity(wanted, CoverSearch.tokens(n))
            } ?: 0.0)
        }.filter { it.second >= 0.8 }
        if (scoreById.isEmpty()) return null
        val entities = call(mapOf("action" to "wbgetentities", "ids" to scoreById.joinToString("|") { it.first },
            "props" to "claims|labels", "languages" to (listOf("en", "es", lang).distinct().joinToString("|"))))["entities"] as? JsonObject ?: return null

        val bestId = scoreById.filter { (id, _) -> entities[id]?.let { e -> claimIds(e, "P31").any { it in GAME_CLASSES } } == true }
            .maxByOrNull { it.second }?.first ?: return null
        val best = entities[bestId]

        var genreIds = claimIds(best, "P136")
        // Sin género en su ficha: el de la obra de la que es versión (P629), su serie (P179) o en la que se basa (P144).
        // («Red Rescue Team» no tiene género, pero «Pokémon Mystery Dungeon» sí).
        if (genreIds.isEmpty()) {
            val related = (claimIds(best, "P629") + claimIds(best, "P179") + claimIds(best, "P144")).distinct().take(5)
            if (related.isNotEmpty()) {
                val rel = call(mapOf("action" to "wbgetentities", "ids" to related.joinToString("|"), "props" to "claims"))["entities"] as? JsonObject
                genreIds = related.firstNotNullOfOrNull { id -> claimIds(rel?.get(id), "P136").ifEmpty { null } }.orEmpty()
            }
        }
        val devIds = claimIds(best, "P178").take(2)
        val pubIds = claimIds(best, "P123").take(1)
        val missing = (genreIds + devIds + pubIds).filter { it !in labels }.distinct()
        if (missing.isNotEmpty()) {
            val got = call(mapOf("action" to "wbgetentities", "ids" to missing.take(50).joinToString("|"),
                "props" to "labels", "languages" to "en"))["entities"] as? JsonObject
            got?.forEach { (id, e) -> e["labels"]["en"]["value"].str?.let { labels[id] = it } }
        }
        return Facts(
            genres = genreIds.mapNotNull { labels[it] },
            developer = devIds.mapNotNull { labels[it] }.joinToString(", ").ifEmpty { null },
            publisher = pubIds.firstNotNullOfOrNull { labels[it] },
            year = years(best).mapNotNull { Years.valid(it) }.minOrNull(),
            id = bestId,
            titles = listOf("en", "es", lang).distinct().mapNotNull { best?.get("labels")?.get(it)?.get("value")?.str }.distinct(),
            steamAppId = best?.get("claims")?.get("P1733").arr.firstNotNullOfOrNull { it["mainsnak"]["datavalue"]["value"].str?.toLongOrNull() },
        )
    }
}
