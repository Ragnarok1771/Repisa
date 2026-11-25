package com.repisa.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.repisa.app.R
import com.repisa.app.data.SafTree
import com.repisa.app.i18n.AppLanguage
import com.repisa.app.i18n.L10n
import com.repisa.app.launch.AppInfo
import com.repisa.app.model.Consoles
import com.repisa.app.model.label
import com.repisa.app.model.Source
import com.repisa.app.sources.EpicSource
import com.repisa.app.sources.SteamLogin
import org.json.JSONObject

private enum class Page(val titleRes: Int, val subRes: Int?, val emoji: String) {
    EMULATORS(R.string.menu_emulators, R.string.menu_emulators_sub, "🎮"),
    COVERS(R.string.menu_covers, R.string.menu_covers_sub, "🖼"),
    PLAYTIME(R.string.menu_playtime, R.string.menu_playtime_sub, "⏰"),
    DISPLAY(R.string.menu_display, R.string.menu_display_sub, "📺"),
    THEMES(R.string.menu_themes, R.string.menu_themes_sub, "🎨"),
    COLLECTIONS(R.string.menu_collections, R.string.menu_collections_sub, "📂"),
    LANGUAGE(R.string.menu_language, null, "🌐"),
    DATA(R.string.menu_data, R.string.menu_data_sub, "💾"),
}

/**
 * Ajustes: Steam y Epic en la página principal y el resto agrupado en submenús.
 * [onLanguageChanged] recrea la pantalla con el idioma nuevo.
 */
