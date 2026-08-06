package com.ascend.feature.dashboard.prototype

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Precomputed sigil geometry, cached by [SigilGeometryKey] + a radius bucket so complex Paths are
 * built once (via remember in the composable) rather than every frame. Only cheap scalars —
 * alpha, scale, rotation, trim, glow, pulse — animate against this cached geometry. Also hosts the
 * drawing of the original medallion glyphs (basic geometry only; never text or pseudo-script).
 */

data class ArcSpec(
    val radius: Float,
    val startAngle: Float,
    val sweep: Float,
)

/** All shapes for one sigil, in local space with the origin at the sigil centre. */
class OrnateSigilGeometry(
    val radius: Float,
    val innerPaths: List<Path>,
    val connectors: List<Pair<Offset, Offset>>,
    val arcs: List<ArcSpec>,
    val motif: Path,
    val motifTicks: Int,
    val medallionCenters: List<Offset>,
    val medallionRadius: Float,
    val proficiencyIndex: Int,
    /** The static per-class core anchor (CP2). Null for the legacy/undifferentiated geometry. */
    val coreAnchor: Path? = null,
    /** CP-B: the addressable outer-instrumentation layer set (static rings + isolated medallions + live XP rings). */
    val instrumentation: SigilInstrumentation = SigilInstrumentation(emptyList(), emptyList()),
    /** CP-C: the per-class rotating middle group (support ring + central structure). Null for legacy. */
    val middleGroup: SigilMiddleGroup? = null,
    /** CP-C: the data-driven rank-detail overlay, composed on top of the middle group (procedural). */
    val rankOverlay: List<Path> = emptyList(),
)

/** The 12-mark inner support-glyph ring (short radial ticks) between the class ring and the central star. */
private fun supportGlyphRing(r: Float): Path =
    Path().apply {
        val ringR = r * 0.46f
        val marks = 12
        for (i in 0 until marks) {
            val a = 2.0 * Math.PI * i / marks - Math.PI / 2
            val ca = cos(a).toFloat()
            val sa = sin(a).toFloat()
            moveTo(ca * (ringR - r * 0.02f), sa * (ringR - r * 0.02f))
            lineTo(ca * (ringR + r * 0.02f), sa * (ringR + r * 0.02f))
        }
    }

private fun circlePath(
    center: Offset,
    radius: Float,
): Path = Path().apply { addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)) }

/**
 * The distinct central rotating structure for a class (CP2). Shape language only — each reads
 * differently in monochrome. [sides] feeds the neutral/legacy polygon fallback.
 */
private fun classCentralPaths(
    structure: CentralStructure,
    r: Float,
    sides: Int,
): List<Path> =
    when (structure) {
        // Berserker — one bold five-point star with long sharp spikes (aggressive, 5-fold), grounded
        // by a small inner pentagon so it never reads as a generic many-pointed star.
        CentralStructure.FIVE_POINT_STAR ->
            listOf(
                starPath(5, r * 0.38f, r * 0.10f, -90f),
                polygonPath(5, r * 0.14f, -90f + 36f),
            )
        // Monk — a flat-sided hexagon (the distinctive read) enclosing an inscribed hexagram: 6-fold
        // disciplined symmetry, calm and geometric rather than spiky.
        CentralStructure.HEXAGRAM ->
            listOf(
                polygonPath(6, r * 0.30f, -90f),
                polygonPath(3, r * 0.28f, -90f),
                polygonPath(3, r * 0.28f, 90f),
            )
        // Magician — an all-curves six-petal rosette inside a bounding circle: flowing, circular,
        // no straight lines anywhere.
        CentralStructure.ROSETTE ->
            buildList {
                val petals = 6
                val off = r * 0.17f
                val cr = r * 0.17f
                for (k in 0 until petals) {
                    val a = -Math.PI / 2 + 2.0 * Math.PI * k / petals
                    add(circlePath(Offset((cos(a) * off).toFloat(), (sin(a) * off).toFloat()), cr))
                }
                add(circlePath(Offset.Zero, r * 0.30f))
            }
        // Neutral — the legacy polygon + star look, so an unselected class stays understated.
        CentralStructure.LAYERED_POLYGON ->
            listOf(
                polygonPath(sides, r * 0.32f, -90f),
                starPath(sides, r * 0.28f, r * 0.13f, -90f),
            )
    }

