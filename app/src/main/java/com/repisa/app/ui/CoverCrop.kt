package com.repisa.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repisa.app.R
import com.repisa.app.model.Game
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** Carga una imagen de la galería reducida a un tamaño manejable (lado mayor ≤ 2400 px). */
fun loadPicked(context: Context, uri: Uri): Bitmap? = runCatching {
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
        val longest = max(info.size.width, info.size.height)
        if (longest > 2400) {
            val f = 2400f / longest
            decoder.setTargetSize((info.size.width * f).roundToInt(), (info.size.height * f).roundToInt())
        }
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
}.getOrNull()

/**
 * Encuadrar una foto como carátula: el marco tiene la proporción de la caja del sistema (Switch más alta y
 * estrecha, N64 apaisada...). La foto se mueve y se amplía con el dedo o con los botones − / +.
 * Al guardar se recorta y se guarda en la app como carátula de ese juego.
 */
@Composable
fun CoverCropDialog(game: Game, bitmap: Bitmap, onDismiss: () -> Unit, onSave: (File) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val aspect = game.console.case.aspect
    val image: ImageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offX by remember { mutableFloatStateOf(0f) }
    var offY by remember { mutableFloatStateOf(0f) }
    var frame by remember { mutableStateOf(IntSize.Zero) }

    // Escala para que la foto cubra el marco, y límites para que nunca quede hueco dentro.
    fun baseScale() = if (frame.width == 0) 1f else max(frame.width / bitmap.width.toFloat(), frame.height / bitmap.height.toFloat())
    fun clamp() {
        val s = baseScale() * zoom
        val maxX = max(0f, (bitmap.width * s - frame.width) / 2)
        val maxY = max(0f, (bitmap.height * s - frame.height) / 2)
        offX = offX.coerceIn(-maxX, maxX); offY = offY.coerceIn(-maxY, maxY)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().padding(16.dp).background(Color(0xFF1E1612), RoundedCornerShape(14.dp)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.crop_title, game.console.name), color = Color(0xFFFFD27A), fontSize = 18.sp)
            Text(stringResource(R.string.crop_hint), color = Color(0xFFBFA98A), fontSize = 12.sp)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val h = minOf(maxHeight, maxWidth / aspect)
                Box(
                    Modifier.size(h * aspect, h).clipToBounds().border(2.dp, Color(0xFFFFD27A))
                        .pointerInput(bitmap) {
                            detectTransformGestures { _, pan, gesture, _ ->
                                zoom = (zoom * gesture).coerceIn(1f, 6f)
                                offX += pan.x; offY += pan.y
                                clamp()
                            }
                        },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        frame = IntSize(size.width.roundToInt(), size.height.roundToInt())
                        val s = baseScale() * zoom
                        val w = bitmap.width * s; val hh = bitmap.height * s
                        drawImage(
                            image,
                            dstOffset = IntOffset((size.width / 2 + offX - w / 2).roundToInt(), (size.height / 2 + offY - hh / 2).roundToInt()),
                            dstSize = IntSize(w.roundToInt(), hh.roundToInt()),
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { zoom = (zoom / 1.15f).coerceAtLeast(1f); clamp() }) { Text("−", color = Color.White, fontSize = 18.sp) }
                OutlinedButton(onClick = { zoom = (zoom * 1.15f).coerceAtMost(6f); clamp() }) { Text("+", color = Color.White, fontSize = 18.sp) }
                OutlinedButton(onClick = { zoom = 1f; offX = 0f; offY = 0f }) { Text(stringResource(R.string.crop_reset), color = Color.White) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Button(onClick = {
                    // Del marco a píxeles de la foto: un punto (x,y) respecto al centro del marco está en
                    // (ancho/2 + (x - offX)/s, alto/2 + (y - offY)/s) de la foto.
                    val s = baseScale() * zoom
                    val left = (bitmap.width / 2f + (-frame.width / 2f - offX) / s).roundToInt().coerceIn(0, bitmap.width - 1)
                    val top = (bitmap.height / 2f + (-frame.height / 2f - offY) / s).roundToInt().coerceIn(0, bitmap.height - 1)
                    val cw = (frame.width / s).roundToInt().coerceIn(1, bitmap.width - left)
                    val ch = (frame.height / s).roundToInt().coerceIn(1, bitmap.height - top)
                    val cropped = Bitmap.createBitmap(bitmap, left, top, cw, ch)
                    // Tamaño de carátula: 900 px de alto como mucho (nítida en la caja, poco peso).
                    val outH = minOf(ch, 900)
                    val scaled = Bitmap.createScaledBitmap(cropped, (outH * cw / ch.toFloat()).roundToInt().coerceAtLeast(1), outH, true)
                    val dir = File(context.filesDir, "covers").apply { mkdirs() }
                    val file = File(dir, "${game.id.replace(Regex("[^A-Za-z0-9_-]"), "_")}-${System.currentTimeMillis()}.jpg")
                    file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                    onSave(file)
                }) { Text(stringResource(R.string.save)) }
            }
        }
    }
}
