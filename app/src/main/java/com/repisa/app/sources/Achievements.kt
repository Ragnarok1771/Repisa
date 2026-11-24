package com.repisa.app.sources

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

    private val edition = setOf("goty", "game", "of", "the", "year", "edition", "definitive", "complete", "deluxe", "ultimate",
        "remastered", "standard", "enhanced", "directors", "cut", "collection", "anniversary", "gold", "premium", "special")

    /** appid de Steam de un juego: el suyo si es de Steam; si es de Epic, el del mismo juego en la tienda de Steam. */
    suspend fun steamAppId(game: Game): Int? {
        if (game.source == Source.STEAM) return game.id.removePrefix("steam:").toIntOrNull()
        val title = game.originalTitle ?: game.title
        val url = "https://store.steampowered.com/api/storesearch/".toHttpUrl().newBuilder()
            .addQueryParameter("term", title).addQueryParameter("l", "english").addQueryParameter("cc", "US").build()
        val (code, body) = get(url.toString())
        if (code != 200) return null
        // Sin palabras de edición («Game of the Year», «Definitive Edition»...) ni DLC del mismo juego.
        fun core(t: String) = CoverSearch.tokens(t) - edition
        val wanted = core(title)
        if (wanted.isEmpty()) return null
        return Http.json.parseToJsonElement(body)["items"].arr
            .filter { !Regex("(?i)\\b(soundtrack|dlc|season pass|pack|bundle)\\b").containsMatchIn(it["name"].str.orEmpty()) }
            .map { it to CoverSearch.similarity(wanted, core(it["name"].str.orEmpty())) }
            .filter { it.second >= 0.85 }.maxByOrNull { it.second }?.first?.get("id")?.long?.toInt()
    }

    /** Lista de logros de un juego de Steam (API pública): nombre y descripción traducidos, icono y % de jugadores. */
    private class SteamSchema(val items: List<Achievement>, val byKey: Map<Long, String>)

    private suspend fun steamSchema(appId: Int): SteamSchema {
        val (code, body) = get("https://api.steampowered.com/IPlayerService/GetGameAchievements/v1/?appid=$appId&language=${steamLang()}")
        if (code == 429 || code >= 500) error("Steam $code") // límite de peticiones: reintentar más tarde
        if (code != 200) return SteamSchema(emptyList(), emptyMap())
        val list = Http.json.parseToJsonElement(body)["response"]["achievements"].arr
        val byKey = HashMap<Long, String>()
        val items = list.mapNotNull { a ->
            val id = a["internal_name"].str ?: return@mapNotNull null
            a["internal_key"].long?.let { byKey[it] = id }
            if (a["archived"]?.toString() == "true") return@mapNotNull null
            Achievement(
                id = id,
                name = a["localized_name"].str.orEmpty(),
                description = a["localized_desc"].str.orEmpty(),
                icon = a["icon"].str?.let { "https://cdn.fastly.steamstatic.com/steamcommunity/public/images/apps/$appId/$it" },
                percent = a["player_percent_unlocked"].str?.toFloatOrNull(),
                hidden = a["hidden"]?.toString() == "true",
            )
        }
        return SteamSchema(items, byKey)
    }

    /**
     * Tus desbloqueos en Steam (id del logro → fecha), con el mismo servicio que usa la web de Steam
     * (IPlayerService/GetUserAchievements): con la sesión de Steam de Repisa funciona aunque tu perfil sea privado;
     * sin ella, si los detalles de juego son públicos. Con clave de API se usa también GetPlayerAchievements.
     * Devuelve null si Steam no deja verlos.
     */
    private suspend fun steamUnlocks(settings: Settings, appId: Int, schema: SteamSchema): Map<String, Long?>? {
        val steamId = settings.steamLinkedId ?: settings.steamUser ?: return null
        val token = runCatching { SteamLogin.fetchWebToken() }.getOrNull()
        val auth = buildString {
            token?.let { append("&access_token=").append(it) }
            if (token == null) settings.steamApiKey?.let { append("&key=").append(it) }
        }
        val (code, body) = get("https://api.steampowered.com/IPlayerService/GetUserAchievements/v1/?appid=$appId&steamid=$steamId$auth")
        if (code == 429 || code >= 500) error("Steam $code")
        if (code == 200) {
            val r = Http.json.parseToJsonElement(body)["response"]
            // Con acceso, la respuesta trae siempre schema_version (aunque no tengas ningún logro); si es privado, viene vacía.
            if (r["schema_version"] != null) {
                val out = HashMap<String, Long?>()
                r["achievements"].arr.forEach { a ->
                    val id = a["internal_key"].long?.let { schema.byKey[it] } ?: a["internal_name"].str ?: return@forEach
                    val unlocked = a["unlocked"]?.toString().let { it == "true" || it == "1" }
                    val time = (a["unlock_time"].long ?: a["rtime_unlocked"].long ?: 0L) * 1000
                    out[id] = if (unlocked) time.takeIf { it > 0 } ?: 1L else null
                }
                return out
            }
        }
        // Clave de API (opcional): el método clásico.
        settings.steamApiKey?.let { key ->
            val (c2, b2) = get("https://api.steampowered.com/ISteamUserStats/GetPlayerAchievements/v1/?appid=$appId&steamid=$steamId&key=$key")
            if (c2 == 200) runCatching {
                val stats = Http.json.parseToJsonElement(b2)["playerstats"]
                if (stats["success"]?.toString() != "false") return stats["achievements"].arr.associate { a ->
                    a["apiname"].str!! to (if (a["achieved"].long == 1L) ((a["unlocktime"].long ?: 0L) * 1000).takeIf { it > 0 } ?: 1L else null)
                }
            }
        }
        return null
    }

    /** Logros de Steam para un juego de Steam o de Epic. [manual]: marcas a mano (id → fecha) para juegos de Epic. */
    suspend fun steam(game: Game, settings: Settings, manual: Map<String, Long>): AchievementSet? {
        val appId = steamAppId(game) ?: return null
        val schema = steamSchema(appId)
        if (schema.items.isEmpty()) return null
        val unlocks = if (game.source == Source.STEAM) steamUnlocks(settings, appId, schema) else null
        val url = "https://steamcommunity.com/stats/$appId/achievements"
        return if (unlocks != null) {
            AchievementSet(STEAM, schema.items.map { a -> a.copy(unlockedAt = unlocks[a.id]) }, url)
        } else {
            AchievementSet(STEAM_LIST, schema.items.map { a -> a.copy(unlockedAt = manual[a.id], manual = a.id in manual) },
                url, note = if (game.source == Source.STEAM) "private" else "epic")
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
