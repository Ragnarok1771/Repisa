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
    }

    suspend fun save(games: List<Game>) = withContext(Dispatchers.IO) {
        val tmp = File(file.parentFile, "library.json.tmp")
        tmp.writeText(Http.json.encodeToString(serializer, games))
        tmp.renameTo(file)
    }
}

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
        releaseYear = if (old.userEdited) old.releaseYear else new.releaseYear ?: old.releaseYear,
        description = new.description ?: old.description,
        playtimeMinutes = maxOf(new.playtimeMinutes, if (new.source == old.source) 0 else old.playtimeMinutes),
        trackedMinutes = old.trackedMinutes,
        lastPlayed = listOfNotNull(old.lastPlayed, new.lastPlayed).maxOrNull(),
        coverUrl = if ((old.userEdited || old.customCover) && old.coverUrl != null) old.coverUrl else new.coverUrl ?: old.coverUrl,
        customCover = old.customCover,
        altCoverUrl = new.altCoverUrl ?: old.altCoverUrl,
        romUri = new.romUri ?: old.romUri,
        romPath = new.romPath ?: old.romPath,
        corePath = new.corePath ?: old.corePath,
        romName = new.romName ?: old.romName,
        storeUrl = new.storeUrl ?: old.storeUrl,
        favorite = old.favorite,
        metaFetched = old.metaFetched,
        userEdited = old.userEdited,
    )
    return merged.copy(coverAspect = if (merged.coverUrl == old.coverUrl) old.coverAspect else null)
}