@Composable
fun SettingsScreen(vm: LibraryViewModel, onBack: () -> Unit, onLanguageChanged: () -> Unit) {
    var page by rememberSaveable { mutableStateOf<Page?>(null) }
    // Al volver de un submenú, la página principal sigue donde estaba y el foco vuelve a ese submenú.
    var lastPage by rememberSaveable { mutableStateOf<Page?>(null) }
    val mainScroll = rememberSaveable(saver = androidx.compose.foundation.ScrollState.Saver) { androidx.compose.foundation.ScrollState(0) }
    // Se incrementa al cambiar un ajuste: las páginas lo leen para releer preferencias SIN reiniciar
    // el desplazamiento ni los campos que el usuario está escribiendo.
    var tick by remember { mutableIntStateOf(0) }
    val changed: () -> Unit = { tick++ }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    BackHandler(enabled = page != null) { page = null }
    LaunchedEffect(Unit) { vm.debugPage?.let { p -> page = Page.entries.firstOrNull { it.name == p }; vm.debugPage = null } }

    Column(Modifier.fillMaxSize().roomBackground()) {
        Row(Modifier.fillMaxWidth().background(Color(0xCC120C09)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (page != null) page = null else onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
            }
            Text(
                page?.let { stringResource(it.titleRes) } ?: stringResource(R.string.settings),
                color = Color(0xFFFFD27A), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, fontFamily = DisplayFont,
            )
            Spacer(Modifier.weight(1f))
            if (page == null) Button(onClick = { vm.syncAll(); onBack() }) { Text(stringResource(R.string.sync_all)) }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(if (page == null) mainScroll else pageScrollState(page)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (page) {
                null -> MainPage(vm, tick, changed, lastPage) { lastPage = it; page = it }
                Page.EMULATORS -> EmulatorsPage(vm, tick, changed)
                Page.COVERS -> CoversPage(vm, tick, changed, onBack)
                Page.PLAYTIME -> PlaytimePage(vm, tick)
                Page.DISPLAY -> DisplayPage(vm, tick, changed)
                Page.THEMES -> ThemesPage(vm)
                Page.DATA -> DataPage(vm)
                Page.COLLECTIONS -> CollectionsPage(vm)
                Page.LANGUAGE -> LanguageList(L10n.current) { lang ->
                    L10n.setLanguage(vm.getApplication<android.app.Application>(), lang); onLanguageChanged()
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** Un desplazamiento distinto por submenú (al entrar en uno se empieza arriba). */
@Composable
private fun pageScrollState(page: Any?): androidx.compose.foundation.ScrollState =
    androidx.compose.runtime.key(page) { rememberScrollState() }

// ---------------------------------------------------------------------------- Página principal

@Composable
private fun MainPage(vm: LibraryViewModel, tick: Int, changed: () -> Unit, returnTo: Page?, open: (Page) -> Unit) {
    val s = vm.settings
    @Suppress("UNUSED_VARIABLE") val t = tick
    var epicWeb by remember { mutableStateOf(false) }
    var steamWeb by remember { mutableStateOf(false) }

    Section(stringResource(R.string.source_steam)) {
        if (s.steamLinkedId != null || s.steamApiKey != null) {
            Hint(s.steamLinkedName?.let { stringResource(R.string.steam_linked, it) }
                ?: s.steamLinkedId?.let { stringResource(R.string.steam_linked, "SteamID $it") }
                ?: stringResource(R.string.steam_linked_key))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.syncOne(Source.STEAM) }) { Text(stringResource(R.string.sync)) }
                OutlinedButton(onClick = { vm.steamUnlink(); changed() }) { Text(stringResource(R.string.unlink), color = Color.White) }
            }
        } else {
            Hint(stringResource(R.string.steam_link_hint))
            Button(onClick = { steamWeb = true }) { Text(stringResource(R.string.steam_link)) }
            var advanced by remember { mutableStateOf(false) }
            TextButton(onClick = { advanced = !advanced }) {
                Text(stringResource(if (advanced) R.string.steam_advanced_hide else R.string.steam_advanced_show))
            }
            if (advanced) {
                Hint(stringResource(R.string.steam_key_hint))
                TextButton(onClick = { vm.launcher.openUrl("https://steamcommunity.com/dev/apikey") }) {
                    Text(stringResource(R.string.steam_get_key))
                }
                var key by remember { mutableStateOf(s.steamApiKey.orEmpty()) }
                var user by remember { mutableStateOf(s.steamUser.orEmpty()) }
                Field(stringResource(R.string.steam_key), key, secret = true) { key = it }
                Field(stringResource(R.string.steam_user), user) { user = it }
                Button(onClick = { s.steamApiKey = key; s.steamUser = user; vm.syncOne(Source.STEAM); changed() },
                    enabled = key.isNotBlank() && user.isNotBlank()) { Text(stringResource(R.string.save_and_sync)) }
            }
        }
    }

    Section(stringResource(R.string.source_epic)) {
        if (vm.epic.isLoggedIn) {
            Hint(s.epicDisplayName?.let { stringResource(R.string.epic_connected, it) } ?: stringResource(R.string.epic_connected_plain))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.syncOne(Source.EPIC) }) { Text(stringResource(R.string.sync)) }
                OutlinedButton(onClick = { vm.epicLogout(); changed() }) { Text(stringResource(R.string.sign_out), color = Color.White) }
            }
        } else {
            Hint(stringResource(R.string.epic_link_hint))
            Button(onClick = { epicWeb = true }) { Text(stringResource(R.string.epic_link)) }
            var other by remember { mutableStateOf(false) }
            TextButton(onClick = { other = !other }) {
                Text(stringResource(if (other) R.string.other_methods_hide else R.string.other_methods_show))
            }
            if (other) {
                Hint(stringResource(R.string.epic_code_hint))
                OutlinedButton(onClick = { vm.epicStartLink() }) { Text(stringResource(R.string.epic_link_code), color = Color.White) }
                Hint(stringResource(R.string.epic_browser_hint))
                TextButton(onClick = { vm.launcher.openUrl(EpicSource.LOGIN_URL) }) { Text(stringResource(R.string.epic_open_browser)) }
                var code by remember { mutableStateOf("") }
                Field(stringResource(R.string.epic_auth_code), code) { code = it }
                Button(onClick = { vm.epicLogin(code); code = "" }, enabled = code.isNotBlank()) { Text(stringResource(R.string.use_code)) }
            }
        }
    }

    // RetroAchievements: logros de juegos de emulador (GB, GBA, DS, N64, SNES, PS1, PS2, PSP, GameCube...)
    // Igual que Steam y Epic: los campos solo salen en una ventana al vincular (con el mando, pasar por aquí
    // ya no abre el teclado).
    Section("RetroAchievements") {
        var linking by remember { mutableStateOf(false) }
        if (s.raUser != null && s.raApiKey != null) {
            Hint(stringResource(R.string.ra_linked, s.raUser.orEmpty()))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.checkRetroAchievements() }) { Text(stringResource(R.string.sync)) }
                OutlinedButton(onClick = { s.raUser = null; s.raApiKey = null; changed() }) {
                    Text(stringResource(R.string.unlink), color = Color.White)
                }
            }
        } else {
            Hint(stringResource(R.string.ra_hint))
            Button(onClick = { linking = true }) { Text(stringResource(R.string.ra_link)) }
        }
        if (linking) {
            var user by remember { mutableStateOf(s.raUser.orEmpty()) }
            var key by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { linking = false },
                title = { Text("RetroAchievements") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.ra_hint), fontSize = 13.sp)
                        OutlinedTextField(user, { user = it }, label = { Text(stringResource(R.string.ra_user)) }, singleLine = true)
                        OutlinedTextField(key, { key = it }, label = { Text(stringResource(R.string.ra_key)) }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        s.raUser = user; s.raApiKey = key; linking = false; changed(); vm.checkRetroAchievements()
                    }, enabled = user.isNotBlank() && key.isNotBlank()) { Text(stringResource(R.string.save)) }
                },
                dismissButton = { TextButton(onClick = { linking = false }) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }

    // Submenús
    Column(Modifier.fillMaxWidth().background(Color(0xB3120C09), RoundedCornerShape(12.dp))) {
        Page.entries.forEachIndexed { i, p ->
            if (i > 0) HorizontalDivider(color = Color(0x22FFFFFF))
            val back = remember { androidx.compose.ui.focus.FocusRequester() }
            if (p == returnTo) LaunchedEffect(Unit) { runCatching { back.requestFocus() } }
            Row(
                Modifier.fillMaxWidth().focusRequester(back).clickable { open(p) }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(p.emoji, fontSize = 22.sp, modifier = Modifier.width(40.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(p.titleRes), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    val sub = p.subRes?.let { stringResource(it) }
                        ?: L10n.current?.let { "${it.flag} ${it.nativeName}" }
                    sub?.let { Text(it, color = Color(0xFFBFA98A), fontSize = 12.sp) }
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFFBFA98A))
            }
        }
    }

    // Abajo del todo: buscar actualización a mano (por si el aviso no salió solo).
    Row(Modifier.fillMaxWidth().background(Color(0xB3120C09), RoundedCornerShape(12.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("⬆ " + stringResource(R.string.updates_title), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("Repisa v${com.repisa.app.BuildConfig.VERSION_NAME}", color = Color(0xFFBFA98A), fontSize = 12.sp)
        }
        Button(onClick = { vm.checkForUpdate(quiet = false) }) { Text(stringResource(R.string.updates_check_now)) }
    }

    if (epicWeb) {
        LoginWebDialog(stringResource(R.string.epic_link), EpicSource.LOGIN_URL, onDismiss = { epicWeb = false }) { view, url, done ->
            // Al terminar, Epic muestra un JSON con "authorizationCode".
            if (!url.contains("/id/api/redirect")) return@LoginWebDialog
            view.evaluateJavascript("document.body.innerText") { raw ->
                val text = runCatching { JSONObject("{\"v\":$raw}").getString("v") }.getOrDefault(raw)
                val code = runCatching { JSONObject(text).optString("authorizationCode") }.getOrNull()
                if (!code.isNullOrBlank()) { done(); epicWeb = false; vm.epicLogin(code) }
            }
        }
    }
    val epicLink by vm.epicLink.collectAsState()
    epicLink?.let { state ->
        EpicLinkDialog(state, onOpen = { vm.launcher.openUrl(it) }, onRetry = { vm.epicStartLink() },
            onDismiss = { vm.epicCancelLink(); changed() })
    }
    if (steamWeb) {
        LoginWebDialog(stringResource(R.string.steam_link), SteamLogin.LOGIN_URL, onDismiss = { steamWeb = false }) { _, url, done ->
            // Solo cuando la sesión es buena de verdad: una cookie vieja y caducada no cuenta (si no, el diálogo se
            // cerraba solo antes de poder iniciar sesión y salía «Steam no confirmó la sesión»).
            if (!url.contains("/login") && SteamLogin.hasFreshWebSession()) { done(); steamWeb = false; vm.steamLinked(); changed() }
        }
    }
}

// ---------------------------------------------------------------------------- Colecciones

/**
 * Colecciones propias: cada una con sus juegos, para añadir o quitar juegos, renombrarla o borrarla.
 * Borrar una colección no borra sus juegos.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CollectionsPage(vm: LibraryViewModel) {
    val collections by vm.collections.collectAsState()
    val games by vm.displayGames.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<GameCollection?>(null) }
    var deleting by remember { mutableStateOf<GameCollection?>(null) }
    var picking by remember { mutableStateOf<GameCollection?>(null) }
    Section(stringResource(R.string.menu_collections)) {
        Hint(stringResource(R.string.collections_hint))
        Button(onClick = { creating = true }) { Text("＋ " + stringResource(R.string.collection_new)) }
        if (collections.isEmpty()) Hint(stringResource(R.string.collections_empty))
    }
    collections.forEach { c ->
        val inside = games.filter { c.id in it.collections }.sortedBy { it.sortKey }
        Section("📂 " + c.name + "  ·  " + L10n.games(inside.size)) {
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { picking = c }) { Text("＋ " + stringResource(R.string.collection_add_games)) }
                OutlinedButton(onClick = { renaming = c }) { Text("✎ " + stringResource(R.string.collection_rename), color = Color.White) }
                OutlinedButton(onClick = { deleting = c }) { Text("🗑 " + stringResource(R.string.delete), color = Color(0xFFFF8A80)) }
            }
            if (inside.isEmpty()) Hint(stringResource(R.string.collection_no_games))
            inside.forEach { g ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(g.title, color = Color(0xFFE8D9C0), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                    Text(g.console.label, color = Color(0xFF9C8A70), fontSize = 12.sp)
                    TextButton(onClick = { vm.removeFromCollection(setOf(g.id), c.id) }) { Text("✕ " + stringResource(R.string.remove)) }
                }
            }
        }
    }
    if (creating) CollectionNameDialog(onDismiss = { creating = false }) { vm.createCollection(it); creating = false }
    renaming?.let { c -> CollectionNameDialog(c.name, onDismiss = { renaming = null }) { vm.renameCollection(c.id, it); renaming = null } }
    deleting?.let { c ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            text = { Text(stringResource(R.string.collection_delete_confirm, c.name)) },
            confirmButton = { TextButton(onClick = { vm.deleteCollection(c.id); deleting = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    picking?.let { c -> CollectionGamesDialog(c, games, vm, onDismiss = { picking = null }) }
}

/** Elegir con casillas qué juegos van en la colección (con buscador). */
@Composable
private fun CollectionGamesDialog(c: GameCollection, games: List<com.repisa.app.model.Game>, vm: LibraryViewModel, onDismiss: () -> Unit) {
    val before = remember(c.id) { games.filter { c.id in it.collections }.map { it.id }.toSet() }
    var chosen by remember(c.id) { mutableStateOf(before) }
    var query by remember { mutableStateOf("") }
    val list = remember(games, query) {
        val q = query.trim().lowercase()
        games.filter { q.isEmpty() || it.title.lowercase().contains(q) }
            .sortedWith(compareByDescending<com.repisa.app.model.Game> { it.id in before }.thenBy { it.sortKey })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.collection_pick_title, c.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(query, { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search)) })
                androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(list.size, key = { list[it].id }) { i ->
                        val g = list[i]
                        val on = g.id in chosen
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
                            .clickable { chosen = if (on) chosen - g.id else chosen + g.id }) {
                            androidx.compose.material3.Checkbox(on, { chosen = if (on) chosen - g.id else chosen + g.id })
                            Text(g.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(g.console.label, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                (chosen - before).takeIf { it.isNotEmpty() }?.let { vm.addToCollection(it, c.id) }
                (before - chosen).takeIf { it.isNotEmpty() }?.let { vm.removeFromCollection(it, c.id) }
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ---------------------------------------------------------------------------- Emuladores

@Composable
private fun EmulatorsPage(vm: LibraryViewModel, tick: Int, changed: () -> Unit) {
    val s = vm.settings
    @Suppress("UNUSED_VARIABLE") val t = tick
    val context = LocalContext.current
    fun persist(uri: Uri) = context.contentResolver.takePersistableUriPermission(
        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
    )
    val pickRoms = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { persist(uri); s.romFolders = s.romFolders + uri.toString(); changed(); vm.syncOne(Source.EMULATOR) }
    }
    val pickEsde = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { persist(uri); s.esdeFolder = uri.toString(); changed(); vm.syncOne(Source.EMULATOR) }
    }
    val pickCocoon = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { persist(uri); s.cocoonFolder = uri.toString(); changed(); vm.syncOne(Source.EMULATOR) }
    }
    val pickRetroArch = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { persist(uri); s.retroarchFolder = uri.toString(); changed(); vm.syncOne(Source.EMULATOR) }
    }
    var pickEmuFor by remember { mutableStateOf<String?>(null) }

    Section(stringResource(R.string.menu_emulators)) {
        Hint(stringResource(R.string.folder_deep_hint))
        Hint(stringResource(R.string.rom_hint))
        s.romFolders.forEach { uri -> FolderRow(SafTree.displayName(context, uri)) { s.romFolders = s.romFolders - uri; changed() } }
        OutlinedButton(onClick = { pickRoms.launch(null) }) { Text(stringResource(R.string.add_rom_folder), color = Color.White) }
        HorizontalDivider(color = Color(0x33FFFFFF))
        Hint(stringResource(R.string.esde_hint))
        s.esdeFolder?.let { FolderRow(SafTree.displayName(context, it)) { s.esdeFolder = null; changed() } }
            ?: OutlinedButton(onClick = { pickEsde.launch(null) }) { Text(stringResource(R.string.choose_esde), color = Color.White) }
        Hint(stringResource(R.string.ra_hint))
        s.retroarchFolder?.let { FolderRow(SafTree.displayName(context, it)) { s.retroarchFolder = null; changed() } }
            ?: OutlinedButton(onClick = { pickRetroArch.launch(null) }) { Text(stringResource(R.string.choose_ra), color = Color.White) }
        Hint(stringResource(R.string.cocoon_hint))
        s.cocoonFolder?.let { FolderRow(SafTree.displayName(context, it)) { s.cocoonFolder = null; changed() } }
            ?: OutlinedButton(onClick = { pickCocoon.launch(null) }) { Text(stringResource(R.string.choose_cocoon), color = Color.White) }
        if (s.cocoonFolder != null || s.esdeFolder != null) FrontendReportText(vm.frontendReport.collectAsState().value)
        Button(onClick = { vm.syncOne(Source.EMULATOR) }) { Text(stringResource(R.string.scan_emulators)) }
    }

    val consoles = vm.consolesInLibrary()
    if (consoles.isNotEmpty()) Section(stringResource(R.string.emu_section)) {
        Hint(stringResource(R.string.emu_per_system_hint))
        consoles.forEach { id ->
            val pkg = s.emulatorFor(id)
            Row(Modifier.fillMaxWidth().clickable { pickEmuFor = id }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(Consoles.byId(id).name, color = Color.White, modifier = Modifier.weight(1f))
                val auto = if (pkg == null) remember(id) { vm.launcher.autoEmulator(id) } else null
                Text(
                    when {
                        pkg != null -> vm.launcher.appLabel(pkg)
                        auto != null -> stringResource(R.string.emu_auto, vm.launcher.appLabel(auto))
                        else -> stringResource(R.string.emu_not_found)
                    },
                    color = if (pkg == null && auto == null) Color(0xFF9C8A70) else Color(0xFFFFD27A),
                )
            }
        }
    }
    pickEmuFor?.let { consoleId ->
        EmulatorPicker(
            title = stringResource(R.string.emulator_for, Consoles.byId(consoleId).name),
            apps = remember { vm.launcher.installedApps() },
            onDismiss = { pickEmuFor = null },
        ) { pkg -> s.setEmulatorFor(consoleId, pkg); pickEmuFor = null; changed() }
    }
}

