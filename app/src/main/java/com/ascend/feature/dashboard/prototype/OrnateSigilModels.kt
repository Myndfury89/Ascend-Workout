package com.ascend.feature.dashboard.prototype

/*
 * Data model for the Original Ornate Sigil System. Everything is driven by explicit,
 * configurable data (a rank-geometry catalog, an original-glyph set, and per-class styles) —
 * the renderer never derives complexity from a rank *name* or branches on a class id. It is a
 * secondary, low-opacity atmospheric element behind the readable Status information; all values
 * it represents also appear as text elsewhere in the panel.
 */

/** The nine rank tiers, lowest to highest. Complexity is mapped by data, not by this name. */
enum class RankTier {
    INITIATE,
    IRON,
    BRONZE,
    SILVER,
    GOLD,
    VANGUARD,
    ASCENDANT,
    APEX,
    MYTHIC,
}

/**
 * A data-driven description of a rank's inner geometry. Complexity increases through several
 * independent dials — not line count alone — so higher ranks can add rotation, nodes, nested
 * polygons, radial intersections, or segmented arcs. [complexityScore] is a single monotonic
 * summary used for ordering/tests and must rise with rank.
 */
data class RankGeometryConfig(
    val basePolygonSides: Int,
    val starLayers: Int,
    val rotatedPolygons: Int,
    val nestedPolygons: Int,
    val radialConnectors: Int,
    val segmentedArcs: Int,
    val motifTicks: Int,
) {
    val complexityScore: Int
        get() =
            basePolygonSides + starLayers * 3 + rotatedPolygons * 2 +
                nestedPolygons * 2 + radialConnectors + segmentedArcs + motifTicks / 4
}

/**
 * The configurable rank → geometry mapping. Starting visual mappings per the design brief;
 * kept as data so the whole progression can be retuned without touching the renderer.
 */
object RankGeometryCatalog {
    private val table: Map<RankTier, RankGeometryConfig> =
        mapOf(
            // Initiate: minimal four-point construction.
            RankTier.INITIATE to RankGeometryConfig(basePolygonSides = 4, starLayers = 0, rotatedPolygons = 0, nestedPolygons = 0, radialConnectors = 0, segmentedArcs = 0, motifTicks = 12),
            // Iron: simple star with one interlocking layer.
            RankTier.IRON to RankGeometryConfig(basePolygonSides = 5, starLayers = 1, rotatedPolygons = 0, nestedPolygons = 0, radialConnectors = 0, segmentedArcs = 0, motifTicks = 16),
            // Bronze: additional rotated polygon.
            RankTier.BRONZE to RankGeometryConfig(basePolygonSides = 5, starLayers = 1, rotatedPolygons = 1, nestedPolygons = 0, radialConnectors = 0, segmentedArcs = 2, motifTicks = 20),
            // Silver: second interlocking star layer.
            RankTier.SILVER to RankGeometryConfig(basePolygonSides = 6, starLayers = 2, rotatedPolygons = 1, nestedPolygons = 0, radialConnectors = 0, segmentedArcs = 3, motifTicks = 24),
            // Gold: denser interior segmentation.
            RankTier.GOLD to RankGeometryConfig(basePolygonSides = 6, starLayers = 2, rotatedPolygons = 1, nestedPolygons = 1, radialConnectors = 4, segmentedArcs = 4, motifTicks = 28),
            // Vanguard: additional radial connectors.
            RankTier.VANGUARD to RankGeometryConfig(basePolygonSides = 7, starLayers = 2, rotatedPolygons = 2, nestedPolygons = 1, radialConnectors = 7, segmentedArcs = 5, motifTicks = 32),
            // Ascendant: finer interior polygon layer.
            RankTier.ASCENDANT to RankGeometryConfig(basePolygonSides = 8, starLayers = 3, rotatedPolygons = 2, nestedPolygons = 2, radialConnectors = 8, segmentedArcs = 6, motifTicks = 36),
            // Apex: high-complexity multi-layer construction.
            RankTier.APEX to RankGeometryConfig(basePolygonSides = 9, starLayers = 3, rotatedPolygons = 3, nestedPolygons = 2, radialConnectors = 10, segmentedArcs = 8, motifTicks = 40),
            // Mythic: maximum approved complexity.
            RankTier.MYTHIC to RankGeometryConfig(basePolygonSides = 10, starLayers = 4, rotatedPolygons = 3, nestedPolygons = 3, radialConnectors = 12, segmentedArcs = 10, motifTicks = 48),
        )

    fun configFor(tier: RankTier): RankGeometryConfig = table.getValue(tier)
}

/**
 * The closed set of **original** medallion glyphs, each built only from basic geometry (lines,
 * arcs, polygons, dots). Deliberately not derived from — and not resembling — any real
 * alchemical, astrological, occult, religious, runic, or known-game symbol. The five universal
 * attributes plus the three class-proficiency marks.
 */
