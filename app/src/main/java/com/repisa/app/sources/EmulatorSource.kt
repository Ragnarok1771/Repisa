package com.repisa.app.sources

import android.content.Context
import android.net.Uri
import com.repisa.app.R
import com.repisa.app.data.Http
import com.repisa.app.i18n.L10n
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

    private data class Runtime(val minutes: Int, val lastPlayed: Long?)

    suspend fun fetchGames(onProgress: (String) -> Unit): List<Game> = withContext(Dispatchers.IO) {
        val games = LinkedHashMap<String, Game>()
        fun add(g: Game) { games[g.id] = games[g.id]?.let { mergeEmu(it, g) } ?: g }

        settings.romFolders.forEach { uri ->
            onProgress(L10n.s(R.string.status_scanning_roms, SafTree.displayName(context, uri)))
            runCatching { scanRomFolder(SafTree(context, Uri.parse(uri))) }.getOrDefault(emptyList()).forEach(::add)
        }

        val raTree = settings.retroarchFolder?.let { SafTree(context, Uri.parse(it)) }
        if (raTree != null) {
            onProgress(L10n.s(R.string.status_ra_playlists))
            runCatching { readPlaylists(raTree) }.getOrDefault(emptyList()).forEach(::add)
        }

        // ES-DE y Cocoon (o cualquier frontend): gamelist.xml y copias de seguridad.
        val entries = mutableListOf<FrontendEntry>()
        var report = FrontendReport()
        listOfNotNull(settings.esdeFolder?.let { it to "ES-DE" }, settings.cocoonFolder?.let { it to "Cocoon" }).forEach { (uri, name) ->
            onProgress(L10n.s(R.string.status_reading, name))
            runCatching { FrontendReader(context, SafTree(context, Uri.parse(uri))).read() }.getOrNull()?.let { (found, r) ->
                entries += found
                report = report.copy(lists = report.lists + r.lists, systems = report.systems + r.systems,
                    entries = report.entries + r.entries, withGenre = report.withGenre + r.withGenre,
                    withTime = report.withTime + r.withTime, backupFiles = report.backupFiles + r.backupFiles,
                    backupWithTime = report.backupWithTime + r.backupWithTime)
            }
        }
        val index = FrontendIndex(entries)

        val runtimes = raTree?.let {
            onProgress(L10n.s(R.string.status_ra_time))
            runCatching { readRuntimeLogs(it) }.getOrNull()
        } ?: emptyMap()

        val result = games.values.map { g ->
            var out = g
            val key = romKey(g.romName ?: g.title)
            index.find(g.consoleId, key, g.title)?.let { m ->
                report.matched++
                if ((m.playMinutes ?: 0) > 0) report.matchedWithTime++
                out = applyMeta(out, m)
            }
            // Si varias fuentes registran el mismo juego (RetroArch y el frontend), se toma la mayor
            // para no contar dos veces las mismas partidas.
            runtimes[key]?.let { r ->
                out = out.copy(
                    playtimeMinutes = maxOf(out.playtimeMinutes, r.minutes),
                    lastPlayed = listOfNotNull(r.lastPlayed, out.lastPlayed).maxOrNull(),
                )
            }
            out
        }.toMutableList()

        // Juegos que el frontend conoce pero que no salieron en las carpetas escaneadas.
        index.unused().forEach { (consoleId, fileKey, m) ->
            val key = fileKey ?: romKey(m.name ?: return@forEach)
            val id = "emu:$consoleId:$key"
            if (result.none { it.id == id }) result += applyMeta(Game(id, Source.EMULATOR, m.name ?: cleanTitle(key), consoleId, romName = key), m)
        }
        if (settings.esdeFolder != null || settings.cocoonFolder != null) settings.frontendReport = report.encode()
        result
    }

    private fun applyMeta(g: Game, m: FrontendMeta) = g.copy(
        title = m.name?.takeIf { it.isNotBlank() } ?: g.title,
        genres = Genres.normalize(m.genre?.split(Regex("""\s*[,/;]\s*""")).orEmpty()).ifEmpty { g.genres },
        developer = m.developer ?: g.developer,
        publisher = m.publisher ?: g.publisher,
        releaseYear = m.year ?: g.releaseYear,
        description = m.desc ?: g.description,
        lastPlayed = listOfNotNull(m.lastPlayed, g.lastPlayed).maxOrNull(),
        coverUrl = m.cover ?: g.coverUrl,
        playtimeMinutes = maxOf(g.playtimeMinutes, m.playMinutes ?: 0),
    )

    private fun mergeEmu(a: Game, b: Game) = a.copy(
        romUri = a.romUri ?: b.romUri,
        romPath = a.romPath ?: b.romPath,
        corePath = a.corePath ?: b.corePath,
    )

    // ---------------------------------------------------------------- Carpetas de ROMs

    private val skipDirs = setOf("media", "images", "videos", "manuals", "downloaded_media", "screenshots",
        "snaps", "boxart", "covers", "saves", "states", "bios", "system", "cheats", "textures", "imgs")

    /**
     * Basta con elegir la carpeta principal: se buscan carpetas con nombre de sistema (switch/, snes/, ps2/...)
     * hasta 4 niveles por debajo ("Emulación/ROMs/switch").
     */
    private fun scanRomFolder(tree: SafTree): List<Game> {
        val root = tree.root
        Consoles.byFolder(root.name)?.let { return scanConsole(tree, root, it) }
        val out = mutableListOf<Game>()
        var visited = 0
        fun walk(dir: SafTree.Doc, depth: Int) {
            for (c in tree.children(dir)) {
                if (!c.isDir || c.name.startsWith(".")) continue
                val console = Consoles.byFolder(c.name)
                when {
                    console != null -> out += scanConsole(tree, c, console)
                    depth < 3 && c.name.lowercase() !in skipDirs && c.name != "Android" && ++visited < 1500 -> walk(c, depth + 1)
                }
            }
        }
        walk(root, 0)
        return out
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

    // ---------------------------------------------------------------- RetroArch

    private fun readPlaylists(tree: SafTree): List<Game> {
        val dir = tree.findDir("playlists", skip = skipDirs) ?: return emptyList()
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
        val logs = tree.findDir("playlists", skip = skipDirs)?.let { tree.child(it, "logs") } ?: return emptyMap()
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
        /**
         * Tiempo en segundos ("37842"), "h:mm:ss" o con unidades ("10h 30m"). Devuelve minutos.
         */
        fun parsePlaytime(raw: String): Int? {
            val t = raw.trim()
            t.toLongOrNull()?.let { return (it / 60).toInt() }
            if (':' in t) {
                val p = t.split(':').mapNotNull { it.trim().toIntOrNull() }
                return when (p.size) { 3 -> p[0] * 60 + p[1]; 2 -> p[0] * 60 + p[1]; else -> null }
            }
            val h = Regex("""(\d+)\s*h""").find(t)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val m = Regex("""(\d+)\s*m""").find(t)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            return if (h + m > 0) h * 60 + m else null
        }

        private val discRegex = Regex("""\s*[(\[](disc|disk|cd)\s*\d+[^)\]]*[)\]]""", RegexOption.IGNORE_CASE)

        /** Clave para cruzar el mismo juego entre carpetas, ES-DE y RetroArch. */
        fun romKey(romName: String): String = romName.replace(discRegex, "").trim().lowercase()

        private fun discNumber(name: String): Int =
            Regex("""(disc|disk|cd)\s*(\d+)""", RegexOption.IGNORE_CASE).find(name)?.groupValues?.get(2)?.toIntOrNull() ?: 0

        /** Palabras de relleno que suelen ir al final del nombre de archivo y no forman parte del título. */
        private val junk = setOf(
            "nsp", "xci", "nsz", "xcz", "cia", "3ds", "nds", "iso", "rvz", "wbfs", "eshop", "update", "upd", "dlc",
            "base", "decrypted", "dec", "trimmed", "switch", "nsw", "usa", "eur", "jpn", "pal", "ntsc", "esp",
            "spanish", "english", "multi", "en", "es", "fr", "de", "it", "jp", "us", "eu", "rom", "final", "fixed",
        )
        private val junkPattern = Regex("""(?i)^(v\d+(\.\d+)*|rev\s?\d*|multi\d+|0100[0-9a-f]{12}|[0-9a-f]{16})$""")

        /**
         * Convierte un nombre de archivo en un título legible:
         *  "Legend of Zelda, The - A Link to the Past (USA) [!]" → "The Legend of Zelda - A Link to the Past"
         *  "4262 - Kingdom Hearts 358-2 Days (Europe)"            → "Kingdom Hearts 358-2 Days"
         *  "Bravely_Default_II_[0100B6801137E000][v0].nsp"        → "Bravely Default II"
         */
        fun cleanTitle(romName: String): String {
            var t = romName.replace(Regex("""\s*[(\[{][^)\]}]*[)\]}]"""), " ")
            // Nombres con "_" o "." en lugar de espacios.
            if (!t.contains(' ') || t.count { it == '_' } >= 2) t = t.replace('_', ' ')
            if (!t.contains(' ') && t.count { it == '.' } >= 2) t = t.replace('.', ' ')
            // Número de lista de lanzamientos al principio ("4262 - ", "0123 ").
            t = t.replace(Regex("""^\s*#?\d{4}\s*[-–_.]\s+(?=\S)"""), "")
            // Relleno al final: extensiones, versiones, idiomas, "eShop", "Update"...
            val words = t.trim().split(Regex("""\s+""")).toMutableList()
            while (words.size > 1) {
                val w = words.last().trim('-', '–', '+', ',').lowercase()
                if (w.isEmpty() || w in junk || junkPattern.matches(w)) words.removeAt(words.lastIndex) else break
            }
            t = words.joinToString(" ").trim().trimEnd('-', '–', ',').trim()
            Regex("""^(.*?), (The|A|An|El|La|Los|Las|Le|Les|Die|Der|Das)( - .*)?$""").find(t)?.let { m ->
                t = "${m.groupValues[2]} ${m.groupValues[1]}${m.groupValues[3]}"
            }
            return t.ifBlank { romName }
        }
    }
}
