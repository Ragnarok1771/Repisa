package com.repisa.app.model

import com.repisa.app.R
import com.repisa.app.i18n.L10n
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * En qué punto está cada juego. Cada estado tiene su color (cinta en la esquina de la caja) y su icono, para que
 * nunca se distinga solo por el color.
 */
enum class PlayStatus(private val labelRes: Int, val color: Long, val icon: String) {
    BACKLOG(R.string.status_backlog, 0xFFF4C430, "⏳"),
    PLAYING(R.string.status_playing, 0xFF7BD389, "▶"),
    FINISHED(R.string.status_finished, 0xFF2E8B57, "✓"),
    COMPLETED(R.string.status_completed, 0xFF9B59B6, "★"),
    ABANDONED(R.string.status_abandoned, 0xFFE74C3C, "✕"),
    /** Juegos sin final (Fortnite, Counter-Strike...): no cuentan como pendientes ni en «los más jugados». */
    NOT_APPLICABLE(R.string.status_na, 0xFF8C8C8C, "∅");

    val label: String get() = L10n.s(labelRes)

    companion object {
        fun of(name: String?): PlayStatus? = entries.firstOrNull { it.name == name }
    }
}

/** Reglas globales de estado (ajuste «Estado automático»). */
object StatusRules {
    /** Sin estado puesto a mano: sin tiempo jugado = Pendiente; con tiempo = Jugando. */
    // Estado de Compose: al cambiar el ajuste, las cajas que lo usan se vuelven a dibujar solas.
    var auto: Boolean by androidx.compose.runtime.mutableStateOf(true)

    /** Orden de las repisas por estado (se puede cambiar en Ajustes → Pantalla). */
    val DEFAULT_ORDER = listOf(PlayStatus.PLAYING, PlayStatus.BACKLOG, PlayStatus.FINISHED, PlayStatus.COMPLETED,
        PlayStatus.ABANDONED, PlayStatus.NOT_APPLICABLE)
    var order: List<PlayStatus> by androidx.compose.runtime.mutableStateOf(DEFAULT_ORDER)

    /** Lee el orden guardado («PLAYING,BACKLOG,...»); los estados que falten van al final en su orden normal. */
    fun parseOrder(saved: String?): List<PlayStatus> {
        val list = saved?.split(',')?.mapNotNull { PlayStatus.of(it.trim()) }?.distinct().orEmpty()
        return list + DEFAULT_ORDER.filter { it !in list }
    }
}

/** El estado puesto a mano o, si no hay y el estado automático está activo, el que corresponde por tiempo jugado. */
val Game.playStatus: PlayStatus?
    get() = PlayStatus.of(status) ?: if (StatusRules.auto) (if (totalMinutes > 0) PlayStatus.PLAYING else PlayStatus.BACKLOG) else null