// ---------------------------------------------------------------------------- Carátulas y datos

@Composable
private fun CoversPage(vm: LibraryViewModel, tick: Int, changed: () -> Unit, onBack: () -> Unit) {
    val s = vm.settings
    @Suppress("UNUSED_VARIABLE") val t = tick
    Section(stringResource(R.string.menu_covers)) {
        Hint(stringResource(R.string.covers_hint))
        Button(onClick = { vm.findMissingCovers(); onBack() }) { Text(stringResource(R.string.find_missing)) }
        HorizontalDivider(color = Color(0x33FFFFFF))
        Hint(stringResource(R.string.sgdb_hint))
        TextButton(onClick = { vm.launcher.openUrl("https://www.steamgriddb.com/profile/preferences/api") }) {
            Text(stringResource(R.string.sgdb_get))
        }
        var sgdb by remember { mutableStateOf(s.steamGridDbKey.orEmpty()) }
        Field(stringResource(R.string.sgdb_key), sgdb, secret = true) { sgdb = it }
        OutlinedButton(onClick = { s.steamGridDbKey = sgdb.ifBlank { null }; changed() }) {
            Text(stringResource(R.string.save_key), color = Color.White)
        }
        HorizontalDivider(color = Color(0x33FFFFFF))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.libretro_toggle), color = Color.White, modifier = Modifier.weight(1f))
            Switch(checked = s.useLibretroCovers, onCheckedChange = { s.useLibretroCovers = it; changed() })
        }
    }
}

