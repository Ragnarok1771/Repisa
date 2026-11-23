package com.repisa.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class Source(val label: String) {
    STEAM("Steam"),
    EPIC("Epic Games"),
    EMULATOR("Emuladores"),
}

@Serializable
data class Game(
    /** Identificador estable: "steam:440", "epic:<appName>", "emu:snes:<nombre de rom>". */
    val id: String,
    val source: Source,
    val title: String,
    /** Id de [Consoles]: "pc", "snes", "ps2"... */
    val consoleId: String,
    val genres: List<String> = emptyList(),
    val developer: String? = null,
    val publisher: String? = null,
    val releaseYear: Int? = null,
    val description: String? = null,
    /** Minutos de juego reportados por la fuente (Steam, Epic, RetroArch). */
    val playtimeMinutes: Int = 0,
    /** Minutos medidos por Repisa al lanzar el juego desde la app. */
    val trackedMinutes: Int = 0,
    /** Fecha (epoch ms) de la última partida. */
    val lastPlayed: Long? = null,
    val coverUrl: String? = null,
    val altCoverUrl: String? = null,
    /** Proporción ancho/alto de la carátula descargada (para dar a la caja su forma exacta). */
    val coverAspect: Float? = null,
    /** content:// del ROM (carpeta elegida con el selector de Android). */
    val romUri: String? = null,
    /** Ruta absoluta del ROM (playlists de RetroArch). */
    val romPath: String? = null,
    /** Core de RetroArch con el que se añadió a la playlist. */
    val corePath: String? = null,
    /** Nombre del archivo sin extensión, usado para cruzar datos (ES-DE, RetroArch, thumbnails). */
    val romName: String? = null,
    val storeUrl: String? = null,
    val favorite: Boolean = false,
    /** Juego de Steam prestado por el grupo familiar (no es de la cuenta del usuario). */
    val familyShared: Boolean = false,
    /** Carátula elegida por el usuario con el buscador: no se sustituye al sincronizar. */
    val customCover: Boolean = false,
    /** Calificación personal del usuario. */
    val rating: Rating? = null,
    /** Metacritic: nota de la prensa (0-100) y de los usuarios (0-10). */
    val metascore: Int? = null,
    val userScore: Double? = null,
    val metacriticUrl: String? = null,
    /** HowLongToBeat, en horas: historia, historia + extras, completista. */
    val hltbMain: Float? = null,
    val hltbExtra: Float? = null,
    val hltbComplete: Float? = null,
    val hltbUrl: String? = null,
    /** Cuándo se consultaron Metacritic y HowLongToBeat por última vez. */
    val extFetchedAt: Long? = null,
    /** Ya se buscaron metadatos online para este juego. */
    val metaFetched: Boolean = false,
    /** El usuario editó los datos a mano: no sobrescribir con metadatos automáticos. */
    val userEdited: Boolean = false,
) {
    val totalMinutes: Int get() = playtimeMinutes + trackedMinutes
    val console: Console get() = Consoles.byId(consoleId)
    val sortKey: String
        get() = title.lowercase()
            .removePrefix("the ").removePrefix("el ").removePrefix("la ")
            .trimStart()
}

fun formatPlaytime(minutes: Int): String = when {
    minutes <= 0 -> "Sin jugar"
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
