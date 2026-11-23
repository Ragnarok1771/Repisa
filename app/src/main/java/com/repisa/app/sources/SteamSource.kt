package com.repisa.app.sources

import android.util.Base64
import android.webkit.CookieManager
import com.repisa.app.data.Http
import com.repisa.app.data.arr
import com.repisa.app.data.get
import com.repisa.app.data.int
import com.repisa.app.data.long
import com.repisa.app.data.str
import com.repisa.app.model.Game
import com.repisa.app.model.Genres
import com.repisa.app.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

/**
 * Biblioteca de Steam mediante la Steam Web API. Dos formas de acceso:
 *  - Cuenta vinculada: el usuario inicia sesión en la web oficial de Steam dentro de la app
 *    y usamos el token web de esa sesión ([SteamLogin]).
 *  - Avanzado: clave de API propia + SteamID.
 */
class SteamSource private constructor(
    private val apiKey: String?,
    private val user: String?,
    private val accessToken: String?,
    private val steamId: String?,
) {
    companion object {
        fun withApiKey(apiKey: String, user: String) = SteamSource(apiKey, user, null, null)
        fun withToken(token: String, steamId: String) = SteamSource(null, null, token, steamId)

        private const val ASSETS = "https://shared.akamai.steamstatic.com/store_item_assets/"

        /**
         * Géneros, estudio y año desde la tienda de Steam (no necesita clave).
         * La tienda limita a ~200 peticiones cada 5 minutos, así que se llama con pausas.
         */
        suspend fun storeDetails(game: Game): Game? {
            val appId = game.id.removePrefix("steam:")
            val data = Http.getJson("https://store.steampowered.com/api/appdetails?appids=$appId&l=spanish")[appId]
            if (data["success"].str != "true") return null
            val d = data["data"]
            val year = d["release_date"]["date"].str?.let { Regex("""(19|20)\d{2}""").find(it)?.value?.toInt() }
            return game.copy(
                genres = Genres.normalize(d["genres"].arr.mapNotNull { it["description"].str }),
                developer = game.developer ?: d["developers"][0].str,
                publisher = game.publisher ?: d["publishers"][0].str,
                releaseYear = year,
                description = d["short_description"].str,
                altCoverUrl = game.altCoverUrl ?: d["header_image"].str,
            )
        }
    }

    private fun auth(b: okhttp3.HttpUrl.Builder) = b.apply {
        if (apiKey != null) addQueryParameter("key", apiKey) else addQueryParameter("access_token", accessToken)
    }

    suspend fun fetchGames(): List<Game> {
        val id = steamId ?: resolveSteamId()
        val url = auth("https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/".toHttpUrl().newBuilder())
            .addQueryParameter("steamid", id)
            .addQueryParameter("include_appinfo", "1")
            .addQueryParameter("include_played_free_games", "1")
            .addQueryParameter("format", "json")
            .build()
        val games = Http.getJson(url.toString())["response"]["games"].arr
        if (games.isEmpty()) {
            throw IllegalStateException(
                if (apiKey != null) "Steam no devolvió juegos. En tu perfil de Steam, Privacidad → 'Detalles de juegos' debe estar en Público."
                else "Steam no devolvió juegos. Prueba a desvincular y volver a vincular tu cuenta."
            )
        }
        val list = games.mapNotNull { g ->
            val appId = g["appid"].long ?: return@mapNotNull null
            val name = g["name"].str ?: return@mapNotNull null
            Game(
                id = "steam:$appId",
                source = Source.STEAM,
                title = name,
                consoleId = "pc",
                playtimeMinutes = g["playtime_forever"].int ?: 0,
                lastPlayed = g["rtime_last_played"].long?.takeIf { it > 0 }?.times(1000),
                // Carátula vertical clásica; se corrige abajo con la ruta exacta de la tienda.
                coverUrl = "${ASSETS}steam/apps/$appId/library_600x900_2x.jpg",
                altCoverUrl = "${ASSETS}steam/apps/$appId/header.jpg",
                storeUrl = "https://store.steampowered.com/app/$appId",
            )
        }
        return withStoreCovers(list)
    }

    /**
     * Los juegos recientes guardan la carátula vertical en rutas con hash; la API de la tienda
     * (IStoreBrowseService) nos da la ruta exacta, además del estudio. Se piden de 50 en 50.
     */
    private suspend fun withStoreCovers(games: List<Game>): List<Game> {
        val info = mutableMapOf<String, Pair<String?, String?>>() // appid -> (carátula, estudio)
        games.chunked(50).forEach { chunk ->
            runCatching {
                val ids = chunk.joinToString(",") { """{"appid":${it.id.removePrefix("steam:")}}""" }
                val input = """{"ids":[$ids],"context":{"language":"spanish","country_code":"ES","steam_realm":1},""" +
                    """"data_request":{"include_assets":true,"include_basic_info":true}}"""
                val url = "https://api.steampowered.com/IStoreBrowseService/GetItems/v1/".toHttpUrl().newBuilder()
                    .addQueryParameter("input_json", input).build()
                Http.getJson(url.toString())["response"]["store_items"].arr.forEach { item ->
                    val appId = item["appid"].str ?: item["id"].str ?: return@forEach
                    val assets = item["assets"]
                    val format = assets["asset_url_format"].str
                    val file = assets["library_capsule_2x"].str ?: assets["library_capsule"].str
                    val cover = if (format != null && file != null) ASSETS + format.replace("\${FILENAME}", file) else null
                    val dev = item["basic_info"]["developers"][0]["name"].str
                    info[appId] = cover to dev
                }
            }
        }
        return games.map { g ->
            val (cover, dev) = info[g.id.removePrefix("steam:")] ?: return@map g
            g.copy(coverUrl = cover ?: g.coverUrl, developer = dev ?: g.developer)
        }
    }

    /** Acepta un SteamID64, un nombre personalizado o la URL completa del perfil. */
    private suspend fun resolveSteamId(): String {
        val clean = user.orEmpty().trim().trimEnd('/')
        Regex("""(\d{17})""").find(clean)?.let { return it.groupValues[1] }
        val vanity = clean.substringAfterLast("/id/").substringAfterLast('/')
        val url = auth("https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/".toHttpUrl().newBuilder())
            .addQueryParameter("vanityurl", vanity)
            .build()
        val resp = Http.getJson(url.toString())["response"]
        return resp["steamid"].str
            ?: throw IllegalArgumentException("No encontré el perfil de Steam \"$vanity\". Usa tu SteamID64 (17 dígitos).")
    }
}

