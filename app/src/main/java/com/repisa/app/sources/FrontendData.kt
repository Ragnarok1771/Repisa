package com.repisa.app.sources

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Xml
import com.repisa.app.data.Http
import com.repisa.app.data.SafTree
import com.repisa.app.model.Console
import com.repisa.app.model.Consoles
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

/** Datos de un juego que un frontend (ES-DE, Cocoon, Batocera...) tiene guardados. */
internal data class FrontendMeta(
    val name: String?, val genre: String?, val developer: String?, val publisher: String?,
    val year: Int?, val desc: String?, val lastPlayed: Long?, val cover: String?,
    /** Tiempo de juego registrado por el frontend, en minutos. */
    val playMinutes: Int?,
) {
    fun combine(b: FrontendMeta) = FrontendMeta(
        name = name ?: b.name, genre = genre ?: b.genre, developer = developer ?: b.developer,
        publisher = publisher ?: b.publisher, year = year ?: b.year, desc = desc ?: b.desc,
        lastPlayed = listOfNotNull(lastPlayed, b.lastPlayed).maxOrNull(), cover = cover ?: b.cover,
        playMinutes = listOfNotNull(playMinutes, b.playMinutes).maxOrNull(),
    )
}

/**
 * Un juego leído de un frontend. [consoleId] puede faltar (p.ej. en una copia de seguridad sin sistema):
 * entonces se empareja solo por nombre.
 */
internal class FrontendEntry(
    val consoleId: String?,
    /** Nombre del archivo del juego, como [EmulatorSource.romKey]. */
    val fileKey: String?,
    /** Palabras del título (ver [CoverSearch.tokens]). */
    val nameTokens: Set<String>,
    var meta: FrontendMeta,
) {
    var used = false
}

/** Qué se encontró en las carpetas de frontends; se muestra en Ajustes para saber si funcionó. */
data class FrontendReport(
    val lists: Int = 0,
    val systems: Set<String> = emptySet(),
    val entries: Int = 0,
    val withGenre: Int = 0,
    val withTime: Int = 0,
    val backupFiles: Int = 0,
    val backupWithTime: Int = 0,
    var matched: Int = 0,
    var matchedWithTime: Int = 0,
    val at: Long = System.currentTimeMillis(),
) {
    fun encode() = listOf(lists, systems.joinToString(","), entries, withGenre, withTime, backupFiles,
        backupWithTime, matched, matchedWithTime, at).joinToString("|")

    companion object {
        fun decode(s: String?): FrontendReport? = runCatching {
            val p = s!!.split('|')
            FrontendReport(p[0].toInt(), p[1].split(',').filter { it.isNotEmpty() }.toSet(), p[2].toInt(), p[3].toInt(),
                p[4].toInt(), p[5].toInt(), p[6].toInt(), p[7].toInt(), p[8].toInt(), p[9].toLong())
        }.getOrNull()
    }
}

/**
 * Lee lo que un frontend ha dejado en una carpeta, sea cual sea su estructura:
 *  - gamelist.xml (ES-DE, exportación de Cocoon, Batocera, RetroBat...): en gamelists/<sistema>/,
 *    en <sistema>/gamelist.xml, como <sistema>.xml dentro de "gamelists"...
 *    El sistema se saca de la carpeta, del nombre del archivo, de la etiqueta <system> o de las extensiones.
 *  - Copias de seguridad (JSON, SQLite o .zip con ellos): se buscan registros con nombre y tiempo de juego.
 */
internal class FrontendReader(private val context: Context, private val tree: SafTree) {

    private val skipDirs = setOf("media", "images", "videos", "manuals", "downloaded_media", "screenshots",
        "snaps", "boxart", "covers", "3dboxes", "marquees", "fanart", "titlescreens", "miniatures", "physicalmedia",
        "saves", "states", "bios", "system", "cheats", "textures", "imgs", "themes", "sounds", "music", "cache",
        "shaders", "artwork", "wallpapers")

