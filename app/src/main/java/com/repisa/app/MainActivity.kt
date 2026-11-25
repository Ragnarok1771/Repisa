package com.repisa.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.repisa.app.model.Source
import com.repisa.app.R
import com.repisa.app.ui.DetailScreen
import com.repisa.app.ui.GroupMode
import com.repisa.app.ui.LibraryViewModel
import com.repisa.app.ui.RatedScreen
import com.repisa.app.ui.RepisaTheme
import com.repisa.app.ui.SecondScreen
import com.repisa.app.ui.SettingsScreen
import com.repisa.app.ui.ShelfScreen

class MainActivity : ComponentActivity() {
    private val vm: LibraryViewModel by lazy { (application as RepisaApp).library() }
    private lateinit var secondScreen: SecondScreen

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.repisa.app.i18n.L10n.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        secondScreen = SecondScreen(this, vm)
        // Los juegos se abren desde esta pantalla y sin transición (Android deslizaba Repisa y el juego de lado).
        vm.launcher.starter = { intent, options ->
            startActivity(intent, options)
            @Suppress("DEPRECATION") overridePendingTransition(0, 0)
            if (android.os.Build.VERSION.SDK_INT >= 34) overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
        vm.releaseSecondScreen = { secondScreen.stop() }
        debugAnimation(intent)
        setContent {
            com.repisa.app.i18n.Localized {
                RepisaTheme {
                    // Primera vez: elegir idioma antes de nada.
                    if (com.repisa.app.i18n.L10n.current == null) {
                        com.repisa.app.ui.WelcomeScreen { lang -> com.repisa.app.i18n.L10n.setLanguage(application, lang) }
                    } else {
                        App(vm, onSettingsClosed = { secondScreen.update() }, onLanguageChanged = {})
                    }
                }
            }
        }
    }

    override fun onStart() { super.onStart(); secondScreen.start() }
    override fun onStop() { secondScreen.stop(); super.onStop() }
    override fun onResume() { super.onResume(); vm.onResume() }

    override fun onDestroy() {
        vm.launcher.starter = null
        vm.releaseSecondScreen = null
        super.onDestroy()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        debugAnimation(intent)
    }

    /** Solo en la versión de pruebas: congelar la animación al jugar en un momento (para revisar sus fotogramas). */
    private fun debugAnimation(intent: android.content.Intent?) {
        if (!BuildConfig.DEBUG || intent == null) return
        val kind = intent.getStringExtra("anim_kind")?.let { runCatching { com.repisa.app.ui.LaunchKind.valueOf(it) }.getOrNull() } ?: return
        vm.previewLaunch(kind, intent.getStringExtra("anim_console"))
        com.repisa.app.ui.LaunchDebug.freezeAt = intent.getFloatExtra("anim_at", -1f).takeIf { it >= 0f }
    }

