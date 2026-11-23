package com.repisa.app.sources

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.repisa.app.data.Http
import com.repisa.app.data.SafTree
import com.repisa.app.data.Settings
import com.repisa.app.data.arr
import com.repisa.app.data.get
import com.repisa.app.data.str
import com.repisa.app.model.Console
import com.repisa.app.model.Consoles
import com.repisa.app.model.Game
import com.repisa.app.model.Genres
import com.repisa.app.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Juegos de emuladores. Combina tres orígenes:
 *  - Carpetas de ROMs organizadas por sistema (snes/, psx/, ps2/... como ES-DE o RetroArch).
 *  - Datos de ES-DE: gamelist.xml (género, estudio, año, descripción) y portadas descargadas.
 *  - RetroArch: playlists (.lpl) y registros de tiempo de juego (.lrtl).
 */
class EmulatorSource(private val context: Context, private val settings: Settings) {

    private data class EsdeMeta(
        val name: String?, val genre: String?, val developer: String?, val publisher: String?,
        val year: Int?, val desc: String?, val lastPlayed: Long?, val cover: String?,
    )

    private data class Runtime(val minutes: Int, val lastPlayed: Long?)

    suspend fun fetchGames(onProgress: (String) -> Unit): List<Game> = withContext(Dispatchers.IO) {
        val games = LinkedHashMap<String, Game>()
        fun add(g: Game) { games[g.id] = games[g.id]?.let { mergeEmu(it, g) } ?: g }

        settings.romFolders.forEach { uri ->
            onProgress("Buscando ROMs en ${SafTree.displayName(context, uri)}…")
            runCatching { scanRomFolder(SafTree(context, Uri.parse(uri))) }.getOrDefault(emptyList()).forEach(::add)
        }

        val raTree = settings.retroarchFolder?.let { SafTree(context, Uri.parse(it)) }
        if (raTree != null) {
            onProgress("Leyendo playlists de RetroArch…")
            runCatching { readPlaylists(raTree) }.getOrDefault(emptyList()).forEach(::add)
        }

        val esde = settings.esdeFolder?.let {
            onProgress("Leyendo datos de ES-DE…")
            runCatching { readEsde(SafTree(context, Uri.parse(it))) }.getOrNull()
        } ?: emptyMap()

        // Juegos que ES-DE conoce pero que no salieron en las carpetas escaneadas.
        esde.forEach { (consoleId, metas) ->
            metas.forEach { (key, m) ->
                val id = "emu:$consoleId:$key"
                if (id !in games) add(Game(id, Source.EMULATOR, m.name ?: key, consoleId, romName = key))
            }
        }

        val runtimes = raTree?.let {
            onProgress("Leyendo tiempo de juego de RetroArch…")
            runCatching { readRuntimeLogs(it) }.getOrNull()
        } ?: emptyMap()

        games.values.map { g ->
            var out = g
            val key = romKey(g.romName ?: g.title)
            esde[g.consoleId]?.get(key)?.let { m ->
                out = out.copy(
                    title = m.name?.takeIf { it.isNotBlank() } ?: out.title,
                    genres = Genres.normalize(listOfNotNull(m.genre)).ifEmpty { out.genres },
                    developer = m.developer ?: out.developer,
                    publisher = m.publisher ?: out.publisher,
                    releaseYear = m.year ?: out.releaseYear,
                    description = m.desc ?: out.description,
                    lastPlayed = listOfNotNull(m.lastPlayed, out.lastPlayed).maxOrNull(),
                    coverUrl = m.cover ?: out.coverUrl,
                )
            }
            runtimes[key]?.let { r ->
                out = out.copy(
                    playtimeMinutes = r.minutes,
                    lastPlayed = listOfNotNull(r.lastPlayed, out.lastPlayed).maxOrNull(),
                )
            }
            out
        }
    }

    private fun mergeEmu(a: Game, b: Game) = a.copy(
        romUri = a.romUri ?: b.romUri,
        romPath = a.romPath ?: b.romPath,
        corePath = a.corePath ?: b.corePath,
    )

    // ---------------------------------------------------------------- Carpetas de ROMs

