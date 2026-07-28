package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/*
 * A wide, dramatic energy shell around the readable central panel. It is larger than the panel
 * (the content is inset), reads as partly physical / partly holographic, and carries the
 * strongest saturated energy in the composition — bright upper/lower rails, layered angular
 * corner structures with cyan concentrations, thin circuit lines, broken glowing fragments, a
 * faint outward glow, and a subtle rotational corner detail. It never fills with information —
 * it is a shell, not a second dashboard.
 */

private const val RAIL_INSET_DP = 26f
private const val SIDE_INSET_DP = 14f

/**
 * Draws the energy frame around the box bounds. [energy] (0..1) is the entrance reveal; [pulse]
 * (0..1) brightens it for major events; [rotation] turns the small corner arcs; [motion] keeps
 * fragments/rotation alive (false → a static but still-identifiable frame for minimal effects).
 */
fun DrawScope.drawEnergyFrame(
    energy: Float,
    pulse: Float,
    rotation: Float,
    motion: Boolean,
) {
    val e = energy.coerceIn(0f, 1f)
    val bright = (0.55f + 0.45f * pulse) * e
    val railY = RAIL_INSET_DP.dp.toPx()
    val sideX = SIDE_INSET_DP.dp.toPx()
    val w = size.width
    val h = size.height

    // Faint outward glow — layered translucent rounded rects expanding beyond the shell.
    for (i in 1..3) {
        val inset = -i * 5f
        drawRoundRect(
            color = StatusPalette.violet.copy(alpha = 0.05f * e / i),
            topLeft = Offset(sideX + inset, railY + inset),
            size = Size(w - 2 * (sideX + inset), h - 2 * (railY + inset)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
            style = Stroke(width = 2f),
        )
    }

    // Dim side edges (the frame is weakest on the sides, strongest top/bottom).
    drawLine(StatusPalette.indigo.copy(alpha = 0.5f * e), Offset(sideX, railY), Offset(sideX, h - railY), strokeWidth = 1.5f)
    drawLine(StatusPalette.indigo.copy(alpha = 0.5f * e), Offset(w - sideX, railY), Offset(w - sideX, h - railY), strokeWidth = 1.5f)

    drawRail(y = railY, width = w, sideX = sideX, bright = bright, top = true)
    drawRail(y = h - railY, width = w, sideX = sideX, bright = bright, top = false)

    // Layered angular corner structures + a bright cyan concentration + a rotating arc fragment.
    drawCorner(Offset(sideX, railY), 1f, 1f, bright, rotation, motion)
    drawCorner(Offset(w - sideX, railY), -1f, 1f, bright, rotation, motion)
    drawCorner(Offset(sideX, h - railY), 1f, -1f, bright, rotation, motion)
    drawCorner(Offset(w - sideX, h - railY), -1f, -1f, bright, rotation, motion)

    if (motion) drawFragments(w, h, railY, e)
}

/** A bright luminous rail near an edge, with thin circuit ticks — the heaviest visual weight. */
private fun DrawScope.drawRail(
    y: Float,
    width: Float,
    sideX: Float,
    bright: Float,
    top: Boolean,
) {
    val x0 = sideX + 34f
    val x1 = width - sideX - 34f
    // Soft under-glow band.
    drawRect(
        brush =
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, StatusPalette.violet.copy(alpha = 0.16f * bright), Color.Transparent),
                startY = y - 10f,
                endY = y + 10f,
            ),
        topLeft = Offset(x0, y - 10f),
        size = Size(x1 - x0, 20f),
    )
    // The bright rail line itself: cyan core fading to violet at the ends.
    drawLine(
        brush =
            Brush.horizontalGradient(
                listOf(
                    StatusPalette.violet.copy(alpha = 0.2f * bright),
                    StatusPalette.cyan.copy(alpha = 0.95f * bright),
                    StatusPalette.violet.copy(alpha = 0.2f * bright),
                ),
                startX = x0,
                endX = x1,
            ),
        start = Offset(x0, y),
        end = Offset(x1, y),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round,
    )
    // Thin circuit ticks hanging off the rail.
    val ticks = 7
    for (i in 0 until ticks) {
        val tx = x0 + (x1 - x0) * (i + 0.5f) / ticks
        val dir = if (top) 1f else -1f
        drawLine(StatusPalette.cyanSoft.copy(alpha = 0.5f * bright), Offset(tx, y), Offset(tx, y + dir * 6f), strokeWidth = 1f)
    }
}

/** Layered angular brackets + cyan corner concentration + a small rotating arc fragment. */
private fun DrawScope.drawCorner(
    origin: Offset,
    sx: Float,
    sy: Float,
    bright: Float,
    rotation: Float,
    motion: Boolean,
) {
    val armX = 40f
    val armY = 30f
    // Outer bracket.
    drawLine(
        StatusPalette.cyan.copy(alpha = 0.9f * bright),
        origin,
        origin + Offset(sx * armX, 0f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round,
    )
    drawLine(
        StatusPalette.cyan.copy(alpha = 0.9f * bright),
        origin,
        origin + Offset(0f, sy * armY),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round,
    )
    // Inner offset bracket (layered structure).
    val inner = origin + Offset(sx * 8f, sy * 8f)
    drawLine(StatusPalette.violetBright.copy(alpha = 0.6f * bright), inner, inner + Offset(sx * armX * 0.6f, 0f), strokeWidth = 1.5f)
    drawLine(StatusPalette.violetBright.copy(alpha = 0.6f * bright), inner, inner + Offset(0f, sy * armY * 0.6f), strokeWidth = 1.5f)
    // Bright energy concentration at the corner.
    drawCircle(StatusPalette.cyanSoft.copy(alpha = bright), radius = 3.5f, center = origin)
    drawCircle(StatusPalette.cyan.copy(alpha = 0.4f * bright), radius = 8f, center = origin)
    // A subtle rotating arc fragment just inside the corner.
    val arcCenter = origin + Offset(sx * 22f, sy * 22f)
    val r = 14f
    rotate(degrees = if (motion) rotation else 0f, pivot = arcCenter) {
        drawArc(
            color = StatusPalette.violetBright.copy(alpha = 0.7f * bright),
            startAngle = 20f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(arcCenter.x - r, arcCenter.y - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = 1.5f, cap = StrokeCap.Round),
        )
    }
}

/** Broken glowing fragments floating just outside the rails — uneven, holographic. */
private fun DrawScope.drawFragments(
    w: Float,
    h: Float,
    railY: Float,
    e: Float,
) {
    val count = 6
    for (i in 0 until count) {
        val onTop = i % 2 == 0
        val fx = w * (0.18f + 0.64f * (i / (count - 1f)))
        val fy = if (onTop) railY - 12f else h - railY + 12f
        val len = 10f + (i % 3) * 6f
        drawLine(
            StatusPalette.cyan.copy(alpha = (0.15f + 0.1f * (i % 3)) * e),
            Offset(fx - len / 2, fy),
            Offset(fx + len / 2, fy - 4f),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * The composed shell: draws [drawEnergyFrame] behind an inset content slot. The frame therefore
 * extends wider and taller than [content] (the readable central panel), which sits inside the
 * rails.
 */
@Composable
fun StatusEnergyFrame(
    energy: Float,
    pulse: Float,
    rotation: Float,
    frameMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .drawBehind { drawEnergyFrame(energy, pulse, rotation, frameMotion) }
                .padding(horizontal = (SIDE_INSET_DP + 8).dp, vertical = (RAIL_INSET_DP + 8).dp),
        content = content,
    )
}
