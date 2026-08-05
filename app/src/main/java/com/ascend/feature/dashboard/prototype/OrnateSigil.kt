package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.min
import kotlin.math.sin

private val AttributeOrder = listOf("Strength", "Endurance", "Agility", "Discipline", "Recovery")

/**
 * The Original Ornate Sigil — a secondary, low-opacity atmospheric seal. Geometry is cached by
 * [SigilGeometryKey] + radius bucket (built once, not per frame); only cheap scalars animate.
 * Opacity is **layered**, never one global alpha: background ornament faintest, outer ring/motif
 * faint, rank geometry moderate, the active progress segment clearer, and a pulsing medallion the
 * brightest temporary element. A single [semanticDescription] carries the represented values to
 * accessibility services — the decorative lines are not exposed individually.
 */
@Composable
fun OrnateSigil(
    state: OrnateSigilState,
    animation: OrnateSigilAnimation,
    semanticDescription: String,
    modifier: Modifier = Modifier,
    simplified: Boolean = false,
    minimal: Boolean = false,
    frameMotion: Boolean = true,
    rotationProfile: SigilRotationProfile = SigilRotationProfile.LEGACY,
    opacityProfile: SigilOpacityProfile = SigilOpacityProfile.LEGACY,
    stationaryOverlay: Boolean = false,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val radiusPx = with(density) { min(maxWidth.toPx(), maxHeight.toPx()) } / 2f
        val bucket = radiusBucket(radiusPx)
        val key = state.geometryKey(simplified, minimal)
        val geometry = remember(key, bucket) { buildOrnateGeometry(key, bucket.toFloat()) }

        Canvas(Modifier.matchParentSize().semantics { contentDescription = semanticDescription }) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawOrnateSigil(state, animation, geometry, center, minimal, frameMotion, rotationProfile, opacityProfile, stationaryOverlay)
        }
    }
}

