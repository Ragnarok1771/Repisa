package com.repisa.app

import com.repisa.app.launch.Emulators
import com.repisa.app.launch.RetroCores
import com.repisa.app.model.Consoles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmulatorsTest {
    private val ids = Consoles.all.map { it.id }.toSet()

    @Test fun everyProfilePointsToRealSystems() {
        Emulators.all.forEach { p -> p.consoles.forEach { assertTrue("${p.name}: $it", it in ids) } }
        RetroCores.cores.keys.forEach { assertTrue(it, it in ids) }
    }

    @Test fun packagesAreUnique() {
        val pkgs = Emulators.all.flatMap { it.packages }
        assertEquals(pkgs.size, pkgs.toSet().size)
    }

    @Test fun preferredOrder() {
        assertEquals("Eden", Emulators.candidates("switch").first().first.name)
        assertEquals("melonDS", Emulators.candidates("nds").first().first.name)
        assertEquals("Dolphin", Emulators.candidates("gc").first().first.name)
        // RetroArch queda siempre como último recurso
        assertEquals("RetroArch", Emulators.candidates("snes").last().first.name)
        assertEquals("/data/data/com.retroarch.aarch64/cores/snes9x_libretro_android.so",
            RetroCores.corePath("com.retroarch.aarch64", "snes"))
    }
}
