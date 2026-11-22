package com.ascend.core.designsystem.sigil

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/*
 * Data-driven geometry for the proficiency glyphs of the classes on the shared line style (Assassin,
 * Fighter, Ranger, Guardian), approved by Matt 11/21. Original basic geometry only — the same
 * line / arc / dot vocabulary as the other medallion glyphs; no new drawing primitive. All values are
 * in glyph units (u = the glyph radius), +y down, origin at the medallion centre.
 */

/** One stroke of a data-driven medallion glyph. */
sealed interface GlyphStroke {
    /** Connected round-capped line segments through [points]. */
    data class Polyline(
        val points: List<Offset>,
    ) : GlyphStroke

    /** Open round-capped arc on the circle at [center] with [radius]; angles as `drawArc`. */
    data class Arc(
        val center: Offset,
        val radius: Float,
        val startDeg: Float,
        val sweepDeg: Float,
    ) : GlyphStroke

    /** Filled dot of radius `radiusU * u + radiusWeight * strokeWeight`. */
    data class Dot(
        val center: Offset,
        val radiusU: Float = 0f,
        val radiusWeight: Float = 0f,
    ) : GlyphStroke
}

object ProficiencyGlyphGeometry {
    /** Assassin — Precision: top-left and bottom-right corner brackets (arm 0.3u) framing a centre dot. */
    val precisionBrackets: List<GlyphStroke> =
        listOf(
            GlyphStroke.Polyline(
                listOf(Offset(x = -0.5f, y = -0.2f), Offset(x = -0.5f, y = -0.5f), Offset(x = -0.2f, y = -0.5f)),
            ),
            GlyphStroke.Polyline(
                listOf(Offset(x = 0.5f, y = 0.2f), Offset(x = 0.5f, y = 0.5f), Offset(x = 0.2f, y = 0.5f)),
            ),
            GlyphStroke.Dot(center = Offset.Zero, radiusWeight = 1.2f),
        )

    /** Fighter — Combat Rhythm: flat lead, one spike up, one spike down, flat tail. */
    val combatPulse: List<GlyphStroke> =
        listOf(
            GlyphStroke.Polyline(
                listOf(
                    Offset(x = -0.62f, y = 0.05f),
                    Offset(x = -0.30f, y = 0.05f),
                    Offset(x = -0.14f, y = -0.42f),
                    Offset(x = 0.06f, y = 0.42f),
                    Offset(x = 0.22f, y = 0.05f),
                    Offset(x = 0.62f, y = 0.05f),
                ),
            ),
        )

    /** Ranger — Pursuit: rising arc (centre (0.1, 0.5), r 0.65, 190° +95°) and a lead dot ahead on its path. */
    val pursuitTrajectory: List<GlyphStroke> =
        listOf(
            GlyphStroke.Arc(center = Offset(x = 0.1f, y = 0.5f), radius = 0.65f, startDeg = 190f, sweepDeg = 95f),
            GlyphStroke.Dot(center = Offset(x = 0.46f, y = -0.4f), radiusU = 0.085f),
        )

    /** Guardian — Fortitude: battlement outline, three merlons over an open base. */
    val fortitudeRampart: List<GlyphStroke> =
        listOf(
            GlyphStroke.Polyline(
                listOf(
                    Offset(x = -0.55f, y = 0.38f),
                    Offset(x = -0.55f, y = -0.30f),
                    Offset(x = -0.30f, y = -0.30f),
                    Offset(x = -0.30f, y = -0.10f),
                    Offset(x = -0.11f, y = -0.10f),
                    Offset(x = -0.11f, y = -0.30f),
                    Offset(x = 0.11f, y = -0.30f),
                    Offset(x = 0.11f, y = -0.10f),
                    Offset(x = 0.30f, y = -0.10f),
                    Offset(x = 0.30f, y = -0.30f),
                    Offset(x = 0.55f, y = -0.30f),
                    Offset(x = 0.55f, y = 0.38f),
                ),
            ),
        )

    private val byGlyph: Map<MedallionGlyph, List<GlyphStroke>> =
        mapOf(
            MedallionGlyph.PRECISION_BRACKETS to precisionBrackets,
            MedallionGlyph.COMBAT_PULSE to combatPulse,
            MedallionGlyph.PURSUIT_TRAJECTORY to pursuitTrajectory,
            MedallionGlyph.FORTITUDE_RAMPART to fortitudeRampart,
        )

    /** The glyphs drawn from data here. */
    val glyphs: Set<MedallionGlyph> get() = byGlyph.keys

    fun strokesFor(glyph: MedallionGlyph): List<GlyphStroke> = byGlyph[glyph].orEmpty()
}

/** Draws data-driven glyph [strokes] scaled by [u] around [center]. */
fun DrawScope.drawGlyphStrokes(
    strokes: List<GlyphStroke>,
    center: Offset,
    u: Float,
    color: Color,
    weight: Float,
) {
    strokes.forEach { stroke ->
        when (stroke) {
            is GlyphStroke.Polyline ->
                stroke.points.zipWithNext().forEach { (a, b) ->
                    drawLine(color, center + a * u, center + b * u, strokeWidth = weight, cap = StrokeCap.Round)
                }
            is GlyphStroke.Arc -> {
                val r = stroke.radius * u
                drawArc(
                    color = color,
                    startAngle = stroke.startDeg,
                    sweepAngle = stroke.sweepDeg,
                    useCenter = false,
                    topLeft = center + stroke.center * u - Offset(r, r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = weight, cap = StrokeCap.Round),
                )
            }
            is GlyphStroke.Dot ->
                drawCircle(
                    color,
                    radius = stroke.radiusU * u + stroke.radiusWeight * weight,
                    center = center + stroke.center * u,
                )
        }
    }
}
