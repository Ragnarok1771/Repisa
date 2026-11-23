package com.repisa.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import kotlin.math.abs

/** Tamaño total (frontal + lateral 3D) que ocupa una caja de altura [height]. */
fun boxSize(game: Game, height: Dp): Pair<Dp, Dp> {
    val c = game.console.case
    val w = height * c.aspect
    val depth = height * c.depth * 0.55f
    return (w + depth) to (height + depth * 0.45f)
}

/**
 * Una caja de juego "física": portada al frente, lateral y tapa en falsa perspectiva,
 * franja del sistema (PS2, Switch...) y brillo de plástico.
 */
@Composable
fun GameBox(game: Game, height: Dp, modifier: Modifier = Modifier, lifted: Boolean = false) {
    val case = game.console.case
    val caseColor = Color(case.caseColor)
    val frontW = height * case.aspect
    val depth = height * case.depth * 0.55f
    val rise = depth * 0.45f
    val density = LocalDensity.current

    val lift by animateFloatAsState(if (lifted) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "lift")

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
                // Lateral derecho
                val side = Path().apply {
                    moveTo(fw, r); lineTo(fw + d, 0f); lineTo(fw + d, h - r); lineTo(fw, h); close()
                }
                drawPath(side, Brush.verticalGradient(listOf(lerp(caseColor, Color.Black, 0.25f), lerp(caseColor, Color.Black, 0.55f))))
                // Banda de la franja también en el lateral
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
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                .background(caseColor)
                .then(
                    if (lifted) Modifier.border(2.dp, Color(0xFFFFD27A), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    else Modifier
                ),
        ) {
            val plastic = case.caseColor != 0xFF2B2420 // las cajas de cartón no tienen marco de plástico
            Box(Modifier.fillMaxSize().padding(if (plastic) frontW * 0.035f else 0.dp)) {
                CoverArt(game)
            }
            case.bandColor?.let { band ->
                Box(
                    Modifier.fillMaxWidth().height(height * 0.075f).background(Color(band)),
                    contentAlignment = Alignment.Center,
                ) {
                    case.bandText?.let {
                        Text(
                            it, color = Color(case.bandTextColor), fontWeight = FontWeight.Bold,
                            fontSize = with(density) { (height * 0.045f).toSp() }, maxLines = 1, letterSpacing = 1.sp,
                        )
                    }
                }
            }
            // Bisagra del estuche (a la izquierda) y brillo del plástico
            Box(Modifier.width(frontW * 0.03f).fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.20f),
                        0.35f to Color.White.copy(alpha = 0.04f),
                        0.6f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.22f),
                        start = Offset.Zero, end = Offset.Infinite,
                    )
                )
            )
        }
    }
}

/** Portada con cadena de respaldo: portada → alternativa → portada generada. */
@Composable
fun BoxScope.CoverArt(game: Game) {
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
    val c1 = Color.hsv(hue, 0.55f, 0.55f)
    val c2 = Color.hsv((hue + 40) % 360, 0.65f, 0.25f)
    Box(
        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(c1, c2))).padding(6.dp),
    ) {
        Column(Modifier.align(Alignment.Center)) {
            Text(
                game.title, color = Color.White, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                fontSize = 13.sp, lineHeight = 14.sp, maxLines = 5, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            game.console.name.uppercase(), color = Color.White.copy(alpha = 0.7f), fontSize = 7.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
