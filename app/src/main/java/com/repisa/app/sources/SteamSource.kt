package com.repisa.app.sources

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import com.repisa.app.data.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.FormBody
import java.net.URLDecoder
import android.webkit.CookieManager
import com.repisa.app.R
import com.repisa.app.data.Http
import com.repisa.app.i18n.L10n
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

        /** [isGame]: true si la tienda lo marca como juego, false si es otra cosa, null si no está en la tienda. */
        data class StoreInfo(val cover: String?, val header: String?, val developer: String?, val isGame: Boolean?)

        /** Nombre (en inglés) de cada etiqueta de Steam: se pide una vez por sesión. */
        @Volatile private var tagNames: Map<Long, String>? = null

        private suspend fun tagNames(): Map<Long, String> = tagNames ?: Http.getJson(
            "https://api.steampowered.com/IStoreService/GetTagList/v1/?language=english"
        )["response"]["tags"].arr.mapNotNull { t -> t["tagid"].long?.let { id -> t["name"].str?.let { id to it } } }
            .toMap().also { if (it.isNotEmpty()) tagNames = it }

        /** Géneros por appid, y si algún bloque no se pudo pedir (para reintentarlo más tarde). */
        data class TagGenres(val genres: Map<Long, List<String>>, val failed: Boolean)

        /**
         * Géneros de muchos juegos a la vez a partir de las etiquetas que pone la comunidad de Steam
         * («JRPG», «FPS», «Souls-like»...), mucho más precisas que los géneros de la ficha («Acción», «Rol»).
         * Se piden de 50 en 50; si un bloque falla, los demás se aprovechan igual.
         */
        suspend fun tagGenres(appIds: List<Long>): TagGenres {
            val names = runCatching { tagNames() }.getOrElse { return TagGenres(emptyMap(), true) }
            val out = mutableMapOf<Long, List<String>>()
            var failed = false
            appIds.distinct().chunked(50).forEach { chunk ->
                val ids = chunk.joinToString(",") { """{"appid":$it}""" }
                val input = """{"ids":[$ids],"context":{"language":"english","country_code":"US","steam_realm":1},""" +
                    """"data_request":{"include_tag_count":20}}"""
                val url = "https://api.steampowered.com/IStoreBrowseService/GetItems/v1/".toHttpUrl().newBuilder()
                    .addQueryParameter("input_json", input).build()
                val items = runCatching { Http.getJson(url.toString())["response"]["store_items"].arr }
                    .getOrElse { failed = true; return@forEach }
                items.forEach { item ->
                    val appId = item["appid"].long ?: return@forEach
                    val tags = item["tags"].arr.mapNotNull { t ->
                        val name = t["tagid"].long?.let(names::get) ?: return@mapNotNull null
                        name to (t["weight"].long ?: 0L).toInt()
                    }
                    Genres.fromSteamTags(tags).takeIf { it.isNotEmpty() }?.let { out[appId] = it }
                }
            }
            return TagGenres(out, failed)
        }

        /** Resultado de buscar un juego en Steam por su nombre: su appid y los géneros de sus etiquetas. */
        data class NameMatch(val appId: Long, val genres: List<String>)

        private val notGame = Regex("(?i)\\b(soundtrack|ost|dlc|season pass|demo|playtest|public test|public testing|shadows mode|artbook|wallpaper)\\b")
        private fun compact(t: String) = CoverSearch.norm(EmulatorSource.cleanTitle(t)).filter { it.isLetterOrDigit() }
        /** Sin limpiar paréntesis ni corchetes: «Rustler (Grand Theft Horse)» = «Rustler Grand Theft Horse». */
        private fun raw(t: String) = java.text.Normalizer.normalize(t.replace(tm, ""), java.text.Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "").lowercase().filter { it.isLetterOrDigit() }
        private val tm = Regex("[™®©℠]")

        /**
         * Busca un juego en Steam por su nombre con el buscador de sugerencias de la tienda (IStoreQueryService, sin clave,
         * tolerante: encuentra «G-MODEArchives+ Persona3...» escribiendo «G-MODE Archives+ Persona 3...») y devuelve en la
         * misma respuesta sus etiquetas. Si con el nombre completo no sale, prueba sin las últimas palabras
         * («Nightingale Legacy Mode» → «Nightingale», «Chivalry 2 Public Testing» → «Chivalry 2») y sin las primeras.
         * Devuelve null si no lo encuentra; lanza una excepción si Steam no responde.
         */
        suspend fun searchByName(title: String): NameMatch? {
            val names = runCatching { tagNames() }.getOrElse { throw it }
            val clean = EmulatorSource.cleanTitle(title).replace(Regex("[™®©℠]"), "").trim()
            val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
            // Nombre completo; sin palabras de edición; sin coletillas al final («Legacy Mode», «Public Testing»,
            // «25th Anniversary»: solo esas, para no convertir «Rocket League» en «Rocket»); sin las primeras palabras.
            val core = CoverSearch.norm(clean).split(' ').filter { it.isNotEmpty() && it !in editionWords }.joinToString(" ")
            val suffixless = buildList {
                var w = words
                while (w.size > 1 && isSuffix(w.last())) { w = w.dropLast(1); add(w.joinToString(" ").trimEnd(':', '-', '–', ',')) }
            }
            val prefixless = (1..2).map { words.drop(it) }.filter { it.size >= 4 }.map { it.joinToString(" ") }
            val terms = (listOf(clean, core) + suffixless + prefixless).distinct()
            val wanted = CoverSearch.tokens(clean) - editionWords
            for ((i, term) in terms.withIndex()) {
                if (i > 0) kotlinx.coroutines.delay(250)
                val input = """{"search_term":${kotlinx.serialization.json.JsonPrimitive(term)},"max_results":8,""" +
                    """"context":{"language":"english","country_code":"US","steam_realm":1},"data_request":{"include_tag_count":20}}"""
                val url = "https://api.steampowered.com/IStoreQueryService/SearchSuggestions/v1/".toHttpUrl().newBuilder()
                    .addQueryParameter("input_json", input).build()
                val items = Http.getJson(url.toString())["response"]["store_items"].arr
                val prefixCut = term in prefixless && term != clean && term != core
                val shortened = term != clean && term != core
                val best = items.filter { it["type"].long == 0L && !notGame.containsMatchIn(it["name"].str.orEmpty()) }.mapNotNull { item ->
                    val name = item["name"].str ?: return@mapNotNull null
                    val got = CoverSearch.tokens(name) - editionWords
                    val score = when {
                        // Idéntico (con corchetes y todo) gana a igual sin espacios: «[REDACTED]» antes que «REDACTED».
                        name.replace(tm, "").trim().equals(title.replace(tm, "").trim(), ignoreCase = true) -> 1.1
                        compact(name) == compact(clean) || compact(name) == compact(term) || raw(name) == raw(title) -> 1.0
                        // Sin las primeras palabras: que el nombre de Steam termine igual («G-MODEArchives+ Persona3 Aigis...»).
                        prefixCut && compact(term).length >= 12 && compact(name).endsWith(compact(term)) -> 0.95
                        // Con el nombre recortado solo vale si coincide entero (es el juego base).
                        shortened -> 0.0
                        wanted.size >= 2 && got.containsAll(wanted) && got.size - wanted.size <= 3 -> 0.9
                        else -> CoverSearch.similarity(wanted, got)
                    }
                    (item to score).takeIf { score >= 0.85 }
                }.maxByOrNull { it.second }?.first ?: continue
                val appId = best["appid"].long ?: best["id"].long ?: continue
                val tags = best["tags"].arr.mapNotNull { t ->
                    val name = t["tagid"].long?.let(names::get) ?: return@mapNotNull null
                    name to (t["weight"].long ?: 0L).toInt()
                }
                return NameMatch(appId, Genres.fromSteamTags(tags))
            }
            return null
        }

        /** Coletillas que se pueden quitar del final para dar con el juego base. */
        private fun isSuffix(w: String): Boolean {
            val x = CoverSearch.norm(w).replace(" ", "")
            return x.isEmpty() || x in suffixWords || Regex("""\d+(st|nd|rd|th)""").matches(x)
        }
        private val suffixWords = setOf("legacy", "mode", "public", "testing", "test", "beta", "playtest", "edition", "goty", "game",
            "of", "the", "year", "remastered", "remaster", "anniversary", "deluxe", "definitive", "complete", "enhanced", "directors",
            "director", "cut", "collection", "bundle", "ultimate", "gold", "premium", "special", "standard", "regeneration", "hd",
            "version", "trial", "online", "multiplayer", "singleplayer", "classic", "reloaded", "redux")

        private val editionWords = setOf("goty", "game", "of", "the", "year", "edition", "definitive", "complete", "deluxe", "ultimate",
            "remastered", "standard", "enhanced", "directors", "cut", "collection", "anniversary", "gold", "premium", "special")

        /** Datos de la tienda (IStoreBrowseService, sin clave) para muchos juegos a la vez, de 50 en 50. */
        suspend fun storeInfo(appIds: List<Long>): Map<Long, StoreInfo> {
            val out = mutableMapOf<Long, StoreInfo>()
            appIds.distinct().chunked(50).forEach { chunk ->
                runCatching {
                    val ids = chunk.joinToString(",") { """{"appid":$it}""" }
                    val input = """{"ids":[$ids],"context":{"language":"spanish","country_code":"ES","steam_realm":1},""" +
                        """"data_request":{"include_assets":true,"include_basic_info":true}}"""
                    val url = "https://api.steampowered.com/IStoreBrowseService/GetItems/v1/".toHttpUrl().newBuilder()
                        .addQueryParameter("input_json", input).build()
                    Http.getJson(url.toString())["response"]["store_items"].arr.forEach { item ->
                        val appId = item["appid"].long ?: item["id"].long ?: return@forEach
                        if (item["success"].long != 1L) {
                            item["id"].long?.let { out[it] = StoreInfo(null, null, null, null) }
                            return@forEach
                        }
                        val assets = item["assets"]
                        val format = assets["asset_url_format"].str
                        fun asset(name: String?) = if (format != null && name != null) ASSETS + format.replace("\${FILENAME}", name) else null
                        out[appId] = StoreInfo(
                            // 600×900 basta para la caja; la versión _2x pesa 4 veces más y daba tirones.
                            cover = asset(assets["library_capsule"].str ?: assets["library_capsule_2x"].str),
                            header = asset(assets["header"].str),
                            developer = item["basic_info"]["developers"][0]["name"].str,
                            isGame = item["type"].long?.let { it == 0L },
                        )
                    }
                }
            }
            return out
        }

        /** Carátula vertical oficial de cada appid. */
        suspend fun libraryCovers(appIds: List<Long>): Map<Long, String> =
            storeInfo(appIds).mapNotNull { (k, v) -> v.cover?.let { k to it } }.toMap()

        /**
         * Géneros, estudio y año desde la tienda de Steam (no necesita clave).
         * La tienda limita a ~200 peticiones cada 5 minutos, así que se llama con pausas.
         */
        data class Facts(val genres: List<String>, val developer: String?, val publisher: String?, val year: Int?,
                         val description: String?, val header: String?)

        /**
         * Género, estudio, editora, año y descripción de la ficha de la tienda de Steam. Devuelve null si el juego ya
         * no está en la tienda; lanza una excepción si Steam no responde (límite de peticiones), para reintentarlo.
         */
        suspend fun appFacts(appId: String): Facts? {
            val data = Http.getJson("https://store.steampowered.com/api/appdetails?appids=$appId&l=spanish")[appId]
            if (data["success"].str != "true") return null
            val d = data["data"]
            return Facts(
                genres = d["genres"].arr.mapNotNull { it["description"].str },
                developer = d["developers"].arr.mapNotNull { it.str }.take(2).joinToString(", ").ifEmpty { null },
                publisher = d["publishers"][0].str,
                year = d["release_date"]["date"].str?.let { Regex("""(19|20)\d{2}""").find(it)?.value?.toInt() },
                description = d["short_description"].str,
                header = d["header_image"].str,
            )
        }

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
                // La cabecera de appdetails trae la ruta exacta (con hash en juegos recientes).
                altCoverUrl = d["header_image"].str ?: game.altCoverUrl,
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
                if (apiKey != null) L10n.s(R.string.err_steam_no_games_key) else L10n.s(R.string.err_steam_no_games_linked)
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
                coverUrl = "${ASSETS}steam/apps/$appId/library_600x900.jpg",
                altCoverUrl = "${ASSETS}steam/apps/$appId/header.jpg",
                storeUrl = "https://store.steampowered.com/app/$appId",
            )
        }
        val own = list.mapNotNull { it.id.removePrefix("steam:").toLongOrNull() }.toSet()
        familyError = null
        val family = runCatching { familyGames(id, own) }
            .onFailure { familyError = it.message }
            .getOrDefault(emptyList())
        return withStoreCovers(list + family)
    }

    /** Si falló la lectura de la biblioteca familiar, el motivo (para avisar sin perder el resto). */
    var familyError: String? = null
        private set

    /** Corrige carátula, cabecera y estudio con los datos exactos de la tienda. */
    private suspend fun withStoreCovers(games: List<Game>): List<Game> {
        val info = storeInfo(games.mapNotNull { it.id.removePrefix("steam:").toLongOrNull() })
        return games.mapNotNull { g ->
            val i = info[g.id.removePrefix("steam:").toLongOrNull()]
            // Fuera herramientas: lo que la tienda marca como no-juego, o lo que no está en la tienda y
            // tiene nombre de herramienta (servidores dedicados, kits de mods, editores...).
            if (i?.isGame == false) return@mapNotNull null
            if (i?.isGame != true && com.repisa.app.model.Tools.looksLikeTool(g.title)) return@mapNotNull null
            if (i == null) return@mapNotNull g
            g.copy(coverUrl = i.cover ?: g.coverUrl, altCoverUrl = i.header ?: g.altCoverUrl, developer = i.developer ?: g.developer)
        }
    }

    // ------------------------------------------------------------------ Grupo familiar

    /**
     * Juegos compartidos por el grupo familiar de Steam (solo con cuenta vinculada, no con clave de API).
     * Sus horas salen de ClientGetLastPlayedTimes, que incluye también lo jugado en juegos prestados.
     */
    private suspend fun familyGames(myId: String, own: Set<Long>): List<Game> {
        if (accessToken == null) return emptyList()
        fun url(method: String) = "https://api.steampowered.com/$method/".toHttpUrl().newBuilder()
            .addQueryParameter("access_token", accessToken)
        val groupId = Http.getJson(url("IFamilyGroupsService/GetFamilyGroupForUser/v1")
            .addQueryParameter("steamid", myId).build().toString())["response"]["family_groupid"].str
            ?.takeIf { it != "0" } ?: return emptyList()
        val apps = Http.getJson(url("IFamilyGroupsService/GetSharedLibraryApps/v1")
            .addQueryParameter("family_groupid", groupId)
            .addQueryParameter("include_own", "false")
            .addQueryParameter("include_excluded", "false")
            .addQueryParameter("include_free", "false")
            .addQueryParameter("include_non_games", "false")
            .addQueryParameter("steamid", myId)
            .build().toString())["response"]["apps"].arr
        val played = runCatching { lastPlayedTimes() }.getOrDefault(emptyMap())
        return apps.mapNotNull { a ->
            val appId = a["appid"].long ?: return@mapNotNull null
            if (appId in own) return@mapNotNull null
            val name = a["name"].str ?: return@mapNotNull null
            val p = played[appId]
            Game(
                id = "steam:$appId",
                source = Source.STEAM,
                title = name,
                consoleId = "pc",
                playtimeMinutes = p?.first ?: 0,
                lastPlayed = p?.second ?: a["rt_last_played"].long?.takeIf { it > 0 }?.times(1000),
                coverUrl = "${ASSETS}steam/apps/$appId/library_600x900.jpg",
                altCoverUrl = "${ASSETS}steam/apps/$appId/header.jpg",
                storeUrl = "https://store.steampowered.com/app/$appId",
                familyShared = true,
            )
        }
    }

    /** appid → (minutos jugados, última partida en ms) de todo lo jugado en la cuenta, propio o prestado. */
    private suspend fun lastPlayedTimes(): Map<Long, Pair<Int, Long?>> {
        val url = "https://api.steampowered.com/IPlayerService/ClientGetLastPlayedTimes/v1/".toHttpUrl().newBuilder()
            .addQueryParameter("access_token", accessToken)
            .addQueryParameter("min_last_played", "0").build()
        return Http.getJson(url.toString())["response"]["games"].arr.mapNotNull { g ->
            val id = g["appid"].long ?: return@mapNotNull null
            id to ((g["playtime_forever"].long ?: 0L).toInt() to g["last_playtime"].long?.takeIf { it > 0 }?.times(1000))
        }.toMap()
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
 * La contraseña nunca pasa por Repisa: solo se guardan las cookies que deja Steam.
 *
 * La sesión de Steam tiene dos partes: un pase corto (cookie `steamLoginSecure`, dura horas) y un pase largo de
 * renovación (cookie `steamRefresh_steam` de login.steampowered.com, dura meses). Cuando el corto caduca, Repisa lo
 * renueva sola con el largo, igual que hace un navegador al volver a abrir la tienda; así no hay que volver a vincular.
 */
object SteamLogin {
    const val LOGIN_URL = "https://store.steampowered.com/login/?redirectDir=account%2F"
    private const val STORE = "https://store.steampowered.com"
    private const val LOGIN = "https://login.steampowered.com"
    private const val REFRESH_COOKIE = "steamRefresh_steam"
    private const val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    private lateinit var appContext: Context
    private val lock = Mutex()
    @Volatile private var cached: String? = null

    /** Último motivo por el que no se pudo obtener la sesión (para el mensaje de error). */
    @Volatile var lastProblem: String? = null
        private set

    fun init(context: Context) { appContext = context.applicationContext }

    private fun cookieValue(cookies: String?, name: String): String? =
        cookies?.split(';')?.map { it.trim() }?.firstOrNull { it.startsWith("$name=") }?.substringAfter('=')?.takeIf { it.isNotBlank() }

    private suspend fun cookies(url: String): String? = withContext(Dispatchers.Main) { CookieManager.getInstance().getCookie(url) }

    /**
     * País de la tienda de Steam de la cuenta (cookie `steamCountry`, p. ej. «CL|…»): con él los precios salen
     * en la moneda de la persona (pesos chilenos, dólares, euros...).
     */
    suspend fun storeCountry(): String? = runCatching {
        cookieValue(cookies(STORE), "steamCountry")?.let { URLDecoder.decode(it, "UTF-8") }?.substringBefore('|')
            ?.uppercase()?.takeIf { it.length == 2 }
    }.getOrNull()

    /** true cuando el WebView ya tiene la cookie de sesión de Steam (aunque esté caducada). */
    fun isLoggedInWeb(): Boolean =
        CookieManager.getInstance().getCookie(STORE)?.contains("steamLoginSecure=") == true

    /** El pase corto que va dentro de `steamLoginSecure` («steamid||jwt»). */
    private fun accessFromCookie(cookies: String?): String? =
        cookieValue(cookies, "steamLoginSecure")?.let { URLDecoder.decode(it, "UTF-8") }?.substringAfter("||", "")?.takeIf { it.count { c -> c == '.' } == 2 }

    private fun jwtPayload(token: String): JsonObject? = runCatching {
        val json = String(Base64.decode(token.split('.')[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
        Http.json.parseToJsonElement(json) as JsonObject
    }.getOrNull()

    /** true si el token no caduca en los próximos 2 minutos (si no trae fecha, se da por bueno). */
    private fun fresh(token: String): Boolean {
        val exp = jwtPayload(token)?.get("exp").long ?: return true
        return exp * 1000 > System.currentTimeMillis() + 120_000
    }

    /** true si la sesión del WebView es válida ahora mismo (el pase corto no ha caducado). */
    fun hasFreshWebSession(): Boolean =
        accessFromCookie(CookieManager.getInstance().getCookie(STORE))?.let(::fresh) == true

    /** El token es un JWT; su campo "sub" es el SteamID64 de la cuenta. */
    fun steamIdFromToken(token: String): String? = jwtPayload(token)?.get("sub").str

    /**
     * Token web válido para las API de Steam, renovando la sesión si hace falta. null si Steam ya no la acepta
     * (entonces sí hay que volver a vincular; [lastProblem] dice por qué).
     */
    suspend fun token(settings: Settings): String? = lock.withLock {
        cached?.takeIf(::fresh)?.let { return it }
        restoreRefreshCookie(settings)
        val problems = mutableListOf<String>()
        fun found(t: String): String { cached = t; lastProblem = null; return t }
        tokenFromCookies(problems)?.let { saveRefreshCookie(settings); return found(it) }
        // El pase corto caducó: se renueva con el largo, primero directamente y si no, abriendo la tienda por detrás.
        if (refreshDirect(problems)) tokenFromCookies(problems)?.let { saveRefreshCookie(settings); return found(it) }
        if (refreshWithWebView(problems)) tokenFromCookies(problems)?.let { saveRefreshCookie(settings); return found(it) }
        lastProblem = problems.distinct().joinToString("; ").ifBlank { null }
        null
    }

    /** Saca el token de la sesión actual, por tres caminos (si Steam cambia uno, quedan los otros). */
    private suspend fun tokenFromCookies(problems: MutableList<String>): String? {
        val cookies = cookies(STORE)
        val access = accessFromCookie(cookies)
        if (access == null) { problems += "sin sesión"; return null }
        if (!fresh(access)) { problems += "sesión caducada"; return null }
        // 1) El servicio de la tienda que entrega el token web.
        runCatching {
            val req = Request.Builder().url("$STORE/pointssummary/ajaxgetasyncconfig").header("Cookie", cookies!!).header("User-Agent", UA).build()
            Http.getJson(req)["data"]["webapi_token"].str
        }.onFailure { problems += "asyncconfig: ${it.message?.take(60)}" }.getOrNull()?.takeIf { it.isNotBlank() && fresh(it) }?.let { return it }
        // 2) La configuración que la tienda incrusta en sus páginas.
        runCatching {
            withContext(Dispatchers.IO) {
                Http.client.newCall(Request.Builder().url("$STORE/account/").header("Cookie", cookies!!).header("User-Agent", UA).build())
                    .execute().use { r ->
                        val html = r.body?.string().orEmpty()
                        Regex("""webapi_token&quot;:&quot;([A-Za-z0-9_\-.]+)&quot;""").find(html)?.groupValues?.get(1)
                            ?: Regex(""""webapi_token"\s*:\s*"([A-Za-z0-9_\-.]+)"""").find(html)?.groupValues?.get(1)
                    }
            }
        }.getOrNull()?.takeIf { fresh(it) }?.let { return it }
        // 3) El propio pase de la cookie, que las API de Steam también aceptan.
        return access
    }

    /** Copia cifrada del pase de renovación, por si el WebView pierde sus cookies. */
    private suspend fun saveRefreshCookie(settings: Settings) {
        cookieValue(cookies(LOGIN), REFRESH_COOKIE)?.let { if (it != settings.steamRefreshCookie) settings.steamRefreshCookie = it }
    }

    private suspend fun restoreRefreshCookie(settings: Settings) {
        val saved = settings.steamRefreshCookie ?: return
        if (cookieValue(cookies(LOGIN), REFRESH_COOKIE) != null) return
        withContext(Dispatchers.Main) {
            CookieManager.getInstance().setCookie(LOGIN, "$REFRESH_COOKIE=$saved; Path=/; Secure; HttpOnly; Max-Age=${200L * 24 * 3600}")
            CookieManager.getInstance().flush()
        }
    }

    /** Renovación directa: lo mismo que hace la web de Steam por dentro (jwt/ajaxrefresh y luego settoken). */
    private suspend fun refreshDirect(problems: MutableList<String>): Boolean = runCatching {
        val loginCookies = cookies(LOGIN)
        if (cookieValue(loginCookies, REFRESH_COOKIE) == null) { problems += "sin pase de renovación"; return false }
        val r = Http.getJson(Request.Builder().url("$LOGIN/jwt/ajaxrefresh").header("Cookie", loginCookies!!)
            .header("User-Agent", UA).header("Origin", STORE).header("Referer", "$STORE/")
            .post(FormBody.Builder().add("redir", "$STORE/").build()).build())
        if (r["success"]?.toString() != "true") { problems += "Steam no renovó la sesión (${r["error"].str ?: "?"})"; return false }
        val target = r["login_url"].str ?: "$STORE/login/settoken"
        val form = FormBody.Builder()
        listOf("steamID", "nonce", "redir", "auth").forEach { k -> r[k].str?.let { form.add(k, it) } }
        val setCookies = withContext(Dispatchers.IO) {
            Http.client.newBuilder().followRedirects(false).build().newCall(
                Request.Builder().url(target).header("Cookie", cookies(STORE).orEmpty()).header("User-Agent", UA)
                    .header("Origin", STORE).header("Referer", "$STORE/").post(form.build()).build()
            ).execute().use { it.headers("Set-Cookie") }
        }
        withContext(Dispatchers.Main) {
            val cm = CookieManager.getInstance()
            setCookies.forEach { cm.setCookie(STORE, it) }
            cm.flush()
        }
        hasFreshWebSession().also { if (!it) problems += "renovación directa sin efecto" }
    }.getOrElse { problems += "renovación: ${it.message?.take(60)}"; false }

    /** Renovación como un navegador: abre la tienda en un WebView invisible y deja que Steam renueve la cookie. */
    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun refreshWithWebView(problems: MutableList<String>): Boolean {
        if (!::appContext.isInitialized) return false
        val ok = withContext(Dispatchers.Main) {
            val web = runCatching {
                WebView(appContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = settings.userAgentString.replace("; wv", "").replace(Regex("""Version/\d+\.\d+\s*"""), "")
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = WebViewClient()
                    loadUrl("$STORE/account/")
                }
            }.getOrElse { problems += "WebView: ${it.message?.take(60)}"; return@withContext false }
            try {
                withTimeoutOrNull(25_000) {
                    while (!hasFreshWebSession()) delay(700)
                    true
                } ?: false
            } finally {
                CookieManager.getInstance().flush()
                web.stopLoading()
                web.destroy()
            }
        }
        if (!ok) problems += "la tienda no renovó la sesión"
        return ok
    }

    /** Nombre público del perfil (solo para mostrarlo en Ajustes). */
    suspend fun profileName(steamId: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            Http.client.newCall(Request.Builder().url("https://steamcommunity.com/profiles/$steamId/?xml=1").build())
                .execute().use { r ->
                    Regex("""<steamID><!\[CDATA\[(.*?)]]></steamID>""").find(r.body?.string().orEmpty())?.groupValues?.get(1)
                }
        }.getOrNull()
    }

    /** Olvida la sesión de Steam (cookies de Steam y copia del pase de renovación). */
    fun clearSession(settings: Settings? = null) {
        cached = null
        settings?.steamRefreshCookie = null
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }
}