// ---------------------------------------------------------------------------- Tiempo de juego

@Composable
private fun PlaytimePage(vm: LibraryViewModel, tick: Int) {
    @Suppress("UNUSED_VARIABLE") val t = tick
    val context = LocalContext.current
    Section(stringResource(R.string.menu_playtime)) {
        Hint(stringResource(R.string.autosync_hint))
        HorizontalDivider(color = Color(0x33FFFFFF))
        val granted = vm.launcher.usage.hasPermission()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (granted) R.string.usage_granted else R.string.usage_denied), color = Color.White)
                Hint(stringResource(R.string.usage_hint))
            }
            if (!granted) OutlinedButton(onClick = { context.startActivity(vm.launcher.usage.permissionIntent()) }) {
                Text(stringResource(R.string.grant), color = Color.White)
            }
        }
        HorizontalDivider(color = Color(0x33FFFFFF))
        // Cómo se interpreta el tiempo escrito a mano en «Editar datos».
        var total by remember { mutableStateOf(vm.settings.manualTimeIsTotal) }
        Text(stringResource(R.string.manual_time_mode), color = Color.White)
        listOf(false to R.string.manual_time_add, true to R.string.manual_time_total).forEach { (value, label) ->
            Row(Modifier.fillMaxWidth().clickable { total = value; vm.settings.manualTimeIsTotal = value }.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.RadioButton(selected = total == value, onClick = { total = value; vm.settings.manualTimeIsTotal = value })
                Text(stringResource(label), color = Color(0xFFE8D9C0), fontSize = 14.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------- Pantalla

@Composable
private fun DisplayPage(vm: LibraryViewModel, tick: Int, changed: () -> Unit) {
    val s = vm.settings
    @Suppress("UNUSED_VARIABLE") val t = tick
    Section(stringResource(R.string.menu_display)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.second_screen), color = Color.White)
                Hint(stringResource(R.string.second_screen_hint))
            }
            Switch(checked = s.secondScreen, onCheckedChange = { s.secondScreen = it; changed() })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.companion_setting), color = Color.White)
                Hint(stringResource(R.string.companion_setting_hint))
            }
            var comp by remember { mutableStateOf(s.companionPanel) }
            Switch(checked = comp, onCheckedChange = { comp = it; s.companionPanel = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.setting_continue_shelf), color = Color.White)
                Hint(stringResource(R.string.shelf_fold_hint))
            }
            var c by remember { mutableStateOf(s.continueShelf) }
            Switch(checked = c, onCheckedChange = { c = it; vm.setContinueShelf(it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.setting_auto_status), color = Color.White)
                Hint(stringResource(R.string.setting_auto_status_hint))
            }
            var c by remember { mutableStateOf(s.autoStatus) }
            Switch(checked = c, onCheckedChange = { c = it; vm.setAutoStatus(it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.setting_status_boxes), color = Color.White, modifier = Modifier.weight(1f))
            var c by remember { mutableStateOf(s.showStatusOnBoxes) }
            Switch(checked = c, onCheckedChange = { c = it; s.showStatusOnBoxes = it })
        }
        HorizontalDivider(color = Color(0x33FFFFFF))
        // El valor se ve mientras se arrastra; se guarda al soltar.
        var h by remember { mutableFloatStateOf(s.shelfHeightDp.toFloat()) }
        Text(stringResource(R.string.box_size, h.toInt()), color = Color.White)
        Hint(stringResource(R.string.box_size_hint))
        Slider(value = h, onValueChange = { h = it }, onValueChangeFinished = { s.shelfHeightDp = h.toInt() },
            valueRange = 90f..240f, modifier = Modifier.widthIn(max = 560.dp))
        // Animación al pulsar «Jugar» (con vista previa de cada tipo).
        HorizontalDivider(color = Color(0x33FFFFFF))
        Text(stringResource(R.string.launch_anim), color = Color.White)
        Hint(stringResource(R.string.launch_anim_hint))
        var animMode by remember { mutableStateOf(s.launchAnimation) }
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("always" to R.string.launch_anim_always, "daily" to R.string.launch_anim_daily, "never" to R.string.launch_anim_never)
                .forEach { (mode, label) ->
                    androidx.compose.material3.FilterChip(animMode == mode, { animMode = mode; s.launchAnimation = mode },
                        { Text(stringResource(label)) })
                }
        }
        // Vista previa de cada animación: sección que se abre y se cierra, con las consolas por marca.
        var showPreviews by rememberSaveable { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { showPreviews = !showPreviews }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text((if (showPreviews) "▾  " else "▸  ") + stringResource(R.string.launch_anim_preview_all), color = Color(0xFFFFD27A),
                fontWeight = FontWeight.SemiBold)
        }
        if (showPreviews) {
            listOf(
                "PC" to listOf("pc" to "PC (Steam / Epic)"),
                "Nintendo" to listOf("nes" to "NES", "snes" to "Super Nintendo", "n64" to "Nintendo 64", "gb" to "Game Boy", "gbc" to "Game Boy Color",
                    "gba" to "Game Boy Advance", "nds" to "DS", "3ds" to "3DS", "gc" to "GameCube", "wii" to "Wii", "wiiu" to "Wii U",
                    "switch" to "Switch", "switch2" to "Switch 2"),
                "Sony" to listOf("psx" to "PlayStation", "ps2" to "PS2", "psp" to "PSP", "ps3" to "PS3", "psvita" to "PS Vita", "ps4" to "PS4", "ps5" to "PS5"),
                "Microsoft" to listOf("xbox" to "Xbox", "xbox360" to "Xbox 360"),
                "Sega" to listOf("mastersystem" to "Master System", "megadrive" to "Mega Drive", "gamegear" to "Game Gear", "saturn" to "Saturn",
                    "dreamcast" to "Dreamcast"),
                "Atari" to listOf("atari5200" to "5200", "atari7800" to "7800", "atarijaguar" to "Jaguar", "atarilynx" to "Lynx"),
                stringResource(R.string.launch_anim_others) to listOf("atari2600" to "Atari 2600", "neogeo" to "Neo Geo", "pcengine" to "PC Engine",
                    "3do" to "3DO", "colecovision" to "ColecoVision", "intellivision" to "Intellivision", "vectrex" to "Vectrex",
                    "cdi" to "CD-i", "pcfx" to "PC-FX", "odyssey2" to "Odyssey 2", "channelf" to "Channel F", "sg1000" to "SG-1000",
                    "ngp" to "Neo Geo Pocket", "wonderswan" to "WonderSwan", "pokemini" to "Pokémon Mini", "virtualboy" to "Virtual Boy",
                    "gameandwatch" to "Game & Watch", "supervision" to "Supervision", "ngage" to "N-Gage",
                    "arcade" to "Arcade", "dos" to "DOS / ordenadores", "scummvm" to "ScummVM / motores"),
            ).forEach { (brand, items) ->
                Text(brand, color = Color(0xFFBFA98A), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.forEach { (id, label) -> OutlinedButton(onClick = { vm.previewConsole(id) }) { Text(label, color = Color.White) } }
                }
            }
        }
        // Orden de las repisas cuando se agrupa por estado.
        HorizontalDivider(color = Color(0x33FFFFFF))
        Text(stringResource(R.string.status_order), color = Color.White)
        Hint(stringResource(R.string.status_order_hint))
        val order = com.repisa.app.model.StatusRules.order
        order.forEachIndexed { i, st ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}.  ${st.icon} ${st.label}", color = Color(st.color), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(enabled = i > 0, onClick = {
                    vm.setStatusOrder(order.toMutableList().apply { add(i - 1, removeAt(i)) })
                }) { Text("▲") }
                TextButton(enabled = i < order.lastIndex, onClick = {
                    vm.setStatusOrder(order.toMutableList().apply { add(i + 1, removeAt(i)) })
                }) { Text("▼") }
            }
        }
        if (order != com.repisa.app.model.StatusRules.DEFAULT_ORDER)
            TextButton(onClick = { vm.setStatusOrder(com.repisa.app.model.StatusRules.DEFAULT_ORDER) }) { Text(stringResource(R.string.status_order_reset)) }
        val hidden by vm.hidden.collectAsState()
        if (hidden.isNotEmpty()) {
            HorizontalDivider(color = Color(0x33FFFFFF))
            Text(stringResource(R.string.hidden_count, hidden.size), color = Color.White)
            OutlinedButton(onClick = { vm.unhideAll() }) { Text(stringResource(R.string.unhide_all), color = Color.White) }
        }
    }

    // Salvapantallas: tiempo sin tocar nada hasta que salta (botones rápidos y barra para cualquier valor).
    Section("🌙 " + stringResource(R.string.screensaver_title)) {
        Hint(stringResource(R.string.screensaver_desc))
        var minutes by remember { mutableIntStateOf(s.screensaverMinutes) }
        fun set(m: Int) { minutes = m; s.screensaverMinutes = m }
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(0, 3, 5, 10, 15, 20).forEach { m ->
                androidx.compose.material3.FilterChip(minutes == m, { set(m) },
                    { Text(if (m == 0) stringResource(R.string.screensaver_never) else stringResource(R.string.screensaver_min, m)) })
            }
        }
        Text(if (minutes == 0) stringResource(R.string.screensaver_off) else stringResource(R.string.screensaver_after, minutes), color = Color.White)
        var slider by remember(minutes) { mutableFloatStateOf(minutes.toFloat()) }
        Slider(value = slider, onValueChange = { slider = it; minutes = it.toInt() }, onValueChangeFinished = { set(slider.toInt()) },
            valueRange = 0f..60f, steps = 59, modifier = Modifier.widthIn(max = 560.dp))
        OutlinedButton(onClick = { vm.screensaver.value = true }) { Text("▶ " + stringResource(R.string.screensaver_try), color = Color.White) }
    }

    // Lista de deseos de Steam: repisa propia con precios y aviso cuando algo baja.
    Section("⭐ " + stringResource(R.string.wish_shelf)) {
        Hint(stringResource(R.string.wish_desc))
        val notifyPermission = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
        val context = androidx.compose.ui.platform.LocalContext.current
        fun askNotifications() {
            if (android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                notifyPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        var on by remember { mutableStateOf(s.wishlistShelf) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.wish_show), color = Color.White, modifier = Modifier.weight(1f))
            Switch(checked = on, onCheckedChange = { on = it; vm.setWishlistShelf(it) })
        }
        var alerts by remember { mutableStateOf(s.wishlistAlerts) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.wish_alerts), color = Color.White)
                Hint(stringResource(R.string.wish_alerts_hint))
            }
            Switch(checked = alerts, onCheckedChange = {
                alerts = it; s.wishlistAlerts = it; if (it) askNotifications(); com.repisa.app.data.WishlistJob.schedule(context)
            })
        }
        var background by remember { mutableStateOf(s.wishlistBackground) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.wish_background), color = if (alerts) Color.White else Color(0xFF9C8A70))
                Hint(stringResource(R.string.wish_background_hint))
            }
            Switch(checked = background, enabled = alerts, onCheckedChange = {
                background = it; s.wishlistBackground = it; com.repisa.app.data.WishlistJob.schedule(context)
            })
        }
        val wish by vm.wishlist.collectAsState()
        val wishStatus by vm.wishStatus.collectAsState()
        // Lo que pasó la última vez que se consultó (cuántos juegos, cuándo, país de la tienda... o el error).
        Hint(wishStatus ?: if (wish.isEmpty()) stringResource(R.string.wish_none_yet) else stringResource(R.string.wish_loaded, wish.size))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = { scope.launch { vm.refreshWishlist(quiet = false) } }, enabled = s.steamConfigured) {
                Text("🔄 " + stringResource(R.string.wish_refresh))
            }
        }
    }
}

