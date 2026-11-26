package com.repisa.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.R
import com.repisa.app.model.Source

/**
 * Guía de primera vez: qué es Repisa y los pasos para tenerla lista (copia de seguridad, Steam/Epic, carpeta de ROMs,
 * tiempo de juego y cómo se usa con el mando). [openSettings] lleva a Ajustes; al volver se sigue donde se dejó.
 */
@Composable
fun OnboardingScreen(vm: LibraryViewModel, openSettings: () -> Unit, onDone: (withTour: Boolean) -> Unit) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(vm.settings.onboardingStep) }
    fun go(n: Int) { step = n; vm.settings.onboardingStep = n }
    val steps = 7

    val pickRoms = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            vm.settings.romFolders = vm.settings.romFolders + uri.toString()
            vm.syncOne(Source.EMULATOR)
        }
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.importBackup(uri) }
    var granted by remember { mutableStateOf(vm.launcher.usage.hasPermission()) }

    Box(Modifier.fillMaxSize().roomBackground().background(Color(0xB3120C08)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 720.dp).padding(20.dp).background(Color(0xF0261A12), RoundedCornerShape(16.dp)).padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Puntos de progreso
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(steps) { i -> Box(Modifier.size(10.dp).background(if (i <= step) Color(0xFFFFD27A) else Color(0x44FFFFFF), CircleShape)) }
            }
            when (step) {
                0 -> {
                    Title("📚 " + stringResource(R.string.onb_welcome_title))
                    Body(stringResource(R.string.onb_welcome_body))
                    Body(stringResource(R.string.onb_backup_body))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { go(1) }) { Text(stringResource(R.string.onb_start)) }
                        OutlinedButton(onClick = { importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) {
                            Text(stringResource(R.string.onb_import), color = Color.White)
                        }
                    }
                }
                1 -> {
                    Title("🖥 " + stringResource(R.string.onb_pc_title))
                    Body(stringResource(R.string.onb_pc_body))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { go(2); openSettings() }) { Text(stringResource(R.string.onb_pc_link)) }
                        OutlinedButton(onClick = { go(2) }) { Text(stringResource(R.string.onb_skip_step), color = Color.White) }
                    }
                }
                2 -> {
                    Title("🎮 " + stringResource(R.string.onb_emu_title))
                    Body(stringResource(R.string.onb_emu_body))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { pickRoms.launch(null) }) { Text(stringResource(R.string.add_rom_folder)) }
                        OutlinedButton(onClick = { go(3) }) { Text(stringResource(R.string.onb_next), color = Color.White) }
                    }
                    if (vm.settings.romFolders.isNotEmpty()) Body("✓ " + stringResource(R.string.onb_emu_added, vm.settings.romFolders.size))
                }
                3 -> {
                    // RetroAchievements (recomendado): los logros de los juegos emulados.
                    Title("🏆 " + stringResource(R.string.onb_ra_title))
                    Body(stringResource(R.string.onb_ra_body))
                    val linked = vm.settings.raUser != null && vm.settings.raApiKey != null
                    if (linked) Body("✓ " + stringResource(R.string.ra_linked, vm.settings.raUser.orEmpty()))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!linked) Button(onClick = { go(4); openSettings() }) { Text(stringResource(R.string.ra_link)) }
                        OutlinedButton(onClick = { go(4) }) { Text(stringResource(if (linked) R.string.onb_next else R.string.onb_skip_step), color = Color.White) }
                    }
                }
                4 -> {
                    // SteamGridDB (recomendado): más carátulas, para que cada caja tenga la suya.
                    Title("🖼 " + stringResource(R.string.onb_sgdb_title))
                    Body(stringResource(R.string.onb_sgdb_body))
                    val has = !vm.settings.steamGridDbKey.isNullOrBlank()
                    if (has) Body("✓ " + stringResource(R.string.onb_sgdb_done))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!has) Button(onClick = { vm.launcher.openUrl("https://www.steamgriddb.com/profile/preferences/api") }) {
                            Text(stringResource(R.string.sgdb_get))
                        }
                        if (!has) OutlinedButton(onClick = { go(5); vm.debugPage = "COVERS"; openSettings() }) {
                            Text(stringResource(R.string.onb_sgdb_paste), color = Color.White)
                        }
                        OutlinedButton(onClick = { go(5) }) { Text(stringResource(if (has) R.string.onb_next else R.string.onb_skip_step), color = Color.White) }
                    }
                }
                5 -> {
                    Title("⏱ " + stringResource(R.string.onb_time_title))
                    Body(stringResource(R.string.onb_time_body))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!granted) Button(onClick = {
                            runCatching { context.startActivity(vm.launcher.usage.permissionIntent()) }
                            granted = vm.launcher.usage.hasPermission()
                        }) { Text(stringResource(R.string.grant)) } else Body("✓ " + stringResource(R.string.usage_granted))
                        OutlinedButton(onClick = { go(6) }) { Text(stringResource(R.string.onb_next), color = Color.White) }
                    }
                }
                else -> {
                    Title("🎉 " + stringResource(R.string.onb_done_title))
                    Body(stringResource(R.string.onb_done_body))
                    Button(onClick = { onDone(true) }) { Text(stringResource(R.string.onb_finish)) }
                }
            }
            Spacer(Modifier.size(4.dp))
            Row {
                if (step > 0) TextButton(onClick = { go(step - 1) }) { Text(stringResource(R.string.back)) }
                Spacer(Modifier.weight(1f))
                if (step < steps - 1) TextButton(onClick = { onDone(false) }) { Text(stringResource(R.string.onb_skip_all)) }
            }
        }
    }
}

@Composable
private fun Title(text: String) = Text(text, color = Color(0xFFFFD27A), fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = DisplayFont)

@Composable
private fun Body(text: String) = Text(text, color = Color(0xFFE8D9C0), fontSize = 15.sp, lineHeight = 21.sp)