    private val skipDirs = setOf("media", "images", "videos", "manuals", "downloaded_media", "screenshots",
        "snaps", "boxart", "covers", "saves", "states", "bios", "system", "cheats", "textures", "imgs")

    private fun scanRomFolder(tree: SafTree): List<Game> {
        val root = tree.root
        Consoles.byFolder(root.name)?.let { return scanConsole(tree, root, it) }
        return tree.children(root).filter { it.isDir }.flatMap { dir ->
            val console = Consoles.byFolder(dir.name) ?: return@flatMap emptyList()
            scanConsole(tree, dir, console)
        }
    }

    private fun scanConsole(tree: SafTree, dir: SafTree.Doc, console: Console): List<Game> {
        val files = mutableListOf<SafTree.Doc>()
        fun walk(d: SafTree.Doc, depth: Int) {
            for (c in tree.children(d)) {
                if (c.name.startsWith(".")) continue
                if (c.isDir) {
                    if (depth < 2 && c.name.lowercase() !in skipDirs) walk(c, depth + 1)
                } else if (c.name.substringAfterLast('.', "").lowercase() in console.extensions) {
                    files += c
                }
            }
        }
        walk(dir, 0)

        // Agrupa discos de un mismo juego; prioriza .m3u, que ya los reúne.
        val priority = listOf("m3u", "chd", "cue", "gdi", "cdi", "iso", "rvz", "pbp")
        return files.groupBy { romKey(it.name.substringBeforeLast('.')) }.map { (key, docs) ->
            val best = docs.minBy { d ->
                val ext = d.name.substringAfterLast('.').lowercase()
                priority.indexOf(ext).let { if (it < 0) priority.size else it } * 10 + discNumber(d.name)
            }
            val romName = best.name.substringBeforeLast('.')
            Game(
                id = "emu:${console.id}:$key",
                source = Source.EMULATOR,
                title = cleanTitle(romName),
                consoleId = console.id,
                romUri = best.uri.toString(),
                romName = romName,
            )
        }
    }

    // ---------------------------------------------------------------- ES-DE

