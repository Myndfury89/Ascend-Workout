package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/*
 * The main status surface, "carved out of darkness by light": a dark internal gradient with a
 * thin luminous edge, brighter at two corners, a soft scan line, and lightly clipped corners.
 * Deliberately NOT a Material card — no elevation, no fill, minimal inner dividers.
 */

/** One lit run of the panel's perimeter (used to bias brightness toward chosen corners). */
data class EdgeLightSegment(
    val startFraction: Float,
    val endFraction: Float,
    val brightness: Float,
)

private const val CUT = 22f

// When the panel frames itself (structure A), its lit outline is inset from the box edges by this
// margin so the outward glow, rails, and corner fragments have a band to occupy inside the box.
private const val FRAME_MARGIN_DP = 10f

/**
 * The panel's lightly-clipped outline (top-right and bottom-left cut) as a reusable path, optionally
 * [inset] from the box edges (used by the self-framing edge so its glow/rails sit in a margin band).
 */
private fun panelPath(
    size: Size,
    inset: Float = 0f,
): Path {
    val cut = CUT
    val l = inset
    val t = inset
    val r = size.width - inset
    val b = size.height - inset
    return Path().apply {
        moveTo(l, t)
        lineTo(r - cut, t)
        lineTo(r, t + cut)
        lineTo(r, b)
        lineTo(l + cut, b)
        lineTo(l, b - cut)
        close()
    }
}

/**
 * The dark internal fill with a subtle interior texture — a deep-navy radial (brighter toward the
 * upper-centre sigil), faint diagonal glass marks, and thin internal grid lines. Every texture is
 * kept very low-opacity so text stays fully legible. [accent] is the class tint for a whisper of
 * top haze.
 */
fun DrawScope.drawStatusPanelSurface(
    accent: Color,
    materialize: Float,
    inset: Float = 0f,
) {
    val path = panelPath(size, inset)
    drawPath(
        path = path,
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        Color(0xFF121627).copy(alpha = materialize),
                        Color(0xFF0B0E1B).copy(alpha = materialize),
                        Color(0xFF070810).copy(alpha = materialize),
                    ),
                center = Offset(size.width * 0.5f, size.height * 0.18f),
                radius = size.height * 1.1f,
            ),
    )
    clipPath(path) {
        // Thin internal grid lines — a faint technical substrate.
        val step = 26f
        var x = step
        while (x < size.width) {
            drawLine(Color(0xFF9FB0FF).copy(alpha = 0.02f * materialize), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += step
        }
        var y = step
        while (y < size.height) {
            drawLine(Color(0xFF9FB0FF).copy(alpha = 0.02f * materialize), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += step
        }
        // Faint diagonal glass marks.
        for (i in 0..4) {
            val off = size.width * (i / 4f)
            drawLine(
                StatusPalette.cyanSoft.copy(alpha = 0.025f * materialize),
                Offset(off, 0f),
                Offset(off - size.height * 0.5f, size.height),
                strokeWidth = 1.5f,
            )
        }
        // A whisper of accent haze inside the top edge.
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors = listOf(accent.copy(alpha = 0.06f * materialize), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.5f,
                ),
        )
    }
}

/**
 * The luminous panel edge — restrained overall (thin cyan/violet outline + soft outer glow) with
 * brighter corner intersections at the two clipped corners. The drama lives in the outer frame;
 * this keeps the reading surface clean.
 */
