package com.repisa.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repisa.app.R
import com.repisa.app.data.LibraryStore
import com.repisa.app.i18n.L10n
import com.repisa.app.data.Settings
import com.repisa.app.data.mergeGame
import com.repisa.app.launch.Launcher
import com.repisa.app.model.Consoles
import com.repisa.app.model.label
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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SortMode(private val labelRes: Int) {
    ALPHA(R.string.sort_alpha), PLAYTIME(R.string.sort_playtime), RECENT(R.string.sort_recent), YEAR(R.string.sort_year),
    CONSOLE(R.string.sort_console), RATING(R.string.sort_rating), METACRITIC(R.string.sort_metacritic);

    val label: String get() = L10n.s(labelRes)
}

enum class GroupMode(private val labelRes: Int) {
    NONE(R.string.group_none), GENRE(R.string.group_genre), CONSOLE(R.string.group_console),
    DEVELOPER(R.string.group_developer), SOURCE(R.string.group_source), DECADE(R.string.group_decade),
    FAVORITES(R.string.group_favorites);

    val label: String get() = L10n.s(labelRes)
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
    val coverSearch = com.repisa.app.sources.CoverSearch(app, settings)
    private val metadata = Metadata(settings, coverSearch)
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
            SortMode.RATING -> filtered.sortedWith(compareByDescending<Game> { it.rating?.overall ?: -1f }.thenBy { it.sortKey })
            SortMode.METACRITIC -> filtered.sortedWith(compareByDescending<Game> { it.metascore ?: -1 }.thenBy { it.sortKey })
        }
        fun group(title: String, games: List<Game>) =
            ShelfGroup(title, "${L10n.games(games.size)} · ${hours(games)}", games)

        return when (o.group) {
            GroupMode.NONE -> if (sorted.isEmpty()) emptyList() else listOf(group(MY_COLLECTION, sorted))
            GroupMode.GENRE -> {
                val map = linkedMapOf<String, MutableList<Game>>()
                val none = L10n.s(R.string.shelf_no_genre)
                sorted.forEach { g -> g.genres.map(L10n::genre).ifEmpty { listOf(none) }.forEach { map.getOrPut(it) { mutableListOf() } += g } }
                map.entries.sortedWith(compareBy<Map.Entry<String, MutableList<Game>>> { it.key == none }.thenByDescending { it.value.size })
                    .map { group(it.key, it.value) }
            }
            GroupMode.CONSOLE -> sorted.groupBy { it.consoleId }.entries
                .sortedWith(compareBy({ Consoles.byId(it.key).year }, { Consoles.byId(it.key).name }))
                .map { group(Consoles.byId(it.key).let { c -> if (c.id == "pc") "PC" else if (c.id == "other") c.label else "${c.maker} · ${c.name}" }, it.value) }
            GroupMode.DEVELOPER -> {
                val unknown = L10n.s(R.string.shelf_unknown_studio)
                sorted.groupBy { it.developer?.trim()?.ifBlank { null } ?: unknown }.entries
                    .sortedWith(compareBy<Map.Entry<String, List<Game>>> { it.key == unknown }.thenByDescending { it.value.size }.thenBy { it.key })
                    .map { group(it.key, it.value) }
            }
            GroupMode.SOURCE -> sorted.groupBy { it.source }.entries.sortedBy { it.key.ordinal }
                .map { group(it.key.label, it.value) }
            GroupMode.DECADE -> sorted.groupBy { it.releaseYear?.let { y -> y / 10 * 10 } ?: 9999 }.entries
                .sortedBy { it.key }
                .map { group(if (it.key == 9999) L10n.s(R.string.shelf_unknown_year) else L10n.s(R.string.shelf_decade, it.key), it.value) }
            GroupMode.FAVORITES -> sorted.partition { it.favorite }.let { (fav, rest) ->
                listOfNotNull(fav.takeIf { it.isNotEmpty() }?.let { group(L10n.s(R.string.shelf_favorites), it) },
                    rest.takeIf { it.isNotEmpty() }?.let { group(L10n.s(R.string.shelf_rest), it) })
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
            else if (!hasAnySource()) message.value = L10n.s(R.string.msg_connect_sources)
            else if (quiet) {
                // Resumen de lo que cambió desde la última vez (p. ej. horas jugadas en Steam desde el PC).
                val changed = _games.value.filter { g -> before[g.id]?.let { g.totalMinutes > it } == true }
                if (changed.isNotEmpty()) {
                    val mins = changed.sumOf { it.totalMinutes - (before[it.id] ?: 0) }
                    val names = changed.take(3).joinToString(", ") { it.title }
                    message.value = L10n.s(R.string.msg_time_updated,
                        if (changed.size > 3) L10n.s(R.string.msg_and_more, names, changed.size - 3) else names, formatPlaytime(mins))
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
        status.value = L10n.s(R.string.status_syncing, source.label)
        return try {
            val known = _games.value.associateBy { it.id }
            val fresh: List<Game> = when (source) {
                Source.STEAM -> steamSource().let { src ->
                    src.fetchGames().also {
                        src.familyError?.let { e -> if (!quiet) message.value = L10n.s(R.string.msg_family_error, e) }
                    }
                }
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
            if (!quiet) message.value = L10n.s(R.string.msg_sync_count, source.label, fresh.size)
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
            ?: error(L10n.s(R.string.err_steam_session))
        return SteamSource.withToken(token, id)
    }

    /** Se llama cuando el usuario termina de iniciar sesión en la web de Steam. */
    fun steamLinked() = viewModelScope.launch {
        status.value = L10n.s(R.string.status_linking_steam)
        try {
            val token = SteamLogin.fetchWebToken() ?: error("Steam no confirmó la sesión")
            val id = SteamLogin.steamIdFromToken(token) ?: error("no se pudo leer el SteamID")
            settings.steamLinkedId = id
            settings.steamLinkedName = SteamLogin.profileName(id)
            message.value = L10n.s(R.string.msg_steam_linked) + (settings.steamLinkedName?.let { ": $it" } ?: "")
            syncSource(Source.STEAM)?.let { message.value = it }
            startEnrichment()
        } catch (e: Exception) {
            message.value = L10n.s(R.string.msg_steam_link_failed, e.message ?: "")
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
                status.value = L10n.s(R.string.status_metadata, i + 1, pending.size)
                val enriched = runCatching { metadata.enrich(g) }.getOrElse { g.copy(metaFetched = true) }
                replace(enriched, save = i % 10 == 9 || i == pending.lastIndex)
            }
            status.value = null
            // Tras actualizar la app con buscadores de carátulas nuevos, repasar una vez los juegos que no tienen.
            if (settings.coverPass < COVER_PASS) {
                missingCoversPass(quiet = true)
                settings.coverPass = COVER_PASS
            }
        }
    }

    private companion object {
        val MY_COLLECTION get() = L10n.s(R.string.shelf_my_collection)
        /** Súbelo cuando se añada una fuente de carátulas nueva para que se repasen los juegos sin carátula. */
        const val COVER_PASS = 2
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
            coverUrl = if (current.customCover) current.coverUrl else enriched.coverUrl,
            customCover = current.customCover,
            rating = current.rating,
            metascore = current.metascore, userScore = current.userScore, metacriticUrl = current.metacriticUrl,
            hltbMain = current.hltbMain, hltbExtra = current.hltbExtra, hltbComplete = current.hltbComplete,
            hltbUrl = current.hltbUrl, extFetchedAt = current.extFetchedAt,
            coverAspect = if (current.customCover || enriched.coverUrl == current.coverUrl) current.coverAspect else null,
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
            message.value = L10n.s(R.string.msg_session_added, g.title, formatPlaytime(s.minutes)) +
                if (s.measured) "" else L10n.s(R.string.msg_session_approx)
        }
        autoSync()
    }

    fun epicLogin(code: String) = viewModelScope.launch {
        status.value = L10n.s(R.string.status_epic_connecting)
        try {
            epic.login(code)
            message.value = L10n.s(R.string.msg_epic_linked)
            syncSource(Source.EPIC)?.let { message.value = it }
            startEnrichment()
        } catch (e: Exception) {
            message.value = L10n.s(R.string.msg_epic_login_failed, e.message ?: "")
        } finally {
            status.value = null
        }
    }

    // ------------------------------------------------------------------ Epic: vincular con código

    sealed interface EpicLink {
        data object Starting : EpicLink
        data class Waiting(val code: EpicSource.DeviceCode) : EpicLink
        data class Failed(val message: String) : EpicLink
        data object Done : EpicLink
    }

    val epicLink = MutableStateFlow<EpicLink?>(null)
    private var epicLinkJob: Job? = null

    /** Pide un código a Epic y espera (sin bloquear) a que el usuario lo acepte en la web. */
    fun epicStartLink() {
        epicLinkJob?.cancel()
        epicLinkJob = viewModelScope.launch {
            epicLink.value = EpicLink.Starting
            try {
                val code = epic.startDeviceLogin()
                epicLink.value = EpicLink.Waiting(code)
                while (System.currentTimeMillis() < code.expiresAt) {
                    kotlinx.coroutines.delay(code.intervalSec * 1000L)
                    if (epic.pollDeviceLogin(code)) {
                        epicLink.value = EpicLink.Done
                        message.value = L10n.s(R.string.msg_epic_linked)
                        syncSource(Source.EPIC)?.let { message.value = it }
                        status.value = null
                        startEnrichment()
                        return@launch
                    }
                }
                epicLink.value = EpicLink.Failed(L10n.s(R.string.msg_epic_code_expired))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = if (e is java.io.IOException && e !is com.repisa.app.data.HttpException)
                    L10n.s(R.string.msg_epic_no_conn)
                else e.message ?: e.javaClass.simpleName
                epicLink.value = EpicLink.Failed(msg)
            }
        }
    }

    fun epicCancelLink() { epicLinkJob?.cancel(); epicLink.value = null }

    // ------------------------------------------------------------------ Metacritic / HowLongToBeat

    private val extLoading = mutableSetOf<String>()

    /** Al abrir la ficha: consulta Metacritic y HowLongToBeat si no se hizo en las últimas 2 semanas. */
    fun loadExternalInfo(game: Game, force: Boolean = false) {
        val fresh = game.extFetchedAt?.let { System.currentTimeMillis() - it < 14L * 86_400_000 } == true
        if ((fresh && !force) || !extLoading.add(game.id)) return
        viewModelScope.launch {
            try {
                val (mc, hl) = kotlinx.coroutines.coroutineScope {
                    val a = async { runCatching { com.repisa.app.sources.GameInfo.metacritic(game) }.getOrNull() }
                    val b = async { runCatching { com.repisa.app.sources.GameInfo.hltb(game) }.getOrNull() }
                    a.await() to b.await()
                }
                mutex.withLock {
                    _games.update { list -> list.map { g ->
                        if (g.id != game.id) g else g.copy(
                            metascore = mc?.metascore ?: g.metascore, userScore = mc?.userScore ?: g.userScore,
                            metacriticUrl = mc?.url ?: g.metacriticUrl,
                            hltbMain = hl?.main ?: g.hltbMain, hltbExtra = hl?.extra ?: g.hltbExtra,
                            hltbComplete = hl?.complete ?: g.hltbComplete, hltbUrl = hl?.url ?: g.hltbUrl,
                            extFetchedAt = System.currentTimeMillis(),
                        )
                    } }
                    store.save(_games.value)
                }
            } finally {
                extLoading.remove(game.id)
            }
        }
    }

    // ------------------------------------------------------------------ Calificaciones

    val ratingScale = MutableStateFlow(
        runCatching { com.repisa.app.model.RatingScale.valueOf(settings.ratingScale) }.getOrDefault(com.repisa.app.model.RatingScale.TEN)
    )

    fun setRatingScale(s: com.repisa.app.model.RatingScale) { settings.ratingScale = s.name; ratingScale.value = s }

    fun setRating(game: Game, rating: com.repisa.app.model.Rating?) = updateGame(game.copy(rating = rating))

    // ------------------------------------------------------------------ Carátulas

    /** El usuario eligió una carátula en el buscador. */
    fun setCustomCover(game: Game, url: String) =
        updateGame(game.copy(coverUrl = url, coverAspect = null, customCover = true))

    /** Busca carátula para todos los juegos que no tienen (o cuya carátula ya no carga). */
    fun findMissingCovers() {
        if (enrichJob?.isActive == true) return
        enrichJob = viewModelScope.launch { missingCoversPass(quiet = false) }
    }

    private suspend fun missingCoversPass(quiet: Boolean) {
        run {
            status.value = L10n.s(R.string.status_checking_covers)
            val candidates = _games.value.filter { !it.customCover && !it.userEdited }
            val missing = candidates.filter { g ->
                val url = g.coverUrl
                url == null || (url.startsWith("http") && !com.repisa.app.data.Http.exists(url))
            }
            var found = 0
            missing.forEachIndexed { i, g ->
                status.value = L10n.s(R.string.status_finding_covers, i + 1, missing.size)
                val url = runCatching { coverSearch.auto(g) }.getOrNull()
                if (url != null) found++
                mutex.withLock {
                    _games.update { list -> list.map {
                        if (it.id == g.id) it.copy(coverUrl = url, coverAspect = null) else it
                    } }
                    if (i % 10 == 9 || i == missing.lastIndex) store.save(_games.value)
                }
            }
            status.value = null
            if (quiet && found == 0) return
            message.value = when {
                missing.isEmpty() -> L10n.s(R.string.msg_all_covers)
                else -> L10n.s(R.string.msg_covers_found, found, missing.size) +
                    if (found < missing.size) L10n.s(R.string.msg_covers_rest) else ""
            }
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
