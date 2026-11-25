package com.repisa.app.launch

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.DocumentsContract
import com.repisa.app.R
import com.repisa.app.data.Settings
import com.repisa.app.i18n.L10n
import com.repisa.app.model.Game
import com.repisa.app.model.Source

data class AppInfo(val packageName: String, val label: String, val icon: Drawable?)

/**
 * Abre los juegos de emulador directamente: con el emulador elegido para cada consola o, si no hay ninguno
 * elegido, con el primero conocido que esté instalado (ver [Emulators]).
 */
class Launcher(private val context: Context, private val settings: Settings) {

    private val pm get() = context.packageManager

    fun canLaunch(g: Game): Boolean = when (g.source) {
        Source.EMULATOR -> g.romUri != null || (g.romPath != null && g.corePath != null)
        else -> installedSteamId(g) != null && gameNative() != null
    }

    /** GameNative (o su variante NativeRunner), la app que juega los juegos de Steam en Android. */
    fun gameNative(): String? = listOf("app.gamenative", "app.nativerunner").firstOrNull { isInstalled(it) }

    /** Número de Steam del juego (o de una de sus copias) si está instalado en la consola. */
    fun installedSteamId(g: Game): String? {
        val installed = settings.steamInstalledIds
        return (listOf(g.id) + g.copyIds).filter { it.startsWith("steam:") }.map { it.removePrefix("steam:") }.firstOrNull { it in installed }
    }

    /**
     * Opciones al abrir el juego: sin transición (Android deslizaba Repisa a la izquierda y el emulador entraba por la
     * derecha). La animación de Repisa ya termina en negro, así que el juego aparece directamente.
     * [displayId]: pantalla en la que abrirlo (la de arriba en la AYN Thor).
     */
    /**
     * Quién abre el juego: la pantalla de Repisa (MainActivity) si está abierta, para poder anular justo después
     * la transición de Android (si se abre desde la app, Android ignora la petición de no animar).
     */
    var starter: ((Intent, android.os.Bundle) -> Unit)? = null

    private fun start(intent: Intent, options: android.os.Bundle) {
        starter?.let { return it(intent, options) }
        context.startActivity(intent, options)
    }

    private fun noTransition(displayId: Int?): android.os.Bundle =
        android.app.ActivityOptions.makeCustomAnimation(context, 0, 0)
            .apply { displayId?.let { setLaunchDisplayId(it) } }.toBundle()

    fun isInstalled(pkg: String): Boolean = runCatching { pm.getPackageInfo(pkg, 0); true }.getOrDefault(false)

    /** El primer emulador conocido instalado para un sistema (ver [Emulators]). */
    fun autoEmulator(consoleId: String): String? =
        Emulators.candidates(consoleId).firstOrNull { isInstalled(it.second) }?.second

    /** Emulador con el que se abre un sistema: el elegido en Ajustes o, si no hay, el detectado. */
    fun emulatorFor(consoleId: String): String? =
        settings.emulatorFor(consoleId)?.takeIf { isInstalled(it) } ?: autoEmulator(consoleId)

    /** Sistema → emulador detectado, para los sistemas indicados. */
    fun detected(consoleIds: Collection<String>): Map<String, String> =
        consoleIds.mapNotNull { id -> emulatorFor(id)?.let { id to it } }.toMap()

