package com.repisa.app.model

/**
 * Nombres de estudio unificados para las repisas: cada tienda y frontend escribe el mismo estudio a su manera
 * («ATLUS», «Atlus», «Atlus Co., Ltd.», «SEGA», «Sega Corporation»...). Se comparan sin mayúsculas, acentos,
 * signos ni la forma jurídica, y un juego hecho por dos estudios («Intelligent Systems, Koei Tecmo») va en ambos.
 */
object Studios {
    /** Formas jurídicas al final del nombre (o antes de una coma que separa estudios). */
    private val legal = Regex(
        """(?i)[,\s]+(co\.?,?\s*ltd\.?|company,?\s*limited|inc\.?|ltd\.?|llc|l\.l\.c\.?|corporation|corp\.?|gmbh|s\.?a\.?|s\.?l\.?|""" +
            """k\.?k\.?|limited|pty\.?\s*ltd\.?|s\.?r\.?l\.?|b\.?v\.?|a/s|oy|ab|plc|games\s+inc\.?)\.?(?=\s*(?:$|[,/;]))""",
    )
    private val separators = Regex("""\s*[,/;|]\s*""")

    /** Los estudios de un campo «desarrollador», limpios y sin repetir. */
    fun split(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        var s = raw.trim()
        repeat(3) { s = s.replace(legal, "") } // «Square Enix Co., Ltd.», «Foo, Inc., Bar Ltd.»
        return s.split(separators).map { it.trim().trimEnd('.', ',') }.filter { it.length > 1 }.distinctBy { key(it) }
    }

    /** Clave para comparar: «ATLUS», «Atlus» y «atlus» dan lo mismo. */
    fun key(name: String): String = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
        .replace(Regex("""\p{Mn}+"""), "").lowercase().replace(Regex("""[^a-z0-9]"""), "")

    /**
     * De las formas en que aparece un mismo estudio, la que se muestra: la más usada; a igualdad, la que no está toda
     * en mayúsculas («Atlus» mejor que «ATLUS»).
     */
    fun display(variants: List<String>): String = variants.groupingBy { it }.eachCount().entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key == it.key.uppercase() })
        .first().key
}
