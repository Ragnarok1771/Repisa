package com.repisa.app.sources

import android.content.Context
import android.util.Xml
import com.repisa.app.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.zip.ZipInputStream

/**
 * GameTDB (gametdb.com): base de datos libre de juegos de Nintendo con carátulas escaneadas,
 * sin cuenta ni clave. Cubre Switch, 3DS, DS, Wii, GameCube y Wii U.
 *
 * Se descarga una vez la lista de juegos de cada sistema (id, región, título), se guarda
 * comprimida en caché y se busca por palabras clave. La carátula se pide por id:
 *   https://art.gametdb.com/switch/coverHQ/US/AXN7A.jpg
 */
class GameTdb(private val context: Context) {

    data class Entry(
        val id: String, val region: String, val title: String, val type: String,
        val developer: String? = null, val year: Int? = null, val genre: String? = null, val synopsis: String? = null,
    )

    private data class Platform(val db: String, val art: String, val idFilter: (String) -> Boolean)

    private val platforms = mapOf(
        "switch" to Platform("switchtdb", "switch") { true },
        "3ds" to Platform("3dstdb", "3ds") { true },
        "nds" to Platform("dstdb", "ds") { true },
        "wii" to Platform("wiitdb", "wii") { it.first() in "RS" },
        "gc" to Platform("wiitdb", "wii") { it.first() in "GD" },
        "wiiu" to Platform("wiiutdb", "wiiu") { true },
    )

    fun supports(consoleId: String) = consoleId in platforms

    private val digitalTypes = setOf("CUSTOM", "eShop", "3DSWare", "DSiWare", "WiiWare", "Channel", "Homebrew")

    private val mutex = Mutex()
    private val memory = mutableMapOf<String, List<Entry>>()

    private suspend fun entries(db: String): List<Entry> = mutex.withLock {
        memory[db]?.let { return@withLock it }
        val list = withContext(Dispatchers.IO) {
            val cache = File(context.cacheDir, "gametdb/$db.v2.tsv")
            val fresh = cache.exists() && System.currentTimeMillis() - cache.lastModified() < 30L * 86_400_000
            if (!fresh) runCatching { download(db, cache) }
            if (!cache.exists()) return@withContext emptyList()
            cache.readLines().mapNotNull { line ->
                val p = line.split('\t')
                if (p.size >= 4) Entry(p[0], p[1], p[2], p[3],
                    p.getOrNull(4)?.ifBlank { null }, p.getOrNull(5)?.toIntOrNull(),
                    p.getOrNull(6)?.ifBlank { null }, p.getOrNull(7)?.ifBlank { null }) else null
            }
        }
        if (list.isNotEmpty()) memory[db] = list
        list
    }