/** The small static core anchor per class (CP2). No squares/diamonds. */
private fun coreAnchorPath(
    anchor: CoreAnchorShape,
    r: Float,
): Path =
    when (anchor) {
        CoreAnchorShape.SPIKE_STAR_4 -> starPath(4, r * 0.11f, r * 0.028f, -90f)
        CoreAnchorShape.HEXAGON -> polygonPath(6, r * 0.085f, -90f)
        CoreAnchorShape.CIRCLE -> circlePath(Offset.Zero, r * 0.075f)
        CoreAnchorShape.RING_DOT -> circlePath(Offset.Zero, r * 0.05f)
    }

private fun polygonPath(
    sides: Int,
    radius: Float,
    rotationDeg: Float,
): Path =
    Path().apply {
        val rot = Math.toRadians(rotationDeg.toDouble())
        for (i in 0 until sides) {
            val a = rot + 2.0 * Math.PI * i / sides
            val p = Offset((cos(a) * radius).toFloat(), (sin(a) * radius).toFloat())
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }

private fun starPath(
    points: Int,
    outer: Float,
    inner: Float,
    rotationDeg: Float,
): Path =
    Path().apply {
        val rot = Math.toRadians(rotationDeg.toDouble())
        val steps = points * 2
        for (i in 0 until steps) {
            val r = if (i % 2 == 0) outer else inner
            val a = rot + Math.PI * i / points
            val p = Offset((cos(a) * r).toFloat(), (sin(a) * r).toFloat())
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }

/** Builds (and the caller caches) the full geometry for a key at a bucketed radius. */
fun buildOrnateGeometry(
    key: SigilGeometryKey,
    radiusPx: Float,
): OrnateSigilGeometry {
    val cfg = RankGeometryCatalog.configFor(key.rankTier)
    val style = SigilClassStyleCatalog.styleFor(key.variant)
    val classGeo = ClassSigilGeometryCatalog.forVariant(key.variant)
    val r = radiusPx
    val innerPaths = mutableListOf<Path>()
    var middleGroup: SigilMiddleGroup? = null
    var rankOverlay: List<Path> = emptyList()

    if (key.classDistinct) {
        // CP2/CP-C: the class's distinct central silhouette leads (the middle group), and the
        // data-driven rank overlay adds only small nested inner detail on top — no full-size rank
        // polygon that would blur the class shape. The two are kept as separate addressable layers.
        val central = classCentralPaths(classGeo.central, r, cfg.basePolygonSides)
        val overlay =
            (0 until cfg.nestedPolygons).map { i ->
                polygonPath(cfg.basePolygonSides + 1, r * (0.15f - i * 0.035f).coerceAtLeast(0.06f), -90f)
            }
        innerPaths += central
        innerPaths += overlay
        middleGroup = SigilMiddleGroup(supportRing = supportGlyphRing(r), central = central)
        rankOverlay = overlay
    } else {
        // Legacy (current production) construction — unchanged.
        innerPaths += polygonPath(cfg.basePolygonSides, r * 0.34f, -90f)
        for (layer in 0 until cfg.starLayers) {
            val rot = -90f + layer * 18f
            innerPaths += starPath(cfg.basePolygonSides, r * (0.30f - layer * 0.04f), r * (0.15f - layer * 0.02f).coerceAtLeast(0.05f), rot)
        }
        for (i in 0 until cfg.rotatedPolygons) {
            innerPaths += polygonPath(cfg.basePolygonSides, r * 0.34f, -90f + (i + 1) * (180f / (cfg.rotatedPolygons + 1)))
        }
        for (i in 0 until cfg.nestedPolygons) {
            innerPaths += polygonPath(cfg.basePolygonSides + 1, r * (0.22f - i * 0.05f).coerceAtLeast(0.08f), -90f)
        }
    }

    // Radial connectors from the inner geometry out toward the ring.
    val connectors =
        (0 until cfg.radialConnectors).map { i ->
            val a = (2.0 * Math.PI * i / cfg.radialConnectors.coerceAtLeast(1))
            Offset((cos(a) * r * 0.34f).toFloat(), (sin(a) * r * 0.34f).toFloat()) to
                Offset((cos(a) * r * 0.5f).toFloat(), (sin(a) * r * 0.5f).toFloat())
        }

    // Segmented arcs (fragmented ring pieces), spaced around.
    val arcs =
        (0 until cfg.segmentedArcs).map { i ->
            val start = -90f + i * (360f / cfg.segmentedArcs.coerceAtLeast(1))
            ArcSpec(radius = r * 0.5f, startAngle = start + 6f, sweep = (360f / cfg.segmentedArcs.coerceAtLeast(1)) - 12f)
        }

    // Abstract, non-linguistic border motif: alternating long/short ticks with small nested
    // triangles, repeated around the outer radius. Density scales with rank, capped in minimal.
    val ticks =
        if (key.minimal) {
            0
        } else if (key.simplified) {
            cfg.motifTicks / 2
        } else {
            cfg.motifTicks
        }
    val motif = Path()
    val outerR = r * 0.9f
    for (i in 0 until ticks) {
        val a = 2.0 * Math.PI * i / ticks
        val ca = cos(a).toFloat()
        val sa = sin(a).toFloat()
        val long = i % 3 == 0
        val inR = outerR - if (long) r * 0.06f else r * 0.03f
        motif.moveTo(ca * inR, sa * inR)
        motif.lineTo(ca * outerR, sa * outerR)
        if (long && !key.simplified) {
            // A small nested chevron fragment on the long ticks (original, non-script).
            val bx = ca * (outerR - r * 0.03f)
            val by = sa * (outerR - r * 0.03f)
            val perp = Offset(-sa, ca)
            motif.moveTo(bx - perp.x * r * 0.02f, by - perp.y * r * 0.02f)
            motif.lineTo(ca * outerR, sa * outerR)
            motif.lineTo(bx + perp.x * r * 0.02f, by + perp.y * r * 0.02f)
        }
    }

    // Medallion positions on a mid-outer ring, using stable normalized angles.
    val hasProf = key.medallionCount > MedallionGlyph.attributeGlyphs.size
    val angles = medallionAngles(key.medallionCount, hasProf)
    val medRingR = r * 0.7f
    val centers = angles.map { Offset(cos(it) * medRingR, sin(it) * medRingR) }
    val proficiencyIndex = if (hasProf) centers.lastIndex else -1
    val medallionRadius = r * (if (style.useArcs) 0.085f else 0.09f)

    return OrnateSigilGeometry(
        radius = r,
        innerPaths = innerPaths,
        connectors = connectors,
        arcs = arcs,
        motif = motif,
        motifTicks = ticks,
        medallionCenters = centers,
        medallionRadius = medallionRadius,
        proficiencyIndex = proficiencyIndex,
        coreAnchor = if (key.classDistinct) coreAnchorPath(classGeo.anchor, r) else null,
        instrumentation =
            SigilInstrumentation.build(
                centers = centers,
                proficiencyIndex = proficiencyIndex,
                proficiencyGlyph = style.proficiencyGlyph ?: MedallionGlyph.DISCIPLINE_SQUARES,
            ),
        middleGroup = middleGroup,
        rankOverlay = rankOverlay,
    )
}

/** Radius bucket for the geometry cache — avoids rebuilds on sub-pixel size changes. */
fun radiusBucket(radiusPx: Float): Int = (radiusPx / 16f).roundToInt() * 16

/**
 * Draws one original medallion glyph from basic geometry (lines / arcs / polygons / dots). No
 * character, letterform, rune, or symbol is ever produced.
 */
fun DrawScope.drawMedallionGlyph(
    glyph: MedallionGlyph,
    center: Offset,
    radius: Float,
    color: Color,
    weight: Float,
) {
    val s = Stroke(width = weight, cap = StrokeCap.Round)

    fun line(
        a: Offset,
        b: Offset,
    ) = drawLine(color, center + a, center + b, strokeWidth = weight, cap = StrokeCap.Round)
    val u = radius
    when (glyph) {
        MedallionGlyph.STRENGTH_WEDGE -> {
            // Two stacked upward wedges.
            for (k in 0..1) {
                val dy = -u * 0.15f + k * u * 0.5f
                line(Offset(-u * 0.55f, dy + u * 0.3f), Offset(0f, dy - u * 0.3f))
                line(Offset(0f, dy - u * 0.3f), Offset(u * 0.55f, dy + u * 0.3f))
            }
        }
        MedallionGlyph.ENDURANCE_BARS -> {
            for (k in -1..1) {
                val y = k * u * 0.32f
                line(Offset(-u * 0.5f, y), Offset(u * 0.5f, y))
            }
        }
        MedallionGlyph.AGILITY_DART -> {
            line(Offset(-u * 0.5f, u * 0.45f), Offset(u * 0.35f, -u * 0.5f))
            line(Offset(-u * 0.35f, -u * 0.15f), Offset(u * 0.5f, u * 0.2f))
        }
        MedallionGlyph.DISCIPLINE_SQUARES -> {
            drawPath(squarePath(center, u * 0.5f, 0f), color, style = s)
            drawPath(squarePath(center, u * 0.34f, 45f), color, style = s)
        }
        MedallionGlyph.RECOVERY_CRADLE -> {
            drawArc(color, 20f, 140f, false, topLeft = center + Offset(-u * 0.5f, -u * 0.2f), size = Size(u, u), style = s)
            drawCircle(color, radius = weight * 0.9f, center = center + Offset(0f, -u * 0.35f))
        }
        MedallionGlyph.FORCE_BURST -> {
            for (k in 0..2) {
                val a = -Math.PI / 2 + k * (2 * Math.PI / 3)
                line(Offset.Zero, Offset((cos(a) * u * 0.55f).toFloat(), (sin(a) * u * 0.55f).toFloat()))
            }
        }
        MedallionGlyph.BODY_RINGLET -> {
            drawCircle(color, radius = u * 0.42f, center = center, style = s)
            drawCircle(color, radius = weight, center = center + Offset(-u * 0.55f, 0f))
            drawCircle(color, radius = weight, center = center + Offset(u * 0.55f, 0f))
        }
        MedallionGlyph.ENERGY_FLOW -> {
            drawArc(color, 200f, 160f, false, topLeft = center + Offset(-u * 0.5f, -u * 0.5f), size = Size(u, u), style = s)
            drawArc(color, 20f, 160f, false, topLeft = center + Offset(-u * 0.3f, -u * 0.3f), size = Size(u * 0.6f, u * 0.6f), style = s)
        }
    }
}

fun DrawScope.drawMedallionRing(
    center: Offset,
    radius: Float,
    color: Color,
    weight: Float,
) = drawCircle(color, radius = radius, center = center, style = Stroke(width = weight))

private fun squarePath(
    center: Offset,
    half: Float,
    rotationDeg: Float,
): Path =
    Path().apply {
        val rot = Math.toRadians(rotationDeg.toDouble())
        val pts =
            listOf(
                Offset(-half, -half),
                Offset(half, -half),
                Offset(half, half),
                Offset(-half, half),
            ).map {
                val x = it.x * cos(rot).toFloat() - it.y * sin(rot).toFloat()
                val y = it.x * sin(rot).toFloat() + it.y * cos(rot).toFloat()
                center + Offset(x, y)
            }
        moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
        close()
    }

/** Convenience: run [block] translated so the sigil's local origin sits at [center]. */
inline fun DrawScope.atSigilCenter(
    center: Offset,
    block: DrawScope.() -> Unit,
) = translate(center.x, center.y) { block() }
