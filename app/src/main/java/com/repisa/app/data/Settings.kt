package com.repisa.app.data

import android.content.Context
import androidx.core.content.edit

/** Preferencias y credenciales. Se guardan en el almacenamiento privado de la app. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("repisa", Context.MODE_PRIVATE)

    private fun str(key: String) = prefs.getString(key, null)?.takeIf { it.isNotBlank() }
    private fun put(key: String, value: String?) = prefs.edit { if (value == null) remove(key) else putString(key, value) }

    // --- Steam ---
    var steamApiKey: String? get() = str("steam_key"); set(v) = put("steam_key", v?.trim())
    /** Lo que escribió el usuario: SteamID64, nombre personalizado o URL del perfil. */
    var steamUser: String? get() = str("steam_user"); set(v) = put("steam_user", v?.trim())

    /** Cuenta vinculada iniciando sesión (sin clave de API). */
    var steamLinkedId: String? get() = str("steam_linked_id"); set(v) = put("steam_linked_id", v)
    var steamLinkedName: String? get() = str("steam_linked_name"); set(v) = put("steam_linked_name", v)
    val steamConfigured: Boolean get() = steamLinkedId != null || (steamApiKey != null && steamUser != null)

    // --- Epic ---
    var epicAccessToken: String? get() = str("epic_access"); set(v) = put("epic_access", v)
    var epicRefreshToken: String? get() = str("epic_refresh"); set(v) = put("epic_refresh", v)
    var epicAccountId: String? get() = str("epic_account"); set(v) = put("epic_account", v)
    var epicDisplayName: String? get() = str("epic_name"); set(v) = put("epic_name", v)
    /** Cliente de Epic con el que se obtuvo la sesión: "launcher" (lanzador) o "device" (vinculación con código). */
    var epicClient: String get() = str("epic_client") ?: "launcher"; set(v) = put("epic_client", v)
    var epicExpiresAt: Long
        get() = prefs.getLong("epic_expires", 0)
        set(v) = prefs.edit { putLong("epic_expires", v) }

    fun clearEpic() {
        epicAccessToken = null; epicRefreshToken = null; epicAccountId = null; epicDisplayName = null; epicExpiresAt = 0
        put("epic_client", null)
    }

    // --- Emuladores (URIs de carpetas concedidas con el selector de Android) ---
    var romFolders: Set<String>
        get() = prefs.getStringSet("rom_folders", emptySet()) ?: emptySet()
        set(v) = prefs.edit { putStringSet("rom_folders", v) }
    var esdeFolder: String? get() = str("esde_folder"); set(v) = put("esde_folder", v)
    var retroarchFolder: String? get() = str("ra_folder"); set(v) = put("ra_folder", v)
    /** Carpeta de datos de Cocoon (o cualquier carpeta con gamelist.xml exportados). */
    var cocoonFolder: String? get() = str("cocoon_folder"); set(v) = put("cocoon_folder", v)
    val hasEmulatorSources: Boolean
        get() = romFolders.isNotEmpty() || esdeFolder != null || retroarchFolder != null || cocoonFolder != null

    /** Emulador elegido por consola: "consoleId" -> nombre de paquete. */
    fun emulatorFor(consoleId: String): String? = str("emu_$consoleId")
    fun setEmulatorFor(consoleId: String, pkg: String?) = put("emu_$consoleId", pkg)

    // --- Metadatos ---
    /** Clave gratuita de https://www.steamgriddb.com/profile/preferences/api (buscador de carátulas). */
    var steamGridDbKey: String? get() = str("sgdb_key"); set(v) = put("sgdb_key", v?.trim())
    /** Última "pasada" automática de búsqueda de carátulas que faltan (ver LibraryViewModel.COVER_PASS). */
    var coverPass: Int
        get() = prefs.getInt("cover_pass", 0)
        set(v) = prefs.edit { putInt("cover_pass", v) }
    var useLibretroCovers: Boolean
        get() = prefs.getBoolean("libretro_covers", true)
        set(v) = prefs.edit { putBoolean("libretro_covers", v) }

    // --- Pantalla ---
    var secondScreen: Boolean
        get() = prefs.getBoolean("second_screen", true)
        set(v) = prefs.edit { putBoolean("second_screen", v) }
    var shelfHeightDp: Int
        get() = prefs.getInt("shelf_height", 140)
        set(v) = prefs.edit { putInt("shelf_height", v) }

    /** Última sincronización automática (epoch ms). */
    var lastAutoSync: Long
        get() = prefs.getLong("last_auto_sync", 0)
        set(v) = prefs.edit { putLong("last_auto_sync", v) }

    // --- Calificaciones ---
    var ratingScale: String get() = str("rating_scale") ?: "TEN"; set(v) = put("rating_scale", v)
    /** Plantilla de criterios (JSON) con la que empieza cada calificación nueva. */
    var ratingTemplate: String? get() = str("rating_template"); set(v) = put("rating_template", v)

    // --- Vista ---
    var sortMode: String get() = str("sort") ?: "ALPHA"; set(v) = put("sort", v)
    var groupMode: String get() = str("group") ?: "NONE"; set(v) = put("group", v)

    // --- Sesión de juego en curso (para medir tiempo) ---
    var pendingSessionGame: String? get() = str("session_game"); set(v) = put("session_game", v)
    var pendingSessionPackage: String? get() = str("session_pkg"); set(v) = put("session_pkg", v)
    var pendingSessionStart: Long
        get() = prefs.getLong("session_start", 0)
        set(v) = prefs.edit { putLong("session_start", v) }
}
