package com.repisa.app.model

import com.repisa.app.R
import com.repisa.app.i18n.L10n

/**
 * En qué punto está cada juego. Cada estado tiene su color (cinta en la esquina de la caja) y su icono, para que
 * nunca se distinga solo por el color.
 */
enum class PlayStatus(private val labelRes: Int, val color: Long, val icon: String) {
    BACKLOG(R.string.status_backlog, 0xFFF4C430, "⏳"),
    PLAYING(R.string.status_playing, 0xFF7BD389, "▶"),
    FINISHED(R.string.status_finished, 0xFF2E8B57, "✓"),
    COMPLETED(R.string.status_completed, 0xFF9B59B6, "★"),
    ABANDONED(R.string.status_abandoned, 0xFFE74C3C, "✕");

    val label: String get() = L10n.s(labelRes)

    companion object {
        fun of(name: String?): PlayStatus? = entries.firstOrNull { it.name == name }
    }
}

val Game.playStatus: PlayStatus? get() = PlayStatus.of(status)
