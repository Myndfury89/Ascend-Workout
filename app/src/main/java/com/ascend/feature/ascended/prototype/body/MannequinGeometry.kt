package com.ascend.feature.ascended.prototype.body

import androidx.compose.ui.geometry.Offset
import com.ascend.feature.ascended.prototype.model.AttachPoint
import com.ascend.feature.ascended.prototype.model.BodyBase
import com.ascend.feature.ascended.prototype.model.MuscleRegionId
import com.ascend.feature.ascended.prototype.model.RegionSide

/*
 * The base mannequin as pure, testable geometry. Every region is a normalized polygon (x, y in
 * 0..1, front-facing, head at top) so the body regions are INDIVIDUALLY ADDRESSABLE — each can
 * later respond to class, attributes, Skill mastery, aura, and equipment. Compose Path construction
 * lives in the drawing layer; keeping the point data pure here makes male/female geometry unit-
 * testable without Compose. Male and female share the same region set and vertical rhythm but use
 * different horizontal proportions (broader male shoulders / stronger female hip rhythm), so the two
 * bases are genuinely distinct rather than a recolor.
 */

/** Horizontal half-widths + limb widths (fractions of viewport width) that shape a body base. */
data class BodyProportions(
    val shoulderHalf: Float,
    val chestHalf: Float,
    val waistHalf: Float,
    val hipHalf: Float,
    val thighHalf: Float,
    val kneeHalf: Float,
    val calfHalf: Float,
    val ankleHalf: Float,
    val neckHalf: Float,
    val headHalf: Float,
    val upperArmHalf: Float,
    val foreArmHalf: Float,
)

/** One addressable region: its id, which side of the body, its polygon, and a base grayscale tone. */
data class MannequinRegion(
    val id: MuscleRegionId,
    val side: RegionSide,
    val polygon: List<Offset>,
    val tone: Float,
)

object MannequinGeometry {
    // Vertical anchors (fraction of height), shared by both bases — the figure's rhythm.
    private const val HEAD_TOP = 0.035f
    private const val HEAD_BOT = 0.120f
    private const val JAW = 0.100f
    private const val NECK_BOT = 0.160f
    private const val SHOULDER = 0.180f
    private const val CHEST_TOP = 0.195f
    private const val CHEST_BOT = 0.300f
    private const val ABS_TOP = 0.300f
    private const val ABS_MID = 0.380f
    private const val ABS_BOT = 0.455f
    private const val HIP_TOP = 0.455f
    private const val CROTCH = 0.550f
    private const val KNEE_TOP = 0.710f
    private const val KNEE_BOT = 0.752f
    private const val CALF_BOT = 0.900f
    private const val ANKLE = 0.905f
    private const val FOOT_BOT = 0.965f
    private const val ELBOW = 0.400f
    private const val WRIST = 0.520f
    private const val HAND_BOT = 0.575f

    private const val CX = 0.5f

    fun proportionsFor(base: BodyBase): BodyProportions =
        when (base) {
            BodyBase.MALE ->
                BodyProportions(
                    shoulderHalf = 0.330f, chestHalf = 0.215f, waistHalf = 0.145f, hipHalf = 0.165f,
                    thighHalf = 0.100f, kneeHalf = 0.062f, calfHalf = 0.072f, ankleHalf = 0.036f,
                    neckHalf = 0.058f, headHalf = 0.078f, upperArmHalf = 0.058f, foreArmHalf = 0.044f,
                )
            BodyBase.FEMALE ->
                BodyProportions(
                    shoulderHalf = 0.265f, chestHalf = 0.165f, waistHalf = 0.113f, hipHalf = 0.188f,
                    thighHalf = 0.106f, kneeHalf = 0.055f, calfHalf = 0.066f, ankleHalf = 0.033f,
                    neckHalf = 0.047f, headHalf = 0.070f, upperArmHalf = 0.048f, foreArmHalf = 0.037f,
                )
        }

