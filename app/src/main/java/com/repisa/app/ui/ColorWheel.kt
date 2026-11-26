package com.repisa.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repisa.app.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Rueda de colores: el tono alrededor y la intensidad del centro (blanco) al borde, más el brillo debajo.
 * Se usa tocando o arrastrando; con el mando, ←/→ cambian el tono y ↑/↓ la intensidad.
 */
@Composable
fun ColorWheelDialog(title: String, initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toInt(), it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2].coerceAtLeast(0.05f)) }
    val color = Color.hsv(hue, sat, value)
    // Ventana ancha (rectangular): rueda grande, barra de brillo vertical y el color elegido.
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.widthIn(max = 760.dp).padding(horizontal = 24.dp),
        title = { Text(title) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                ColorWheel(hue, sat, value) { h, s -> hue = h; sat = s }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("☀", fontSize = 16.sp)
                    BrightnessBar(hue, sat, value) { value = it.coerceAtLeast(0.05f) }
                    Text("🌑", fontSize = 14.sp)
                }
                Column(Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(220.dp, 120.dp).background(color, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(12.dp)))
                    Text(stringResource(R.string.color_brightness) + ": ${(value * 100).toInt()} %", fontSize = 13.sp)
                    Text("#%06X".format(color.toArgbLong() and 0xFFFFFF), fontSize = 13.sp, color = Color(0xFFBFA98A))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(color.toArgbLong()) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun Color.toArgbLong(): Long = (this.toArgb().toLong() and 0xFFFFFFFFL)

@Composable
private fun ColorWheel(hue: Float, sat: Float, value: Float, onChange: (Float, Float) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    fun pick(p: Offset, size: Float) {
        val c = size / 2
        val dx = p.x - c; val dy = p.y - c
        val h = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 360) % 360).toFloat()
        val s = (hypot(dx, dy) / c).coerceIn(0f, 1f)
        onChange(h, s)
    }
    Canvas(
        Modifier.size(280.dp)
            .border(if (focused) 3.dp else 0.dp, Color(0xFFFFD27A), CircleShape)
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key) {
                    Key.DirectionLeft -> { onChange((hue - 6f + 360f) % 360f, sat); true }
                    Key.DirectionRight -> { onChange((hue + 6f) % 360f, sat); true }
                    Key.DirectionUp -> { onChange(hue, (sat + 0.05f).coerceAtMost(1f)); true }
                    Key.DirectionDown -> { onChange(hue, (sat - 0.05f).coerceAtLeast(0f)); true }
                    else -> false
                }
            }
            .focusable()
            .pointerInput(Unit) { detectTapGestures { pick(it, size.width.toFloat()) } }
            .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); pick(change.position, size.width.toFloat()) } },
    ) {
        val r = size.minDimension / 2
        // Tono alrededor y, del centro al borde, de blanco al color puro; oscurecido según el brillo.
        val hues = (0..12).map { Color.hsv((it * 30f) % 360f, 1f, 1f) }
        drawCircle(Brush.sweepGradient(hues, center), r)
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), center, r), r)
        drawCircle(Color.Black.copy(alpha = 1f - value), r)
        // Marcador del color elegido.
        val a = Math.toRadians(hue.toDouble())
        val p = Offset(center.x + (cos(a) * sat * r).toFloat(), center.y + (sin(a) * sat * r).toFloat())
        drawCircle(Color.hsv(hue, sat, value), 11.dp.toPx(), p)
        drawCircle(Color.White, 11.dp.toPx(), p, style = Stroke(3.dp.toPx()))
        drawCircle(Color.Black.copy(alpha = 0.5f), 13.dp.toPx(), p, style = Stroke(1.dp.toPx()))
    }
}

/** Brillo en vertical: arriba el color con todo su brillo, abajo negro. Tocar, arrastrar o ↑/↓ con el mando. */
@Composable
private fun BrightnessBar(hue: Float, sat: Float, value: Float, onChange: (Float) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Canvas(
        Modifier.size(44.dp, 260.dp)
            .border(if (focused) 3.dp else 1.dp, if (focused) Color(0xFFFFD27A) else Color(0x55FFFFFF), RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key) {
                    Key.DirectionUp -> { onChange((value + 0.05f).coerceAtMost(1f)); true }
                    Key.DirectionDown -> { onChange((value - 0.05f).coerceAtLeast(0f)); true }
                    else -> false
                }
            }
            .focusable()
            .pointerInput(Unit) { detectTapGestures { onChange(1f - (it.y / size.height).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); onChange(1f - (change.position.y / size.height).coerceIn(0f, 1f)) } },
    ) {
        val corner = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
        drawRoundRect(Brush.verticalGradient(listOf(Color.hsv(hue, sat, 1f), Color.Black)), cornerRadius = corner)
        val y = (1f - value) * size.height
        drawRoundRect(Color.White, topLeft = Offset(-2.dp.toPx(), y - 4.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(size.width + 4.dp.toPx(), 8.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
    }
}