fun DrawScope.drawStatusPanelEdge(
    accent: Color,
    materialize: Float,
) {
    val path = panelPath(size)
    // Soft restrained outer glow.
    drawPath(path, color = StatusPalette.violet.copy(alpha = 0.10f * materialize), style = Stroke(width = 4.dp.toPx()))
    drawPath(path, color = StatusPalette.infoLine.copy(alpha = 0.22f * materialize), style = Stroke(width = 1.dp.toPx()))
    drawPath(path, color = StatusPalette.cyan.copy(alpha = 0.5f * materialize), style = Stroke(width = 1.5.dp.toPx()))
    // Brighter breaks + a small cross-mark at the two clipped corners.
    val bright = StatusPalette.cyanSoft.copy(alpha = 0.95f * materialize)
    drawLine(bright, Offset(size.width - CUT, 0f), Offset(size.width, CUT), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(bright, Offset(0f, size.height - CUT), Offset(CUT, size.height), strokeWidth = 3f, cap = StrokeCap.Round)
    drawCircle(bright, radius = 2.5f, center = Offset(size.width - CUT / 2, CUT / 2))
    drawCircle(bright, radius = 2.5f, center = Offset(CUT / 2, size.height - CUT / 2))
}

/**
 * The **self-framing** panel edge (structure A): the panel's own perimeter carries the dramatic
 * outer energy — a layered outward glow, bright cyan/violet top & bottom rails on the panel edges
 * with inward circuit ticks, angular corner concentrations with small rotating arcs, and bright
 * cross-breaks at the two clipped corners. There is no separate outer shell; this edge *is* the
 * frame. [energy] (0..1) is the entrance reveal, [pulse] brightens it for major events, [rotation]
 * turns the corner arcs, and [motion] keeps arcs/fragments alive (false → static but identifiable).
 * The frame carries the composition's saturated energy palette (violet/cyan), not the class accent.
 */
fun DrawScope.drawStatusPanelFrameEdge(
    energy: Float,
    pulse: Float,
    rotation: Float,
    motion: Boolean,
    inset: Float,
) {
    val e = energy.coerceIn(0f, 1f)
    if (e <= 0f) return
    val bright = (0.55f + 0.45f * pulse.coerceIn(0f, 1f)) * e
    val cut = CUT
    val l = inset
    val t = inset
    val r = size.width - inset
    val b = size.height - inset

    // Outward glow — layered translucent outlines expanding into the margin band.
    for (i in 1..3) {
        drawPath(panelPath(size, inset - i * 4f), color = StatusPalette.violet.copy(alpha = 0.06f * e / i), style = Stroke(width = 2f))
    }
    val path = panelPath(size, inset)
    // Base luminous outline.
    drawPath(path, color = StatusPalette.violet.copy(alpha = 0.14f * e), style = Stroke(width = 5.dp.toPx()))
    drawPath(path, color = StatusPalette.infoLine.copy(alpha = 0.26f * e), style = Stroke(width = 1.dp.toPx()))
    drawPath(path, color = StatusPalette.cyan.copy(alpha = 0.55f * bright), style = Stroke(width = 1.5.dp.toPx()))
    // Dim side edges (weakest on the sides, strongest top/bottom).
    drawLine(StatusPalette.indigo.copy(alpha = 0.45f * e), Offset(l, t + cut), Offset(l, b - cut), strokeWidth = 1.5f)
    drawLine(StatusPalette.indigo.copy(alpha = 0.45f * e), Offset(r, t + cut), Offset(r, b), strokeWidth = 1.5f)
    // Bright rails ON the panel's own top and bottom edges — the heaviest visual weight.
    drawFrameRail(l + cut, r - cut, t, bright, top = true)
    drawFrameRail(l + cut, r - cut, b, bright, top = false)
    // Angular corner concentrations + rotating arcs at the two square corners.
    drawFrameCorner(Offset(l, t + cut), 1f, 1f, bright, rotation, motion)
    drawFrameCorner(Offset(r, b), -1f, -1f, bright, rotation, motion)
    // Bright cross-breaks + marks at the two clipped corners (top-right, bottom-left).
    val breakColor = StatusPalette.cyanSoft.copy(alpha = 0.95f * e)
    drawLine(breakColor, Offset(r - cut, t), Offset(r, t + cut), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(breakColor, Offset(l, b - cut), Offset(l + cut, b), strokeWidth = 3f, cap = StrokeCap.Round)
    drawCircle(breakColor, radius = 2.5f, center = Offset(r - cut / 2, t + cut / 2))
    drawCircle(breakColor, radius = 2.5f, center = Offset(l + cut / 2, b - cut / 2))
    // Broken glowing fragments floating just outside the top/bottom rails.
    if (motion) {
        val count = 6
        for (i in 0 until count) {
            val onTop = i % 2 == 0
            val fx = l + (r - l) * (0.18f + 0.64f * (i / (count - 1f)))
            val fy = if (onTop) t - 8f else b + 8f
            val len = 9f + (i % 3) * 5f
            drawLine(
                StatusPalette.cyan.copy(alpha = (0.14f + 0.09f * (i % 3)) * e),
                Offset(fx - len / 2, fy),
                Offset(fx + len / 2, fy - 3f),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** A bright luminous rail along one horizontal panel edge, cyan-cored, with inward circuit ticks. */
private fun DrawScope.drawFrameRail(
    x0: Float,
    x1: Float,
    y: Float,
    bright: Float,
    top: Boolean,
) {
    drawRect(
        brush =
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, StatusPalette.violet.copy(alpha = 0.18f * bright), Color.Transparent),
                startY = y - 9f,
                endY = y + 9f,
            ),
        topLeft = Offset(x0, y - 9f),
        size = Size(x1 - x0, 18f),
    )
    drawLine(
        brush =
            Brush.horizontalGradient(
                listOf(
                    StatusPalette.violet.copy(alpha = 0.25f * bright),
                    StatusPalette.cyan.copy(alpha = 0.95f * bright),
                    StatusPalette.violet.copy(alpha = 0.25f * bright),
                ),
                startX = x0,
                endX = x1,
            ),
        start = Offset(x0, y),
        end = Offset(x1, y),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round,
    )
    val ticks = 7
    for (i in 0 until ticks) {
        val tx = x0 + (x1 - x0) * (i + 0.5f) / ticks
        val dir = if (top) 1f else -1f
        drawLine(StatusPalette.cyanSoft.copy(alpha = 0.45f * bright), Offset(tx, y), Offset(tx, y + dir * 6f), strokeWidth = 1f)
    }
}

/** An angular corner bracket + a bright energy concentration + a small rotating arc fragment. */
private fun DrawScope.drawFrameCorner(
    origin: Offset,
    sx: Float,
    sy: Float,
    bright: Float,
    rotation: Float,
    motion: Boolean,
) {
    val arm = 30f
    drawLine(
        StatusPalette.cyan.copy(alpha = 0.9f * bright),
        origin,
        origin + Offset(sx * arm, 0f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round,
    )
    drawLine(
        StatusPalette.cyan.copy(alpha = 0.9f * bright),
        origin,
        origin + Offset(0f, sy * arm),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round,
    )
    drawCircle(StatusPalette.cyanSoft.copy(alpha = bright), radius = 3f, center = origin)
    drawCircle(StatusPalette.cyan.copy(alpha = 0.35f * bright), radius = 7f, center = origin)
    val c = origin + Offset(sx * 20f, sy * 20f)
    val r = 12f
    rotate(degrees = if (motion) rotation else 0f, pivot = c) {
        drawArc(
            color = StatusPalette.violetBright.copy(alpha = 0.65f * bright),
            startAngle = 20f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(c.x - r, c.y - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = 1.5f, cap = StrokeCap.Round),
        )
    }
}

/** A soft horizontal scan line sweeping down the panel; [progress] is 0..1 (its Y position). */
fun DrawScope.drawStatusPanelScan(
    accent: Color,
    progress: Float,
    quality: Float = 1f,
) {
    if (quality <= 0f) return
    val y = size.height * progress
    drawRect(
        brush =
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, accent.copy(alpha = 0.10f * quality), Color.Transparent),
                startY = y - 40f,
                endY = y + 40f,
            ),
        topLeft = Offset(0f, y - 40f),
        size = Size(size.width, 80f),
    )
}

/**
 * The composed edge-lit panel: dark surface, luminous edge, optional scan, lightly-clipped
 * corners, and a content slot. [materialize] (0..1) is the entrance reveal; [scan] (0..1) is
 * the scan position; [effectsQuality] dials the scan down (0 disables it).
 *
 * When [selfFraming] is set (structure A — used in production), the panel's own edge carries the
 * dramatic outer energy frame instead of a restrained outline: the surface insets by a margin and
 * [drawStatusPanelFrameEdge] draws bright rails, corners, glow, and fragments on the panel's own
 * perimeter, driven by [frameEnergy] / [framePulse] / [frameRotation] / [frameMotion]. Left off by
 * default so the prototype's frame-plus-panel composition is unchanged.
 */
@Composable
fun EdgeLitStatusPanel(
    accent: Color,
    materialize: Float,
    scan: Float,
    modifier: Modifier = Modifier,
    effectsQuality: Float = 1f,
    selfFraming: Boolean = false,
    frameEnergy: Float = 0f,
    framePulse: Float = 0f,
    frameRotation: Float = 0f,
    frameMotion: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .drawBehind {
                    val inset = if (selfFraming) FRAME_MARGIN_DP.dp.toPx() else 0f
                    drawStatusPanelSurface(accent, materialize, inset)
                }
                .drawWithContent {
                    drawContent()
                    if (selfFraming) {
                        val inset = FRAME_MARGIN_DP.dp.toPx()
                        clipPath(panelPath(size, inset)) { drawStatusPanelScan(accent, scan, effectsQuality) }
                        drawStatusPanelFrameEdge(frameEnergy, framePulse, frameRotation, frameMotion, inset)
                    } else {
                        drawStatusPanelScan(accent, scan, effectsQuality)
                        drawStatusPanelEdge(accent, materialize)
                    }
                },
        content = content,
    )
}