    fun read(): Pair<List<FrontendEntry>, FrontendReport> {
        val lists = mutableListOf<Pair<SafTree.Doc, SafTree.Doc>>() // carpeta, xml
        val ancestors = HashMap<String, List<String>>()
        val backups = mutableListOf<SafTree.Doc>()
        var visited = 0
        fun walk(dir: SafTree.Doc, depth: Int, path: List<String>) {
            if (++visited > 6000) return
            val children = tree.children(dir)
            for (c in children) {
                if (c.isDir) continue
                val n = c.name.lowercase()
                val inGamelists = dir.name.equals("gamelists", true) || path.any { it.equals("gamelists", true) }
                when {
                    n.endsWith(".xml") && (n.startsWith("gamelist") || inGamelists) -> {
                        lists += dir to c; ancestors[c.id] = path
                    }
                    n.endsWith(".json") || n.endsWith(".db") || n.endsWith(".sqlite") || n.endsWith(".sqlite3") ||
                        n.endsWith(".zip") && (n.contains("backup") || n.contains("cocoon") ||
                            (path + dir.name).any { it.contains("backup", true) }) -> backups += c
                }
            }
            if (depth < 8) children.filter { it.isDir && !it.name.startsWith(".") && it.name.lowercase() !in skipDirs }
                .forEach { walk(it, depth + 1, path + dir.name) }
        }
        walk(tree.root, 0, emptyList())

        val entries = mutableListOf<FrontendEntry>()
        val systems = mutableSetOf<String>()
        val media = tree.path("downloaded_media")
        for ((dir, xmlDoc) in lists) {
            val xml = tree.readText(xmlDoc) ?: continue
            val games = runCatching { parseGamelist(xml) }.getOrNull() ?: continue
            val console = consoleFor(dir, xmlDoc, ancestors[xmlDoc.id].orEmpty(), xml, games)
            console?.let { systems += it.id }
            // Portadas descargadas por ES-DE: downloaded_media/<sistema>/covers/
            val covers: Map<String, String> = media?.let { m ->
                val sysName = if (dir.name.equals("gamelists", true)) xmlDoc.name.substringBeforeLast('.') else dir.name
                val sysMedia = tree.child(m, sysName) ?: return@let null
                val coverDir = tree.child(sysMedia, "covers") ?: tree.child(sysMedia, "3dboxes") ?: return@let null
                tree.children(coverDir).associate { EmulatorSource.romKey(it.name.substringBeforeLast('.')) to it.uri.toString() }
            } ?: emptyMap()
            for (f in games) {
                val path = f["path"]
                val fileKey = path?.let { fileKeyOf(it) }
                val name = f["name"] ?: f["title"]
                val meta = FrontendMeta(
                    name = name,
                    genre = f["genre"] ?: f["genres"] ?: f["category"],
                    developer = f["developer"],
                    publisher = f["publisher"],
                    year = (f["releasedate"] ?: f["release"] ?: f["year"])?.take(4)?.toIntOrNull()?.takeIf { it > 1950 },
                    desc = f["desc"] ?: f["description"],
                    lastPlayed = f["lastplayed"]?.let(::parseDate),
                    cover = fileKey?.let { covers[it] }
                        ?: (f["thumbnail"] ?: f["image"] ?: f["cover"] ?: f["boxart"])?.let { resolveRelative(dir, it) },
                    playMinutes = playtimeFields.firstNotNullOfOrNull { f[it] }?.let(EmulatorSource::parsePlaytime),
                )
                val tokens = CoverSearch.tokens(name ?: fileKey ?: continue)
                entries += FrontendEntry(console?.id, fileKey, tokens, meta)
            }
        }

        var backupWithTime = 0
        for (doc in backups) {
            val found = runCatching { readBackup(doc) }.getOrDefault(emptyList())
            backupWithTime += found.count { it.meta.playMinutes != null }
            entries += found
        }

        val report = FrontendReport(
            lists = lists.size,
            systems = systems,
            entries = entries.size,
            withGenre = entries.count { it.meta.genre != null },
            withTime = entries.count { (it.meta.playMinutes ?: 0) > 0 },
            backupFiles = backups.size,
            backupWithTime = backupWithTime,
        )
        return entries to report
    }

    // ------------------------------------------------------------------ gamelist.xml

