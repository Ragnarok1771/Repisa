package com.repisa.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Barra de estado de la pantalla de abajo: reloj de 24 horas a la izquierda; a la derecha,
 * la cobertura de la red (tres barritas) y la batería con su porcentaje.
 * Sirve para consolas Android de doble pantalla que no tienen otra forma de verlo de un vistazo.
 */
@Composable
fun DeviceStatusBar(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically) {
        Clock24()
        Spacer(Modifier.weight(1f))
        NetworkBars()
        Spacer(Modifier.width(12.dp))
        BatteryIndicator()
    }
}

private val StatusText = Color(0xFFF2E6D0)

// ── Reloj ────────────────────────────────────────────────────────────────────

@Composable
private fun Clock24() {
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            // Espera justo hasta el cambio de minuto.
            val ms = 60_000L - (System.currentTimeMillis() % 60_000L)
            delay(ms + 50)
        }
    }
    val text = "%02d:%02d".format(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
    Text(text, color = StatusText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

// ── Batería ──────────────────────────────────────────────────────────────────

private data class BatteryState(val percent: Int, val charging: Boolean)

@Composable
private fun rememberBattery(): BatteryState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(BatteryState(100, false)) }
    DisposableEffect(context) {
        fun read(i: Intent?) {
            i ?: return
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            if (level >= 0) state = BatteryState(
                (level * 100 / scale).coerceIn(0, 100),
                status == BatteryManager.BATTERY_STATUS_CHARGING || (plugged != 0 && status != BatteryManager.BATTERY_STATUS_DISCHARGING),
            )
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = read(i)
        }
        // Es una difusión «pegajosa»: el registro devuelve ya el último estado.
        read(context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return state
}

@Composable
fun BatteryIndicator(modifier: Modifier = Modifier) {
    val b = rememberBattery()
    val color = when {
        b.charging -> Color(0xFF4CD964)
        b.percent <= 20 -> Color(0xFFFF4D4D)
        b.percent <= 50 -> Color(0xFFFFCC33)
        else -> Color(0xFF4CD964)
    }
    // Al cargar, el relleno sube en bucle desde el nivel actual hasta lleno, como en los teléfonos.
    val anim = rememberInfiniteTransition(label = "carga")
    val sweep by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "relleno")
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("${b.percent} %", color = if (b.percent <= 20 && !b.charging) color else StatusText,
            fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(width = 30.dp, height = 15.dp)) {
            Canvas(Modifier.size(width = 30.dp, height = 15.dp)) {
                val tipW = size.width * 0.09f
                val bodyW = size.width - tipW
                val stroke = 1.6.dp.toPx()
                val r = CornerRadius(3.dp.toPx())
                // Cuerpo y polo.
                drawRoundRect(StatusText, Offset(stroke / 2, stroke / 2), Size(bodyW - stroke, size.height - stroke), r, style = Stroke(stroke))
                drawRoundRect(StatusText, Offset(bodyW, size.height * 0.3f), Size(tipW, size.height * 0.4f), CornerRadius(1.5.dp.toPx()))
                // Relleno.
                val pad = stroke + 1.2.dp.toPx()
                val innerW = bodyW - pad * 2
                val base = b.percent / 100f
                val frac = if (b.charging) base + (1f - base) * sweep else base
                drawRoundRect(color, Offset(pad, pad), Size((innerW * frac).coerceAtLeast(innerW * 0.04f), size.height - pad * 2),
                    CornerRadius(1.5.dp.toPx()))
                // Rayito.
                if (b.charging) {
                    val cx = bodyW / 2; val h = size.height
                    val bolt = Path().apply {
                        moveTo(cx + h * 0.10f, h * 0.10f)
                        lineTo(cx - h * 0.28f, h * 0.56f)
                        lineTo(cx - h * 0.02f, h * 0.56f)
                        lineTo(cx - h * 0.12f, h * 0.90f)
                        lineTo(cx + h * 0.28f, h * 0.42f)
                        lineTo(cx + h * 0.02f, h * 0.42f)
                        close()
                    }
                    drawPath(bolt, Color.White)
                    drawPath(bolt, Color(0xFF1B3A1F), style = Stroke(0.8.dp.toPx()))
                }
            }
        }
    }
}

// ── Red ──────────────────────────────────────────────────────────────────────

/** 0 = sin conexión; 1–3 = barritas de señal. -1 = conectado pero sin salida a internet. */
@Composable
private fun rememberNetworkBars(): Int {
    val context = LocalContext.current
    var bars by remember { mutableIntStateOf(3) }
    LaunchedEffect(context) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        while (true) {
            bars = runCatching {
                val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
                when {
                    caps == null -> 0
                    !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> -1
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        var rssi = caps.signalStrength
                        @Suppress("DEPRECATION")
                        if (rssi == NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED || rssi >= 0)
                            rssi = wifi?.connectionInfo?.rssi ?: -50
                        when {
                            rssi >= -62 -> 3
                            rssi >= -74 -> 2
                            else -> 1
                        }
                    }
                    else -> 3 // cable u otra conexión sin medida de señal
                }
            }.getOrDefault(0)
            delay(3000)
        }
    }
    return bars
}

@Composable
private fun NetworkBars() {
    val bars = rememberNetworkBars()
    Canvas(Modifier.size(width = 20.dp, height = 15.dp)) {
        val gap = 2.5.dp.toPx()
        val w = (size.width - gap * 2) / 3
        val off = Color(0x55F2E6D0)
        val on = if (bars == -1) Color(0xFFFFCC33) else StatusText
        for (i in 0 until 3) {
            val h = size.height * (0.4f + 0.3f * i)
            val lit = bars == -1 || i < bars
            drawRoundRect(if (lit) on else off, Offset(i * (w + gap), size.height - h), Size(w, h), CornerRadius(1.dp.toPx()))
        }
        if (bars == 0) {
            // Sin conexión: una raya roja cruzando las barritas.
            drawLine(Color(0xFFFF4D4D), Offset(0f, size.height), Offset(size.width, 0f), 2.dp.toPx())
        }
    }
}
