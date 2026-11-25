package com.repisa.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repisa.app.R
import com.repisa.app.data.LibraryStore
import com.repisa.app.i18n.L10n
import com.repisa.app.data.Settings
import com.repisa.app.data.hasPersonalData
import com.repisa.app.data.mergeGame
import com.repisa.app.launch.Launcher
import com.repisa.app.model.Consoles
import com.repisa.app.model.Copies
import com.repisa.app.model.label
import com.repisa.app.model.Game
import com.repisa.app.model.playStatus
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SortMode(private val labelRes: Int) {
    ALPHA(R.string.sort_alpha), PLAYTIME(R.string.sort_playtime), RECENT(R.string.sort_recent), YEAR(R.string.sort_year),
    CONSOLE(R.string.sort_console), RATING(R.string.sort_rating), METACRITIC(R.string.sort_metacritic),
    ACHIEVEMENTS(R.string.sort_achievements);

    val label: String get() = L10n.s(labelRes)
}

enum class GroupMode(private val labelRes: Int) {
    NONE(R.string.group_none), GENRE(R.string.group_genre), CONSOLE(R.string.group_console),
    DEVELOPER(R.string.group_developer), SOURCE(R.string.group_source), DECADE(R.string.group_decade),
    FAVORITES(R.string.group_favorites), STATUS(R.string.group_status);

    val label: String get() = L10n.s(labelRes)
}

/** Una repisa. [key] identifica la repisa para poder plegarla (y que siga plegada al volver). */
data class ShelfGroup(val title: String, val subtitle: String, val games: List<Game>, val key: String = "grp:$title",
                      /** Al agrupar por consola: cuál es (para dibujarla junto a la placa). */
                      val consoleId: String? = null)

data class ViewOptions(
    val sort: SortMode = SortMode.ALPHA,
    val group: GroupMode = GroupMode.NONE,
    val source: Source? = null,
    val query: String = "",
    val continueShelf: Boolean = true,
    /** Se incrementa para forzar a reconstruir las repisas. */
    val rev: Int = 0,
    val collections: List<GameCollection> = emptyList(),
    /** Solo los juegos de esta consola (al elegirla en «Mis consolas»). */
    val console: String? = null,
)

