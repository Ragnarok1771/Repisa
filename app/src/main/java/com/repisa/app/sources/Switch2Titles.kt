package com.repisa.app.sources

/**
 * Juegos de Switch 2 que reconoce Banana Native, por su número (title ID), y lectura de ese número desde main.npdm.
 * Lista sacada de la tabla de juegos de Banana Native.
 */
object Switch2Titles {
    private val names = mapOf(
        "040074A01BF12000" to "Donkey Kong Bananza",
        "04007EF01F8AE000" to "Fire Emblem: Fortune's Weave",
        "0400C3F00006E000" to "Mario Kart World",
        "0400A3A01EE4C000" to "Pokémon Pokopia",
        "0400E88025B1A000" to "Star Fox",
        "040041E0212AE000" to "Fast Fusion",
        "04003F40232DE000" to "Hyrule Warriors: Age of Imprisonment",
        "04004D300C5AE001" to "Kirby and the Forgotten Land - Nintendo Switch 2 Edition",
        "0400D4201C6EA000" to "Mario Tennis Fever",
        "0400380020056000" to "Splatoon Raiders",
        "040015100B514001" to "Super Mario Bros. Wonder - Nintendo Switch 2 Edition",
        "0400486024DEC000" to "The Legend of Zelda: Ocarina of Time",
    )

    fun name(titleId: String): String? = names[titleId.uppercase()]

    /** Un nombre de archivo o carpeta parecido a uno de la lista («Ocarina of Time.nup» → el de Zelda). */
    fun byName(fileName: String): String? {
        val wanted = CoverSearch.tokens(fileName)
        if (wanted.isEmpty()) return null
        return names.values.map { it to CoverSearch.similarity(wanted, CoverSearch.tokens(it)) }
            .filter { it.second >= 0.6 }.maxByOrNull { it.second }?.first
    }

    /**
     * Número del programa en main.npdm: cabecera «META»; en 0x70 está dónde empieza la sección «ACI0», y el número
     * va 0x10 bytes después de ella (8 bytes, de menor a mayor).
     */
    fun programId(npdm: ByteArray): String? {
        if (npdm.size < 0x80 || String(npdm, 0, 4, Charsets.US_ASCII) != "META") return null
        val aci = u32(npdm, 0x70)
        if (aci < 0 || aci + 0x18 > npdm.size || String(npdm, aci, 4, Charsets.US_ASCII) != "ACI0") return null
        var id = 0L
        for (i in 7 downTo 0) id = (id shl 8) or (npdm[aci + 0x10 + i].toLong() and 0xFF)
        return "%016X".format(id)
    }

    private fun u32(b: ByteArray, at: Int): Int =
        (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8) or ((b[at + 2].toInt() and 0xFF) shl 16) or ((b[at + 3].toInt() and 0xFF) shl 24)
}