    private fun readEsde(tree: SafTree): Map<String, Map<String, EsdeMeta>> {
        val gamelists = tree.path("gamelists") ?: return emptyMap()
        val media = tree.path("downloaded_media")
        val out = mutableMapOf<String, MutableMap<String, EsdeMeta>>()
        for (sysDir in tree.children(gamelists).filter { it.isDir }) {
            val console = Consoles.byFolder(sysDir.name) ?: continue
            val xml = tree.child(sysDir, "gamelist.xml")?.let { tree.readText(it) } ?: continue
            val covers: Map<String, String> = media?.let { m ->
                val sysMedia = tree.child(m, sysDir.name) ?: return@let null
                val coverDir = tree.child(sysMedia, "covers") ?: tree.child(sysMedia, "3dboxes") ?: return@let null
                tree.children(coverDir).associate { romKey(it.name.substringBeforeLast('.')) to it.uri.toString() }
            } ?: emptyMap()
            val map = out.getOrPut(console.id) { mutableMapOf() }
            parseGamelist(xml).forEach { fields ->
                val path = fields["path"] ?: return@forEach
                val key = romKey(path.substringAfterLast('/').substringBeforeLast('.'))
                map[key] = EsdeMeta(
                    name = fields["name"],
                    genre = fields["genre"],
                    developer = fields["developer"],
                    publisher = fields["publisher"],
                    year = fields["releasedate"]?.take(4)?.toIntOrNull()?.takeIf { it > 1950 },
                    desc = fields["desc"],
                    lastPlayed = fields["lastplayed"]?.let(::parseEsdeDate),
                    cover = covers[key],
                )
            }
        }
        return out
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
                    if (parser.name == "game") current = mutableMapOf() else tag = parser.name
                }
                XmlPullParser.TEXT -> {
                    val t = tag
                    if (current != null && t != null && parser.text.isNotBlank()) current[t] = parser.text.trim()
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "game") { current?.let(result::add); current = null }
                    tag = null
                }
                XmlPullParser.END_DOCUMENT -> return result
            }
        }
    }

    private fun parseEsdeDate(s: String): Long? = runCatching {
        LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()

    // ---------------------------------------------------------------- RetroArch

    private fun readPlaylists(tree: SafTree): List<Game> {
        val dir = tree.path("playlists") ?: return emptyList()
        return tree.children(dir).filter { it.name.endsWith(".lpl") }.flatMap { file ->
            val json = tree.readText(file)?.let { runCatching { Http.json.parseToJsonElement(it) }.getOrNull() }
                ?: return@flatMap emptyList()
            val playlistConsole = Consoles.byLibretroName(file.name) ?: Consoles.guess(file.name)
            json["items"].arr.mapNotNull { item ->
                val path = item["path"].str ?: return@mapNotNull null
                val core = item["core_path"].str?.takeIf { it != "DETECT" }
                val console = item["db_name"].str?.let(Consoles::byLibretroName)
                    ?: playlistConsole
                    ?: item["core_name"].str?.let(Consoles::guess)
                    ?: core?.let(Consoles::guess)
                    ?: return@mapNotNull null
                val romName = path.substringBefore('#').substringAfterLast('/').substringBeforeLast('.')
                Game(
                    id = "emu:${console.id}:${romKey(romName)}",
                    source = Source.EMULATOR,
                    title = cleanTitle(item["label"].str ?: romName),
                    consoleId = console.id,
                    romPath = path.substringBefore('#'),
                    corePath = core,
                    romName = romName,
                )
            }
        }
    }

    private fun readRuntimeLogs(tree: SafTree): Map<String, Runtime> {
        val logs = tree.path("playlists", "logs") ?: return emptyMap()
        val totals = mutableMapOf<String, Runtime>()
        fun visit(d: SafTree.Doc, depth: Int) {
            for (c in tree.children(d)) {
                if (c.isDir && depth < 2) visit(c, depth + 1)
                else if (c.name.endsWith(".lrtl")) {
                    val json = tree.readText(c)?.let { runCatching { Http.json.parseToJsonElement(it) }.getOrNull() } ?: continue
                    val minutes = json["runtime"].str?.let(::hmsToMinutes) ?: 0
                    val last = json["last_played"].str?.let {
                        runCatching {
                            LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }.getOrNull()
                    }
                    val key = romKey(c.name.removeSuffix(".lrtl"))
                    val prev = totals[key]
                    totals[key] = Runtime(
                        (prev?.minutes ?: 0) + minutes,
                        listOfNotNull(prev?.lastPlayed, last).maxOrNull(),
                    )
                }
            }
        }
        visit(logs, 0)
        return totals
    }

    private fun hmsToMinutes(s: String): Int {
        val p = s.split(':').mapNotNull { it.trim().toIntOrNull() }
        return when (p.size) {
            3 -> p[0] * 60 + p[1] + if (p[2] >= 30) 1 else 0
            2 -> p[0] + if (p[1] >= 30) 1 else 0
            else -> 0
        }
    }

    companion object {
        private val discRegex = Regex("""\s*[(\[](disc|disk|cd)\s*\d+[^)\]]*[)\]]""", RegexOption.IGNORE_CASE)

        /** Clave para cruzar el mismo juego entre carpetas, ES-DE y RetroArch. */
        fun romKey(romName: String): String = romName.replace(discRegex, "").trim().lowercase()

        private fun discNumber(name: String): Int =
            Regex("""(disc|disk|cd)\s*(\d+)""", RegexOption.IGNORE_CASE).find(name)?.groupValues?.get(2)?.toIntOrNull() ?: 0

        /** "Legend of Zelda, The - A Link to the Past (USA) [!]" → "The Legend of Zelda - A Link to the Past" */
        fun cleanTitle(romName: String): String {
            var t = romName.replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), "").replace('_', ' ').trim()
            Regex("""^(.*?), (The|A|An|El|La|Los|Las|Le|Les|Die|Der|Das)( - .*)?$""").find(t)?.let { m ->
                t = "${m.groupValues[2]} ${m.groupValues[1]}${m.groupValues[3]}"
            }
            return t.ifBlank { romName }
        }
    }
}
