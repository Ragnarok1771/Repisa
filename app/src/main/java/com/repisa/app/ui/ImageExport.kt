package com.repisa.app.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Dibuja [content] y a la vez lo graba en [layer], para poder convertirlo en imagen con [captureBitmap]
 * (el Wrapped y las repisas para presumir).
 */
@Composable
fun Capturable(layer: GraphicsLayer, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }) { content() }
}

@Composable
fun rememberCaptureLayer(): GraphicsLayer = rememberGraphicsLayer()

/** Imagen normal (ARGB) de lo grabado: las de la GPU no se pueden guardar en PNG directamente. */
suspend fun captureBitmap(layer: GraphicsLayer): Bitmap =
    layer.toImageBitmap().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)

object ImageExport {
    /** Guarda en la galería (Imágenes/Repisa). Devuelve true si se guardó. */
    suspend fun saveToGallery(context: Context, bitmap: Bitmap, name: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Repisa")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        }.getOrDefault(false)
    }

    /** Abre el menú de compartir de Android (Discord, WhatsApp, Telegram...) con la imagen. */
    suspend fun share(context: Context, bitmap: Bitmap, name: String, text: String) {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            File(dir, "$name.png").also { f -> f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/**
 * Para pantallas que se abren encima de otra (calendario, resumen del año): se quedan con todos los toques
 * (antes un toque en una zona vacía llegaba al botón de la pantalla de debajo) y el foco del mando no sale de ellas.
 */
fun Modifier.blockUnderneath(): Modifier = this
    .pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent() } } }
    .focusProperties { onExit = { cancelFocusChange() } }
    .focusGroup()
