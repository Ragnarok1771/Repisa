package com.repisa.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.repisa.app.data.SafTree
import com.repisa.app.launch.AppInfo
import com.repisa.app.model.Consoles
import com.repisa.app.model.Source
import com.repisa.app.sources.EpicSource
import com.repisa.app.sources.SteamLogin
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: LibraryViewModel, onBack: () -> Unit) {
    val s = vm.settings
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) } // fuerza recomposición tras cambiar prefs
    fun changed() { refresh++ }
    // Al volver de los ajustes de Android (p. ej. tras conceder un permiso), refrescar.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { changed() }

    fun persist(uri: Uri) = context.contentResolver.takePersistableUriPermission(
        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
    )
    val pickRoms = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { persist(uri); s.romFolders = s.romFolders + uri.toString(); changed() }
    }
    val pickEsde = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { persist(uri); s.esdeFolder = uri.toString(); changed() }
    }
    val pickCocoon = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { persist(uri); s.cocoonFolder = uri.toString(); changed() }
    }
    val pickRetroArch = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { persist(uri); s.retroarchFolder = uri.toString(); changed() }
    }

    var epicWeb by remember { mutableStateOf(false) }
    var steamWeb by remember { mutableStateOf(false) }
    var pickEmuFor by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().roomBackground()) {
        Row(Modifier.fillMaxWidth().background(Color(0xCC120C09)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) }
            Text("Ajustes", color = Color(0xFFFFD27A), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
            Spacer(Modifier.weight(1f))
            Button(onClick = { vm.syncAll(); onBack() }) { Text("Sincronizar todo") }
        }
        // `refresh` se lee aquí para que la columna se recomponga al cambiar ajustes.
        key(refresh) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ---------------------------------------------------------- Steam
                Section("Steam") {
                    val linked = s.steamLinkedId != null || s.steamApiKey != null
                    if (linked) {
                        Hint("Cuenta vinculada" + (s.steamLinkedName?.let { ": $it" } ?: s.steamLinkedId?.let { " (SteamID $it)" } ?: " con clave de API") + ".")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.syncOne(Source.STEAM) }) { Text("Sincronizar Steam") }
                            OutlinedButton(onClick = { vm.steamUnlink(); changed() }) { Text("Desvincular", color = Color.White) }
                        }
                    } else {
                        Hint("Inicia sesión en la página oficial de Steam (con tu usuario o con el código QR de la app de Steam). " +
                            "Repisa solo lee tu lista de juegos y tus horas; tu contraseña no pasa por la app.")
                        Button(onClick = { steamWeb = true }) { Text("Vincular cuenta de Steam") }
                        var advanced by remember { mutableStateOf(false) }
                        TextButton(onClick = { advanced = !advanced }) {
                            Text(if (advanced) "Ocultar opción avanzada" else "Avanzado: usar una clave de API en su lugar")
                        }
                        if (advanced) {
                            Hint("Clave de la Steam Web API y tu SteamID64 o URL de perfil. 'Detalles de juegos' debe estar en Público.")
                            TextButton(onClick = { vm.launcher.openUrl("https://steamcommunity.com/dev/apikey") }) { Text("Conseguir clave en steamcommunity.com/dev/apikey") }
                            var key by remember { mutableStateOf(s.steamApiKey.orEmpty()) }
                            var user by remember { mutableStateOf(s.steamUser.orEmpty()) }
                            Field("Clave de API de Steam", key, secret = true) { key = it }
                            Field("SteamID64 / perfil", user) { user = it }
                            Button(onClick = {
                                s.steamApiKey = key; s.steamUser = user; vm.syncOne(Source.STEAM); changed()
                            }, enabled = key.isNotBlank() && user.isNotBlank()) { Text("Guardar y sincronizar") }
                        }
                    }
                }

                // ---------------------------------------------------------- Epic
                Section("Epic Games") {
                    if (vm.epic.isLoggedIn) {
                        Hint("Conectado${s.epicDisplayName?.let { " como $it" } ?: ""}.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.syncOne(Source.EPIC) }) { Text("Sincronizar Epic") }
                            OutlinedButton(onClick = { vm.epicLogout(); changed() }) { Text("Cerrar sesión", color = Color.White) }
                        }
                    } else {
                        Hint("Pulsa el botón: Repisa te dará un código. Ábrelo en epicgames.com/activate desde cualquier navegador " +
                            "(o escanea el QR con el móvil), inicia sesión como siempre —también con Google, Xbox, PlayStation o Steam— " +
                            "y acepta. Repisa solo lee tu biblioteca y tu tiempo de juego.")
                        Button(onClick = { vm.epicStartLink() }) { Text("Vincular cuenta de Epic Games") }
                        var other by remember { mutableStateOf(false) }
                        TextButton(onClick = { other = !other }) { Text(if (other) "Ocultar otros métodos" else "Otros métodos de inicio de sesión") }
                        if (other) {
                            OutlinedButton(onClick = { epicWeb = true }) { Text("Iniciar sesión dentro de la app", color = Color.White) }
                            Hint("O ábrelo en el navegador, inicia sesión y copia aquí el valor de \"authorizationCode\":")
                            TextButton(onClick = { vm.launcher.openUrl(EpicSource.LOGIN_URL) }) { Text("Abrir el login de Epic en el navegador") }
                            var code by remember { mutableStateOf("") }
                            Field("Código de autorización", code) { code = it }
                            Button(onClick = { vm.epicLogin(code); code = "" }, enabled = code.isNotBlank()) { Text("Usar código") }
                        }
                    }
                }

                // ---------------------------------------------------------- Emuladores
                Section("Emuladores") {
                    Hint("Elige la carpeta donde guardas los ROMs (con subcarpetas por sistema: snes, psx, ps2, gba, switch…, como ES-DE). " +
                        "Puedes añadir varias, p. ej. memoria interna y tarjeta SD.")
                    s.romFolders.forEach { uri ->
                        FolderRow(SafTree.displayName(context, uri)) { s.romFolders = s.romFolders - uri; changed() }
                    }
                    OutlinedButton(onClick = { pickRoms.launch(null) }) { Text("Añadir carpeta de ROMs", color = Color.White) }

                    HorizontalDivider(color = Color(0x33FFFFFF))
                    Hint("Opcional: carpeta ES-DE (usa sus portadas descargadas, géneros, estudios y fechas).")
                    s.esdeFolder?.let { FolderRow(SafTree.displayName(context, it)) { s.esdeFolder = null; changed() } }
                        ?: OutlinedButton(onClick = { pickEsde.launch(null) }) { Text("Elegir carpeta ES-DE", color = Color.White) }

                    Hint("Opcional: carpeta RetroArch (lee las playlists y el tiempo de juego). Activa en RetroArch: " +
                        "Ajustes → Guardado → 'Guardar registros de tiempo de juego'.")
                    s.retroarchFolder?.let { FolderRow(SafTree.displayName(context, it)) { s.retroarchFolder = null; changed() } }
                        ?: OutlinedButton(onClick = { pickRetroArch.launch(null) }) { Text("Elegir carpeta RetroArch", color = Color.White) }

                    Hint("Opcional: carpeta de datos de Cocoon. En Cocoon usa Ajustes → Library & Data → Export Metadata para que " +
                        "genere los gamelist.xml; Repisa lee de ahí géneros, fechas, carátulas y el tiempo de juego si viene incluido. " +
                        "Sirve también para Batocera, RetroBat u otros frontends que exporten gamelist.xml.")
                    s.cocoonFolder?.let { FolderRow(SafTree.displayName(context, it)) { s.cocoonFolder = null; changed() } }
                        ?: OutlinedButton(onClick = { pickCocoon.launch(null) }) { Text("Elegir carpeta de Cocoon", color = Color.White) }

                    Button(onClick = { vm.syncOne(Source.EMULATOR) }) { Text("Escanear emuladores") }

                    val consoles = vm.consolesInLibrary()
                    if (consoles.isNotEmpty()) {
                        HorizontalDivider(color = Color(0x33FFFFFF))
                        Hint("Emulador para abrir cada sistema (si no eliges ninguno, Android preguntará). Repisa mide el tiempo que pasas jugando lo que abres desde aquí.")
                        consoles.forEach { id ->
                            val pkg = s.emulatorFor(id)
                            Row(Modifier.fillMaxWidth().clickable { pickEmuFor = id }.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(Consoles.byId(id).name, color = Color.White, modifier = Modifier.weight(1f))
                                Text(pkg?.let { vm.launcher.appLabel(it) } ?: "Preguntar", color = Color(0xFFFFD27A))
                            }
                        }
                    }
                }

                // ---------------------------------------------------------- Tiempo de juego
                Section("Tiempo de juego") {
                    Hint("Steam, Epic y emuladores se vuelven a leer solos al abrir Repisa o al volver a ella, así que " +
                        "las horas que juegues en el PC, en otro frontend o en RetroArch aparecen aquí actualizadas.")
                    val granted = vm.launcher.usage.hasPermission()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Acceso a datos de uso: " + if (granted) "concedido ✓" else "no concedido", color = Color.White)
                            Hint("Con este permiso de Android (el mismo que usa Cocoon), Repisa mide solo el tiempo que el emulador " +
                                "está realmente en pantalla en los juegos que abres desde aquí: no cuenta el reposo ni otras apps.")
                        }
                        if (!granted) OutlinedButton(onClick = { context.startActivity(vm.launcher.usage.permissionIntent()) }) {
                            Text("Conceder", color = Color.White)
                        }
                    }
                }

                // ---------------------------------------------------------- Metadatos
                Section("Portadas y datos") {
                    Hint("Carátulas: cada juego tiene un botón \"Buscar carátula\" en su ficha. Con este botón se buscan " +
                        "automáticamente las de todos los juegos que no tienen, por palabras clave del nombre.")
                    Button(onClick = { vm.findMissingCovers(); onBack() }) { Text("Buscar carátulas que faltan") }
                    Hint("Recomendado: SteamGridDB, la base de carátulas que usa Cocoon (gratis). Crea una cuenta, ve a " +
                        "Preferencias → API y pega aquí tu clave.")
                    TextButton(onClick = { vm.launcher.openUrl("https://www.steamgriddb.com/profile/preferences/api") }) {
                        Text("Conseguir clave de SteamGridDB")
                    }
                    var sgdb by remember { mutableStateOf(s.steamGridDbKey.orEmpty()) }
                    Field("Clave de SteamGridDB", sgdb, secret = true) { sgdb = it }
                    OutlinedButton(onClick = { s.steamGridDbKey = sgdb.ifBlank { null }; changed() }) { Text("Guardar clave", color = Color.White) }
                    HorizontalDivider(color = Color(0x33FFFFFF))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Portadas de libretro-thumbnails para emuladores", color = Color.White, modifier = Modifier.weight(1f))
                        Switch(checked = s.useLibretroCovers, onCheckedChange = { s.useLibretroCovers = it; changed() })
                    }
                    Hint("Opcional: IGDB completa géneros, estudios y portadas de cualquier juego (sobre todo Epic y emuladores). " +
                        "Crea una aplicación gratis en dev.twitch.tv y pega su Client ID y Client Secret.")
                    TextButton(onClick = { vm.launcher.openUrl("https://api-docs.igdb.com/#account-creation") }) { Text("Cómo conseguir las credenciales de IGDB") }
                    var id by remember { mutableStateOf(s.igdbClientId.orEmpty()) }
                    var secret by remember { mutableStateOf(s.igdbClientSecret.orEmpty()) }
                    Field("IGDB Client ID", id) { id = it }
                    Field("IGDB Client Secret", secret, secret = true) { secret = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            s.igdbClientId = id.ifBlank { null }; s.igdbClientSecret = secret.ifBlank { null }
                            vm.startEnrichment(force = true); changed()
                        }) { Text("Guardar y volver a buscar datos") }
                    }
                }

                // ---------------------------------------------------------- Calificaciones
                Section("Calificaciones") {
                    val scale by vm.ratingScale.collectAsState()
                    Hint("Escala con la que pones y ves las notas. Puedes cambiarla cuando quieras: las notas ya puestas se convierten solas.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.repisa.app.model.RatingScale.entries.forEach { sc ->
                            androidx.compose.material3.FilterChip(selected = scale == sc, onClick = { vm.setRatingScale(sc) },
                                label = { Text(sc.label) })
                        }
                    }
                    Hint("Plantilla: lo que se califica por defecto en cada juego nuevo y cuánto pesa cada cosa. En cada juego puedes " +
                        "añadir, quitar o renombrar lo que quieras (\"Historia\", \"Música\", \"El cielo\"…).")
                    val template = remember { androidx.compose.runtime.mutableStateListOf<com.repisa.app.model.Criterion>().apply { addAll(loadTemplate(s)) } }
                    CriteriaEditor(template, scale, withScores = false)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { saveTemplate(s, template.filter { it.name.isNotBlank() }); changed() }) { Text("Guardar plantilla") }
                        TextButton(onClick = { template.clear(); template.addAll(com.repisa.app.model.DefaultCriteria) }) { Text("Restaurar la original") }
                    }
                }

                // ---------------------------------------------------------- Pantalla
                Section("Pantalla") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Usar la segunda pantalla", color = Color.White)
                            Hint("En la AYN Thor, la pantalla de abajo muestra la ficha del juego seleccionado.")
                        }
                        Switch(checked = s.secondScreen, onCheckedChange = { s.secondScreen = it; changed() })
                    }
                    Text("Tamaño de las cajas: ${s.shelfHeightDp} dp (altura de una caja de DVD; el resto, a escala real)", color = Color.White)
                    var h by remember { mutableStateOf(s.shelfHeightDp.toFloat()) }
                    Slider(value = h, onValueChange = { h = it }, onValueChangeFinished = { s.shelfHeightDp = h.toInt(); changed() },
                        valueRange = 90f..240f, modifier = Modifier.widthIn(max = 480.dp))
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (epicWeb) {
        LoginWebDialog("Vincular cuenta de Epic Games", EpicSource.LOGIN_URL, onDismiss = { epicWeb = false }) { view, url, done ->
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
        LoginWebDialog("Vincular cuenta de Steam", SteamLogin.LOGIN_URL, onDismiss = { steamWeb = false }) { _, _, done ->
            if (SteamLogin.isLoggedInWeb()) { done(); steamWeb = false; vm.steamLinked(); changed() }
        }
    }
    pickEmuFor?.let { consoleId ->
        EmulatorPicker(
            title = "Emulador para ${Consoles.byId(consoleId).name}",
            apps = remember { vm.launcher.installedApps() },
            onDismiss = { pickEmuFor = null },
        ) { pkg -> s.setEmulatorFor(consoleId, pkg); pickEmuFor = null; changed() }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Color(0xB3120C09), RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = Color(0xFFFFD27A), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)
        content()
    }
}

