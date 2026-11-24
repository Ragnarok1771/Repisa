package com.repisa.app.sources

import android.webkit.CookieManager
import com.repisa.app.data.Http
import com.repisa.app.data.Settings
import com.repisa.app.data.arr
import com.repisa.app.data.get
import com.repisa.app.data.long
import com.repisa.app.data.obj
import com.repisa.app.data.str
import com.repisa.app.i18n.L10n
import com.repisa.app.model.Game
import com.repisa.app.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Un logro: [unlockedAt] es cuándo se consiguió (null = aún no); [manual] = marcado a mano por el usuario. */
@Serializable
data class Achievement(
    val id: String,
    val name: String,
    val description: String = "",
    val icon: String? = null,
    val percent: Float? = null,
    val unlockedAt: Long? = null,
    val manual: Boolean = false,
    val hidden: Boolean = false,
    val points: Int? = null,
)

/**
 * Logros de un juego. [kind]:
 *  - STEAM: los tuyos en Steam (con fecha de desbloqueo).
 *  - STEAM_LIST: la lista de logros de Steam sin tu progreso (juego de Epic, o perfil de Steam privado):
 *    puedes marcarlos a mano.
 *  - RETRO: RetroAchievements (con fecha).
 */
@Serializable
data class AchievementSet(
    val kind: String,
    val items: List<Achievement>,
    val url: String? = null,
    val note: String? = null,
    val fetchedAt: Long = System.currentTimeMillis(),
) {
    val unlocked: Int get() = items.count { it.unlockedAt != null }
}

object Achievements {
    const val STEAM = "STEAM"
    const val STEAM_LIST = "STEAM_LIST"
    const val RETRO = "RETRO"

    private const val UA = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"

    private fun steamLang(): String = when (L10n.current?.tag?.substringBefore('-')) {
        "es" -> if (L10n.current?.tag == "es-419") "latam" else "spanish"
        "fr" -> "french"; "de" -> "german"; "it" -> "italian"
        "pt" -> if (L10n.current?.tag == "pt-BR") "brazilian" else "portuguese"
        else -> "english"
    }

