package com.repisa.app.model

/**
 * Cómo se dibuja la caja física de cada sistema.
 * [aspect] = ancho / alto de la portada frontal.
 */
data class CaseStyle(
    val aspect: Float,
    /** Color del plástico/cartón de la caja (lateral y bordes). */
    val caseColor: Long,
    /** Franja superior (como las cajas de PS2 o Switch). null = sin franja. */
    val bandColor: Long? = null,
    val bandText: String? = null,
    val bandTextColor: Long = 0xFFFFFFFF,
    /** Grosor relativo del lateral de la caja. */
    val depth: Float = 0.09f,
    /** Altura de la caja real en milímetros (una caja de DVD mide 190 mm). */
    val heightMm: Int = 190,
) {
    /** Altura relativa a una caja de DVD, para que en la repisa cada caja tenga su tamaño real. */
    val heightScale: Float get() = heightMm / 190f
}

data class Console(
    val id: String,
    val name: String,
    val maker: String,
    val year: Int,
    /** Nombres de carpeta habituales (estilo ES-DE / RetroArch). */
    val folders: List<String>,
    val extensions: Set<String>,
    /** Nombre del sistema en libretro-thumbnails (portadas gratuitas). */
    val libretro: String? = null,
    val case: CaseStyle,
    /** Palabras clave para reconocer el sistema en nombres de cores o playlists. */
    val keywords: List<String> = emptyList(),
)

private val CART = setOf("zip", "7z")

object Consoles {
    private const val BLACK = 0xFF16161A
    private const val DVD_BLACK = 0xFF101014
    private const val CLEAR = 0xFF2A2D33
    private const val CARDBOARD = 0xFF2B2420