/**
 * Vinculación de la cuenta de Steam iniciando sesión en la web oficial (dentro de un WebView).
 * Con la sesión iniciada, la propia tienda entrega un token web de corta duración que sirve para
 * consultar la biblioteca; la contraseña nunca pasa por Repisa.
 */
object SteamLogin {
    const val LOGIN_URL = "https://store.steampowered.com/login/?redirectDir=account%2F"
    private const val STORE = "https://store.steampowered.com"

    /** true cuando el WebView ya tiene la cookie de sesión de Steam. */
    fun isLoggedInWeb(): Boolean =
        CookieManager.getInstance().getCookie(STORE)?.contains("steamLoginSecure=") == true

    /** Pide a la tienda el token web de la sesión. null si la sesión ya no es válida. */
    suspend fun fetchWebToken(): String? {
        val cookies = withContext(Dispatchers.Main) { CookieManager.getInstance().getCookie(STORE) } ?: return null
        if (!cookies.contains("steamLoginSecure=")) return null
        val req = Request.Builder().url("$STORE/pointssummary/ajaxgetasyncconfig").header("Cookie", cookies).build()
        return runCatching { Http.getJson(req)["data"]["webapi_token"].str }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    /** El token es un JWT; su campo "sub" es el SteamID64 de la cuenta. */
    fun steamIdFromToken(token: String): String? = runCatching {
        val payload = token.split('.')[1]
        val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
        (Http.json.parseToJsonElement(json) as JsonObject)["sub"].str
    }.getOrNull()

    /** Nombre público del perfil (solo para mostrarlo en Ajustes). */
    suspend fun profileName(steamId: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            Http.client.newCall(Request.Builder().url("https://steamcommunity.com/profiles/$steamId/?xml=1").build())
                .execute().use { r ->
                    Regex("""<steamID><!\[CDATA\[(.*?)]]></steamID>""").find(r.body?.string().orEmpty())?.groupValues?.get(1)
                }
        }.getOrNull()
    }

    fun clearSession() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }
}
