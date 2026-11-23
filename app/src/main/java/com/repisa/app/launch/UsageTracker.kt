package com.repisa.app.launch

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings as AndroidSettings

/**
 * Tiempo real de juego con el permiso "Acceso a datos de uso" de Android (el mismo que usa Cocoon).
 * Android registra cuándo cada app pasa a primer plano y cuándo deja de estarlo; sumando esos
 * intervalos sabemos cuánto tiempo estuvo el emulador realmente en pantalla, sin contar el tiempo
 * con la consola en reposo o con otra app abierta.
 */
class UsageTracker(private val context: Context) {

    fun hasPermission(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) ==
            AppOpsManager.MODE_ALLOWED
    }

    fun permissionIntent(): Intent = Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Milisegundos que cada app estuvo en primer plano entre [start] y [end].
     */
    fun foregroundTime(start: Long, end: Long): Map<String, Long> {
        if (!hasPermission()) return emptyMap()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usm.queryEvents(start, end)
        val openSince = mutableMapOf<String, Long>()
        val total = mutableMapOf<String, Long>()
        val seen = mutableSetOf<String>()
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val pkg = e.packageName ?: continue
            val firstEvent = seen.add(pkg)
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> openSince.putIfAbsent(pkg, e.timeStamp)
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    // Si lo primero que vemos es que se pausa, ya estaba en pantalla al empezar.
                    val t = openSince.remove(pkg) ?: if (firstEvent) start else null
                    if (t != null) total[pkg] = (total[pkg] ?: 0L) + (e.timeStamp - t)
                }
                UsageEvents.Event.ACTIVITY_STOPPED -> openSince.remove(pkg)?.let { t ->
                    total[pkg] = (total[pkg] ?: 0L) + (e.timeStamp - t)
                }
                // Pantalla apagada o consola en reposo: se corta la sesión de todas las apps.
                UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.KEYGUARD_SHOWN -> {
                    openSince.forEach { (p, t) -> total[p] = (total[p] ?: 0L) + (e.timeStamp - t) }
                    openSince.clear()
                }
            }
        }
        openSince.forEach { (p, t) -> total[p] = (total[p] ?: 0L) + (end - t) }
        return total
    }

    /**
     * Minutos jugados en la sesión que empezó en [start]. Si sabemos qué emulador se abrió, se cuenta
     * solo ese; si no (se eligió en "Abrir con…"), la app ajena que más tiempo estuvo en pantalla.
     * Devuelve null si no hay permiso.
     */
    fun sessionMinutes(start: Long, end: Long, pkg: String?): Int? {
        if (!hasPermission()) return null
        val times = foregroundTime(start, end)
        val ms = if (pkg != null) times[pkg] ?: 0L
        else times.filterKeys { it !in ignoredPackages() }.values.maxOrNull() ?: 0L
        return ((ms + 30_000) / 60_000).toInt()
    }

    private fun ignoredPackages(): Set<String> {
        val home = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_ALL,
        ).map { it.activityInfo.packageName }
        return home.toSet() + context.packageName + setOf("android", "com.android.systemui", "com.android.settings",
            "com.google.android.permissioncontroller", "com.android.permissioncontroller")
    }
}