// ---------------------------------------------------------------------------- Idioma

/** Lista de idiomas con bandera; táctil y grande. También la usa la pantalla de bienvenida. */
@Composable
fun LanguageList(selected: AppLanguage?, onPick: (AppLanguage) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppLanguage.entries.forEach { lang ->
            val active = lang == selected
            Row(
                Modifier.fillMaxWidth()
                    .background(if (active) Color(0xFF5B3A1E) else Color(0xB3120C09), RoundedCornerShape(12.dp))
                    .clickable { onPick(lang) }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(lang.flag, fontSize = 30.sp)
                Spacer(Modifier.width(16.dp))
                Text(lang.nativeName, color = Color.White, fontSize = 18.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f))
                if (active) Text("✓", color = Color(0xFFFFD27A), fontSize = 20.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------- Piezas comunes

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Color(0xB3120C09), RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = Color(0xFFFFD27A), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = DisplayFont)
        content()
    }
}

@Composable
private fun Hint(text: String) = Text(text, color = Color(0xFFBFA98A), fontSize = 12.sp, lineHeight = 16.sp)

@Composable
private fun Field(label: String, value: String, secret: Boolean = false, onChange: (String) -> Unit) {
    // Se muestra como un botón: el teclado solo sale al pulsarlo (con el mando, pasar por encima no lo abre).
    var editing by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { editing = true }, modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Text(label, color = Color(0xFFBFA98A), fontSize = 12.sp)
            Text(
                when { value.isEmpty() -> "✎ " + stringResource(R.string.field_tap_to_write); secret -> "••••••••"; else -> value },
                color = if (value.isEmpty()) Color(0xFF9C8A70) else Color.White, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
    if (editing) {
        var text by remember { mutableStateOf(value) }
        val focus = remember { androidx.compose.ui.focus.FocusRequester() }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(label) },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, singleLine = true,
                    visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            },
            confirmButton = { TextButton(onClick = { onChange(text.trim()); editing = false }) { Text(stringResource(R.string.done)) } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun FolderRow(name: String, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("📁 $name", color = Color.White, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, stringResource(R.string.remove), tint = Color(0xFFE57373)) }
    }
}

