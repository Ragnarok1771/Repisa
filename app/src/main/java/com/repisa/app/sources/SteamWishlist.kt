package com.repisa.app.sources

import com.repisa.app.data.Http
import com.repisa.app.data.arr
import com.repisa.app.data.get
import com.repisa.app.data.int
import com.repisa.app.data.long
import com.repisa.app.data.obj
import com.repisa.app.data.str
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Un juego de la lista de deseos de Steam con su precio actual (null = gratis o sin precio todavía). */
@Serializable
data class WishItem(
    val appId: Long,
    val name: String,
    val cover: String? = null,
    val addedAt: Long = 0,
    val priceCents: Long? = null,
    val price: String? = null,
    val originalPrice: String? = null,
    val discountPct: Int = 0,
)

/**
 * Lista de deseos de Steam:
 *  1. IWishlistService/GetWishlist → los appid (pública sin más; privada, con la sesión de Steam vinculada).
 *  2. IStoreBrowseService/GetItems → nombre y carátula vertical.
 *  3. store/api/appdetails?filters=price_overview → precio, precio original y descuento en la moneda del país.
 */
object SteamWishlist {
    private const val ASSETS = "https://shared.akamai.steamstatic.com/store_item_assets/"

    suspend fun fetch(steamId: String, accessToken: String?, country: String, language: String = "spanish"): List<WishItem> {
        fun url(token: String?) = "https://api.steampowered.com/IWishlistService/GetWishlist/v1/".toHttpUrl().newBuilder()
            .addQueryParameter("steamid", steamId)
            .apply { if (token != null) addQueryParameter("access_token", token) }
            .build().toString()
        // Con la sesión (sirve aunque la lista sea privada); si Steam rechaza la sesión, sin ella (lista pública).
        val json = if (accessToken == null) Http.getJson(url(null))
            else runCatching { Http.getJson(url(accessToken)) }.getOrElse { Http.getJson(url(null)) }
        val entries = json["response"]["items"].arr.mapNotNull { e ->
            val id = e["appid"].long ?: return@mapNotNull null
            id to ((e["date_added"].long ?: 0L) * 1000)
        }
        if (entries.isEmpty()) return emptyList()
        val ids = entries.map { it.first }
        val names = mutableMapOf<Long, Pair<String, String?>>()
        // Precio de la «mejor opción de compra» para cualquiera (la que muestra la tienda): con juegos que tienen
        // varias versiones (Yakuza Kiwami: la actualización para quien tiene la Legacy a 1.990 y el juego a 18.990)
        // la otra consulta devolvía la primera que encontraba, que no siempre es la buena.
        val best = mutableMapOf<Long, Price>()
        ids.chunked(50).forEach { chunk ->
            runCatching {
                val list = chunk.joinToString(",") { """{"appid":$it}""" }
                val input = """{"ids":[$list],"context":{"language":"$language","country_code":"$country","steam_realm":1},""" +
                    """"data_request":{"include_assets":true}}"""
                val u = "https://api.steampowered.com/IStoreBrowseService/GetItems/v1/".toHttpUrl().newBuilder()
                    .addQueryParameter("input_json", input).build()
                Http.getJson(u.toString())["response"]["store_items"].arr.forEach { item ->
                    val id = item["appid"].long ?: item["id"].long ?: return@forEach
                    val name = item["name"].str ?: return@forEach
                    val format = item["assets"]["asset_url_format"].str
                    // Los DLC no suelen tener carátula vertical: se usa la cápsula o la cabecera de la tienda.
                    val cap = item["assets"]["library_capsule"].str ?: item["assets"]["main_capsule"].str ?: item["assets"]["header"].str
                    names[id] = name to (if (format != null && cap != null) ASSETS + format.replace("\${FILENAME}", cap) else null)
                    val bp = item["best_purchase_option"]
                    val cents = bp["final_price_in_cents"].long
                    if (cents != null) best[id] = Price(cents, bp["formatted_final_price"].str.orEmpty(),
                        bp["formatted_original_price"].str?.ifBlank { null }, bp["discount_pct"].int ?: 0)
                }
            }
        }
        // Solo para los que la tienda no dio la mejor opción, se pregunta de la otra forma.
        val prices = best + prices(ids.filter { it !in best }, country)
        return entries.mapNotNull { (id, added) ->
            val (name, cover) = names[id] ?: return@mapNotNull null
            val p = prices[id]
            WishItem(id, name, cover ?: "https://cdn.cloudflare.steamstatic.com/steam/apps/$id/library_600x900.jpg", added,
                p?.final, p?.finalText, p?.initialText?.takeIf { p.discount > 0 }, p?.discount ?: 0)
        }
    }

    private data class Price(val final: Long, val finalText: String, val initialText: String?, val discount: Int)

    /** Precios de muchos juegos a la vez (la tienda admite varios appid con filters=price_overview). */
    private suspend fun prices(ids: List<Long>, country: String): Map<Long, Price> {
        val out = mutableMapOf<Long, Price>()
        ids.chunked(80).forEachIndexed { i, chunk ->
            // Una pausa entre consultas y un reintento: la tienda frena las peticiones muy seguidas.
            if (i > 0) kotlinx.coroutines.delay(300)
            repeat(2) { attempt ->
            if (attempt > 0) kotlinx.coroutines.delay(2000)
            val ok = runCatching {
                val json = Http.getJson("https://store.steampowered.com/api/appdetails?appids=${chunk.joinToString(",")}" +
                    "&filters=price_overview&cc=${country.lowercase()}")
                json.obj.forEach { (k, v) ->
                    val po = v["data"]["price_overview"] ?: return@forEach
                    val final = po["final"].long ?: return@forEach
                    out[k.toLong()] = Price(final, po["final_formatted"].str.orEmpty(),
                        po["initial_formatted"].str?.ifBlank { null }, po["discount_percent"].int ?: 0)
                }
            }.isSuccess
            if (ok) return@forEachIndexed
            }
        }
        return out
    }
}
