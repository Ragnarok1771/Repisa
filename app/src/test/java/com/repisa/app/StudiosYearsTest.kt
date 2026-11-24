package com.repisa.app

import com.repisa.app.model.Studios
import com.repisa.app.sources.Years
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudiosYearsTest {
    @Test fun sameStudioWrittenDifferently() {
        val keys = listOf("ATLUS", "Atlus", "Atlus Co., Ltd.", "ATLUS CO.,LTD").map { Studios.split(it).single().let(Studios::key) }.toSet()
        assertEquals(setOf("atlus"), keys)
        assertEquals("sega", Studios.key(Studios.split("Sega Corporation").single()))
        assertEquals("squareenix", Studios.key(Studios.split("Square Enix Co., Ltd.").single()))
        assertEquals(listOf("Intelligent Systems", "Koei Tecmo"), Studios.split("Intelligent Systems, Koei Tecmo"))
        assertEquals(listOf("Game Freak"), Studios.split("Game Freak, Inc."))
        assertEquals("Atlus", Studios.display(listOf("ATLUS", "Atlus", "Atlus")))
        assertEquals("Atlus", Studios.display(listOf("ATLUS", "Atlus")))
        assertEquals(emptyList<String>(), Studios.split(null))
    }

    @Test fun zeroDateIsUnknownYear() {
        assertNull(Years.valid(1969))
        assertNull(Years.valid(1970))
        assertEquals(2000, Years.valid(2000))
        assertEquals(1972, Years.valid(1972))
        assertNull(Years.valid(3000))
    }
}
