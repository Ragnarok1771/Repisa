package com.repisa.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.R
import com.repisa.app.data.Updater

/** Aviso de versión nueva con sus notas del parche: actualizar ahora, más tarde u omitir esta versión. */
@Composable
fun UpdateDialog(release: Updater.Release, progress: Float?, onUpdate: () -> Unit, onLater: () -> Unit, onSkip: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (progress == null) onLater() },
        title = { Text(stringResource(R.string.update_title, release.version)) },
        text = {
            Column {
                Text(
                    // Notas tal cual vienen de GitHub, sin los símbolos de formato.
                    release.notes.replace(Regex("""\*\*|`|^#+\s*""", RegexOption.MULTILINE), "").trim()
                        .ifEmpty { stringResource(R.string.update_no_notes) },
                    fontSize = 14.sp,
                    modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                )
                progress?.let {
                    LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                    Text(stringResource(R.string.update_downloading, (it * 100).toInt()), fontSize = 12.sp)
                }
            }
        },
        confirmButton = { Button(onClick = onUpdate, enabled = progress == null) { Text(stringResource(R.string.update_now)) } },
        dismissButton = {
            Column {
                TextButton(onClick = onLater, enabled = progress == null) { Text(stringResource(R.string.update_later)) }
                TextButton(onClick = onSkip, enabled = progress == null) { Text(stringResource(R.string.update_skip)) }
            }
        },
    )
}
