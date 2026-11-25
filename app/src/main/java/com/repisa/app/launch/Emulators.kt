package com.repisa.app.launch

import android.content.Intent

/**
 * Emuladores conocidos de Android y cómo arrancar un juego en cada uno (las mismas formas que usa ES-DE).
 * El orden de la lista es la preferencia: para cada sistema se elige el primero que esté instalado.
 *
 * Como los nombres internos cambian entre versiones, [activities] son candidatas: se usa la primera que exista
 * en el emulador instalado; si ninguna existe se abre con ACTION_VIEW dirigido al emulador, y si tampoco,
 * se abre el emulador sin juego. Nunca se muestra el selector de apps de Android.
 */
data class EmuProfile(
    val name: String,
    val packages: List<String>,
    val consoles: Set<String>,
    val activities: List<String> = emptyList(),
    val action: String = Intent.ACTION_VIEW,
    /** Extra donde va la URI del juego (content://...). */
    val uriExtra: String? = null,
    /** Extra donde va la ruta del archivo (/storage/emulated/0/...). */
    val pathExtra: String? = null,
    /** El emulador no sabe abrir un archivo concreto (Vita3K, RPCS3...): se abre sin juego. */
    val openOnly: Boolean = false,
    /** RetroArch: se arranca con el core de [RetroCores]. */
    val retroArch: Boolean = false,
    /** Extras sí/no que necesita el emulador para arrancar el juego directamente. */
    val boolExtras: Map<String, Boolean> = emptyMap(),
)

object Emulators {
    private const val NFC = "android.nfc.action.TECH_DISCOVERED" // así arrancan juego yuzu y sus derivados
    private val YUZU_ACT = "org.yuzu.yuzu_emu.activities.EmulationActivity"
    private val CITRA_ACT = "org.citra.citra_emu.activities.EmulationActivity"
    private val EXPLUS = "com.imagine.BaseActivity"
    private val SEGA8 = setOf("mastersystem", "megadrive", "gamegear", "segacd", "sega32x")