@Composable
private fun Hint(text: String) = Text(text, color = Color(0xFFBFA98A), fontSize = 12.sp, lineHeight = 16.sp)

@Composable
private fun Field(label: String, value: String, secret: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text),
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
    )
}

@Composable
private fun FolderRow(name: String, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("📁 $name", color = Color.White, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Quitar", tint = Color(0xFFE57373)) }
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
                    Text("Preguntar cada vez", modifier = Modifier.fillMaxWidth().clickable { onPick(null) }.padding(12.dp))
                }
                items(apps, key = { it.packageName }) { app ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(app.packageName) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        app.icon?.let {
                            Image(it.toBitmap(64, 64).asImageBitmap(), null, Modifier.size(32.dp))
                        }
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(app.label)
                            Text(app.packageName, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
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
                        // Algunas webs rechazan los WebView: nos presentamos como Chrome móvil.
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
                TextButton(onClick = onDismiss) { Text("Cancelar") }
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
        title = { Text("Vincular cuenta de Epic Games") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (state) {
                    LibraryViewModel.EpicLink.Starting -> { CircularProgressIndicator(); Text("Pidiendo un código a Epic…") }
                    is LibraryViewModel.EpicLink.Waiting -> {
                        Text("1. Abre epicgames.com/activate (o escanea el QR con el móvil)")
                        Text("2. Inicia sesión y escribe este código:")
                        Text(state.code.userCode, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp,
                            fontFamily = FontFamily.Monospace)
                        QrCode(state.code.verificationUrl, Modifier.size(170.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("  Esperando a que aceptes en la web…", fontSize = 12.sp)
                        }
                    }
                    is LibraryViewModel.EpicLink.Failed -> {
                        Text("No se pudo vincular: ${state.message}")
                        Text("Si se repite, prueba con \"Otros métodos de inicio de sesión\".", fontSize = 12.sp)
                    }
                    LibraryViewModel.EpicLink.Done -> Text("¡Cuenta vinculada! Cargando tu biblioteca…")
                }
            }
        },
        confirmButton = {
            when (state) {
                is LibraryViewModel.EpicLink.Waiting -> TextButton(onClick = { onOpen(state.code.verificationUrl) }) { Text("Abrir aquí") }
                is LibraryViewModel.EpicLink.Failed -> TextButton(onClick = onRetry) { Text("Pedir otro código") }
                LibraryViewModel.EpicLink.Done -> TextButton(onClick = onDismiss) { Text("Listo") }
                else -> Unit
            }
        },
        dismissButton = { if (state !is LibraryViewModel.EpicLink.Done) TextButton(onClick = onDismiss) { Text("Cancelar") } },
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
    Image(bitmap, "Código QR", modifier.background(Color.White).padding(8.dp),
        filterQuality = androidx.compose.ui.graphics.FilterQuality.None)
}
