package com.repisa.app.launch

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import com.repisa.app.data.Settings
import com.repisa.app.model.Game
import com.repisa.app.model.Source

data class AppInfo(val packageName: String, val label: String, val icon: Drawable?)

/**
 * Abre los juegos de emulador con la app elegida para cada consola.
 * Para algunos emuladores conocidos se usa su forma específica de arrancar un juego
 * (las mismas que usa ES-DE); para el resto, ACTION_VIEW con el archivo.
 */
class Launcher(private val context: Context, private val settings: Settings) {

    private data class Preset(val activity: String, val extra: String? = null, val view: Boolean = false)

    private val presets = mapOf(
        "org.dolphinemu.dolphinemu" to Preset("org.dolphinemu.dolphinemu.ui.main.MainActivity", extra = "AutoStartFile"),
        "org.dolphinemu.mmjr" to Preset("org.dolphinemu.dolphinemu.ui.main.MainActivity", extra = "AutoStartFile"),
        "com.github.stenzek.duckstation" to Preset("com.github.stenzek.duckstation.EmulationActivity", extra = "bootPath"),
        "xyz.aethersx2.android" to Preset("xyz.aethersx2.android.EmulationActivity", extra = "bootPath"),
        "org.ppsspp.ppsspp" to Preset("org.ppsspp.ppsspp.PpssppActivity", view = true),
        "org.ppsspp.ppssppgold" to Preset("org.ppsspp.ppsspp.PpssppActivity", view = true),
        "com.flycast.emulator" to Preset("com.flycast.emulator.MainActivity", view = true),
    )

    fun canLaunch(g: Game): Boolean = when (g.source) {
        Source.EMULATOR -> g.romUri != null || (g.romPath != null && g.corePath != null)
        else -> false
    }

    /** Lanza el juego. Devuelve un mensaje de error o null si todo fue bien. */
    fun launch(g: Game): String? {
        val pkg = settings.emulatorFor(g.consoleId)
        val intent: Intent = when {
            // Juego de una playlist de RetroArch sin emulador elegido: arrancar RetroArch con su core.
            (pkg == null || pkg.startsWith("com.retroarch")) && g.romPath != null && g.corePath != null ->
                retroArchIntent(pkg ?: "com.retroarch", g.romPath, g.corePath)
            g.romUri != null -> viewIntent(pkg, Uri.parse(g.romUri))
            else -> return "No sé dónde está el archivo de este juego. Añade su carpeta de ROMs en Ajustes."
        }
        return try {
            context.startActivity(intent)
            settings.pendingSessionGame = g.id
            settings.pendingSessionStart = System.currentTimeMillis()
            null
        } catch (_: ActivityNotFoundException) {
            "No se encontró el emulador. Elige uno para ${g.console.name} en Ajustes."
        } catch (e: SecurityException) {
            "Android no permitió abrir el juego: ${e.message}"
        }
    }

    private fun viewIntent(pkg: String?, uri: Uri): Intent {
        val base = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (pkg != null) {
            val preset = presets[pkg]
            return when {
                preset?.extra != null -> base.setAction(Intent.ACTION_MAIN)
                    .setComponent(ComponentName(pkg, preset.activity))
                    .putExtra(preset.extra, uri.toString())
                    .setDataAndType(uri, "application/octet-stream")
                preset != null -> base.setAction(Intent.ACTION_VIEW)
                    .setComponent(ComponentName(pkg, preset.activity))
                    .setDataAndType(uri, "application/octet-stream")
                else -> base.setAction(Intent.ACTION_VIEW).setPackage(pkg).setDataAndType(uri, "application/octet-stream")
            }
        }
        return Intent.createChooser(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/octet-stream")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            "Abrir con…",
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

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
        return listOf("emu", "retroarch", "dolphin", "duckstation", "sx2", "ppsspp", "citra", "azahar", "yuzu", "eden",
            "flycast", "redream", "melonds", "drastic", "mupen", "snes", "mgba", "vita3k", "winlator", "cemu", "lemuroid",
            "citron", "sudachi", "xemu", "saturn", "yaba", "m64", "pcsx", "nether", "lime3ds", "gamenative", "pluvia")
            .any { t.contains(it) }
    }

    /**
     * Si al volver a la app había una partida en curso, devuelve (idDelJuego, minutos jugados).
     * Se ignoran sesiones de menos de 1 minuto y se limitan a 12 h por si la app se quedó abierta.
     */
    fun consumeSession(): Pair<String, Int>? {
        val id = settings.pendingSessionGame ?: return null
        val minutes = ((System.currentTimeMillis() - settings.pendingSessionStart) / 60_000).toInt()
        settings.pendingSessionGame = null
        return if (minutes in 1..720) id to minutes else null
    }
}
