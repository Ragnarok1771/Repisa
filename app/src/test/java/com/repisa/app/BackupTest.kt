package com.repisa.app

import android.content.SharedPreferences
import com.repisa.app.data.Backup
import com.repisa.app.model.Game
import com.repisa.app.model.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SharedPreferences en memoria para probar la copia sin Android. */
private class FakePrefs(val map: MutableMap<String, Any?> = mutableMapOf()) : SharedPreferences {
    override fun getAll(): MutableMap<String, *> = map
    override fun getString(k: String, d: String?) = map[k] as? String ?: d
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(k: String, d: MutableSet<String>?) = map[k] as? MutableSet<String> ?: d
    override fun getInt(k: String, d: Int) = map[k] as? Int ?: d
    override fun getLong(k: String, d: Long) = map[k] as? Long ?: d
    override fun getFloat(k: String, d: Float) = map[k] as? Float ?: d
    override fun getBoolean(k: String, d: Boolean) = map[k] as? Boolean ?: d
    override fun contains(k: String) = k in map
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        override fun putString(k: String, v: String?) = apply { map[k] = v }
        override fun putStringSet(k: String, v: MutableSet<String>?) = apply { map[k] = v }
        override fun putInt(k: String, v: Int) = apply { map[k] = v }
        override fun putLong(k: String, v: Long) = apply { map[k] = v }
        override fun putFloat(k: String, v: Float) = apply { map[k] = v }
        override fun putBoolean(k: String, v: Boolean) = apply { map[k] = v }
        override fun remove(k: String) = apply { map.remove(k) }
        override fun clear() = apply { map.clear() }
        override fun commit() = true
        override fun apply() = Unit
    }
}

class BackupTest {
    @Test fun roundTripKeepsUserDataAndNeverSecrets() {
        val prefs = FakePrefs(mutableMapOf(
            "rating_scale" to "HUNDRED", "second_screen" to false, "shelf_height" to 150,
            "hidden_games" to mutableSetOf("emu:gba:x"), "epic_refresh" to "SECRETO", "sgdb_key" to "CLAVE",
            "rom_folders" to mutableSetOf("content://x"),
        ))
        val fe = Game("emu:switch:fe", Source.EMULATOR, "Fire Emblem Three Houses", "switch", favorite = true, manualMinutes = 1072)
        val text = Backup.export(listOf(fe), prefs, "0.12.0")
        assertFalse(text.contains("SECRETO")); assertFalse(text.contains("CLAVE")); assertFalse(text.contains("content://x"))

        val parsed = Backup.parse(text)
        val fresh = FakePrefs()
        Backup.restoreSettings(parsed.settings, fresh)
        assertEquals("HUNDRED", fresh.getString("rating_scale", null))
        assertEquals(150, fresh.getInt("shelf_height", 0))
        assertEquals(setOf("emu:gba:x"), fresh.getStringSet("hidden_games", null))
        assertFalse(fresh.contains("epic_refresh"))

        // Tras reinstalar, el escaneo trae el juego sin datos tuyos: la copia se los devuelve.
        val rescanned = fe.copy(favorite = false, manualMinutes = 0, playtimeMinutes = 30)
        val merged = Backup.mergeLibrary(listOf(rescanned), parsed.games).single()
        assertTrue(merged.favorite)
        assertEquals(1072, merged.manualMinutes)
        assertEquals(30, merged.playtimeMinutes)
    }
}
