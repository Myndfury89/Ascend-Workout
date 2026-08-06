package com.ascend.feature.ascended.prototype.body

import androidx.compose.ui.geometry.Offset
import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase
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

    /** True when a class has a CP3 refined silhouette (others still fall back to the CP2 blockout). */
    fun hasRefined(cls: AscendedClass): Boolean = cls == AscendedClass.GUARDIAN

    fun build(
        cls: AscendedClass,
        base: BodyBase,
        fidelity: SilhouetteFidelity = SilhouetteFidelity.BLOCKOUT,
    ): ClassSilhouette =
        when (fidelity) {
            SilhouetteFidelity.BLOCKOUT -> ClassSilhouette(blockout(cls, base))
            SilhouetteFidelity.REFINED -> ClassSilhouette(refined(cls, base))
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
    ): List<SilhouetteShape> =
        when (cls) {
            AscendedClass.GUARDIAN -> refinedGuardian(base)
            else -> blockout(cls, base)
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
