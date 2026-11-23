package com.repisa.app.model

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
enum class RatingScale(val max: Int, val label: String) {
    FIVE(5, "Del 0 al 5"), TEN(10, "Del 0 al 10"), HUNDRED(100, "Del 0 al 100");

    /** Convierte 0..1 a la escala, con los decimales que tienen sentido en cada una. */
    fun format(normalized: Float): String {
        val v = normalized * max
        return if (max >= 100) Math.round(v).toString()
        else if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v).replace('.', ',')
    }
}

/** Plantilla inicial de criterios; el usuario la cambia en Ajustes y en cada juego. */
val DefaultCriteria = listOf(
    Criterion("Jugabilidad", 30f, 0f),
    Criterion("Historia", 25f, 0f),
    Criterion("Gráficos y arte", 15f, 0f),
    Criterion("Música y sonido", 15f, 0f),
    Criterion("Duración y rejugabilidad", 15f, 0f),
)