    /** Anchor points for class / equipment / Skill overlays; they shift with the body base. */
    fun attachPoints(base: BodyBase): Map<AttachPoint, Offset> {
        val p = proportionsFor(base)
        return mapOf(
            AttachPoint.HEAD to Offset(CX, (HEAD_TOP + HEAD_BOT) / 2f),
            AttachPoint.CHEST_CENTER to Offset(CX, (CHEST_TOP + CHEST_BOT) / 2f),
            AttachPoint.SHOULDER_RIGHT to Offset(CX + p.shoulderHalf, SHOULDER + 0.03f),
            AttachPoint.SHOULDER_LEFT to Offset(CX - p.shoulderHalf, SHOULDER + 0.03f),
            AttachPoint.HAND_RIGHT to Offset(CX + p.shoulderHalf, HAND_BOT),
            AttachPoint.HAND_LEFT to Offset(CX - p.shoulderHalf, HAND_BOT),
            AttachPoint.WAIST to Offset(CX, ABS_BOT),
            AttachPoint.FOOT_RIGHT to Offset(CX + p.ankleHalf + 0.02f, FOOT_BOT),
            AttachPoint.FOOT_LEFT to Offset(CX - p.ankleHalf - 0.02f, FOOT_BOT),
        )
    }

    /**
     * Build every addressable region for a body base, in back-to-front draw order (legs, torso,
     * head, then arms over). Central regions are emitted once; paired regions are emitted for the
     * right side and mirrored to the left.
     */
    fun build(base: BodyBase): List<MannequinRegion> {
        val p = proportionsFor(base)
        val out = ArrayList<MannequinRegion>()

        // --- legs (drawn first, behind the torso) ---
        pair(out, MuscleRegionId.HIP, tone = 0.62f, hip(p))
        pair(out, MuscleRegionId.THIGH, tone = 0.70f, thigh(p))
        pair(out, MuscleRegionId.KNEE, tone = 0.56f, knee(p))
        pair(out, MuscleRegionId.CALF, tone = 0.66f, calf(p))
        pair(out, MuscleRegionId.FOOT, tone = 0.58f, foot(p))

        // --- torso ---
        center(out, MuscleRegionId.PELVIS, tone = 0.58f, pelvis(p))
        center(out, MuscleRegionId.ABS_LOWER, tone = 0.66f, absLower(p))
        center(out, MuscleRegionId.ABS_UPPER, tone = 0.70f, absUpper(p))
        pair(out, MuscleRegionId.OBLIQUE, tone = 0.56f, oblique(p))
        center(out, MuscleRegionId.STERNUM, tone = 0.48f, sternum())
        pair(out, MuscleRegionId.PECTORAL, tone = 0.74f, pectoral(base, p))
        pair(out, MuscleRegionId.TRAPEZIUS, tone = 0.52f, trapezius(p))
        pair(out, MuscleRegionId.DELTOID, tone = 0.72f, deltoid(p))

        // --- head / neck ---
        center(out, MuscleRegionId.NECK, tone = 0.55f, neck(p))
        center(out, MuscleRegionId.HEAD, tone = 0.66f, head(p))

        // --- arms (drawn last, over the torso sides) ---
        pair(out, MuscleRegionId.UPPER_ARM, tone = 0.64f, upperArm(p))
        pair(out, MuscleRegionId.FOREARM, tone = 0.58f, foreArm(p))
        pair(out, MuscleRegionId.HAND, tone = 0.60f, hand(p))

        return out
    }

    // --- region polygons (right side / central), normalized ---

    private fun head(p: BodyProportions): List<Offset> {
        val h = p.headHalf
        return listOf(
            o(CX, HEAD_TOP),
            o(CX + 0.7f * h, 0.050f),
            o(CX + h, 0.070f),
            o(CX + 0.55f * h, JAW),
            o(CX, HEAD_BOT),
            o(CX - 0.55f * h, JAW),
            o(CX - h, 0.070f),
            o(CX - 0.7f * h, 0.050f),
        )
    }