    val all: List<Console> = listOf(
        Console("pc", "PC", "PC", 1981, listOf("pc", "windows", "steam", "epic"), setOf("exe", "lnk", "desktop"),
            null, CaseStyle(0.667f, DVD_BLACK, depth = 0.08f, heightMm = 190)),

        Console("nes", "NES / Famicom", "Nintendo", 1983, listOf("nes", "famicom", "fds"),
            setOf("nes", "unf", "unif", "fds") + CART, "Nintendo - Nintendo Entertainment System",
            CaseStyle(0.72f, CARDBOARD, depth = 0.12f, heightMm = 178), listOf("fceumm", "nestopia", "mesen", "nes")),
        Console("snes", "Super Nintendo", "Nintendo", 1990, listOf("snes", "sfc", "superfamicom", "snesna"),
            setOf("sfc", "smc", "fig", "swc", "bs") + CART, "Nintendo - Super Nintendo Entertainment System",
            CaseStyle(1.38f, CARDBOARD, depth = 0.10f, heightMm = 133), listOf("snes9x", "bsnes", "super nintendo")),
        Console("n64", "Nintendo 64", "Nintendo", 1996, listOf("n64", "nintendo64"),
            setOf("n64", "z64", "v64") + CART, "Nintendo - Nintendo 64",
            CaseStyle(1.38f, CARDBOARD, depth = 0.10f, heightMm = 130), listOf("mupen64", "parallel", "nintendo 64")),
        Console("gc", "GameCube", "Nintendo", 2001, listOf("gc", "gamecube", "ngc"),
            setOf("iso", "gcm", "rvz", "ciso", "gcz", "dol"), "Nintendo - GameCube",
            CaseStyle(0.71f, BLACK, 0xFF4B3F8C, "GAMECUBE", heightMm = 190), listOf("dolphin", "gamecube")),
        Console("wii", "Wii", "Nintendo", 2006, listOf("wii"),
            setOf("wbfs", "iso", "rvz", "wia", "ciso"), "Nintendo - Wii",
            CaseStyle(0.71f, 0xFFE9E9EC, 0xFFF4F4F6, "Wii", 0xFF2A9BD8, heightMm = 190), listOf("wii")),
        Console("wiiu", "Wii U", "Nintendo", 2012, listOf("wiiu"),
            setOf("wua", "wux", "wud", "rpx"), null,
            CaseStyle(0.71f, 0xFF1B6FB8, 0xFF1B6FB8, "Wii U", heightMm = 190), listOf("cemu", "wii u")),
        Console("switch", "Nintendo Switch", "Nintendo", 2017, listOf("switch", "nsw"),
            setOf("nsp", "xci", "nsz", "xcz"), null,
            CaseStyle(0.62f, 0xFFD3232B, 0xFFE60012, "SWITCH", depth = 0.07f, heightMm = 170), listOf("yuzu", "eden", "citron", "sudachi", "suyu", "ryujinx", "switch")),
        Console("gb", "Game Boy", "Nintendo", 1989, listOf("gb", "gameboy"),
            setOf("gb", "dmg") + CART, "Nintendo - Game Boy",
            CaseStyle(0.93f, CARDBOARD, depth = 0.11f, heightMm = 127), listOf("gambatte", "sameboy", "game boy")),
        Console("gbc", "Game Boy Color", "Nintendo", 1998, listOf("gbc", "gameboycolor"),
            setOf("gbc", "cgb") + CART, "Nintendo - Game Boy Color",
            CaseStyle(0.93f, CARDBOARD, depth = 0.11f, heightMm = 127), listOf("game boy color")),
        Console("gba", "Game Boy Advance", "Nintendo", 2001, listOf("gba", "gameboyadvance"),
            setOf("gba", "agb") + CART, "Nintendo - Game Boy Advance",
            CaseStyle(0.95f, CARDBOARD, depth = 0.11f, heightMm = 127), listOf("mgba", "vba", "gpsp", "game boy advance")),
        Console("nds", "Nintendo DS", "Nintendo", 2004, listOf("nds", "ds"),
            setOf("nds", "dsi") + CART, "Nintendo - Nintendo DS",
            CaseStyle(0.89f, 0xFFF1F1F3, 0xFFE0E0E4, "DS", 0xFF555560, 0.08f, heightMm = 137), listOf("melonds", "desmume", "drastic", "nintendo ds")),
        Console("3ds", "Nintendo 3DS", "Nintendo", 2011, listOf("3ds", "n3ds"),
            setOf("3ds", "cia", "cci", "cxi", "app", "3dsx"), "Nintendo - Nintendo 3DS",
            CaseStyle(0.89f, 0xFFF1F1F3, 0xFFCE1A2C, "3DS", depth = 0.08f, heightMm = 137), listOf("citra", "azahar", "lime3ds", "panda3ds", "3ds")),

        Console("psx", "PlayStation", "Sony", 1994, listOf("psx", "ps1", "playstation", "psone"),
            setOf("cue", "chd", "pbp", "m3u", "ccd", "mds", "ecm"), "Sony - PlayStation",
            CaseStyle(1.0f, CLEAR, depth = 0.08f, heightMm = 125), listOf("duckstation", "swanstation", "pcsx", "beetle psx", "mednafen psx", "playstation")),
        Console("ps2", "PlayStation 2", "Sony", 2000, listOf("ps2", "playstation2"),
            setOf("iso", "chd", "cso", "zso", "gz", "elf"), "Sony - PlayStation 2",
            CaseStyle(0.71f, DVD_BLACK, 0xFF0A0A0C, "PlayStation 2", 0xFFCFD3DA, heightMm = 190), listOf("aethersx2", "nethersx2", "pcsx2", "playstation 2")),
        Console("psp", "PSP", "Sony", 2004, listOf("psp", "playstationportable"),
            setOf("iso", "cso", "pbp", "chd"), "Sony - PlayStation Portable",
            CaseStyle(0.58f, 0xFF1A1C22, 0xFF0C0C0E, "PSP", depth = 0.07f, heightMm = 172), listOf("ppsspp", "psp")),
        Console("psvita", "PS Vita", "Sony", 2011, listOf("psvita", "vita"),
            setOf("vpk", "zip"), null,
            CaseStyle(0.80f, 0xFF15171C, 0xFF1E5AA8, "PS Vita", depth = 0.07f, heightMm = 135), listOf("vita3k", "vita")),
        Console("ps3", "PlayStation 3", "Sony", 2006, listOf("ps3"),
            setOf("iso", "pkg"), "Sony - PlayStation 3",
            CaseStyle(0.80f, DVD_BLACK, 0xFF0A0A0C, "PS3", 0xFFCFD3DA, heightMm = 170), listOf("rpcs3", "playstation 3")),

        Console("mastersystem", "Master System", "Sega", 1985, listOf("mastersystem", "sms", "mark3"),
            setOf("sms") + CART, "Sega - Master System - Mark III",
            CaseStyle(0.72f, BLACK, depth = 0.10f, heightMm = 190), listOf("master system")),
        Console("megadrive", "Mega Drive / Genesis", "Sega", 1988, listOf("megadrive", "genesis", "md"),
            setOf("md", "gen", "smd", "bin", "68k", "sgd") + CART, "Sega - Mega Drive - Genesis",
            CaseStyle(0.72f, BLACK, depth = 0.10f, heightMm = 190), listOf("genesis plus", "picodrive", "blastem", "mega drive", "genesis")),
        Console("sega32x", "32X", "Sega", 1994, listOf("sega32x", "32x"),
            setOf("32x") + CART, "Sega - 32X",
            CaseStyle(0.72f, BLACK, depth = 0.10f, heightMm = 190), listOf("32x")),
        Console("segacd", "Mega-CD / Sega CD", "Sega", 1991, listOf("segacd", "megacd"),
            setOf("cue", "chd", "iso", "m3u"), "Sega - Mega-CD - Sega CD",
            CaseStyle(1.0f, CLEAR, depth = 0.08f, heightMm = 125), listOf("sega cd", "mega-cd")),
        Console("gamegear", "Game Gear", "Sega", 1990, listOf("gamegear", "gg"),
            setOf("gg") + CART, "Sega - Game Gear",
            CaseStyle(0.93f, CARDBOARD, depth = 0.10f, heightMm = 127), listOf("game gear")),
        Console("saturn", "Saturn", "Sega", 1994, listOf("saturn"),
            setOf("cue", "chd", "ccd", "mds", "m3u"), "Sega - Saturn",
            CaseStyle(0.9f, CLEAR, depth = 0.09f, heightMm = 160), listOf("yabause", "kronos", "beetle saturn", "saturn")),
        Console("dreamcast", "Dreamcast", "Sega", 1998, listOf("dreamcast", "dc"),
            setOf("cdi", "gdi", "chd", "m3u"), "Sega - Dreamcast",
            CaseStyle(1.0f, CLEAR, 0xFFF4F4F2, "DREAMCAST", 0xFFF07A1A, 0.08f, heightMm = 125), listOf("flycast", "redream", "reicast", "dreamcast")),

        Console("pcengine", "PC Engine / TG16", "NEC", 1987, listOf("pcengine", "tg16", "turbografx16", "pce"),
            setOf("pce", "sgx") + CART, "NEC - PC Engine - TurboGrafx 16",
            CaseStyle(0.80f, BLACK, depth = 0.09f, heightMm = 150), listOf("pc engine", "turbografx")),
        Console("neogeo", "Neo Geo", "SNK", 1990, listOf("neogeo", "neogeoaes"),
            setOf("zip", "7z"), "SNK - Neo Geo",
            CaseStyle(0.70f, BLACK, 0xFF0A0A0A, "NEO·GEO", 0xFFE8C04A, heightMm = 235), listOf("neo geo")),
        Console("arcade", "Arcade", "Arcade", 1978, listOf("arcade", "mame", "fbneo", "fba", "cps1", "cps2", "cps3"),
            setOf("zip", "7z"), "MAME",
            CaseStyle(0.75f, 0xFF20123A, 0xFFE8304A, "ARCADE", heightMm = 190), listOf("mame", "fbneo", "final burn", "arcade")),
        Console("atari2600", "Atari 2600", "Atari", 1977, listOf("atari2600", "a2600"),
            setOf("a26", "bin") + CART, "Atari - 2600",
            CaseStyle(0.72f, CARDBOARD, depth = 0.11f, heightMm = 180), listOf("stella", "atari 2600")),
        Console("xbox", "Xbox", "Microsoft", 2001, listOf("xbox"),
            setOf("iso", "xiso"), "Microsoft - Xbox",
            CaseStyle(0.71f, 0xFF1F7A1F, 0xFF178A17, "XBOX", heightMm = 190), listOf("xemu", "xbox")),
        Console("dos", "MS-DOS", "PC", 1981, listOf("dos", "pcdos", "msdos"),
            setOf("exe", "com", "bat", "dosz", "conf") + CART, "DOS",
            CaseStyle(0.80f, CARDBOARD, depth = 0.14f, heightMm = 230), listOf("dosbox")),
        Console("other", "Otros", "—", 2000, emptyList(), emptySet(), null, CaseStyle(0.71f, BLACK, heightMm = 190)),
    )

