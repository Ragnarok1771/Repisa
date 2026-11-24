package com.repisa.app.model

import com.repisa.app.R
import com.repisa.app.i18n.L10n

/**
 * Logros de Repisa: valen para cualquier juego (también Switch, 3DS o Wii U, que no tienen logros propios) y la app
 * puede comprobarlos de verdad con lo que sabe del juego. La fecha es cuándo Repisa vio que se cumplía.
 */
enum class RepisaAchievement(private val nameRes: Int, private val descRes: Int, val icon: String, val met: (Game) -> Boolean) {
    FIRST(R.string.rach_first, R.string.rach_first_d, "🎮", { it.totalMinutes > 0 }),
    HOURS_10(R.string.rach_10h, R.string.rach_10h_d, "⏱", { it.totalMinutes >= 600 }),
    HOURS_50(R.string.rach_50h, R.string.rach_50h_d, "⌛", { it.totalMinutes >= 3000 }),
    HOURS_100(R.string.rach_100h, R.string.rach_100h_d, "🔥", { it.totalMinutes >= 6000 }),
    BEYOND_HLTB(R.string.rach_hltb, R.string.rach_hltb_d, "🐢", { g -> g.hltbMain?.let { g.totalMinutes / 60f >= it } == true }),
    FINISHED(R.string.rach_finished, R.string.rach_finished_d, "✓",
        { it.playStatus == PlayStatus.FINISHED || it.playStatus == PlayStatus.COMPLETED }),
    COMPLETED(R.string.rach_completed, R.string.rach_completed_d, "★", { it.playStatus == PlayStatus.COMPLETED }),
    RATED(R.string.rach_rated, R.string.rach_rated_d, "📝", { it.rating != null }),
    NOTES(R.string.rach_notes, R.string.rach_notes_d, "📓", { !it.notes.isNullOrBlank() }),
    FAVORITE(R.string.rach_favorite, R.string.rach_favorite_d, "♥", { it.favorite });

    val title: String get() = L10n.s(nameRes)
    val description: String get() = L10n.s(descRes)
}