    /**
     * Botones globales del mando. Se tratan aquí (y no en Compose) para que funcionen
     * aunque todavía no haya ninguna caja enfocada.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        val global = code == KeyEvent.KEYCODE_BUTTON_START || code == KeyEvent.KEYCODE_BUTTON_SELECT ||
            code == KeyEvent.KEYCODE_BUTTON_L1 || code == KeyEvent.KEYCODE_BUTTON_R1
        if (!global) return super.dispatchKeyEvent(event)
        if (event.action != KeyEvent.ACTION_UP) return true
        when (code) {
            KeyEvent.KEYCODE_BUTTON_START -> {
                vm.inSettings.value = !vm.inSettings.value
                if (!vm.inSettings.value) secondScreen.update()
            }
            KeyEvent.KEYCODE_BUTTON_SELECT -> {
                val modes = GroupMode.entries
                vm.setGroup(modes[(modes.indexOf(vm.options.value.group) + 1) % modes.size])
                vm.message.value = com.repisa.app.i18n.L10n.s(R.string.msg_group_changed, vm.options.value.group.label)
            }
            else -> {
                val order = listOf<Source?>(null) + Source.entries
                val i = order.indexOf(vm.options.value.source)
                val step = if (code == KeyEvent.KEYCODE_BUTTON_R1) 1 else order.size - 1
                vm.setSource(order[(i + step) % order.size])
            }
        }
        return true
    }
}

@Composable
private fun App(vm: LibraryViewModel, onSettingsClosed: () -> Unit, onLanguageChanged: () -> Unit) {
    val inSettings by vm.inSettings.collectAsState()
    var inRated by rememberSaveable { mutableStateOf(false) }
    var inRandom by rememberSaveable { mutableStateOf(false) }
    var inStats by rememberSaveable { mutableStateOf(false) }
    var inAch by rememberSaveable { mutableStateOf(false) }
    val selectedId by vm.selectedId.collectAsState()
    val games by vm.displayGames.collectAsState()
    val selected = games.firstOrNull { it.id == selectedId }
    val snackbar = remember { SnackbarHostState() }
    val message by vm.message.collectAsState()

    // Al abrir: ¿hay una versión nueva de Repisa? (como mucho cada 6 h; se puede desactivar en Ajustes)
    LaunchedEffect(Unit) { vm.checkForUpdate(quiet = true) }
    val update by vm.update.collectAsState()
    val updateProgress by vm.updateProgress.collectAsState()

    // En Ajustes, estadísticas, logros o calificaciones no hay carátula seleccionada: la pantalla de abajo muestra el resumen.
    LaunchedEffect(inSettings, inStats, inAch, inRated) {
        if (inSettings || inStats || inAch || inRated) vm.focused.value = null
    }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.message.value = null }
    }

    fun closeSettings() { vm.inSettings.value = false; onSettingsClosed() }

    // Siempre activo: en la repisa principal, B/Atrás no cierra la app (se pulsaba sin querer y había que
    // volver a abrirla). Para salir, el botón de inicio.
    BackHandler(enabled = true) {
        when {
            selected != null -> vm.selectedId.value = null
            vm.selecting.value -> vm.endSelection()
            inRandom -> inRandom = false
            inStats -> inStats = false
            inAch -> inAch = false
            inRated -> inRated = false
            inSettings -> closeSettings()
            else -> Unit
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xFF16100C)).safeDrawingPadding().onPreviewKeyEvent { e ->
            // Durante la animación al jugar, cualquier botón la salta (y no llega a lo que hay debajo).
            if (vm.launching.value != null) {
                if (e.type == KeyEventType.KeyUp) vm.finishLaunchAnimation()
                return@onPreviewKeyEvent true
            }
            if (e.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
            when (e.key.nativeKeyCode) {
                KeyEvent.KEYCODE_BUTTON_B -> when {
                    selected != null -> { vm.selectedId.value = null; true }
                    vm.selecting.value -> { vm.endSelection(); true }
                    inRandom -> { inRandom = false; true }
                    inStats -> { inStats = false; true }
                    inAch -> { inAch = false; true }
                    inRated -> { inRated = false; true }
                    inSettings -> { closeSettings(); true }
                    else -> false
                }
                else -> false
            }
        },
    ) {
        if (inSettings) {
            SettingsScreen(vm, onBack = { closeSettings() }, onLanguageChanged = onLanguageChanged)
        } else androidx.compose.runtime.CompositionLocalProvider(
            com.repisa.app.ui.LocalShowStatus provides remember(inSettings) { vm.settings.showStatusOnBoxes },
        ) {
            when {
                inStats -> com.repisa.app.ui.StatsScreen(vm, onBack = { inStats = false })
                inAch -> com.repisa.app.ui.AchievementsOverview(vm, onBack = { inAch = false }, onOpen = { vm.selectedId.value = it.id })
                inRated -> RatedScreen(vm, onBack = { inRated = false }, onOpen = { vm.selectedId.value = it.id })
                else -> ShelfScreen(vm, onOpenSettings = { vm.inSettings.value = true }, onOpenGame = { vm.selectedId.value = it.id },
                    onOpenRated = { inRated = true }, onOpenRandom = { inRandom = true }, onOpenStats = { inStats = true },
                    onOpenAchievements = { inAch = true })
            }
            if (inRandom) com.repisa.app.ui.RandomPicker(vm, onClose = { inRandom = false }, onOpen = { vm.selectedId.value = it.id })
            selected?.let { DetailScreen(it, vm, onClose = { vm.selectedId.value = null }) }
        }
        val onboarding by vm.onboarding.collectAsState()
        if (onboarding && !inSettings) com.repisa.app.ui.OnboardingScreen(vm, openSettings = { vm.inSettings.value = true },
            onDone = { vm.finishOnboarding() })
        val launching by vm.launching.collectAsState()
        launching?.let { r ->
            androidx.activity.compose.BackHandler { vm.finishLaunchAnimation() }
            com.repisa.app.ui.LaunchAnimation(r.game, r.kind, onFinish = { vm.finishLaunchAnimation() })
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp))
        update?.let { r -> com.repisa.app.ui.UpdateDialog(r, updateProgress, onUpdate = { vm.installUpdate() }) }
    }
}