@Suppress("LongMethod") // one cohesive draw pass; splitting layers would spread shared state.
private fun DrawScope.drawOrnateSigil(
    state: OrnateSigilState,
    anim: OrnateSigilAnimation,
    geo: OrnateSigilGeometry,
    center: Offset,
    minimal: Boolean,
    frameMotion: Boolean,
    rotationProfile: SigilRotationProfile,
    opacityProfile: SigilOpacityProfile,
    stationaryOverlay: Boolean,
) {
    val assembly = anim.assembly.coerceIn(0f, 1f)
    // A brief opacity bump mid-assembly (25–35%), settling back to the restrained default.
    val bump = sin(assembly * Math.PI.toFloat()).coerceIn(0f, 1f)
    val op = state.settledOpacity + (OrnateSigilState.ASSEMBLY_PEAK_OPACITY - state.settledOpacity) * bump
    val a = op * assembly
    val baseRot = if (frameMotion) anim.rotation else 0f
    val w = opacityProfile.weights
    val style = SigilClassStyleCatalog.styleFor(state.variant)
    val weight = style.lineWeight

    translate(center.x, center.y) {
        // Layer 1 — background ornament (faintest): a soft aura.
        drawCircle(StatusPalette.violet.copy(alpha = a * w.aura), radius = geo.radius * 0.92f, center = Offset.Zero)

        // Layer 2 — outer border motif (ornament). Rotates only under the legacy profile; the
        // ceremonial rule keeps the outer frame stationary.
        rotate(rotationProfile.outerMotifRotation(baseRot), pivot = Offset.Zero) {
            drawPath(geo.motif, StatusPalette.cyanSoft.copy(alpha = a * w.outerMotif), style = Stroke(width = 1f, cap = StrokeCap.Round))
        }
        // Structural ring tracks — stationary instrumentation, highest ornamental opacity in RingForward.
        ringTrack(geo.radius * 0.72f, StatusPalette.infoLine.copy(alpha = a * w.ringPrimary), weight = 2f)
        ringTrack(geo.radius * 0.6f, StatusPalette.cyanSoft.copy(alpha = a * w.ringSecondary), weight = 1.4f, dashed = true)

        // Layer 3 — ceremonial middle geometry (the only drifting layer under the ceremonial rule).
        rotate(rotationProfile.middleRotation(baseRot), pivot = Offset.Zero) {
            geo.innerPaths.forEachIndexed { i, path ->
                val newest = state.newRankLayer && i >= geo.innerPaths.lastIndex - 1
                val la = if (newest) (a * 1.2f + anim.glow * 0.3f) else a
                drawPath(path, accentBlend(state.variant).copy(alpha = la.coerceAtMost(w.centralCap)), style = Stroke(width = weight))
            }
            geo.connectors.forEach { (p0, p1) ->
                drawLine(StatusPalette.violetBright.copy(alpha = a * w.connectors), p0, p1, strokeWidth = weight * 0.7f)
            }
        }
        // Inner detail arcs — reversed parallax against the middle under the ceremonial rule.
        if (!minimal) {
            rotate(rotationProfile.innerParallaxRotation(baseRot), pivot = Offset.Zero) {
                geo.arcs.forEach { arc ->
                    drawArc(
                        color = StatusPalette.cyan.copy(alpha = a * w.arcs),
                        startAngle = arc.startAngle,
                        sweepAngle = arc.sweep,
                        useCenter = false,
                        topLeft = Offset(-arc.radius, -arc.radius),
                        size = Size(arc.radius * 2, arc.radius * 2),
                        style = Stroke(width = weight * 0.6f, cap = StrokeCap.Round),
                    )
                }
            }
        }

        // Layer 4 — active progress segments (stationary instrumentation). Player ring solid; class
        // ring dashed and inner — distinguishable by position, pattern, and thickness, not colour alone.
        val playerFill = (op * w.playerMult).coerceAtMost(w.playerCap) + anim.glow * w.playerGlow
        ringArc(
            geo.radius * 0.72f,
            anim.playerRingTrim * assembly,
            StatusPalette.infoLine.copy(alpha = playerFill.coerceAtMost(w.playerFinalCap)),
            weight = 3f,
        )
        state.classRing?.let {
            val classFill = (op * w.classMult).coerceAtMost(w.classCap) + anim.proficiencyPulse.minus(1f).coerceAtLeast(0f) * 0.8f
            ringArc(
                geo.radius * 0.6f,
                anim.classRingTrim * assembly,
                StatusPalette.cyanSoft.copy(alpha = classFill.coerceAtMost(w.classFinalCap)),
                weight = 1.8f,
                dashed = true,
            )
        }

        // Layer 5 — medallions (stationary). Base moderate; the pulsing one is the brightest temporary element.
        geo.medallionCenters.forEachIndexed { i, c ->
            val isProficiency = i == geo.proficiencyIndex
            val pulse = if (isProficiency) anim.proficiencyPulse else anim.medallionPulse.getOrElse(i) { 1f }
            val glyph =
                if (isProficiency) {
                    style.proficiencyGlyph ?: MedallionGlyph.DISCIPLINE_SQUARES
                } else {
                    MedallionGlyph.attributeGlyphs.getOrElse(i) { MedallionGlyph.DISCIPLINE_SQUARES }
                }
            val color = medallionColor(i, isProficiency, state.variant)
            val extra = ((pulse - 1f) / 0.18f).coerceIn(0f, 1f) * 0.45f
            val mAlpha = (a * w.medallionMult + extra).coerceAtMost(w.medallionFinalCap)
            val mRadius = geo.medallionRadius * (if (isProficiency) 1.25f else 1f) * pulse
            drawMedallionRing(c, mRadius, color.copy(alpha = mAlpha * 0.7f), weight = 1.2f)
            drawMedallionGlyph(glyph, c, mRadius * 0.7f, color.copy(alpha = mAlpha), weight = weight * 0.7f)
        }

        // Debug-only review aid: mark which layers are stationary vs rotating. Fixed dots sit on the
        // structural rings (they never move); the bright dot rides the ceremonial middle (it orbits).
        if (stationaryOverlay) {
            drawCircle(StatusPalette.infoLine.copy(alpha = 0.9f), radius = 3f, center = Offset(0f, -geo.radius * 0.72f))
            drawCircle(StatusPalette.infoLine.copy(alpha = 0.9f), radius = 3f, center = Offset(0f, -geo.radius * 0.6f))
            rotate(rotationProfile.middleRotation(baseRot), pivot = Offset.Zero) {
                drawCircle(StatusPalette.cyan.copy(alpha = 0.95f), radius = 4.5f, center = Offset(0f, -geo.radius * 0.4f))
            }
        }
    }
}

private fun DrawScope.ringTrack(
    radius: Float,
    color: Color,
    weight: Float,
    dashed: Boolean = false,
) {
    drawCircle(
        color = color,
        radius = radius,
        center = Offset.Zero,
        style = Stroke(width = weight, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4f, 6f)) else null),
    )
}

private fun DrawScope.ringArc(
    radius: Float,
    trim: Float,
    color: Color,
    weight: Float,
    dashed: Boolean = false,
) {
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = 360f * trim.coerceIn(0f, 1f),
        useCenter = false,
        topLeft = Offset(-radius, -radius),
        size = Size(radius * 2, radius * 2),
        style =
            Stroke(
                width = weight,
                cap = StrokeCap.Round,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(5f, 5f)) else null,
            ),
    )
}

private fun accentBlend(variant: StatusClassVariant): Color =
    when (variant) {
        StatusClassVariant.BERSERKER -> StatusPalette.violetBright
        StatusClassVariant.MONK -> StatusPalette.cyanSoft
        StatusClassVariant.MAGICIAN -> StatusPalette.violet
        StatusClassVariant.NEUTRAL -> StatusPalette.infoLine
    }

private fun medallionColor(
    index: Int,
    proficiency: Boolean,
    variant: StatusClassVariant,
): Color =
    if (proficiency) {
        StatusSigilVariant.of(variant).core
    } else {
        AttributeAccent[AttributeOrder.getOrElse(index) { "Discipline" }] ?: StatusPalette.infoLine
    }