    private fun neck(p: BodyProportions): List<Offset> {
        val n = p.neckHalf
        return listOf(
            o(CX - 0.8f * n, JAW + 0.005f),
            o(CX + 0.8f * n, JAW + 0.005f),
            o(CX + n, SHOULDER),
            o(CX - n, SHOULDER),
        )
    }

    private fun sternum(): List<Offset> =
        listOf(o(CX - 0.03f, CHEST_TOP + 0.02f), o(CX + 0.03f, CHEST_TOP + 0.02f), o(CX + 0.02f, ABS_TOP), o(CX - 0.02f, ABS_TOP))

    private fun absUpper(p: BodyProportions): List<Offset> {
        val w = p.waistHalf
        return listOf(o(CX - 0.95f * w, ABS_TOP), o(CX + 0.95f * w, ABS_TOP), o(CX + 0.9f * w, ABS_MID), o(CX - 0.9f * w, ABS_MID))
    }

    private fun absLower(p: BodyProportions): List<Offset> {
        val w = p.waistHalf
        return listOf(o(CX - 0.9f * w, ABS_MID), o(CX + 0.9f * w, ABS_MID), o(CX + 0.8f * w, ABS_BOT), o(CX - 0.8f * w, ABS_BOT))
    }

    private fun pelvis(p: BodyProportions): List<Offset> {
        val w = 0.8f * p.waistHalf
        return listOf(o(CX - w, ABS_BOT), o(CX + w, ABS_BOT), o(CX + 0.06f, CROTCH - 0.01f), o(CX, CROTCH), o(CX - 0.06f, CROTCH - 0.01f))
    }

    private fun trapezius(p: BodyProportions): List<Offset> =
        listOf(
            o(CX + p.neckHalf, NECK_BOT - 0.008f),
            o(CX + 0.8f * p.shoulderHalf, SHOULDER),
            o(CX + 0.7f * p.chestHalf, CHEST_TOP + 0.02f),
        )

    private fun deltoid(p: BodyProportions): List<Offset> =
        listOf(
            o(CX + 0.85f * p.chestHalf, CHEST_TOP),
            o(CX + p.shoulderHalf, SHOULDER + 0.01f),
            o(CX + p.shoulderHalf - 0.01f, SHOULDER + 0.06f),
            o(CX + 0.8f * p.chestHalf, CHEST_TOP + 0.05f),
        )

    private fun pectoral(
        base: BodyBase,
        p: BodyProportions,
    ): List<Offset> {
        val c = p.chestHalf
        return if (base == BodyBase.FEMALE) {
            // A rounder lower chest mass with a defined lower transition.
            listOf(
                o(CX + 0.03f, CHEST_TOP + 0.03f),
                o(CX + c, CHEST_TOP + 0.05f),
                o(CX + c + 0.012f, CHEST_BOT - 0.03f),
                o(CX + 0.6f * c, CHEST_BOT + 0.012f),
                o(CX + 0.03f, CHEST_BOT - 0.01f),
            )
        } else {
            // A broader, flatter upper pectoral plane.
            listOf(
                o(CX + 0.03f, CHEST_TOP + 0.02f),
                o(CX + c, CHEST_TOP + 0.03f),
                o(CX + 0.95f * c, CHEST_BOT - 0.02f),
                o(CX + 0.03f, CHEST_BOT),
            )
        }
    }

    private fun oblique(p: BodyProportions): List<Offset> =
        listOf(
            o(CX + 0.9f * p.chestHalf, CHEST_BOT - 0.02f),
            o(CX + p.waistHalf + 0.01f, ABS_TOP + 0.02f),
            o(CX + p.waistHalf, ABS_BOT),
            o(CX + 0.9f * p.waistHalf, ABS_MID),
        )

