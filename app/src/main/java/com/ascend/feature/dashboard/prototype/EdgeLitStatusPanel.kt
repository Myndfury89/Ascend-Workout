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

/** The panel's lightly-clipped outline (top-right and bottom-left cut) as a reusable path. */
private fun panelPath(size: Size): Path {
    val cut = CUT
    return Path().apply {
        moveTo(0f, 0f)
        lineTo(size.width - cut, 0f)
        lineTo(size.width, cut)
        lineTo(size.width, size.height)
        lineTo(cut, size.height)
        lineTo(0f, size.height - cut)
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
) {
    val path = panelPath(size)
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
 */
@Composable
fun EdgeLitStatusPanel(
    accent: Color,
    materialize: Float,
    scan: Float,
    modifier: Modifier = Modifier,
    effectsQuality: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .drawBehind {
                    drawStatusPanelSurface(accent, materialize)
                }
                .drawWithContent {
                    drawContent()
                    drawStatusPanelScan(accent, scan, effectsQuality)
                    drawStatusPanelEdge(accent, materialize)
                },
        content = content,
    )
}