    private fun consoleFor(dir: SafTree.Doc, xml: SafTree.Doc, path: List<String>, text: String,
                           games: List<Map<String, String>>): Console? {
        val fileBase = xml.name.substringBeforeLast('.').lowercase().removePrefix("gamelist").trim('_', '-', '.', ' ')
        return Consoles.byFolder(dir.name)
            ?: fileBase.takeIf { it.isNotEmpty() }?.let { Consoles.byFolder(it) ?: Consoles.guess(it) }
            ?: Regex("""<(system|platform)>([^<]+)</""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(2)
                ?.trim()?.let { Consoles.byFolder(it) ?: Consoles.guess(it) }
            ?: Consoles.guess(dir.name.replace('_', ' ').replace('-', ' '))
            ?: path.asReversed().firstNotNullOfOrNull { Consoles.byFolder(it) }
            ?: byExtensions(games.mapNotNull { it["path"] })
    }

    /** El sistema que más se repite por extensión, contando solo extensiones propias de un único sistema. */
    private fun byExtensions(paths: List<String>): Console? {
        val generic = setOf("zip", "7z", "iso", "bin", "chd", "cue", "m3u", "img", "rom")
        return paths.map { decode(it).substringAfterLast('.', "").lowercase() }
            .filter { it.isNotEmpty() && it !in generic }
            .mapNotNull { ext -> Consoles.all.filter { ext in it.extensions }.singleOrNull() }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    }

    private fun parseGamelist(xml: String): List<Map<String, String>> {
        val parser = Xml.newPullParser()
        parser.setInput(xml.reader())
        val result = mutableListOf<Map<String, String>>()
        var current: MutableMap<String, String>? = null
        var tag: String? = null
        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    val n = parser.name.lowercase()
                    if (n == "game") {
                        current = mutableMapOf()
                        // Algunos frontends guardan datos como atributos: <game id=".." playtime="..">
                        for (i in 0 until parser.attributeCount) current[parser.getAttributeName(i).lowercase()] = parser.getAttributeValue(i)
                    } else tag = n
                }
                XmlPullParser.TEXT -> {
                    val t = tag
                    if (current != null && t != null && parser.text.isNotBlank()) current[t] = parser.text.trim()
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("game", true)) { current?.let(result::add); current = null }
                    tag = null
                }
                XmlPullParser.END_DOCUMENT -> return result
            }
        }
    }

    /** Resuelve "./media/covers/x.png" relativo a la carpeta del gamelist. */
    private fun resolveRelative(base: SafTree.Doc, path: String): String? {
        if (path.startsWith("http") || path.startsWith("content://")) return path
        if (path.startsWith("/")) return null
        var cur: SafTree.Doc = base
        for (part in path.removePrefix("./").split('/').filter { it.isNotEmpty() && it != "." }) {
            cur = tree.child(cur, part) ?: return null
        }
        return cur.uri.toString()
    }

    // ------------------------------------------------------------------ Copias de seguridad

    private fun readBackup(doc: SafTree.Doc): List<FrontendEntry> {
        val n = doc.name.lowercase()
        val input = tree.open(doc) ?: return emptyList()
        return input.use { stream ->
            when {
                n.endsWith(".zip") -> readZip(stream)
                n.endsWith(".json") -> readJson(stream.readLimited() ?: return emptyList())
                else -> readSqlite(stream)
            }
        }
    }

    private fun readZip(stream: InputStream): List<FrontendEntry> {
        val out = mutableListOf<FrontendEntry>()
        ZipInputStream(stream).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                val n = e.name.lowercase()
                when {
                    e.isDirectory -> Unit
                    n.endsWith(".json") -> zip.readLimited()?.let { out += readJson(it) }
                    n.endsWith(".db") || n.endsWith(".sqlite") || n.endsWith(".sqlite3") -> out += readSqlite(zip)
                    n.endsWith(".xml") && n.contains("gamelist") -> Unit // poco habitual en copias; se ignora
                }
            }
        }
        return out
    }

    /** Lee como mucho 40 MB (una copia con miles de juegos ocupa bastante menos). */
    private fun InputStream.readLimited(): String? {
        val bytes = readNBytesCompat(40 * 1024 * 1024)
        return bytes?.toString(Charsets.UTF_8)
    }

    private fun InputStream.readNBytesCompat(max: Int): ByteArray? {
        val buf = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(64 * 1024)
        while (true) {
            val r = read(chunk)
            if (r < 0) break
            buf.write(chunk, 0, r)
            if (buf.size() > max) return null
        }
        return buf.toByteArray()
    }

    private fun readJson(text: String): List<FrontendEntry> {
        val root = runCatching { Http.json.parseToJsonElement(text) }.getOrNull() ?: return emptyList()
        val out = mutableListOf<FrontendEntry>()
        // Tiempo de sesiones sueltas: id del juego -> segundos (se suman al final).
        val sessionSeconds = HashMap<String, Double>()
        val byId = HashMap<String, FrontendEntry>()
        fun visit(e: JsonElement, depth: Int) {
            if (depth > 12) return
            when (e) {
                is JsonArray -> e.forEach { visit(it, depth + 1) }
                is JsonObject -> {
                    val keys = e.keys.associateBy { it.lowercase() }
                    fun prim(vararg names: String) = names.firstNotNullOfOrNull { k -> (e[keys[k] ?: return@firstNotNullOfOrNull null] as? JsonPrimitive) }
                    val name = prim("title", "name", "gametitle", "game_title", "displayname", "display_name")?.contentOrNullSafe()
                    val path = prim("path", "rompath", "rom_path", "filepath", "file_path", "uri", "romuri", "rom_uri", "filename", "file")?.contentOrNullSafe()
                    val time = keys.entries.firstOrNull { (k, _) -> isPlaytimeKey(k) }?.let { (k, orig) ->
                        (e[orig] as? JsonPrimitive)?.let { p -> toMinutes(k, p) }
                    }
                    val id = prim("id", "gameid", "game_id", "uuid", "romid", "rom_id")?.contentOrNullSafe()
                    if (name != null || path != null) {
                        val system = prim("platform", "system", "console", "systemname", "platformid", "platform_id")?.contentOrNullSafe()
                        val console = system?.let { Consoles.byFolder(it) ?: Consoles.guess(it) }
                        val fileKey = path?.let { fileKeyOf(it) }
                        val genre = prim("genre", "genres")?.contentOrNullSafe()
                            ?: (e[keys["genres"] ?: ""] as? JsonArray)?.firstOrNull()?.let { (it as? JsonPrimitive)?.contentOrNullSafe() }
                        val looksLikeGame = time != null || genre != null || fileKey != null
                        if (looksLikeGame) {
                            val entry = FrontendEntry(console?.id, fileKey, CoverSearch.tokens(name ?: fileKey!!),
                                FrontendMeta(name, genre, prim("developer")?.contentOrNullSafe(), prim("publisher")?.contentOrNullSafe(),
                                    null, null, null, null, time))
                            out += entry
                            id?.let { byId[it] = entry }
                        }
                    } else {
                        // Sesión: {gameId, duration} o {gameId, start, end}
                        val ref = keys.entries.firstOrNull { (k, _) -> (k.contains("game") || k.contains("rom")) && k.endsWith("id") }
                            ?.let { (e[it.value] as? JsonPrimitive)?.contentOrNullSafe() }
                        if (ref != null) {
                            val seconds = sessionSeconds(keys, e)
                            if (seconds != null) sessionSeconds[ref] = (sessionSeconds[ref] ?: 0.0) + seconds
                        }
                    }
                    e.values.forEach { if (it is JsonObject || it is JsonArray) visit(it, depth + 1) }
                }
                else -> Unit
            }
        }
        visit(root, 0)
        sessionSeconds.forEach { (id, s) ->
            val entry = byId[id] ?: return@forEach
            val minutes = (s / 60).toInt()
            if (minutes > (entry.meta.playMinutes ?: 0)) entry.meta = entry.meta.copy(playMinutes = minutes)
        }
        return out
    }

    private fun JsonPrimitive.contentOrNullSafe(): String? = if (this is kotlinx.serialization.json.JsonNull) null else content.takeIf { it.isNotBlank() }

    private fun sessionSeconds(keys: Map<String, String>, e: JsonObject): Double? {
        keys.entries.firstOrNull { (k, _) -> k.contains("duration") || k.contains("elapsed") || k == "length" || k == "seconds" }
            ?.let { (k, orig) -> (e[orig] as? JsonPrimitive)?.let { p -> toMinutes(k, p)?.let { return it * 60.0 } } }
        val start = keys.entries.firstOrNull { (k, _) -> k.startsWith("start") }?.let { (e[it.value] as? JsonPrimitive)?.longOrNull }
        val end = keys.entries.firstOrNull { (k, _) -> k.startsWith("end") || k.startsWith("stop") }?.let { (e[it.value] as? JsonPrimitive)?.longOrNull }
        if (start != null && end != null && end > start) {
            val d = (end - start).toDouble()
            return if (start > 100_000_000_000L) d / 1000 else d // marcas en milisegundos o en segundos
        }
        return null
    }

    private fun readSqlite(stream: InputStream): List<FrontendEntry> {
        val tmp = File.createTempFile("frontend", ".db", context.cacheDir)
        try {
            tmp.outputStream().use { stream.copyTo(it) }
            val db = SQLiteDatabase.openDatabase(tmp.path, null, SQLiteDatabase.OPEN_READONLY)
            return db.use { readTables(it) }
        } catch (_: Exception) {
            return emptyList()
        } finally {
            tmp.delete()
        }
    }

    private fun readTables(db: SQLiteDatabase): List<FrontendEntry> {
        val tables = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
        fun columns(t: String) = db.rawQuery("PRAGMA table_info(\"$t\")", null).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(1)) }
        }
        val out = mutableListOf<FrontendEntry>()
        val byId = HashMap<String, FrontendEntry>()
        val sessions = mutableListOf<Triple<String, String, List<String>>>() // tabla, columna de juego, columnas
        for (t in tables) {
            val cols = columns(t)
            val lower = cols.associateBy { it.lowercase() }
            val nameCol = listOf("title", "name", "gametitle", "game_title", "displayname", "display_name").firstNotNullOfOrNull { lower[it] }
            val pathCol = listOf("path", "rompath", "rom_path", "filepath", "file_path", "uri", "romuri", "rom_uri", "filename").firstNotNullOfOrNull { lower[it] }
            val timeCol = lower.entries.firstOrNull { isPlaytimeKey(it.key) }?.value
            val genreCol = listOf("genre", "genres").firstNotNullOfOrNull { lower[it] }
            val systemCol = listOf("platform", "system", "console", "platformid", "platform_id", "systemname").firstNotNullOfOrNull { lower[it] }
            val idCol = listOf("id", "_id", "gameid", "game_id", "uuid").firstNotNullOfOrNull { lower[it] }
            if (nameCol != null || pathCol != null) {
                if (timeCol == null && genreCol == null && pathCol == null) continue
                val sel = listOfNotNull(nameCol, pathCol, timeCol, genreCol, systemCol, idCol)
                db.rawQuery("SELECT ${sel.joinToString { "\"$it\"" }} FROM \"$t\" LIMIT 20000", null).use { c ->
                    while (c.moveToNext()) {
                        fun v(col: String?) = col?.let { runCatching { c.getString(sel.indexOf(it)) }.getOrNull() }
                        val name = v(nameCol)?.takeIf { it.isNotBlank() }
                        val fileKey = v(pathCol)?.takeIf { it.isNotBlank() }?.let { fileKeyOf(it) }
                        val minutes = timeCol?.let { col -> v(col)?.let { toMinutes(col.lowercase(), JsonPrimitive(it.toDoubleOrNull() ?: return@let null)) } }
                        val console = v(systemCol)?.let { Consoles.byFolder(it) ?: Consoles.guess(it) }
                        val entry = FrontendEntry(console?.id, fileKey, CoverSearch.tokens(name ?: fileKey ?: continue),
                            FrontendMeta(name, v(genreCol), null, null, null, null, null, null, minutes))
                        out += entry
                        v(idCol)?.let { byId[it] = entry }
                    }
                }
            } else {
                val ref = cols.firstOrNull { val l = it.lowercase(); (l.contains("game") || l.contains("rom")) && l.endsWith("id") }
                if (ref != null) sessions += Triple(t, ref, cols)
            }
        }
        // Tablas de sesiones: suma la duración por juego.
        for ((t, ref, cols) in sessions) {
            val lower = cols.associateBy { it.lowercase() }
            val dur = lower.entries.firstOrNull { it.key.contains("duration") || it.key.contains("elapsed") }?.value
            val start = lower.entries.firstOrNull { it.key.startsWith("start") }?.value
            val end = lower.entries.firstOrNull { it.key.startsWith("end") || it.key.startsWith("stop") }?.value
            val totals = HashMap<String, Double>()
            when {
                dur != null -> db.rawQuery("SELECT \"$ref\", SUM(\"$dur\") FROM \"$t\" GROUP BY \"$ref\"", null).use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0) ?: continue
                        toMinutes(dur.lowercase(), JsonPrimitive(c.getDouble(1)))?.let { totals[id] = it * 60.0 }
                    }
                }
                start != null && end != null -> db.rawQuery("SELECT \"$ref\", SUM(\"$end\" - \"$start\"), MAX(\"$start\") FROM \"$t\" GROUP BY \"$ref\"", null).use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0) ?: continue
                        val s = c.getDouble(1)
                        totals[id] = if (c.getLong(2) > 100_000_000_000L) s / 1000 else s
                    }
                }
            }
            totals.forEach { (id, s) ->
                val entry = byId[id] ?: return@forEach
                val minutes = (s / 60).toInt()
                if (minutes > (entry.meta.playMinutes ?: 0)) entry.meta = entry.meta.copy(playMinutes = minutes)
            }
        }
        return out
    }

    companion object {
        /** Etiquetas de tiempo de juego que usan distintos frontends (en minúsculas). */
        val playtimeFields = listOf("playtime", "gametime", "timeplayed", "totalplaytime", "playedtime", "play_time",
            "playtimeseconds", "playtime_seconds", "totaltime", "total_time", "timeplayedseconds")

        fun isPlaytimeKey(k: String): Boolean {
            val l = k.lowercase().replace("_", "")
            return l in playtimeFields.map { it.replace("_", "") } ||
                (l.contains("playtime") || l.contains("timeplayed") || l.contains("gametime") || l.contains("totalplay"))
        }

        /** Convierte un valor numérico de tiempo a minutos según cómo se llame el campo. */
        fun toMinutes(key: String, p: JsonPrimitive): Int? {
            val v = p.doubleOrNull ?: return p.content.let { EmulatorSource.parsePlaytime(it) }
            if (v <= 0) return null
            val k = key.lowercase()
            val minutes = when {
                k.contains("millis") || k.endsWith("ms") -> v / 60_000
                k.contains("min") -> v
                k.contains("hour") -> v * 60
                v > 36_000_000 -> v / 60_000 // más de 10 000 h si fueran segundos: son milisegundos
                else -> v / 60
            }
            return minutes.toInt().takeIf { it > 0 }
        }

        /** Decodifica %2F, %20... sin convertir "+" en espacio ("Mario + Rabbids"). */
        fun decode(s: String): String =
            runCatching { java.net.URLDecoder.decode(s.replace("+", "%2B"), "UTF-8") }.getOrDefault(s)

        /** "content://...%2FSwitch%2FAtelier%20Ryza.nsp" o "./Atelier Ryza.nsp" → clave del archivo. */
        fun fileKeyOf(path: String): String? {
            val decoded = decode(path).substringBefore('#')
            val name = decoded.substringAfterLast('/').substringAfterLast(':').substringAfterLast('\\')
            if (name.isBlank()) return null
            val base = if ('.' in name && name.substringAfterLast('.').length in 1..5) name.substringBeforeLast('.') else name
            return EmulatorSource.romKey(base).takeIf { it.isNotBlank() }
        }

        private fun parseDate(s: String): Long? = runCatching {
            LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull() ?: s.toLongOrNull()?.let { if (it > 100_000_000_000L) it else it * 1000 }
    }
}

/**
 * Empareja los juegos de la biblioteca con lo leído de los frontends:
 * primero por nombre de archivo, luego por título exacto y por último por título parecido.
 */
internal class FrontendIndex(private val entries: List<FrontendEntry>) {
    private val byFile = entries.filter { it.fileKey != null }.groupBy { (it.consoleId ?: "*") to it.fileKey!! }
    private val byName = entries.filter { it.nameTokens.isNotEmpty() }.groupBy { (it.consoleId ?: "*") to it.nameTokens }
    private val byConsole = entries.groupBy { it.consoleId ?: "*" }

    fun find(consoleId: String, fileKey: String, title: String): FrontendMeta? {
        val tokens = CoverSearch.tokens(title)
        val hits = LinkedHashSet<FrontendEntry>()
        for (c in listOf(consoleId, "*")) {
            byFile[c to fileKey]?.let(hits::addAll)
            if (tokens.isNotEmpty()) byName[c to tokens]?.let(hits::addAll)
        }
        if (hits.isEmpty() && tokens.size >= 1) {
            (byConsole[consoleId].orEmpty() + byConsole["*"].orEmpty())
                .map { it to CoverSearch.similarity(tokens, it.nameTokens) }
                .filter { it.second >= 0.85 }
                .maxByOrNull { it.second }?.first?.let(hits::add)
        }
        if (hits.isEmpty()) return null
        hits.forEach { it.used = true }
        return hits.map { it.meta }.reduce { a, b -> a.combine(b) }
    }

    /** Juegos de un sistema conocido que no se emparejaron: (sistema, archivo, datos). */
    fun unused(): List<Triple<String, String?, FrontendMeta>> =
        entries.filter { !it.used && it.consoleId != null && it.consoleId != "other" }
            .groupBy { it.consoleId!! to (it.fileKey ?: it.nameTokens.sorted().joinToString(" ")) }
            .map { (k, list) -> Triple(k.first, list.firstNotNullOfOrNull { it.fileKey }, list.map { it.meta }.reduce { a, b -> a.combine(b) }) }
}
