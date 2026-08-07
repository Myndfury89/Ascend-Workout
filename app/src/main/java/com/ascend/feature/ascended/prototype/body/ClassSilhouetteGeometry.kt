package com.ascend.feature.ascended.prototype.body

import androidx.compose.ui.geometry.Offset
import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase
import com.ascend.feature.ascended.prototype.model.EvolutionStage
import kotlin.math.cos
import kotlin.math.sin

/*
 * The seven class silhouettes as flat cut-paper shape language. Each figure is a handful of large
 * normalized polygons whose PROPORTIONS + PROPS make the class read at a glance: Mage tallest/
 * narrowest, Berserker widest/heaviest, Monk centred/symmetrical with a halo, Assassin narrowest/
 * sharpest, Fighter blocky mid-mass, Ranger lean with a diagonal cloak + bow, Guardian broad and
 * shield-dominant. Male/female apply only a small proportion delta so the CLASS shape language stays
 * stronger than the gender difference. No faces, no texture, no fine detail — silhouette first.
 */
object ClassSilhouetteGeometry {
    private const val CX = 0.5f

    /** Small per-base proportion delta: class shape language dominates, gender only nudges it. */
    private data class Gender(val shoulder: Float, val waist: Float, val hip: Float)

    private fun gender(base: BodyBase): Gender =
        when (base) {
            BodyBase.MALE -> Gender(1f, 1f, 1f)
            BodyBase.FEMALE -> Gender(0.90f, 0.87f, 1.14f)
        }

    /**
     * The nominal shoulder half-width that anchors each class's proportions (before the small gender
     * delta). Encodes the proportion hierarchy: Berserker widest … Assassin narrowest. The builders
     * use these same values, so this doubles as the source of truth for the silhouette massing.
     */
    fun nominalShoulderHalf(cls: AscendedClass): Float =
        when (cls) {
            AscendedClass.BERSERKER -> 0.300f
            AscendedClass.GUARDIAN -> 0.255f
            AscendedClass.FIGHTER -> 0.175f
            AscendedClass.RANGER -> 0.140f
            AscendedClass.MONK -> 0.135f
            AscendedClass.MAGICIAN -> 0.100f
            AscendedClass.ASSASSIN -> 0.085f
        }

    /** True when a class has a CP3 refined silhouette. All seven are refined as of the full CP3 pass. */
    fun hasRefined(
        @Suppress("UNUSED_PARAMETER") cls: AscendedClass,
    ): Boolean = true

    fun build(
        cls: AscendedClass,
        base: BodyBase,
        fidelity: SilhouetteFidelity = SilhouetteFidelity.BLOCKOUT,
        stage: EvolutionStage = EvolutionStage.MASTERED,
    ): ClassSilhouette {
        val shapes =
            when (fidelity) {
                SilhouetteFidelity.BLOCKOUT -> blockout(cls, base)
                SilhouetteFidelity.REFINED -> refined(cls, base)
            }
        // Cumulative gating: a stage shows its own equipment plus everything from earlier stages.
        return ClassSilhouette(shapes.filter { it.minStage.ordinal <= stage.ordinal })
    }

    /**
     * Which evolution stage each equipment / accessory shape first appears at. Anything not listed is
     * BASE (the clean class body + simple clothing). Keyed by shape name and shared across classes
     * where names repeat (e.g. poleyns, gauntlets). This is what makes a stage cumulative: Base has no
     * major weapon/shield/staff; Early adds first accessories; Advanced adds the main equipment;
     * Mastered adds the final flourish.
     */
    private val STAGE_BY_NAME: Map<String, EvolutionStage> =
        buildMap {
            listOf(
                "bracerL", "bracerR", "browBand", "legWrapL", "legWrapR", "beads", "sashKnot",
                "shoulderDrapeL", "shoulderDrapeR", "staffShaft", "scarf1", "clothTail", "wrapForearmR",
                "beltStrap", "gauntletL", "gauntletR", "poleynL", "poleynR", "helmBrow", "plateRidge",
                "quiver", "chestStrap", "cloakFold", "hoodPeak", "belt", "visor", "breastRidge",
                "pauldronLlame", "pauldronRlame", "mantleCollarL", "mantleCollarR",
            ).forEach { put(it, EvolutionStage.EARLY_GROWTH) }
            listOf(
                "axeHandle", "axeHead", "halo", "staffHead", "focus", "hemGlow", "scarf2", "bladeR",
                "swordBlade", "swordGuard", "bow", "arrowFletch1", "shield", "shieldRim",
            ).forEach { put(it, EvolutionStage.ADVANCED) }
            listOf("axeEdge", "staffCore", "focusRing", "bladeL", "arrowFletch2", "shieldBoss", "crown")
                .forEach { put(it, EvolutionStage.MASTERED) }
        }

    private fun blockout(
        cls: AscendedClass,
        base: BodyBase,
    ): List<SilhouetteShape> =
        when (cls) {
            AscendedClass.MAGICIAN -> mage(base)
            AscendedClass.BERSERKER -> berserker(base)
            AscendedClass.MONK -> monk(base)
            AscendedClass.ASSASSIN -> assassin(base)
            AscendedClass.FIGHTER -> fighter(base)
            AscendedClass.RANGER -> ranger(base)
            AscendedClass.GUARDIAN -> guardian(base)
        }

    /** Refined shapes where a class has been through the CP3 pass; otherwise the CP2 blockout. */
    private fun refined(
        cls: AscendedClass,
        base: BodyBase,
    ): List<SilhouetteShape> {
        val raw =
            when (cls) {
                AscendedClass.MAGICIAN -> refinedMagician(base)
                AscendedClass.BERSERKER -> refinedBerserker(base)
                AscendedClass.MONK -> refinedMonk(base)
                AscendedClass.ASSASSIN -> refinedAssassin(base)
                AscendedClass.FIGHTER -> refinedFighter(base)
                AscendedClass.RANGER -> refinedRanger(base)
                AscendedClass.GUARDIAN -> refinedGuardian(base)
            }
        return raw.map { it.copy(minStage = STAGE_BY_NAME[it.name] ?: EvolutionStage.BASE) }
    }

    /** Approximate head/helm centre-Y per class, used to place the Perception manifestation. */
    fun refinedHeadY(cls: AscendedClass): Float =
        when (cls) {
            AscendedClass.GUARDIAN -> 0.17f
            AscendedClass.MAGICIAN -> 0.185f
            AscendedClass.MONK -> 0.205f
            AscendedClass.ASSASSIN -> 0.225f
            AscendedClass.FIGHTER -> 0.235f
            AscendedClass.RANGER -> 0.245f
            AscendedClass.BERSERKER -> 0.255f
        }

    /**
     * Class aura as flat concentric rings behind the figure, growing with the evolution stage. Base
     * has none; Early a faint ring; Advanced two; Mastered three — a controlled, readable presence,
     * never a gradient glow. Drawn behind the body and skipped in the silhouette/outline review modes.
     */
    fun auraShapes(stage: EvolutionStage): List<SilhouetteShape> {
        val rings = stage.ordinal // BASE 0, EARLY 1, ADVANCED 2, MASTERED 3
        if (rings <= 0) return emptyList()
        val radii = listOf(0.30f, 0.40f, 0.48f)
        return (0 until rings.coerceAtMost(radii.size)).map { i ->
            ringShape("aura$i", SilhouetteTone.MEDIUM, CX, 0.46f, radii[i], 0.006f)
        }
    }

    /**
     * The Perception Skill manifestation, placed at the head: L1 faint eyes; L5 brighter eyes + a
     * sensing halo + scan lines; L10 awakened eyes + a layered awareness field + energetic traces.
     * A clearly stronger read at 10 than at 1. Drawn in front and skipped in silhouette/outline modes.
     */
    fun perceptionShapes(
        level: Int,
        cls: AscendedClass,
    ): List<SilhouetteShape> {
        if (level <= 0) return emptyList()
        val hy = refinedHeadY(cls)
        val out = ArrayList<SilhouetteShape>()
        // Eyes (brighter/larger with level).
        val eyeR =
            if (level >= 10) {
                0.016f
            } else if (level >= 5) {
                0.013f
            } else {
                0.010f
            }
        out.add(octagonShape("eyeL", SilhouetteTone.LIGHT, CX - 0.018f, hy, eyeR))
        out.add(octagonShape("eyeR", SilhouetteTone.LIGHT, CX + 0.018f, hy, eyeR))
        if (level >= 5) {
            out.add(ringShape("senseHalo", SilhouetteTone.MEDIUM, CX, hy, 0.075f, 0.006f))
            out.add(scanLine("scanL", CX - 0.13f, hy - 0.01f, CX - 0.05f, hy - 0.01f))
            out.add(scanLine("scanR", CX + 0.05f, hy - 0.01f, CX + 0.13f, hy - 0.01f))
        }
        if (level >= 10) {
            out.add(ringShape("awareField", SilhouetteTone.MEDIUM, CX, hy, 0.115f, 0.005f))
            out.add(scanLine("traceUp", CX, hy - 0.14f, CX, hy - 0.09f))
            out.add(scanLine("traceDL", CX - 0.11f, hy + 0.10f, CX - 0.06f, hy + 0.05f))
            out.add(scanLine("traceDR", CX + 0.06f, hy + 0.05f, CX + 0.11f, hy + 0.10f))
        }
        return out
    }

