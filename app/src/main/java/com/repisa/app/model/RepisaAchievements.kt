package com.repisa.app.model

import com.repisa.app.R
import com.repisa.app.data.PlaySession
import com.repisa.app.i18n.L10n
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Categoría de un logro, como los trofeos: bronce (fácil), plata, oro (reto) y platino. */
enum class Tier(val medal: String, private val labelRes: Int) {
    BRONZE("🥉", R.string.tier_bronze), SILVER("🥈", R.string.tier_silver), GOLD("🥇", R.string.tier_gold),
    PLATINUM("💎", R.string.tier_platinum);

    val label: String get() = L10n.s(labelRes)
}

/** Lo que Repisa sabe de un juego para comprobar sus logros: el juego y sus partidas anotadas. */
class GameAchContext(val game: Game, allSessions: List<PlaySession>) {
    private val zone = ZoneId.systemDefault()
    private fun date(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    private fun hourOf(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).hour

    private val ids = (listOf(game.id) + game.copyIds).toSet()
    /** Partidas anotadas (sin los «último día jugado», que no tienen duración). */
    val sessions = allSessions.filter { !it.lastOnly && it.gameId in ids }
    /** Partidas medidas por Repisa (su hora y su duración son reales). */
    val measured = sessions.filter { it.measured }
    val hours: Float get() = game.totalMinutes / 60f
    val days: List<LocalDate> = sessions.map { date(it.start) }.distinct().sorted()

    val longestMeasured: Int = measured.maxOfOrNull { it.minutes } ?: 0
    val maxSessionsInADay: Int = sessions.groupingBy { date(it.start) }.eachCount().values.maxOrNull() ?: 0
    val maxMinutesInADay: Int = sessions.groupBy { date(it.start) }.values.maxOfOrNull { l -> l.sumOf { it.minutes } } ?: 0
    val months: Int = days.map { it.year * 12 + it.monthValue }.distinct().size
    val weekdays: Int = days.map { it.dayOfWeek }.distinct().size
    val seasons: Int = days.map { (it.monthValue % 12) / 3 }.distinct().size

    /** Más días seguidos jugando. */
    val streak: Int = run {
        var best = 0; var run = 0; var prev: LocalDate? = null
        days.forEach { d -> run = if (prev != null && ChronoUnit.DAYS.between(prev, d) == 1L) run + 1 else 1; best = maxOf(best, run); prev = d }
        best
    }
    /** Volvió a jugarlo tras 30 días o más sin tocarlo. */
    val comeback: Boolean = days.zipWithNext().any { (a, b) -> ChronoUnit.DAYS.between(a, b) >= 30 }
    val night: Boolean = measured.any { hourOf(it.start) in 0..4 }
    val morning: Boolean = measured.any { hourOf(it.start) in 5..8 }
    val evening: Boolean = sessions.any { hourOf(it.start) in 19..23 }
    val weekend: Boolean = days.any { it.dayOfWeek.value >= 6 }
    val acrossMidnight: Boolean = measured.any { date(it.start) != date(it.end) }
    val newYear: Boolean = days.any { it.monthValue == 1 && it.dayOfMonth == 1 }
    val anniversary: Boolean = days.size >= 2 && ChronoUnit.DAYS.between(days.first(), days.last()) >= 365

    val finished: Boolean get() = game.playStatus == PlayStatus.FINISHED || game.playStatus == PlayStatus.COMPLETED
    val hasPlatformAch: Boolean get() = (game.achTotal ?: 0) > 0
    val allPlatformAch: Boolean get() = hasPlatformAch && game.achUnlocked == game.achTotal

    /** Horas de HowLongToBeat o, si no se saben, [fallback]. */
    fun hltb(value: Float?, fallback: Float) = value?.takeIf { it > 0f } ?: fallback
}

/**
 * Logros de Repisa de cada juego: valen para cualquier juego (también Switch, 3DS o Wii U, que no tienen logros
 * propios) y la app los comprueba con lo que sabe del juego y sus partidas anotadas. La fecha es cuándo Repisa vio
 * que se cumplía. Bronce y plata cuentan para el platino de los juegos sin logros propios; los de oro son retos.
 */
enum class RepisaAchievement(
    private val nameRes: Int, private val descRes: Int, val icon: String, val tier: Tier,
    private val arg: Int? = null, val met: (GameAchContext) -> Boolean,
) {
    // ── Bronce
    FIRST(R.string.rach_first, R.string.rach_first_d, "🎮", Tier.BRONZE, met = { it.game.totalMinutes > 0 }),
    HOUR_1(R.string.rach_hour1, R.string.rd_hours1, "⏱", Tier.BRONZE, met = { it.hours >= 1f }),
    FROM_REPISA(R.string.rach_from_repisa, R.string.rach_from_repisa_d, "▶", Tier.BRONZE, met = { it.measured.isNotEmpty() }),
    SESSIONS_3(R.string.rach_sessions_3, R.string.rd_sessions, "🔁", Tier.BRONZE, 3, { it.sessions.size >= 3 }),
    DAYS_2(R.string.rach_days_2, R.string.rd_days, "📆", Tier.BRONZE, 2, { it.days.size >= 2 }),
    SESSION_30M(R.string.rach_session_30m, R.string.rach_session_30m_d, "☕", Tier.BRONZE, met = { it.longestMeasured >= 30 }),
    WEEKEND(R.string.rach_weekend, R.string.rach_weekend_d, "🛋", Tier.BRONZE, met = { it.weekend }),
    EVENING(R.string.rach_evening, R.string.rach_evening_d, "🌆", Tier.BRONZE, met = { it.evening }),
    RATED(R.string.rach_rated, R.string.rach_rated_d, "📝", Tier.BRONZE, met = { it.game.rating != null }),
    NOTES(R.string.rach_notes, R.string.rach_notes_d, "📓", Tier.BRONZE, met = { !it.game.notes.isNullOrBlank() }),
    FAVORITE(R.string.rach_favorite, R.string.rach_favorite_d, "♥", Tier.BRONZE, met = { it.game.favorite }),
    COLLECTION(R.string.rach_collection, R.string.rach_collection_d, "📂", Tier.BRONZE, met = { it.game.collections.isNotEmpty() }),
    STATUS(R.string.rach_status, R.string.rach_status_d, "🏷", Tier.BRONZE, met = { it.game.status != null }),
    FINISHED(R.string.rach_finished, R.string.rach_finished_d, "✓", Tier.BRONZE, met = { it.finished }),

    // ── Plata
    SESSION_2H(R.string.rach_session_2h, R.string.rd_session_hours, "🍿", Tier.SILVER, 2, { it.longestMeasured >= 120 }),
    DAY_3H(R.string.rach_day_3h, R.string.rd_day_hours, "📺", Tier.SILVER, 3, { it.maxMinutesInADay >= 180 }),
    SESSIONS_10(R.string.rach_sessions_10, R.string.rd_sessions, "🔟", Tier.SILVER, 10, { it.sessions.size >= 10 }),
    DAYS_7(R.string.rach_days_7, R.string.rd_days, "📅", Tier.SILVER, 7, { it.days.size >= 7 }),
    STREAK_3(R.string.rach_streak_3, R.string.rd_streak, "🔥", Tier.SILVER, 3, { it.streak >= 3 }),
    THREE_A_DAY(R.string.rach_three_a_day, R.string.rach_three_a_day_d, "🔂", Tier.SILVER, met = { it.maxSessionsInADay >= 3 }),
    NIGHT(R.string.rach_night, R.string.rach_night_d, "🦉", Tier.SILVER, met = { it.night }),
    MORNING(R.string.rach_morning, R.string.rach_morning_d, "🌅", Tier.SILVER, met = { it.morning }),
    MONTHS_3(R.string.rach_months_3, R.string.rd_months, "🗓", Tier.SILVER, 3, { it.months >= 3 }),
    WEEKDAYS(R.string.rach_weekdays, R.string.rach_weekdays_d, "🧮", Tier.SILVER, met = { it.weekdays >= 7 }),
    COMEBACK(R.string.rach_comeback, R.string.rach_comeback_d, "🔙", Tier.SILVER, met = { it.comeback }),
    HLTB_MAIN(R.string.rach_hltb_main, R.string.rach_hltb_main_d, "📖", Tier.SILVER, met = { it.hours >= it.hltb(it.game.hltbMain, 10f) }),
    HLTB_EXTRA(R.string.rach_hltb_extra, R.string.rach_hltb_extra_d, "🧭", Tier.SILVER, met = { it.hours >= it.hltb(it.game.hltbExtra, 20f) }),
    CRITIC(R.string.rach_critic, R.string.rach_critic_d, "🔍", Tier.SILVER, met = { (it.game.rating?.criteria?.size ?: 0) >= 4 }),
    DIARY(R.string.rach_diary, R.string.rach_diary_d, "🖋", Tier.SILVER, met = { (it.game.notes?.length ?: 0) >= 300 }),
    COMPLETED(R.string.rach_completed, R.string.rach_completed_d, "★", Tier.SILVER, met = { it.game.playStatus == PlayStatus.COMPLETED }),

    // ── Oro (retos)
    SESSION_4H(R.string.rach_session_4h, R.string.rd_session_hours, "🏃", Tier.GOLD, 4, { it.longestMeasured >= 240 }),
    SESSION_8H(R.string.rach_session_8h, R.string.rd_session_hours, "🛌", Tier.GOLD, 8, { it.longestMeasured >= 480 }),
    DAY_6H(R.string.rach_day_6h, R.string.rd_day_hours, "🎬", Tier.GOLD, 6, { it.maxMinutesInADay >= 360 }),
    SESSIONS_50(R.string.rach_sessions_50, R.string.rd_sessions, "🎯", Tier.GOLD, 50, { it.sessions.size >= 50 }),
    SESSIONS_100(R.string.rach_sessions_100, R.string.rd_sessions, "💯", Tier.GOLD, 100, { it.sessions.size >= 100 }),
    DAYS_30(R.string.rach_days_30, R.string.rd_days, "🗒", Tier.GOLD, 30, { it.days.size >= 30 }),
    STREAK_7(R.string.rach_streak_7, R.string.rd_streak, "♨", Tier.GOLD, 7, { it.streak >= 7 }),
    STREAK_30(R.string.rach_streak_30, R.string.rd_streak, "☄", Tier.GOLD, 30, { it.streak >= 30 }),
    MONTHS_6(R.string.rach_months_6, R.string.rd_months, "⏳", Tier.GOLD, 6, { it.months >= 6 }),
    MONTHS_12(R.string.rach_months_12, R.string.rd_months, "🎂", Tier.GOLD, 12, { it.months >= 12 }),
    SEASONS(R.string.rach_seasons, R.string.rach_seasons_d, "🍂", Tier.GOLD, met = { it.seasons >= 4 }),
    MIDNIGHT(R.string.rach_midnight, R.string.rach_midnight_d, "🕛", Tier.GOLD, met = { it.acrossMidnight }),
    NEW_YEAR(R.string.rach_new_year, R.string.rach_new_year_d, "🎆", Tier.GOLD, met = { it.newYear }),
    ANNIVERSARY(R.string.rach_anniversary, R.string.rach_anniversary_d, "🎉", Tier.GOLD, met = { it.anniversary }),
    HLTB_COMPLETE(R.string.rach_hltb_complete, R.string.rach_hltb_complete_d, "🧩", Tier.GOLD, met = { it.hours >= it.hltb(it.game.hltbComplete, 40f) }),
    HLTB_DOUBLE(R.string.rach_hltb_double, R.string.rach_hltb_double_d, "🐢", Tier.GOLD, met = { it.hours >= 2 * it.hltb(it.game.hltbComplete, 40f) }),
    SPEEDRUN(R.string.rach_speedrun, R.string.rach_speedrun_d, "⚡", Tier.GOLD,
        met = { it.finished && it.game.hltbMain != null && it.hours > 0f && it.hours < it.game.hltbMain }),
    HOURS_50(R.string.rach_hours_50, R.string.rd_hours, "⌛", Tier.GOLD, 50, { it.hours >= 50f }),
    HOURS_100(R.string.rach_hours_100, R.string.rd_hours, "🔥", Tier.GOLD, 100, { it.hours >= 100f }),
    HOURS_250(R.string.rach_hours_250, R.string.rd_hours, "🏔", Tier.GOLD, 250, { it.hours >= 250f }),
    HOURS_500(R.string.rach_hours_500, R.string.rd_hours, "🌋", Tier.GOLD, 500, { it.hours >= 500f }),
    MASTERPIECE(R.string.rach_masterpiece, R.string.rach_masterpiece_d, "🌟", Tier.GOLD, met = { (it.game.rating?.overall ?: 0f) >= 0.95f }),
    HUNTER(R.string.rach_hunter, R.string.rach_hunter_d, "🏆", Tier.GOLD, met = { it.allPlatformAch });

    val title: String get() = L10n.s(nameRes)
    val description: String get() = if (arg != null) L10n.s(descRes, arg) else L10n.s(descRes)

    companion object {
        /** Los que cuentan para el platino de un juego sin logros propios. */
        val forPlatinum = entries.filter { it.tier == Tier.BRONZE || it.tier == Tier.SILVER }
    }
}

/**
 * Platino de un juego: estado «100 %» y todos sus logros. Si el juego tiene logros propios (Steam,
 * RetroAchievements, marcados a mano), todos esos; si no (Switch...), todos los de Repisa de bronce y plata.
 */
object Platinum {
    fun isPlatinum(ctx: GameAchContext): Boolean =
        ctx.game.playStatus == PlayStatus.COMPLETED &&
            (if (ctx.hasPlatformAch) ctx.allPlatformAch else RepisaAchievement.forPlatinum.all { it.met(ctx) })