enum class MedallionGlyph {
    // Universal attributes.
    STRENGTH_WEDGE, // stacked upward wedges — upward force
    ENDURANCE_BARS, // three level bars in a ring — sustained output
    AGILITY_DART, // offset crossed slashes — quickness
    DISCIPLINE_SQUARES, // square with an inscribed rotated square — order
    RECOVERY_CRADLE, // downward arc cradle under a dot — rest

    // Class proficiencies.
    FORCE_BURST, // three short thick radial spokes — Berserker Force
    BODY_RINGLET, // balanced ring flanked by two dots — Monk Body Mastery
    ENERGY_FLOW, // nested double arc — Magician Energy Control
    ;

    companion object {
        /** The five universal attribute glyphs, in canonical attribute order. */
        val attributeGlyphs: List<MedallionGlyph> =
            listOf(STRENGTH_WEDGE, ENDURANCE_BARS, AGILITY_DART, DISCIPLINE_SQUARES, RECOVERY_CRADLE)

        fun forAttribute(name: String): MedallionGlyph =
            when (name) {
                "Strength" -> STRENGTH_WEDGE
                "Endurance" -> ENDURANCE_BARS
                "Agility" -> AGILITY_DART
                "Discipline" -> DISCIPLINE_SQUARES
                "Recovery" -> RECOVERY_CRADLE
                else -> DISCIPLINE_SQUARES
            }
    }
}

/**
 * How a class shapes the *shared* sigil structure — never a separate seal. Class choice changes
 * only central-geometry style, accent emphasis, line weight, arc-vs-straight behaviour, and
 * assembly rhythm; the ring/medallion/motif structure is identical across classes.
 */
data class SigilClassStyle(
    val lineWeight: Float,
    val useArcs: Boolean,
    val pointEmphasis: Float,
    val assemblyRhythm: Float,
    val proficiencyGlyph: MedallionGlyph?,
)

object SigilClassStyleCatalog {
    fun styleFor(variant: StatusClassVariant): SigilClassStyle =
        when (variant) {
            StatusClassVariant.BERSERKER ->
                SigilClassStyle(
                    lineWeight = 2.6f,
                    useArcs = false,
                    pointEmphasis = 1.25f,
                    assemblyRhythm = 0.8f,
                    proficiencyGlyph = MedallionGlyph.FORCE_BURST,
                )
            StatusClassVariant.MONK ->
                SigilClassStyle(
                    lineWeight = 1.7f,
                    useArcs = false,
                    pointEmphasis = 1.0f,
                    assemblyRhythm = 1.0f,
                    proficiencyGlyph = MedallionGlyph.BODY_RINGLET,
                )
            StatusClassVariant.MAGICIAN ->
                SigilClassStyle(
                    lineWeight = 1.5f,
                    useArcs = true,
                    pointEmphasis = 0.9f,
                    assemblyRhythm = 1.15f,
                    proficiencyGlyph = MedallionGlyph.ENERGY_FLOW,
                )
            StatusClassVariant.NEUTRAL ->
                SigilClassStyle(lineWeight = 1.9f, useArcs = false, pointEmphasis = 1.0f, assemblyRhythm = 1.0f, proficiencyGlyph = null)
        }
}

/**
 * Which sigil layers rotate. [LEGACY] is the current production behaviour (the outer border motif
 * and the rank geometry both drift). [CEREMONIAL_MIDDLE] enforces the handoff rule: **only the
 * ceremonial middle rotates** — the outer frame, the progress instrumentation, tick rings, and
 * medallions stay stationary — with a subtle reversed inner parallax. Progress instrumentation never
 * rotates in either profile.
 */
enum class SigilRotationProfile {
    LEGACY,
    CEREMONIAL_MIDDLE,
    ;

    /** Outer border/bezel motif rotation. Stationary under the ceremonial rule. */
    fun outerMotifRotation(rot: Float): Float = if (this == LEGACY) rot else 0f

    /** The ceremonial middle (rank star / interlocking geometry) — the only layer that drifts. */
    fun middleRotation(rot: Float): Float = if (this == LEGACY) rot * 0.5f else rot

    /** Inner detail drift; reversed against the middle for a restrained parallax when ceremonial. */
    fun innerParallaxRotation(rot: Float): Float = if (this == LEGACY) rot * 0.5f else -rot * 0.8f

    /** Progress rings / arcs / ticks / medallions — data instrumentation — never rotate. */
    @Suppress("unused")
    fun progressRotation(rot: Float): Float = 0f
}

/**
 * Per-layer idle opacity weights over the settled base. [Legacy] reproduces the current production
 * numbers exactly; [RingForward] applies the refined hierarchy — **circular structural rings read
 * highest, the central major geometry medium, and micro-ornament lowest**, with the whole seal still
 * restrained behind the interface. Kept as data so the hierarchy is unit-testable and tunable.
 */