    /** A thin flat bar between two points — the Perception scan/trace lines. */
    private fun scanLine(
        name: String,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
    ): SilhouetteShape {
        val t = 0.004f
        return SilhouetteShape(
            name,
            SilhouetteTone.MEDIUM,
            listOf(Offset(x0, y0 - t), Offset(x1, y1 - t), Offset(x1, y1 + t), Offset(x0, y0 + t)),
        )
    }

    // --- Mage: tall, narrow, vertical, staff + pointed hat ---
    private fun mage(base: BodyBase): List<SilhouetteShape> {
        val g = gender(base)
        val sh = 0.100f * g.shoulder
        val hem = 0.180f
        return listOf(
            poly(
                "staffShaft",
                SilhouetteTone.DARK,
                p(CX - 0.185f, 0.20f),
                p(CX - 0.165f, 0.20f),
                p(CX - 0.165f, 0.92f),
                p(CX - 0.185f, 0.92f),
            ),
            octagonShape("staffOrb", SilhouetteTone.MEDIUM, CX - 0.175f, 0.165f, 0.040f),
            poly("robeBody", SilhouetteTone.DARK, p(CX - sh, 0.235f), p(CX + sh, 0.235f), p(CX + hem, 0.92f), p(CX - hem, 0.92f)),
            poly(
                "robeHem",
                SilhouetteTone.MEDIUM,
                p(CX - 0.085f, 0.60f),
                p(CX + 0.085f, 0.60f),
                p(CX + 0.125f, 0.92f),
                p(CX - 0.125f, 0.92f),
            ),
            poly(
                "shoulderCape",
                SilhouetteTone.DARK,
                p(CX - 0.150f, 0.235f),
                p(CX + 0.150f, 0.235f),
                p(CX + 0.10f, 0.33f),
                p(CX - 0.10f, 0.33f),
            ),
            poly(
                "rightSleeve",
                SilhouetteTone.DARK,
                p(CX + 0.09f, 0.26f),
                p(CX + 0.165f, 0.31f),
                p(CX + 0.145f, 0.52f),
                p(CX + 0.07f, 0.50f),
            ),
            octagonShape("head", SilhouetteTone.SILHOUETTE, CX, 0.185f, 0.034f),
            poly("hatCone", SilhouetteTone.SILHOUETTE, p(CX + 0.015f, 0.030f), p(CX + 0.105f, 0.155f), p(CX - 0.105f, 0.155f)),
            poly(
                "hatBrim",
                SilhouetteTone.DARK,
                p(CX - 0.125f, 0.150f),
                p(CX + 0.125f, 0.150f),
                p(CX + 0.095f, 0.190f),
                p(CX - 0.095f, 0.190f),
            ),
            octagonShape("focus", SilhouetteTone.LIGHT, CX + 0.165f, 0.40f, 0.028f),
        )
    }

