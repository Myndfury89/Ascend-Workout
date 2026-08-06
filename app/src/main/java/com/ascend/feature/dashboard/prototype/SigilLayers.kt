package com.ascend.feature.dashboard.prototype

import androidx.compose.ui.geometry.Offset

/*
 * CP-B: the addressable, shared-origin **outer instrumentation** layer set for the sigil — the
 * static rings + isolated medallions, plus the two LIVE XP rings kept as separate, never-flattened
 * entities (their trims are driven by real progress). This formalises what the renderer already
 * drew inline so each piece is addressable (a medallion's colour can be swapped on a Reward-tier
 * event; the live rings can never be baked into static artwork) per the handoff Asset export
 * checklist. Geometry is authored relative to the sigil CENTRE; [SigilLayout.ORIGIN] documents the
 * handoff's shared (150,150) registration point so every layer — instrumentation, middle group,
 * anchor, event assets — lines up on one origin.
 */

object SigilLayout {
    /** The shared logical origin all layers register to (matches the handoff prototype's (150,150)). */
    const val ORIGIN = 150f

    /** The logical authoring viewport (0..300); layers are drawn scaled to the real radius. */
    const val VIEWPORT = 300f

    /** Logical→pixel scale: 150 logical units == the sigil radius. */
    fun radiusScale(radiusPx: Float): Float = radiusPx / ORIGIN
}

/** An addressable medallion role. STRENGTH..RECOVERY are the five attributes; PROFICIENCY is the class mark. */
enum class MedallionRole { STRENGTH, ENDURANCE, AGILITY, DISCIPLINE, RECOVERY, PROFICIENCY }

/** One isolated medallion layer (so its fill can be swapped independently on events). */
data class SigilMedallion(
    val role: MedallionRole,
    val center: Offset,
    val glyph: MedallionGlyph,
    val isProficiency: Boolean,
) {
    /** Index into the attribute pulse list; -1 for the proficiency medallion (uses its own pulse). */
    val pulseIndex: Int get() = if (isProficiency) -1 else role.ordinal
}

/** Which ring: two static structural tracks, plus the two live (trim-driven) progress rings. */
enum class SigilRingRole { STRUCTURAL_PRIMARY, STRUCTURAL_SECONDARY, PLAYER_XP, CLASS_XP }

/**
 * A ring layer. [radiusFraction] is of the sigil radius (shared-origin, radius-independent).
 * [live] marks the trim-driven progress rings, which must stay separate cached paths — never
 * flattened into the static instrumentation artwork.
 */
data class SigilRing(
    val role: SigilRingRole,
    val radiusFraction: Float,
    val weight: Float,
    val dashed: Boolean,
    val live: Boolean,
) {
    fun radius(sigilRadius: Float): Float = radiusFraction * sigilRadius
}

/** The addressable outer-instrumentation layer set: static rings + medallions + the live XP rings. */
data class SigilInstrumentation(
    val rings: List<SigilRing>,
    val medallions: List<SigilMedallion>,
) {
    val structuralRings: List<SigilRing> get() = rings.filter { !it.live }
    val liveRings: List<SigilRing> get() = rings.filter { it.live }

    fun ring(role: SigilRingRole): SigilRing? = rings.firstOrNull { it.role == role }

    fun medallion(role: MedallionRole): SigilMedallion? = medallions.firstOrNull { it.role == role }

    companion object {
        private val ATTRIBUTE_ROLES =
            listOf(
                MedallionRole.STRENGTH,
                MedallionRole.ENDURANCE,
                MedallionRole.AGILITY,
                MedallionRole.DISCIPLINE,
                MedallionRole.RECOVERY,
            )

        /** The fixed ring layer set: two static structural tracks + the two live XP rings. */
        val RINGS: List<SigilRing> =
            listOf(
                SigilRing(SigilRingRole.STRUCTURAL_PRIMARY, radiusFraction = 0.72f, weight = 2f, dashed = false, live = false),
                SigilRing(SigilRingRole.STRUCTURAL_SECONDARY, radiusFraction = 0.60f, weight = 1.4f, dashed = true, live = false),
                SigilRing(SigilRingRole.PLAYER_XP, radiusFraction = 0.72f, weight = 3f, dashed = false, live = true),
                SigilRing(SigilRingRole.CLASS_XP, radiusFraction = 0.60f, weight = 1.8f, dashed = true, live = true),
            )

        /** Build the isolated medallion layers from the computed centres + proficiency glyph. */
        fun build(
            centers: List<Offset>,
            proficiencyIndex: Int,
            proficiencyGlyph: MedallionGlyph,
        ): SigilInstrumentation {
            val medallions =
                centers.mapIndexed { i, c ->
                    if (i == proficiencyIndex) {
                        SigilMedallion(MedallionRole.PROFICIENCY, c, proficiencyGlyph, isProficiency = true)
                    } else {
                        SigilMedallion(
                            ATTRIBUTE_ROLES.getOrElse(i) { MedallionRole.DISCIPLINE },
                            c,
                            MedallionGlyph.attributeGlyphs.getOrElse(i) { MedallionGlyph.DISCIPLINE_SQUARES },
                            isProficiency = false,
                        )
                    }
                }
            return SigilInstrumentation(RINGS, medallions)
        }
    }
}