@Composable
private fun EmulatorPicker(title: String, apps: List<AppInfo>, onDismiss: () -> Unit, onPick: (String?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                item {
                    Text(stringResource(R.string.emu_auto_option),
                        modifier = Modifier.fillMaxWidth().clickable { onPick(null) }.padding(12.dp))
                }
                items(apps, key = { it.packageName }) { app ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(app.packageName) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        app.icon?.let { Image(it.toBitmap(64, 64).asImageBitmap(), null, Modifier.size(32.dp)) }
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(app.label)
                            Text(app.packageName, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * Página oficial de inicio de sesión (Steam o Epic) dentro de la app.
 * [onPage] se llama al terminar de cargar cada página; debe llamar a `done()` cuando detecte el login.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LoginWebDialog(
    title: String,
    url: String,
    onDismiss: () -> Unit,
    onPage: (view: WebView, url: String, done: () -> Unit) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().padding(12.dp).background(Color.White, RoundedCornerShape(12.dp))) {
            AndroidView(
                modifier = Modifier.fillMaxSize().padding(top = 44.dp),
                factory = { ctx ->
                    CookieManager.getInstance().setAcceptCookie(true)
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        // Muchas webs de login rechazan los WebView: nos presentamos como Chrome para Android normal
                        // (se quitan las marcas "; wv" y "Version/4.0" que delatan al WebView).
                        settings.userAgentString = settings.userAgentString
                            .replace("; wv", "").replace(Regex("""Version/\d+\.\d+\s*"""), "")
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            var finished = false
                            override fun onPageFinished(view: WebView, url: String) {
                                if (!finished) onPage(view, url) { finished = true; CookieManager.getInstance().flush() }
                            }
                        }
                        loadUrl(url)
                    }
                },
            )
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 12.dp))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

/** Diálogo de "vincular con código": muestra el código, un QR y espera a que el usuario acepte. */
@Composable
private fun EpicLinkDialog(
    state: LibraryViewModel.EpicLink,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.epic_link)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (state) {
                    LibraryViewModel.EpicLink.Starting -> { CircularProgressIndicator(); Text(stringResource(R.string.epic_requesting)) }
                    is LibraryViewModel.EpicLink.Waiting -> {
                        Text(stringResource(R.string.epic_step1))
                        Text(stringResource(R.string.epic_step2))
                        Text(state.code.userCode, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp,
                            fontFamily = FontFamily.Monospace)
                        QrCode(state.code.verificationUrl, Modifier.size(170.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("  " + stringResource(R.string.epic_waiting), fontSize = 12.sp)
                        }
                    }
                    is LibraryViewModel.EpicLink.Failed -> {
                        Text(stringResource(R.string.epic_failed, state.message))
                        Text(stringResource(R.string.epic_failed_hint), fontSize = 12.sp)
                    }
                    LibraryViewModel.EpicLink.Done -> Text(stringResource(R.string.epic_done))
                }
            }
        },
        confirmButton = {
            when (state) {
                is LibraryViewModel.EpicLink.Waiting -> TextButton(onClick = { onOpen(state.code.verificationUrl) }) {
                    Text(stringResource(R.string.open_here))
                }
                is LibraryViewModel.EpicLink.Failed -> TextButton(onClick = onRetry) { Text(stringResource(R.string.new_code)) }
                LibraryViewModel.EpicLink.Done -> TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
                else -> Unit
            }
        },
        dismissButton = {
            if (state !is LibraryViewModel.EpicLink.Done) TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Código QR dibujado con ZXing. */
@Composable
private fun QrCode(text: String, modifier: Modifier = Modifier) {
    val bitmap = remember(text) {
        val m = com.google.zxing.qrcode.QRCodeWriter().encode(text, com.google.zxing.BarcodeFormat.QR_CODE, 0, 0)
        val bmp = android.graphics.Bitmap.createBitmap(m.width, m.height, android.graphics.Bitmap.Config.ARGB_8888)
        for (x in 0 until m.width) for (y in 0 until m.height) {
            bmp.setPixel(x, y, if (m.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
        bmp.asImageBitmap()
    }
    Image(bitmap, "QR", modifier.background(Color.White).padding(8.dp),
        filterQuality = androidx.compose.ui.graphics.FilterQuality.None)
}

/** Qué encontró Repisa en las carpetas de ES-DE/Cocoon, para saber si la lectura funcionó. */
@Composable
private fun FrontendReportText(raw: String?) {
    val r = com.repisa.app.sources.FrontendReport.decode(raw)
    val lines = buildList {
        if (r == null) { add(stringResource(R.string.frontend_reading)); return@buildList }
        if (r.lists == 0 && r.backupFiles == 0) { add(stringResource(R.string.frontend_report_empty)); return@buildList }
        if (r.lists > 0) add(stringResource(R.string.frontend_report, r.lists,
            r.systems.joinToString(", ") { com.repisa.app.model.Consoles.byId(it).name }.ifEmpty { "?" },
            r.entries, r.withGenre, r.withTime, r.matched, r.matchedWithTime))
        if (r.backupFiles > 0) add(stringResource(R.string.frontend_report_backup, r.backupFiles, r.backupWithTime))
        if (r.entries > 0 && r.matched == 0) add(stringResource(R.string.frontend_report_no_match))
        else if (r.withTime == 0) add(stringResource(R.string.frontend_report_no_time))
    }
    Column(Modifier.fillMaxWidth().background(Color(0x22FFD27A), RoundedCornerShape(8.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { Text(it, color = Color(0xFFE8D9C0), fontSize = 13.sp) }
        if (r != null) DiagnosticsButtons()
    }
}

/** Copiar o compartir el diagnóstico de la última lectura, para enviárselo a quien te ayude. */
@Composable
private fun DiagnosticsButtons() {
    val context = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    fun text() = runCatching {
        java.io.File(context.cacheDir, com.repisa.app.sources.EmulatorSource.DIAG_FILE).readText()
    }.getOrNull()
    var copied by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { text()?.let { clipboard.setText(androidx.compose.ui.text.AnnotatedString(it)); copied = true } }) {
            Text(stringResource(if (copied) R.string.diag_copied else R.string.diag_copy), color = Color.White)
        }
        OutlinedButton(onClick = {
            text()?.let {
                context.startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, it), null,
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }) { Text(stringResource(R.string.diag_share), color = Color.White) }
    }
}

// ---------------------------------------------------------------------------- Copia de seguridad y versión

@Composable
private fun DataPage(vm: LibraryViewModel) {
    val s = vm.settings
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportBackup(uri)
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importBackup(uri)
    }
    Section(stringResource(R.string.backup_title)) {
        Hint(stringResource(R.string.backup_hint))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())
                exportFile.launch("repisa-copia-$date.json")
            }) { Text(stringResource(R.string.backup_export)) }
            OutlinedButton(onClick = { importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) {
                Text(stringResource(R.string.backup_import), color = Color.White)
            }
        }
    }
    Section(stringResource(R.string.updates_title)) {
        Text("Repisa v${com.repisa.app.BuildConfig.VERSION_NAME}", color = Color.White)
        Hint(stringResource(R.string.updates_hint))
        OutlinedButton(onClick = { vm.checkForUpdate(quiet = false) }) { Text(stringResource(R.string.updates_check_now), color = Color.White) }
        HorizontalDivider(color = Color(0x33FFFFFF))
        OutlinedButton(onClick = { vm.restartOnboarding() }) { Text(stringResource(R.string.onb_again), color = Color.White) }
        Hint(stringResource(R.string.license_hint))
    }
}