    /** Lo que falta: (logros que faltan, ¿falta el estado 100 %?). */
    fun missing(ctx: GameAchContext): Pair<Int, Boolean> {
        val left = if (ctx.hasPlatformAch) (ctx.game.achTotal ?: 0) - (ctx.game.achUnlocked ?: 0)
            else RepisaAchievement.forPlatinum.count { !it.met(ctx) }
        return left to (ctx.game.playStatus != PlayStatus.COMPLETED)
    }
}

/** Lo que Repisa sabe de toda tu biblioteca para los logros de la app. */
class AppAchContext(
    val games: List<Game>,
    val sessions: List<PlaySession>,
    val platinums: Int,
    val collections: Int,
    val wishlist: Int,
    val themeName: String,
) {
    private val zone = ZoneId.systemDefault()
    val played = games.filter { it.totalMinutes > 0 }
    val totalHours = games.sumOf { it.totalMinutes } / 60
    val finished = games.count { it.playStatus == PlayStatus.FINISHED || it.playStatus == PlayStatus.COMPLETED }
    val completed = games.count { it.playStatus == PlayStatus.COMPLETED }
    val abandoned = games.count { it.playStatus == PlayStatus.ABANDONED }
    val rated = games.count { it.rating != null }
    val systems = played.map { it.consoleId }.distinct().size
    val realSessions = sessions.filter { !it.lastOnly }
    val platformAch = games.sumOf { it.achUnlocked ?: 0 }
    val perfect = games.count { (it.achTotal ?: 0) > 0 && it.achUnlocked == it.achTotal }
    val streak: Int = run {
        val days = realSessions.map { Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate() }.distinct().sorted()
        var best = 0; var run = 0; var prev: LocalDate? = null
        days.forEach { d -> run = if (prev != null && ChronoUnit.DAYS.between(prev, d) == 1L) run + 1 else 1; best = maxOf(best, run); prev = d }
        best
    }
}

/** Logros de la app: por toda la biblioteca (cuántos juegos, horas, platinos, rachas...), con su progreso. */
enum class AppAchievement(
    private val nameRes: Int, private val descRes: Int, val icon: String, val tier: Tier, val target: Int,
    val value: (AppAchContext) -> Int,
) {
    LIB_10(R.string.aach_lib_10, R.string.ad_games, "📚", Tier.BRONZE, 10, { it.games.size }),
    LIB_50(R.string.aach_lib_50, R.string.ad_games, "📚", Tier.BRONZE, 50, { it.games.size }),
    LIB_100(R.string.aach_lib_100, R.string.ad_games, "🗄", Tier.SILVER, 100, { it.games.size }),
    LIB_250(R.string.aach_lib_250, R.string.ad_games, "🏛", Tier.GOLD, 250, { it.games.size }),
    LIB_500(R.string.aach_lib_500, R.string.ad_games, "🏰", Tier.GOLD, 500, { it.games.size }),
    TOT_100(R.string.aach_tot_100, R.string.ad_hours, "⏱", Tier.BRONZE, 100, { it.totalHours }),
    TOT_500(R.string.aach_tot_500, R.string.ad_hours, "⌛", Tier.SILVER, 500, { it.totalHours }),
    TOT_1000(R.string.aach_tot_1000, R.string.ad_hours, "🔥", Tier.GOLD, 1000, { it.totalHours }),
    TOT_2500(R.string.aach_tot_2500, R.string.ad_hours, "🌋", Tier.GOLD, 2500, { it.totalHours }),
    FIN_1(R.string.aach_fin_1, R.string.ad_finished, "✓", Tier.BRONZE, 1, { it.finished }),
    FIN_10(R.string.aach_fin_10, R.string.ad_finished, "🎞", Tier.BRONZE, 10, { it.finished }),
    FIN_25(R.string.aach_fin_25, R.string.ad_finished, "🎬", Tier.SILVER, 25, { it.finished }),
    FIN_50(R.string.aach_fin_50, R.string.ad_finished, "🏁", Tier.GOLD, 50, { it.finished }),
    FIN_100(R.string.aach_fin_100, R.string.ad_finished, "🗿", Tier.GOLD, 100, { it.finished }),
    CMP_1(R.string.aach_cmp_1, R.string.ad_completed, "★", Tier.BRONZE, 1, { it.completed }),
    CMP_10(R.string.aach_cmp_10, R.string.ad_completed, "✨", Tier.SILVER, 10, { it.completed }),
    CMP_25(R.string.aach_cmp_25, R.string.ad_completed, "🌠", Tier.GOLD, 25, { it.completed }),
    PLAT_1(R.string.aach_plat_1, R.string.ad_platinum, "💎", Tier.SILVER, 1, { it.platinums }),
    PLAT_5(R.string.aach_plat_5, R.string.ad_platinum, "💎", Tier.GOLD, 5, { it.platinums }),
    PLAT_10(R.string.aach_plat_10, R.string.ad_platinum, "👑", Tier.GOLD, 10, { it.platinums }),
    PLAT_25(R.string.aach_plat_25, R.string.ad_platinum, "🏆", Tier.PLATINUM, 25, { it.platinums }),
    RATE_1(R.string.aach_rate_1, R.string.ad_rated, "📝", Tier.BRONZE, 1, { it.rated }),
    RATE_10(R.string.aach_rate_10, R.string.ad_rated, "🔍", Tier.SILVER, 10, { it.rated }),
    RATE_50(R.string.aach_rate_50, R.string.ad_rated, "🎓", Tier.GOLD, 50, { it.rated }),
    SYS_3(R.string.aach_sys_3, R.string.ad_systems, "🕹", Tier.BRONZE, 3, { it.systems }),
    SYS_10(R.string.aach_sys_10, R.string.ad_systems, "🎛", Tier.SILVER, 10, { it.systems }),
    SYS_20(R.string.aach_sys_20, R.string.ad_systems, "🗺", Tier.GOLD, 20, { it.systems }),
    STREAK_7(R.string.aach_streak_7, R.string.ad_streak, "🔥", Tier.BRONZE, 7, { it.streak }),
    STREAK_30(R.string.aach_streak_30, R.string.ad_streak, "☄", Tier.SILVER, 30, { it.streak }),
    STREAK_100(R.string.aach_streak_100, R.string.ad_streak, "🌞", Tier.GOLD, 100, { it.streak }),
    SES_10(R.string.aach_ses_10, R.string.ad_sessions, "🔁", Tier.BRONZE, 10, { it.realSessions.size }),
    SES_100(R.string.aach_ses_100, R.string.ad_sessions, "🎯", Tier.SILVER, 100, { it.realSessions.size }),
    SES_500(R.string.aach_ses_500, R.string.ad_sessions, "💯", Tier.GOLD, 500, { it.realSessions.size }),
    COLL_1(R.string.aach_coll_1, R.string.ad_collections, "📂", Tier.BRONZE, 1, { it.collections }),
    COLL_5(R.string.aach_coll_5, R.string.ad_collections, "🗂", Tier.SILVER, 5, { it.collections }),
    ACH_100(R.string.aach_ach_100, R.string.ad_ach, "🏅", Tier.BRONZE, 100, { it.platformAch }),
    ACH_1000(R.string.aach_ach_1000, R.string.ad_ach, "🎖", Tier.SILVER, 1000, { it.platformAch }),
    ACH_5000(R.string.aach_ach_5000, R.string.ad_ach, "🏆", Tier.GOLD, 5000, { it.platformAch }),
    PERF_1(R.string.aach_perf_1, R.string.ad_perfect, "🥇", Tier.SILVER, 1, { it.perfect }),
    PERF_10(R.string.aach_perf_10, R.string.ad_perfect, "🏵", Tier.GOLD, 10, { it.perfect }),
    ABANDON(R.string.aach_abandon, R.string.aach_abandon_d, "🚪", Tier.BRONZE, 1, { it.abandoned }),
    THEME(R.string.aach_theme, R.string.aach_theme_d, "🎨", Tier.BRONZE, 1, { if (it.themeName != "WALNUT") 1 else 0 }),
    CUSTOM_THEME(R.string.aach_custom_theme, R.string.aach_custom_theme_d, "🖌", Tier.BRONZE, 1, { if (it.themeName == "CUSTOM") 1 else 0 }),
    WISH_10(R.string.aach_wish_10, R.string.aach_wish_10_d, "⭐", Tier.BRONZE, 10, { it.wishlist });

    val title: String get() = L10n.s(nameRes)
    val description: String get() = if (descArg) L10n.s(descRes, target) else L10n.s(descRes)
    private val descArg: Boolean get() = descRes != R.string.aach_abandon_d && descRes != R.string.aach_theme_d &&
        descRes != R.string.aach_custom_theme_d && descRes != R.string.aach_wish_10_d
    fun met(ctx: AppAchContext) = value(ctx) >= target
}
