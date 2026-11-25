package com.repisa.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.repisa.app.R
import com.repisa.app.i18n.L10n
import com.repisa.app.sources.WishItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/** Lista de deseos guardada (para verla sin conexión) y avisos de bajada de precio. */
class WishlistStore(private val context: Context) {
    private val file = File(context.filesDir, "wishlist.json")
    private val serializer = ListSerializer(WishItem.serializer())

    suspend fun load(): List<WishItem> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        runCatching { Http.json.decodeFromString(serializer, file.readText()) }.getOrDefault(emptyList())
    }

    suspend fun save(items: List<WishItem>) = withContext(Dispatchers.IO) {
        val tmp = File(file.parentFile, "wishlist.json.tmp")
        tmp.writeText(Http.json.encodeToString(serializer, items))
        tmp.renameTo(file)
    }

    /** Juegos que ahora cuestan menos que la última vez que se miró (mismo juego, precio conocido antes y ahora). */
    fun drops(old: List<WishItem>, fresh: List<WishItem>): List<Pair<WishItem, WishItem>> {
        val before = old.associateBy { it.appId }
        return fresh.mapNotNull { n ->
            val o = before[n.appId] ?: return@mapNotNull null
            if (o.priceCents != null && n.priceCents != null && n.priceCents < o.priceCents) o to n else null
        }
    }

    /** Notificación «ha bajado de precio» que abre la página de la tienda. */
    fun notifyDrop(old: WishItem, new: WishItem) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, L10n.s(R.string.wish_channel), NotificationManager.IMPORTANCE_DEFAULT))
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            context, new.appId.toInt(),
            // En la app de Steam si está instalada; si no, en el navegador.
            Intent(Intent.ACTION_VIEW, Uri.parse("https://store.steampowered.com/app/${new.appId}")).let { i ->
                val steam = Intent(i).setPackage("com.valvesoftware.android.steam.community")
                if (steam.resolveActivity(context.packageManager) != null) steam else i
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = L10n.s(R.string.wish_drop_text, old.price ?: "?", new.price ?: "?") +
            if (new.discountPct > 0) "  (−${new.discountPct} %)" else ""
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(L10n.s(R.string.wish_drop_title, new.name))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(new.appId.toInt(), n) }
    }

    companion object { const val CHANNEL = "wishlist_prices" }
}

/**
 * Revisión de la lista de deseos: la descarga, avisa de lo que ha bajado desde la última vez (comparando con lo
 * guardado en el archivo, que es lo que vale aunque la revisión la haga la app abierta o la tarea en segundo plano)
 * y la guarda. Devuelve la lista nueva y el país de la tienda usado para los precios.
 */
object WishlistCheck {
    suspend fun run(context: Context, settings: Settings): Pair<List<WishItem>, String> {
        val id = settings.steamLinkedId ?: settings.steamUser?.takeIf { u -> u.length == 17 && u.all { it.isDigit() } }
            ?: error(L10n.s(R.string.wish_no_account))
        // Con la sesión de Steam si se puede (sirve para listas privadas); si tarda o falla, sin ella.
        val token = if (settings.steamLinkedId != null)
            kotlinx.coroutines.withTimeoutOrNull(30_000) { runCatching { com.repisa.app.sources.SteamLogin.token(settings) }.getOrNull() }
        else null
        // La moneda de la cuenta de Steam; si no se sabe, la del país del dispositivo.
        val country = com.repisa.app.sources.SteamLogin.storeCountry() ?: java.util.Locale.getDefault().country.takeIf { it.length == 2 } ?: "US"
        val lang = if (L10n.current?.tag?.startsWith("es") != false) "spanish" else "english"
        val fresh = com.repisa.app.sources.SteamWishlist.fetch(id, token, country, lang)
        val store = WishlistStore(context)
        val old = store.load()
        if (settings.wishlistAlerts) store.drops(old, fresh).forEach { (o, n) -> store.notifyDrop(o, n) }
        store.save(fresh)
        return fresh to country
    }
}

/**
 * Tarea de Android que revisa la lista de deseos cada 6 horas aunque Repisa esté cerrada (con internet),
 * para avisar de las bajadas de precio sin tener que abrir la app.
 */
class WishlistJob : android.app.job.JobService() {
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: android.app.job.JobParameters): Boolean {
        val s = Settings(applicationContext)
        if (!wanted(s)) return false
        scope.launch {
            runCatching { WishlistCheck.run(applicationContext, s) }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: android.app.job.JobParameters) = true

    companion object {
        private const val JOB_ID = 4207

        private fun wanted(s: Settings) = s.steamConfigured && s.wishlistShelf && s.wishlistAlerts && s.wishlistBackground

        /** Programa (o quita) la revisión periódica según los ajustes. */
        fun schedule(context: Context) {
            val js = context.getSystemService(android.app.job.JobScheduler::class.java) ?: return
            if (!wanted(Settings(context))) { js.cancel(JOB_ID); return }
            if (js.getPendingJob(JOB_ID) != null) return
            js.schedule(
                android.app.job.JobInfo.Builder(JOB_ID, android.content.ComponentName(context, WishlistJob::class.java))
                    .setPeriodic(6 * 60 * 60 * 1000L)
                    .setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .build(),
            )
        }
    }
}