// ---------------------------------------------------------------------------- Temas

/** Elegir el tema de vitrina: cada tarjeta es una vista previa en pequeño (pared, cajas y balda). */
@Composable
private fun ThemesPage(vm: LibraryViewModel) {
    val games by vm.displayGames.collectAsState()
    val sample = remember(games) { games.filter { it.coverUrl != null }.take(4) }
    Section("🎨 " + stringResource(R.string.menu_themes)) {
        Hint(stringResource(R.string.themes_hint))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            (ShelfTheme.entries + ShelfTheme.custom).forEach { t ->
                ThemeCard(t, ShelfTheme.current.name == t.name, sample) { vm.setTheme(t) }
            }
        }
    }
    // Personalizado: textura de uno de los temas y colores a elegir.
    Section("🖌 " + stringResource(R.string.theme_custom)) {
        Hint(stringResource(R.string.theme_custom_hint))
        var c by remember { mutableStateOf(CustomTheme.decode(vm.settings.customTheme)) }
        fun apply(n: CustomTheme) { c = n; vm.setCustomTheme(n) }
        Text(stringResource(R.string.theme_texture), color = Color.White)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(PanelStyle.PLANKS to R.string.texture_wood, PanelStyle.BRUSHED to R.string.texture_metal,
                PanelStyle.GLASS to R.string.texture_glass, PanelStyle.PEGBOARD to R.string.texture_pegboard).forEach { (p, label) ->
                androidx.compose.material3.FilterChip(c.panel == p, { apply(c.copy(panel = p)) }, { Text(stringResource(label)) })
            }
        }
        ColorRow(stringResource(R.string.theme_color_wall), c.wall, WALL_COLORS) { apply(c.copy(wall = it)) }
        ColorRow(stringResource(R.string.theme_color_shelf), c.shelf, SHELF_COLORS) { apply(c.copy(shelf = it)) }
        ColorRow(stringResource(R.string.theme_color_light), c.light, LIGHT_COLORS) { apply(c.copy(light = it)) }
    }
}