    /** Lanza el juego directamente en su emulador. Devuelve un mensaje de error o null si todo fue bien. */
    fun launch(g: Game, displayId: Int? = null): String? {
        val options = noTransition(displayId)
        // Juegos de Steam instalados: los abre GameNative con su número de Steam.
        if (g.source != Source.EMULATOR) {
            val app = gameNative() ?: return L10n.s(R.string.err_launch_noemu, "Steam")
            val appId = installedSteamId(g)?.toIntOrNull() ?: return L10n.s(R.string.err_launch_nofile)
            val intent = Intent("app.gamenative.LAUNCH_GAME").setComponent(ComponentName(app, "app.gamenative.MainActivity"))
                .putExtra("game_source", "STEAM").putExtra("app_id", appId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            return try {
                settings.pendingSessionStart = System.currentTimeMillis()
                start(intent, options)
                settings.pendingSessionGame = g.id
                settings.pendingSessionPackage = app
                null
            } catch (_: ActivityNotFoundException) {
                L10n.s(R.string.err_launch_noemu, "Steam")
            } catch (e: SecurityException) {
                L10n.s(R.string.err_launch_denied, e.message ?: "")
            }
        }
        val pkg = emulatorFor(g.consoleId)
        val intent: Intent = when {
            // Juego de una playlist de RetroArch: arrancar RetroArch con su core.
            (pkg == null || pkg.startsWith("com.retroarch")) && g.romPath != null && g.corePath != null ->
                retroArchIntent(pkg ?: Emulators.candidates("snes").map { it.second }.firstOrNull { it.startsWith("com.retroarch") && isInstalled(it) } ?: "com.retroarch",
                    g.romPath, g.corePath)
            pkg == null -> return L10n.s(R.string.err_launch_noemu, g.console.name)
            g.romUri == null -> return L10n.s(R.string.err_launch_nofile)
            else -> gameIntent(pkg, g) ?: return L10n.s(R.string.err_launch_noemu, g.console.name)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        return try {
            settings.pendingSessionStart = System.currentTimeMillis()
            start(intent, options)
            settings.pendingSessionGame = g.id
            settings.pendingSessionPackage = intent.component?.packageName ?: intent.`package` ?: pkg
            null
        } catch (_: ActivityNotFoundException) {
            L10n.s(R.string.err_launch_noemu, g.console.name)
        } catch (e: SecurityException) {
            L10n.s(R.string.err_launch_denied, e.message ?: "")
        }
    }

    private fun activityExists(pkg: String, cls: String) =
        runCatching { pm.getActivityInfo(ComponentName(pkg, cls), 0); true }.getOrDefault(false)

    /**
     * Intent para abrir [g] en [pkg]: la forma propia del emulador si la conocemos, si no ACTION_VIEW dirigido
     * a ese emulador, y si tampoco, el emulador sin juego. Nunca el selector de apps.
     */
    private fun gameIntent(pkg: String, g: Game): Intent? {
        val uri = Uri.parse(g.romUri)
        val profile = Emulators.profileFor(pkg)
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        if (profile?.openOnly == true) return pm.getLaunchIntentForPackage(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (profile?.retroArch == true) {
            val path = filePath(uri)
            val core = RetroCores.corePath(pkg, g.consoleId)
            if (path != null && core != null) return retroArchIntent(pkg, path, core)
        }
        profile?.activities?.firstOrNull { activityExists(pkg, it) }?.let { act ->
            return Intent(profile.action).setComponent(ComponentName(pkg, act))
                .setDataAndType(uri, "application/octet-stream").addFlags(flags).apply {
                    profile.uriExtra?.let { putExtra(it, uri.toString()) }
                    profile.pathExtra?.let { k -> filePath(uri)?.let { putExtra(k, it) } }
                    profile.boolExtras.forEach { (k, v) -> putExtra(k, v) }
                }
        }
        val view = Intent(Intent.ACTION_VIEW).setPackage(pkg).setDataAndType(uri, "application/octet-stream").addFlags(flags)
        if (pm.resolveActivity(view, 0) != null) return view
        return pm.getLaunchIntentForPackage(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** content://.../document/primary%3AROMs%2Fnds%2FKirby.nds → /storage/emulated/0/ROMs/nds/Kirby.nds */
    private fun filePath(uri: Uri): String? = runCatching {
        val id = DocumentsContract.getDocumentId(uri)
        val vol = id.substringBefore(':')
        val rel = id.substringAfter(':')
        if (vol.equals("primary", true)) "/storage/emulated/0/$rel" else "/storage/$vol/$rel"
    }.getOrNull()

    private fun retroArchIntent(pkg: String, rom: String, core: String): Intent {
        val dataDir = "/data/data/$pkg"
        return Intent(Intent.ACTION_MAIN)
            .setComponent(ComponentName(pkg, "com.retroarch.browser.retroactivity.RetroActivityFuture"))
            .putExtra("ROM", rom)
            .putExtra("LIBRETRO", core)
            .putExtra("CONFIGFILE", "/storage/emulated/0/Android/data/$pkg/files/retroarch.cfg")
            .putExtra("IME", "com.android.inputmethod.latin/.LatinIME")
            .putExtra("DATADIR", dataDir)
            .putExtra("SDCARD", "/storage/emulated/0")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Apps instaladas que se pueden elegir como emulador. */
    fun installedApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString(), runCatching { it.loadIcon(pm) }.getOrNull()) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedWith(compareByDescending<AppInfo> { looksLikeEmulator(it) }.thenBy { it.label.lowercase() })
    }

    fun appLabel(pkg: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    private fun looksLikeEmulator(a: AppInfo): Boolean {
        val t = (a.packageName + " " + a.label).lowercase()
        if (Emulators.isKnown(a.packageName)) return true
        return listOf("emu", "retroarch", "dolphin", "duckstation", "sx2", "ppsspp", "citra", "azahar", "yuzu", "eden",
            "flycast", "redream", "melonds", "drastic", "mupen", "snes", "mgba", "vita3k", "winlator", "cemu", "lemuroid",
            "citron", "sudachi", "xemu", "saturn", "yaba", "m64", "pcsx", "nether", "lime3ds", "gamenative", "pluvia")
            .any { t.contains(it) }
    }

    val usage = UsageTracker(context)

    data class Session(val gameId: String, val minutes: Int, val measured: Boolean)

    /**
     * Si al volver a la app había una partida en curso, devuelve cuánto se jugó.
     * Con "Acceso a datos de uso" se cuenta solo el tiempo con el emulador en pantalla;
     * sin él, el tiempo transcurrido hasta volver a Repisa (máx. 12 h).
     */
    fun consumeSession(): Session? {
        val id = settings.pendingSessionGame ?: return null
        val start = settings.pendingSessionStart
        val now = System.currentTimeMillis()
        settings.pendingSessionGame = null
        val measured = runCatching { usage.sessionMinutes(start, now, settings.pendingSessionPackage) }.getOrNull()
        val minutes = measured ?: ((now - start) / 60_000).toInt()
        return if (minutes in 1..720) Session(id, minutes, measured != null) else null
    }
}
