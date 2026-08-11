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
    classGeometry: Boolean = false,
    internalGlow: Boolean = false,
    warmAccents: Boolean = false,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val radiusPx = with(density) { min(maxWidth.toPx(), maxHeight.toPx()) } / 2f
        val bucket = radiusBucket(radiusPx)
        val key = state.geometryKey(simplified, minimal, classGeometry)
        val geometry = remember(key, bucket) { buildOrnateGeometry(key, bucket.toFloat()) }

        Canvas(Modifier.matchParentSize().semantics { contentDescription = semanticDescription }) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawOrnateSigil(
                state, animation, geometry, center, minimal, frameMotion,
                rotationProfile, opacityProfile, stationaryOverlay, classGeometry, internalGlow, warmAccents,
            )
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
    classGeometry: Boolean,
    internalGlow: Boolean,
    warmAccents: Boolean,
) {
    val assembly = anim.assembly.coerceIn(0f, 1f)
    // A brief opacity bump mid-assembly (25–35%), settling back to the restrained default.
    val bump = sin(assembly * Math.PI.toFloat()).coerceIn(0f, 1f)
    val op = state.settledOpacity + (OrnateSigilState.ASSEMBLY_PEAK_OPACITY - state.settledOpacity) * bump
    val a = op * assembly
    val classCat = ClassSigilGeometryCatalog.forVariant(state.variant)
    // Per-class rotation feel (CP2): signed factor sets speed + direction (Mage reverses).
    val baseRot0 = if (frameMotion) anim.rotation else 0f
    val baseRot = if (classGeometry) baseRot0 * classCat.rotationFactor else baseRot0
    val w = opacityProfile.weights
    val style = SigilClassStyleCatalog.styleFor(state.variant)
    val weight = style.lineWeight
    // Class-distinct central strokes carry the per-class weight emphasis (Berserker heaviest).
    val centralWeight = if (classGeometry) weight * classCat.strokeScale else weight
    // Controlled internal glow: rises with the settled opacity + event glow, hard-capped so bright
    // areas never bloom out. Rendered as WIDE FAINT under-strokes beneath the crisp lines, so line
    // quality and shape are always preserved inside the glow. Concentrated on rings + central geometry.
    val glowE = if (internalGlow) (op * 0.7f + anim.glow * 0.6f).coerceAtMost(0.34f) else 0f
    val accent = accentBlend(state.variant, warmAccents)
    val proficiencyColor = classAccent(state.variant, warmAccents)

    translate(center.x, center.y) {
        // Layer 1 — background ornament (faintest): a soft aura.
        drawCircle(StatusPalette.violet.copy(alpha = a * w.aura), radius = geo.radius * 0.92f, center = Offset.Zero)

        // Internal glow bed — a soft radial pool of energy around the central geometry only.
        if (glowE > 0f) {
            drawCircle(
                brush =
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = glowE * 0.5f), Color.Transparent),
                        center = Offset.Zero,
                        radius = geo.radius * 0.4f,
                    ),
                radius = geo.radius * 0.4f,
                center = Offset.Zero,
            )
        }

        // Layer 2 — outer border motif (ornament). Rotates only under the legacy profile; the
        // ceremonial rule keeps the outer frame stationary.
        rotate(rotationProfile.outerMotifRotation(baseRot), pivot = Offset.Zero) {
            drawPath(geo.motif, StatusPalette.cyanSoft.copy(alpha = a * w.outerMotif), style = Stroke(width = 1f, cap = StrokeCap.Round))
        }
        // Structural ring tracks (CP-B: addressable instrumentation layers) — stationary, highest
        // ornamental opacity in RingForward. A wide faint under-stroke gives each major ring a
        // luminous halo without washing out the crisp line.
        val primaryRing = geo.instrumentation.ring(SigilRingRole.STRUCTURAL_PRIMARY)
        val secondaryRing = geo.instrumentation.ring(SigilRingRole.STRUCTURAL_SECONDARY)
        if (glowE > 0f) {
            primaryRing?.let { ringTrack(it.radius(geo.radius), StatusPalette.cyan.copy(alpha = glowE * 0.6f), weight = 7f) }
            secondaryRing?.let { ringTrack(it.radius(geo.radius), StatusPalette.cyanSoft.copy(alpha = glowE * 0.45f), weight = 5f) }
        }
        primaryRing?.let { ringTrack(it.radius(geo.radius), StatusPalette.infoLine.copy(alpha = a * w.ringPrimary), weight = it.weight) }
        secondaryRing?.let {
            ringTrack(
                it.radius(geo.radius),
                StatusPalette.cyanSoft.copy(alpha = a * w.ringSecondary),
                weight = it.weight,
                dashed = it.dashed,
            )
        }

        // Layer 3 — ceremonial middle geometry (the only drifting layer under the ceremonial rule).
        rotate(rotationProfile.middleRotation(baseRot), pivot = Offset.Zero) {
            // Wide faint under-stroke for the central class geometry — softly energized from within.
            if (glowE > 0f) {
                geo.innerPaths.forEach { path ->
                    drawPath(path, accent.copy(alpha = glowE * 0.5f), style = Stroke(width = centralWeight * 3f))
                }
            }
            geo.innerPaths.forEachIndexed { i, path ->
                val newest = state.newRankLayer && i >= geo.innerPaths.lastIndex - 1
                val la = if (newest) (a * 1.2f + anim.glow * 0.3f) else a
                drawPath(
                    path,
                    accent.copy(alpha = la.coerceAtMost(w.centralCap)),
                    style = Stroke(width = centralWeight),
                )
            }
            geo.connectors.forEach { (p0, p1) ->
                drawLine(StatusPalette.violetBright.copy(alpha = a * w.connectors), p0, p1, strokeWidth = weight * 0.7f)
            }
            // CP-C: the 12-mark inner support-glyph ring rides the middle group and rotates with it.
            geo.middleGroup?.let { mg ->
                drawPath(mg.supportRing, StatusPalette.cyanSoft.copy(alpha = a * w.arcs), style = Stroke(width = 1f, cap = StrokeCap.Round))
            }
        }
        // Static per-class core anchor (CP2) — holds the eye while the middle drifts. Slightly
        // emphasized and never rotated; only present in the class-distinct geometry.
        if (classGeometry) {
            geo.coreAnchor?.let { anchor ->
                val anchorAlpha = (a * 1.5f).coerceAtMost((w.centralCap + 0.12f).coerceAtMost(0.9f))
                drawPath(anchor, accent.copy(alpha = anchorAlpha), style = Stroke(width = centralWeight))
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

        // Layer 4 — live progress rings (CP-B: separate, never-flattened trim-driven layers). Player
        // ring solid; class ring dashed and inner — distinguishable by position, pattern, and thickness.
        geo.instrumentation.ring(SigilRingRole.PLAYER_XP)?.let { ring ->
            val playerFill = (op * w.playerMult).coerceAtMost(w.playerCap) + anim.glow * w.playerGlow
            ringArc(
                ring.radius(geo.radius),
                anim.playerRingTrim * assembly,
                StatusPalette.infoLine.copy(alpha = playerFill.coerceAtMost(w.playerFinalCap)),
                weight = ring.weight,
            )
        }
        if (state.classRing != null) {
            geo.instrumentation.ring(SigilRingRole.CLASS_XP)?.let { ring ->
                val classFill = (op * w.classMult).coerceAtMost(w.classCap) + anim.proficiencyPulse.minus(1f).coerceAtLeast(0f) * 0.8f
                ringArc(
                    ring.radius(geo.radius),
                    anim.classRingTrim * assembly,
                    StatusPalette.cyanSoft.copy(alpha = classFill.coerceAtMost(w.classFinalCap)),
                    weight = ring.weight,
                    dashed = ring.dashed,
                )
            }
        }

        // Layer 5 — medallions (CP-B: isolated addressable layers). Base moderate; the pulsing one is
        // the brightest temporary element, and each medallion's colour is swappable independently.
        geo.instrumentation.medallions.forEach { m ->
            val pulse = if (m.isProficiency) anim.proficiencyPulse else anim.medallionPulse.getOrElse(m.pulseIndex) { 1f }
            val color = medallionColor(m.role.ordinal, m.isProficiency, proficiencyColor)
            val extra = ((pulse - 1f) / 0.18f).coerceIn(0f, 1f) * 0.45f
            val mAlpha = (a * w.medallionMult + extra).coerceAtMost(w.medallionFinalCap)
            val mRadius = geo.medallionRadius * (if (m.isProficiency) 1.25f else 1f) * pulse
            drawMedallionRing(m.center, mRadius, color.copy(alpha = mAlpha * 0.7f), weight = 1.2f)
            drawMedallionGlyph(m.glyph, m.center, mRadius * 0.7f, color.copy(alpha = mAlpha), weight = weight * 0.7f)
        }

        // CP3 — attribute center-out wave: a thin circle scaling from the centre out toward the ring
        // structure and fading as it expands. Stationary (never rotates); restrained, not a flash.
        if (anim.wave > 0.001f && anim.wave < 0.999f) {
            val waveRadius = geo.radius * AttributeWaveCatalog.REACH_FRACTION * anim.wave
            drawCircle(accent.copy(alpha = 0.5f * (1f - anim.wave)), radius = waveRadius, center = Offset.Zero, style = Stroke(width = 2f))
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

private fun accentBlend(
    variant: StatusClassVariant,
    warm: Boolean,
): Color =
    when {
        // Refined-prototype warm treatment: Berserker's linework reads red-orange, not violet.
        warm && variant == StatusClassVariant.BERSERKER -> StatusPalette.ember
        variant == StatusClassVariant.BERSERKER -> StatusPalette.violetBright
        variant == StatusClassVariant.MONK -> StatusPalette.cyanSoft
        variant == StatusClassVariant.MAGE -> StatusPalette.violet
        else -> StatusPalette.infoLine
    }

private fun medallionColor(
    index: Int,
    proficiency: Boolean,
    proficiencyColor: Color,
): Color =
    if (proficiency) {
        proficiencyColor
    } else {
        AttributeAccent[AttributeOrder.getOrElse(index) { "Discipline" }] ?: StatusPalette.infoLine
    }
