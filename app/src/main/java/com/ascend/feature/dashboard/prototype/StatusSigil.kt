package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
 * An original, geometric sigil system. Nothing here recreates a known franchise emblem —
 * each class is built from angular rings, radial marks, interlocking segments, rotating arc
 * fragments and layered polygons, tinted by its own accent. Driven entirely by
 * [StatusSigilState] so it is reusable across the prototype states.
 */

/** The sigil identity — kept separate from the class model so the sigil layer is standalone. */
enum class StatusSigilVariant(
    val core: Color,
    val glow: Color,
    val sides: Int,
    val spokes: Int,
) {
    NEUTRAL(Color(0xFF8AA0B8), Color(0xFF3D5068), sides = 6, spokes = 6),
    BERSERKER(Color(0xFFE8A33D), Color(0xFF7A4A10), sides = 3, spokes = 12),
    MONK(Color(0xFF3FD9C7), Color(0xFF0E6157), sides = 8, spokes = 8),
    MAGICIAN(Color(0xFF9B8CFF), Color(0xFF3B2E7A), sides = 5, spokes = 10),
    ;

    companion object {
        fun of(variant: StatusClassVariant): StatusSigilVariant =
            when (variant) {
                StatusClassVariant.NEUTRAL -> NEUTRAL
                StatusClassVariant.BERSERKER -> BERSERKER
                StatusClassVariant.MONK -> MONK
                StatusClassVariant.MAGICIAN -> MAGICIAN
            }
    }
}

/**
 * The sigil's full render state. [assembly] (0..1) grows the marks out from the centre on
 * entrance; [rotationDegrees] slowly turns the outer arc; [glow] pulses the luminance;
 * [progressionActive] and [majorUnlock] brighten and complete the emblem on big moments.
 */
data class StatusSigilState(
    val variant: StatusSigilVariant,
    val tier: Int,
    val assembly: Float = 1f,
    val rotationDegrees: Float = 0f,
    val glow: Float = 0.6f,
    val progressionActive: Boolean = false,
    val majorUnlock: Boolean = false,
)

/** The animation inputs for the sigil, produced by the screen's Animatables. */
data class StatusSigilAnimation(
    val assembly: Float,
    val rotationDegrees: Float,
    val glowPulse: Float,
)

/** Draws the soft radial aura behind the sigil (its own layer so quality can be dialled down). */
fun DrawScope.drawStatusSigilGlow(
    state: StatusSigilState,
    quality: Float = 1f,
) {
    if (quality <= 0f) return
    val radius = min(size.width, size.height) / 2f
    val strength = (0.25f + 0.55f * state.glow) * quality
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(state.variant.glow.copy(alpha = strength), Color.Transparent),
                center = center,
                radius = radius * 1.15f,
            ),
        radius = radius * 1.15f,
        center = center,
    )
}

/**
 * The sigil itself. Cheap to draw — a handful of stroked paths and lines, no per-frame
 * allocation beyond the rotation transform. The outer ring, radial spokes, class-specific
 * interior and a rotating arc fragment together read as one coherent emblem.
 */
@Composable
fun StatusSigil(
    state: StatusSigilState,
    animation: StatusSigilAnimation,
    modifier: Modifier = Modifier,
    effectsQuality: Float = 1f,
) {
    Canvas(modifier = modifier) {
        val r = min(size.width, size.height) / 2f
        val c = center
        val core = state.variant.core
        val assembled = animation.assembly.coerceIn(0f, 1f)
        val glow = (state.glow * 0.4f + 0.6f * animation.glowPulse).coerceIn(0f, 1f)
        val lineAlpha = 0.35f + 0.65f * glow

        drawStatusSigilGlow(state.copy(glow = glow), effectsQuality)

        // Layered outer polygon ring (angular ring), scaled up as the sigil assembles.
        val outer = polygonPath(c, r * (0.78f + 0.14f * assembled), state.variant.sides, rotationDeg = -90f)
        drawPath(outer, color = core.copy(alpha = lineAlpha), style = Stroke(width = r * 0.045f))
        val inner = polygonPath(c, r * 0.5f * assembled, state.variant.sides, rotationDeg = -90f + 180f / state.variant.sides)
        drawPath(inner, color = core.copy(alpha = lineAlpha * 0.7f), style = Stroke(width = r * 0.03f))

        // Radial marks (spokes) fanning out from a central gap.
        val spokes = state.variant.spokes
        for (i in 0 until spokes) {
            val ang = (2.0 * Math.PI * i / spokes)
            val from = c + Offset((cos(ang) * r * 0.30f).toFloat(), (sin(ang) * r * 0.30f).toFloat())
            val to = c + Offset((cos(ang) * r * 0.62f * assembled).toFloat(), (sin(ang) * r * 0.62f * assembled).toFloat())
            drawLine(core.copy(alpha = lineAlpha * 0.6f), from, to, strokeWidth = r * 0.02f, cap = StrokeCap.Round)
        }

        // Class-specific interior: interlocking layered polygons at half rotation.
        val interior = polygonPath(c, r * 0.34f * assembled, maxOf(3, state.variant.sides - 1), rotationDeg = 90f)
        drawPath(interior, color = core.copy(alpha = lineAlpha), style = Stroke(width = r * 0.03f))

        // Rotating arc fragment — the "alive" element (paused under reduced motion via rotation held).
        rotate(degrees = animation.rotationDegrees, pivot = c) {
            val arc = r * 0.92f
            drawArc(
                color = core.copy(alpha = lineAlpha),
                startAngle = -40f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(c.x - arc, c.y - arc),
                size = Size(arc * 2, arc * 2),
                style = Stroke(width = r * 0.05f, cap = StrokeCap.Round),
            )
            drawArc(
                color = core.copy(alpha = lineAlpha * 0.8f),
                startAngle = 150f,
                sweepAngle = 50f,
                useCenter = false,
                topLeft = Offset(c.x - arc, c.y - arc),
                size = Size(arc * 2, arc * 2),
                style = Stroke(width = r * 0.035f, cap = StrokeCap.Round),
            )
        }

        // Central core dot — brighter when a progression is active or a rank unlocked.
        val coreAlpha = if (state.progressionActive || state.majorUnlock) 1f else 0.7f
        drawCircle(core.copy(alpha = coreAlpha * glow), radius = r * (0.08f + 0.02f * assembled), center = c)
        if (state.majorUnlock) {
            drawCircle(core.copy(alpha = 0.5f * glow), radius = r * 0.16f * assembled, center = c, style = Stroke(width = r * 0.02f))
        }
    }
}

/** A regular polygon path with [sides] vertices on a circle of [radius], rotated for aesthetics. */
private fun polygonPath(
    center: Offset,
    radius: Float,
    sides: Int,
    rotationDeg: Float,
): Path {
    val path = Path()
    val rot = Math.toRadians(rotationDeg.toDouble())
    for (i in 0 until sides) {
        val ang = rot + 2.0 * Math.PI * i / sides
        val p = center + Offset((cos(ang) * radius).toFloat(), (sin(ang) * radius).toFloat())
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    return path
}
