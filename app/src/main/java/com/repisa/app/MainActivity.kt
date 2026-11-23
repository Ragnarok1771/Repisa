package com.repisa.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
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
import com.repisa.app.ui.DetailScreen
import com.repisa.app.ui.GroupMode
import com.repisa.app.ui.LibraryViewModel
import com.repisa.app.ui.RepisaTheme
import com.repisa.app.ui.SecondScreen
import com.repisa.app.ui.SettingsScreen
import com.repisa.app.ui.ShelfScreen

class MainActivity : ComponentActivity() {
    private val vm: LibraryViewModel by viewModels()
    private lateinit var secondScreen: SecondScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        secondScreen = SecondScreen(this, vm)
        setContent { RepisaTheme { App(vm, onSettingsClosed = { secondScreen.update() }) } }
    }

    override fun onStart() { super.onStart(); secondScreen.start() }
    override fun onStop() { secondScreen.stop(); super.onStop() }
    override fun onResume() { super.onResume(); vm.onResume() }
}

@Composable
private fun App(vm: LibraryViewModel, onSettingsClosed: () -> Unit) {
    var inSettings by rememberSaveable { mutableStateOf(false) }
    val selectedId by vm.selectedId.collectAsState()
    val games by vm.games.collectAsState()
    val selected = games.firstOrNull { it.id == selectedId }
    val snackbar = remember { SnackbarHostState() }
    val message by vm.message.collectAsState()

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.message.value = null }
    }

    fun closeSettings() { inSettings = false; onSettingsClosed() }

    BackHandler(enabled = inSettings || selected != null) {
        if (selected != null) vm.selectedId.value = null else closeSettings()
    }

    Box(
        Modifier.fillMaxSize().onPreviewKeyEvent { e ->
            if (e.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
            when (e.key.nativeKeyCode) {
                KeyEvent.KEYCODE_BUTTON_B -> when {
                    selected != null -> { vm.selectedId.value = null; true }
                    inSettings -> { closeSettings(); true }
                    else -> false
                }
                KeyEvent.KEYCODE_BUTTON_START -> { if (inSettings) closeSettings() else inSettings = true; true }
                KeyEvent.KEYCODE_BUTTON_SELECT -> {
                    val modes = GroupMode.entries
                    vm.setGroup(modes[(modes.indexOf(vm.options.value.group) + 1) % modes.size]); true
                }
                KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1 -> {
                    val order = listOf<Source?>(null) + Source.entries
                    val i = order.indexOf(vm.options.value.source)
                    val step = if (e.key.nativeKeyCode == KeyEvent.KEYCODE_BUTTON_R1) 1 else order.size - 1
                    vm.setSource(order[(i + step) % order.size]); true
                }
                else -> false
            }
        },
    ) {
        if (inSettings) {
            SettingsScreen(vm, onBack = { closeSettings() })
        } else {
            ShelfScreen(vm, onOpenSettings = { inSettings = true }, onOpenGame = { vm.selectedId.value = it.id })
            selected?.let { DetailScreen(it, vm, onClose = { vm.selectedId.value = null }) }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp))
    }
}
