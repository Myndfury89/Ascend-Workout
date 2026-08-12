package com.ascend.feature.ascended.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ascend.feature.ascended.model.AscendedClass
import com.ascend.feature.ascended.model.BodyBase
import com.ascend.feature.ascended.model.EvolutionStage

/*
 * The production dark-field for a "Your Ascended" figure: a SELF-CONTAINED deep blue-violet ground +
 * a luminous [DarkFieldBacking] spotlight/floor sigil so the dark grayscale art reads, with the
 * figure composited on top. Deliberately depends on nothing in the dashboard prototype (no
 * StatusAtmosphere/fog/particles) — production must not reference prototype code. Static + reduced-
 * motion-safe: it introduces no continuous animation of its own at this checkpoint.
 */
private val FIELD_ACCENT = Color(0xFFA88BFF)
private val GROUND_DEEP = Color(0xFF04050B)
private val GROUND_TINT = Color(0xFF0E0B1E)
private val GROUND_NAVY = Color(0xFF080A16)

@Composable
fun AscendedStatusField(
    ascendedClass: AscendedClass?,
    bodyBase: BodyBase,
    modifier: Modifier = Modifier,
    stage: EvolutionStage = EvolutionStage.BASE,
    perceptionLevel: Int = 0,
    @Suppress("UNUSED_PARAMETER") reducedMotion: Boolean = false,
) {
    Box(modifier) {
        ProductionFieldBackground(Modifier.matchParentSize())
        DarkFieldBacking(FIELD_ACCENT, Modifier.matchParentSize())
        AscendedFigure(
            ascendedClass = ascendedClass,
            bodyBase = bodyBase,
            modifier = Modifier.matchParentSize(),
            stage = stage,
            perceptionLevel = perceptionLevel,
        )
    }
}

/** The self-contained dark holographic ground — a deep blue-violet radial, brighter toward centre. */
@Composable
private fun ProductionFieldBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(GROUND_TINT, GROUND_NAVY, GROUND_DEEP),
                    center = Offset(size.width * 0.5f, size.height * 0.34f),
                    radius = size.maxDimension * 0.8f,
                ),
        )
    }
}

/** A luminous spotlight + floor sigil that lifts a dark figure off the dark holographic field. */
@Composable
fun DarkFieldBacking(
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.fillMaxSize()) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.42f
        // Spotlight halo behind the figure — a lighter cool wash the dark silhouette reads against.
        drawRect(
            brush =
                Brush.radialGradient(
                    colors =
                        listOf(
                            Color(0.52f, 0.55f, 0.70f, 0.40f),
                            Color(0.22f, 0.24f, 0.38f, 0.16f),
                            Color.Transparent,
                        ),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.62f,
                ),
        )
        // Floor sigil at the feet — a flattened ceremonial ring pair.
        val fy = size.height * 0.90f
        val rx = size.width * 0.28f
        val ry = size.height * 0.032f
        drawOval(accent.copy(alpha = 0.32f), topLeft = Offset(cx - rx, fy - ry), size = Size(rx * 2f, ry * 2f), style = Stroke(width = 2f))
        drawOval(
            accent.copy(alpha = 0.18f),
            topLeft = Offset(cx - rx * 0.68f, fy - ry * 0.68f),
            size = Size(rx * 1.36f, ry * 1.36f),
            style = Stroke(width = 1.2f),
        )
    }
}
