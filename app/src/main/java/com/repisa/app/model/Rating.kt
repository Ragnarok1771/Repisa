package com.repisa.app.model

import com.repisa.app.R
import com.repisa.app.i18n.L10n
import kotlinx.serialization.Serializable

/**
 * Un aspecto que el usuario califica (lo que quiera: "Historia", "Música", "El cielo"...).
 * [weight] es el peso en % y [score] la nota normalizada de 0 a 1, así se puede cambiar
 * la escala (1-5, 1-10, 1-100) sin perder las notas ya puestas.
 */
@Serializable
data class Criterion(val name: String, val weight: Float, val score: Float)

@Serializable
data class Rating(
    val criteria: List<Criterion>,
    val note: String? = null,
    val ratedAt: Long = System.currentTimeMillis(),
) {
    /** Nota global de 0 a 1: media ponderada por los pesos (si todos pesan 0, media simple). */
    val overall: Float
        get() {
            if (criteria.isEmpty()) return 0f
            val totalWeight = criteria.sumOf { it.weight.toDouble() }
            return if (totalWeight <= 0.0) criteria.map { it.score }.average().toFloat()
            else (criteria.sumOf { it.weight.toDouble() * it.score } / totalWeight).toFloat()
        }

    /** Estrellas de 0 a 5 (con fracciones: 4,75 = cuatro estrellas y tres cuartos). */
    val stars: Float get() = overall * 5f
}

/** Escala con la que el usuario pone y ve las notas. */
enum class RatingScale(val max: Int, private val labelRes: Int) {
    FIVE(5, R.string.scale_5), TEN(10, R.string.scale_10), HUNDRED(100, R.string.scale_100);

    val label: String get() = L10n.s(labelRes)

    /** Convierte 0..1 a la escala, con los decimales que tienen sentido en cada una (coma o punto según el idioma). */
    fun format(normalized: Float): String {
        val v = Math.round(normalized * max * 10) / 10f
        return if (max >= 100) Math.round(v).toString()
        else if (v % 1f == 0f) v.toInt().toString() else L10n.decimal(v)
    }
}

/** Plantilla inicial de criterios (en el idioma elegido); el usuario la cambia en Mis calificaciones y en cada juego. */
val DefaultCriteria: List<Criterion>
    get() = listOf(
        Criterion(L10n.s(R.string.crit_gameplay), 30f, 0f),
        Criterion(L10n.s(R.string.crit_story), 25f, 0f),
        Criterion(L10n.s(R.string.crit_graphics), 15f, 0f),
        Criterion(L10n.s(R.string.crit_music), 15f, 0f),
        Criterion(L10n.s(R.string.crit_length), 15f, 0f),
    )
