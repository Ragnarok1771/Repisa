package com.repisa.app

import com.repisa.app.model.Criterion
import com.repisa.app.model.Rating
import com.repisa.app.model.RatingScale
import com.repisa.app.sources.GameInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class RatingTest {
    @Test
    fun weightedAverage() {
        // 30 % con un 9/10 y 70 % con un 6/10 → 6,9
        val r = Rating(listOf(Criterion("Historia", 30f, 0.9f), Criterion("Jugabilidad", 70f, 0.6f)))
        assertEquals(0.69f, r.overall, 0.0001f)
        assertEquals("6,9", RatingScale.TEN.format(r.overall))
        assertEquals("69", RatingScale.HUNDRED.format(r.overall))
        assertEquals(3.45f, r.stars, 0.0001f)
    }

    @Test
    fun starsFollowScale() {
        assertEquals(5f, Rating(listOf(Criterion("Todo", 100f, 1f))).stars, 0.0001f)
        // 95/100 → 4,75 estrellas (cuatro y tres cuartos)
        assertEquals(4.75f, Rating(listOf(Criterion("Todo", 100f, 0.95f))).stars, 0.0001f)
    }

    @Test
    fun weightsNeedNotSumToHundred() {
        val r = Rating(listOf(Criterion("A", 1f, 1f), Criterion("B", 1f, 0f)))
        assertEquals(0.5f, r.overall, 0.0001f)
        assertEquals(0.5f, Rating(listOf(Criterion("A", 0f, 1f), Criterion("B", 0f, 0f))).overall, 0.0001f)
    }

    @Test
    fun metacriticSlugs() {
        assertEquals("the-legend-of-zelda-tears-of-the-kingdom", GameInfo.slug("The Legend of Zelda: Tears of the Kingdom"))
        assertEquals("pokemon-legends-arceus", GameInfo.slug("Pokémon Legends: Arceus"))
        assertEquals("luigis-mansion-3", GameInfo.slug("Luigi's Mansion 3"))
    }
}