private val WALL_COLORS = listOf(0xFF2A1A0F, 0xFF5E4024, 0xFF1C1C1E, 0xFF3A3F45, 0xFF0E1720, 0xFF16324A, 0xFF2E3A2A, 0xFF1F4030,
    0xFF26103D, 0xFF4A1530, 0xFF5A1E1E, 0xFF6B5A3A, 0xFFB0A58E, 0xFF2B2B5E)
private val SHELF_COLORS = listOf(0xFF8C5A30, 0xFFD9B07A, 0xFF3E2C22, 0xFFB9C0C7, 0xFF8EC4E0, 0xFFD8363F, 0xFF2F7D4F, 0xFF3F55E0,
    0xFF8E44AD, 0xFFE67E22, 0xFFF1C40F, 0xFFEDEDED, 0xFF222222, 0xFFE91E63)
private val LIGHT_COLORS = listOf(0xFFFFD9A0, 0xFFFFF1D0, 0xFFE4F2FF, 0xFF9EEBFF, 0xFFFF66E0, 0xFFA4E86A, 0xFFFF6B6B, 0xFFFFE45A,
    0xFFB388FF, 0xFFFFFFFF)

/** Fila de muestras de color; la elegida lleva borde blanco. */
@Composable
private fun ColorRow(title: String, selected: Long, colors: List<Long>, onPick: (Long) -> Unit) {
    Text(title, color = Color.White, modifier = Modifier.padding(top = 4.dp))
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { col ->
            var focused by remember { mutableStateOf(false) }
            Box(
                Modifier.size(38.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color(col))
                    .border(if (col == selected || focused) 3.dp else 1.dp,
                        if (focused) Color(0xFFFFD27A) else if (col == selected) Color.White else Color(0x55FFFFFF),
                        androidx.compose.foundation.shape.CircleShape)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { onPick(col) },
            ) { if (col == selected) Text("✓", color = if (Color(col).red + Color(col).green > 1.3f) Color.Black else Color.White,
                fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center)) }
        }
    }
}

@Composable
private fun ThemeCard(t: ShelfTheme, chosen: Boolean, sample: List<com.repisa.app.model.Game>, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier.width(250.dp).clip(RoundedCornerShape(12.dp))
            .border(if (chosen || focused) 3.dp else 1.dp, if (focused) Color.White else if (chosen) Color(0xFFFFD27A) else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(130.dp).roomBackground(t)) {
            Column(Modifier.align(Alignment.BottomCenter)) {
                Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                    sample.forEach { g -> GameBox(g, 62.dp, showStatus = false) }
                    if (sample.isEmpty()) repeat(4) { Box(Modifier.size(42.dp, 60.dp).background(Color(0x55FFFFFF))) }
                }
                WoodPlank(t, Modifier.fillMaxWidth().height(22.dp))
            }
            CabinetSides(theme = t)
            Box(Modifier.padding(10.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(t.plaque), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(t.label, color = t.plaqueText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
            }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xCC120C09)).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("${t.emoji}  ${t.label}", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (chosen) Text("✓", color = Color(0xFFFFD27A), fontWeight = FontWeight.Black)
        }
    }
}
