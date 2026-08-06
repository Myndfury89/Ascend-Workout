package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.util.lerp
import kotlin.math.sin

/*
 * CP4: the skill crest for the unlock menu, with a per-skill **reveal personality** driven by
 * [reveal] (0..1). All four resolve to the same readable crest at reveal=1 (so the menu is identical
 * once settled and under reduced motion, where the caller passes reveal=1):
 *  - Perception    (SCAN)          — a scan wipe reveals an eye/lens
 *  - Strength Boost (COMPRESS_SNAP)— compresses from oversize then snaps to place
 *  - Breath Control (BREATH_CYCLES)— expands/contracts twice, settling
 *  - Body Awareness (POINTS_ALIGN) — four points converge into an aligned diamond
 * Original geometry only (arcs / chevrons / rings / dots) — no letterforms or borrowed marks.
 */
@Composable
fun SkillCrest(
    skill: PrototypeSkill,
    accent: Color,
    reveal: Float,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val rv = reveal.coerceIn(0f, 1f)
        val glow = Color.White.copy(alpha = 0.55f + 0.25f * pulse.coerceIn(0f, 1f))
        val line = accent.copy(alpha = (0.6f + 0.3f * pulse).coerceAtMost(0.9f))
        when (skill.revealStyle) {
            SkillRevealStyle.SCAN -> drawPerception(rv, line, glow)
            SkillRevealStyle.COMPRESS_SNAP -> drawStrengthBoost(rv, line, glow)
            SkillRevealStyle.BREATH_CYCLES -> drawBreathControl(rv, line, glow)
            SkillRevealStyle.POINTS_ALIGN -> drawBodyAwareness(rv, line, glow)
        }
    }
}

/** Perception — an eye/lens (two arcs + iris + pupil) revealed by a left-to-right scan wipe. */
private fun DrawScope.drawPerception(
    rv: Float,
    line: Color,
    glow: Color,
) {
    val c = center
    val r = size.minDimension / 2f
    val a = rv // alpha ramps with the scan
    val box = Size(r * 1.6f, r * 1.2f)
    val tl = Offset(c.x - box.width / 2f, c.y - box.height / 2f)
    drawArc(line.copy(alpha = 0.8f * a), 200f, 140f, false, tl, box, style = Stroke(2f, cap = StrokeCap.Round))
    drawArc(line.copy(alpha = 0.8f * a), 20f, 140f, false, tl, box, style = Stroke(2f, cap = StrokeCap.Round))
    drawCircle(line.copy(alpha = 0.7f * a), r * 0.34f, c, style = Stroke(1.5f))
    drawCircle(glow.copy(alpha = glow.alpha * a), r * 0.12f, c)
    // The scan bar sweeps across while revealing; gone once settled.
    if (rv < 0.999f) {
        val x = c.x - r + 2f * r * rv
        drawLine(glow, Offset(x, c.y - r * 0.7f), Offset(x, c.y + r * 0.7f), strokeWidth = 1.5f)
    }
}

/** Strength Boost — three stacked upward chevrons that compress from oversize, then snap to place. */
private fun DrawScope.drawStrengthBoost(
    rv: Float,
    line: Color,
    glow: Color,
) {
    val c = center
    val r = size.minDimension / 2f
    // Compress: from 1.25x down to 1.0x with a tiny overshoot near the end.
    val overshoot = if (rv > 0.85f) 1f + (1f - rv) * 0.3f else lerp(1.25f, 1f, rv)
    scale(overshoot, overshoot, pivot = c) {
        for (k in 0..2) {
            val dy = -r * 0.5f + k * r * 0.5f
            val col = (if (k == 2) glow else line).copy(alpha = rv)
            drawLine(
                col,
                Offset(c.x - r * 0.55f, c.y + dy + r * 0.25f),
                Offset(c.x, c.y + dy - r * 0.25f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round,
            )
            drawLine(
                col,
                Offset(c.x, c.y + dy - r * 0.25f),
                Offset(c.x + r * 0.55f, c.y + dy + r * 0.25f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Breath Control — three concentric rings that expand/contract twice before settling. */
private fun DrawScope.drawBreathControl(
    rv: Float,
    line: Color,
    glow: Color,
) {
    val c = center
    val r = size.minDimension / 2f
    val breathe = 1f + 0.14f * sin(rv * (2f * Math.PI.toFloat()) * 2f) * (1f - rv)
    for (k in 0..2) {
        val rr = r * (0.35f + k * 0.28f) * breathe
        val col = (if (k == 0) glow else line).copy(alpha = rv * (1f - k * 0.18f))
        drawCircle(col, rr, c, style = Stroke(if (k == 0) 2f else 1.5f))
    }
}

/** Body Awareness — four points converge from the corners into an aligned diamond frame. */
private fun DrawScope.drawBodyAwareness(
    rv: Float,
    line: Color,
    glow: Color,
) {
    val c = center
    val r = size.minDimension / 2f
    val spread = r * 0.6f + (1f - rv) * r * 0.7f // points start far out, converge inward
    val pts =
        listOf(
            Offset(c.x, c.y - spread),
            Offset(c.x + spread, c.y),
            Offset(c.x, c.y + spread),
            Offset(c.x - spread, c.y),
        )
    // Connecting diamond fades in as the points align.
    val edgeAlpha = rv * rv
    for (i in pts.indices) {
        val p0 = pts[i]
        val p1 = pts[(i + 1) % pts.size]
        drawLine(line.copy(alpha = 0.7f * edgeAlpha), p0, p1, strokeWidth = 1.5f)
    }
    pts.forEach { drawCircle(glow.copy(alpha = glow.alpha * rv), 3f, it) }
    drawCircle(line.copy(alpha = 0.6f * rv), r * 0.14f, c, style = Stroke(1.5f))
}