    private fun upperArm(p: BodyProportions): List<Offset> {
        val xi = p.shoulderHalf - p.upperArmHalf
        val xo = p.shoulderHalf + 0.005f
        return listOf(o(CX + xi, SHOULDER + 0.03f), o(CX + xo, SHOULDER + 0.04f), o(CX + xo + 0.008f, ELBOW), o(CX + xi + 0.012f, ELBOW))
    }

    private fun foreArm(p: BodyProportions): List<Offset> {
        val xi = p.shoulderHalf - p.upperArmHalf
        val xo = p.shoulderHalf + 0.005f
        return listOf(
            o(CX + xi + 0.012f, ELBOW),
            o(CX + xo + 0.008f, ELBOW),
            o(CX + p.shoulderHalf + 0.012f, WRIST),
            o(CX + p.shoulderHalf - p.foreArmHalf + 0.02f, WRIST),
        )
    }

    private fun hand(p: BodyProportions): List<Offset> =
        listOf(
            o(CX + p.shoulderHalf - p.foreArmHalf + 0.02f, WRIST),
            o(CX + p.shoulderHalf + 0.012f, WRIST),
            o(CX + p.shoulderHalf + 0.02f, HAND_BOT),
            o(CX + p.shoulderHalf - 0.02f, HAND_BOT + 0.005f),
        )

    private fun hip(p: BodyProportions): List<Offset> =
        listOf(
            o(CX + 0.8f * p.waistHalf, ABS_BOT),
            o(CX + p.hipHalf, HIP_TOP + 0.02f),
            o(CX + 0.9f * p.hipHalf, CROTCH - 0.005f),
            o(CX + 0.05f, CROTCH),
        )

    private fun thigh(p: BodyProportions): List<Offset> =
        listOf(
            o(CX + 0.02f, CROTCH),
            o(CX + 0.92f * p.hipHalf, CROTCH - 0.015f),
            o(CX + p.thighHalf + 0.03f, (CROTCH + KNEE_TOP) / 2f),
            o(CX + p.kneeHalf + 0.01f, KNEE_TOP),
            o(CX + 0.02f, KNEE_TOP),
        )

    private fun knee(p: BodyProportions): List<Offset> =
        listOf(o(CX + 0.02f, KNEE_TOP), o(CX + p.kneeHalf + 0.01f, KNEE_TOP), o(CX + p.kneeHalf, KNEE_BOT), o(CX + 0.02f, KNEE_BOT))

    private fun calf(p: BodyProportions): List<Offset> =
        listOf(
            o(CX + 0.02f, KNEE_BOT),
            o(CX + p.kneeHalf, KNEE_BOT),
            o(CX + p.calfHalf + 0.01f, (KNEE_BOT + CALF_BOT) / 2f),
            o(CX + p.ankleHalf + 0.01f, ANKLE),
            o(CX + 0.025f, ANKLE),
        )

    private fun foot(p: BodyProportions): List<Offset> =
        listOf(o(CX + 0.02f, ANKLE), o(CX + p.ankleHalf + 0.01f, ANKLE), o(CX + p.ankleHalf + 0.03f, FOOT_BOT), o(CX + 0.015f, FOOT_BOT))

    // --- helpers ---

    private fun o(
        x: Float,
        y: Float,
    ) = Offset(x, y)

    private fun center(
        out: MutableList<MannequinRegion>,
        id: MuscleRegionId,
        tone: Float,
        polygon: List<Offset>,
    ) {
        out.add(MannequinRegion(id, RegionSide.CENTER, polygon, tone))
    }

    private fun pair(
        out: MutableList<MannequinRegion>,
        id: MuscleRegionId,
        tone: Float,
        rightPolygon: List<Offset>,
    ) {
        out.add(MannequinRegion(id, RegionSide.RIGHT, rightPolygon, tone))
        out.add(MannequinRegion(id, RegionSide.LEFT, rightPolygon.map { Offset(1f - it.x, it.y) }, tone))
    }
}
