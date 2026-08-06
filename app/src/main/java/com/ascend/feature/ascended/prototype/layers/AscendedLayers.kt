package com.ascend.feature.ascended.prototype.layers

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
import androidx.compose.ui.graphics.drawscope.rotateRad
import com.ascend.feature.ascended.prototype.model.AscendedState
import com.ascend.feature.dashboard.prototype.StatusAtmosphere
import com.ascend.feature.dashboard.prototype.StatusFog
import com.ascend.feature.dashboard.prototype.StatusParticleField
import kotlin.math.cos
import kotlin.math.sin

/*
 * The non-body layers of the "Your Ascended" viewport. CP1 ships the field background (reused Status
 * atmosphere) and the floor sigil beneath the character; the aura layer is present but silent at the
 * base state; and the class / equipment / Skill layers are structural seams that render nothing yet
 * (they gain real content in CP2/CP3). Keeping them in the stack now fixes the layer order so later
 * checkpoints only fill them in.
 */

/** Layer 1: the deep holographic field — reused Status atmosphere + fog + particles. */
@Composable
fun AscendedFieldBackground(
    sweep: Float,
    running: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        StatusAtmosphere(sweep = sweep)
        StatusFog(running = running)
        StatusParticleField(running = running)
    }
}

/** Layer 2: a ceremonial floor sigil under the character's feet — a flattened perspective ring set. */
@Composable
fun FloorSigilLayer(
    accent: Color,
    rotation: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.5f, size.height * 0.9f)
        val rx = size.width * 0.30f
        val ry = size.height * 0.045f
        // Two concentric flattened ellipses.
        drawOval(
            color = accent.copy(alpha = 0.28f),
            topLeft = Offset(center.x - rx, center.y - ry),
            size = Size(rx * 2f, ry * 2f),
            style = Stroke(width = 2f),
        )
        drawOval(
            color = accent.copy(alpha = 0.18f),
            topLeft = Offset(center.x - rx * 0.7f, center.y - ry * 0.7f),
            size = Size(rx * 1.4f, ry * 1.4f),
            style = Stroke(width = 1.2f),
        )
        // Radial ticks, slowly rotating in perspective (static under reduced motion via rotation=const).
        rotateRad(radians = rotation, pivot = center) {
            val ticks = 12
            for (i in 0 until ticks) {
                val a = (2.0 * Math.PI * i / ticks)
                val ox = center.x + (cos(a) * rx).toFloat()
                val oy = center.y + (sin(a) * ry).toFloat()
                val ix = center.x + (cos(a) * rx * 0.82f).toFloat()
                val iy = center.y + (sin(a) * ry * 0.82f).toFloat()
                drawLine(accent.copy(alpha = 0.22f), Offset(ix, iy), Offset(ox, oy), strokeWidth = 1.4f)
            }
        }
    }
}

/** Layer 3: the class aura. Silent at the base state (intensity 0); it never obscures the mannequin. */
@Composable
fun AuraLayer(
    state: AscendedState,
    accent: Color,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    if (!state.hasStrongAura) return
    Canvas(modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.5f, size.height * 0.42f)
        val radius = size.minDimension * (0.4f + 0.08f * pulse) * state.auraIntensity.coerceIn(0f, 1f)
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.12f * state.auraIntensity), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
            radius = radius,
            center = center,
        )
    }
}

/** Layer 5 seam: class silhouette modifier. CP2 fills this in; base state is class-neutral. */
@Composable
fun ClassSilhouetteLayer(
    state: AscendedState,
    modifier: Modifier = Modifier,
) {
    if (!state.hasClassIdentity) return
    // CP2: apply per-class silhouette/posture modification anchored to MannequinGeometry.attachPoints.
    Box(modifier)
}

/** Layer 6 seam: earned equipment overlays. CP3 fills this in; base state has no equipment. */
@Composable
fun EquipmentOverlayLayer(
    state: AscendedState,
    modifier: Modifier = Modifier,
) {
    if (!state.hasEquipment) return
    // CP3: draw earned garments / weapons / armour at the body attach points.
    Box(modifier)
}

/** Layer 7 seam: Skill manifestations. CP3+ fills this in; base state has no Skill levels. */
@Composable
fun SkillManifestationLayer(
    state: AscendedState,
    modifier: Modifier = Modifier,
) {
    if (state.skillLevels.isEmpty()) return
    // CP3/CP4: eye glow (Perception), force glow (Strength Boost), breath rhythm, body-node lines.
    Box(modifier)
}
