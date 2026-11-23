package com.repisa.app.sources

import android.util.Base64
import com.repisa.app.data.Http
import com.repisa.app.data.Settings
import com.repisa.app.data.arr
import com.repisa.app.data.get
import com.repisa.app.data.long
import com.repisa.app.data.obj
import com.repisa.app.data.str
import com.repisa.app.model.Game
import com.repisa.app.model.Source
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.time.Instant

/**
 * Biblioteca de Epic Games usando los mismos servicios que el lanzador oficial
 * (el mismo método que usan Legendary y Heroic Games Launcher).
 *
 * El inicio de sesión devuelve un "authorizationCode" que se cambia por un token.
 */
class EpicSource(private val settings: Settings) {

    companion object {
        // Credenciales públicas del cliente del Epic Games Launcher, las mismas que usan Legendary y Heroic
        // (legendary/api/egs.py).
        private const val CLIENT_ID = "34a02cf8f4414e29b15921876da36f9a"
        private const val CLIENT_SECRET = "daafbccc737745039dffe53d94fc76cf"
        /** Epic espera que las peticiones vengan del lanzador. */
        private const val UA = "UELauncher/11.0.1-14907503+++Portal+Release-Live Windows/10.0.19041.1.256.64bit"
        private const val OAUTH = "https://account-public-service-prod03.ol.epicgames.com/account/api/oauth/token"
        private const val LIBRARY = "https://library-service.live.use1a.on.epicgames.com/library/api/public"
        private const val CATALOG = "https://catalog-public-service-prod06.ol.epicgames.com/catalog/api/shared/namespace"

        /** Página de login: al terminar muestra un JSON con "authorizationCode". */
        const val LOGIN_URL = "https://www.epicgames.com/id/login?redirectUrl=" +
            "https%3A%2F%2Fwww.epicgames.com%2Fid%2Fapi%2Fredirect%3FclientId%3D$CLIENT_ID%26responseType%3Dcode"

        // Cliente público de Epic que admite el inicio de sesión "con código" (device code), como en consolas y teles.
        private const val DEVICE_CLIENT_ID = "98f7e42c2e3a4f86a74eb43fbb41ed39"
        private const val DEVICE_CLIENT_SECRET = "0a2449a2-001a-451e-afec-3e812901c4d7"
        private const val OAUTH_BASE = "https://account-public-service-prod03.ol.epicgames.com/account/api/oauth"

        /** Extrae el código si el usuario pega el JSON entero en lugar de solo el código. */
        fun extractCode(text: String): String {
            Regex(""""authorizationCode"\s*:\s*"([^"]+)"""").find(text)?.let { return it.groupValues[1] }
            return text.trim().trim('"')
        }
    }

    private val basic = "basic " + Base64.encodeToString("$CLIENT_ID:$CLIENT_SECRET".toByteArray(), Base64.NO_WRAP)

    val isLoggedIn: Boolean get() = settings.epicRefreshToken != null

    // ------------------------------------------------------------------ Vincular con código

    data class DeviceCode(
        val userCode: String,
        val verificationUrl: String,
        val deviceCode: String,
        val intervalSec: Int,
        val expiresAt: Long,
    )

    private val deviceBasic = "basic " + Base64.encodeToString("$DEVICE_CLIENT_ID:$DEVICE_CLIENT_SECRET".toByteArray(), Base64.NO_WRAP)

    /** Pide a Epic un código para que el usuario lo introduzca en epicgames.com/activate. */
    suspend fun startDeviceLogin(): DeviceCode {
        val cc = Http.getJson(Request.Builder().header("User-Agent", UA).url("$OAUTH_BASE/token").header("Authorization", deviceBasic)
            .post(FormBody.Builder().add("grant_type", "client_credentials").build()).build())["access_token"].str
            ?: error("Epic no respondió")
        val r = Http.getJson(Request.Builder().header("User-Agent", UA).url("$OAUTH_BASE/deviceAuthorization").header("Authorization", "bearer $cc")
            .post(FormBody.Builder().add("prompt", "login").build()).build())
        return DeviceCode(
            userCode = r["user_code"].str ?: error("Epic no devolvió un código"),
            verificationUrl = r["verification_uri_complete"].str ?: "https://www.epicgames.com/activate",
            deviceCode = r["device_code"].str!!,
            intervalSec = (r["interval"].long ?: 5L).toInt().coerceAtLeast(3),
            expiresAt = System.currentTimeMillis() + (r["expires_in"].long ?: 600L) * 1000,
        )
    }

    /**
     * Comprueba si el usuario ya aceptó en la web. Devuelve true cuando la cuenta queda vinculada,
     * false si todavía está pendiente. Lanza una excepción si el código caducó o Epic lo rechazó.
     */
    suspend fun pollDeviceLogin(code: DeviceCode): Boolean {
        val resp = try {
            Http.getJson(Request.Builder().header("User-Agent", UA).url("$OAUTH_BASE/token").header("Authorization", deviceBasic)
                .post(FormBody.Builder().add("grant_type", "device_code").add("device_code", code.deviceCode).build()).build())
        } catch (e: com.repisa.app.data.HttpException) {
            if (e.body.contains("authorization_pending") || e.body.contains("slow_down")) return false
            throw IllegalStateException(epicError(e.body) ?: "Epic rechazó el código (${e.code})")
        }
        val deviceToken = resp["access_token"].str ?: return false
        // Lo ideal es cambiar el token "con código" por uno del lanzador de Epic. Si Epic no lo permite
        // (invalid_client_credentials), nos quedamos con la sesión del propio código.
        val launcherOk = runCatching {
            val exchange = Http.getJson(Request.Builder().header("User-Agent", UA).url("$OAUTH_BASE/exchange")
                .header("Authorization", "bearer $deviceToken").build())["code"].str ?: error("sin código de intercambio")
            tokenRequest(FormBody.Builder()
                .add("grant_type", "exchange_code")
                .add("exchange_code", exchange)
                .add("token_type", "eg1")
                .build())
        }.isSuccess
        if (!launcherOk) saveSession(resp, client = "device")
        return true
    }

    private fun epicError(body: String): String? =
        Regex(""""errorMessage"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)

    suspend fun login(authorizationCode: String) {
        try { loginInternal(authorizationCode) } catch (e: com.repisa.app.data.HttpException) {
            throw IllegalStateException(epicError(e.body) ?: "error ${e.code}")
        }
    }

    private suspend fun loginInternal(authorizationCode: String) {
        tokenRequest(FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", extractCode(authorizationCode))
            .add("token_type", "eg1")
            .build())
    }

    private suspend fun tokenRequest(body: FormBody, client: String = "launcher") {
        val req = Request.Builder().header("User-Agent", UA).url(OAUTH).header("Authorization", if (client == "device") deviceBasic else basic)
            .post(body).build()
        saveSession(Http.getJson(req), client)
    }

    private fun saveSession(r: JsonElement, client: String) {
        settings.epicClient = client
        settings.epicAccessToken = r["access_token"].str ?: error("Epic no devolvió token")
        settings.epicRefreshToken = r["refresh_token"].str
        settings.epicAccountId = r["account_id"].str
        settings.epicDisplayName = r["displayName"].str ?: settings.epicDisplayName
        settings.epicExpiresAt = r["expires_at"].str?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: (System.currentTimeMillis() + 3600_000)
    }

    private suspend fun ensureToken(): String {
        val token = settings.epicAccessToken
        if (token != null && System.currentTimeMillis() < settings.epicExpiresAt - 60_000) return token
        val refresh = settings.epicRefreshToken ?: error("No has iniciado sesión en Epic Games")
        tokenRequest(FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refresh)
            .add("token_type", "eg1")
            .build(), client = settings.epicClient)
        return settings.epicAccessToken!!
    }

    private suspend fun authGet(url: String): JsonElement = try {
        Http.getJson(Request.Builder().header("User-Agent", UA).url(url).header("Authorization", "bearer ${ensureToken()}").build())
    } catch (e: com.repisa.app.data.HttpException) {
        throw IllegalStateException(
            (epicError(e.body) ?: "error ${e.code}") +
                if (e.code == 401 || e.code == 403) " (Epic no deja leer la biblioteca con la sesión '${settings.epicClient}')" else ""
        )
    }

    /**
     * @param known juegos que ya tenemos guardados, para no volver a pedir su ficha al catálogo.
     */
    suspend fun fetchGames(known: Map<String, Game>): List<Game> = coroutineScope {
        // 1) Elementos de la biblioteca (paginados).
        data class Item(val namespace: String, val catalogItemId: String, val appName: String)
        val items = mutableListOf<Item>()
        var cursor: String? = null
        do {
            val url = "$LIBRARY/items".toHttpUrl().newBuilder()
                .addQueryParameter("includeMetadata", "true")
                .apply { cursor?.let { addQueryParameter("cursor", it) } }
                .build().toString()
            val page = authGet(url)
            page["records"].arr.forEach { r ->
                val ns = r["namespace"].str ?: return@forEach
                val cat = r["catalogItemId"].str ?: return@forEach
                val app = r["appName"].str ?: return@forEach
                if (ns != "ue" && r["sandboxType"].str != "PRIVATE") items += Item(ns, cat, app)
            }
            cursor = page["responseMetadata"]["nextCursor"].str
        } while (cursor != null)

        // 2) Tiempo de juego (segundos por appName).
        val playtime: Map<String, Long> = runCatching {
            authGet("$LIBRARY/playtime/account/${settings.epicAccountId}/all").arr
                .associate { (it["artifactId"].str ?: "") to (it["totalTime"].long ?: 0L) }
        }.getOrDefault(emptyMap())

        // 3) Ficha del catálogo (título, portada vertical, estudio). Solo para los que no conocemos.
        val limiter = Semaphore(6)
        val jobs = items.distinctBy { it.appName }.map { item ->
            async {
                val id = "epic:${item.appName}"
                val minutes = ((playtime[item.appName] ?: 0L) / 60).toInt()
                val old = known[id]
                if (old != null && old.coverUrl != null) {
                    return@async old.copy(playtimeMinutes = minutes)
                }
                val info = limiter.withPermit {
                    runCatching {
                        authGet("$CATALOG/${item.namespace}/bulk/items?id=${item.catalogItemId}" +
                            "&includeDLCDetails=true&includeMainGameDetails=true&country=ES&locale=es-ES")[item.catalogItemId]
                    }.getOrNull()
                } ?: return@async null
                // Saltar DLCs y contenidos que no son juegos.
                if (info["mainGameItem"].let { it != null && it !is JsonNull }) return@async null
                val categories = info["categories"].arr.mapNotNull { it["path"].str }
                if (categories.isNotEmpty() && categories.none { it == "games" || it == "applications" }) return@async null
                val images = info["keyImages"].arr.associate { (it["type"].str ?: "") to (it["url"].str ?: "") }
                val attrs = info["customAttributes"].obj
                val year = (info["releaseInfo"][0]["dateAdded"].str ?: info["creationDate"].str)
                    ?.take(4)?.toIntOrNull()
                Game(
                    id = id,
                    source = Source.EPIC,
                    title = info["title"].str ?: item.appName,
                    consoleId = "pc",
                    developer = info["developer"].str ?: attrs["developerName"]["value"].str,
                    publisher = attrs["publisherName"]["value"].str,
                    releaseYear = year,
                    description = info["description"].str,
                    playtimeMinutes = minutes,
                    coverUrl = images["DieselGameBoxTall"] ?: images["OfferImageTall"] ?: images["DieselStoreFrontTall"],
                    altCoverUrl = images["DieselGameBox"] ?: images["Thumbnail"] ?: images["OfferImageWide"],
                    storeUrl = "https://store.epicgames.com/es-ES/browse?q=" +
                        java.net.URLEncoder.encode(info["title"].str ?: item.appName, "UTF-8"),
                )
            }
        }
        jobs.mapNotNull { it.await() }
    }
}
