package com.repisa.app.data

import android.content.Context
import com.repisa.app.model.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.util.Calendar

/**
 * Una partida: qué juego, cuándo empezó y cuántos minutos duró.
 * [measured] = la midió Repisa al abrir el juego desde la app; false = son horas nuevas que trajo
 * la sincronización (jugado en el PC, en Steam...), y su hora es la de la sincronización.
 */
@Serializable
data class PlaySession(
    val gameId: String,
    val title: String,
    val consoleId: String,
    val start: Long,
    val minutes: Int,
    val measured: Boolean = true,
    /**
     * No es una partida anotada sino «el último día que se jugó», la única fecha que guardan Steam y los emuladores
     * de antes de que Repisa empezara a anotar partidas. No tiene duración.
     */
    val lastOnly: Boolean = false,
) {
    val end: Long get() = start + minutes * 60_000L
}

/** Registro de partidas (sessions.json), la base del calendario y del resumen del año. */
class SessionLog(context: Context) {
    private val file = File(context.filesDir, "sessions.json")
    private val serializer = ListSerializer(PlaySession.serializer())

    suspend fun load(): List<PlaySession> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        runCatching { Http.json.decodeFromString(serializer, file.readText()) }.getOrDefault(emptyList())
    }

    suspend fun save(sessions: List<PlaySession>) = withContext(Dispatchers.IO) {
        val tmp = File(file.parentFile, "sessions.json.tmp")
        tmp.writeText(Http.json.encodeToString(serializer, sessions))
        tmp.renameTo(file)
    }
}

/** Día (año·1000 + día del año) de un instante, en la hora local. */
fun dayKey(ms: Long): Int = Calendar.getInstance().run { timeInMillis = ms; get(Calendar.YEAR) * 1000 + get(Calendar.DAY_OF_YEAR) }

/**
 * Partidas inventadas con los juegos reales del usuario, para ver el calendario y el resumen del año
 * sin haber jugado todavía (Ajustes → Pantalla → Vista previa).
 */
object SampleSessions {
    fun make(games: List<Game>, days: Int = 400, seed: Int = 7): List<PlaySession> {
        val pool = games.filter { it.coverUrl != null }.ifEmpty { games }
        if (pool.isEmpty()) return emptyList()
        val rnd = kotlin.random.Random(seed)
        // Unos pocos favoritos que se juegan mucho y el resto de vez en cuando.
        val favorites = pool.shuffled(rnd).take(minOf(6, pool.size))
        val out = mutableListOf<PlaySession>()
        val cal = Calendar.getInstance()
        val today = cal.clone() as Calendar
        for (d in 0 until days) {
            val day = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -d) }
            // Fines de semana más partidas; algunas rachas de días sin jugar.
            val weekend = day.get(Calendar.DAY_OF_WEEK).let { it == Calendar.SATURDAY || it == Calendar.SUNDAY }
            val chance = if (weekend) 0.8 else 0.5
            if ((d / 9) % 5 == 3 || rnd.nextDouble() > chance) continue
            repeat(1 + rnd.nextInt(if (weekend) 3 else 2)) {
                val g = if (rnd.nextDouble() < 0.7) favorites[rnd.nextInt(favorites.size)] else pool[rnd.nextInt(pool.size)]
                day.set(Calendar.HOUR_OF_DAY, 16 + rnd.nextInt(7))
                day.set(Calendar.MINUTE, rnd.nextInt(60))
                out += PlaySession(g.id, g.title, g.consoleId, day.timeInMillis, 15 + rnd.nextInt(150), rnd.nextBoolean())
            }
        }
        return out.sortedBy { it.start }
    }
}
