package com.ascend.feature.ascended.prototype

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
import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase
import com.ascend.feature.ascended.prototype.model.EvolutionStage
import com.ascend.feature.dashboard.prototype.StatusAtmosphere
import com.ascend.feature.dashboard.prototype.StatusFog
import com.ascend.feature.dashboard.prototype.StatusPalette
import com.ascend.feature.dashboard.prototype.StatusParticleField

/*
 * The dark-field integration: the imported "Your Ascended" figure composited onto the holographic
 * Status field (deep atmosphere + drifting fog/particles) rather than the flat light review surface.
 * Because the figure art is dark grayscale on a dark ground, a luminous [DarkFieldBacking] "spotlight"
 * + floor sigil sits BEHIND the figure so its silhouette reads. Ambient motion (fog/particles) is
 * gated so reduced motion — and headless render tests — resolve to a static, readable field.
 */
private val FIELD_ACCENT = StatusPalette.violetBright

@Composable
fun AscendedStatusField(
    ascendedClass: AscendedClass,
    bodyBase: BodyBase,
    modifier: Modifier = Modifier,
    stage: EvolutionStage = EvolutionStage.BASE,
    perceptionLevel: Int = 0,
    ambient: Boolean = true,
    sweep: Float = 0.35f,
) {
    Box(modifier) {
        StatusAtmosphere(sweep = sweep)
        if (ambient) {
            StatusFog(running = true)
            StatusParticleField(running = true)
        }
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