data class SigilOpacityWeights(
    val aura: Float,
    val outerMotif: Float,
    val ringPrimary: Float,
    val ringSecondary: Float,
    val centralCap: Float,
    val connectors: Float,
    val arcs: Float,
    val playerMult: Float,
    val playerCap: Float,
    val playerGlow: Float,
    val playerFinalCap: Float,
    val classMult: Float,
    val classCap: Float,
    val classFinalCap: Float,
    val medallionMult: Float,
    val medallionFinalCap: Float,
) {
    companion object {
        /** Byte-for-byte the current production sigil opacity math. */
        val Legacy =
            SigilOpacityWeights(
                aura = 0.10f, outerMotif = 0.5f, ringPrimary = 0.35f, ringSecondary = 0.30f,
                centralCap = 0.70f, connectors = 0.70f, arcs = 0.60f,
                playerMult = 1.5f, playerCap = 0.62f, playerGlow = 0.25f, playerFinalCap = 0.85f,
                classMult = 1.4f, classCap = 0.55f, classFinalCap = 0.80f,
                medallionMult = 1.1f, medallionFinalCap = 0.85f,
            )

        /** Rings forward: structural rings dominate, central medium, ornament faint. */
        val RingForward =
            SigilOpacityWeights(
                aura = 0.05f, outerMotif = 0.28f, ringPrimary = 1.55f, ringSecondary = 1.15f,
                centralCap = 0.55f, connectors = 0.30f, arcs = 0.28f,
                playerMult = 3.0f, playerCap = 0.97f, playerGlow = 0.25f, playerFinalCap = 1.0f,
                classMult = 2.6f, classCap = 0.90f, classFinalCap = 0.97f,
                medallionMult = 0.9f, medallionFinalCap = 0.80f,
            )
    }
}

/** The two opacity presets, selectable in the review harness for current-vs-proposed comparison. */
enum class SigilOpacityProfile(val weights: SigilOpacityWeights) {
    LEGACY(SigilOpacityWeights.Legacy),
    RING_FORWARD(SigilOpacityWeights.RingForward),
}

/** The cache key: geometry is precomputed once per unique combination, not per frame. */
data class SigilGeometryKey(
    val rankTier: RankTier,
    val variant: StatusClassVariant,
    val medallionCount: Int,
    val simplified: Boolean,
    val minimal: Boolean,
)

/**
 * The full render state for the ornate sigil. Rings map to real prototype data: [playerRing] is
 * progress through the current Player Level; [classRing] (optional) through the current Class
 * Level. [activeMedallion] (0..4, or -1) is the attribute medallion currently pulsing; the sixth
 * proficiency medallion is shown only when [showProficiency] is set. [settledOpacity] keeps the
 * whole seal restrained by default; per-layer opacity is layered on top, never one global alpha.
 */
data class OrnateSigilState(
    val rankTier: RankTier,
    val variant: StatusClassVariant,
    val playerRing: Float,
    val classRing: Float?,
    val activeMedallion: Int,
    val showProficiency: Boolean,
    val settledOpacity: Float = DEFAULT_SETTLED_OPACITY,
    val newRankLayer: Boolean = false,
) {
    val medallionCount: Int get() = MedallionGlyph.attributeGlyphs.size + if (showProficiency) 1 else 0

    fun geometryKey(
        simplified: Boolean,
        minimal: Boolean,
    ): SigilGeometryKey = SigilGeometryKey(rankTier, variant, medallionCount, simplified, minimal)

    companion object {
        const val DEFAULT_SETTLED_OPACITY = 0.16f
        const val ASSEMBLY_PEAK_OPACITY = 0.32f
    }
}

/** Lightweight per-frame animation inputs (only cheap scalars are animated, never geometry). */
data class OrnateSigilAnimation(
    val assembly: Float,
    val rotation: Float,
    val glow: Float,
    val playerRingTrim: Float,
    val classRingTrim: Float,
    val medallionPulse: List<Float>,
    val proficiencyPulse: Float,
)

/**
 * Stable, normalized angular medallion positions (radians). Five universal medallions are spread
 * evenly; when the sixth (proficiency) is shown it anchors at the bottom and is returned last,
 * with the five redistributed evenly around the remaining perimeter — computed the same way every
 * frame so nothing jitters.
 */
fun medallionAngles(
    count: Int,
    hasProficiency: Boolean,
): List<Float> {
    val twoPi = (2.0 * Math.PI).toFloat()
    if (!hasProficiency) {
        // Five evenly spaced, first at the top.
        return (0 until count).map { -twoPi / 4f + twoPi * it / count }
    }
    val universal = count - 1
    val bottom = twoPi / 4f // +90° = bottom anchor
    // Distribute the universal medallions across the top arc, leaving the bottom for proficiency.
    val span = twoPi * (universal.toFloat() / (universal + 1))
    val start = bottom + (twoPi - span) / 2f
    val positions = (0 until universal).map { start + span * it / (universal - 1).coerceAtLeast(1) }
    return positions + bottom
}