    private suspend fun get(url: String, cookies: String? = null): Pair<Int, String> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).header("User-Agent", UA).apply { cookies?.let { header("Cookie", it) } }.build()
        Http.client.newCall(req).execute().use { it.code to (it.body?.string().orEmpty()) }
    }

    // ------------------------------------------------------------------ Steam

    /** appid de Steam de un juego: el suyo si es de Steam; si es de Epic, el del mismo juego en la tienda de Steam. */
    suspend fun steamAppId(game: Game): Int? {
        if (game.source == Source.STEAM) return game.id.removePrefix("steam:").toIntOrNull()
        val title = game.originalTitle ?: game.title
        val url = "https://store.steampowered.com/api/storesearch/".toHttpUrl().newBuilder()
            .addQueryParameter("term", title).addQueryParameter("l", "english").addQueryParameter("cc", "US").build()
        val (code, body) = get(url.toString())
        if (code != 200) return null
        val wanted = CoverSearch.tokens(title)
        return Http.json.parseToJsonElement(body)["items"].arr
            .map { it to CoverSearch.similarity(wanted, CoverSearch.tokens(it["name"].str.orEmpty())) }
            .filter { it.second >= 0.85 }.maxByOrNull { it.second }?.first?.get("id")?.long?.toInt()
    }

    /** Lista completa de logros de un juego de Steam (nombre, descripción, icono) desde su página pública. */
    private suspend fun steamSchema(appId: Int): List<Achievement> {
        val (code, html) = get("https://steamcommunity.com/achievements/$appId?l=${steamLang()}")
        if (code != 200) return emptyList()
        val row = Regex("""\\"api_name\\":\\"(.*?)\\",\\"name\\":\\"(.*?)\\",\\"description\\":\\"(.*?)\\",\\"hidden\\":(true|false).*?\\"icon_achieved\\":\\"([^"\\]+)\\"""")
        return row.findAll(html).map { m ->
            val (api, name, desc, hidden, icon) = m.destructured
            Achievement(api, unescape(name), unescape(desc),
                "https://cdn.fastly.steamstatic.com/steamcommunity/public/images/apps/$appId/$icon", hidden = hidden == "true")
        }.distinctBy { it.id }.toList()
    }

    private fun unescape(s: String) = s.replace(Regex("""\\\\u([0-9a-fA-F]{4})""")) { it.groupValues[1].toInt(16).toChar().toString() }
        .replace("\\\\\\\"", "\"").replace("\\\\n", " ").replace("\\\\/", "/").replace("\\\\'", "'")

    /** % de jugadores que tiene cada logro (API pública de Steam, sin clave). */
    private suspend fun steamPercents(appId: Int): Map<String, Float> {
        val (code, body) = get("https://api.steampowered.com/ISteamUserStats/GetGlobalAchievementPercentagesForApp/v2/?gameid=$appId")
        if (code != 200) return emptyMap()
        return Http.json.parseToJsonElement(body)["achievementpercentages"]["achievements"].arr
            .mapNotNull { a -> a["name"].str?.let { n -> a["percent"].str?.toFloatOrNull()?.let { n to it } } }.toMap()
    }

    /**
     * Tus desbloqueos en Steam (apiName → fecha). Prueba, en orden: clave de API, la sesión de Steam de Repisa y
     * el perfil público (XML de la comunidad). Devuelve null si Steam no deja verlos (perfil privado).
     */
    private suspend fun steamUnlocks(settings: Settings, appId: Int): Map<String, Long?>? {
        val steamId = settings.steamLinkedId ?: settings.steamUser ?: return null
        fun parseApi(body: String): Map<String, Long?>? = runCatching {
            val stats = Http.json.parseToJsonElement(body)["playerstats"]
            if (stats["success"]?.toString() == "false") return null
            stats["achievements"].arr.associate { a ->
                a["apiname"].str!! to (if (a["achieved"].long == 1L) (a["unlocktime"].long ?: 0L) * 1000 else null)
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() }

        val base = "https://api.steampowered.com/ISteamUserStats/GetPlayerAchievements/v1/?appid=$appId&steamid=$steamId"
        settings.steamApiKey?.let { key -> get("$base&key=$key").takeIf { it.first == 200 }?.let { parseApi(it.second) }?.let { return it } }
        runCatching { SteamLogin.fetchWebToken() }.getOrNull()?.let { token ->
            get("$base&access_token=$token").takeIf { it.first == 200 }?.let { parseApi(it.second) }?.let { return it }
        }
        // Perfil de la comunidad en XML (funciona si los detalles de juego son públicos, o con la sesión iniciada).
        val cookies = runCatching { withContext(Dispatchers.Main) { CookieManager.getInstance().getCookie("https://steamcommunity.com") } }.getOrNull()
        val (code, xml) = get("https://steamcommunity.com/profiles/$steamId/stats/$appId/achievements/?xml=1", cookies)
        if (code != 200 || !xml.contains("<achievements>")) return null
        return Regex("""<achievement closed="(\d)">(.*?)</achievement>""", RegexOption.DOT_MATCHES_ALL).findAll(xml).mapNotNull { m ->
            val api = Regex("""<apiname>(?:<!\[CDATA\[)?(.*?)(?:]]>)?</apiname>""").find(m.groupValues[2])?.groupValues?.get(1) ?: return@mapNotNull null
            val time = Regex("""<unlockTimestamp>(\d+)</unlockTimestamp>""").find(m.groupValues[2])?.groupValues?.get(1)?.toLongOrNull()
            api to (if (m.groupValues[1] == "1") (time ?: 0L) * 1000 else null)
        }.toMap().takeIf { it.isNotEmpty() }
    }

    /** Logros de Steam para un juego de Steam o de Epic. [manual]: marcas a mano (id → fecha) para juegos de Epic. */
    suspend fun steam(game: Game, settings: Settings, manual: Map<String, Long>): AchievementSet? {
        val appId = steamAppId(game) ?: return null
        val schema = steamSchema(appId)
        if (schema.isEmpty()) return null
        val percents = runCatching { steamPercents(appId) }.getOrDefault(emptyMap())
        val unlocks = if (game.source == Source.STEAM) runCatching { steamUnlocks(settings, appId) }.getOrNull() else null
        val url = "https://steamcommunity.com/stats/$appId/achievements"
        return if (unlocks != null) {
            AchievementSet(STEAM, schema.map { a ->
                a.copy(percent = percents[a.id], unlockedAt = unlocks[a.id]?.takeIf { it > 0 } ?: unlocks[a.id]?.let { 1L })
            }, url)
        } else {
            AchievementSet(STEAM_LIST, schema.map { a ->
                a.copy(percent = percents[a.id], unlockedAt = manual[a.id], manual = a.id in manual)
            }, url, note = if (game.source == Source.STEAM) "private" else "epic")
        }
    }

    // ------------------------------------------------------------------ RetroAchievements

    /** Sistemas de Repisa → id de consola en RetroAchievements. */
    val raConsoles = mapOf(
        "megadrive" to 1, "n64" to 2, "snes" to 3, "gb" to 4, "gba" to 5, "gbc" to 6, "nes" to 7, "pcengine" to 8,
        "segacd" to 9, "sega32x" to 10, "mastersystem" to 11, "psx" to 12, "gamegear" to 15, "gc" to 16, "nds" to 18,
        "wii" to 19, "ps2" to 21, "atari2600" to 25, "arcade" to 27, "saturn" to 39, "dreamcast" to 40, "psp" to 41,
    )

    private val raGameLists = HashMap<Int, List<Pair<Int, String>>>()

    private suspend fun raGet(settings: Settings, endpoint: String, params: String): String? {
        val user = settings.raUser ?: return null
        val key = settings.raApiKey ?: return null
        val (code, body) = get("https://retroachievements.org/API/$endpoint.php?z=$user&y=$key&$params")
        return body.takeIf { code == 200 }
    }

    /** Comprueba usuario y clave (para Ajustes). */
    suspend fun raCheck(settings: Settings): Boolean =
        raGet(settings, "API_GetUserProfile", "u=${settings.raUser}")?.let { it.contains("\"User\"") } == true

    private suspend fun raGameId(settings: Settings, game: Game): Int? {
        val console = raConsoles[game.consoleId] ?: return null
        val list = raGameLists[console] ?: raGet(settings, "API_GetGameList", "i=$console&f=1")?.let { body ->
            Http.json.parseToJsonElement(body).arr.mapNotNull { g ->
                val id = g["ID"].long?.toInt() ?: return@mapNotNull null
                val title = g["Title"].str ?: return@mapNotNull null
                if (title.startsWith("~")) null else id to title // fuera hacks, homebrew y similares
            }
        }?.also { raGameLists[console] = it } ?: return null
        val wanted = CoverSearch.tokens(game.title)
        return list.map { it to CoverSearch.similarity(wanted, CoverSearch.tokens(it.second.replace("|", " "))) }
            .filter { it.second >= 0.85 }.maxByOrNull { it.second }?.first?.first
    }

    suspend fun retro(game: Game, settings: Settings): AchievementSet? {
        if (settings.raUser == null || settings.raApiKey == null) return null
        val id = raGameId(settings, game) ?: return null
        val body = raGet(settings, "API_GetGameInfoAndUserProgress", "u=${settings.raUser}&g=$id") ?: return null
        val root = Http.json.parseToJsonElement(body)
        val players = root["NumDistinctPlayers"].long?.toFloat()?.takeIf { it > 0 }
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        fun date(s: String?) = s?.let { runCatching { LocalDateTime.parse(it, fmt).toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull() }
        val items = root["Achievements"].obj.values.sortedBy { it["DisplayOrder"].long ?: 0 }.map { a ->
            Achievement(
                id = a["ID"].str ?: "",
                name = a["Title"].str.orEmpty(),
                description = a["Description"].str.orEmpty(),
                icon = a["BadgeName"].str?.let { "https://media.retroachievements.org/Badge/$it.png" },
                percent = players?.let { p -> (a["NumAwarded"].long ?: 0) * 100f / p },
                unlockedAt = date(a["DateEarnedHardcore"].str) ?: date(a["DateEarned"].str),
                points = a["Points"].long?.toInt(),
            )
        }
        if (items.isEmpty()) return null
        return AchievementSet(RETRO, items, "https://retroachievements.org/game/$id")
    }
}
