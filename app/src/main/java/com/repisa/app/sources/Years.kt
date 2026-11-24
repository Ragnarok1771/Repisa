package com.repisa.app.sources

/**
 * Años de salida creíbles. Las exportaciones de ES-DE/Cocoon ponen la «fecha cero» (01/01/1970, o 31/12/1969 según
 * la zona horaria) cuando no saben la fecha: eso no es un año, es «desconocido».
 */
object Years {
    private val max get() = java.time.Year.now().value + 2
    fun valid(year: Int?): Int? = year?.takeIf { it in 1971..max }
}
