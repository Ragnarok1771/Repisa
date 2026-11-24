package com.repisa.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.repisa.app.model.Game
import com.repisa.app.model.playStatus
import kotlin.math.abs

/** Proporción (ancho/alto) del frontal: la de la carátula real si ya se conoce, si no la típica del sistema. */
fun frontAspect(game: Game): Float = game.coverAspect?.coerceIn(0.5f, 1.5f) ?: game.console.case.aspect

/** Tamaño total (frontal + lateral 3D) que ocupa una caja de altura [height]. */
fun boxSize(game: Game, height: Dp): Pair<Dp, Dp> {
    val w = height * frontAspect(game)
    val depth = height * game.console.case.depth * 0.55f
    return (w + depth) to (height + depth * 0.45f)
}

/**
 * Una caja de juego "física": la carátula del juego ocupa todo el frontal, como la caja que compras
 * en la tienda; el lomo y la tapa se dibujan en falsa perspectiva con el color del estuche del sistema.
 * Si no hay carátula, se genera una con el título y la franja típica del sistema (PS2, Switch...).
 *
 * [onCoverAspect] recibe la proporción real de la carátula al cargarla, para ajustar la caja a ella.
 */
@Composable
fun GameBox(
    game: Game,
    height: Dp,
    modifier: Modifier = Modifier,
    lifted: Boolean = false,
    onCoverAspect: ((Float) -> Unit)? = null,
    showStatus: Boolean = LocalShowStatus.current,
) {
    val case = game.console.case
    val caseColor = Color(case.caseColor)
    val frontW = height * frontAspect(game)
    val depth = height * case.depth * 0.55f
    val rise = depth * 0.45f
    val density = LocalDensity.current
    var hasCover by remember(game.id, game.coverUrl) { mutableStateOf(false) }

    val lift by animateFloatAsState(if (lifted) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "lift")
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)

    Box(
        modifier
            .size(frontW + depth, height + rise)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1f)
                scaleX = 1f + 0.07f * lift
                scaleY = 1f + 0.07f * lift
                translationY = -with(density) { (height * 0.06f).toPx() } * lift
                rotationY = -6f * lift
                cameraDistance = 12f * density.density
            }
            .drawBehind {
                val d = depth.toPx()
                val r = rise.toPx()
                val fw = frontW.toPx()
                val h = size.height
                // Tapa superior
                val top = Path().apply {
                    moveTo(0f, r); lineTo(d, 0f); lineTo(fw + d, 0f); lineTo(fw, r); close()
                }
                drawPath(top, lerp(caseColor, Color.White, 0.18f))
                // Lomo (lateral derecho)
                val side = Path().apply {
                    moveTo(fw, r); lineTo(fw + d, 0f); lineTo(fw + d, h - r); lineTo(fw, h); close()
                }
                drawPath(side, Brush.verticalGradient(listOf(lerp(caseColor, Color.Black, 0.25f), lerp(caseColor, Color.Black, 0.55f))))
                case.bandColor?.let { band ->
                    val bh = h * 0.075f
                    val sideBand = Path().apply {
                        moveTo(fw, r); lineTo(fw + d, 0f); lineTo(fw + d, bh); lineTo(fw, r + bh); close()
                    }
                    drawPath(sideBand, lerp(Color(band), Color.Black, 0.3f))
                }
            },
    ) {
        Box(
            Modifier
                .offset(y = rise)
                .size(frontW, height)
                .clip(shape)
                .background(caseColor)
                .then(if (lifted) Modifier.border(2.dp, Color(0xFFFFD27A), shape) else Modifier),
        ) {
            CoverArt(game, onLoaded = { aspect ->
                hasCover = true
                val known = game.coverAspect
                if (onCoverAspect != null && (known == null || abs(known - aspect) > 0.02f)) onCoverAspect(aspect)
            })
            if (!hasCover) {
                // Portada generada: se le añade la franja del sistema para que parezca su caja.
                case.bandColor?.let { band ->
                    Box(
                        Modifier.fillMaxWidth().height(height * 0.085f).background(Color(band)),
                        contentAlignment = Alignment.Center,
                    ) {
                        case.bandText?.let {
                            // Tamaño que cabe siempre a lo ancho («PlayStation 2» en una caja estrecha): si no cabía,
                            // el texto se salía y se veía descentrado.
                            val size = minOf(height * 0.05f, frontW * 0.88f / (it.length * 0.62f).coerceAtLeast(1f))
                            Text(
                                it, color = Color(case.bandTextColor), fontWeight = FontWeight.Bold,
                                fontSize = with(density) { size.toSp() }, maxLines = 1, softWrap = false,
                                lineHeight = with(density) { size.toSp() }, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().wrapContentHeight(unbounded = true),
                            )
                        }
                    }
                }
            }
            // Borde del lomo y reflejo del plástico/cartón sobre la carátula
            Box(Modifier.width(frontW * 0.02f).fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.16f),
                        0.3f to Color.White.copy(alpha = 0.03f),
                        0.6f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.18f),
                        start = Offset.Zero, end = Offset.Infinite,
                    )
                )
            )
            // Cinta de estado en la esquina (pendiente, jugando, terminado, 100 %, abandonado).
            if (showStatus) game.playStatus?.let { StatusCorner(it, (frontW * 0.30f).coerceIn(14.dp, 44.dp)) }
        }
    }
}