    // --- Berserker: widest, heaviest, jagged fur shoulders + big arms + axe ---
    private fun berserker(base: BodyBase): List<SilhouetteShape> {
        val g = gender(base)
        val sh = 0.30f * g.shoulder
        val waist = 0.15f * g.waist
        return listOf(
            poly(
                "torso",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.20f, 0.30f),
                p(CX + 0.20f, 0.30f),
                p(CX + waist, 0.60f),
                p(CX - waist, 0.60f),
            ),
            legPair(0.055f, 0.585f, 0.075f, 0.94f, SilhouetteTone.SILHOUETTE, "legL", "legR")[0],
            legPair(0.055f, 0.585f, 0.075f, 0.94f, SilhouetteTone.SILHOUETTE, "legL", "legR")[1],
            poly(
                "furShoulderR",
                SilhouetteTone.DARK,
                p(CX + 0.10f, 0.235f),
                p(CX + sh, 0.225f),
                p(CX + sh - 0.02f, 0.33f),
                p(CX + 0.28f, 0.30f),
                p(CX + 0.24f, 0.37f),
                p(CX + 0.13f, 0.34f),
            ),
            poly(
                "furShoulderL",
                SilhouetteTone.DARK,
                p(CX - 0.10f, 0.235f),
                p(CX - sh, 0.225f),
                p(CX - sh + 0.02f, 0.33f),
                p(CX - 0.28f, 0.30f),
                p(CX - 0.24f, 0.37f),
                p(CX - 0.13f, 0.34f),
            ),
            poly("armR", SilhouetteTone.SILHOUETTE, p(CX + 0.20f, 0.33f), p(CX + 0.30f, 0.36f), p(CX + 0.29f, 0.60f), p(CX + 0.19f, 0.58f)),
            poly("armL", SilhouetteTone.SILHOUETTE, p(CX - 0.20f, 0.33f), p(CX - 0.30f, 0.36f), p(CX - 0.29f, 0.60f), p(CX - 0.19f, 0.58f)),
            poly(
                "bracerR",
                SilhouetteTone.MEDIUM,
                p(CX + 0.19f, 0.52f),
                p(CX + 0.29f, 0.54f),
                p(CX + 0.285f, 0.63f),
                p(CX + 0.185f, 0.61f),
            ),
            octagonShape("head", SilhouetteTone.SILHOUETTE, CX, 0.255f, 0.036f),
            poly(
                "axeHandle",
                SilhouetteTone.DARK,
                p(CX + 0.30f, 0.34f),
                p(CX + 0.315f, 0.34f),
                p(CX + 0.315f, 0.86f),
                p(CX + 0.30f, 0.86f),
            ),
            poly("axeHead", SilhouetteTone.MEDIUM, p(CX + 0.30f, 0.35f), p(CX + 0.40f, 0.40f), p(CX + 0.40f, 0.52f), p(CX + 0.30f, 0.50f)),
            poly("beltWrap", SilhouetteTone.MEDIUM, p(CX - waist, 0.56f), p(CX + waist, 0.56f), p(CX + waist, 0.62f), p(CX - waist, 0.62f)),
        )
    }

    // --- Monk: centred, symmetrical, prayer pose, halo, sash, wide lower cloth ---
    private fun monk(base: BodyBase): List<SilhouetteShape> {
        val g = gender(base)
        val sh = 0.135f * g.shoulder
        return listOf(
            ringShape("halo", SilhouetteTone.MEDIUM, CX, 0.155f, 0.085f, 0.012f),
            poly(
                "robeBody",
                SilhouetteTone.SILHOUETTE,
                p(CX - sh, 0.255f),
                p(CX + sh, 0.255f),
                p(CX + 0.115f, 0.52f),
                p(CX - 0.115f, 0.52f),
            ),
            poly(
                "lowerClothL",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.115f, 0.50f),
                p(CX + 0.01f, 0.50f),
                p(CX + 0.02f, 0.92f),
                p(CX - 0.175f, 0.92f),
            ),
            poly(
                "lowerClothR",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.01f, 0.50f),
                p(CX + 0.115f, 0.50f),
                p(CX + 0.175f, 0.92f),
                p(CX - 0.02f, 0.92f),
            ),
            poly("sash", SilhouetteTone.MEDIUM, p(CX - sh, 0.50f), p(CX + sh, 0.50f), p(CX + sh, 0.55f), p(CX - sh, 0.55f)),
            poly("prayerHands", SilhouetteTone.LIGHT, p(CX, 0.36f), p(CX + 0.045f, 0.44f), p(CX, 0.50f), p(CX - 0.045f, 0.44f)),
            poly(
                "armR",
                SilhouetteTone.DARK,
                p(CX + sh - 0.02f, 0.28f),
                p(CX + sh + 0.02f, 0.30f),
                p(CX + 0.04f, 0.46f),
                p(CX + 0.01f, 0.44f),
            ),
            poly(
                "armL",
                SilhouetteTone.DARK,
                p(CX - sh + 0.02f, 0.28f),
                p(CX - sh - 0.02f, 0.30f),
                p(CX - 0.04f, 0.46f),
                p(CX - 0.01f, 0.44f),
            ),
            octagonShape("head", SilhouetteTone.SILHOUETTE, CX, 0.215f, 0.038f),
            beadRing("beads", SilhouetteTone.MEDIUM, CX, 0.30f, 0.085f),
        )
    }

    // --- Assassin: narrowest, sharpest, hood + sideways scarf + short blades ---
    private fun assassin(base: BodyBase): List<SilhouetteShape> {
        val g = gender(base)
        val sh = 0.085f * g.shoulder
        val waist = 0.055f * g.waist
        return listOf(
            poly("torso", SilhouetteTone.SILHOUETTE, p(CX - sh, 0.275f), p(CX + sh, 0.275f), p(CX + waist, 0.55f), p(CX - waist, 0.55f)),
            poly(
                "legR",
                SilhouetteTone.SILHOUETTE,
                p(CX + 0.005f, 0.53f),
                p(CX + waist + 0.01f, 0.53f),
                p(CX + 0.05f, 0.93f),
                p(CX + 0.02f, 0.93f),
            ),
            poly(
                "legL",
                SilhouetteTone.SILHOUETTE,
                p(CX - waist - 0.01f, 0.53f),
                p(CX - 0.005f, 0.53f),
                p(CX - 0.02f, 0.93f),
                p(CX - 0.05f, 0.93f),
            ),
            poly("scarf", SilhouetteTone.MEDIUM, p(CX + 0.05f, 0.28f), p(CX + 0.26f, 0.235f), p(CX + 0.27f, 0.275f), p(CX + 0.06f, 0.33f)),
            poly(
                "armR",
                SilhouetteTone.SILHOUETTE,
                p(CX + sh - 0.01f, 0.30f),
                p(CX + sh + 0.03f, 0.32f),
                p(CX + 0.10f, 0.52f),
                p(CX + 0.06f, 0.50f),
            ),
            poly(
                "armL",
                SilhouetteTone.SILHOUETTE,
                p(CX - sh + 0.01f, 0.30f),
                p(CX - sh - 0.03f, 0.32f),
                p(CX - 0.10f, 0.52f),
                p(CX - 0.06f, 0.50f),
            ),
            poly("bladeR", SilhouetteTone.LIGHT, p(CX + 0.10f, 0.52f), p(CX + 0.13f, 0.52f), p(CX + 0.15f, 0.70f), p(CX + 0.12f, 0.70f)),
            poly(
                "hood",
                SilhouetteTone.DARK,
                p(CX - 0.055f, 0.19f),
                p(CX + 0.055f, 0.19f),
                p(CX + 0.045f, 0.29f),
                p(CX + 0.02f, 0.25f),
                p(CX - 0.02f, 0.25f),
                p(CX - 0.045f, 0.29f),
            ),
            octagonShape("head", SilhouetteTone.SILHOUETTE, CX, 0.225f, 0.030f),
            poly("clothStrip", SilhouetteTone.DARK, p(CX - 0.02f, 0.50f), p(CX + 0.03f, 0.50f), p(CX + 0.06f, 0.74f), p(CX + 0.02f, 0.74f)),
        )
    }

    // --- Fighter: balanced, sturdy, rectangular armour blocks + straight sword ---
    private fun fighter(base: BodyBase): List<SilhouetteShape> {
        val g = gender(base)
        val sh = 0.175f * g.shoulder
        val waist = 0.115f * g.waist
        return listOf(
            poly("torso", SilhouetteTone.SILHOUETTE, p(CX - sh, 0.28f), p(CX + sh, 0.28f), p(CX + waist, 0.56f), p(CX - waist, 0.56f)),
            legPair(0.045f, 0.545f, 0.06f, 0.93f, SilhouetteTone.SILHOUETTE, "legL", "legR")[0],
            legPair(0.045f, 0.545f, 0.06f, 0.93f, SilhouetteTone.SILHOUETTE, "legL", "legR")[1],
            poly(
                "pauldronR",
                SilhouetteTone.DARK,
                p(CX + 0.10f, 0.27f),
                p(CX + sh + 0.02f, 0.27f),
                p(CX + sh, 0.35f),
                p(CX + 0.10f, 0.35f),
            ),
            poly(
                "pauldronL",
                SilhouetteTone.DARK,
                p(CX - 0.10f, 0.27f),
                p(CX - sh - 0.02f, 0.27f),
                p(CX - sh, 0.35f),
                p(CX - 0.10f, 0.35f),
            ),
            poly(
                "armR",
                SilhouetteTone.SILHOUETTE,
                p(CX + sh - 0.02f, 0.33f),
                p(CX + sh + 0.02f, 0.35f),
                p(CX + sh, 0.56f),
                p(CX + sh - 0.05f, 0.54f),
            ),
            poly(
                "armL",
                SilhouetteTone.SILHOUETTE,
                p(CX - sh + 0.02f, 0.33f),
                p(CX - sh - 0.02f, 0.35f),
                p(CX - sh, 0.56f),
                p(CX - sh + 0.05f, 0.54f),
            ),
            poly(
                "chestPlate",
                SilhouetteTone.MEDIUM,
                p(CX - 0.10f, 0.31f),
                p(CX + 0.10f, 0.31f),
                p(CX + 0.09f, 0.48f),
                p(CX - 0.09f, 0.48f),
            ),
            poly(
                "swordBlade",
                SilhouetteTone.LIGHT,
                p(CX + sh - 0.005f, 0.44f),
                p(CX + sh + 0.02f, 0.44f),
                p(CX + sh + 0.02f, 0.82f),
                p(CX + sh + 0.005f, 0.85f),
                p(CX + sh - 0.005f, 0.82f),
            ),
            poly(
                "swordGuard",
                SilhouetteTone.DARK,
                p(CX + sh - 0.03f, 0.43f),
                p(CX + sh + 0.045f, 0.43f),
                p(CX + sh + 0.045f, 0.46f),
                p(CX + sh - 0.03f, 0.46f),
            ),
            octagonShape("helm", SilhouetteTone.SILHOUETTE, CX, 0.235f, 0.040f),
            poly("belt", SilhouetteTone.DARK, p(CX - waist, 0.52f), p(CX + waist, 0.52f), p(CX + waist, 0.57f), p(CX - waist, 0.57f)),
        )
    }

    // --- Ranger: lean, diagonal cloak, chest strap, bow + quiver ---
    private fun ranger(base: BodyBase): List<SilhouetteShape> {
        val g = gender(base)
        val sh = 0.140f * g.shoulder
        val waist = 0.095f * g.waist
        return listOf(
            poly(
                "cloak",
                SilhouetteTone.SILHOUETTE,
                p(CX - sh - 0.02f, 0.27f),
                p(CX + 0.08f, 0.30f),
                p(CX + 0.18f, 0.90f),
                p(CX - 0.14f, 0.92f),
            ),
            poly("torso", SilhouetteTone.DARK, p(CX - sh, 0.29f), p(CX + sh, 0.29f), p(CX + waist, 0.55f), p(CX - waist, 0.55f)),
            poly(
                "legR",
                SilhouetteTone.SILHOUETTE,
                p(CX + 0.005f, 0.53f),
                p(CX + waist, 0.53f),
                p(CX + 0.075f, 0.92f),
                p(CX + 0.03f, 0.92f),
            ),
            poly(
                "legL",
                SilhouetteTone.SILHOUETTE,
                p(CX - waist, 0.53f),
                p(CX - 0.005f, 0.53f),
                p(CX - 0.03f, 0.92f),
                p(CX - 0.075f, 0.92f),
            ),
            poly(
                "chestStrap",
                SilhouetteTone.MEDIUM,
                p(CX - 0.09f, 0.31f),
                p(CX - 0.055f, 0.30f),
                p(CX + 0.075f, 0.50f),
                p(CX + 0.04f, 0.51f),
            ),
            poly("hood", SilhouetteTone.DARK, p(CX - 0.06f, 0.20f), p(CX + 0.06f, 0.20f), p(CX + 0.05f, 0.30f), p(CX - 0.05f, 0.30f)),
            octagonShape("head", SilhouetteTone.SILHOUETTE, CX, 0.235f, 0.032f),
            poly(
                "armL",
                SilhouetteTone.SILHOUETTE,
                p(CX - sh + 0.01f, 0.31f),
                p(CX - sh - 0.02f, 0.33f),
                p(CX - 0.11f, 0.53f),
                p(CX - 0.07f, 0.51f),
            ),
            bowShape("bow", SilhouetteTone.MEDIUM, CX - 0.185f, 0.50f, 0.22f),
            poly("quiver", SilhouetteTone.DARK, p(CX + 0.10f, 0.25f), p(CX + 0.145f, 0.25f), p(CX + 0.135f, 0.44f), p(CX + 0.09f, 0.44f)),
            poly(
                "arrowFletch",
                SilhouetteTone.LIGHT,
                p(CX + 0.10f, 0.22f),
                p(CX + 0.145f, 0.22f),
                p(CX + 0.145f, 0.27f),
                p(CX + 0.10f, 0.27f),
            ),
        )
    }

    // --- Guardian: broad, immovable, tall helm crown, mantle, layered armour, big shield ---
    private fun guardian(base: BodyBase): List<SilhouetteShape> {
        val g = gender(base)
        val sh = 0.255f * g.shoulder
        val waist = 0.16f * g.waist
        return listOf(
            poly(
                "mantle",
                SilhouetteTone.SILHOUETTE,
                p(CX - sh - 0.02f, 0.24f),
                p(CX + sh + 0.02f, 0.24f),
                p(CX + 0.22f, 0.92f),
                p(CX - 0.22f, 0.92f),
            ),
            legPair(0.075f, 0.60f, 0.10f, 0.94f, SilhouetteTone.SILHOUETTE, "legL", "legR")[0],
            legPair(0.075f, 0.60f, 0.10f, 0.94f, SilhouetteTone.SILHOUETTE, "legL", "legR")[1],
            poly(
                "pauldronR",
                SilhouetteTone.DARK,
                p(CX + 0.09f, 0.235f),
                p(CX + sh, 0.225f),
                p(CX + sh - 0.01f, 0.34f),
                p(CX + 0.11f, 0.35f),
            ),
            poly(
                "pauldronL",
                SilhouetteTone.DARK,
                p(CX - 0.09f, 0.235f),
                p(CX - sh, 0.225f),
                p(CX - sh + 0.01f, 0.34f),
                p(CX - 0.11f, 0.35f),
            ),
            poly(
                "chestPlate",
                SilhouetteTone.MEDIUM,
                p(CX - 0.12f, 0.30f),
                p(CX + 0.12f, 0.30f),
                p(CX + waist, 0.56f),
                p(CX - waist, 0.56f),
            ),
            poly(
                "skirtArmour",
                SilhouetteTone.DARK,
                p(CX - waist, 0.55f),
                p(CX + waist, 0.55f),
                p(CX + 0.19f, 0.72f),
                p(CX - 0.19f, 0.72f),
            ),
            poly(
                "helm", SilhouetteTone.SILHOUETTE,
                p(
                    CX - 0.05f,
                    0.235f,
                ),
                p(
                    CX + 0.05f,
                    0.235f,
                ),
                p(CX + 0.045f, 0.15f), p(CX + 0.02f, 0.17f), p(CX, 0.10f), p(CX - 0.02f, 0.17f), p(CX - 0.045f, 0.15f),
            ),
            poly(
                "crown", SilhouetteTone.DARK,
                p(
                    CX - 0.05f,
                    0.155f,
                ),
                p(
                    CX - 0.03f,
                    0.09f,
                ),
                p(CX - 0.01f, 0.15f), p(CX + 0.01f, 0.08f), p(CX + 0.03f, 0.15f), p(CX + 0.05f, 0.10f), p(CX + 0.05f, 0.17f),
            ),
            poly(
                "shield",
                SilhouetteTone.MEDIUM,
                p(CX + 0.16f, 0.34f),
                p(CX + 0.34f, 0.36f),
                p(CX + 0.35f, 0.58f),
                p(CX + 0.255f, 0.70f),
                p(CX + 0.16f, 0.58f),
            ),
            poly(
                "shieldBoss",
                SilhouetteTone.LIGHT,
                p(CX + 0.245f, 0.44f),
                p(CX + 0.285f, 0.47f),
                p(CX + 0.255f, 0.56f),
                p(CX + 0.215f, 0.51f),
            ),
        )
    }

    // --- CP3 refined silhouettes ---

    /**
     * Refined Guardian: a fortress read built from layered flat masses rather than stacked rectangles.
     * A designed mantle + raised collar sit distinct from a tapered breastplate with a central ridge;
     * broad anatomical pauldrons are layered (cap + lame); arms are visible (right arm + gauntlet, left
     * arm behind the shield); the lower body is two separated greaves with poleyns and sabatons (real
     * stance + negative space between the legs); a crowned great-helm has a gorget and a visor slit; and
     * a heater shield with rim + boss sits to the side — integrated, but not hiding the torso. Still a
     * flat 2D cutout, no faces or surface detail. Gender nudges only the shoulder spread and leg stance.
     */
    private fun refinedGuardian(base: BodyBase): List<SilhouetteShape> {
        val g = gender(base)
        val s = g.shoulder
        val h = g.hip
        return listOf(
            // mantle (behind) + raised collar
            poly(
                "mantleBack", SilhouetteTone.SILHOUETTE,
                p(CX - 0.16f, 0.235f), p(CX - 0.28f * s, 0.27f), p(CX - 0.24f, 0.60f), p(CX - 0.20f, 0.92f),
                p(CX + 0.20f, 0.92f), p(CX + 0.24f, 0.60f), p(CX + 0.28f * s, 0.27f), p(CX + 0.16f, 0.235f),
            ),
            poly(
                "mantleCollarL",
                SilhouetteTone.DARK,
                p(CX - 0.12f, 0.235f),
                p(CX - 0.175f, 0.135f),
                p(CX - 0.075f, 0.175f),
                p(CX - 0.06f, 0.235f),
            ),
            poly(
                "mantleCollarR",
                SilhouetteTone.DARK,
                p(CX + 0.12f, 0.235f),
                p(CX + 0.175f, 0.135f),
                p(CX + 0.075f, 0.175f),
                p(CX + 0.06f, 0.235f),
            ),
            // left arm (behind the shield)
            poly(
                "armL",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.15f, 0.35f),
                p(CX - 0.095f, 0.36f),
                p(CX - 0.10f, 0.55f),
                p(CX - 0.16f, 0.54f),
            ),
            // separated greaves (stance + negative space at centre)
            poly(
                "legL",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.02f, 0.57f),
                p(CX - 0.13f * h, 0.58f),
                p(CX - 0.125f * h, 0.80f),
                p(CX - 0.10f, 0.90f),
                p(CX - 0.03f, 0.90f),
                p(CX - 0.025f, 0.74f),
            ),
            poly(
                "legR",
                SilhouetteTone.SILHOUETTE,
                p(CX + 0.02f, 0.57f),
                p(CX + 0.13f * h, 0.58f),
                p(CX + 0.125f * h, 0.80f),
                p(CX + 0.10f, 0.90f),
                p(CX + 0.03f, 0.90f),
                p(CX + 0.025f, 0.74f),
            ),
            poly("poleynL", SilhouetteTone.MEDIUM, p(CX - 0.13f, 0.70f), p(CX - 0.04f, 0.70f), p(CX - 0.05f, 0.78f), p(CX - 0.135f, 0.78f)),
            poly("poleynR", SilhouetteTone.MEDIUM, p(CX + 0.04f, 0.70f), p(CX + 0.13f, 0.70f), p(CX + 0.135f, 0.78f), p(CX + 0.05f, 0.78f)),
            poly(
                "sabatonL",
                SilhouetteTone.DARK,
                p(CX - 0.115f, 0.885f),
                p(CX - 0.025f, 0.885f),
                p(CX - 0.015f, 0.95f),
                p(CX - 0.145f, 0.95f),
            ),
            poly(
                "sabatonR",
                SilhouetteTone.DARK,
                p(CX + 0.025f, 0.885f),
                p(CX + 0.115f, 0.885f),
                p(CX + 0.145f, 0.95f),
                p(CX + 0.015f, 0.95f),
            ),
            // waist lames + tapered cuirass with a central ridge (torso distinct from mantle)
            poly(
                "faulds", SilhouetteTone.DARK,
                p(CX - 0.14f, 0.455f), p(CX + 0.14f, 0.455f), p(CX + 0.16f, 0.55f), p(CX + 0.06f, 0.585f),
                p(CX, 0.55f), p(CX - 0.06f, 0.585f), p(CX - 0.16f, 0.55f),
            ),
            poly(
                "breastplate",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.135f, 0.265f),
                p(CX + 0.135f, 0.265f),
                p(CX + 0.15f, 0.40f),
                p(CX + 0.095f, 0.47f),
                p(CX - 0.095f, 0.47f),
                p(CX - 0.15f, 0.40f),
            ),
            poly(
                "breastRidge",
                SilhouetteTone.MEDIUM,
                p(CX - 0.03f, 0.285f),
                p(CX + 0.03f, 0.285f),
                p(CX + 0.05f, 0.45f),
                p(CX, 0.47f),
                p(CX - 0.05f, 0.45f),
            ),
            poly("gorget", SilhouetteTone.DARK, p(CX - 0.05f, 0.225f), p(CX + 0.05f, 0.225f), p(CX + 0.065f, 0.26f), p(CX - 0.065f, 0.26f)),
            // layered pauldrons (cap + lame)
            poly(
                "pauldronL",
                SilhouetteTone.MEDIUM,
                p(CX - 0.10f, 0.245f),
                p(CX - 0.27f * s, 0.235f),
                p(CX - 0.285f * s, 0.31f),
                p(CX - 0.24f, 0.35f),
                p(CX - 0.11f, 0.335f),
            ),
            poly(
                "pauldronLlame",
                SilhouetteTone.DARK,
                p(CX - 0.14f, 0.335f),
                p(CX - 0.255f, 0.345f),
                p(CX - 0.245f, 0.40f),
                p(CX - 0.15f, 0.39f),
            ),
            poly(
                "pauldronR",
                SilhouetteTone.MEDIUM,
                p(CX + 0.10f, 0.245f),
                p(CX + 0.27f * s, 0.235f),
                p(CX + 0.285f * s, 0.31f),
                p(CX + 0.24f, 0.35f),
                p(CX + 0.11f, 0.335f),
            ),
            poly(
                "pauldronRlame",
                SilhouetteTone.DARK,
                p(CX + 0.14f, 0.335f),
                p(CX + 0.255f, 0.345f),
                p(CX + 0.245f, 0.40f),
                p(CX + 0.15f, 0.39f),
            ),
            // right arm + gauntlet
            poly(
                "armR",
                SilhouetteTone.SILHOUETTE,
                p(CX + 0.19f, 0.36f),
                p(CX + 0.26f, 0.38f),
                p(CX + 0.245f, 0.58f),
                p(CX + 0.255f, 0.66f),
                p(CX + 0.185f, 0.67f),
                p(CX + 0.17f, 0.50f),
            ),
            poly(
                "gauntletR",
                SilhouetteTone.DARK,
                p(CX + 0.185f, 0.64f),
                p(CX + 0.255f, 0.65f),
                p(CX + 0.25f, 0.72f),
                p(CX + 0.18f, 0.71f),
            ),
            // crowned great-helm + visor slit
            poly(
                "helm",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.055f, 0.235f),
                p(CX - 0.06f, 0.15f),
                p(CX - 0.035f, 0.105f),
                p(CX + 0.035f, 0.105f),
                p(CX + 0.06f, 0.15f),
                p(CX + 0.055f, 0.235f),
            ),
            poly(
                "visor",
                SilhouetteTone.MEDIUM,
                p(CX - 0.045f, 0.165f),
                p(CX + 0.045f, 0.165f),
                p(CX + 0.045f, 0.185f),
                p(CX - 0.045f, 0.185f),
            ),
            poly(
                "crown", SilhouetteTone.DARK,
                p(CX - 0.06f, 0.155f), p(CX - 0.048f, 0.075f), p(CX - 0.022f, 0.14f), p(CX, 0.055f),
                p(CX + 0.022f, 0.14f), p(CX + 0.048f, 0.075f), p(CX + 0.06f, 0.155f),
            ),
            // heater shield (to the side, integrated, not hiding the torso) + rim + boss
            poly(
                "shield",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.35f, 0.33f),
                p(CX - 0.11f, 0.35f),
                p(CX - 0.105f, 0.55f),
                p(CX - 0.225f, 0.72f),
                p(CX - 0.355f, 0.55f),
            ),
            poly(
                "shieldRim",
                SilhouetteTone.MEDIUM,
                p(CX - 0.325f, 0.36f),
                p(CX - 0.13f, 0.375f),
                p(CX - 0.13f, 0.54f),
                p(CX - 0.225f, 0.68f),
                p(CX - 0.33f, 0.54f),
            ),
            poly(
                "shieldBoss",
                SilhouetteTone.LIGHT,
                p(CX - 0.235f, 0.47f),
                p(CX - 0.195f, 0.51f),
                p(CX - 0.235f, 0.58f),
                p(CX - 0.275f, 0.51f),
            ),
        )
    }

    // --- Refined Berserker: widest, layered fur mane, big arms, axe, separated thick legs ---
    private fun refinedBerserker(base: BodyBase): List<SilhouetteShape> {
        val g = gender(base)
        val s = g.shoulder
        val w = g.waist
        val h = g.hip
        return listOf(
            poly(
                "torso",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.19f, 0.31f),
                p(CX + 0.19f, 0.31f),
                p(CX + 0.13f * w, 0.58f),
                p(CX - 0.13f * w, 0.58f),
            ),
            poly(
                "legL",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.02f, 0.56f),
                p(CX - 0.15f * h, 0.57f),
                p(CX - 0.14f * h, 0.78f),
                p(CX - 0.11f, 0.90f),
                p(CX - 0.03f, 0.90f),
                p(CX - 0.03f, 0.72f),
            ),
            poly(
                "legR",
                SilhouetteTone.SILHOUETTE,
                p(CX + 0.02f, 0.56f),
                p(CX + 0.15f * h, 0.57f),
                p(CX + 0.14f * h, 0.78f),
                p(CX + 0.11f, 0.90f),
                p(CX + 0.03f, 0.90f),
                p(CX + 0.03f, 0.72f),
            ),
            poly(
                "legWrapL",
                SilhouetteTone.MEDIUM,
                p(CX - 0.145f, 0.74f),
                p(CX - 0.03f, 0.74f),
                p(CX - 0.04f, 0.86f),
                p(CX - 0.15f, 0.86f),
            ),
            poly(
                "legWrapR",
                SilhouetteTone.MEDIUM,
                p(CX + 0.03f, 0.74f),
                p(CX + 0.145f, 0.74f),
                p(CX + 0.15f, 0.86f),
                p(CX + 0.04f, 0.86f),
            ),
            poly("bootL", SilhouetteTone.DARK, p(CX - 0.14f, 0.88f), p(CX - 0.02f, 0.88f), p(CX - 0.01f, 0.95f), p(CX - 0.16f, 0.95f)),
            poly("bootR", SilhouetteTone.DARK, p(CX + 0.02f, 0.88f), p(CX + 0.14f, 0.88f), p(CX + 0.16f, 0.95f), p(CX + 0.01f, 0.95f)),
            poly(
                "furShoulderL", SilhouetteTone.SILHOUETTE,
                p(
                    CX - 0.10f,
                    0.24f,
                ),
                p(
                    CX - 0.22f,
                    0.195f,
                ),
                p(CX - 0.30f * s, 0.24f), p(CX - 0.30f * s, 0.35f), p(CX - 0.33f * s, 0.40f), p(CX - 0.22f, 0.37f), p(CX - 0.13f, 0.34f),
            ),
            poly(
                "furShoulderR", SilhouetteTone.SILHOUETTE,
                p(
                    CX + 0.10f,
                    0.24f,
                ),
                p(
                    CX + 0.22f,
                    0.195f,
                ),
                p(CX + 0.30f * s, 0.24f), p(CX + 0.30f * s, 0.35f), p(CX + 0.33f * s, 0.40f), p(CX + 0.22f, 0.37f), p(CX + 0.13f, 0.34f),
            ),
            poly("furInnerL", SilhouetteTone.DARK, p(CX - 0.12f, 0.27f), p(CX - 0.24f, 0.28f), p(CX - 0.22f, 0.38f), p(CX - 0.13f, 0.35f)),
            poly("furInnerR", SilhouetteTone.DARK, p(CX + 0.12f, 0.27f), p(CX + 0.24f, 0.28f), p(CX + 0.22f, 0.38f), p(CX + 0.13f, 0.35f)),
            poly(
                "armL",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.19f, 0.35f),
                p(CX - 0.30f, 0.38f),
                p(CX - 0.29f, 0.60f),
                p(CX - 0.185f, 0.58f),
            ),
            poly(
                "armR",
                SilhouetteTone.SILHOUETTE,
                p(CX + 0.19f, 0.35f),
                p(CX + 0.30f, 0.38f),
                p(CX + 0.31f, 0.62f),
                p(CX + 0.195f, 0.60f),
            ),
            poly(
                "bracerL",
                SilhouetteTone.MEDIUM,
                p(CX - 0.29f, 0.51f),
                p(CX - 0.185f, 0.52f),
                p(CX - 0.19f, 0.61f),
                p(CX - 0.295f, 0.60f),
            ),
            poly(
                "bracerR",
                SilhouetteTone.MEDIUM,
                p(CX + 0.195f, 0.52f),
                p(CX + 0.31f, 0.53f),
                p(CX + 0.305f, 0.62f),
                p(CX + 0.20f, 0.61f),
            ),
            poly(
                "beltWrap",
                SilhouetteTone.MEDIUM,
                p(CX - 0.13f, 0.55f),
                p(CX + 0.13f, 0.55f),
                p(CX + 0.145f, 0.62f),
                p(CX - 0.145f, 0.62f),
            ),
            octagonShape("head", SilhouetteTone.SILHOUETTE, CX, 0.255f, 0.038f),
            poly(
                "browBand",
                SilhouetteTone.DARK,
                p(CX - 0.05f, 0.235f),
                p(CX + 0.05f, 0.235f),
                p(CX + 0.055f, 0.205f),
                p(CX, 0.185f),
                p(CX - 0.055f, 0.205f),
            ),
            poly("axeHandle", SilhouetteTone.DARK, p(CX + 0.30f, 0.33f), p(CX + 0.32f, 0.33f), p(CX + 0.32f, 0.88f), p(CX + 0.30f, 0.88f)),
            poly(
                "axeHead",
                SilhouetteTone.SILHOUETTE,
                p(CX + 0.31f, 0.34f),
                p(CX + 0.43f, 0.39f),
                p(CX + 0.44f, 0.50f),
                p(CX + 0.31f, 0.49f),
            ),
            poly("axeEdge", SilhouetteTone.LIGHT, p(CX + 0.41f, 0.405f), p(CX + 0.44f, 0.44f), p(CX + 0.43f, 0.49f), p(CX + 0.40f, 0.46f)),
        )
    }

    // --- Refined Monk: centred, symmetrical, halo, prayer, draped cloth, beads ---
    private fun refinedMonk(base: BodyBase): List<SilhouetteShape> {
        val g = gender(base)
        val sh = 0.135f * g.shoulder
        return listOf(
            ringShape("halo", SilhouetteTone.MEDIUM, CX, 0.150f, 0.088f, 0.012f),
            poly(
                "robeBody",
                SilhouetteTone.SILHOUETTE,
                p(CX - sh, 0.255f),
                p(CX + sh, 0.255f),
                p(CX + 0.115f, 0.50f),
                p(CX - 0.115f, 0.50f),
            ),
            poly(
                "lowerClothL",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.115f, 0.48f),
                p(CX - 0.005f, 0.48f),
                p(CX - 0.015f, 0.90f),
                p(CX - 0.175f, 0.90f),
            ),
            poly(
                "lowerClothR",
                SilhouetteTone.SILHOUETTE,
                p(CX + 0.005f, 0.48f),
                p(CX + 0.115f, 0.48f),
                p(CX + 0.175f, 0.90f),
                p(CX + 0.015f, 0.90f),
            ),
            poly("clothFoldL", SilhouetteTone.DARK, p(CX - 0.09f, 0.52f), p(CX - 0.03f, 0.52f), p(CX - 0.05f, 0.88f), p(CX - 0.11f, 0.88f)),
            poly("clothFoldR", SilhouetteTone.DARK, p(CX + 0.03f, 0.52f), p(CX + 0.09f, 0.52f), p(CX + 0.11f, 0.88f), p(CX + 0.05f, 0.88f)),
            poly(
                "sash",
                SilhouetteTone.MEDIUM,
                p(CX - sh, 0.48f),
                p(CX + sh, 0.48f),
                p(CX + sh + 0.01f, 0.535f),
                p(CX - sh - 0.01f, 0.535f),
            ),
            poly("sashKnot", SilhouetteTone.DARK, p(CX - 0.03f, 0.51f), p(CX + 0.03f, 0.51f), p(CX + 0.045f, 0.60f), p(CX - 0.045f, 0.60f)),
            poly(
                "forearmL",
                SilhouetteTone.SILHOUETTE,
                p(CX - sh + 0.01f, 0.29f),
                p(CX - sh + 0.05f, 0.31f),
                p(CX - 0.015f, 0.44f),
                p(CX - 0.05f, 0.42f),
            ),
            poly(
                "forearmR",
                SilhouetteTone.SILHOUETTE,
                p(CX + sh - 0.01f, 0.29f),
                p(CX + sh - 0.05f, 0.31f),
                p(CX + 0.015f, 0.44f),
                p(CX + 0.05f, 0.42f),
            ),
            poly("prayerHands", SilhouetteTone.LIGHT, p(CX, 0.35f), p(CX + 0.045f, 0.43f), p(CX, 0.49f), p(CX - 0.045f, 0.43f)),
            poly(
                "shoulderDrapeL",
                SilhouetteTone.DARK,
                p(CX - sh, 0.26f),
                p(CX - sh - 0.03f, 0.30f),
                p(CX - sh + 0.02f, 0.40f),
                p(CX - sh + 0.03f, 0.30f),
            ),
            poly(
                "shoulderDrapeR",
                SilhouetteTone.DARK,
                p(CX + sh, 0.26f),
                p(CX + sh + 0.03f, 0.30f),
                p(CX + sh - 0.02f, 0.40f),
                p(CX + sh - 0.03f, 0.30f),
            ),
            octagonShape("head", SilhouetteTone.SILHOUETTE, CX, 0.205f, 0.040f),
            beadRing("beads", SilhouetteTone.MEDIUM, CX, 0.29f, 0.085f),
            poly("collar", SilhouetteTone.DARK, p(CX - 0.05f, 0.245f), p(CX + 0.05f, 0.245f), p(CX + 0.035f, 0.28f), p(CX - 0.035f, 0.28f)),
        )
    }

    // --- Refined Magician: tallest, narrow, layered robe, pointed hat, ornate staff, floating focus ---
    private fun refinedMagician(base: BodyBase): List<SilhouetteShape> {
        val g = gender(base)
        val sh = 0.095f * g.shoulder
        return listOf(
            poly("staffShaft", SilhouetteTone.DARK, p(CX - 0.20f, 0.20f), p(CX - 0.18f, 0.20f), p(CX - 0.18f, 0.93f), p(CX - 0.20f, 0.93f)),
            ringShape("staffHead", SilhouetteTone.MEDIUM, CX - 0.19f, 0.155f, 0.048f, 0.014f),
            octagonShape("staffCore", SilhouetteTone.LIGHT, CX - 0.19f, 0.155f, 0.020f),
            poly("robeBody", SilhouetteTone.SILHOUETTE, p(CX - sh, 0.235f), p(CX + sh, 0.235f), p(CX + 0.19f, 0.94f), p(CX - 0.19f, 0.94f)),
            poly(
                "robeInnerL",
                SilhouetteTone.DARK,
                p(CX - 0.06f, 0.42f),
                p(CX - 0.005f, 0.42f),
                p(CX - 0.02f, 0.94f),
                p(CX - 0.11f, 0.94f),
            ),
            poly(
                "robeInnerR",
                SilhouetteTone.DARK,
                p(CX + 0.005f, 0.42f),
                p(CX + 0.06f, 0.42f),
                p(CX + 0.11f, 0.94f),
                p(CX + 0.02f, 0.94f),
            ),
            poly("hemGlow", SilhouetteTone.MEDIUM, p(CX - 0.10f, 0.82f), p(CX + 0.10f, 0.82f), p(CX + 0.19f, 0.94f), p(CX - 0.19f, 0.94f)),
            poly("mantle", SilhouetteTone.DARK, p(CX - 0.16f, 0.235f), p(CX + 0.16f, 0.235f), p(CX + 0.11f, 0.36f), p(CX - 0.11f, 0.36f)),
            poly(
                "sleeveR",
                SilhouetteTone.SILHOUETTE,
                p(CX + 0.10f, 0.25f),
                p(CX + 0.175f, 0.30f),
                p(CX + 0.15f, 0.56f),
                p(CX + 0.07f, 0.54f),
            ),
            poly(
                "collar",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.06f, 0.225f),
                p(CX + 0.06f, 0.225f),
                p(CX + 0.10f, 0.28f),
                p(CX - 0.10f, 0.28f),
            ),
            octagonShape("head", SilhouetteTone.SILHOUETTE, CX, 0.185f, 0.032f),
            poly(
                "hatCone",
                SilhouetteTone.SILHOUETTE,
                p(CX + 0.02f, 0.020f),
                p(CX + 0.045f, 0.075f),
                p(CX + 0.11f, 0.155f),
                p(CX - 0.11f, 0.155f),
            ),
            poly("hatFold", SilhouetteTone.DARK, p(CX + 0.02f, 0.020f), p(CX + 0.10f, 0.145f), p(CX + 0.04f, 0.13f)),
            poly(
                "hatBrim",
                SilhouetteTone.DARK,
                p(CX - 0.13f, 0.150f),
                p(CX + 0.13f, 0.150f),
                p(CX + 0.095f, 0.195f),
                p(CX - 0.095f, 0.195f),
            ),
            octagonShape("focus", SilhouetteTone.LIGHT, CX + 0.18f, 0.42f, 0.030f),
            octagonShape("focusRing", SilhouetteTone.MEDIUM, CX + 0.18f, 0.42f, 0.050f),
        )
    }

    // --- Refined Assassin: narrowest, hooded, sharp, trailing scarf, dual blades, dynamic ---
    private fun refinedAssassin(base: BodyBase): List<SilhouetteShape> {
        val g = gender(base)
        val sh = 0.085f * g.shoulder
        val waist = 0.055f * g.waist
        return listOf(
            poly("torso", SilhouetteTone.SILHOUETTE, p(CX - sh, 0.275f), p(CX + sh, 0.275f), p(CX + waist, 0.54f), p(CX - waist, 0.54f)),
            poly(
                "legR",
                SilhouetteTone.SILHOUETTE,
                p(CX + 0.005f, 0.52f),
                p(CX + waist + 0.015f, 0.52f),
                p(CX + 0.075f, 0.92f),
                p(CX + 0.035f, 0.92f),
            ),
            poly(
                "legL",
                SilhouetteTone.SILHOUETTE,
                p(CX - waist - 0.005f, 0.52f),
                p(CX - 0.005f, 0.52f),
                p(CX - 0.02f, 0.92f),
                p(CX - 0.06f, 0.92f),
            ),
            poly("bootR", SilhouetteTone.DARK, p(CX + 0.035f, 0.88f), p(CX + 0.08f, 0.88f), p(CX + 0.10f, 0.94f), p(CX + 0.025f, 0.94f)),
            poly("bootL", SilhouetteTone.DARK, p(CX - 0.065f, 0.88f), p(CX - 0.015f, 0.88f), p(CX - 0.01f, 0.94f), p(CX - 0.085f, 0.94f)),
            poly("scarf1", SilhouetteTone.MEDIUM, p(CX + 0.05f, 0.29f), p(CX + 0.24f, 0.24f), p(CX + 0.27f, 0.27f), p(CX + 0.07f, 0.33f)),
            poly("scarf2", SilhouetteTone.DARK, p(CX + 0.16f, 0.27f), p(CX + 0.30f, 0.28f), p(CX + 0.29f, 0.31f), p(CX + 0.16f, 0.31f)),
            poly(
                "armR",
                SilhouetteTone.SILHOUETTE,
                p(CX + sh - 0.01f, 0.30f),
                p(CX + sh + 0.035f, 0.32f),
                p(CX + 0.11f, 0.50f),
                p(CX + 0.065f, 0.49f),
            ),
            poly(
                "armL",
                SilhouetteTone.SILHOUETTE,
                p(CX - sh + 0.01f, 0.30f),
                p(CX - sh - 0.035f, 0.32f),
                p(CX - 0.10f, 0.50f),
                p(CX - 0.06f, 0.48f),
            ),
            poly("bladeR", SilhouetteTone.LIGHT, p(CX + 0.10f, 0.49f), p(CX + 0.13f, 0.49f), p(CX + 0.155f, 0.70f), p(CX + 0.125f, 0.70f)),
            poly("bladeL", SilhouetteTone.MEDIUM, p(CX - 0.10f, 0.48f), p(CX - 0.075f, 0.48f), p(CX - 0.05f, 0.66f), p(CX - 0.08f, 0.66f)),
            poly(
                "hood",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.06f, 0.185f),
                p(CX + 0.02f, 0.16f),
                p(CX + 0.075f, 0.22f),
                p(CX + 0.05f, 0.30f),
                p(CX - 0.02f, 0.27f),
                p(CX - 0.06f, 0.29f),
            ),
            poly(
                "faceShadow",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.03f, 0.22f),
                p(CX + 0.035f, 0.225f),
                p(CX + 0.02f, 0.27f),
                p(CX - 0.02f, 0.265f),
            ),
            poly("beltStrap", SilhouetteTone.DARK, p(CX - waist, 0.50f), p(CX + waist, 0.50f), p(CX + 0.05f, 0.60f), p(CX - 0.02f, 0.58f)),
            poly("clothTail", SilhouetteTone.DARK, p(CX - 0.02f, 0.52f), p(CX + 0.03f, 0.52f), p(CX + 0.07f, 0.78f), p(CX + 0.03f, 0.78f)),
            poly(
                "wrapForearmR",
                SilhouetteTone.MEDIUM,
                p(CX + 0.075f, 0.45f),
                p(CX + 0.115f, 0.47f),
                p(CX + 0.105f, 0.53f),
                p(CX + 0.065f, 0.51f),
            ),
        )
    }

    // --- Refined Fighter: balanced, layered plate, straight sword, separated armoured legs ---
    private fun refinedFighter(base: BodyBase): List<SilhouetteShape> {
        val g = gender(base)
        val sh = 0.175f * g.shoulder
        val waist = 0.115f * g.waist
        return listOf(
            poly(
                "torso",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.13f, 0.28f),
                p(CX + 0.13f, 0.28f),
                p(CX + waist, 0.52f),
                p(CX - waist, 0.52f),
            ),
            poly(
                "legL",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.02f, 0.52f),
                p(CX - 0.11f, 0.53f),
                p(CX - 0.10f, 0.78f),
                p(CX - 0.085f, 0.90f),
                p(CX - 0.03f, 0.90f),
                p(CX - 0.025f, 0.70f),
            ),
            poly(
                "legR",
                SilhouetteTone.SILHOUETTE,
                p(CX + 0.02f, 0.52f),
                p(CX + 0.11f, 0.53f),
                p(CX + 0.10f, 0.78f),
                p(CX + 0.085f, 0.90f),
                p(CX + 0.03f, 0.90f),
                p(CX + 0.025f, 0.70f),
            ),
            poly("poleynL", SilhouetteTone.MEDIUM, p(CX - 0.105f, 0.70f), p(CX - 0.03f, 0.70f), p(CX - 0.04f, 0.77f), p(CX - 0.11f, 0.77f)),
            poly("poleynR", SilhouetteTone.MEDIUM, p(CX + 0.03f, 0.70f), p(CX + 0.105f, 0.70f), p(CX + 0.11f, 0.77f), p(CX + 0.04f, 0.77f)),
            poly("bootL", SilhouetteTone.DARK, p(CX - 0.095f, 0.88f), p(CX - 0.02f, 0.88f), p(CX - 0.01f, 0.94f), p(CX - 0.115f, 0.94f)),
            poly("bootR", SilhouetteTone.DARK, p(CX + 0.02f, 0.88f), p(CX + 0.095f, 0.88f), p(CX + 0.115f, 0.94f), p(CX + 0.01f, 0.94f)),
            poly("faulds", SilhouetteTone.DARK, p(CX - 0.12f, 0.50f), p(CX + 0.12f, 0.50f), p(CX + 0.13f, 0.60f), p(CX - 0.13f, 0.60f)),
            poly(
                "breastplate",
                SilhouetteTone.MEDIUM,
                p(CX - 0.11f, 0.30f),
                p(CX + 0.11f, 0.30f),
                p(CX + 0.095f, 0.46f),
                p(CX - 0.095f, 0.46f),
            ),
            poly(
                "plateRidge",
                SilhouetteTone.LIGHT,
                p(CX - 0.02f, 0.31f),
                p(CX + 0.02f, 0.31f),
                p(CX + 0.03f, 0.45f),
                p(CX - 0.03f, 0.45f),
            ),
            poly(
                "pauldronL",
                SilhouetteTone.MEDIUM,
                p(CX - 0.10f, 0.26f),
                p(CX - sh, 0.255f),
                p(CX - sh - 0.01f, 0.33f),
                p(CX - 0.11f, 0.34f),
            ),
            poly(
                "pauldronR",
                SilhouetteTone.MEDIUM,
                p(CX + 0.10f, 0.26f),
                p(CX + sh, 0.255f),
                p(CX + sh + 0.01f, 0.33f),
                p(CX + 0.11f, 0.34f),
            ),
            poly(
                "armL",
                SilhouetteTone.SILHOUETTE,
                p(CX - sh + 0.01f, 0.32f),
                p(CX - sh - 0.02f, 0.34f),
                p(CX - 0.135f, 0.54f),
                p(CX - 0.095f, 0.53f),
            ),
            poly(
                "armR",
                SilhouetteTone.SILHOUETTE,
                p(CX + sh - 0.01f, 0.32f),
                p(CX + sh + 0.02f, 0.34f),
                p(CX + 0.135f, 0.54f),
                p(CX + 0.095f, 0.53f),
            ),
            poly(
                "gauntletL",
                SilhouetteTone.DARK,
                p(CX - 0.135f, 0.51f),
                p(CX - 0.09f, 0.52f),
                p(CX - 0.095f, 0.59f),
                p(CX - 0.14f, 0.58f),
            ),
            octagonShape("helm", SilhouetteTone.SILHOUETTE, CX, 0.235f, 0.042f),
            poly(
                "helmBrow",
                SilhouetteTone.DARK,
                p(CX - 0.042f, 0.225f),
                p(CX + 0.042f, 0.225f),
                p(CX + 0.042f, 0.245f),
                p(CX - 0.042f, 0.245f),
            ),
            poly(
                "swordBlade",
                SilhouetteTone.LIGHT,
                p(CX + 0.135f, 0.40f),
                p(CX + 0.16f, 0.40f),
                p(CX + 0.16f, 0.80f),
                p(CX + 0.1475f, 0.84f),
                p(CX + 0.135f, 0.80f),
            ),
            poly(
                "swordGuard",
                SilhouetteTone.DARK,
                p(CX + 0.11f, 0.39f),
                p(CX + 0.185f, 0.39f),
                p(CX + 0.185f, 0.42f),
                p(CX + 0.11f, 0.42f),
            ),
        )
    }

    // --- Refined Ranger: lean, hooded, diagonal cloak, chest strap, bow + quiver ---
    private fun refinedRanger(base: BodyBase): List<SilhouetteShape> {
        val g = gender(base)
        val sh = 0.140f * g.shoulder
        val waist = 0.095f * g.waist
        return listOf(
            poly(
                "cloak",
                SilhouetteTone.SILHOUETTE,
                p(CX - sh - 0.02f, 0.27f),
                p(CX + 0.09f, 0.30f),
                p(CX + 0.20f, 0.72f),
                p(CX + 0.12f, 0.90f),
                p(CX - 0.15f, 0.90f),
                p(CX - 0.13f, 0.55f),
            ),
            poly("cloakFold", SilhouetteTone.DARK, p(CX + 0.02f, 0.40f), p(CX + 0.10f, 0.44f), p(CX + 0.15f, 0.86f), p(CX + 0.04f, 0.86f)),
            poly(
                "torso",
                SilhouetteTone.SILHOUETTE,
                p(CX - sh + 0.02f, 0.29f),
                p(CX + sh, 0.29f),
                p(CX + waist, 0.53f),
                p(CX - waist, 0.53f),
            ),
            poly(
                "legR",
                SilhouetteTone.SILHOUETTE,
                p(CX + 0.005f, 0.52f),
                p(CX + waist, 0.52f),
                p(CX + 0.075f, 0.90f),
                p(CX + 0.035f, 0.90f),
            ),
            poly(
                "legL",
                SilhouetteTone.SILHOUETTE,
                p(CX - waist, 0.52f),
                p(CX - 0.005f, 0.52f),
                p(CX - 0.035f, 0.90f),
                p(CX - 0.075f, 0.90f),
            ),
            poly("bootR", SilhouetteTone.DARK, p(CX + 0.035f, 0.87f), p(CX + 0.08f, 0.87f), p(CX + 0.10f, 0.93f), p(CX + 0.025f, 0.93f)),
            poly("bootL", SilhouetteTone.DARK, p(CX - 0.08f, 0.87f), p(CX - 0.035f, 0.87f), p(CX - 0.025f, 0.93f), p(CX - 0.10f, 0.93f)),
            poly(
                "chestStrap",
                SilhouetteTone.MEDIUM,
                p(CX - 0.09f, 0.31f),
                p(CX - 0.05f, 0.30f),
                p(CX + 0.08f, 0.49f),
                p(CX + 0.04f, 0.51f),
            ),
            poly("belt", SilhouetteTone.DARK, p(CX - waist, 0.50f), p(CX + waist, 0.50f), p(CX + waist, 0.55f), p(CX - waist, 0.55f)),
            poly(
                "hood",
                SilhouetteTone.SILHOUETTE,
                p(CX - 0.06f, 0.20f),
                p(CX + 0.06f, 0.20f),
                p(CX + 0.055f, 0.30f),
                p(CX - 0.055f, 0.30f),
            ),
            poly("hoodPeak", SilhouetteTone.DARK, p(CX - 0.02f, 0.185f), p(CX + 0.05f, 0.215f), p(CX + 0.02f, 0.24f)),
            octagonShape("head", SilhouetteTone.SILHOUETTE, CX, 0.245f, 0.030f),
            poly(
                "armL",
                SilhouetteTone.SILHOUETTE,
                p(CX - sh + 0.02f, 0.31f),
                p(CX - sh - 0.01f, 0.33f),
                p(CX - 0.115f, 0.52f),
                p(CX - 0.075f, 0.51f),
            ),
            bowShape("bow", SilhouetteTone.MEDIUM, CX - 0.20f, 0.50f, 0.30f),
            poly("quiver", SilhouetteTone.DARK, p(CX + 0.11f, 0.24f), p(CX + 0.155f, 0.24f), p(CX + 0.145f, 0.45f), p(CX + 0.10f, 0.45f)),
            poly(
                "arrowFletch1",
                SilhouetteTone.LIGHT,
                p(CX + 0.105f, 0.19f),
                p(CX + 0.135f, 0.19f),
                p(CX + 0.135f, 0.25f),
                p(CX + 0.105f, 0.25f),
            ),
            poly(
                "arrowFletch2",
                SilhouetteTone.MEDIUM,
                p(CX + 0.14f, 0.20f),
                p(CX + 0.165f, 0.20f),
                p(CX + 0.16f, 0.25f),
                p(CX + 0.135f, 0.25f),
            ),
        )
    }

    // --- shared shape helpers ---

    // A plain point (Offset is an inline value class, which cannot be a vararg element type).
    private class Pt(val x: Float, val y: Float)

    private fun p(
        x: Float,
        y: Float,
    ) = Pt(x, y)

    private fun poly(
        name: String,
        tone: SilhouetteTone,
        vararg pts: Pt,
    ) = SilhouetteShape(name, tone, pts.map { Offset(it.x, it.y) })

    private fun octagonShape(
        name: String,
        tone: SilhouetteTone,
        cx: Float,
        cy: Float,
        r: Float,
    ): SilhouetteShape {
        val pts =
            (0 until 8).map {
                val a = Math.PI / 4.0 * it - Math.PI / 2.0
                Offset(cx + (cos(a) * r).toFloat(), cy + (sin(a) * r).toFloat())
            }
        return SilhouetteShape(name, tone, pts)
    }

    /** A thin ring (Monk halo) approximated as a many-sided annulus outline swept to a polygon. */
    private fun ringShape(
        name: String,
        tone: SilhouetteTone,
        cx: Float,
        cy: Float,
        r: Float,
        thickness: Float,
    ): SilhouetteShape {
        val outer = (0 until 16).map { arc(cx, cy, r, it, 16) }
        val inner = (15 downTo 0).map { arc(cx, cy, r - thickness, it, 16) }
        return SilhouetteShape(name, tone, outer + inner)
    }

    private fun beadRing(
        name: String,
        tone: SilhouetteTone,
        cx: Float,
        cy: Float,
        r: Float,
    ): SilhouetteShape {
        // A simple beaded collar: a shallow arc of the necklace as one flat shape.
        val pts = (0..6).map { arc(cx, cy + 0.02f, r, it + 5, 16) }
        return SilhouetteShape(name, tone, pts + listOf(Offset(cx + 0.04f, cy + 0.10f), Offset(cx - 0.04f, cy + 0.10f)))
    }

    private fun bowShape(
        name: String,
        tone: SilhouetteTone,
        cx: Float,
        cy: Float,
        h: Float,
    ): SilhouetteShape {
        val pts =
            listOf(
                Offset(cx, cy - h / 2f),
                Offset(cx - 0.06f, cy),
                Offset(cx, cy + h / 2f),
                Offset(cx - 0.025f, cy + h / 2f),
                Offset(cx - 0.085f, cy),
                Offset(cx - 0.025f, cy - h / 2f),
            )
        return SilhouetteShape(name, tone, pts)
    }

    private fun arc(
        cx: Float,
        cy: Float,
        r: Float,
        step: Int,
        steps: Int,
    ): Offset {
        val a = Math.PI * step / (steps - 1)
        return Offset(cx - (cos(a) * r).toFloat(), cy - (sin(a) * r).toFloat())
    }

    /** A symmetric pair of straight legs (returns [left, right]). */
    private fun legPair(
        half: Float,
        topY: Float,
        outerHalf: Float,
        botY: Float,
        tone: SilhouetteTone,
        leftName: String,
        rightName: String,
    ): List<SilhouetteShape> {
        val rightPts =
            listOf(
                Offset(CX + 0.005f, topY),
                Offset(CX + half + 0.01f, topY),
                Offset(CX + outerHalf, botY),
                Offset(CX + 0.02f, botY),
            )
        val right = SilhouetteShape(rightName, tone, rightPts)
        val left = SilhouetteShape(leftName, tone, rightPts.map { Offset(1f - it.x, it.y) })
        return listOf(left, right)
    }
}
