package com.repisa.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repisa.app.data.LibraryStore
import com.repisa.app.data.Settings
import com.repisa.app.data.mergeGame
import com.repisa.app.launch.Launcher
import com.repisa.app.model.Consoles
import com.repisa.app.model.Game
import com.repisa.app.model.Source
import com.repisa.app.model.formatPlaytime
import com.repisa.app.sources.EmulatorSource
import com.repisa.app.sources.EpicSource
import com.repisa.app.sources.Metadata
import com.repisa.app.sources.SteamLogin
import com.repisa.app.sources.SteamSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SortMode(val label: String) {
    ALPHA("A – Z"), PLAYTIME("Tiempo de juego"), RECENT("Jugado recientemente"), YEAR("Año"), CONSOLE("Consola")
}

enum class GroupMode(val label: String) {
    NONE("Todo junto"), GENRE("Género"), CONSOLE("Consola"), DEVELOPER("Estudio"), SOURCE("Plataforma"),
    DECADE("Década"), FAVORITES("Favoritos")
}

data class ShelfGroup(val title: String, val subtitle: String, val games: List<Game>)

data class ViewOptions(
    val sort: SortMode = SortMode.ALPHA,
    val group: GroupMode = GroupMode.NONE,
    val source: Source? = null,
    val query: String = "",
)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    val settings = Settings(app)
    private val store = LibraryStore(app)
    val launcher = Launcher(app, settings)
    val epic = EpicSource(settings)
    private val metadata = Metadata(settings)
    private val mutex = Mutex()

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    private val _options = MutableStateFlow(
        ViewOptions(
            sort = runCatching { SortMode.valueOf(settings.sortMode) }.getOrDefault(SortMode.ALPHA),
            group = runCatching { GroupMode.valueOf(settings.groupMode) }.getOrDefault(GroupMode.NONE),
        )
    )
    val options: StateFlow<ViewOptions> = _options.asStateFlow()

    /** Texto de progreso de la sincronización (null = sin tareas). */
    val status = MutableStateFlow<String?>(null)
    /** Mensajes puntuales para mostrar en un snackbar. */
    val message = MutableStateFlow<String?>(null)
    /** Juego resaltado (con el mando o el dedo): se muestra en la segunda pantalla. */
    val focused = MutableStateFlow<Game?>(null)
    val selectedId = MutableStateFlow<String?>(null)
    val inSettings = MutableStateFlow(false)

    val shelves: StateFlow<List<ShelfGroup>> = combine(_games, _options) { g, o -> buildShelves(g, o) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var enrichJob: Job? = null
    private var loaded = false
    private val libraryLoaded = kotlinx.coroutines.CompletableDeferred<Unit>()
    private var syncJob: Job? = null

    init {
        viewModelScope.launch {
            _games.value = store.load()
            loaded = true
            libraryLoaded.complete(Unit)
            if (!autoSync()) startEnrichment()
        }
    }

    fun hasAnySource() = settings.steamConfigured || epic.isLoggedIn ||
        settings.hasEmulatorSources

    // ------------------------------------------------------------------ Vista

    fun setSort(s: SortMode) { settings.sortMode = s.name; _options.update { it.copy(sort = s) } }
    fun setGroup(g: GroupMode) { settings.groupMode = g.name; _options.update { it.copy(group = g) } }
    fun setSource(s: Source?) = _options.update { it.copy(source = s) }
    fun setQuery(q: String) = _options.update { it.copy(query = q) }

    private fun buildShelves(all: List<Game>, o: ViewOptions): List<ShelfGroup> {
        val q = o.query.trim().lowercase()
        val filtered = all.filter { g ->
            (o.source == null || g.source == o.source) &&
                (q.isEmpty() || g.title.lowercase().contains(q) || g.developer?.lowercase()?.contains(q) == true ||
                    g.genres.any { it.lowercase().contains(q) } || g.console.name.lowercase().contains(q))
        }
        val sorted = when (o.sort) {
            SortMode.ALPHA -> filtered.sortedBy { it.sortKey }
            SortMode.PLAYTIME -> filtered.sortedWith(compareByDescending<Game> { it.totalMinutes }.thenBy { it.sortKey })
            SortMode.RECENT -> filtered.sortedWith(compareByDescending<Game> { it.lastPlayed ?: 0 }.thenBy { it.sortKey })
            SortMode.YEAR -> filtered.sortedWith(compareBy<Game> { it.releaseYear ?: 9999 }.thenBy { it.sortKey })
            SortMode.CONSOLE -> filtered.sortedWith(compareBy<Game> { it.console.year }.thenBy { it.console.name }.thenBy { it.sortKey })
        }
        fun group(title: String, games: List<Game>) =
            ShelfGroup(title, "${games.size} juego${if (games.size == 1) "" else "s"} · ${hours(games)}", games)

        return when (o.group) {
            GroupMode.NONE -> if (sorted.isEmpty()) emptyList() else listOf(group("Mi colección", sorted))
            GroupMode.GENRE -> {
                val map = linkedMapOf<String, MutableList<Game>>()
                sorted.forEach { g -> g.genres.ifEmpty { listOf("Sin género") }.forEach { map.getOrPut(it) { mutableListOf() } += g } }
                map.entries.sortedWith(compareBy<Map.Entry<String, MutableList<Game>>> { it.key == "Sin género" }.thenByDescending { it.value.size })
                    .map { group(it.key, it.value) }
            }
            GroupMode.CONSOLE -> sorted.groupBy { it.consoleId }.entries
                .sortedWith(compareBy({ Consoles.byId(it.key).year }, { Consoles.byId(it.key).name }))
                .map { group(Consoles.byId(it.key).let { c -> if (c.id == "pc") "PC" else "${c.maker} · ${c.name}" }, it.value) }
            GroupMode.DEVELOPER -> sorted.groupBy { it.developer?.trim()?.ifBlank { null } ?: "Estudio desconocido" }.entries
                .sortedWith(compareBy<Map.Entry<String, List<Game>>> { it.key == "Estudio desconocido" }.thenByDescending { it.value.size }.thenBy { it.key })
                .map { group(it.key, it.value) }
            GroupMode.SOURCE -> sorted.groupBy { it.source }.entries.sortedBy { it.key.ordinal }
                .map { group(it.key.label, it.value) }
            GroupMode.DECADE -> sorted.groupBy { it.releaseYear?.let { y -> "Años ${y / 10 * 10}" } ?: "Año desconocido" }.entries
                .sortedBy { it.key }
                .map { group(it.key, it.value) }
            GroupMode.FAVORITES -> sorted.partition { it.favorite }.let { (fav, rest) ->
                listOfNotNull(fav.takeIf { it.isNotEmpty() }?.let { group("★ Favoritos", it) },
                    rest.takeIf { it.isNotEmpty() }?.let { group("El resto", it) })
            }
        }
    }

    private fun hours(games: List<Game>): String {
        val m = games.sumOf { it.totalMinutes }
        return if (m < 60) "$m min" else "${m / 60} h"
    }

    // ------------------------------------------------------------------ Sincronización

    fun syncAll(quiet: Boolean = false) {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            enrichJob?.cancel()
            settings.lastAutoSync = System.currentTimeMillis()
            val before = _games.value.associate { it.id to it.totalMinutes }
            val errors = mutableListOf<String>()
            if (settings.steamConfigured) syncSource(Source.STEAM, quiet)?.let(errors::add)
            if (epic.isLoggedIn) syncSource(Source.EPIC, quiet)?.let(errors::add)
            if (settings.hasEmulatorSources) syncSource(Source.EMULATOR, quiet)?.let(errors::add)
            if (errors.isNotEmpty()) message.value = errors.joinToString("\n")
            else if (!hasAnySource()) message.value = "Conecta Steam, Epic o tus carpetas de emuladores en Ajustes."
            else if (quiet) {
                // Resumen de lo que cambió desde la última vez (p. ej. horas jugadas en Steam desde el PC).
                val changed = _games.value.filter { g -> before[g.id]?.let { g.totalMinutes > it } == true }
                if (changed.isNotEmpty()) {
                    val mins = changed.sumOf { it.totalMinutes - (before[it.id] ?: 0) }
                    message.value = "Tiempo actualizado: " + changed.take(3).joinToString(", ") { it.title } +
                        (if (changed.size > 3) " y ${changed.size - 3} más" else "") + " (+${formatPlaytime(mins)})"
                }
            }
            status.value = null
            startEnrichment()
        }
    }

    /**
     * Sincroniza solo si hace un rato que no se hace (al abrir la app o volver a ella),
     * para que las horas de Steam, Epic y emuladores estén siempre al día.
     * Devuelve true si lanzó una sincronización.
     */
    fun autoSync(): Boolean {
        if (!loaded || !hasAnySource()) return false
        if (System.currentTimeMillis() - settings.lastAutoSync < 10 * 60_000L && _games.value.isNotEmpty()) return false
        syncAll(quiet = true)
        return true
    }

    fun syncOne(source: Source) = viewModelScope.launch {
        enrichJob?.cancel()
        syncSource(source)?.let { message.value = it }
        status.value = null
        startEnrichment()
    }

    /** Devuelve un mensaje de error o null. */
    private suspend fun syncSource(source: Source, quiet: Boolean = false): String? {
        status.value = "Sincronizando ${source.label}…"
        return try {
            val known = _games.value.associateBy { it.id }
            val fresh: List<Game> = when (source) {
                Source.STEAM -> steamSource().fetchGames()
                Source.EPIC -> epic.fetchGames(known)
                Source.EMULATOR -> EmulatorSource(getApplication(), settings).fetchGames { status.value = it }
            }
            mutex.withLock {
                val current = _games.value.associateBy { it.id }
                val merged = fresh.map { mergeGame(current[it.id], it) }
                val freshIds = merged.map { it.id }.toSet()
                // Lo que la fuente ya no devuelve se elimina (juego desinstalado, ROM borrada...).
                _games.value = _games.value.filter { it.source != source && it.id !in freshIds } + merged
                store.save(_games.value)
            }
            if (!quiet) message.value = "${source.label}: ${fresh.size} juegos"
            null
        } catch (e: Exception) {
            "${source.label}: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    private suspend fun steamSource(): SteamSource {
        val key = settings.steamApiKey
        val user = settings.steamUser
        if (key != null && user != null) return SteamSource.withApiKey(key, user)
        val id = settings.steamLinkedId ?: error("No hay ninguna cuenta de Steam vinculada")
        val token = SteamLogin.fetchWebToken()
            ?: error("la sesión de Steam ha caducado. Vuelve a vincular tu cuenta en Ajustes.")
        return SteamSource.withToken(token, id)
    }

    /** Se llama cuando el usuario termina de iniciar sesión en la web de Steam. */
    fun steamLinked() = viewModelScope.launch {
        status.value = "Vinculando cuenta de Steam…"
        try {
            val token = SteamLogin.fetchWebToken() ?: error("Steam no confirmó la sesión")
            val id = SteamLogin.steamIdFromToken(token) ?: error("no se pudo leer el SteamID")
            settings.steamLinkedId = id
            settings.steamLinkedName = SteamLogin.profileName(id)
            message.value = "Cuenta de Steam vinculada" + (settings.steamLinkedName?.let { ": $it" } ?: "")
            syncSource(Source.STEAM)?.let { message.value = it }
            startEnrichment()
        } catch (e: Exception) {
            message.value = "Steam: no se pudo vincular la cuenta (${e.message})."
        } finally {
            status.value = null
        }
    }

    fun steamUnlink() = viewModelScope.launch {
        settings.steamLinkedId = null
        settings.steamLinkedName = null
        settings.steamApiKey = null
        settings.steamUser = null
        SteamLogin.clearSession()
        mutex.withLock {
            _games.update { list -> list.filter { it.source != Source.STEAM } }
            store.save(_games.value)
        }
    }

    /** Busca portadas y datos que faltan, de uno en uno y en segundo plano. */
    fun startEnrichment(force: Boolean = false) {
        if (enrichJob?.isActive == true) return
        enrichJob = viewModelScope.launch {
            if (force) mutex.withLock { _games.update { list -> list.map { if (it.userEdited) it else it.copy(metaFetched = false) } } }
            val pending = _games.value.filter { metadata.needsWork(it) }
            pending.forEachIndexed { i, g ->
                status.value = "Buscando portadas y datos (${i + 1}/${pending.size})…"
                val enriched = runCatching { metadata.enrich(g) }.getOrElse { g.copy(metaFetched = true) }
                replace(enriched, save = i % 10 == 9 || i == pending.lastIndex)
            }
            status.value = null
        }
    }

    private suspend fun replace(game: Game, save: Boolean = true) = mutex.withLock {
        _games.update { list -> list.map { if (it.id == game.id) mergeEnriched(it, game) else it } }
        if (focused.value?.id == game.id) focused.value = _games.value.firstOrNull { it.id == game.id }
        if (save) store.save(_games.value)
    }

    /** Aplica el resultado de metadatos sin pisar cambios del usuario hechos mientras tanto. */
    private fun mergeEnriched(current: Game, enriched: Game) =
        if (current.userEdited) current.copy(metaFetched = true)
        else enriched.copy(
            favorite = current.favorite,
            trackedMinutes = current.trackedMinutes,
            coverAspect = if (enriched.coverUrl == current.coverUrl) current.coverAspect else null,
        )

    // ------------------------------------------------------------------ Acciones

    fun updateGame(game: Game) = viewModelScope.launch {
        mutex.withLock {
            _games.update { list -> list.map { if (it.id == game.id) game else it } }
            store.save(_games.value)
        }
        if (focused.value?.id == game.id) focused.value = game
    }

    private var aspectSaveJob: Job? = null

    /** La carátula ya cargó: guarda su proporción para que la caja tenga su forma exacta. */
    fun setCoverAspect(id: String, aspect: Float) {
        _games.update { list -> list.map { if (it.id == id) it.copy(coverAspect = aspect) else it } }
        aspectSaveJob?.cancel()
        aspectSaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            mutex.withLock { store.save(_games.value) }
        }
    }

    fun toggleFavorite(game: Game) = updateGame(game.copy(favorite = !game.favorite))

    fun play(game: Game) {
        launcher.launch(game)?.let { message.value = it }
    }

    fun onResume() = viewModelScope.launch {
        // Si Android cerró Repisa mientras jugabas, hay que esperar a cargar la biblioteca
        // antes de anotar la sesión; si no, se perdería.
        libraryLoaded.await()
        launcher.consumeSession()?.let { s ->
            val g = _games.value.firstOrNull { it.id == s.gameId } ?: return@let
            updateGame(g.copy(trackedMinutes = g.trackedMinutes + s.minutes, lastPlayed = System.currentTimeMillis()))
            message.value = "${g.title}: +${formatPlaytime(s.minutes)} de juego" + if (s.measured) "" else " (aprox.)"
        }
        autoSync()
    }

    fun epicLogin(code: String) = viewModelScope.launch {
        status.value = "Conectando con Epic Games…"
        try {
            epic.login(code)
            message.value = "Sesión de Epic iniciada"
            syncSource(Source.EPIC)?.let { message.value = it }
            startEnrichment()
        } catch (e: Exception) {
            message.value = "Epic: no se pudo iniciar sesión (${e.message}). El código caduca en unos minutos; vuelve a intentarlo."
        } finally {
            status.value = null
        }
    }

    fun epicLogout() = viewModelScope.launch {
        settings.clearEpic()
        mutex.withLock {
            _games.update { list -> list.filter { it.source != Source.EPIC } }
            store.save(_games.value)
        }
    }

    fun removeSource(source: Source) = viewModelScope.launch {
        mutex.withLock {
            _games.update { list -> list.filter { it.source != source } }
            store.save(_games.value)
        }
    }

    fun consolesInLibrary(): List<String> = _games.value.filter { it.source == Source.EMULATOR }
        .map { it.consoleId }.distinct().sortedBy { Consoles.byId(it).year }
}