    private val byIdMap = all.associateBy { it.id }
    private val byFolder: Map<String, Console> =
        all.flatMap { c -> c.folders.map { it to c } }.toMap()

    fun byId(id: String): Console = byIdMap[id] ?: byIdMap.getValue("other")

    fun byFolder(name: String): Console? = byFolder[name.lowercase().replace(" ", "")]

    /** Reconoce un sistema por el nombre de la playlist de RetroArch (p.ej. "Sony - PlayStation.lpl"). */
    fun byLibretroName(name: String): Console? {
        val n = name.removeSuffix(".lpl")
        return all.firstOrNull { it.libretro.equals(n, ignoreCase = true) }
    }

    /** Intenta adivinar el sistema por el nombre de un core o texto libre. */
    fun guess(text: String): Console? {
        val t = text.lowercase()
        // Las palabras más largas primero, para que "game boy advance" gane a "game boy".
        return all.flatMap { c -> c.keywords.map { it to c } }
            .sortedByDescending { it.first.length }
            .firstOrNull { t.contains(it.first) }?.second
    }
}

/** Nombre para mostrar (traduce "Otros"; el resto son nombres propios). */
val Console.label: String
    get() = if (id == "other") com.repisa.app.i18n.L10n.s(com.repisa.app.R.string.console_other) else name