/** Mostrar la cinta de estado en las cajas (ajuste de Pantalla). */
val LocalShowStatus = androidx.compose.runtime.staticCompositionLocalOf { true }

/** Triángulo de color en la esquina superior izquierda de la carátula, con el icono del estado. */
@Composable
fun StatusCorner(status: com.repisa.app.model.PlayStatus, size: Dp) {
    val color = Color(status.color)
    Box(
        Modifier.size(size).drawBehind {
            val tri = Path().apply { moveTo(0f, 0f); lineTo(this@drawBehind.size.width, 0f); lineTo(0f, this@drawBehind.size.height); close() }
            drawPath(tri, color)
            // Filo oscuro para que se separe de cualquier carátula
            drawLine(Color.Black.copy(alpha = 0.45f), Offset(this.size.width, 0f), Offset(0f, this.size.height), strokeWidth = 1.5f)
        },
    ) {
        val density = LocalDensity.current
        Text(
            status.icon, color = if (status == com.repisa.app.model.PlayStatus.BACKLOG || status == com.repisa.app.model.PlayStatus.PLAYING) Color(0xFF1C130D) else Color.White,
            fontWeight = FontWeight.Bold, fontSize = with(density) { (size * 0.34f).toSp() }, lineHeight = with(density) { (size * 0.34f).toSp() },
            modifier = Modifier.padding(start = size * 0.09f, top = size * 0.04f),
        )
    }
}

/** Carátula con cadena de respaldo: carátula → alternativa → portada generada. */
@Composable
fun BoxScope.CoverArt(game: Game, onLoaded: (aspect: Float) -> Unit = {}) {
    var url by remember(game.id, game.coverUrl) { mutableStateOf(game.coverUrl ?: game.altCoverUrl) }
    var failed by remember(game.id, game.coverUrl) { mutableStateOf(url == null) }
    if (failed) {
        GeneratedCover(game)
    } else {
        AsyncImage(
            model = url,
            contentDescription = game.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onSuccess = { state ->
                val size = state.painter.intrinsicSize
                if (size.width > 0 && size.height > 0) onLoaded(size.width / size.height)
            },
            onError = {
                if (url == game.coverUrl && game.altCoverUrl != null && game.altCoverUrl != url) url = game.altCoverUrl
                else failed = true
            },
        )
    }
}

/** Portada hecha a mano cuando no hay imagen: color según el título, nombre grande. */
@Composable
fun GeneratedCover(game: Game) {
    val hue = (abs(game.title.hashCode()) % 360).toFloat()
    val c1 = Color.hsv(hue, 0.50f, 0.72f)
    val c2 = Color.hsv((hue + 40) % 360, 0.60f, 0.34f)
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(c1, c2))).padding(6.dp)) {
        val density = LocalDensity.current
        val titleSize = with(density) { (minOf(maxWidth, maxHeight * 0.8f) * 0.14f).toSp() }
        val smallSize = with(density) { (minOf(maxWidth, maxHeight) * 0.06f).toSp() }
        Text(
            game.title, color = Color.White, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
            fontSize = titleSize, lineHeight = titleSize * 1.1f, maxLines = 5, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
        )
        Text(
            game.console.name.uppercase(), color = Color.White.copy(alpha = 0.75f), fontSize = smallSize,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