    val all: List<EmuProfile> = listOf(
        // Nintendo Switch
        EmuProfile("Eden", listOf("dev.eden.eden_emulator", "dev.legacy.eden_emulator", "dev.eden.eden_nightly", "com.miHoYo.Yuanshen"),
            setOf("switch"), listOf(YUZU_ACT, "dev.eden.eden_emulator.activities.EmulationActivity"), NFC),
        // Switch 2 (Banana Native y sus variantes): se le pasa la ruta de la carpeta del juego o del .nup en «game».
        EmuProfile("Banana Native", listOf("dev.bananative.android", "dev.nxnative.android.play", "dev.nxnative.android", "com.swap2native"),
            setOf("switch2"), listOf("dev.bananative.android.Launch", "dev.nxnative.android.play.Launch", "dev.nxnative.android.Launch",
                "com.swap2native.Launch", "dev.nxnative.android.MainActivity", "dev.bananative.android.MainActivity", "com.swap2native.MainActivity"),
            Intent.ACTION_MAIN, pathExtra = "game", boolExtras = mapOf("run" to true)),
        EmuProfile("Citron", listOf("org.citron.citron_emu", "org.citron.emu"), setOf("switch"),
            listOf("org.citron.citron_emu.activities.EmulationActivity", YUZU_ACT), NFC),
        EmuProfile("Sudachi", listOf("org.sudachi.sudachi_emu", "org.sudachi.sudachi_emu.ea"), setOf("switch"),
            listOf("org.sudachi.sudachi_emu.activities.EmulationActivity", YUZU_ACT), NFC),
        EmuProfile("yuzu", listOf("org.yuzu.yuzu_emu", "org.yuzu.yuzu_emu.ea"), setOf("switch"), listOf(YUZU_ACT), NFC),
        EmuProfile("Kenji-NX", listOf("org.kenjinx.android", "org.ryujinx.android"), setOf("switch")),
        // Nintendo 3DS
        EmuProfile("Azahar", listOf("org.azahar_emu.azahar", "io.github.azahar_emu.azahar"), setOf("3ds"),
            listOf(CITRA_ACT, "org.azahar_emu.azahar.activities.EmulationActivity")),
        EmuProfile("Lime3DS", listOf("io.github.lime3ds.android"), setOf("3ds"),
            listOf("io.github.lime3ds.android.activities.EmulationActivity", CITRA_ACT)),
        EmuProfile("Mandarine", listOf("io.github.mandarine3ds.mandarine"), setOf("3ds"),
            listOf("io.github.mandarine3ds.mandarine.activities.EmulationActivity", CITRA_ACT)),
        EmuProfile("Citra", listOf("org.citra.emu", "org.citra.citra_emu", "org.citra.citra_emu.canary"), setOf("3ds"),
            listOf(CITRA_ACT, "org.citra.emu.ui.EmulationActivity")),
        EmuProfile("Panda3DS", listOf("com.panda3ds.pandroid"), setOf("3ds"), listOf("com.panda3ds.pandroid.app.GameActivity")),
        // Nintendo DS
        // WatermelonDS (melonDS con soporte de dos pantallas) primero; usa el mismo arranque que melonDS.
        EmuProfile("melonDS", listOf("me.magnum.melondualds", "me.magnum.melondualds.nightly", "me.magnum.melonds", "me.magnum.melonds.nightly"), setOf("nds"),
            listOf("me.magnum.melonds.ui.emulator.EmulatorActivity"), Intent.ACTION_MAIN, uriExtra = "uri"),
        EmuProfile("DraStic", listOf("com.dsemu.drastic"), setOf("nds"),
            listOf("com.dsemu.drastic.DraSticActivity"), Intent.ACTION_MAIN, pathExtra = "GAMEPATH"),
        // GameCube / Wii
        EmuProfile("Dolphin", listOf("org.dolphinemu.dolphinemu", "org.dolphinemu.mmjr", "org.dolphinemu.dolphinemu.benchmark"),
            setOf("gc", "wii"), listOf("org.dolphinemu.dolphinemu.ui.main.MainActivity"), Intent.ACTION_MAIN, uriExtra = "AutoStartFile"),
        // Wii U
        EmuProfile("Cemu", listOf("info.cemu.cemu"), setOf("wiiu"),
            listOf("info.cemu.cemu.emulation.EmulationActivity"), Intent.ACTION_MAIN, uriExtra = "launchPath"),
        // Nintendo 64
        EmuProfile("M64Plus FZ", listOf("org.mupen64plusae.v3.fzurita", "org.mupen64plusae.v3.fzurita.pro", "org.mupen64plusae.v3.alpha"),
            setOf("n64"), listOf("paulscode.android.mupen64plusae.SplashActivity")),
        // Game Boy / GBA
        EmuProfile("Pizza Boy GBA", listOf("it.dbtecno.pizzaboygbapro", "it.dbtecno.pizzaboygba"), setOf("gba")),
        EmuProfile("My Boy!", listOf("com.fastemulator.gba", "com.fastemulator.gbafree"), setOf("gba"),
            listOf("com.fastemulator.gba.EmulatorActivity")),
        EmuProfile("Pizza Boy GBC", listOf("it.dbtecno.pizzaboypro", "it.dbtecno.pizzaboy"), setOf("gb", "gbc")),
        EmuProfile("My OldBoy!", listOf("com.fastemulator.gbc", "com.fastemulator.gbcfree"), setOf("gb", "gbc"),
            listOf("com.fastemulator.gbc.EmulatorActivity")),
        EmuProfile("GBA.emu", listOf("com.explusalpha.GbaEmu"), setOf("gba"), listOf(EXPLUS)),
        EmuProfile("GBC.emu", listOf("com.explusalpha.GbcEmu"), setOf("gb", "gbc"), listOf(EXPLUS)),
        // NES / SNES
        EmuProfile("Snes9x EX+", listOf("com.explusalpha.Snes9xPlus"), setOf("snes"), listOf(EXPLUS)),
        EmuProfile("NES.emu", listOf("com.explusalpha.NesEmu"), setOf("nes"), listOf(EXPLUS)),
        // PlayStation
        EmuProfile("DuckStation", listOf("com.github.stenzek.duckstation"), setOf("psx"),
            listOf("com.github.stenzek.duckstation.EmulationActivity"), Intent.ACTION_MAIN, uriExtra = "bootPath"),
        EmuProfile("NetherSX2", listOf("xyz.aethersx2.android", "xyz.aethersx2.custom", "xyz.nethersx2.android"), setOf("ps2"),
            listOf("xyz.aethersx2.android.EmulationActivity"), Intent.ACTION_MAIN, uriExtra = "bootPath"),
        EmuProfile("PPSSPP", listOf("org.ppsspp.ppssppgold", "org.ppsspp.ppsspp"), setOf("psp"),
            listOf("org.ppsspp.ppsspp.PpssppActivity")),
        EmuProfile("Vita3K", listOf("org.vita3k.emulator"), setOf("psvita"), openOnly = true),
        EmuProfile("aPS3e", listOf("aenu.aps3e"), setOf("ps3"), openOnly = true),
        EmuProfile("RPCS3", listOf("net.rpcs3"), setOf("ps3"), openOnly = true),
        // Sega y otros
        EmuProfile("Flycast", listOf("com.flycast.emulator"), setOf("dreamcast"), listOf("com.flycast.emulator.MainActivity")),
        EmuProfile("Redream", listOf("io.recompiled.redream"), setOf("dreamcast"), listOf("io.recompiled.redream.MainActivity")),
        EmuProfile("Yaba Sanshiro 2", listOf("org.devmiyax.yabasanshioro2.pro", "org.devmiyax.yabasanshioro2"), setOf("saturn"),
            listOf("org.uoyabause.android.Yabause"), Intent.ACTION_MAIN, uriExtra = "org.uoyabause.android.FileNameUri"),
        EmuProfile("MD.emu", listOf("com.explusalpha.MdEmu"), SEGA8, listOf(EXPLUS)),
        EmuProfile("Saturn.emu", listOf("com.explusalpha.SaturnEmu"), setOf("saturn"), listOf(EXPLUS)),
        EmuProfile("PCE.emu", listOf("com.explusalpha.PceEmu"), setOf("pcengine"), listOf(EXPLUS)),
        EmuProfile("NEO.emu", listOf("com.explusalpha.NeoEmu"), setOf("neogeo"), listOf(EXPLUS)),
        // RetroArch: último recurso para los sistemas clásicos.
        EmuProfile("RetroArch", listOf("com.retroarch.aarch64", "com.retroarch", "com.retroarch.ra32"),
            RetroCores.cores.keys, retroArch = true),
    )

