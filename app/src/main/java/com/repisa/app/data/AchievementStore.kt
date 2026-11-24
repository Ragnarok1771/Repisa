package com.repisa.app.data

import android.content.Context
import com.repisa.app.sources.AchievementSet
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File

/**
 * Logros guardados:
 *  - Listas descargadas (Steam, RetroAchievements): en caché, se vuelven a pedir cada día.
 *  - Logros marcados a mano (juegos de Epic) y fechas de los logros de Repisa: en los ajustes, así entran en la
 *    copia de seguridad.
 */
class AchievementStore(context: Context, private val settings: Settings) {
    private val dir = File(context.cacheDir, "achievements").apply { mkdirs() }
    private val mapSer = MapSerializer(String.serializer(), MapSerializer(String.serializer(), Long.serializer()))

    private fun file(gameId: String) = File(dir, fileKey(gameId) + ".json")

    fun cached(gameId: String): AchievementSet? = runCatching {
        Http.json.decodeFromString(AchievementSet.serializer(), file(gameId).readText())
    }.getOrNull()

    /** Todos los logros guardados (para la pantalla general): id del juego → logros. */
    fun all(): Map<String, AchievementSet> = dir.listFiles().orEmpty().mapNotNull { f ->
        runCatching { f.nameWithoutExtension to Http.json.decodeFromString(AchievementSet.serializer(), f.readText()) }.getOrNull()
    }.toMap()

    fun fileKey(gameId: String) = gameId.replace(Regex("[^A-Za-z0-9_-]"), "_")

    fun clearAll() { dir.listFiles()?.forEach { it.delete() } }

    fun save(gameId: String, set: AchievementSet) {
        runCatching { file(gameId).writeText(Http.json.encodeToString(AchievementSet.serializer(), set)) }
    }

    private fun readMap(key: String): Map<String, Map<String, Long>> =
        runCatching { Http.json.decodeFromString(mapSer, settings.rawString(key) ?: "{}") }.getOrDefault(emptyMap())

    private fun writeMap(key: String, map: Map<String, Map<String, Long>>) = settings.putRawString(key, Http.json.encodeToString(mapSer, map))

    /** Logros de Steam marcados a mano en un juego (id del logro → cuándo lo marcaste). */
    fun manual(gameId: String): Map<String, Long> = readMap("ach_manual")[gameId].orEmpty()

    fun toggleManual(gameId: String, achId: String): Map<String, Long> {
        val all = readMap("ach_manual").toMutableMap()
        val mine = all[gameId].orEmpty().toMutableMap()
        if (achId in mine) mine.remove(achId) else mine[achId] = System.currentTimeMillis()
        all[gameId] = mine
        writeMap("ach_manual", all)
        return mine
    }

    /** Cuándo se vio cumplido por primera vez cada logro de Repisa de un juego. */
    fun repisaSeen(gameId: String, met: Set<String>): Map<String, Long> {
        val all = readMap("ach_repisa").toMutableMap()
        val mine = all[gameId].orEmpty().toMutableMap()
        val now = System.currentTimeMillis()
        var changed = false
        met.forEach { if (it !in mine) { mine[it] = now; changed = true } }
        if (changed) { all[gameId] = mine; writeMap("ach_repisa", all) }
        return mine
    }
}
