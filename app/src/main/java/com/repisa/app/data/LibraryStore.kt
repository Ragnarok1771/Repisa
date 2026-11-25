package com.repisa.app.data

import android.content.Context
import com.repisa.app.model.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/** Biblioteca guardada como JSON en el almacenamiento interno. */
class LibraryStore(context: Context) {
    private val file = File(context.filesDir, "library.json")
    private val serializer = ListSerializer(Game.serializer())

    suspend fun load(): List<Game> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        runCatching { Http.json.decodeFromString(serializer, file.readText()) }.getOrDefault(emptyList())
            // Años imposibles guardados por versiones anteriores (la «fecha cero» de Cocoon: 1969/1970) → desconocido.
            .map { g -> if (g.releaseYear != null && com.repisa.app.sources.Years.valid(g.releaseYear) == null) g.copy(releaseYear = null, factsFetched = false) else g }
    }

    suspend fun save(games: List<Game>) = withContext(Dispatchers.IO) {
        val tmp = File(file.parentFile, "library.json.tmp")
        tmp.writeText(Http.json.encodeToString(serializer, games))
        tmp.renameTo(file)
    }

    /**
     * Juegos que salieron de la biblioteca (cuenta desvinculada, fuente quitada, juego que la tienda ya no devuelve)
     * con todo lo tuyo: estado, nota, notas, colecciones, favorito, tiempo a mano, carátula propia...
     * Si el juego vuelve (al vincular otra vez), se le devuelve todo.
     */
    private val archiveFile = File(context.filesDir, "archive.json")

    suspend fun loadArchive(): Map<String, Game> = withContext(Dispatchers.IO) {
        if (!archiveFile.exists()) return@withContext emptyMap()
        runCatching { Http.json.decodeFromString(serializer, archiveFile.readText()) }.getOrDefault(emptyList()).associateBy { it.id }
    }

    suspend fun saveArchive(games: Map<String, Game>) = withContext(Dispatchers.IO) {
        val tmp = File(archiveFile.parentFile, "archive.json.tmp")
        tmp.writeText(Http.json.encodeToString(serializer, games.values.toList()))
        tmp.renameTo(archiveFile)
    }
}

/** ¿Tiene algo puesto por el usuario que valga la pena guardar si el juego sale de la biblioteca? */
val Game.hasPersonalData: Boolean
    get() = status != null || rating != null || !notes.isNullOrBlank() || collections.isNotEmpty() || favorite ||
        manualMinutes != 0 || trackedMinutes > 0 || customCover || userEdited || preferredCopy || noMerge ||
        achUnlocked != null

/**
 * Combina lo que acaba de devolver una fuente con lo que ya teníamos guardado.
 * La fuente manda en título, tiempo y rutas; lo que el usuario o los metadatos añadieron se conserva.
 */
fun mergeGame(old: Game?, new: Game): Game {
    if (old == null) return new
    val merged = new.copy(
        title = if (old.userEdited) old.title else new.title,
        genres = if (old.userEdited || new.genres.isEmpty()) old.genres else new.genres,
        developer = if (old.userEdited) old.developer else new.developer ?: old.developer,
        publisher = new.publisher ?: old.publisher,
        releaseYear = if (old.userEdited) old.releaseYear else com.repisa.app.sources.Years.valid(new.releaseYear) ?: com.repisa.app.sources.Years.valid(old.releaseYear),
        description = new.description ?: old.description,
        playtimeMinutes = maxOf(new.playtimeMinutes, if (new.source == old.source) 0 else old.playtimeMinutes),
        trackedMinutes = old.trackedMinutes,
        manualMinutes = old.manualMinutes,
        notes = old.notes,
        status = old.status,
        collections = old.collections,
        achUnlocked = old.achUnlocked, achTotal = old.achTotal,
        lastPlayed = listOfNotNull(old.lastPlayed, new.lastPlayed).maxOrNull(),
        coverUrl = if ((old.userEdited || old.customCover) && old.coverUrl != null) old.coverUrl else new.coverUrl ?: old.coverUrl,
        customCover = old.customCover,
        rating = old.rating,
        preferredCopy = old.preferredCopy, noMerge = old.noMerge,
        metascore = old.metascore, userScore = old.userScore, metacriticUrl = old.metacriticUrl,
        hltbMain = old.hltbMain, hltbExtra = old.hltbExtra, hltbComplete = old.hltbComplete, hltbUrl = old.hltbUrl,
        extFetchedAt = old.extFetchedAt,
        altCoverUrl = new.altCoverUrl ?: old.altCoverUrl,
        romUri = new.romUri ?: old.romUri,
        romPath = new.romPath ?: old.romPath,
        corePath = new.corePath ?: old.corePath,
        romName = new.romName ?: old.romName,
        storeUrl = new.storeUrl ?: old.storeUrl,
        favorite = old.favorite,
        metaFetched = old.metaFetched,
        factsFetched = old.factsFetched,
        userEdited = old.userEdited,
        wikidataId = old.wikidataId, altTitles = old.altTitles, idChecked = old.idChecked,
        steamMatch = old.steamMatch, steamChecked = old.steamChecked,
    )
    return merged.copy(coverAspect = if (merged.coverUrl == old.coverUrl) old.coverAspect else null)
}
