package com.repisa.app.data

import android.content.SharedPreferences
import com.repisa.app.model.Game
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * Copia de seguridad de Repisa: un archivo JSON con la biblioteca (calificaciones, tiempo a mano, favoritos,
 * copias preferentes, carátulas elegidas, datos editados...) y los ajustes.
 *
 * No incluye nunca las sesiones de Steam/Epic ni las claves de API ([Settings.SECRET_KEYS]), ni las carpetas
 * ([Settings.DEVICE_KEYS]): los permisos de carpeta de Android no sobreviven a reinstalar la app.
 */
object Backup {
    const val FORMAT = 1
    private val gamesSerializer = ListSerializer(Game.serializer())

    fun export(games: List<Game>, prefs: SharedPreferences, appVersion: String): String {
        val skip = Settings.SECRET_KEYS + Settings.DEVICE_KEYS
        val savedPrefs = prefs.all.filterKeys { it !in skip }.mapNotNull { (k, v) ->
            val typed: JsonObject = when (v) {
                is String -> JsonObject(mapOf("t" to JsonPrimitive("s"), "v" to JsonPrimitive(v)))
                is Boolean -> JsonObject(mapOf("t" to JsonPrimitive("b"), "v" to JsonPrimitive(v)))
                is Int -> JsonObject(mapOf("t" to JsonPrimitive("i"), "v" to JsonPrimitive(v)))
                is Long -> JsonObject(mapOf("t" to JsonPrimitive("l"), "v" to JsonPrimitive(v)))
                is Float -> JsonObject(mapOf("t" to JsonPrimitive("f"), "v" to JsonPrimitive(v)))
                is Set<*> -> JsonObject(mapOf("t" to JsonPrimitive("set"),
                    "v" to JsonArray(v.filterIsInstance<String>().map { JsonPrimitive(it) })))
                else -> return@mapNotNull null
            }
            k to typed
        }.toMap()
        val root = JsonObject(mapOf(
            "app" to JsonPrimitive("Repisa"),
            "format" to JsonPrimitive(FORMAT),
            "version" to JsonPrimitive(appVersion),
            "exportedAt" to JsonPrimitive(System.currentTimeMillis()),
            "games" to Http.json.encodeToJsonElement(gamesSerializer, games.map { it.stored() }),
            "settings" to JsonObject(savedPrefs),
        ))
        return root.toString()
    }

    class Parsed(val games: List<Game>, val settings: Map<String, JsonElement>, val version: String?)

    fun parse(text: String): Parsed {
        val root = Http.json.parseToJsonElement(text).jsonObject
        require(root["app"]?.jsonPrimitive?.content == "Repisa") { "no es una copia de Repisa" }
        val games = Http.json.decodeFromJsonElement(gamesSerializer, root["games"] ?: JsonArray(emptyList()))
        return Parsed(games, root["settings"]?.jsonObject ?: emptyMap(), root["version"]?.jsonPrimitive?.content)
    }

    /** Escribe los ajustes de la copia (nunca claves ni carpetas, aunque vinieran en el archivo). */
    fun restoreSettings(settings: Map<String, JsonElement>, prefs: SharedPreferences) {
        val skip = Settings.SECRET_KEYS + Settings.DEVICE_KEYS
        val e = prefs.edit()
        settings.forEach { (k, el) ->
            if (k in skip) return@forEach
            val o = el.jsonObject
            val v = o["v"] ?: return@forEach
            runCatching {
                when (o["t"]?.jsonPrimitive?.content) {
                    "s" -> e.putString(k, v.jsonPrimitive.content)
                    "b" -> e.putBoolean(k, v.jsonPrimitive.booleanOrNull ?: v.jsonPrimitive.boolean)
                    "i" -> e.putInt(k, v.jsonPrimitive.int)
                    "l" -> e.putLong(k, v.jsonPrimitive.long)
                    "f" -> e.putFloat(k, v.jsonPrimitive.float)
                    "set" -> e.putStringSet(k, v.jsonArray.map { it.jsonPrimitive.content }.toSet())
                }
            }
        }
        e.apply()
    }

    /**
     * Une la biblioteca actual con la de la copia: los datos frescos de las fuentes (si el juego ya está) y todo
     * lo tuyo de la copia (notas, tiempo a mano, favoritos, copia preferente, carátula elegida, datos editados).
     */
    fun mergeLibrary(current: List<Game>, imported: List<Game>): List<Game> {
        val now = current.associateBy { it.id }
        val fromBackup = imported.map { old -> now[old.id]?.let { mergeGame(old, it) } ?: old }
        val ids = imported.map { it.id }.toSet()
        return fromBackup + current.filter { it.id !in ids }
    }
}
