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
    data class Facts(val genres: List<String>, val developer: String?, val publisher: String?, val year: Int?)

    private const val API = "https://www.wikidata.org/w/api.php"
    private const val UA = "Repisa/1.0 (Android game library; contact via GitHub)"
    /** Clases de Wikidata que son un videojuego (videojuego, remake, expansión, edición...). */
    private val GAME_CLASSES = setOf("Q7889", "Q65963104", "Q209163", "Q21125433", "Q1066707", "Q64170203", "Q111223304")

    private val labels = HashMap<String, String>()
    private var lastCall = 0L

    private suspend fun call(params: Map<String, String>): JsonElement = withContext(Dispatchers.IO) {
        // Peticiones espaciadas: Wikidata pide no saturar su API.
        val wait = 400 - (System.currentTimeMillis() - lastCall)
        if (wait > 0) delay(wait)
        lastCall = System.currentTimeMillis()
        val url = API.toHttpUrl().newBuilder().apply {
            params.forEach { (k, v) -> addQueryParameter(k, v) }
            addQueryParameter("format", "json")
        }.build()
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        // Los errores de red o de límite (429) se lanzan para que el juego se vuelva a intentar más tarde.
        Http.client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("Wikidata ${r.code}")
            Http.json.parseToJsonElement(r.body?.string().orEmpty())
        }
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
        for (query in listOf(clean, clean.replace(" - ", ": ")).distinct()) {
            factsFor(query)?.let { return it }
        }
        return null
    }

    private suspend fun factsFor(query: String): Facts? {
        val found = call(mapOf("action" to "wbsearchentities", "search" to query, "language" to "en",
            "type" to "item", "limit" to "7"))["search"].arr
        if (found.isEmpty()) return null
        val ids = found.mapNotNull { it["id"].str }
        val entities = call(mapOf("action" to "wbgetentities", "ids" to ids.joinToString("|"),
            "props" to "claims|labels", "languages" to "en"))["entities"] as? JsonObject ?: return null

        val wanted = CoverSearch.tokens(query)
        val best = ids.mapNotNull { id ->
            val e = entities[id] ?: return@mapNotNull null
            if (claimIds(e, "P31").none { it in GAME_CLASSES }) return@mapNotNull null
            val label = e["labels"]["en"]["value"].str ?: return@mapNotNull null
            Triple(id, e, CoverSearch.similarity(wanted, CoverSearch.tokens(label)))
        }.filter { it.third >= 0.8 }.maxByOrNull { it.third }?.second ?: return null

        val genreIds = claimIds(best, "P136")
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
            year = years(best).minOrNull(),
        )
    }
}
