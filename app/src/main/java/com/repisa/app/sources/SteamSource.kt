package com.repisa.app.sources

import com.repisa.app.data.Http
import com.repisa.app.data.arr
import com.repisa.app.data.get
import com.repisa.app.data.int
import com.repisa.app.data.long
import com.repisa.app.data.str
import com.repisa.app.model.Game
import com.repisa.app.model.Genres
import com.repisa.app.model.Source
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Biblioteca de Steam mediante la Steam Web API.
 * Necesita una clave de https://steamcommunity.com/dev/apikey y el SteamID del usuario.
 */
class SteamSource(private val apiKey: String, private val user: String) {

    suspend fun fetchGames(): List<Game> {
        val steamId = resolveSteamId()
        val url = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/".toHttpUrl().newBuilder()
            .addQueryParameter("key", apiKey)
            .addQueryParameter("steamid", steamId)
            .addQueryParameter("include_appinfo", "1")
            .addQueryParameter("include_played_free_games", "1")
            .addQueryParameter("format", "json")
            .build()
        val games = Http.getJson(url.toString())["response"]["games"].arr
        if (games.isEmpty()) {
            throw IllegalStateException(
                "Steam no devolvió juegos. Revisa que en tu perfil de Steam, Privacidad → 'Detalles de juegos' esté en Público."
            )
        }
        return games.mapNotNull { g ->
            val appId = g["appid"].long ?: return@mapNotNull null
            val name = g["name"].str ?: return@mapNotNull null
            Game(
                id = "steam:$appId",
                source = Source.STEAM,
                title = name,
                consoleId = "pc",
                playtimeMinutes = g["playtime_forever"].int ?: 0,
                lastPlayed = g["rtime_last_played"].long?.takeIf { it > 0 }?.times(1000),
                coverUrl = "https://shared.steamstatic.com/store_item_assets/steam/apps/$appId/library_600x900_2x.jpg",
                altCoverUrl = "https://cdn.cloudflare.steamstatic.com/steam/apps/$appId/library_600x900.jpg",
                storeUrl = "https://store.steampowered.com/app/$appId",
            )
        }
    }

    /** Acepta un SteamID64, un nombre personalizado o la URL completa del perfil. */
    private suspend fun resolveSteamId(): String {
        val clean = user.trim().trimEnd('/')
        Regex("""(\d{17})""").find(clean)?.let { return it.groupValues[1] }
        val vanity = clean.substringAfterLast("/id/").substringAfterLast('/')
        val url = "https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/".toHttpUrl().newBuilder()
            .addQueryParameter("key", apiKey)
            .addQueryParameter("vanityurl", vanity)
            .build()
        val resp = Http.getJson(url.toString())["response"]
        return resp["steamid"].str
            ?: throw IllegalArgumentException("No encontré el perfil de Steam \"$vanity\". Usa tu SteamID64 (17 dígitos).")
    }

    companion object {
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
                developer = d["developers"][0].str,
                publisher = d["publishers"][0].str,
                releaseYear = year,
                description = d["short_description"].str,
                altCoverUrl = game.altCoverUrl ?: d["header_image"].str,
            )
        }
    }
}
