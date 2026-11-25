package com.repisa.app

import com.repisa.app.sources.Switch2Titles
import org.junit.Assert.assertEquals
import org.junit.Test

class Switch2Test {
    private fun npdm(titleId: Long): ByteArray {
        val b = ByteArray(0x200)
        "META".toByteArray().copyInto(b, 0)
        val aci = 0x100
        b[0x70] = aci.toByte(); b[0x71] = (aci shr 8).toByte()
        "ACI0".toByteArray().copyInto(b, aci)
        for (i in 0 until 8) b[aci + 0x10 + i] = (titleId shr (8 * i)).toByte()
        return b
    }

    @Test fun leeElNumeroDelJuego() {
        val id = Switch2Titles.programId(npdm(0x040074A01BF12000L))
        assertEquals("040074A01BF12000", id)
        assertEquals("Donkey Kong Bananza", Switch2Titles.name(id!!))
    }

    @Test fun reconoceArchivosPorNombre() {
        assertEquals("The Legend of Zelda: Ocarina of Time", Switch2Titles.byName("Ocarina of Time"))
        assertEquals("Donkey Kong Bananza", Switch2Titles.byName("Donkey Kong Bonanza"))
    }
}