/** Colección propia del usuario (repisa con nombre). */
@kotlinx.serialization.Serializable
data class GameCollection(val id: String, val name: String)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    val settings = Settings(app)
    private val store = LibraryStore(app)
    val launcher = Launcher(app, settings)
    val epic = EpicSource(settings)
    val coverSearch = com.repisa.app.sources.CoverSearch(app, settings)
    private val metadata = Metadata(settings, coverSearch, coverSearch.gameTdb)
    private val mutex = Mutex()

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    private val _options = MutableStateFlow(
        ViewOptions(
            sort = runCatching { SortMode.valueOf(settings.sortMode) }.getOrDefault(SortMode.ALPHA),
            group = runCatching { GroupMode.valueOf(settings.groupMode) }.getOrDefault(GroupMode.NONE),
            continueShelf = settings.continueShelf,
            collections = runCatching {
                kotlinx.serialization.json.Json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(GameCollection.serializer()),
                    settings.collectionsJson ?: "[]")
            }.getOrDefault(emptyList()),
        )
    )

    fun setContinueShelf(on: Boolean) { settings.continueShelf = on; _options.update { it.copy(continueShelf = on) } }
    val options: StateFlow<ViewOptions> = _options.asStateFlow()

    /** Texto de progreso de la sincronización (null = sin tareas). */
    val status = MutableStateFlow<String?>(null)
    /** Mensajes puntuales para mostrar en un snackbar. */
    val message = MutableStateFlow<String?>(null)
    /** Juego resaltado (con el mando o el dedo): se muestra en la segunda pantalla. */
    val focused = MutableStateFlow<Game?>(null)
    val selectedId = MutableStateFlow<String?>(null)
    val inSettings = MutableStateFlow(false)

    /** Guía de primera vez visible. */
    val onboarding = MutableStateFlow(!settings.onboardingDone)

    fun finishOnboarding() { settings.onboardingDone = true; onboarding.value = false }

    fun restartOnboarding() { settings.onboardingDone = false; settings.onboardingStep = 0; onboarding.value = true; inSettings.value = false }

    // ------------------------------------------------------------------ Selección múltiple y colecciones

    /** Selección de varias carátulas (para poner estado o añadir a una colección de golpe). */
    val selecting = MutableStateFlow(false)
    val selection = MutableStateFlow<Set<String>>(emptySet())

    fun startSelection(game: Game? = null) { selecting.value = true; selection.value = setOfNotNull(game?.id) }
    fun toggleSelected(game: Game) = selection.update { if (game.id in it) it - game.id else it + game.id }
    fun selectAll(games: List<Game>) = selection.update { it + games.map { g -> g.id } }
    fun endSelection() { selecting.value = false; selection.value = emptySet() }

    /** Aplica un cambio a varios juegos (y a sus copias unidas) y guarda una sola vez. */
    private fun updateMany(ids: Set<String>, change: (Game) -> Game) = viewModelScope.launch {
        val all = ids + displayGames.value.filter { it.id in ids }.flatMap { it.copyIds }
        mutex.withLock {
            _games.update { list -> list.map { if (it.id in all) change(it) else it } }
            store.save(_games.value)
        }
    }

    fun setStatusFor(ids: Set<String>, status: com.repisa.app.model.PlayStatus?) {
        // El estado va en la copia principal (la que se ve); a las demás copias se les quita para no contradecirla.
        val mains = displayGames.value.filter { it.id in ids }.map { it.id }.toSet()
        updateMany(ids) { g -> g.copy(status = if (g.id in mains) status?.name else null) }
        message.value = L10n.s(R.string.msg_status_set, ids.size, status?.label ?: L10n.s(R.string.shelf_no_status))
    }

    val collections = MutableStateFlow(loadCollections())

    private fun loadCollections(): List<GameCollection> = runCatching {
        kotlinx.serialization.json.Json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(GameCollection.serializer()),
            settings.collectionsJson ?: "[]")
    }.getOrDefault(emptyList())

    private fun saveCollections(list: List<GameCollection>) {
        settings.collectionsJson = kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(GameCollection.serializer()), list)
        collections.value = list
        _options.update { it.copy(rev = it.rev + 1, collections = list) }
    }

    fun createCollection(name: String): GameCollection {
        val c = GameCollection("c${System.currentTimeMillis()}", name.trim().ifBlank { L10n.s(R.string.collection_default) })
        saveCollections(collections.value + c)
        return c
    }

    fun renameCollection(id: String, name: String) =
        saveCollections(collections.value.map { if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }) else it })

    fun deleteCollection(id: String) {
        saveCollections(collections.value.filter { it.id != id })
        updateMany(_games.value.filter { id in it.collections }.map { it.id }.toSet()) { it.copy(collections = it.collections - id) }
    }

    fun addToCollection(ids: Set<String>, collectionId: String) {
        val mains = displayGames.value.filter { it.id in ids }.map { it.id }.toSet()
        updateMany(mains.ifEmpty { ids }.let { m -> m }) { g -> if (collectionId in g.collections) g else g.copy(collections = g.collections + collectionId) }
        message.value = L10n.s(R.string.msg_added_collection, ids.size, collections.value.firstOrNull { it.id == collectionId }?.name ?: "")
    }

    fun removeFromCollection(ids: Set<String>, collectionId: String) {
        updateMany(ids) { g -> g.copy(collections = g.collections - collectionId) }
        message.value = L10n.s(R.string.msg_removed_collection, ids.size, collections.value.firstOrNull { it.id == collectionId }?.name ?: "")
    }

    // ------------------------------------------------------------------ Logros

    val achievementStore = com.repisa.app.data.AchievementStore(app, settings)
    /** Logros cargados por juego (null = el juego no tiene logros conocidos). */
    val achievements = MutableStateFlow<Map<String, com.repisa.app.sources.AchievementSet?>>(emptyMap())
    val achievementsLoading = MutableStateFlow<Set<String>>(emptySet())
    val achievementErrors = MutableStateFlow<Set<String>>(emptySet())

    private suspend fun fetchAchievements(game: Game): com.repisa.app.sources.AchievementSet? = when (game.source) {
        Source.STEAM, Source.EPIC -> com.repisa.app.sources.Achievements.steam(game, settings, achievementStore.manual(game.id))
        Source.EMULATOR -> com.repisa.app.sources.Achievements.retro(game, settings)
    }

    private fun publishAchievements(game: Game, set: com.repisa.app.sources.AchievementSet?) {
        achievements.update { it + (game.id to set) }
        val unlocked = set?.unlocked
        val total = set?.items?.size
        if (set != null && (game.achUnlocked != unlocked || game.achTotal != total)) {
            _games.value.firstOrNull { it.id == game.id }?.let { updateGame(it.copy(achUnlocked = unlocked, achTotal = total)) }
        }
    }

    /** Carga los logros de un juego (caché de 1 día; [force] para pedirlos ya). */
    fun loadAchievements(game: Game, force: Boolean = false) = viewModelScope.launch {
        val cached = achievementStore.cached(game.id)
        if (cached != null) achievements.update { it + (game.id to cached) }
        if (!force && cached != null && System.currentTimeMillis() - cached.fetchedAt < 86_400_000L) return@launch
        if (game.id in achievementsLoading.value) return@launch
        achievementsLoading.update { it + game.id }
        val result = runCatching { fetchAchievements(game) }
        val fresh = result.getOrNull()
        // Si el servicio no respondió, se avisa con «Reintentar» en vez de decir que el juego no tiene logros.
        achievementErrors.update { if (result.isFailure) it + game.id else it - game.id }
        if (fresh != null) achievementStore.save(game.id, fresh)
        publishAchievements(game, fresh ?: cached)
        achievementsLoading.update { it - game.id }
    }

    /** Marca o desmarca a mano un logro (juegos de Epic con la lista de Steam, o perfil de Steam privado). */
    fun toggleManualAchievement(game: Game, achId: String) {
        val marks = achievementStore.toggleManual(game.id, achId)
        val set = achievements.value[game.id] ?: return
        val updated = set.copy(items = set.items.map { a ->
            if (set.kind == com.repisa.app.sources.Achievements.STEAM) a else a.copy(unlockedAt = marks[a.id], manual = a.id in marks)
        })
        achievementStore.save(game.id, updated)
        publishAchievements(game, updated)
    }

    /** Logros de Repisa del juego con la fecha en que se cumplieron (las guarda la primera vez que se ven). */
    fun repisaAchievements(game: Game): List<Pair<com.repisa.app.model.RepisaAchievement, Long?>> {
        val ctx = com.repisa.app.model.GameAchContext(game, sessions.value)
        val list = com.repisa.app.model.RepisaAchievement.entries
        val met = list.filter { it.met(ctx) }.map { it.name }.toSet()
        val seen = achievementStore.repisaSeen(game.id, met)
        return list.map { it to (if (it.name in met) seen[it.name] else null) }
    }

    /** Contexto de logros de un juego (para saber si es platino y qué le falta). */
    fun achContext(game: Game) = com.repisa.app.model.GameAchContext(game, sessions.value)

    // ------------------------------------------------------------------ «Hoy juego…»

    /**
     * Recordatorio para la pantalla de abajo: el juego que tienes empezado y llevas 4 días o más sin tocar (el último
     * que jugaste de esos); si no hay, un pendiente corto que lleva tiempo esperando (cambia cada día).
     */
    data class Reminder(val game: Game, val staleDays: Int?, val hltbHours: Float?)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val reminder: StateFlow<Reminder?> by lazy { displayGames.mapLatest { games ->
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val stale = games.filter { it.playStatus == com.repisa.app.model.PlayStatus.PLAYING }
            .mapNotNull { g -> g.lastPlayedAny?.let { g to ((now - it) / day).toInt() } }
            .filter { it.second >= 4 }
            .maxByOrNull { it.first.lastPlayedAny ?: 0 }
        if (stale != null) return@mapLatest Reminder(stale.first, stale.second, null)
        val backlog = games.filter { it.playStatus == com.repisa.app.model.PlayStatus.BACKLOG }
        val short = backlog.filter { (it.hltbMain ?: 0f) in 0.5f..20f }.ifEmpty { backlog }.sortedBy { it.id }
        if (short.isEmpty()) null else short[((now / day) % short.size).toInt()].let { Reminder(it, null, it.hltbMain) }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null) }

    // ------------------------------------------------------------------ Platinos y logros de la app

    /** Juegos con platino → fecha en que se consiguió. */
    val platinums = MutableStateFlow<Map<String, Long>>(emptyMap())
    /** Logros de la app con su fecha (null = aún no) y su progreso actual. */
    val appAchievements = MutableStateFlow<List<Triple<com.repisa.app.model.AppAchievement, Long?, Int>>>(emptyList())

    /** Recalcula platinos y logros de la app; avisa de los nuevos (salvo la primera vez, que solo los apunta). */
    private suspend fun recomputeAchievements() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        val games = displayGames.value
        if (games.isEmpty()) return@withContext
        val ses = sessions.value
        val plat = games.filter { com.repisa.app.model.Platinum.isPlatinum(com.repisa.app.model.GameAchContext(it, ses)) }
        val knownPlat = achievementStore.repisaKnown(PLAT_KEY)
        val seenPlat = achievementStore.repisaSeen(PLAT_KEY, plat.map { it.id }.toSet())
        val ctx = com.repisa.app.model.AppAchContext(games, ses, plat.size, collections.value.size, wishlist.value.size,
            ShelfTheme.current.name)
        val metApp = com.repisa.app.model.AppAchievement.entries.filter { it.met(ctx) }
        val knownApp = achievementStore.repisaKnown(APP_KEY)
        val seenApp = achievementStore.repisaSeen(APP_KEY, metApp.map { it.name }.toSet())
        // Fecha del platino: la del último logro conseguido (Steam, RetroAchievements...), que es cuando de verdad
        // se completó; si no se conoce (juego sin logros propios o aún sin cargar), cuándo lo vio Repisa.
        platinums.value = plat.associate { g ->
            val last = (listOf(g.id) + g.copyIds).mapNotNull { id -> achievementStore.cached(id) }
                .flatMap { it.items }.mapNotNull { it.unlockedAt?.takeIf { t -> t > 1 } }.maxOrNull()
            g.id to (last ?: seenPlat[g.id] ?: System.currentTimeMillis())
        }
        appAchievements.value = com.repisa.app.model.AppAchievement.entries.map { a ->
            Triple(a, if (a in metApp) seenApp[a.name] else null, a.value(ctx))
        }
        // Avisos: un mensaje con lo nuevo (la primera vez solo se apunta lo que ya tenías).
        if (settings.rawString("ach_init") == null) { settings.putRawString("ach_init", "1"); return@withContext }
        val news = plat.filter { it.id !in knownPlat }.map { "💎 " + L10n.s(R.string.plat_new, it.title) } +
            metApp.filter { it.name !in knownApp }.map { "🏆 " + L10n.s(R.string.aach_new, it.title) }
        if (news.isNotEmpty()) message.value = news.take(3).joinToString("\n") + if (news.size > 3) "\n…" else ""
    }



    fun checkRetroAchievements() = viewModelScope.launch {
        val ok = runCatching { com.repisa.app.sources.Achievements.raCheck(settings) }.getOrDefault(false)
        message.value = L10n.s(if (ok) R.string.ra_ok else R.string.ra_failed)
        if (ok) achievementsPass()
    }

    private var achievementsJob: Job? = null

    /** En segundo plano: logros de los juegos de Steam jugados y de los de emulador con RetroAchievements. */
    fun achievementsPass() {
        if (achievementsJob?.isActive == true) return
        achievementsJob = viewModelScope.launch {
            // Tras cambiar cómo se leen los logros de Steam, volver a pedirlos una vez.
            if (settings.achPass < ACH_PASS) {
                achievementStore.clearAll()
                achievements.value = emptyMap()
                mutex.withLock { _games.update { list -> list.map { if (it.achTotal != null) it.copy(achTotal = null, achUnlocked = null) else it } } }
                settings.achPass = ACH_PASS
            }
            val ra = settings.raUser != null && settings.raApiKey != null
            val pending = displayGames.value.filter { g ->
                g.achTotal == null && when (g.source) {
                    Source.STEAM -> g.playtimeMinutes > 0
                    Source.EMULATOR -> ra && g.consoleId in com.repisa.app.sources.Achievements.raConsoles
                    Source.EPIC -> false
                }
            }.take(80)
            for (g in pending) {
                loadAchievements(g).join()
                kotlinx.coroutines.delay(1200)
            }
        }
    }

    // ------------------------------------------------------------------ Actualizaciones

    /** Versión nueva disponible (se muestra un aviso con sus notas). */
    val update = MutableStateFlow<com.repisa.app.data.Updater.Release?>(null)
    /** Progreso de la descarga de la actualización (0..1), o null si no se está descargando. */
    val updateProgress = MutableStateFlow<Float?>(null)

    /**
     * Busca versión nueva al abrir Repisa y cada vez que se vuelve a ella (como mucho una vez cada 10 min).
     * Si la hay, el aviso es obligatorio: solo se puede actualizar. [quiet]: sin mensajes si no hay nada nuevo.
     */
    fun checkForUpdate(quiet: Boolean) = viewModelScope.launch {
        if (update.value != null) return@launch
        if (quiet && System.currentTimeMillis() - lastUpdateCheck < 10 * 60_000L) return@launch
        lastUpdateCheck = System.currentTimeMillis()
        val result = runCatching { com.repisa.app.data.Updater.latest() }
        val r = result.getOrNull()
        when {
            result.exceptionOrNull() is com.repisa.app.data.Updater.NotVisible -> {
                lastUpdateCheck = 0; if (!quiet) message.value = L10n.s(R.string.update_private)
            }
            r == null -> { lastUpdateCheck = 0; if (!quiet) message.value = L10n.s(R.string.update_check_failed) }
            !com.repisa.app.data.Updater.isNewer(r.version, com.repisa.app.BuildConfig.VERSION_NAME) ->
                if (!quiet) message.value = L10n.s(R.string.update_none, com.repisa.app.BuildConfig.VERSION_NAME)
            else -> update.value = r
        }
    }

    private var lastUpdateCheck = 0L
    /** Se pidió el permiso de instalar: al volver a Repisa con él concedido, la actualización sigue sola. */
    private var installAfterPermission = false
    /** APK ya descargado (si se cancela el instalador, «Actualizar» lo vuelve a abrir sin descargar otra vez). */
    private var downloadedApk: Pair<String, java.io.File>? = null

    fun installUpdate() = viewModelScope.launch {
        val r = update.value ?: return@launch
        if (updateProgress.value != null) return@launch
        val app = getApplication<android.app.Application>()
        if (!com.repisa.app.data.Updater.canInstall(app)) {
            installAfterPermission = true
            message.value = L10n.s(R.string.update_allow_install)
            runCatching { app.startActivity(com.repisa.app.data.Updater.permissionIntent(app)) }
            return@launch
        }
        installAfterPermission = false
        downloadedApk?.takeIf { it.first == r.version && it.second.exists() }?.let {
            runCatching { com.repisa.app.data.Updater.install(app, it.second) }; return@launch
        }
        updateProgress.value = 0f
        runCatching { com.repisa.app.data.Updater.download(app, r) { updateProgress.value = it } }
            .onSuccess { downloadedApk = r.version to it; runCatching { com.repisa.app.data.Updater.install(app, it) } }
            .onFailure { message.value = L10n.s(R.string.update_failed, it.message ?: "") }
        updateProgress.value = null
    }

    // ------------------------------------------------------------------ Copia de seguridad

    /** Guarda la copia de seguridad en el archivo que eligió el usuario. */
    fun exportBackup(uri: android.net.Uri) = viewModelScope.launch {
        val app = getApplication<android.app.Application>()
        runCatching {
            val text = com.repisa.app.data.Backup.export(_games.value, settings.prefsForBackup, com.repisa.app.BuildConfig.VERSION_NAME,
                java.io.File(app.filesDir, "covers"), archive.values.toList(), sessions.value)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
            }
        }.onSuccess { message.value = L10n.s(R.string.backup_saved, _games.value.size) }
            .onFailure { message.value = L10n.s(R.string.backup_failed, it.message ?: "") }
    }

    /** Lee una copia de seguridad: une la biblioteca y restaura los ajustes. */
    fun importBackup(uri: android.net.Uri) = viewModelScope.launch {
        val app = getApplication<android.app.Application>()
        runCatching {
            val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                app.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
            }
            val parsed = com.repisa.app.data.Backup.parse(text).let {
                com.repisa.app.data.Backup.Parsed(
                    com.repisa.app.data.Backup.restoreCovers(text, it.games, java.io.File(app.filesDir, "covers")), it.settings, it.version, it.archive,
                    it.sessions)
            }
            com.repisa.app.data.Backup.restoreSettings(parsed.settings, settings.prefsForBackup)
            mutex.withLock {
                _games.value = com.repisa.app.data.Backup.mergeLibrary(_games.value, parsed.games)
                store.save(_games.value)
                val inLibrary = _games.value.map { it.id }.toSet()
                val extra = parsed.archive.filter { it.id !in inLibrary && it.id !in archive }
                if (extra.isNotEmpty()) { archive = archive + extra.associateBy { it.id }; store.saveArchive(archive) }
            }
            if (parsed.sessions.isNotEmpty()) {
                sessions.value = com.repisa.app.data.Backup.mergeSessions(sessions.value, parsed.sessions)
                sessionLog.save(sessions.value)
            }
            // Refrescar lo que depende de los ajustes recién restaurados.
            hidden.value = settings.hiddenGames
            ShelfTheme.custom = CustomTheme.decode(settings.customTheme).build()
            ShelfTheme.current = ShelfTheme.of(settings.shelfTheme)
            wishlistOn.value = settings.wishlistShelf
            wishSort.value = settings.wishlistSort
            com.repisa.app.data.WishlistJob.schedule(app)
            onboarding.value = !settings.onboardingDone
            ratingScale.value = runCatching { com.repisa.app.model.RatingScale.valueOf(settings.ratingScale) }.getOrDefault(ratingScale.value)
            _options.update { it.copy(
                sort = runCatching { SortMode.valueOf(settings.sortMode) }.getOrDefault(it.sort),
                group = runCatching { GroupMode.valueOf(settings.groupMode) }.getOrDefault(it.group),
            ) }
            parsed.settings["language"]?.let { el ->
                val tag = runCatching { (el as kotlinx.serialization.json.JsonObject)["v"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content } }.getOrNull()
                com.repisa.app.i18n.AppLanguage.fromTag(tag)?.takeIf { it != L10n.current }?.let { L10n.setLanguage(app, it) }
            }
            parsed.games.size
        }.onSuccess { message.value = L10n.s(R.string.backup_restored, it) }
            .onFailure { message.value = L10n.s(R.string.backup_failed, it.message ?: "") }
    }

    init {
        com.repisa.app.model.StatusRules.auto = settings.autoStatus
        ShelfTheme.custom = CustomTheme.decode(settings.customTheme).build()
        ShelfTheme.current = ShelfTheme.of(settings.shelfTheme)
        com.repisa.app.model.StatusRules.order = com.repisa.app.model.StatusRules.parseOrder(settings.statusOrder)
    }

    /** Cambia el orden de las repisas por estado. */
    fun setStatusOrder(order: List<com.repisa.app.model.PlayStatus>) {
        com.repisa.app.model.StatusRules.order = order
        settings.statusOrder = order.joinToString(",") { it.name }
        _options.update { it.copy(rev = it.rev + 1) }
    }

    /** Repisas plegadas: solo se ve la placa con el nombre; se despliegan tocándola. */
    val collapsed = MutableStateFlow(settings.collapsedShelves)
    fun toggleCollapsed(key: String) {
        val now = collapsed.value.let { if (key in it) it - key else it + key }
        settings.collapsedShelves = now
        collapsed.value = now
    }

    /** Activa o desactiva el estado automático y vuelve a dibujar las repisas. */
    fun setAutoStatus(on: Boolean) {
        settings.autoStatus = on
        com.repisa.app.model.StatusRules.auto = on
        _options.update { it.copy(rev = it.rev + 1) } // reconstruir «Repisas por estado»
    }

    /** Juegos quitados de la repisa por el usuario. */
    val hidden = MutableStateFlow(settings.hiddenGames)

    /** Quita el juego (y sus copias) de la repisa; no vuelve al sincronizar. */
    fun hideGame(game: Game) {
        settings.hiddenGames = settings.hiddenGames + game.id + game.copyIds
        hidden.value = settings.hiddenGames
        selectedId.value = null
        message.value = L10n.s(R.string.msg_hidden, game.title)
    }

    fun unhideAll() {
        settings.hiddenGames = emptySet()
        hidden.value = emptySet()
    }

    /** Lista de deseos de Steam (ver [refreshWishlist]). */
    val wishlist = MutableStateFlow<List<com.repisa.app.sources.WishItem>>(emptyList())
    val wishlistOn = MutableStateFlow(settings.wishlistShelf)
    /** Orden de la repisa de la lista de deseos (ver [Settings.wishlistSort]). */
    val wishSort = MutableStateFlow(settings.wishlistSort)
    fun setWishSort(s: String) { settings.wishlistSort = s; wishSort.value = s }
    /** true mientras se vuelve a consultar la lista de deseos desde su repisa. */
    val wishRefreshing = MutableStateFlow(false)
    fun refreshWishlistNow() = viewModelScope.launch {
        if (wishRefreshing.value) return@launch
        wishRefreshing.value = true
        try { refreshWishlist(quiet = false) } finally { wishRefreshing.value = false }
    }
    /** Precio de cada caja de la lista de deseos, por id de juego («wish:<appid>»). */
    val wishPrices: StateFlow<Map<String, com.repisa.app.sources.WishItem>> = wishlist
        .map { l -> l.associateBy { "wish:${it.appId}" } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    /** Registro de partidas (calendario y resumen del año). */
    val sessions = MutableStateFlow<List<com.repisa.app.data.PlaySession>>(emptyList())
    private val sessionLog = com.repisa.app.data.SessionLog(app)
    private val wishStore = com.repisa.app.data.WishlistStore(app)

    /** Biblioteca tal como se muestra: copias del mismo juego unidas en una sola caja. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val displayGames: StateFlow<List<Game>> = combine(_games, hidden) { g, h -> g to h }
        .mapLatest { (g, h) -> Copies.merge(if (h.isEmpty()) g else g.filter { it.id !in h }) }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // También se reconstruye al cambiar de idioma (los títulos de las repisas están traducidos).
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val baseShelves: StateFlow<List<ShelfGroup>> = combine(displayGames, _options, L10n.version) { g, o, _ -> g to o }
        .mapLatest { (g, o) -> buildShelves(g, o) }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Repisas con la «Lista de deseos» de Steam al final (si está activada). */
    val shelves: StateFlow<List<ShelfGroup>> = combine(baseShelves, wishlist, _options, wishlistOn, wishSort) { base, wish, o, on, sort ->
        if (!on || wish.isEmpty() || (o.source != null && o.source != Source.STEAM)) return@combine base
        val q = o.query.trim().lowercase()
        val items = wish.filter { q.isEmpty() || it.name.lowercase().contains(q) }.let { l ->
            // Los que no tienen precio (gratis, sin salir...) siempre al final al ordenar por precio o descuento.
            when (sort) {
                "NAME" -> l.sortedBy { it.name.lowercase() }
                "PRICE_DESC" -> l.sortedWith(compareBy<com.repisa.app.sources.WishItem> { it.priceCents == null }.thenByDescending { it.priceCents ?: 0 })
                "PRICE_ASC" -> l.sortedWith(compareBy<com.repisa.app.sources.WishItem> { it.priceCents == null }.thenBy { it.priceCents ?: 0 })
                "DISCOUNT" -> l.sortedWith(compareByDescending<com.repisa.app.sources.WishItem> { it.discountPct }.thenBy { it.name.lowercase() })
                else -> l
            }
        }
        if (items.isEmpty()) return@combine base
        val onSale = items.count { it.discountPct > 0 }
        base + ShelfGroup(
            "⭐ " + L10n.s(R.string.wish_shelf),
            L10n.games(items.size) + if (onSale > 0) " · " + L10n.s(R.string.wish_on_sale, onSale) else "",
            items.map { it.toGame() }, key = "wishlist",
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var enrichJob: Job? = null
    private var loaded = false
    private val libraryLoaded = kotlinx.coroutines.CompletableDeferred<Unit>()
    private var syncJob: Job? = null

    /** Juegos que salieron de la biblioteca, guardados con tus datos (ver [LibraryStore.loadArchive]). */
    private var archive: Map<String, Game> = emptyMap()

    /** Quita juegos de la biblioteca guardando antes lo tuyo de cada uno. Llamar con [mutex] tomado. */
    private suspend fun removeGamesLocked(remove: (Game) -> Boolean) {
        val (gone, kept) = _games.value.partition(remove)
        val worth = gone.filter { it.hasPersonalData }
        if (worth.isNotEmpty()) {
            archive = archive + worth.associateBy { it.id }
            store.saveArchive(archive)
        }
        _games.value = kept
        store.save(kept)
    }

    init {
        viewModelScope.launch {
            _games.value = store.load()
            archive = store.loadArchive()
            sessions.value = sessionLog.load()
            wishlist.value = wishStore.load()
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
    /** Mostrar solo los juegos de una consola (null = todas). */
    fun setConsoleFilter(id: String?) = _options.update { it.copy(console = id) }
    /** Sala «Mis consolas» abierta. */
    val consolesRoom = MutableStateFlow(false)
    /** Pide volver a Estadísticas (al salir del filtro por consola, se vuelve a «Mis consolas»). */
    val reopenStats = MutableStateFlow(false)
    /** Quitar el filtro por consola y volver a «Mis consolas», de donde se eligió. */
    fun closeConsoleFilter() { setConsoleFilter(null); consolesRoom.value = true; reopenStats.value = true }

    private fun buildShelves(all: List<Game>, o: ViewOptions): List<ShelfGroup> {
        val q = o.query.trim().lowercase()
        val filtered = all.filter { g ->
            (o.source == null || g.source == o.source || o.source in g.groupSources) &&
                (o.console == null || g.consoleId == o.console) &&
                (q.isEmpty() || g.title.lowercase().contains(q) || g.developer?.lowercase()?.contains(q) == true ||
                    g.otherTitles.any { it.lowercase().contains(q) } ||
                    g.genres.any { it.lowercase().contains(q) } || g.console.name.lowercase().contains(q))
        }
        val sorted = when (o.sort) {
            SortMode.ALPHA -> filtered.sortedBy { it.sortKey }
            SortMode.PLAYTIME -> filtered.sortedWith(compareByDescending<Game> { it.totalMinutes }.thenBy { it.sortKey })
            SortMode.RECENT -> filtered.sortedWith(compareByDescending<Game> { it.lastPlayedAny ?: 0 }.thenBy { it.sortKey })
            SortMode.YEAR -> filtered.sortedWith(compareBy<Game> { it.releaseYear ?: 9999 }.thenBy { it.sortKey })
            SortMode.CONSOLE -> filtered.sortedWith(compareBy<Game> { it.console.year }.thenBy { it.console.name }.thenBy { it.sortKey })
            SortMode.RATING -> filtered.sortedWith(compareByDescending<Game> { it.rating?.overall ?: -1f }.thenBy { it.sortKey })
            SortMode.METACRITIC -> filtered.sortedWith(compareByDescending<Game> { it.metascore ?: -1 }.thenBy { it.sortKey })
            SortMode.ACHIEVEMENTS -> filtered.sortedWith(compareByDescending<Game> { g ->
                g.achTotal?.takeIf { it > 0 }?.let { (g.achUnlocked ?: 0).toFloat() / it } ?: -1f
            }.thenBy { it.sortKey })
        }
        fun group(title: String, games: List<Game>, key: String = "grp:${o.group.name}:$title", consoleId: String? = null) =
            ShelfGroup(title, "${L10n.games(games.size)} · ${hours(games)}", games, key, consoleId)

        // «Seguir jugando»: los últimos jugados (90 días), arriba del todo, sin buscar.
        val cont = if (o.continueShelf && q.isEmpty()) {
            val since = System.currentTimeMillis() - 90L * 86_400_000
            sorted.filter { (it.lastPlayedAny ?: 0) > since }.sortedByDescending { it.lastPlayedAny }.take(8)
        } else emptyList()
        val head = if (cont.isEmpty()) emptyList() else listOf(ShelfGroup(L10n.s(R.string.shelf_continue), L10n.s(R.string.shelf_continue_sub), cont, "continue"))

        // Colecciones propias («Probar», «Prioridad»...), como en la biblioteca de Steam: arriba, tras «Seguir jugando».
        val own = if (q.isEmpty()) o.collections.mapNotNull { c ->
            sorted.filter { c.id in it.collections }.takeIf { it.isNotEmpty() }?.let { group("📂 " + c.name, it, "col:${c.id}") }
        } else emptyList()

        return head + own + when (o.group) {
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
                .map { group(Consoles.byId(it.key).let { c -> if (c.id == "pc") "PC" else if (c.id == "other") c.label else "${c.maker} · ${c.name}" }, it.value,
                    consoleId = it.key) }
            GroupMode.DEVELOPER -> {
                // Estudios unificados (ATLUS = Atlus = Atlus Co., Ltd.); un juego de dos estudios va en los dos.
                val unknownKey = "\u0000"
                val byKey = linkedMapOf<String, MutableList<Game>>()
                val names = hashMapOf<String, MutableList<String>>()
                sorted.forEach { g ->
                    val studios = com.repisa.app.model.Studios.split(g.developer)
                    if (studios.isEmpty()) byKey.getOrPut(unknownKey) { mutableListOf() } += g
                    studios.forEach { st ->
                        val k = com.repisa.app.model.Studios.key(st)
                        byKey.getOrPut(k) { mutableListOf() } += g
                        names.getOrPut(k) { mutableListOf() } += st
                    }
                }
                byKey.entries.sortedWith(compareBy<Map.Entry<String, MutableList<Game>>> { it.key == unknownKey }
                    .thenByDescending { it.value.size }.thenBy { it.key })
                    .map { (k, games) ->
                        group(if (k == unknownKey) L10n.s(R.string.shelf_unknown_studio) else com.repisa.app.model.Studios.display(names.getValue(k)), games)
                    }
            }
            GroupMode.SOURCE -> sorted.groupBy { it.source }.entries.sortedBy { it.key.ordinal }
                .map { group(it.key.label, it.value) }
            GroupMode.DECADE -> sorted.groupBy { it.releaseYear?.let { y -> y / 10 * 10 } ?: 9999 }.entries
                .sortedBy { it.key }
                .map { group(if (it.key == 9999) L10n.s(R.string.shelf_unknown_year) else L10n.s(R.string.shelf_decade, it.key), it.value) }
            GroupMode.STATUS -> sorted.groupBy { it.playStatus }.entries
                .sortedBy { e -> e.key?.let { com.repisa.app.model.StatusRules.order.indexOf(it) } ?: 99 }
                .map { group(it.key?.let { st -> "${st.icon} ${st.label}" } ?: L10n.s(R.string.shelf_no_status), it.value) }
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
            if (settings.steamConfigured && settings.wishlistShelf) refreshWishlist(quiet = true)
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

    /** "Switch → Eden · DS → melonDS": emuladores con los que se abrirá cada sistema. */
    private fun detectedEmulators(consoles: Set<String>): String? {
        val found = launcher.detected(consoles)
        if (found.isEmpty()) return null
        return L10n.s(R.string.msg_emus_detected, found.entries.joinToString(" · ") { (id, pkg) ->
            "${com.repisa.app.model.Consoles.byId(id).name} → ${launcher.appLabel(pkg)}"
        })
    }

    /** Resumen de la última lectura de ES-DE/Cocoon (se muestra en Ajustes → Emuladores). */
    val frontendReport = MutableStateFlow(settings.frontendReport)

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
                    .also { frontendReport.value = settings.frontendReport }
            }
            mutex.withLock {
                val current = _games.value.associateBy { it.id }
                // Un juego que vuelve (cuenta vinculada otra vez) recupera su estado, nota, notas, colecciones...
                val restored = fresh.filter { current[it.id] == null && archive[it.id] != null }.map { it.id }.toSet()
                val merged = fresh.map { mergeGame(current[it.id] ?: archive[it.id], it) }.map { g ->
                    // Pendiente y con horas nuevas (jugado en el PC, en Steam o en Epic): pasa a «Jugando».
                    val before = current[g.id]?.totalMinutes
                    if (g.status == com.repisa.app.model.PlayStatus.BACKLOG.name && before != null && g.totalMinutes > before)
                        g.copy(status = com.repisa.app.model.PlayStatus.PLAYING.name) else g
                }
                // Horas nuevas que trae la fuente (jugado en el PC, en Steam...): se anotan como partida para el calendario,
                // salvo que Repisa ya midiera esa partida hace poco.
                val now = System.currentTimeMillis()
                val recent = sessions.value.filter { it.measured && now - it.end < 6 * 3_600_000L }.map { it.gameId }.toSet()
                val newSessions = merged.mapNotNull { g ->
                    val old = current[g.id] ?: return@mapNotNull null
                    val delta = g.playtimeMinutes - old.playtimeMinutes
                    if (delta <= 0 || g.id in recent) null
                    else com.repisa.app.data.PlaySession(g.id, g.title, g.consoleId, now - delta * 60_000L, delta, measured = false)
                }
                if (newSessions.isNotEmpty()) addSessions(newSessions)
                val freshIds = merged.map { it.id }.toSet()
                // Lo que la fuente ya no devuelve sale de la repisa (juego desinstalado, ROM borrada...), pero lo tuyo
                // se guarda por si vuelve.
                removeGamesLocked { it.source == source && it.id !in freshIds }
                _games.value = _games.value.filter { it.id !in freshIds } + merged
                store.save(_games.value)
                if (restored.isNotEmpty()) {
                    archive = archive - restored
                    store.saveArchive(archive)
                }
            }
            if (!quiet) message.value = L10n.s(R.string.msg_sync_count, source.label, fresh.size) +
                if (source == Source.EMULATOR) detectedEmulators(fresh.map { it.consoleId }.toSet())?.let { "\n$it" }.orEmpty() else ""
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
        val token = SteamLogin.token(settings)
            ?: error(L10n.s(R.string.err_steam_session) + (SteamLogin.lastProblem?.let { " ($it)" } ?: ""))
        return SteamSource.withToken(token, id)
    }

    /** Se llama cuando el usuario termina de iniciar sesión en la web de Steam. */
    fun steamLinked() = viewModelScope.launch {
        status.value = L10n.s(R.string.status_linking_steam)
        try {
            val token = SteamLogin.token(settings)
                ?: error("Steam no confirmó la sesión" + (SteamLogin.lastProblem?.let { " ($it)" } ?: ""))
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
        SteamLogin.clearSession(settings)
        mutex.withLock { removeGamesLocked { it.source == Source.STEAM } }
    }

    /** Busca portadas y datos que faltan, de uno en uno y en segundo plano. */
    fun startEnrichment(force: Boolean = false) {
        if (enrichJob?.isActive == true) return
        enrichJob = viewModelScope.launch {
            if (force) mutex.withLock { _games.update { list -> list.map { if (it.userEdited) it else it.copy(metaFetched = false) } } }
            // Tras mejorar la búsqueda de género, estudio y año, repasar una vez los juegos a los que les falta algo.
            if (settings.factsPass < FACTS_PASS) {
                mutex.withLock {
                    _games.update { list -> list.map { g ->
                        if (g.genres.isEmpty() || g.developer == null || g.releaseYear == null) g.copy(factsFetched = false) else g
                    } }
                }
                settings.factsPass = FACTS_PASS
            }
            // Revisión de géneros (una vez, tras actualizar a la versión que los mejora): la parte rápida, ya.
            val genreReview = settings.genrePass < GENRE_PASS
            val fastOk = !genreReview || genrePassFast()
            // Primero los juegos sin género (lo que más se nota), luego el resto.
            val pending = _games.value.filter { metadata.needsWork(it) }.sortedBy { it.genres.isNotEmpty() }
            pending.forEachIndexed { i, g ->
                status.value = L10n.s(R.string.status_metadata, i + 1, pending.size)
                val enriched = runCatching { metadata.enrich(g) }.getOrElse { g.copy(metaFetched = true) }
                replace(enriched, save = i % 10 == 9 || i == pending.lastIndex)
            }
            status.value = null
            // La parte lenta del repaso de géneros (Epic), cuando ya está todo lo demás.
            if (genreReview && genrePassEpic() && fastOk) settings.genrePass = GENRE_PASS
            achievementsPass()
            // Tras actualizar la app con buscadores de carátulas nuevos, repasar una vez los juegos que no tienen.
            if (settings.coverPass < COVER_PASS) {
                missingCoversPass(quiet = true)
                settings.coverPass = COVER_PASS
            }
        }
    }

    /**
     * Repaso de géneros, primera parte (rápida): normaliza los guardados («Gacha game» → Gacha), pone a los de Steam
     * los géneros de las etiquetas de la comunidad (de 50 en 50) y deja marcados para buscar los que no tienen
     * género y los de emulador sin identidad (para reconocer sus títulos en español e inglés). Lo puesto a mano no se toca.
     */
    private suspend fun genrePassFast(): Boolean {
        mutex.withLock {
            _games.update { list -> list.map { g ->
                val genres = com.repisa.app.model.Genres.normalize(g.genres, keepUnknown = g.userEdited)
                val redo = genres.isEmpty() || (!g.userEdited && g.source == Source.EMULATOR && g.wikidataId == null)
                g.copy(genres = genres, factsFetched = g.factsFetched && !redo, idChecked = g.idChecked && !redo,
                    // Lo que no se encontró en Steam antes se vuelve a intentar con la búsqueda mejorada.
                    steamChecked = g.steamChecked && g.steamMatch != null)
            } }
        }
        status.value = L10n.s(R.string.status_genres_steam)
        val steamIds = _games.value.filter { !it.userEdited && it.source == Source.STEAM }
            .mapNotNull { g -> g.id.removePrefix("steam:").toLongOrNull()?.let { g.id to it } }.toMap()
        val r = com.repisa.app.sources.SteamSource.tagGenres(steamIds.values.toList())
        mutex.withLock {
            _games.update { list -> list.map { g -> steamIds[g.id]?.let(r.genres::get)?.let { g.copy(genres = it) } ?: g } }
            store.save(_games.value)
        }
        status.value = null
        return !r.failed
    }

    /**
     * Repaso de géneros, segunda parte (lenta, al final): los juegos de Epic que tenían los géneros de la ficha de
     * Steam (o de versiones anteriores) pasan a los de las etiquetas. El emparejado con Steam se guarda en el juego.
     */
    private suspend fun genrePassEpic(): Boolean {
        val epic = _games.value.filter { !it.userEdited && it.source == Source.EPIC && it.genres.isNotEmpty() && !it.steamChecked }
        var ok = true
        epic.forEachIndexed { i, g ->
            status.value = L10n.s(R.string.status_genres, i + 1, epic.size)
            val r = runCatching { com.repisa.app.sources.SteamSource.searchByName(g.title) }
            if (r.isFailure) { ok = false; return@forEachIndexed }
            val m = r.getOrNull()
            mutex.withLock {
                _games.update { list -> list.map {
                    if (it.id != g.id) it
                    else it.copy(steamMatch = m?.appId, steamChecked = true, genres = m?.genres?.ifEmpty { null } ?: it.genres)
                } }
                if (i % 20 == 19) store.save(_games.value)
            }
            kotlinx.coroutines.delay(250)
        }
        mutex.withLock { store.save(_games.value) }
        status.value = null
        return ok
    }

    /** Ajustes → «Buscar géneros que faltan»: vuelve a buscar ya los juegos sin género. */
    fun findMissingGenres() {
        if (enrichJob?.isActive == true) return
        viewModelScope.launch {
            mutex.withLock { _games.update { list -> list.map { if (it.genres.isEmpty()) it.copy(factsFetched = false, steamChecked = false, idChecked = false) else it } } }
            val before = _games.value.count { it.genres.isEmpty() }
            startEnrichment()
            enrichJob?.join()
            val after = _games.value.count { it.genres.isEmpty() }
            // Los que no se pudieron consultar (Steam o Wikidata pidieron esperar) se reintentan solos más tarde.
            val retry = _games.value.count { it.genres.isEmpty() && !it.factsFetched }
            message.value = L10n.s(R.string.msg_genres_found, before - after, before) +
                if (retry > 0) " " + L10n.s(R.string.msg_genres_retry, retry) else ""
        }
    }

    private companion object {
        val MY_COLLECTION get() = L10n.s(R.string.shelf_my_collection)
        /** Súbelo cuando se añada una fuente de carátulas nueva para que se repasen los juegos sin carátula. */
        const val COVER_PASS = 2
        /** Súbelo cuando mejore la búsqueda de género/estudio/año para que se repasen los juegos sin esos datos. */
        const val FACTS_PASS = 2
        /** Súbelo cuando cambie cómo se leen los logros, para volver a pedirlos. */
        const val ACH_PASS = 2
        /** Súbelo cuando cambie cómo se deciden los géneros, para repasar los de toda la biblioteca. */
        const val GENRE_PASS = 2
    }

    private suspend fun replace(game: Game, save: Boolean = true) = mutex.withLock {
        _games.update { list -> list.map { if (it.id == game.id) mergeEnriched(it, game) else it } }
        if (focused.value?.id == game.id) focused.value = _games.value.firstOrNull { it.id == game.id }
        if (save) store.save(_games.value)
    }

    /** Aplica el resultado de metadatos sin pisar cambios del usuario hechos mientras tanto. */
    private fun mergeEnriched(current: Game, enriched: Game) =
        if (current.userEdited) current.copy(
            metaFetched = true, factsFetched = enriched.factsFetched,
            genres = current.genres.ifEmpty { enriched.genres },
            wikidataId = current.wikidataId ?: enriched.wikidataId,
            altTitles = current.altTitles.ifEmpty { enriched.altTitles }, idChecked = current.idChecked || enriched.idChecked,
            steamMatch = current.steamMatch ?: enriched.steamMatch, steamChecked = current.steamChecked || enriched.steamChecked,
        )
        else enriched.copy(
            favorite = current.favorite,
            trackedMinutes = current.trackedMinutes,
            manualMinutes = current.manualMinutes,
            notes = current.notes,
            status = current.status,
            collections = current.collections,
            achUnlocked = current.achUnlocked, achTotal = current.achTotal,
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
            val clean = game.stored()
            _games.update { list -> list.map { if (it.id == game.id) clean else it } }
            store.save(_games.value)
        }
        if (focused.value?.id == game.id) focused.value = game
    }

    private var aspectSaveJob: Job? = null

    /** La carátula ya cargó: guarda su proporción para que la caja tenga su forma exacta. */
    private val pendingAspects = java.util.concurrent.ConcurrentHashMap<String, Float>()

    /**
     * Las carátulas cargan de una en una; aplicar cada proporción por separado reconstruiría la biblioteca
     * cientos de veces seguidas (tirones). Se acumulan y se aplican juntas cada ~1 s.
     */
    fun setCoverAspect(id: String, aspect: Float) {
        pendingAspects[id] = aspect
        if (aspectSaveJob?.isActive == true) return
        aspectSaveJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val batch = HashMap(pendingAspects)
                if (batch.isEmpty()) break
                batch.keys.forEach { pendingAspects.remove(it) }
                mutex.withLock {
                    _games.update { list -> list.map { g -> batch[g.id]?.let { g.copy(coverAspect = it) } ?: g } }
                }
            }
            kotlinx.coroutines.delay(2000)
            mutex.withLock { store.save(_games.value) }
        }
    }

    fun toggleFavorite(game: Game) = updateGame(game.copy(favorite = !game.favorite))

    fun setStatus(game: Game, status: com.repisa.app.model.PlayStatus?) = updateGame(game.copy(status = status?.name))

    // ------------------------------------------------------------------ Copias del mismo juego

    /** Todas las copias de la caja [game] (ella primero). */
    fun copiesOf(game: Game): List<Game> {
        val all = _games.value.associateBy { it.id }
        return listOfNotNull(all[game.id]) + game.copyIds.mapNotNull { all[it] }
    }

    /** Elige qué copia representa al grupo (su carátula, datos y botón de jugar/tienda). */
    fun setPreferredCopy(group: List<Game>, chosen: Game) = viewModelScope.launch {
        val ids = group.map { it.id }.toSet()
        // La nota y el favorito son del juego, no de la copia: pasan a la nueva preferente.
        val rating = group.firstNotNullOfOrNull { it.rating }
        val favorite = group.any { it.favorite }
        mutex.withLock {
            _games.update { list -> list.map {
                when {
                    it.id == chosen.id -> it.copy(preferredCopy = true, rating = it.rating ?: rating, favorite = favorite)
                    it.id in ids -> it.copy(preferredCopy = false)
                    else -> it
                }
            } }
            store.save(_games.value)
        }
        selectedId.value = chosen.id
    }

    /** Separa una copia del grupo para que vuelva a tener su propia caja. */
    fun splitCopy(copy: Game) = viewModelScope.launch {
        mutex.withLock {
            _games.update { list -> list.map { if (it.id == copy.id) it.copy(noMerge = true, preferredCopy = false) else it } }
            store.save(_games.value)
        }
    }

    /** Juego que se está jugando con el panel de la pantalla de abajo abierto (null = ninguno). */
    val playing = MutableStateFlow<String?>(null)

    private var blurJob: Job? = null

    /** Carátula enfocada: la pantalla de abajo muestra su ficha. */
    fun focusGame(g: Game) { blurJob?.cancel(); focused.value = g }

    /**
     * Sin carátula enfocada (el mando fue a la barra de arriba, a los filtros...): la pantalla de abajo vuelve al
     * resumen de la biblioteca. Se espera un momento por si el foco solo está pasando a otra carátula.
     */
    fun blurGame(g: Game) {
        blurJob?.cancel()
        blurJob = viewModelScope.launch { kotlinx.coroutines.delay(150); if (focused.value?.id == g.id) focused.value = null }
    }

    /** Animación al jugar en curso (o vista previa desde Ajustes, que no abre ningún juego). */
    data class LaunchRequest(val game: Game, val kind: LaunchKind, val preview: Boolean = false, val launched: Boolean = false)
    val launching = MutableStateFlow<LaunchRequest?>(null)

    /** «Jugar»: primero la animación (si el juego tiene y toca según el ajuste) y luego se abre. */
    fun play(game: Game) {
        val kind = LaunchKind.of(game)
        val today = java.time.LocalDate.now().toString()
        val due = when (settings.launchAnimation) {
            "never" -> false
            "daily" -> settings.lastLaunchAnimationDay != today
            else -> true
        }
        if (kind != null && due && launcher.canLaunch(game)) {
            launching.value = LaunchRequest(game, kind)
            return
        }
        playNow(game)
    }

    /** Termina (o se salta) la animación: abre el juego, salvo en la vista previa. */
    fun finishLaunchAnimation() {
        val r = launching.value ?: return
        if (r.preview) { launching.value = null; return }
        if (r.launched) return
        settings.lastLaunchAnimationDay = java.time.LocalDate.now().toString()
        // El negro del final se queda puesto hasta que el juego tapa Repisa (así no se ve Repisa un instante).
        if (!playNow(r.game)) { launching.value = null; return }
        val done = r.copy(launched = true)
        launching.value = done
        viewModelScope.launch { kotlinx.coroutines.delay(10_000); if (launching.value === done) launching.value = null }
    }

    /** Vista previa de una animación desde Ajustes, con un juego de la biblioteca de ese tipo. */
    /** Vista previa de la animación de una consola (Ajustes → «Verla»). */
    fun previewConsole(consoleId: String) {
        val kind = LaunchKind.of(Game(id = "preview", source = if (consoleId == "pc") Source.STEAM else Source.EMULATOR, title = "", consoleId = consoleId))
            ?: return
        previewLaunch(kind, consoleId.takeIf { it != "pc" })
    }

    fun previewLaunch(kind: LaunchKind, consoleId: String? = null) {
        // Con un juego tuyo de esa consola (con carátula si puede ser); si no tienes ninguno, uno de prueba.
        val game = consoleId?.let { id ->
            displayGames.value.filter { it.consoleId == id }.maxByOrNull { if (it.coverUrl != null) 1 else 0 }
                ?: Game(id = "preview", source = Source.EMULATOR, title = "Repisa", consoleId = id)
        }
            ?: displayGames.value.filter { LaunchKind.of(it) == kind }.maxByOrNull { if (it.coverUrl != null) 1 else 0 }
            ?: Game(id = "preview", source = if (kind == LaunchKind.PC) Source.STEAM else Source.EMULATOR, title = "Repisa",
                consoleId = when (kind) {
                    LaunchKind.PC -> "pc"; LaunchKind.SWITCH -> "switch"; LaunchKind.PS2 -> "ps2"
                    LaunchKind.HANDHELD -> "3ds"; LaunchKind.GAMECUBE -> "gc"; LaunchKind.WII -> "wii"
                    LaunchKind.PS1 -> "psx"; LaunchKind.PS3 -> "ps3"; LaunchKind.XBOX -> "xbox"; LaunchKind.CART -> "snes"
                    LaunchKind.ARCADE -> "arcade"; LaunchKind.COMPUTER -> "dos"
                })
        launching.value = LaunchRequest(game, kind, preview = true)
    }

    /** Libera la pantalla de abajo (la pone MainActivity): los juegos de DS y 3DS la usan como segunda pantalla. */
    var releaseSecondScreen: (() -> Unit)? = null

    /** Abre el juego. Devuelve false si no se pudo (el motivo va en [message]). */
    private fun playNow(game: Game): Boolean {
        if (game.consoleId in setOf("nds", "3ds")) releaseSecondScreen?.invoke()
        val app = getApplication<android.app.Application>()
        // Si estaba pendiente, al jugarlo pasa a «Jugando».
        if (game.playStatus == com.repisa.app.model.PlayStatus.BACKLOG) setStatus(game, com.repisa.app.model.PlayStatus.PLAYING)
        // DS y 3DS usan las dos pantallas: para ellos no hay panel.
        val companion = settings.companionPanel && game.consoleId !in setOf("nds", "3ds")
        val bottom = if (companion) secondDisplayId(app) else null
        if (bottom == null) {
            launcher.launch(game)?.let { message.value = it; return false }
            return true
        }
        // Panel en la pantalla de abajo y el juego en la de arriba: las dos siguen activas a la vez.
        playing.value = game.id
        runCatching {
            app.startActivity(
                android.content.Intent(app, com.repisa.app.CompanionActivity::class.java)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                        android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION),
                // Sin transición: el panel aparece quieto en la pantalla de abajo (antes entraba deslizándose).
                android.app.ActivityOptions.makeCustomAnimation(app, 0, 0).setLaunchDisplayId(bottom).toBundle(),
            )
        }
        launcher.launch(game, android.view.Display.DEFAULT_DISPLAY)?.let { message.value = it; playing.value = null; return false }
        return true
    }

    /** La pantalla secundaria (la de abajo en la AYN Thor), si hay. */
    private fun secondDisplayId(context: android.content.Context): Int? {
        val dm = context.getSystemService(android.content.Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
        val candidates = dm.getDisplays(android.hardware.display.DisplayManager.DISPLAY_CATEGORY_PRESENTATION).toList() + dm.displays.toList()
        return candidates.firstOrNull { it.displayId != android.view.Display.DEFAULT_DISPLAY && it.state != android.view.Display.STATE_OFF }?.displayId
    }

    fun onResume() = viewModelScope.launch {
        playing.value = null // de vuelta en Repisa: se cierra el panel de la partida
        if (launching.value?.launched == true) launching.value = null
        // Versión nueva: se busca también al volver a Repisa; y si se acaba de dar el permiso de instalar, sigue sola.
        if (installAfterPermission && update.value != null &&
            com.repisa.app.data.Updater.canInstall(getApplication<android.app.Application>())) installUpdate()
        else checkForUpdate(quiet = true)
        // Si Android cerró Repisa mientras jugabas, hay que esperar a cargar la biblioteca
        // antes de anotar la sesión; si no, se perdería.
        libraryLoaded.await()
        launcher.consumeSession()?.let { s ->
            val g = _games.value.firstOrNull { it.id == s.gameId } ?: return@let
            updateGame(g.copy(trackedMinutes = g.trackedMinutes + s.minutes, lastPlayed = System.currentTimeMillis()))
            if (s.minutes > 0) addSessions(listOf(com.repisa.app.data.PlaySession(g.id, g.title, g.consoleId,
                System.currentTimeMillis() - s.minutes * 60_000L, s.minutes, s.measured)))
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
        // Con datos: se refresca cada 2 semanas. Si faltaba algo, se reintenta a los 2 días.
        val complete = (game.metascore != null || game.userScore != null) && game.hltbMain != null
        val maxAge = if (complete) 14L * 86_400_000 else 2L * 86_400_000
        val fresh = game.extFetchedAt?.let { System.currentTimeMillis() - it < maxAge } == true
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
    fun setCustomCover(game: Game, url: String, aspect: Float? = null) =
        updateGame(game.copy(coverUrl = url, coverAspect = aspect, customCover = true))

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
                url == null || (url.startsWith("http") && com.repisa.app.data.Http.definitelyMissing(url))
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
        mutex.withLock { removeGamesLocked { it.source == Source.EPIC } }
    }

    fun removeSource(source: Source) = viewModelScope.launch {
        mutex.withLock { removeGamesLocked { it.source == source } }
    }

    fun consolesInLibrary(): List<String> = _games.value.filter { it.source == Source.EMULATOR }
        .map { it.consoleId }.distinct().sortedBy { Consoles.byId(it).year }

    // ------------------------------------------------------------------ Temas, partidas, resumen del año, salvapantallas

    fun setTheme(t: ShelfTheme) { ShelfTheme.current = t; settings.shelfTheme = t.name }

    fun setCustomTheme(c: CustomTheme) {
        settings.customTheme = c.encode()
        ShelfTheme.custom = c.build()
        setTheme(ShelfTheme.custom)
    }

    private fun addSessions(new: List<com.repisa.app.data.PlaySession>) {
        sessions.update { (it + new).sortedBy { s -> s.start } }
        val snapshot = sessions.value
        viewModelScope.launch { sessionLog.save(snapshot) }
    }

    /**
     * Partidas anotadas más, de cada juego, el último día que se jugó según Steam, Epic o el emulador (si ese día
     * no hay ya una partida anotada): así el calendario muestra también lo jugado antes de anotar partidas.
     */
    fun withLastPlayed(real: List<com.repisa.app.data.PlaySession>, games: List<Game>): List<com.repisa.app.data.PlaySession> {
        val known = real.map { it.gameId to com.repisa.app.data.dayKey(it.start) }.toSet()
        val extra = games.mapNotNull { g ->
            val t = g.lastPlayed?.takeIf { it > 0 } ?: return@mapNotNull null
            if (g.id to com.repisa.app.data.dayKey(t) in known) null
            else com.repisa.app.data.PlaySession(g.id, g.title, g.consoleId, t, 0, measured = false, lastOnly = true)
        }
        return (real + extra).sortedBy { it.start }
    }

    /** Partidas de ejemplo con tus juegos (para ver el calendario y el resumen del año sin haber jugado aún). */
    fun sampleSessions() = com.repisa.app.data.SampleSessions.make(displayGames.value)

    /** Resumen del año abierto: null = cerrado; true = con datos de ejemplo. */
    val wrapped = MutableStateFlow<Boolean?>(null)
    /** Calendario de partidas abierto: null = cerrado; true = con datos de ejemplo. */
    val calendar = MutableStateFlow<Boolean?>(null)
    /** Botones del mando para el calendario (L1/R1 y recuperar el foco), enviados desde la actividad. */
    val calendarKeys = kotlinx.coroutines.flow.MutableSharedFlow<Int>(extraBufferCapacity = 8)
    /** true si algo del calendario tiene el foco del mando (si no, la cruceta se lo devuelve). */
    @Volatile var calendarFocused = false
    /** Joystick derecho (-1..1, 0 en reposo): desplaza la lista de partidas del día. */
    val rightStick = MutableStateFlow(0f)
    val screensaver = MutableStateFlow(false)
    /** Solo pruebas: submenú de Ajustes que se abre directamente. */
    var debugPage: String? = null
    @Volatile var lastInteraction = System.currentTimeMillis()
    fun poke() { lastInteraction = System.currentTimeMillis() }

    // ------------------------------------------------------------------ Lista de deseos de Steam

    /** Resultado de la última consulta de la lista de deseos (se muestra en Ajustes). */
    val wishStatus = MutableStateFlow<String?>(null)

    fun setWishlistShelf(on: Boolean) {
        settings.wishlistShelf = on; wishlistOn.value = on
        com.repisa.app.data.WishlistJob.schedule(getApplication())
        if (on && wishlist.value.isEmpty()) viewModelScope.launch { refreshWishlist(quiet = false) }
    }

    /** Descarga la lista de deseos, avisa de lo que ha bajado de precio y la guarda. */
    suspend fun refreshWishlist(quiet: Boolean) {
        try {
            val (fresh, country) = com.repisa.app.data.WishlistCheck.run(getApplication(), settings)
            wishlist.value = fresh
            val now = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date())
            wishStatus.value = (if (fresh.isEmpty()) L10n.s(R.string.wish_empty) else L10n.s(R.string.wish_loaded, fresh.size)) +
                " · " + L10n.s(R.string.wish_checked, now, country)
            if (!quiet) message.value = if (fresh.isEmpty()) L10n.s(R.string.wish_empty) else L10n.s(R.string.wish_loaded, fresh.size)
        } catch (e: Exception) {
            wishStatus.value = L10n.s(R.string.wish_error, e.message ?: e.javaClass.simpleName)
            if (!quiet) message.value = wishStatus.value
        }
    }


    // Al final de la clase: aquí ya existen la biblioteca, las partidas y la lista de deseos.
    init {
        viewModelScope.launch {
            libraryLoaded.await()
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            combine(displayGames, sessions, wishlist, collections) { _, _, _, _ -> Unit }
                .debounce(1500)
                .collect { runCatching { recomputeAchievements() } }
        }
    }
}

/** Claves con las que se guardan las fechas de platinos y logros de la app. */
private const val PLAT_KEY = "__platinum__"
private const val APP_KEY = "__app__"

/** Caja de la lista de deseos: un juego de Steam que no está en la biblioteca (id «wish:<appid>»). */
fun com.repisa.app.sources.WishItem.toGame() = Game(
    id = "wish:$appId", source = Source.STEAM, title = name, consoleId = "pc", coverUrl = cover,
    storeUrl = "https://store.steampowered.com/app/$appId", status = com.repisa.app.model.PlayStatus.NOT_APPLICABLE.name,
)

val Game.isWish: Boolean get() = id.startsWith("wish:")