    private val byPackage: Map<String, EmuProfile> = all.flatMap { p -> p.packages.map { it to p } }.toMap()

    fun profileFor(pkg: String): EmuProfile? = byPackage[pkg]

    /** Emuladores conocidos para un sistema, en orden de preferencia: (perfil, paquete). */
    fun candidates(consoleId: String): List<Pair<EmuProfile, String>> =
        all.filter { consoleId in it.consoles }.flatMap { p -> p.packages.map { p to it } }

    fun isKnown(pkg: String) = pkg in byPackage
}

/** Core de RetroArch por defecto para cada sistema (los mismos que propone ES-DE). */
object RetroCores {
    val cores = mapOf(
        "nes" to "fceumm", "snes" to "snes9x", "n64" to "mupen64plus_next_gles3", "gb" to "gambatte", "gbc" to "gambatte",
        "gba" to "mgba", "nds" to "melondsds", "psx" to "swanstation", "psp" to "ppsspp",
        "mastersystem" to "genesis_plus_gx", "megadrive" to "genesis_plus_gx", "gamegear" to "genesis_plus_gx",
        "segacd" to "genesis_plus_gx", "sega32x" to "picodrive", "saturn" to "yabasanshiro", "dreamcast" to "flycast",
        "pcengine" to "mednafen_pce_fast", "neogeo" to "fbneo", "arcade" to "fbneo", "atari2600" to "stella",
        "dos" to "dosbox_pure", "3ds" to "citra", "gc" to "dolphin", "wii" to "dolphin",
    )

    fun corePath(pkg: String, consoleId: String): String? =
        cores[consoleId]?.let { "/data/data/$pkg/cores/${it}_libretro_android.so" }
}