    /** Descarga el zip de GameTDB y guarda id, región, título en inglés, tipo, estudio, año, género y sinopsis. */
    private fun download(db: String, cache: File) {
        val req = Request.Builder().url("https://www.gametdb.com/$db.zip?LANG=EN").build()
        Http.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("GameTDB ${resp.code}")
            val out = StringBuilder()
            ZipInputStream(resp.body!!.byteStream()).use { zip ->
                var e = zip.nextEntry
                while (e != null && !e.name.endsWith(".xml")) e = zip.nextEntry
                if (e == null) error("zip sin XML")
                val parser = Xml.newPullParser()
                parser.setInput(zip, "UTF-8")
                var id: String? = null; var region = ""; var type = ""; var title: String? = null
                var dev = ""; var genre = ""; var synopsis = ""; var year = 9999
                var inGame = false; var lang: String? = null; var tag: String? = null
                while (true) {
                    when (parser.next()) {
                        XmlPullParser.START_TAG -> {
                            tag = parser.name
                            when (parser.name) {
                                "game" -> { inGame = true; id = null; region = ""; type = ""; title = null
                                    dev = ""; genre = ""; synopsis = ""; year = 9999 }
                                "locale" -> lang = parser.getAttributeValue(null, "lang")
                                else -> if (parser.name.startsWith("date")) {
                                    // <date year=".."/>, <date_JPN year=".."/>...: el año más antiguo
                                    parser.getAttributeValue(null, "year")?.toIntOrNull()?.let { if (it in 1970 until year) year = it }
                                }
                            }
                        }
                        XmlPullParser.TEXT -> if (inGame) {
                            val t = parser.text.trim()
                            if (t.isNotEmpty()) when (tag) {
                                "id" -> if (id == null) id = t
                                "region" -> region = t
                                "type" -> type = t
                                "title" -> if (lang == "EN" || title == null) title = t
                                "developer" -> dev = t
                                "genre" -> genre = t
                                "synopsis" -> if (lang == "EN" || synopsis.isEmpty()) synopsis = t
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "game") {
                                inGame = false
                                val i = id; val ti = title
                                fun clean(v: String) = v.replace(Regex("[\\t\\r\\n]+"), " ").trim()
                                if (i != null && ti != null) out.append(i).append('\t').append(region).append('\t')
                                    .append(clean(ti)).append('\t').append(type).append('\t').append(clean(dev)).append('\t')
                                    .append(if (year == 9999) "" else year.toString()).append('\t').append(clean(genre))
                                    .append('\t').append(clean(synopsis).take(500)).append('\n')
                            }
                            if (parser.name == "locale") lang = null
                            tag = null
                        }
                        XmlPullParser.END_DOCUMENT -> break
                    }
                }
            }
            cache.parentFile?.mkdirs()
            cache.writeText(out.toString())
        }
    }

    /** Juegos que más se parecen a [query], descartando demos, ediciones caseras y similares. */
    suspend fun find(consoleId: String, query: String, limit: Int, minScore: Double): List<Pair<Entry, Double>> {
        val p = platforms[consoleId] ?: return emptyList()
        val wanted = CoverSearch.tokens(query)
        if (wanted.isEmpty()) return emptyList()
        return entries(p.db).asSequence()
            // Fuera demos, ediciones caseras y versiones solo digitales: estas últimas tienen en GameTDB una
            // portada genérica (fondo rojo con "Nintendo eShop"), no una carátula de verdad.
            .filter { p.idFilter(it.id) && it.type !in digitalTypes && !it.type.startsWith("VC") && !it.title.contains("demo", true) }
            .map { e ->
                val regionBonus = when (e.region) { "NTSC-U", "ALL" -> 0.03; "PAL" -> 0.02; else -> 0.0 }
                e to CoverSearch.similarity(wanted, CoverSearch.tokens(e.title)) + regionBonus
            }
            .filter { it.second >= minScore }
            .sortedByDescending { it.second }
            .distinctBy { it.first.title.lowercase() } // una por título (la de mejor región)
            .take(limit)
            .toList()
    }

    /** Estudio, año, géneros y sinopsis del juego que más se parece (para juegos de emulador sin datos). */
    suspend fun facts(consoleId: String, title: String): Entry? {
        val p = platforms[consoleId] ?: return null
        val wanted = CoverSearch.tokens(title)
        if (wanted.isEmpty()) return null
        val all = entries(p.db)
        if (all.isEmpty()) error("GameTDB no disponible") // sin red: se reintentará
        return all.asSequence()
            .filter { p.idFilter(it.id) && !it.title.contains("demo", true) }
            .map { it to CoverSearch.similarity(wanted, CoverSearch.tokens(it.title)) }
            .filter { it.second >= 0.85 }
            // a igualdad de parecido, la entrada con más datos
            .maxByOrNull { it.second + (if (it.first.developer != null) 0.01 else 0.0) + (if (it.first.genre != null) 0.01 else 0.0) }
            ?.first
    }

    /** Primera URL de carátula que exista para ese juego (prueba calidad y región). */
    suspend fun coverUrl(consoleId: String, e: Entry): String? {
        val art = platforms[consoleId]?.art ?: return null
        val regions = when (e.region) {
            "NTSC-J" -> listOf("JA", "US", "EN")
            "PAL" -> listOf("ES", "EN", "US")
            "NTSC-K" -> listOf("KO", "US", "EN")
            else -> listOf("US", "EN", "ES")
        }
        for (kind in listOf("coverHQ", "coverM", "cover")) for (r in regions) {
            val ext = if (kind == "cover") "png" else "jpg"
            val url = "https://art.gametdb.com/$art/$kind/$r/${e.id}.$ext"
            if (Http.exists(url)) return url
        }
        return null
    }
}
