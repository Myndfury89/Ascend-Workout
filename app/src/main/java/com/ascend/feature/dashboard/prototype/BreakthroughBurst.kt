package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/*
 * CP-D: the Ascension-tier level-up **breakthrough burst** — a 12-line radial burst (never a box).
 * Two implementations, selectable for comparison behind the prototype toggle:
 *  - PROCEDURAL: the rays are recomputed in Canvas every frame (cheap; fine for one-off events).
 *  - PRE_AUTHORED: the burst geometry is authored once as an ImageVector (built once via remember,
 *    the handoff's "pre-authored vector asset" for the Ascension tier), and only scale/alpha/tint
 *    animate — nothing is recomputed per frame. No new dependency (uses Compose's own ImageVector).
 *
 * NOTE: real FPS/thermal profiling needs on-device runs (Robolectric-only here). The burst is 12
 * lines + a ring — not the system's perf risk (the handoff flags particle-convergence for that);
 * both are provided so the reviewer can compare on real hardware and keep the better one.
 */

private const val BURST_VIEWPORT = 120f

@Composable
fun BreakthroughBurst(
    accent: Color,
    pulse: Float,
    mode: BurstMode,
    modifier: Modifier = Modifier,
) {
    when (mode) {
        BurstMode.PROCEDURAL -> ProceduralBurst(accent, pulse, modifier)
        BurstMode.PRE_AUTHORED -> {
            val vector = remember { breakthroughBurstVector() }
            val painter = rememberVectorPainter(vector)
            val scale = 0.85f + 0.15f * pulse.coerceIn(0f, 1f)
            Image(
                painter = painter,
                contentDescription = null,
                modifier =
                    modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                colorFilter = ColorFilter.tint(accent.copy(alpha = (0.4f + 0.3f * pulse).coerceAtMost(0.85f))),
            )
        }
    }
}

/** The per-frame Canvas rays (the original procedural burst). */
@Composable
private fun ProceduralBurst(
    accent: Color,
    pulse: Float,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val rays = 12
        val inner = size.minDimension * 0.18f
        val outer = size.minDimension * (0.42f + 0.06f * pulse)
        for (i in 0 until rays) {
            val a = 2.0 * Math.PI * i / rays
            val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
            drawLine(
                accent.copy(alpha = 0.35f + 0.25f * pulse),
                c + dir * inner,
                c + dir * outer,
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(StatusPalette.cyan.copy(alpha = 0.25f + 0.2f * pulse), radius = inner, style = Stroke(width = 1.5f))
    }
}

/**
 * The pre-authored burst geometry: 12 radial spokes (inner→outer) + one thin shockwave ring,
 * authored once as an ImageVector in a 120-unit viewport centred at (60,60). Stroked white so a
 * [ColorFilter.tint] can recolour it to the class accent.
 */
internal fun breakthroughBurstVector(): ImageVector {
    val center = BURST_VIEWPORT / 2f
    val rays = 12
    val inner = center * 0.18f
    val outer = center * 0.94f
    val ringR = center * 0.74f
    return ImageVector.Builder(
        defaultWidth = BURST_VIEWPORT.dp,
        defaultHeight = BURST_VIEWPORT.dp,
        viewportWidth = BURST_VIEWPORT,
        viewportHeight = BURST_VIEWPORT,
    ).apply {
        for (i in 0 until rays) {
            val a = 2.0 * Math.PI * i / rays
            val ca = cos(a).toFloat()
            val sa = sin(a).toFloat()
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(center + ca * inner, center + sa * inner)
                lineTo(center + ca * outer, center + sa * outer)
            }
        }
        // A single thin shockwave ring (two half-arcs make the full circle).
        path(stroke = SolidColor(Color.White), strokeLineWidth = 1.5f) {
            moveTo(center + ringR, center)
            arcToRelative(ringR, ringR, 0f, true, true, -2f * ringR, 0f)
            arcToRelative(ringR, ringR, 0f, true, true, 2f * ringR, 0f)
            close()
        }
    }.build()
}
